package com.rede_social_api.user.dto.response;

import com.rede_social_api.follow.entity.FollowStatus;

import java.util.UUID;

public record UserPublicResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        String location,
        Integer followerCount,
        Integer followingCount,
        boolean isPrivate,
        Boolean isFollowing,
        FollowStatus followStatus
) {}
