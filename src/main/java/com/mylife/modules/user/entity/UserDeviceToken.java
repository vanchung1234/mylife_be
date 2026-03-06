package com.mylife.modules.user.entity;

import com.mylife.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity lưu FCM device token của từng người dùng.
 * Mỗi thiết bị (mobile, tablet, web) sẽ có một token riêng.
 */
@Getter
@Setter
@Entity
@Table(name = "user_device_tokens")
public class UserDeviceToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * FCM device token (unique cho mỗi device).
     */
    @Column(name = "device_token", nullable = false, unique = true, length = 255)
    private String deviceToken;

    /**
     * Loại thiết bị (ANDROID, IOS, WEB... - tuỳ client gửi lên).
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;

    /**
     * Tên thiết bị hoặc mô tả (tuỳ chọn).
     */
    @Column(name = "device_name", length = 255)
    private String deviceName;

    /**
     * Cho phép nhận notification trên thiết bị này hay không.
     */
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    /**
     * Thời điểm cuối cùng token này được sử dụng/đăng ký.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}

