package com.mylife.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enum định nghĩa các mã lỗi của hệ thống.
 * Mỗi lỗi gồm:
 * - code: mã lỗi (dùng cho frontend xử lý)
 * - message: thông báo lỗi (có thể hiển thị cho user)
 * - statusCode: HTTP status code tương ứng
 * 
 * Cách đặt code: [DOMAIN]_[ERROR_TYPE] (ví dụ: AUTH_INVALID_TOKEN)
 */
@Getter
public enum ErrorCode {

    // ========== LỖI XÁC THỰC & NGƯỜI DÙNG (AUTH) ==========
    AUTH_INVALID_CREDENTIALS("AUTH_001", "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_EXPIRED("AUTH_002", "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("AUTH_003", "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_TOKEN_EXPIRED("AUTH_004", "Refresh token đã hết hạn", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_TOKEN_REVOKED("AUTH_005", "Refresh token đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_LOCKED("AUTH_006", "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_DISABLED("AUTH_007", "Tài khoản chưa được kích hoạt", HttpStatus.FORBIDDEN),
    AUTH_2FA_REQUIRED("AUTH_008", "Yêu cầu xác thực 2 yếu tố", HttpStatus.UNAUTHORIZED),
    AUTH_2FA_INVALID("AUTH_009", "Mã xác thực 2 yếu tố không đúng", HttpStatus.UNAUTHORIZED),

    // ========== LỖI NGƯỜI DÙNG (USER) ==========
    USER_NOT_FOUND("USER_001", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_EMAIL_EXISTS("USER_002", "Email đã được sử dụng", HttpStatus.CONFLICT),
    USER_INVALID_OLD_PASSWORD("USER_003", "Mật khẩu cũ không đúng", HttpStatus.BAD_REQUEST),

    // ========== LỖI TÀI CHÍNH ==========
    ACCOUNT_NOT_FOUND("FIN_001", "Không tìm thấy tài khoản", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("FIN_002", "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND("FIN_003", "Không tìm thấy giao dịch", HttpStatus.NOT_FOUND),
    BUDGET_NOT_FOUND("FIN_004", "Không tìm thấy ngân sách", HttpStatus.NOT_FOUND),
    INSUFFICIENT_BALANCE("FIN_005", "Số dư không đủ", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION_TYPE("FIN_006", "Loại giao dịch không hợp lệ", HttpStatus.BAD_REQUEST),

    // ========== LỖI SỨC KHỎE ==========
    ACTIVITY_NOT_FOUND("HEALTH_001", "Không tìm thấy hoạt động", HttpStatus.NOT_FOUND),

    // ========== LỖI HỌC TẬP ==========
    FLASHCARD_SET_NOT_FOUND("LEARN_001", "Không tìm thấy bộ flashcard", HttpStatus.NOT_FOUND),
    FLASHCARD_NOT_FOUND("LEARN_002", "Không tìm thấy flashcard", HttpStatus.NOT_FOUND),

    // ========== LỖI VALIDATION DỮ LIỆU ==========
    VALIDATION_ERROR("VALID_001", "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("VALID_002", "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),

    // ========== LỖI HỆ THỐNG ==========
    INTERNAL_SERVER_ERROR("SYS_001", "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("SYS_002", "Lỗi cơ sở dữ liệu", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("SYS_003", "Lỗi dịch vụ bên ngoài", HttpStatus.SERVICE_UNAVAILABLE),
    FILE_UPLOAD_ERROR("SYS_004", "Lỗi upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_ERROR("SYS_005", "Lỗi xóa file", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========== LỖI PHÂN QUYỀN ==========
    ACCESS_DENIED("SEC_001", "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("SEC_002", "Vui lòng đăng nhập", HttpStatus.UNAUTHORIZED),

    // ========== LỖI HABIT ==========
    HABIT_NOT_FOUND("HABIT_001", "Không tìm thấy thói quen", HttpStatus.NOT_FOUND),
    HABIT_LOG_NOT_FOUND("HABIT_002", "Không tìm thấy log của thói quen", HttpStatus.NOT_FOUND),
    HABIT_CANNOT_CHECK_IN_PAST("HABIT_003", "Không thể check-in cho ngày trong quá khứ xa", HttpStatus.BAD_REQUEST),
    HABIT_ALREADY_CHECKED("HABIT_004", "Hôm nay bạn đã check-in rồi", HttpStatus.CONFLICT),
    HABIT_PAUSED("HABIT_005", "Thói quen đang tạm dừng", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}