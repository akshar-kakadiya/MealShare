package com.mealshare.pickup;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PickupRepository extends JpaRepository<Pickup, UUID> {

    Optional<Pickup> findByDonationRequestId(UUID donationRequestId);

    boolean existsByDonationRequestId(UUID donationRequestId);

    Page<Pickup> findByReceiverIdOrderByScheduledAtDesc(UUID receiverId, Pageable pageable);

    @Query("SELECT p FROM Pickup p WHERE p.donationRequest.donation.donor.id = :donorId ORDER BY p.scheduledAt DESC")
    Page<Pickup> findByDonorIdOrderByScheduledAtDesc(@Param("donorId") UUID donorId, Pageable pageable);

    Page<Pickup> findByStatusOrderByScheduledAtDesc(PickupStatus status, Pageable pageable);

    Page<Pickup> findAllByOrderByScheduledAtDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pickup p WHERE p.id = :id")
    Optional<Pickup> findByIdWithLock(@Param("id") UUID id);

    @Query("SELECT p FROM Pickup p WHERE p.donationRequest.donation.id = :donationId")
    List<Pickup> findByDonationId(@Param("donationId") UUID donationId);

    long countByStatus(PickupStatus status);

    @Query("SELECT p FROM Pickup p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:receiverId IS NULL OR p.receiver.id = :receiverId) AND " +
           "(:search IS NULL OR LOWER(p.donationRequest.donation.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(p.receiver.orgName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Pickup> searchPickups(
            @Param("status") PickupStatus status,
            @Param("receiverId") UUID receiverId,
            @Param("search") String search,
            Pageable pageable
    );
}
