package com.mylife.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mylife.common.exception.ErrorCode;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Cấu trúc response chuẩn cho tất cả API.
 * 
 * @param <T> Kiểu dữ liệu của data trả về
 * 
 * Các trường:
 * - success: true nếu thành công, false nếu thất bại
 * - statusCode: Mã HTTP
 * - message: Thông báo (có thể hiển thị cho user)
 * - errorCode: Mã lỗi (nếu có), dùng cho frontend xử lý logic
 * - timestamp: Thời gian trả về
 * - path: Đường dẫn API được gọi
 * - data: Dữ liệu trả về (nếu thành công)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // Không serialize các trường null thành JSON
public class ApiResponse<T> {

    private boolean success;
    private int statusCode;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private String path;
    private T data;

    /**
     * Tạo response thành công (không có data)
     */
    public static ApiResponse<Void> success(String message, String path) {
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Tạo response thành công (có data)
     */
    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(data)
                .build();
    }

    /**
     * Tạo response thành công với status code tùy chỉnh (ví dụ 201 Created)
     */
    public static <T> ApiResponse<T> success(T data, String message, String path, int statusCode) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(data)
                .build();
    }

    /**
     * Tạo response thất bại từ ErrorCode
     */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String path) {
        return ApiResponse.<Void>builder()
                .success(false)
                .statusCode(errorCode.getHttpStatus().value())
                .message(errorCode.getMessage())
                .errorCode(errorCode.getCode())
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Tạo response thất bại với message tùy chỉnh
     */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String customMessage, String path) {
        return ApiResponse.<Void>builder()
                .success(false)
                .statusCode(errorCode.getHttpStatus().value())
                .message(customMessage)
                .errorCode(errorCode.getCode())
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Tạo response thất bại với dữ liệu lỗi kèm theo (ví dụ: validation errors)
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, T errorData, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(errorCode.getHttpStatus().value())
                .message(errorCode.getMessage())
                .errorCode(errorCode.getCode())
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(errorData)
                .build();
    }
}