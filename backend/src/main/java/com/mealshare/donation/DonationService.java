package com.mealshare.donation;

import com.mealshare.audit.AuditService;
import com.mealshare.common.exception.ResourceNotFoundException;
import com.mealshare.common.util.GeometryUtils;
import com.mealshare.donation.dto.CreateDonationRequest;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donation.dto.UpdateDonationRequest;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileService;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DonationService {

    private final DonationRepository donationRepository;
    private final DonorProfileService donorProfileService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DonationService(DonationRepository donationRepository,
                           DonorProfileService donorProfileService,
                           UserRepository userRepository,
                           AuditService auditService) {
        this.donationRepository = donationRepository;
        this.donorProfileService = donorProfileService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DonationResponse createDonation(String firebaseUid,
                                           CreateDonationRequest request,
                                           HttpServletRequest httpRequest) {
        User user = validateAndGetDonorUser(firebaseUid);

        // 1. Food Safety Declaration Check
        if (request.foodSafetyDeclared() == null || !request.foodSafetyDeclared()) {
            throw new IllegalArgumentException("Food safety declaration is mandatory for donation creation.");
        }

        // 2. Temporal Validations
        validateTemporalConstraints(
                request.preparationDateTime(),
                request.consumeBefore(),
                request.pickupAvailableFrom(),
                request.pickupAvailableUntil()
        );

        // 3. Coordinate Validations
        validateCoordinates(request.latitude(), request.longitude());

        // 4. Resolve / Ensure DonorProfile
        DonorProfile donorProfile = donorProfileService.getOrCreateProfileForUser(
                user,
                request.pickupAddress(),
                request.latitude(),
                request.longitude()
        );

        // 5. PostGIS Point Creation (Longitude = X, Latitude = Y, SRID 4326)
        Point location = GeometryUtils.createPoint(request.longitude(), request.latitude());

        // 6. Entity Mapping & Safe Initial Status
        Donation donation = new Donation();
        donation.setDonor(donorProfile);
        donation.setTitle(request.title().trim());
        donation.setDescription(request.description() != null ? request.description().trim() : null);
        donation.setFoodCategory(request.foodCategory());
        donation.setQuantity(request.quantity());
        donation.setQuantityUnit(request.quantityUnit().trim());
        donation.setPreparationDateTime(request.preparationDateTime());
        donation.setConsumeBefore(request.consumeBefore());
        donation.setPickupAvailableFrom(request.pickupAvailableFrom());
        donation.setPickupAvailableUntil(request.pickupAvailableUntil());
        donation.setPickupAddress(request.pickupAddress().trim());
        donation.setLatitude(request.latitude());
        donation.setLongitude(request.longitude());
        donation.setLocation(location);
        donation.setFoodSafetyDeclared(true);
        donation.setFoodSafetyDeclarationText(request.foodSafetyDeclarationText() != null
                ? request.foodSafetyDeclarationText().trim()
                : "Standard MealShare Food Safety & Freshness Declaration signed by donor.");
        donation.setFoodSafetyDeclaredAt(Instant.now());
        donation.setStatus(DonationStatus.AVAILABLE);

        donation = donationRepository.save(donation);

        // 7. Audit Logging
        auditService.logEvent(
                user.getId().toString(),
                "DONOR",
                "DONATION_CREATED",
                "Donation",
                donation.getId().toString(),
                "{\"title\":\"" + donation.getTitle() + "\",\"quantity\":" + donation.getQuantity() + "}",
                httpRequest
        );

        return DonationResponse.fromEntity(donation);
    }

    public Page<DonationResponse> getMyDonations(String firebaseUid, Pageable pageable) {
        User user = validateAndGetDonorUser(firebaseUid);
        return donorProfileService.findByUserId(user.getId())
                .map(donorProfile -> donationRepository.findByDonorId(donorProfile.getId(), pageable)
                        .map(DonationResponse::fromEntity))
                .orElse(Page.empty(pageable));
    }

    public DonationResponse getDonationById(String firebaseUid, UUID donationId) {
        User user = validateAndGetDonorUser(firebaseUid);
        DonorProfile donorProfile = getDonorProfileOrThrow(user);

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (!donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to view this donation.");
        }

        return DonationResponse.fromEntity(donation);
    }

    @Transactional
    public DonationResponse updateDonation(String firebaseUid,
                                           UUID donationId,
                                           UpdateDonationRequest request,
                                           HttpServletRequest httpRequest) {
        User user = validateAndGetDonorUser(firebaseUid);
        DonorProfile donorProfile = getDonorProfileOrThrow(user);

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (!donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this donation.");
        }

        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot update donation in status: " + donation.getStatus());
        }

        Instant newPrep = request.preparationDateTime() != null ? request.preparationDateTime() : donation.getPreparationDateTime();
        Instant newConsume = request.consumeBefore() != null ? request.consumeBefore() : donation.getConsumeBefore();
        Instant newPickupFrom = request.pickupAvailableFrom() != null ? request.pickupAvailableFrom() : donation.getPickupAvailableFrom();
        Instant newPickupUntil = request.pickupAvailableUntil() != null ? request.pickupAvailableUntil() : donation.getPickupAvailableUntil();

        validateTemporalConstraints(newPrep, newConsume, newPickupFrom, newPickupUntil);

        if (request.title() != null && !request.title().isBlank()) {
            donation.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            donation.setDescription(request.description().trim());
        }
        if (request.foodCategory() != null) {
            donation.setFoodCategory(request.foodCategory());
        }
        if (request.quantity() != null) {
            if (request.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            donation.setQuantity(request.quantity());
        }
        if (request.quantityUnit() != null && !request.quantityUnit().isBlank()) {
            donation.setQuantityUnit(request.quantityUnit().trim());
        }
        donation.setPreparationDateTime(newPrep);
        donation.setConsumeBefore(newConsume);
        donation.setPickupAvailableFrom(newPickupFrom);
        donation.setPickupAvailableUntil(newPickupUntil);

        if (request.pickupAddress() != null && !request.pickupAddress().isBlank()) {
            donation.setPickupAddress(request.pickupAddress().trim());
        }

        if (request.latitude() != null || request.longitude() != null) {
            Double newLat = request.latitude() != null ? request.latitude() : donation.getLatitude();
            Double newLng = request.longitude() != null ? request.longitude() : donation.getLongitude();
            validateCoordinates(newLat, newLng);
            donation.setLatitude(newLat);
            donation.setLongitude(newLng);
            donation.setLocation(GeometryUtils.createPoint(newLng, newLat));
        }

        donation = donationRepository.save(donation);

        auditService.logEvent(
                user.getId().toString(),
                "DONOR",
                "DONATION_UPDATED",
                "Donation",
                donation.getId().toString(),
                "{\"title\":\"" + donation.getTitle() + "\"}",
                httpRequest
        );

        return DonationResponse.fromEntity(donation);
    }

    @Transactional
    public DonationResponse cancelDonation(String firebaseUid,
                                           UUID donationId,
                                           HttpServletRequest httpRequest) {
        User user = validateAndGetDonorUser(firebaseUid);
        DonorProfile donorProfile = getDonorProfileOrThrow(user);

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));

        if (!donation.getDonor().getId().equals(donorProfile.getId())) {
            throw new AccessDeniedException("You do not have permission to cancel this donation.");
        }

        if (donation.getStatus() == DonationStatus.COMPLETED) {
            throw new IllegalStateException("Completed donation cannot be cancelled.");
        }

        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot cancel donation in status: " + donation.getStatus());
        }

        donation.setStatus(DonationStatus.CANCELLED);
        donation = donationRepository.save(donation);

        auditService.logEvent(
                user.getId().toString(),
                "DONOR",
                "DONATION_CANCELLED",
                "Donation",
                donation.getId().toString(),
                "{\"reason\":\"Cancelled by donor\"}",
                httpRequest
        );

        return DonationResponse.fromEntity(donation);
    }

    private User validateAndGetDonorUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new AccessDeniedException("User not found for authenticated account."));

        if (user.getRole() != UserRole.DONOR) {
            throw new AccessDeniedException("User does not have DONOR role.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccessDeniedException("Account is suspended.");
        }

        return user;
    }

    private DonorProfile getDonorProfileOrThrow(User user) {
        return donorProfileService.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Donor profile not found for user."));
    }

    private void validateTemporalConstraints(Instant prep,
                                             Instant consume,
                                             Instant pickupFrom,
                                             Instant pickupUntil) {
        if (prep == null || consume == null || pickupFrom == null || pickupUntil == null) {
            throw new IllegalArgumentException("All preparation, consumption, and pickup timestamps are required.");
        }

        if (!consume.isAfter(prep)) {
            throw new IllegalArgumentException("Consume-before time must be strictly after preparation time.");
        }

        if (!pickupUntil.isAfter(pickupFrom)) {
            throw new IllegalArgumentException("Pickup window end time must be after pickup start time.");
        }

        if (pickupFrom.isBefore(prep.minusSeconds(60))) {
            throw new IllegalArgumentException("Pickup window cannot start before food preparation time.");
        }

        if (!consume.isAfter(pickupFrom)) {
            throw new IllegalArgumentException("Consume-before time must be after pickup start time.");
        }

        if (pickupUntil.isAfter(consume)) {
            throw new IllegalArgumentException("Pickup window cannot extend beyond food consume-before deadline.");
        }

        if (consume.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Consume-before time cannot be in the past.");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
    }
}
