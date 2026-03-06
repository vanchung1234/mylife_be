package com.mylife.modules.user.entity;

import com.mylife.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Refresh token dùng để lấy access token mới.
 * Mỗi user có thể có nhiều refresh token (cho nhiều thiết bị).
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 500)
    private String token; // lưu token đã hash (hoặc mã gốc, tùy thiết kế)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean revoked = false; // đã thu hồi chưa (logout)

    @Column(name = "device_info", length = 500)
    private String deviceInfo; // thông tin thiết bị (user agent)

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Kiểm tra token đã hết hạn chưa.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}