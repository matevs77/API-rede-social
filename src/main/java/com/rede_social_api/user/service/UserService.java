package com.rede_social_api.user.service;

import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.follow.entity.Follow;
import com.rede_social_api.follow.entity.FollowStatus;
import com.rede_social_api.follow.repository.FollowRepository;
import com.rede_social_api.user.dto.request.UpdateProfileRequest;
import com.rede_social_api.user.dto.response.UserPublicResponse;
import com.rede_social_api.user.dto.response.UserSearchResponse;
import com.rede_social_api.user.dto.response.UserSummary;
import com.rede_social_api.user.entity.User;
import com.rede_social_api.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, FollowRepository followRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String username, String email, String password, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already taken");
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .privateProfile(false)
                .followerCount(0)
                .followingCount(0)
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserPublicResponse getMyProfile(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return toPublicResponse(user, currentUserId, null, null);
    }

    @Transactional(readOnly = true)
    public UserPublicResponse getPublicProfile(String username, UUID viewerId) {
        Optional<User> visible = viewerId != null
                ? userRepository.findVisibleProfileByUsername(username, viewerId)
                : userRepository.findLimitedProfileByUsername(username);

        if (visible.isPresent()) {
            User user = visible.get();
            FollowStatus followStatus = null;
            Boolean isFollowing = null;
            if (viewerId != null && !viewerId.equals(user.getId())) {
                Optional<Follow> follow = followRepository.findByFollowerIdAndFollowingId(viewerId, user.getId());
                followStatus = follow.map(Follow::getStatus).orElse(null);
                isFollowing = follow.map(f -> f.getStatus() == FollowStatus.ACCEPTED).orElse(false);
            }
            return toPublicResponse(user, viewerId, isFollowing, followStatus);
        }

        User limited = userRepository.findLimitedProfileByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (limited.isPrivateProfile()) {
            FollowStatus followStatus = null;
            Boolean isFollowing = false;
            if (viewerId != null) {
                Optional<Follow> follow = followRepository.findByFollowerIdAndFollowingId(viewerId, limited.getId());
                followStatus = follow.map(Follow::getStatus).orElse(null);
                isFollowing = follow.map(f -> f.getStatus() == FollowStatus.ACCEPTED).orElse(false);
            }
            return new UserPublicResponse(
                    limited.getId(),
                    limited.getUsername(),
                    limited.getDisplayName(),
                    limited.getAvatarUrl(),
                    null,
                    null,
                    null,
                    null,
                    true,
                    isFollowing,
                    followStatus
            );
        }

        throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
    }

    @Transactional
    public UserPublicResponse updateProfile(UUID currentUserId, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.location() != null) {
            user.setLocation(request.location());
        }
        if (request.isPrivate() != null) {
            user.setPrivateProfile(request.isPrivate());
        }

        userRepository.save(user);
        return toPublicResponse(user, currentUserId, null, null);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return userRepository.searchByUsername(query.trim(), Math.min(limit, 50)).stream()
                .map(u -> new UserSearchResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getAvatarUrl()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummary getUserSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return toSummary(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserSummary> findUserSummary(UUID userId) {
        return userRepository.findById(userId).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials"));
    }

    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    public UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }

    private UserPublicResponse toPublicResponse(User user, UUID viewerId, Boolean isFollowing, FollowStatus followStatus) {
        boolean isOwner = viewerId != null && viewerId.equals(user.getId());
        return new UserPublicResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getLocation(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.isPrivateProfile(),
                isOwner ? null : isFollowing,
                isOwner ? null : followStatus
        );
    }
}
