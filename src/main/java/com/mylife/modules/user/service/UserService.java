package com.mylife.modules.user.service;

import com.mylife.common.exception.BusinessException;
import com.mylife.common.exception.ErrorCode;
import com.mylife.modules.user.dto.request.UpdateProfileRequest;
import com.mylife.modules.user.dto.response.UserProfileResponse;
import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.entity.enums.UserStatus;
import com.mylife.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy thông tin profile của user hiện tại.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToProfileResponse(user);
    }

    /**
     * Cập nhật thông tin profile.
     */
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getDefaultCurrency() != null) {
            user.setDefaultCurrency(request.getDefaultCurrency());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }
        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }
        if (request.getResetTime() != null) {
            user.setResetTime(request.getResetTime());
        }
        if (request.getNotificationEnabled() != null) {
            user.setNotificationEnabled(request.getNotificationEnabled());
        }

        User saved = userRepository.save(user);
        log.info("Updated profile for user: {}", userId);
        return mapToProfileResponse(saved);
    }

    /**
     * Đổi mật khẩu.
     */
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = findUserById(userId);

        // Kiểm tra mật khẩu cũ (nếu user đăng ký local)
        if (user.getAuthProvider() != com.mylife.modules.user.entity.enums.AuthProvider.LOCAL) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Tài khoản này không có mật khẩu (đăng nhập qua Google)");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USER_INVALID_OLD_PASSWORD);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Changed password for user: {}", userId);
    }

    /**
     * Xóa tài khoản (soft delete).
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findUserById(userId);
        user.setDeleted(true);
        user.setStatus(UserStatus.DELETED);
        // Có thể thu hồi tất cả refresh token ở đây (sẽ làm trong service riêng)
        userRepository.save(user);
        log.info("Soft deleted account for user: {}", userId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .defaultCurrency(user.getDefaultCurrency())
                .timezone(user.getTimezone())
                .language(user.getLanguage())
                .resetTime(user.getResetTime())
                .notificationEnabled(user.getNotificationEnabled())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .status(user.getStatus().name())
                .authProvider(user.getAuthProvider().name())
                .emailVerified(user.getEmailVerified())
                .build();
    }
}