package com.mylife.modules.user.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service gửi push notification qua Firebase Cloud Messaging (FCM).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenService fcmTokenService;

    /**
     * Gửi notification tới tất cả thiết bị active của một user.
     */
    public void sendNotificationToUser(UUID userId, String title, String body, Map<String, String> data) {
        List<String> tokens = fcmTokenService.getActiveTokensForUser(userId);
        sendNotificationToTokens(tokens, title, body, data);
    }

    /**
     * Gửi notification tới một danh sách FCM token cụ thể.
     */
    public void sendNotificationToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging bean is null. Skip sending FCM notification.");
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .setNotification(notification)
                .addAllTokens(tokens);

        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        try {
            BatchResponse response = firebaseMessaging.sendMulticast(messageBuilder.build());
            log.info("Sent FCM notification to {} devices. Success: {}, Failure: {}",
                    tokens.size(), response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM notification", e);
            // Không throw ra ngoài để tránh làm fail scheduler; chỉ log lỗi.
        }
    }
}

