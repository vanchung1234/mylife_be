package com.mylife.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
 * Enterprise Standard API Response
 *
 * Quy ước code:
 * 1000–1999 → USER
 * 2000–2999 → TASK
 * 9000–9999 → SYSTEM
 */
@Getter
@Builder
public class ApiResponse<T> {

    // Trạng thái xử lý
    private final boolean success;

    // Mã nội bộ của hệ thống
    private final int code;

    // Thông báo cho client
    private final String message;

    // Dữ liệu trả về
    private final T data;

    // Thời điểm response được tạo
    private final LocalDateTime timestamp;


    /* =========================
       SUCCESS FACTORY
       ========================= */

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /* =========================
       ERROR FACTORY
       ========================= */

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}