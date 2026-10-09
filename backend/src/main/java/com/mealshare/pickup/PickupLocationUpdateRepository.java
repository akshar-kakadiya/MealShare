package com.mealshare.pickup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PickupLocationUpdateRepository extends JpaRepository<PickupLocationUpdate, Long> {

    List<PickupLocationUpdate> findByPickupIdOrderByRecordedAtDesc(UUID pickupId);

    List<PickupLocationUpdate> findTop20ByPickupIdOrderByRecordedAtDesc(UUID pickupId);
}
