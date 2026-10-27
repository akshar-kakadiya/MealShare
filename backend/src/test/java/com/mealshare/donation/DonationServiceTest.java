package com.mealshare.donation;

import com.mealshare.audit.AuditService;
import com.mealshare.donation.dto.CreateDonationRequest;
import com.mealshare.donation.dto.DonationResponse;
import com.mealshare.donation.dto.UpdateDonationRequest;
import com.mealshare.donor.DonorProfile;
import com.mealshare.donor.DonorProfileService;
import com.mealshare.user.User;
import com.mealshare.user.UserRepository;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonorProfileService donorProfileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DonationService donationService;

    private User donorUser;
    private DonorProfile donorProfile;
    private User anotherDonorUser;
    private DonorProfile anotherDonorProfile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        donorUser = new User();
        donorUser.setId(UUID.randomUUID());
        donorUser.setFirebaseUid("donor-firebase-uid");
        donorUser.setName("Green Kitchen");
        donorUser.setEmail("green@kitchen.org");
        donorUser.setRole(UserRole.DONOR);
        donorUser.setStatus(UserStatus.ACTIVE);

        donorProfile = new DonorProfile();
        donorProfile.setId(UUID.randomUUID());
        donorProfile.setUser(donorUser);
        donorProfile.setEntityName("Green Kitchen");
        donorProfile.setDonorType("RESTAURANT");
        donorProfile.setAddressText("123 MG Road");
        donorProfile.setLatitude(12.9716);
        donorProfile.setLongitude(77.5946);

        anotherDonorUser = new User();
        anotherDonorUser.setId(UUID.randomUUID());
        anotherDonorUser.setFirebaseUid("another-firebase-uid");
        anotherDonorUser.setName("Another Donor");
        anotherDonorUser.setRole(UserRole.DONOR);
        anotherDonorUser.setStatus(UserStatus.ACTIVE);

        anotherDonorProfile = new DonorProfile();
        anotherDonorProfile.setId(UUID.randomUUID());
        anotherDonorProfile.setUser(anotherDonorUser);
        anotherDonorProfile.setEntityName("Another Org");

        when(userRepository.findByFirebaseUid("donor-firebase-uid")).thenReturn(Optional.of(donorUser));
        when(userRepository.findByFirebaseUid("another-firebase-uid")).thenReturn(Optional.of(anotherDonorUser));
        when(donorProfileService.findByUserId(donorUser.getId())).thenReturn(Optional.of(donorProfile));
        when(donorProfileService.findByUserId(anotherDonorUser.getId())).thenReturn(Optional.of(anotherDonorProfile));
        when(donorProfileService.getOrCreateProfileForUser(any(), any(), any(), any())).thenReturn(donorProfile);
    }

    private CreateDonationRequest createValidRequest() {
        Instant now = Instant.now();
        Instant prep = now.minus(1, ChronoUnit.HOURS);
        Instant pickupFrom = now.plus(10, ChronoUnit.MINUTES);
        Instant pickupUntil = now.plus(3, ChronoUnit.HOURS);
        Instant consumeBefore = now.plus(5, ChronoUnit.HOURS);

        return new CreateDonationRequest(
                "Fresh Meals",
                "50 packaged vegetarian rice meals",
                FoodCategory.PREPARED_MEALS,
                50.0,
                "MEALS",
                prep,
                consumeBefore,
                pickupFrom,
                pickupUntil,
                "123 MG Road, Bengaluru",
                12.9716,
                77.5946,
                true,
                "Food is safe and hygienic"
        );
    }

    @Test
    void test1_donorCanCreateDonation_startsAsAvailable_withSRID4326_andAudit() {
        CreateDonationRequest request = createValidRequest();

        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> {
            Donation d = invocation.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DonationResponse response = donationService.createDonation("donor-firebase-uid", request, null);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Fresh Meals", response.title());
        assertEquals(DonationStatus.AVAILABLE, response.status());
        assertEquals(50.0, response.quantity());
        assertTrue(response.foodSafetyDeclared());

        // Verify PostGIS Point creation and SRID 4326
        ArgumentCaptor<Donation> captor = ArgumentCaptor.forClass(Donation.class);
        verify(donationRepository).save(captor.capture());
        Donation saved = captor.getValue();
        assertNotNull(saved.getLocation());
        assertEquals(4326, saved.getLocation().getSRID());
        assertEquals(77.5946, saved.getLocation().getX(), 0.0001); // Longitude = X
        assertEquals(12.9716, saved.getLocation().getY(), 0.0001); // Latitude = Y

        // Verify audit event
        verify(auditService).logEvent(
                eq(donorUser.getId().toString()),
                eq("DONOR"),
                eq("DONATION_CREATED"),
                eq("Donation"),
                eq(saved.getId().toString()),
                any(),
                any()
        );
    }

    @Test
    void test6_missingFoodSafetyDeclaration_rejected() {
        Instant now = Instant.now();
        CreateDonationRequest request = new CreateDonationRequest(
                "Meals", "desc", FoodCategory.PREPARED_MEALS, 10.0, "MEALS",
                now.minus(1, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS),
                now, now.plus(2, ChronoUnit.HOURS),
                "Address", 12.0, 77.0, false, null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation("donor-firebase-uid", request, null));
        assertTrue(ex.getMessage().contains("Food safety declaration"));
    }

    @Test
    void test8_invalidCoordinates_rejected() {
        Instant now = Instant.now();
        CreateDonationRequest request = new CreateDonationRequest(
                "Meals", "desc", FoodCategory.PREPARED_MEALS, 10.0, "MEALS",
                now.minus(1, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS),
                now, now.plus(2, ChronoUnit.HOURS),
                "Address", 95.0, 77.0, true, "signed"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation("donor-firebase-uid", request, null));
        assertTrue(ex.getMessage().contains("Latitude"));
    }

    @Test
    void test9_invalidConsumeBefore_beforePreparation_rejected() {
        Instant now = Instant.now();
        CreateDonationRequest request = new CreateDonationRequest(
                "Meals", "desc", FoodCategory.PREPARED_MEALS, 10.0, "MEALS",
                now.plus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS),
                now, now.plus(2, ChronoUnit.HOURS),
                "Address", 12.0, 77.0, true, "signed"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation("donor-firebase-uid", request, null));
        assertTrue(ex.getMessage().contains("strictly after"));
    }

    @Test
    void test10_consumeBefore_notAfterPickupUntil_rejected() {
        Instant now = Instant.now();
        Instant pickupUntil = now.plus(3, ChronoUnit.HOURS);
        // consumeBefore equal to pickupUntil must be rejected
        CreateDonationRequest requestEqual = new CreateDonationRequest(
                "Meals", "desc", FoodCategory.PREPARED_MEALS, 10.0, "MEALS",
                now.minus(1, ChronoUnit.HOURS), pickupUntil,
                now, pickupUntil,
                "Address", 12.0, 77.0, true, "signed"
        );

        IllegalArgumentException exEqual = assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation("donor-firebase-uid", requestEqual, null));
        assertTrue(exEqual.getMessage().contains("Food consumable-until deadline must be later than pickup window end time."));

        // consumeBefore before pickupUntil must also be rejected
        CreateDonationRequest requestBefore = new CreateDonationRequest(
                "Meals", "desc", FoodCategory.PREPARED_MEALS, 10.0, "MEALS",
                now.minus(1, ChronoUnit.HOURS), pickupUntil.minus(30, ChronoUnit.MINUTES),
                now, pickupUntil,
                "Address", 12.0, 77.0, true, "signed"
        );

        IllegalArgumentException exBefore = assertThrows(IllegalArgumentException.class,
                () -> donationService.createDonation("donor-firebase-uid", requestBefore, null));
        assertTrue(exBefore.getMessage().contains("Food consumable-until deadline must be later than pickup window end time."));
    }

    @Test
    void test13_donorSeesOnlyOwnDonations() {
        Donation d1 = new Donation();
        d1.setId(UUID.randomUUID());
        d1.setDonor(donorProfile);
        d1.setTitle("My Food");
        d1.setFoodCategory(FoodCategory.PREPARED_MEALS);
        d1.setQuantity(20.0);
        d1.setQuantityUnit("MEALS");
        d1.setStatus(DonationStatus.AVAILABLE);

        Page<Donation> page = new PageImpl<>(List.of(d1));
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(donationRepository.findByDonorId(donorProfile.getId(), pageable)).thenReturn(page);

        Page<DonationResponse> result = donationService.getMyDonations("donor-firebase-uid", pageable);
        assertEquals(1, result.getTotalElements());
        assertEquals("My Food", result.getContent().get(0).title());
    }

    @Test
    void test14_donorCannotAccessAnotherDonorsDonation() {
        UUID donationId = UUID.randomUUID();
        Donation otherDonation = new Donation();
        otherDonation.setId(donationId);
        otherDonation.setDonor(anotherDonorProfile); // Belongs to another donor

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(otherDonation));

        assertThrows(AccessDeniedException.class,
                () -> donationService.getDonationById("donor-firebase-uid", donationId));
    }

    @Test
    void test15_donorCannotUpdateAnotherDonorsDonation() {
        UUID donationId = UUID.randomUUID();
        Donation otherDonation = new Donation();
        otherDonation.setId(donationId);
        otherDonation.setDonor(anotherDonorProfile);
        otherDonation.setStatus(DonationStatus.AVAILABLE);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(otherDonation));

        UpdateDonationRequest updateRequest = new UpdateDonationRequest(
                "Hacked Title", null, null, null, null, null, null, null, null, null, null, null
        );

        assertThrows(AccessDeniedException.class,
                () -> donationService.updateDonation("donor-firebase-uid", donationId, updateRequest, null));
    }

    @Test
    void test16_donorCannotCancelAnotherDonorsDonation() {
        UUID donationId = UUID.randomUUID();
        Donation otherDonation = new Donation();
        otherDonation.setId(donationId);
        otherDonation.setDonor(anotherDonorProfile);
        otherDonation.setStatus(DonationStatus.AVAILABLE);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(otherDonation));

        assertThrows(AccessDeniedException.class,
                () -> donationService.cancelDonation("donor-firebase-uid", donationId, null));
    }

    @Test
    void test17_validCancellationWorks_andGeneratesAudit() {
        UUID donationId = UUID.randomUUID();
        Donation myDonation = new Donation();
        myDonation.setId(donationId);
        myDonation.setDonor(donorProfile);
        myDonation.setStatus(DonationStatus.AVAILABLE);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(myDonation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(i -> i.getArgument(0));

        DonationResponse response = donationService.cancelDonation("donor-firebase-uid", donationId, null);

        assertEquals(DonationStatus.CANCELLED, response.status());

        verify(auditService).logEvent(
                eq(donorUser.getId().toString()),
                eq("DONOR"),
                eq("DONATION_CANCELLED"),
                eq("Donation"),
                eq(donationId.toString()),
                any(),
                any()
        );
    }

    @Test
    void test18_completedDonationCannotBeCancelled() {
        UUID donationId = UUID.randomUUID();
        Donation myDonation = new Donation();
        myDonation.setId(donationId);
        myDonation.setDonor(donorProfile);
        myDonation.setStatus(DonationStatus.COMPLETED); // Already completed

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(myDonation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> donationService.cancelDonation("donor-firebase-uid", donationId, null));
        assertTrue(ex.getMessage().contains("Completed donation cannot be cancelled"));
    }

    @Test
    void test20_auditEventGeneratedForUpdate() {
        UUID donationId = UUID.randomUUID();
        Instant now = Instant.now();
        Donation myDonation = new Donation();
        myDonation.setId(donationId);
        myDonation.setDonor(donorProfile);
        myDonation.setTitle("Original Title");
        myDonation.setStatus(DonationStatus.AVAILABLE);
        myDonation.setPreparationDateTime(now.minus(1, ChronoUnit.HOURS));
        myDonation.setConsumeBefore(now.plus(4, ChronoUnit.HOURS));
        myDonation.setPickupAvailableFrom(now);
        myDonation.setPickupAvailableUntil(now.plus(2, ChronoUnit.HOURS));
        myDonation.setLatitude(12.9716);
        myDonation.setLongitude(77.5946);

        when(donationRepository.findById(donationId)).thenReturn(Optional.of(myDonation));
        when(donationRepository.save(any(Donation.class))).thenAnswer(i -> i.getArgument(0));

        UpdateDonationRequest updateRequest = new UpdateDonationRequest(
                "Updated Title", "New description", null, null, null, null, null, null, null, null, null, null
        );

        DonationResponse response = donationService.updateDonation("donor-firebase-uid", donationId, updateRequest, null);

        assertEquals("Updated Title", response.title());
        verify(auditService).logEvent(
                eq(donorUser.getId().toString()),
                eq("DONOR"),
                eq("DONATION_UPDATED"),
                eq("Donation"),
                eq(donationId.toString()),
                any(),
                any()
        );
    }
}
