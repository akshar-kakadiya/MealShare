package com.mealshare.donor;

import com.mealshare.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donor_profiles")
public class DonorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "donor_type", nullable = false)
    private String donorType;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "address_text", nullable = false)
    private String addressText;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "policy_accepted", nullable = false)
    private Boolean policyAccepted = false;

    @Column(name = "rating_average", nullable = false)
    private Double ratingAverage = 5.0;

    @Column(name = "completed_donations_count", nullable = false)
    private Integer completedDonationsCount = 0;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "event_info")
    private String eventInfo;

    @Column(name = "photos")
    private String photos;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false)
    private com.mealshare.user.ProfileStatus profileStatus = com.mealshare.user.ProfileStatus.INCOMPLETE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private com.mealshare.user.VerificationStatus verificationStatus = com.mealshare.user.VerificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "action_required_notes")
    private String actionRequiredNotes;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public DonorProfile() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDonorType() { return donorType; }
    public void setDonorType(String donorType) { this.donorType = donorType; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Boolean getPolicyAccepted() { return policyAccepted; }
    public void setPolicyAccepted(Boolean policyAccepted) { this.policyAccepted = policyAccepted; }
    public Double getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(Double ratingAverage) { this.ratingAverage = ratingAverage; }
    public Integer getCompletedDonationsCount() { return completedDonationsCount; }
    public void setCompletedDonationsCount(Integer count) { this.completedDonationsCount = count; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getEventInfo() { return eventInfo; }
    public void setEventInfo(String eventInfo) { this.eventInfo = eventInfo; }

    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }

    public com.mealshare.user.ProfileStatus getProfileStatus() { return profileStatus; }
    public void setProfileStatus(com.mealshare.user.ProfileStatus profileStatus) { this.profileStatus = profileStatus; }

    public com.mealshare.user.VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(com.mealshare.user.VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getActionRequiredNotes() { return actionRequiredNotes; }
    public void setActionRequiredNotes(String actionRequiredNotes) { this.actionRequiredNotes = actionRequiredNotes; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
