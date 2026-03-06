package com.mylife.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO để client đăng ký (hoặc cập nhật) FCM device token.
 */
@Getter
@Setter
public class RegisterFcmTokenRequest {

    /**
     * FCM token lấy từ Firebase trên client.
     */
    @NotBlank
    private String token;

}

