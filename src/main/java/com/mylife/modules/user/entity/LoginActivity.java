package com.mylife.modules.user.entity;

import com.mylife.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "login_activities")
public class LoginActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "failure_reason")
    private String failureReason; // lý do thất bại (nếu success=false)
}