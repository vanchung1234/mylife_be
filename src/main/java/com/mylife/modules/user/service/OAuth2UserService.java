package com.mylife.modules.user.service;

import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.entity.enums.AuthProvider;
import com.mylife.modules.user.entity.enums.UserStatus;
import com.mylife.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Xác định provider (Google, Facebook...)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // Lấy thông tin từ attributes
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = extractEmail(attributes, registrationId);
        String name = extractName(attributes, registrationId);
        String providerId = extractProviderId(attributes, registrationId);
        String avatarUrl = extractAvatarUrl(attributes, registrationId);

        // Kiểm tra email có tồn tại không
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
        }

        // Tìm hoặc tạo user
        User user = findOrCreateUser(provider, providerId, email, name, avatarUrl);

        // Trả về OAuth2User với authorities (có thể thêm authorities nếu cần)
        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "email");
    }

    private User findOrCreateUser(AuthProvider provider, String providerId, String email, String name, String avatarUrl) {
        // Tìm user theo provider và providerId trước
        Optional<User> userOpt = userRepository.findByProviderId(providerId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Cập nhật thông tin nếu cần
            user.setDisplayName(name);
            user.setAvatarUrl(avatarUrl);
            user.setEmailVerified(true);
            return userRepository.save(user);
        }

        // Nếu không tìm thấy theo providerId, tìm theo email
        userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Nếu user đã tồn tại nhưng chưa có provider (tức là đăng ký local), cập nhật thêm provider
            if (user.getAuthProvider() == AuthProvider.LOCAL) {
                user.setAuthProvider(provider);
                user.setProviderId(providerId);
                user.setAvatarUrl(avatarUrl);
                user.setEmailVerified(true);
                // Không thay đổi password
                return userRepository.save(user);
            } else if (user.getAuthProvider() == provider) {
                // Đã có provider này, cập nhật thông tin
                user.setDisplayName(name);
                user.setAvatarUrl(avatarUrl);
                user.setEmailVerified(true);
                return userRepository.save(user);
            } else {
                // Email đã được dùng bởi provider khác - có thể ném lỗi hoặc xử lý theo nghiệp vụ
                throw new OAuth2AuthenticationException("Email already in use with another provider");
            }
        }

        // Chưa tồn tại, tạo mới
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setDisplayName(name);
        newUser.setAvatarUrl(avatarUrl);
        newUser.setAuthProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setEmailVerified(true);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setPasswordHash(null); // không có password
        // Các giá trị mặc định khác đã được set trong entity

        return userRepository.save(newUser);
    }

    // Các phương thức trích xuất thông tin tùy theo provider
    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("email");
        }
        // Có thể thêm các provider khác
        return null;
    }

    private String extractName(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("name");
        }
        return null;
    }

    private String extractProviderId(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("sub");
        }
        return null;
    }

    private String extractAvatarUrl(Map<String, Object> attributes, String registrationId) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("picture");
        }
        return null;
    }
}