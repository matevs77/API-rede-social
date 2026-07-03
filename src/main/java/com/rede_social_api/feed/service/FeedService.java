package com.rede_social_api.feed.service;

import com.rede_social_api.common.pagination.CursorPage;
import com.rede_social_api.feed.dto.response.FeedResponse;
import com.rede_social_api.follow.service.FollowService;
import com.rede_social_api.post.dto.response.PostResponse;
import com.rede_social_api.post.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FeedService {

    private final FollowService followService;
    private final PostService postService;

    public FeedService(FollowService followService, PostService postService) {
        this.followService = followService;
        this.postService = postService;
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(UUID viewerId, String cursor, int limit) {
        List<UUID> followingIds = followService.getAcceptedFollowingIds(viewerId);
        CursorPage<PostResponse> page = postService.findFeedPosts(followingIds, viewerId, cursor, limit);
        return new FeedResponse(page.items(), page.nextCursor());
    }
}
