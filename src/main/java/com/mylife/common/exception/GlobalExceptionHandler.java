package com.mylife.common.exception;

import com.mylife.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Xử lý exception toàn cục cho các REST controller.
 * 
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Giúp bắt các exception từ controller và trả về response dạng JSON.
 */
@Slf4j  // Lombok: tạo logger
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý BusinessException - lỗi nghiệp vụ do chúng ta chủ động throw
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        log.error("Business exception: {}", ex.getMessage());
        
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Void> response = ApiResponse.failure(
                errorCode, 
                request.getRequestURI()
        );
        
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Xử lý lỗi validation từ @Valid (khi dùng với @RequestBody)
     * Ví dụ: một DTO có @NotNull mà bị null
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation error: {}", errors);
        
        ApiResponse<Map<String, String>> response = ApiResponse.failure(
                ErrorCode.VALIDATION_ERROR,
                errors,
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý lỗi validation từ các tham số request (PathVariable, RequestParam)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (error1, error2) -> error1 + ", " + error2
                ));
        
        log.error("Constraint violation: {}", errors);
        
        ApiResponse<Map<String, String>> response = ApiResponse.failure(
                ErrorCode.VALIDATION_ERROR,
                errors,
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý lỗi thiếu tham số request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String error = ex.getParameterName() + " parameter is missing";
        log.error(error);
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.INVALID_REQUEST,
                error,
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý lỗi sai kiểu dữ liệu tham số (ví dụ: truyền string vào int)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String error = String.format("Parameter '%s' should be of type %s", 
                ex.getName(), ex.getRequiredType().getSimpleName());
        log.error(error);
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.INVALID_REQUEST,
                error,
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý lỗi khi request body không đọc được (ví dụ: JSON malformed)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        log.error("Malformed JSON request: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.INVALID_REQUEST,
                "Invalid request body format",
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý lỗi upload file quá lớn
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        log.error("File too large: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.FILE_UPLOAD_ERROR,
                "File size exceeds maximum limit",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * Xử lý lỗi vi phạm toàn vẹn dữ liệu (unique constraint, foreign key...)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        log.error("Database integrity violation: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.DATABASE_ERROR,
                "Data integrity violation",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ========== XỬ LÝ LỖI SPRING SECURITY ==========

    /**
     * Xử lý lỗi xác thực (sai credentials)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        
        log.error("Bad credentials: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Xử lý lỗi tài khoản bị khóa
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(
            LockedException ex, HttpServletRequest request) {
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.AUTH_ACCOUNT_LOCKED,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Xử lý lỗi tài khoản bị vô hiệu hóa
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.AUTH_ACCOUNT_DISABLED,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Xử lý lỗi phân quyền (access denied)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.error("Access denied: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.ACCESS_DENIED,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Xử lý tất cả các exception còn lại (unexpected errors)
     * Đây là fallback cuối cùng
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);  // Log stack trace
        
        ApiResponse<Void> response = ApiResponse.failure(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}