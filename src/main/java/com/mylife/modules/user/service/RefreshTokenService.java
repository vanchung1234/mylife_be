
package com.mylife.modules.user.service;

import com.mylife.modules.user.entity.RefreshToken;
import com.mylife.modules.user.entity.User;
import com.mylife.modules.user.repository.RefreshTokenRepository;
import com.mylife.modules.user.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceInfo, String ipAddress) {
        String token = tokenProvider.generateRefreshToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationSeconds()));
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken saveRefreshToken(User user, String token, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationSeconds()));
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        return refreshTokenRepository.save(refreshToken);
    }
}