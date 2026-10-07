package com.mealshare.receiver;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiverOrganizationRepository extends JpaRepository<ReceiverOrganization, UUID> {

    Optional<ReceiverOrganization> findByUserId(UUID userId);

    Optional<ReceiverOrganization> findByUserFirebaseUid(String firebaseUid);

    boolean existsByUserId(UUID userId);

    long countByVerificationStatus(String verificationStatus);

    @Query("SELECT r FROM ReceiverOrganization r WHERE " +
           "(:verificationStatus IS NULL OR r.verificationStatus = :verificationStatus) AND " +
           "(:search IS NULL OR LOWER(r.orgName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(r.registrationNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<ReceiverOrganization> searchReceiverOrganizations(
            @Param("verificationStatus") String verificationStatus,
            @Param("search") String search,
            Pageable pageable
    );
}
