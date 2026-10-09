package com.mealshare.pickup;

import com.mealshare.receiver.ReceiverOrganization;
import com.mealshare.receiver.request.DonationRequest;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pickups")
public class Pickup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_request_id", unique = true, nullable = false)
    private DonationRequest donationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private ReceiverOrganization receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickupStatus status = PickupStatus.ASSIGNED;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant otpExpiresAt;

    @Column(name = "otp_attempts", nullable = false)
    private Integer otpAttempts = 0;

    @Column(name = "otp_verified_at")
    private Instant otpVerifiedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "started_pickup_at")
    private Instant startedPickupAt;

    @Column(name = "arrived_at_pickup_at")
    private Instant arrivedAtPickupAt;

    @Column(name = "food_inspected_at")
    private Instant foodInspectedAt;

    @Column(name = "inspection_notes")
    private String inspectionNotes;

    @Column(name = "inspection_rejected")
    private Boolean inspectionRejected = false;

    @Column(name = "pickup_photo_url")
    private String pickupPhotoUrl;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "started_delivery_at")
    private Instant startedDeliveryAt;

    @Column(name = "arrived_at_delivery_at")
    private Instant arrivedAtDeliveryAt;

    @Column(name = "delivery_photo_url")
    private String deliveryPhotoUrl;

    @Column(name = "delivery_latitude")
    private Double deliveryLatitude;

    @Column(name = "delivery_longitude")
    private Double deliveryLongitude;

    @Column(name = "delivered_quantity")
    private Double deliveredQuantity;

    @Column(name = "quantity_discrepancy_reason")
    private String quantityDiscrepancyReason;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    @Column(name = "tracking_active", nullable = false)
    private Boolean trackingActive = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Pickup() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DonationRequest getDonationRequest() { return donationRequest; }
    public void setDonationRequest(DonationRequest donationRequest) { this.donationRequest = donationRequest; }

    public ReceiverOrganization getReceiver() { return receiver; }
    public void setReceiver(ReceiverOrganization receiver) { this.receiver = receiver; }

    public PickupStatus getStatus() { return status; }
    public void setStatus(PickupStatus status) { this.status = status; }

    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public Instant getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(Instant otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }

    public Integer getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(Integer otpAttempts) { this.otpAttempts = otpAttempts; }

    public Instant getOtpVerifiedAt() { return otpVerifiedAt; }
    public void setOtpVerifiedAt(Instant otpVerifiedAt) { this.otpVerifiedAt = otpVerifiedAt; }

    public Instant getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(Instant pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getStartedPickupAt() { return startedPickupAt; }
    public void setStartedPickupAt(Instant startedPickupAt) { this.startedPickupAt = startedPickupAt; }

    public Instant getArrivedAtPickupAt() { return arrivedAtPickupAt; }
    public void setArrivedAtPickupAt(Instant arrivedAtPickupAt) { this.arrivedAtPickupAt = arrivedAtPickupAt; }

    public Instant getFoodInspectedAt() { return foodInspectedAt; }
    public void setFoodInspectedAt(Instant foodInspectedAt) { this.foodInspectedAt = foodInspectedAt; }

    public String getInspectionNotes() { return inspectionNotes; }
    public void setInspectionNotes(String inspectionNotes) { this.inspectionNotes = inspectionNotes; }

    public Boolean getInspectionRejected() { return inspectionRejected; }
    public void setInspectionRejected(Boolean inspectionRejected) { this.inspectionRejected = inspectionRejected; }

    public String getPickupPhotoUrl() { return pickupPhotoUrl; }
    public void setPickupPhotoUrl(String pickupPhotoUrl) { this.pickupPhotoUrl = pickupPhotoUrl; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public Instant getStartedDeliveryAt() { return startedDeliveryAt; }
    public void setStartedDeliveryAt(Instant startedDeliveryAt) { this.startedDeliveryAt = startedDeliveryAt; }

    public Instant getArrivedAtDeliveryAt() { return arrivedAtDeliveryAt; }
    public void setArrivedAtDeliveryAt(Instant arrivedAtDeliveryAt) { this.arrivedAtDeliveryAt = arrivedAtDeliveryAt; }

    public String getDeliveryPhotoUrl() { return deliveryPhotoUrl; }
    public void setDeliveryPhotoUrl(String deliveryPhotoUrl) { this.deliveryPhotoUrl = deliveryPhotoUrl; }

    public Double getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(Double deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }

    public Double getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(Double deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }

    public Double getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(Double deliveredQuantity) { this.deliveredQuantity = deliveredQuantity; }

    public String getQuantityDiscrepancyReason() { return quantityDiscrepancyReason; }
    public void setQuantityDiscrepancyReason(String quantityDiscrepancyReason) { this.quantityDiscrepancyReason = quantityDiscrepancyReason; }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(Instant locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public Boolean getTrackingActive() { return trackingActive; }
    public void setTrackingActive(Boolean trackingActive) { this.trackingActive = trackingActive; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
