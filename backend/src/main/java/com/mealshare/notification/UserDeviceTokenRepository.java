package com.mealshare.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

    List<UserDeviceToken> findByUserId(UUID userId);

    Optional<UserDeviceToken> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    Optional<UserDeviceToken> findByFcmToken(String fcmToken);

    void deleteByUserIdAndFcmToken(UUID userId, String fcmToken);

    void deleteByFcmToken(String fcmToken);
}
