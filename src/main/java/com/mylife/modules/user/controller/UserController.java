package com.mylife.modules.user.controller;

import com.mylife.common.response.ApiResponse;
import com.mylife.modules.user.dto.request.ChangePasswordRequest;
import com.mylife.modules.user.dto.request.RegisterFcmTokenRequest;
import com.mylife.modules.user.dto.request.UpdateProfileRequest;
import com.mylife.modules.user.dto.response.UserProfileResponse;
import com.mylife.modules.user.security.CustomUserDetails;
import com.mylife.modules.user.service.FcmTokenService;
import com.mylife.modules.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FcmTokenService fcmTokenService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {
        UserProfileResponse profile = userService.getCurrentUserProfile(currentUser.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Lấy thông tin thành công", request.getRequestURI()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        UserProfileResponse profile = userService.updateProfile(currentUser.getUser().getId(), updateRequest);
        return ResponseEntity.ok(ApiResponse.success(profile, "Cập nhật thành công", request.getRequestURI()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        userService.changePassword(currentUser.getUser().getId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công", httpRequest.getRequestURI()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {
        userService.deleteAccount(currentUser.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Tài khoản đã được xóa", request.getRequestURI()));
    }

    /**
     * Đăng ký (hoặc cập nhật) FCM device token cho user hiện tại.
     * Client nên gọi endpoint này sau khi đăng nhập và mỗi khi token thay đổi.
     */
    @PostMapping("/me/devices/fcm")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody RegisterFcmTokenRequest request,
            @RequestHeader("X-Device-Type") String deviceType,
            @RequestHeader("X-Device-Name") String deviceName,
            HttpServletRequest httpRequest) {

        fcmTokenService.registerToken(
                currentUser.getUser().getId(),
                request.getToken(),
                deviceType,
                deviceName);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đăng ký FCM token thành công", httpRequest.getRequestURI()));
    }

    /**
     * Huỷ đăng ký FCM token cho user hiện tại (tuỳ chọn).
     * Có thể gọi khi logout hoặc user tắt notification cho thiết bị.
     */
    @DeleteMapping("/me/devices/fcm")
    public ResponseEntity<ApiResponse<Void>> unregisterFcmToken(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) {
        fcmTokenService.unregisterToken(currentUser.getUser().getId(), token);
        return ResponseEntity
                .ok(ApiResponse.success(null, "Huỷ đăng ký FCM token thành công", httpRequest.getRequestURI()));
    }
}