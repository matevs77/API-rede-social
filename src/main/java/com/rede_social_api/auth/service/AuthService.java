package com.rede_social_api.auth.service;

import com.rede_social_api.auth.dto.request.LoginRequest;
import com.rede_social_api.auth.dto.request.RefreshTokenRequest;
import com.rede_social_api.auth.dto.request.RegisterRequest;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.auth.entity.RefreshToken;
import com.rede_social_api.auth.repository.RefreshTokenRepository;
import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.common.security.JwtProvider;
import com.rede_social_api.user.entity.User;
import com.rede_social_api.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.username(),
                request.email(),
                request.password(),
                request.displayName()
        );
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userService.findByUsernameOrEmail(request.usernameOrEmail());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtProvider.isValid(token) || !jwtProvider.isRefreshToken(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid refresh token");
        }

        String tokenHash = hashToken(token);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", "Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Refresh token has expired");
        }

        User user = userService.findById(jwtProvider.extractUserId(token));
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtProvider.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        return new AuthResponse(accessToken, refreshToken, jwtProvider.getAccessTokenExpirationMs());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
