package com.rede_social_api.auth.service;

import com.rede_social_api.auth.dto.request.LoginRequest;
import com.rede_social_api.auth.dto.request.RefreshTokenRequest;
import com.rede_social_api.auth.dto.request.RegisterRequest;
import com.rede_social_api.auth.dto.response.AuthResponse;
import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.common.security.JwtProvider;
import com.rede_social_api.user.entity.User;
import com.rede_social_api.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userService.findByUsernameOrEmail(request.usernameOrEmail());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtProvider.isValid(token) || !jwtProvider.isRefreshToken(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid refresh token");
        }
        User user = userService.findById(jwtProvider.extractUserId(token));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(
                jwtProvider.generateAccessToken(user.getId(), user.getUsername()),
                jwtProvider.generateRefreshToken(user.getId(), user.getUsername()),
                jwtProvider.getAccessTokenExpirationMs()
        );
    }
}
