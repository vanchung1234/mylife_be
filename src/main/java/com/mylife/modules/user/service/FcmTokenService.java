package com.mylife.modules.user.service;

import com.mylife.common.exception.BusinessException;
import com.mylife.common.exception.ErrorCode;
import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.entity.UserDeviceToken;
import com.mylife.modules.user.repository.UserDeviceTokenRepository;
import com.mylife.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý FCM device token cho người dùng.
 *
 * Vai trò:
 * - Lưu token khi user đăng nhập / mở app
 * - Xoá token khi logout
 * - Lấy token để gửi push notification
 *
 * Đây là tầng BUSINESS LOGIC.
 */
@Slf4j
@Service
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
public class FcmTokenService {

    // Repository thao tác với bảng User
    private final UserRepository userRepository;

    // Repository thao tác với bảng lưu FCM token của thiết bị
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    /**
     * Đăng ký (hoặc cập nhật) FCM token cho user hiện tại.
     *
     * Policy hiện tại: "1 tài khoản - 1 thiết bị"
     *
     * Nghĩa là:
     * - Nếu user login trên thiết bị mới
     * - Xoá toàn bộ token cũ
     * - Chỉ giữ lại token mới nhất
     *
     * @param userId     ID của user hiện tại (lấy từ JWT)
     * @param token      FCM token do Firebase cấp cho thiết bị
     * @param deviceType ANDROID / IOS / WEB
     * @param deviceName Tên thiết bị (VD: Pixel 8, iPhone 15)
     */
    @Transactional // Đảm bảo toàn bộ logic chạy trong 1 transaction
    public void registerToken(UUID userId, String token, String deviceType, String deviceName) {

        // 1️⃣ Tìm user trong database
        // Nếu không tồn tại -> ném BusinessException
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2️⃣ Xoá toàn bộ token cũ của user
        // Vì policy chỉ cho phép 1 thiết bị active
        userDeviceTokenRepository.deleteAllByUserId(userId);

        // 3️⃣ Kiểm tra xem token này đã tồn tại trong hệ thống chưa
        // (trường hợp cùng 1 thiết bị gửi lại token cũ)
        UserDeviceToken deviceToken = userDeviceTokenRepository.findByDeviceToken(token)
                .orElseGet(UserDeviceToken::new);
        // Nếu tìm thấy -> dùng object cũ
        // Nếu không -> tạo mới

        // 4️⃣ Gán lại thông tin cho token
        deviceToken.setUser(user);                         // Gắn user
        deviceToken.setDeviceToken(token);                 // Gán token
        deviceToken.setDeviceType(deviceType);             // ANDROID / IOS
        deviceToken.setDeviceName(deviceName);             // Tên thiết bị
        deviceToken.setNotificationEnabled(true);          // Mặc định bật notification
        deviceToken.setLastUsedAt(LocalDateTime.now());    // Cập nhật thời điểm sử dụng gần nhất

        // 5️⃣ Lưu vào database
        userDeviceTokenRepository.save(deviceToken);

        // 6️⃣ Ghi log để tracking
        log.info("Registered FCM token for user {} - device: {}", userId, deviceName);
    }

    /**
     * Huỷ đăng ký token.
     *
     * Thường được gọi khi:
     * - User logout
     * - User xoá app
     *
     * Chỉ xoá nếu token thực sự thuộc về user đó
     * (Tránh xoá nhầm token user khác)
     */
    @Transactional
    public void unregisterToken(UUID userId, String token) {

        userDeviceTokenRepository.findByDeviceToken(token).ifPresent(deviceToken -> {

            // Kiểm tra bảo mật:
            // Đảm bảo token này thuộc về đúng user
            if (deviceToken.getUser() != null
                    && deviceToken.getUser().getId() != null
                    && deviceToken.getUser().getId().equals(userId)) {

                userDeviceTokenRepository.delete(deviceToken);

                log.info("Unregistered FCM token for user {} - token: {}", userId, token);
            }
        });
    }

    /**
     * Lấy danh sách token đang bật notification của user.
     *
     * Dùng khi:
     * - Gửi push notification
     * - Gửi thông báo hệ thống
     *
     * @return List<String> danh sách FCM token
     */
    @Transactional(readOnly = true) // Chỉ đọc, không ghi
    public List<String> getActiveTokensForUser(UUID userId) {

        return userDeviceTokenRepository
                .findByUserIdAndNotificationEnabledTrue(userId) // chỉ lấy token đang bật thông báo
                .stream()
                .map(UserDeviceToken::getDeviceToken) // lấy ra mỗi chuỗi token
                .collect(Collectors.toList());
    }
}