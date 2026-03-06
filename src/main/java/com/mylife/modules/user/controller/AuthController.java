package com.mylife.modules.user.controller;

import com.mylife.common.response.ApiResponse;
import com.mylife.modules.user.dto.request.LoginRequest;
import com.mylife.modules.user.dto.request.RefreshTokenRequest;
import com.mylife.modules.user.dto.request.RegisterRequest;
import com.mylife.modules.user.dto.response.AuthResponse;
import com.mylife.modules.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đăng ký thành công", httpRequest.getRequestURI(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-FCM-Token", required = false) String fcmToken,
            @RequestHeader(value = "X-Device-Type", required = false) String deviceType,
            @RequestHeader(value = "X-Device-Name", required = false) String deviceName,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, fcmToken, deviceType, deviceName, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công", httpRequest.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Refresh token thành công", httpRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String bearerToken,
            @RequestHeader(value = "X-FCM-Token", required = false) String fcmToken,
            HttpServletRequest httpRequest) {
        String refreshToken = httpRequest.getParameter("refreshToken");
        authService.logout(refreshToken, fcmToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công", httpRequest.getRequestURI()));
    }
}