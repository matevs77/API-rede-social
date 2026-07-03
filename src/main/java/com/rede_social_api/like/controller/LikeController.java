package com.rede_social_api.like.controller;

import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.like.dto.response.LikeToggleResponse;
import com.rede_social_api.like.service.LikeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/posts/{id}/like")
    public LikeToggleResponse likePost(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return likeService.togglePostLike(currentUser.getId(), id);
    }

    @DeleteMapping("/posts/{id}/like")
    public LikeToggleResponse unlikePost(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return likeService.togglePostLike(currentUser.getId(), id);
    }

    @PostMapping("/comments/{id}/like")
    public LikeToggleResponse likeComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return likeService.toggleCommentLike(currentUser.getId(), id);
    }

    @DeleteMapping("/comments/{id}/like")
    public LikeToggleResponse unlikeComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return likeService.toggleCommentLike(currentUser.getId(), id);
    }
}
