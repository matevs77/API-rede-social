package com.rede_social_api.follow.controller;

import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.follow.dto.response.FollowActionResponse;
import com.rede_social_api.follow.dto.response.FollowListResponse;
import com.rede_social_api.follow.service.FollowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/users/{userId}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowActionResponse follow(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID userId) {
        return followService.follow(currentUser.getId(), userId);
    }

    @DeleteMapping("/users/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID userId) {
        followService.unfollow(currentUser.getId(), userId);
    }

    @PatchMapping("/follows/{followId}/accept")
    public FollowActionResponse acceptFollow(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID followId) {
        return followService.acceptFollowRequest(currentUser.getId(), followId);
    }

    @GetMapping("/users/{userId}/followers")
    public FollowListResponse getFollowers(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return followService.getFollowers(userId, cursor, limit);
    }

    @GetMapping("/users/{userId}/following")
    public FollowListResponse getFollowing(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return followService.getFollowing(userId, cursor, limit);
    }
}
