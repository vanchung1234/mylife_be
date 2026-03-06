package com.mylife.modules.user.entity.enums;

/**
 * Phương thức xác thực người dùng.
 * - LOCAL: đăng nhập bằng email/password do hệ thống quản lý
 * - GOOGLE: đăng nhập qua Google OAuth2
 * - FACEBOOK: (dự phòng)
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    FACEBOOK
}