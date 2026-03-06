package com.mylife.modules.user.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cấu hình Firebase Admin SDK để gửi notification qua FCM.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.enabled:true}")
    private boolean fcmEnabled;

    @Value("${fcm.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!fcmEnabled) {
            log.warn("FCM is disabled via configuration (fcm.enabled=false)");
            return null;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials;
        if (StringUtils.hasText(credentialsPath)) {
            InputStream serviceAccountStream;
            if (credentialsPath.startsWith("classpath:")) {
                String path = credentialsPath.replaceFirst("classpath:", "");
                serviceAccountStream = new ClassPathResource(path).getInputStream();
            } else {
                serviceAccountStream = new FileInputStream(credentialsPath);
            }
            credentials = GoogleCredentials.fromStream(serviceAccountStream);
        } else {
            // Sử dụng GOOGLE_APPLICATION_CREDENTIALS từ biến môi trường
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        log.info("Initializing FirebaseApp for FCM");
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            // Nếu FCM bị disable, không khởi tạo FirebaseMessaging
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}

