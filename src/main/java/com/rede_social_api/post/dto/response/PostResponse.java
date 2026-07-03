package com.rede_social_api.post.dto.response;

import com.rede_social_api.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UserSummary author,
        String content,
        List<String> mediaUrls,
        int likeCount,
        int commentCount,
        boolean likedByViewer,
        Instant createdAt,
        Instant updatedAt
) {}
