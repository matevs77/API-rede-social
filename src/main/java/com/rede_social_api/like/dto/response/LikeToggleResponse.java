package com.rede_social_api.like.dto.response;

public record LikeToggleResponse(
        boolean liked,
        int likeCount
) {}
