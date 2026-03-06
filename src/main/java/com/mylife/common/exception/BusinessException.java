package com.mylife.common.exception;

import lombok.Getter;

/**
 * Exception dành cho các lỗi nghiệp vụ (business logic).
 * Khi throw exception này, cần cung cấp ErrorCode tương ứng.
 * 
 * Ví dụ: throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());  // Gọi constructor của RuntimeException với message
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);  // Cho phép ghi đè message nếu cần
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}