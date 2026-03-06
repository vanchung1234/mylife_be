package com.mylife.modules.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String defaultCurrency;
    private String timezone;
    private String language;
    private LocalTime resetTime;
    private Boolean notificationEnabled;
    private Boolean twoFactorEnabled;
    private String status;
    private String authProvider;
    private Boolean emailVerified;
}