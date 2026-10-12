package com.mealshare.receiver;

import com.mealshare.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receiver_organizations")
public class ReceiverOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "org_type", nullable = false)
    private String orgType;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "address_text", nullable = false)
    private String addressText;

    @Column(name = "service_latitude", nullable = false)
    private Double serviceLatitude;

    @Column(name = "service_longitude", nullable = false)
    private Double serviceLongitude;

    @Column(name = "service_radius_meters", nullable = false)
    private Double serviceRadiusMeters = 10000.0;

    @Column(name = "verification_status", nullable = false)
    private String verificationStatus = "PENDING";

    @Column(name = "beneficiary_capacity", nullable = false)
    private Integer beneficiaryCapacity = 50;

    @Column(name = "rating_average", nullable = false)
    private Double ratingAverage = 5.0;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "description")
    private String description;

    @Column(name = "photos")
    private String photos;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false)
    private com.mealshare.user.ProfileStatus profileStatus = com.mealshare.user.ProfileStatus.INCOMPLETE;

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

    public ReceiverOrganization() {}

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getOrgType() { return orgType; }
    public void setOrgType(String orgType) { this.orgType = orgType; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getAddressText() { return addressText; }
    public void setAddressText(String addressText) { this.addressText = addressText; }
    public Double getServiceLatitude() { return serviceLatitude; }
    public void setServiceLatitude(Double serviceLatitude) { this.serviceLatitude = serviceLatitude; }
    public Double getServiceLongitude() { return serviceLongitude; }
    public void setServiceLongitude(Double serviceLongitude) { this.serviceLongitude = serviceLongitude; }
    public Double getServiceRadiusMeters() { return serviceRadiusMeters; }
    public void setServiceRadiusMeters(Double serviceRadiusMeters) { this.serviceRadiusMeters = serviceRadiusMeters; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public Integer getBeneficiaryCapacity() { return beneficiaryCapacity; }
    public void setBeneficiaryCapacity(Integer beneficiaryCapacity) { this.beneficiaryCapacity = beneficiaryCapacity; }
    public Double getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(Double ratingAverage) { this.ratingAverage = ratingAverage; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotos() { return photos; }
    public void setPhotos(String photos) { this.photos = photos; }

    public com.mealshare.user.ProfileStatus getProfileStatus() { return profileStatus; }
    public void setProfileStatus(com.mealshare.user.ProfileStatus profileStatus) { this.profileStatus = profileStatus; }

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
