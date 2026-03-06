package com.mylife.modules.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Tên hiển thị không quá 100 ký tự")
    private String displayName;

    private String defaultCurrency;

    private String timezone;

    private String language;

    private LocalTime resetTime;

    private Boolean notificationEnabled;
}