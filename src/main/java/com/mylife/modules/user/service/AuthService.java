package com.mylife.modules.user.service;

import com.mylife.common.exception.BusinessException;
import com.mylife.common.exception.ErrorCode;
import com.mylife.modules.user.dto.request.LoginRequest;
import com.mylife.modules.user.dto.request.RefreshTokenRequest;
import com.mylife.modules.user.dto.request.RegisterRequest;
import com.mylife.modules.user.dto.response.AuthResponse;
import com.mylife.modules.user.entity.LoginActivity;
import com.mylife.modules.user.entity.RefreshToken;
import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.entity.enums.AuthProvider;
import com.mylife.modules.user.entity.enums.UserStatus;
import com.mylife.modules.user.repository.LoginActivityRepository;
import com.mylife.modules.user.repository.RefreshTokenRepository;
import com.mylife.modules.user.repository.UserRepository;
import com.mylife.modules.user.security.CustomUserDetails;
import com.mylife.modules.user.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginActivityRepository loginActivityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final FcmTokenService fcmTokenService;

    /**
     * Đăng ký tài khoản mới.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException(ErrorCode.USER_EMAIL_EXISTS);
        }

        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setStatus(UserStatus.ACTIVE); // có thể set INACTIVE nếu cần xác thực email
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        // Tự động đăng nhập sau đăng ký (tạo token)
        return authenticateUser(savedUser, httpRequest);
    }

    /**
     * Đăng nhập bằng email/password.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String fcmToken, String deviceType, String deviceName,
            HttpServletRequest httpRequest) {
        try {
            // Xác thực thông qua Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Lấy thông tin user từ authentication
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Kiểm tra trạng thái user (phòng khi authentication đã check nhưng vẫn check
            // lại)
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
            }

            // Policy: 1 tài khoản - 1 thiết bị
            // - Thu hồi mọi refresh token cũ của user
            // - Đăng ký (ghi đè) FCM token mới cho thiết bị hiện tại (nếu có)
            revokeAllRefreshTokensForUser(user);
            if (fcmToken != null && !fcmToken.isBlank()) {
                fcmTokenService.registerToken(user.getId(), fcmToken, deviceType, deviceName);
            }

            return authenticateUser(user, httpRequest);

        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getEmail(), httpRequest);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
    }

    /**
     * Refresh access token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String token = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

        // Kiểm tra revoked
        if (refreshToken.getRevoked()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);
        }

        // Kiểm tra hết hạn
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        // Tạo access token mới
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());

        // Cập nhật last used
        refreshToken.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(newAccessToken)
                .refreshToken(token) // trả lại refresh token cũ (hoặc có thể tạo mới tùy policy)
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * Đăng xuất (thu hồi refresh token và xoá FCM token nếu có).
     */
    @Transactional
    public void logout(String refreshToken, String fcmToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.info("User logged out, refresh token revoked");

        if (fcmToken != null && !fcmToken.isBlank()) {
            fcmTokenService.unregisterToken(token.getUser().getId(), fcmToken);
        }
    }

    /**
     * Xử lý sau khi xác thực thành công: tạo tokens, ghi login activity.
     */
    private AuthResponse authenticateUser(User user, HttpServletRequest httpRequest) {
        // Tạo access token và refresh token
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken();

        // Lưu refresh token
        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken); // Có thể hash token trước khi lưu (nên làm)
        rt.setUser(user);
        rt.setExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationSeconds()));
        rt.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        rt.setIpAddress(getClientIp(httpRequest));
        refreshTokenRepository.save(rt);

        // Ghi login activity
        LoginActivity activity = new LoginActivity();
        activity.setUser(user);
        activity.setIpAddress(getClientIp(httpRequest));
        activity.setUserAgent(httpRequest.getHeader("User-Agent"));
        activity.setLoginTime(LocalDateTime.now());
        activity.setSuccess(true);
        loginActivityRepository.save(activity);

        // Cập nhật last login cho user
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        user.resetFailedAttempts();
        userRepository.save(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * Xử lý đăng nhập thất bại: tăng failed attempts, khóa nếu cần.
     */
    private void handleFailedLogin(String email, HttpServletRequest httpRequest) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            user.incrementFailedAttempts();
            if (user.getFailedAttempts() >= 5) { // ngưỡng khóa
                user.lock();
                log.warn("User account locked due to too many failed attempts: {}", email);
            }
            userRepository.save(user);

            // Ghi login activity thất bại
            LoginActivity activity = new LoginActivity();
            activity.setUser(user);
            activity.setIpAddress(getClientIp(httpRequest));
            activity.setUserAgent(httpRequest.getHeader("User-Agent"));
            activity.setLoginTime(LocalDateTime.now());
            activity.setSuccess(false);
            activity.setFailureReason("Invalid credentials");
            loginActivityRepository.save(activity);
        });
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Transactional
    public void saveRefreshToken(User user, String refreshToken, HttpServletRequest request) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(refreshToken);
        rt.setUser(user);
        rt.setExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationSeconds()));
        rt.setDeviceInfo(request.getHeader("User-Agent"));
        rt.setIpAddress(getClientIp(request));
        refreshTokenRepository.save(rt);
    }

    /**
     * Thu hồi tất cả refresh token còn hiệu lực của user (policy: 1 tài khoản - 1 thiết bị).
     */
    @Transactional
    protected void revokeAllRefreshTokensForUser(User user) {
        refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(user.getId()) && !rt.getRevoked())
                .forEach(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
}