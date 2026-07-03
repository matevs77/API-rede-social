package com.rede_social_api.post.controller;

import com.rede_social_api.common.pagination.CursorPage;
import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.post.dto.request.CreatePostRequest;
import com.rede_social_api.post.dto.request.UpdatePostRequest;
import com.rede_social_api.post.dto.response.PostResponse;
import com.rede_social_api.post.service.PostService;
import com.rede_social_api.user.repository.UserRepository;
import com.rede_social_api.user.entity.User;
import com.rede_social_api.common.exception.ApiException;
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
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    public PostController(PostService postService, UserRepository userRepository) {
        this.postService = postService;
        this.userRepository = userRepository;
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(currentUser.getId(), request);
    }

    @PatchMapping("/posts/{id}")
    public PostResponse updatePost(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {
        return postService.updatePost(id, currentUser.getId(), request);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        postService.deletePost(id, currentUser.getId());
    }

    @GetMapping("/posts/{id}")
    public PostResponse getPost(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        return postService.getPost(id, currentUser.getId());
    }

    @GetMapping("/users/{username}/posts")
    public CursorPage<PostResponse> getUserPosts(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable String username,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return postService.getPostsByAuthor(user.getId(), currentUser.getId(), cursor, limit);
    }
}
