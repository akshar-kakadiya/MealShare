package com.mealshare.donation;

import com.mealshare.donor.DonorProfile;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private DonorProfile donor;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_category", nullable = false)
    private FoodCategory foodCategory;

    @Column(nullable = false)
    private Double quantity;

    @Column(name = "allocated_quantity", nullable = false)
    private Double allocatedQuantity = 0.0;

    @Column(name = "quantity_unit", nullable = false)
    private String quantityUnit;

    @Column(name = "preparation_datetime", nullable = false)
    private Instant preparationDateTime;

    @Column(name = "consume_before", nullable = false)
    private Instant consumeBefore;

    @Column(name = "pickup_available_from", nullable = false)
    private Instant pickupAvailableFrom;

    @Column(name = "pickup_available_until", nullable = false)
    private Instant pickupAvailableUntil;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location;

    @Column(name = "food_safety_declared", nullable = false)
    private Boolean foodSafetyDeclared = false;

    @Column(name = "food_safety_declaration_text", columnDefinition = "TEXT")
    private String foodSafetyDeclarationText;

    @Column(name = "food_safety_declared_at")
    private Instant foodSafetyDeclaredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status = DonationStatus.AVAILABLE;

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

    public Donation() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DonorProfile getDonor() { return donor; }
    public void setDonor(DonorProfile donor) { this.donor = donor; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public FoodCategory getFoodCategory() { return foodCategory; }
    public void setFoodCategory(FoodCategory foodCategory) { this.foodCategory = foodCategory; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Double allocatedQuantity) {
        if (allocatedQuantity == null || allocatedQuantity < 0.0) {
            throw new IllegalArgumentException("Allocated quantity cannot be null or negative.");
        }
        if (this.quantity != null && allocatedQuantity > this.quantity) {
            throw new IllegalArgumentException("Allocated quantity cannot exceed total quantity.");
        }
        this.allocatedQuantity = allocatedQuantity;
    }

    public Double getRemainingQuantity() {
        if (this.quantity == null) {
            return 0.0;
        }
        double allocated = this.allocatedQuantity != null ? this.allocatedQuantity : 0.0;
        return Math.max(0.0, this.quantity - allocated);
    }

    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }

    public Instant getPreparationDateTime() { return preparationDateTime; }
    public void setPreparationDateTime(Instant preparationDateTime) { this.preparationDateTime = preparationDateTime; }

    public Instant getConsumeBefore() { return consumeBefore; }
    public void setConsumeBefore(Instant consumeBefore) { this.consumeBefore = consumeBefore; }

    public Instant getPickupAvailableFrom() { return pickupAvailableFrom; }
    public void setPickupAvailableFrom(Instant pickupAvailableFrom) { this.pickupAvailableFrom = pickupAvailableFrom; }

    public Instant getPickupAvailableUntil() { return pickupAvailableUntil; }
    public void setPickupAvailableUntil(Instant pickupAvailableUntil) { this.pickupAvailableUntil = pickupAvailableUntil; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public Boolean getFoodSafetyDeclared() { return foodSafetyDeclared; }
    public void setFoodSafetyDeclared(Boolean foodSafetyDeclared) { this.foodSafetyDeclared = foodSafetyDeclared; }

    public String getFoodSafetyDeclarationText() { return foodSafetyDeclarationText; }
    public void setFoodSafetyDeclarationText(String foodSafetyDeclarationText) { this.foodSafetyDeclarationText = foodSafetyDeclarationText; }

    public Instant getFoodSafetyDeclaredAt() { return foodSafetyDeclaredAt; }
    public void setFoodSafetyDeclaredAt(Instant foodSafetyDeclaredAt) { this.foodSafetyDeclaredAt = foodSafetyDeclaredAt; }

    public DonationStatus getStatus() { return status; }
    public void setStatus(DonationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
