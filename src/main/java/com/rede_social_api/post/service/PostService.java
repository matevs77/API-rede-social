package com.rede_social_api.post.service;

import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.common.pagination.CompositeCursor;
import com.rede_social_api.common.pagination.CursorCodec;
import com.rede_social_api.common.pagination.CursorPage;
import com.rede_social_api.like.service.LikeService;
import com.rede_social_api.post.dto.request.CreatePostRequest;
import org.springframework.context.annotation.Lazy;
import com.rede_social_api.post.dto.request.UpdatePostRequest;
import com.rede_social_api.post.dto.response.PostResponse;
import com.rede_social_api.post.entity.Post;
import com.rede_social_api.post.repository.PostRepository;
import com.rede_social_api.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final LikeService likeService;
    private final CursorCodec cursorCodec;

    public PostService(
            PostRepository postRepository,
            UserService userService,
            @Lazy LikeService likeService,
            CursorCodec cursorCodec) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.likeService = likeService;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public PostResponse createPost(UUID authorId, CreatePostRequest request) {
        Post post = Post.builder()
                .authorId(authorId)
                .content(request.content())
                .mediaUrls(request.mediaUrls() != null ? new ArrayList<>(request.mediaUrls()) : new ArrayList<>())
                .likeCount(0)
                .commentCount(0)
                .build();
        post = postRepository.save(post);
        return toResponse(post, authorId, false);
    }

    @Transactional
    public PostResponse updatePost(UUID postId, UUID authorId, UpdatePostRequest request) {
        int updated = postRepository.updateOwnedPost(postId, authorId, request.content());
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found");
        }
        Post post = postRepository.findById(postId).orElseThrow();
        if (request.mediaUrls() != null) {
            post.setMediaUrls(new ArrayList<>(request.mediaUrls()));
            postRepository.save(post);
        }
        return getPost(postId, authorId);
    }

    @Transactional
    public void deletePost(UUID postId, UUID authorId) {
        int deleted = postRepository.deleteOwnedPost(postId, authorId);
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found");
        }
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId, UUID viewerId) {
        Post post = postRepository.findVisibleById(postId, viewerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found"));
        boolean liked = likeService.isLikedByViewer(viewerId, post.getId());
        return toResponse(post, viewerId, liked);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostResponse> getPostsByAuthor(UUID authorId, UUID viewerId, String cursor, int limit) {
        CompositeCursor compositeCursor = cursorCodec.decode(cursor);
        List<Post> posts = postRepository.findVisibleByAuthor(
                authorId,
                viewerId,
                compositeCursor != null ? compositeCursor.createdAt() : null,
                compositeCursor != null ? compositeCursor.id() : null,
                limit + 1
        );
        return mapPage(posts, viewerId, limit);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostResponse> findFeedPosts(List<UUID> authorIds, UUID viewerId, String cursor, int limit) {
        if (authorIds == null || authorIds.isEmpty()) {
            return CursorPage.empty();
        }
        CompositeCursor compositeCursor = cursorCodec.decode(cursor);
        List<Post> posts = postRepository.findFeedPosts(
                authorIds,
                viewerId,
                compositeCursor != null ? compositeCursor.createdAt() : null,
                compositeCursor != null ? compositeCursor.id() : null,
                limit + 1
        );
        return mapPage(posts, viewerId, limit);
    }

    @Transactional(readOnly = true)
    public Post findVisiblePost(UUID postId, UUID viewerId) {
        return postRepository.findVisibleById(postId, viewerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found"));
    }

    @Transactional
    public void incrementCommentCount(UUID postId, int delta) {
        postRepository.incrementCommentCount(postId, delta);
    }

    @Transactional(readOnly = true)
    public UUID getPostAuthorId(UUID postId) {
        return postRepository.findById(postId)
                .map(Post::getAuthorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found"));
    }

    private CursorPage<PostResponse> mapPage(List<Post> posts, UUID viewerId, int limit) {
        int count = Math.min(posts.size(), limit);
        List<Post> page = posts.subList(0, count);
        Set<UUID> likedIds = likeService.findLikedPostIds(viewerId, page.stream().map(Post::getId).toList());

        List<PostResponse> items = page.stream()
                .map(p -> toResponse(p, viewerId, likedIds.contains(p.getId())))
                .toList();

        String nextCursor = null;
        if (posts.size() > limit) {
            Post last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getCreatedAt(), last.getId());
        }
        return CursorPage.of(items, nextCursor);
    }

    private PostResponse toResponse(Post post, UUID viewerId, boolean likedByViewer) {
        return new PostResponse(
                post.getId(),
                userService.getUserSummary(post.getAuthorId()),
                post.getContent(),
                post.getMediaUrls(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByViewer,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
