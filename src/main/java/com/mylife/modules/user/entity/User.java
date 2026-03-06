package com.mylife.modules.user.entity;

import com.mylife.common.base.BaseEntity;
import com.mylife.modules.user.entity.enums.AuthProvider;
import com.mylife.modules.user.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder; // nếu dùng builder pattern
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity đại diện cho người dùng trong hệ thống.
 * Kế thừa BaseEntity để có id, createdAt, updatedAt.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// @SuperBuilder // dùng nếu muốn builder cho class có kế thừa
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // có thể null nếu đăng nhập qua Google

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency = "VND"; // mặc định VND

    @Column(length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(length = 10)
    private String language = "vi";

    @Column(name = "reset_time")
    private LocalTime resetTime = LocalTime.of(4, 0); // 04:00 AM

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret; // secret cho Google Authenticator

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId; // ID từ Google (sub)

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // soft delete flag
    @Column(name = "deleted")
    private Boolean deleted = false;

    // Mối quan hệ với refresh tokens (một user có nhiều refresh token)
    // Được ánh xạ ngược từ RefreshToken entity
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    // Mối quan hệ với login activities
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LoginActivity> loginActivities = new HashSet<>();

    /**
     * Tăng số lần đăng nhập thất bại.
     */
    public void incrementFailedAttempts() {
        this.failedAttempts = (this.failedAttempts == null ? 1 : this.failedAttempts + 1);
    }

    /**
     * Reset số lần thất bại khi đăng nhập thành công.
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockTime = null;
    }

    /**
     * Khóa tài khoản.
     */
    public void lock() {
        this.status = UserStatus.LOCKED;
        this.lockTime = LocalDateTime.now();
    }

    /**
     * Mở khóa tài khoản.
     */
    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.lockTime = null;
        this.failedAttempts = 0;
    }
}