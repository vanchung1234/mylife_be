package com.mylife.modules.user.repository;

import com.mylife.modules.user.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, UUID> {

    Optional<UserDeviceToken> findByDeviceToken(String deviceToken);

    List<UserDeviceToken> findByUserIdAndNotificationEnabledTrue(UUID userId);

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}

