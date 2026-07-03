package com.rede_social_api.comment.service;

import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.common.pagination.CompositeCursor;
import com.rede_social_api.common.pagination.CursorCodec;
import com.rede_social_api.common.pagination.CursorPage;
import com.rede_social_api.comment.dto.request.CreateCommentRequest;
import com.rede_social_api.comment.dto.request.UpdateCommentRequest;
import com.rede_social_api.comment.dto.response.CommentResponse;
import com.rede_social_api.comment.entity.Comment;
import com.rede_social_api.comment.repository.CommentRepository;
import com.rede_social_api.like.entity.LikeTargetType;
import com.rede_social_api.like.repository.LikeRepository;
import com.rede_social_api.notification.entity.NotificationType;
import com.rede_social_api.notification.service.NotificationService;
import com.rede_social_api.post.service.PostService;
import com.rede_social_api.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;
    private final LikeRepository likeRepository;
    private final NotificationService notificationService;
    private final CursorCodec cursorCodec;

    public CommentService(
            CommentRepository commentRepository,
            PostService postService,
            UserService userService,
            LikeRepository likeRepository,
            NotificationService notificationService,
            CursorCodec cursorCodec) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.userService = userService;
        this.likeRepository = likeRepository;
        this.notificationService = notificationService;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public CommentResponse createComment(UUID postId, UUID authorId, CreateCommentRequest request) {
        postService.findVisiblePost(postId, authorId);

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content(request.content())
                .likeCount(0)
                .build();
        comment = commentRepository.save(comment);
        postService.incrementCommentCount(postId, 1);

        UUID postAuthorId = postService.getPostAuthorId(postId);
        notificationService.notify(postAuthorId, authorId, NotificationType.COMMENT, comment.getId());

        return toResponse(comment, authorId, false);
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UUID authorId, UpdateCommentRequest request) {
        int updated = commentRepository.updateOwnedComment(commentId, authorId, request.content());
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found");
        }
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        boolean liked = likeRepository.findByUserIdAndTargetTypeAndTargetId(authorId, LikeTargetType.COMMENT, commentId).isPresent();
        return toResponse(comment, authorId, liked);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID authorId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        int deleted = commentRepository.deleteOwnedComment(commentId, authorId);
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found");
        }
        if (comment != null) {
            postService.incrementCommentCount(comment.getPostId(), -1);
        }
    }

    @Transactional(readOnly = true)
    public CursorPage<CommentResponse> listComments(UUID postId, UUID viewerId, String cursor, int limit) {
        postService.findVisiblePost(postId, viewerId);
        CompositeCursor compositeCursor = cursorCodec.decode(cursor);
        List<Comment> comments = commentRepository.findVisibleByPost(
                postId,
                viewerId,
                compositeCursor != null ? compositeCursor.createdAt() : null,
                compositeCursor != null ? compositeCursor.id() : null,
                limit + 1
        );

        int count = Math.min(comments.size(), limit);
        List<Comment> page = comments.subList(0, count);
        Set<UUID> likedIds = page.isEmpty()
                ? Set.of()
                : new HashSet<>(likeRepository.findLikedTargetIds(
                        viewerId, LikeTargetType.COMMENT, page.stream().map(Comment::getId).toList()));

        List<CommentResponse> items = page.stream()
                .map(c -> toResponse(c, viewerId, likedIds.contains(c.getId())))
                .toList();

        String nextCursor = null;
        if (comments.size() > limit) {
            Comment last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getCreatedAt(), last.getId());
        }
        return CursorPage.of(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public Comment findVisibleComment(UUID commentId, UUID viewerId) {
        return commentRepository.findVisibleById(commentId, viewerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found"));
    }

    @Transactional(readOnly = true)
    public UUID getCommentAuthorId(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(Comment::getAuthorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found"));
    }

    private CommentResponse toResponse(Comment comment, UUID viewerId, boolean likedByViewer) {
        return new CommentResponse(
                comment.getId(),
                userService.getUserSummary(comment.getAuthorId()),
                comment.getContent(),
                comment.getLikeCount(),
                likedByViewer,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
