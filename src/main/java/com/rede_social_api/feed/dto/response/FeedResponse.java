package com.rede_social_api.feed.dto.response;

import com.rede_social_api.post.dto.response.PostResponse;

import java.util.List;

public record FeedResponse(
        List<PostResponse> items,
        String nextCursor
) {}
