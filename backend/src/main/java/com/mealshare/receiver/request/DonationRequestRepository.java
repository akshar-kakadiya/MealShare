package com.mealshare.receiver.request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationRequestRepository extends JpaRepository<DonationRequest, UUID> {

    Page<DonationRequest> findByReceiverOrganizationIdOrderByRequestedAtDesc(UUID receiverOrgId, Pageable pageable);

    List<DonationRequest> findByReceiverOrganizationIdAndStatus(UUID receiverOrgId, DonationRequestStatus status);

    Optional<DonationRequest> findByIdAndReceiverOrganizationId(UUID id, UUID receiverOrgId);

    List<DonationRequest> findByDonationIdOrderByRequestedAtAscIdAsc(UUID donationId);

    Page<DonationRequest> findByDonationIdOrderByRequestedAtAscIdAsc(UUID donationId, Pageable pageable);

    List<DonationRequest> findByDonationIdAndStatusOrderByRequestedAtAscIdAsc(UUID donationId, DonationRequestStatus status);

    boolean existsByDonationIdAndReceiverOrganizationIdAndStatusIn(
            UUID donationId,
            UUID receiverOrgId,
            Collection<DonationRequestStatus> statuses
    );

    long countByStatus(DonationRequestStatus status);

    List<DonationRequest> findByDonationId(UUID donationId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM DonationRequest r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:donationId IS NULL OR r.donation.id = :donationId) AND " +
           "(:receiverOrgId IS NULL OR r.receiverOrganization.id = :receiverOrgId) AND " +
           "(:donorId IS NULL OR r.donation.donor.id = :donorId) AND " +
           "(:search IS NULL OR LOWER(r.donation.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(r.receiverOrganization.orgName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<DonationRequest> searchRequests(
            @org.springframework.data.repository.query.Param("status") DonationRequestStatus status,
            @org.springframework.data.repository.query.Param("donationId") UUID donationId,
            @org.springframework.data.repository.query.Param("receiverOrgId") UUID receiverOrgId,
            @org.springframework.data.repository.query.Param("donorId") UUID donorId,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable
    );
}
