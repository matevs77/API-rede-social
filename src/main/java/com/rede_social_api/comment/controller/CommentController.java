package com.rede_social_api.comment.controller;

import com.rede_social_api.common.pagination.CursorPage;
import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.comment.dto.request.CreateCommentRequest;
import com.rede_social_api.comment.dto.request.UpdateCommentRequest;
import com.rede_social_api.comment.dto.response.CommentResponse;
import com.rede_social_api.comment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return commentService.createComment(postId, currentUser.getId(), request);
    }

    @GetMapping
    public CursorPage<CommentResponse> listComments(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return commentService.listComments(postId, currentUser.getId(), cursor, limit);
    }

    @PatchMapping("/{commentId}")
    public CommentResponse updateComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(commentId, currentUser.getId(), request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID postId,
            @PathVariable UUID commentId) {
        commentService.deleteComment(commentId, currentUser.getId());
    }
}
