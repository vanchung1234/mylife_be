package com.mylife.modules.user.entity.enums;
/**
 * Trạng thái tài khoản.
 * - ACTIVE: hoạt động bình thường
 * - INACTIVE: chưa kích hoạt (ví dụ sau đăng ký cần xác thực email)
 * - LOCKED: bị khóa do nhập sai mật khẩu nhiều lần hoặc admin khóa
 * - DELETED: đã xóa (soft delete)
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    DELETED
}
