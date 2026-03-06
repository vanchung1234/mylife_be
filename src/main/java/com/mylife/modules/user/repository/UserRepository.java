package com.mylife.modules.user.repository;

import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

        /**
         * Tìm user theo email (phân biệt hoa thường? không, email không phân biệt hoa
         * thường).
         */
        Optional<User> findByEmailIgnoreCase(String email);

        /**
         * Kiểm tra email đã tồn tại chưa.
         */
        boolean existsByEmailIgnoreCase(String email);


        /**
         * Tìm user theo email và provider (dùng cho OAuth2).
         */
        Optional<User> findByEmailAndAuthProvider(String email,
                        com.mylife.modules.user.entity.enums.AuthProvider authProvider);

        /**
         * Tìm user theo providerId (dùng cho OAuth2).
         */
        Optional<User> findByProviderId(String providerId);

        /**
         * Tìm tất cả user bị khóa có thời gian lock quá hạn để mở khóa tự động.
         */
        @Query("SELECT u FROM User u WHERE u.status = :status AND u.lockTime < :unlockTime")
        java.util.List<User> findLockedUsersBefore(@Param("status") UserStatus status,
                        @Param("unlockTime") LocalDateTime unlockTime);

        /**
         * Soft delete user (cập nhật deleted flag và status).
         */
        @Modifying
        @Query("UPDATE User u SET u.deleted = true, u.status = :status WHERE u.id = :userId")
        void softDelete(@Param("userId") UUID userId, @Param("status") UserStatus status);
}
