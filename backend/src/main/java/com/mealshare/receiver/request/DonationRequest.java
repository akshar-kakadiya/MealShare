package com.mealshare.receiver.request;

import com.mealshare.donation.Donation;
import com.mealshare.receiver.ReceiverOrganization;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donation_requests")
public class DonationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_org_id", nullable = false)
    private ReceiverOrganization receiverOrganization;

    @Column(name = "requested_quantity", nullable = false)
    private Double requestedQuantity;

    @Column(name = "allocated_quantity", nullable = false)
    private Double allocatedQuantity = 0.0;

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String requestReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationRequestStatus status = DonationRequestStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (allocatedQuantity == null) {
            allocatedQuantity = 0.0;
        }
        requestedAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public DonationRequest() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Donation getDonation() { return donation; }
    public void setDonation(Donation donation) { this.donation = donation; }

    public ReceiverOrganization getReceiverOrganization() { return receiverOrganization; }
    public void setReceiverOrganization(ReceiverOrganization receiverOrganization) { this.receiverOrganization = receiverOrganization; }

    public Double getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Double requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public Double getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Double allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }

    public DonationRequestStatus getStatus() { return status; }
    public void setStatus(DonationRequestStatus status) { this.status = status; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
