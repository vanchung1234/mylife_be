package com.mylife.modules.user.security;

import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.entity.enums.AuthProvider;
import com.mylife.modules.user.entity.enums.UserStatus;
import com.mylife.modules.user.repository.UserRepository;
import com.mylife.modules.user.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        if (email == null) {
            throw new IllegalStateException("Email not found from OAuth2 provider");
        }

        // Tìm user; nếu chưa có (lần đầu OAuth2) thì tạo luôn để tránh crash
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email.toLowerCase());
            u.setDisplayName(name);
            u.setAvatarUrl(avatarUrl);
            u.setAuthProvider(AuthProvider.GOOGLE);
            u.setProviderId(providerId);
            u.setStatus(UserStatus.ACTIVE);
            u.setEmailVerified(true);
            return userRepository.save(u);
        });

        // Tạo access token và refresh token
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken();

        // Lưu refresh token (KHÔNG dùng AuthService để tránh circular dependency)
        refreshTokenService.saveRefreshToken(user, refreshToken, request.getHeader("User-Agent"), getClientIp(request));

        // Tạo redirect URL về frontend kèm token
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("expiresIn", tokenProvider.getAccessTokenExpirationSeconds())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}