package com.mealshare.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    boolean existsByPickupIdAndRaterUserId(UUID pickupId, UUID raterUserId);

    Optional<Rating> findByPickupIdAndRaterUserId(UUID pickupId, UUID raterUserId);

    List<Rating> findByPickupId(UUID pickupId);

    Page<Rating> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Rating> findByRatedUserIdOrderByCreatedAtDesc(UUID ratedUserId, Pageable pageable);

    Page<Rating> findByRaterUserIdOrderByCreatedAtDesc(UUID raterUserId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.ratedUser.id = :userId")
    Double calculateAverageRatingForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.ratedUser.id = :userId")
    Long countRatingsForUser(@Param("userId") UUID userId);
}
