package com.rede_social_api.like.service;

import com.rede_social_api.comment.repository.CommentRepository;
import com.rede_social_api.comment.service.CommentService;
import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.like.dto.response.LikeToggleResponse;
import com.rede_social_api.like.entity.Like;
import com.rede_social_api.like.entity.LikeTargetType;
import com.rede_social_api.like.repository.LikeRepository;
import com.rede_social_api.notification.entity.NotificationType;
import com.rede_social_api.notification.service.NotificationService;
import com.rede_social_api.post.repository.PostRepository;
import com.rede_social_api.post.service.PostService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final NotificationService notificationService;

    public LikeService(
            LikeRepository likeRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            @Lazy PostService postService,
            @Lazy CommentService commentService,
            NotificationService notificationService) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.commentService = commentService;
        this.notificationService = notificationService;
    }

    @Transactional
    public LikeToggleResponse togglePostLike(UUID userId, UUID postId) {
        postService.findVisiblePost(postId, userId);
        return toggleLike(userId, postId, LikeTargetType.POST);
    }

    @Transactional
    public LikeToggleResponse toggleCommentLike(UUID userId, UUID commentId) {
        commentService.findVisibleComment(commentId, userId);
        return toggleLike(userId, commentId, LikeTargetType.COMMENT);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByViewer(UUID userId, UUID postId) {
        return likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, LikeTargetType.POST, postId).isPresent();
    }

    @Transactional(readOnly = true)
    public Set<UUID> findLikedPostIds(UUID userId, List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(likeRepository.findLikedTargetIds(userId, LikeTargetType.POST, postIds));
    }

    private LikeToggleResponse toggleLike(UUID userId, UUID targetId, LikeTargetType targetType) {
        var existing = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            int likeCount = decrementLikeCount(targetId, targetType);
            return new LikeToggleResponse(false, likeCount);
        }

        Like like = Like.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(targetType)
                .build();
        likeRepository.save(like);
        int likeCount = incrementLikeCount(targetId, targetType);

        UUID authorId = getTargetAuthorId(targetId, targetType);
        notificationService.notify(authorId, userId, NotificationType.LIKE, targetId);

        return new LikeToggleResponse(true, likeCount);
    }

    private int incrementLikeCount(UUID targetId, LikeTargetType targetType) {
        if (targetType == LikeTargetType.POST) {
            postRepository.incrementLikeCount(targetId, 1);
            return postRepository.findById(targetId).map(p -> p.getLikeCount()).orElse(0);
        }
        commentRepository.incrementLikeCount(targetId, 1);
        return commentRepository.findById(targetId).map(c -> c.getLikeCount()).orElse(0);
    }

    private int decrementLikeCount(UUID targetId, LikeTargetType targetType) {
        if (targetType == LikeTargetType.POST) {
            postRepository.incrementLikeCount(targetId, -1);
            return postRepository.findById(targetId).map(p -> p.getLikeCount()).orElse(0);
        }
        commentRepository.incrementLikeCount(targetId, -1);
        return commentRepository.findById(targetId).map(c -> c.getLikeCount()).orElse(0);
    }

    private UUID getTargetAuthorId(UUID targetId, LikeTargetType targetType) {
        if (targetType == LikeTargetType.POST) {
            return postService.getPostAuthorId(targetId);
        }
        return commentService.getCommentAuthorId(targetId);
    }
}
