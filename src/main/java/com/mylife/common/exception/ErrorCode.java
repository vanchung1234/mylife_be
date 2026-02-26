package com.mylife.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 * Centralized error code definition
 *
 * Quy ước:
 * 1000–1999 → USER
 * 2000–2999 → TASK
 * 9000–9999 → SYSTEM
 */
@Getter
public enum ErrorCode {

    // ===== SUCCESS =====
    SUCCESS(200, "Success", HttpStatus.OK),

    // ===== SYSTEM =====
    INTERNAL_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}