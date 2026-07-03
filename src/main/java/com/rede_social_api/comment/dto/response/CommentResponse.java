package com.rede_social_api.comment.dto.response;

import com.rede_social_api.user.dto.response.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UserSummary author,
        String content,
        int likeCount,
        boolean likedByViewer,
        Instant createdAt,
        Instant updatedAt
) {}
