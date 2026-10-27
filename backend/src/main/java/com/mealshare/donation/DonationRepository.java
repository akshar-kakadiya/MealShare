package com.mealshare.donation;

import com.mealshare.donation.projection.NearbyDonationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByDonorId(UUID donorId, Pageable pageable);

    Optional<Donation> findByIdAndDonorId(UUID id, UUID donorId);

    boolean existsByIdAndDonorId(UUID id, UUID donorId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.id = :id")
    Optional<Donation> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT d FROM Donation d WHERE d.status IN :candidateStatuses AND (d.pickupAvailableUntil < :now OR d.consumeBefore < :now)")
    java.util.List<Donation> findOverdueCandidateDonations(
            @Param("candidateStatuses") java.util.Collection<DonationStatus> candidateStatuses,
            @Param("now") Instant now
    );

    @Query(value = """
        SELECT d.id AS id,
               d.title AS title,
               d.description AS description,
               d.food_category AS foodCategory,
               d.quantity AS quantity,
               d.quantity_unit AS quantityUnit,
               d.preparation_datetime AS preparationDateTime,
               d.consume_before AS consumeBefore,
               d.pickup_available_from AS pickupAvailableFrom,
               d.pickup_available_until AS pickupAvailableUntil,
               d.pickup_address AS pickupAddress,
               d.latitude AS latitude,
               d.longitude AS longitude,
               d.food_safety_declared AS foodSafetyDeclared,
               d.status AS status,
               d.created_at AS createdAt,
               (ST_Distance(d.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0) AS distanceKm,
               ST_Distance(d.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distanceMeters
        FROM donations d
        WHERE d.status = 'AVAILABLE'
          AND d.consume_before > :now
          AND d.pickup_available_until > :now
          AND ST_DWithin(d.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
          AND (:foodCategory IS NULL OR d.food_category = :foodCategory)
        ORDER BY distanceKm ASC, d.consume_before ASC
        """,
        countQuery = """
        SELECT count(d.id)
        FROM donations d
        WHERE d.status = 'AVAILABLE'
          AND d.consume_before > :now
          AND d.pickup_available_until > :now
          AND ST_DWithin(d.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
          AND (:foodCategory IS NULL OR d.food_category = :foodCategory)
        """,
        nativeQuery = true)
    Page<NearbyDonationProjection> findNearbyAvailable(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusMeters") Double radiusMeters,
            @Param("foodCategory") String foodCategory,
            @Param("now") Instant now,
            Pageable pageable);

    long countByStatus(DonationStatus status);

    @Query("SELECT COALESCE(SUM(d.quantity), 0.0) FROM Donation d")
    Double sumTotalQuantity();

    @Query("SELECT COALESCE(SUM(d.allocatedQuantity), 0.0) FROM Donation d")
    Double sumTotalAllocatedQuantity();

    @Query("SELECT COALESCE(SUM(d.quantity), 0.0) FROM Donation d WHERE d.status = com.mealshare.donation.DonationStatus.COMPLETED")
    Double sumCompletedQuantity();

    java.util.List<Donation> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT d FROM Donation d WHERE " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:category IS NULL OR d.foodCategory = :category) AND " +
           "(:donorId IS NULL OR d.donor.id = :donorId) AND " +
           "(:search IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(d.donor.entityName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Donation> searchDonations(
            @Param("status") DonationStatus status,
            @Param("category") FoodCategory category,
            @Param("donorId") UUID donorId,
            @Param("search") String search,
            Pageable pageable
    );
}
