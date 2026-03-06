package com.mylife.modules.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private UUID userId;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String accessToken;
    private String refreshToken;
    private long expiresIn; // thời gian sống của access token (giây)
}