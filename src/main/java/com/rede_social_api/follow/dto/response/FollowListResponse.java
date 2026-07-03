package com.rede_social_api.follow.dto.response;

import com.rede_social_api.user.dto.response.UserSummary;

import java.util.List;

public record FollowListResponse(
        List<UserSummary> items,
        String nextCursor
) {}
