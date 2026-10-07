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

    @Column(name = "registration_number", nullable = false)
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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
