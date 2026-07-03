package com.rede_social_api.post.repository;

import com.rede_social_api.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query(value = """
            SELECT p.* FROM posts p
            JOIN users u ON u.id = p.author_id
            WHERE p.id = :postId
              AND (
                u.is_private = FALSE
                OR u.id = :viewerId
                OR EXISTS (
                    SELECT 1 FROM follows f
                    WHERE f.follower_id = :viewerId
                      AND f.following_id = u.id
                      AND f.status = 'ACCEPTED'
                )
              )
            """, nativeQuery = true)
    Optional<Post> findVisibleById(@Param("postId") UUID postId, @Param("viewerId") UUID viewerId);

    @Query(value = """
            SELECT p.* FROM posts p
            JOIN users u ON u.id = p.author_id
            WHERE p.author_id = :authorId
              AND (
                u.is_private = FALSE
                OR u.id = :viewerId
                OR EXISTS (
                    SELECT 1 FROM follows f
                    WHERE f.follower_id = :viewerId
                      AND f.following_id = u.id
                      AND f.status = 'ACCEPTED'
                )
              )
              AND (:cursorCreatedAt IS NULL OR (p.created_at, p.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findVisibleByAuthor(
            @Param("authorId") UUID authorId,
            @Param("viewerId") UUID viewerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT p.* FROM posts p
            JOIN users u ON u.id = p.author_id
            WHERE p.author_id IN (:authorIds)
              AND (
                u.is_private = FALSE
                OR u.id = :viewerId
                OR EXISTS (
                    SELECT 1 FROM follows f
                    WHERE f.follower_id = :viewerId
                      AND f.following_id = u.id
                      AND f.status = 'ACCEPTED'
                )
              )
              AND (:cursorCreatedAt IS NULL OR (p.created_at, p.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> findFeedPosts(
            @Param("authorIds") List<UUID> authorIds,
            @Param("viewerId") UUID viewerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    @Modifying
    @Query("UPDATE Post p SET p.content = :content, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.authorId = :authorId")
    int updateOwnedPost(@Param("id") UUID id, @Param("authorId") UUID authorId, @Param("content") String content);

    @Modifying
    @Query("DELETE FROM Post p WHERE p.id = :id AND p.authorId = :authorId")
    int deleteOwnedPost(@Param("id") UUID id, @Param("authorId") UUID authorId);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + :delta WHERE p.id = :id")
    int incrementLikeCount(@Param("id") UUID id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + :delta WHERE p.id = :id")
    int incrementCommentCount(@Param("id") UUID id, @Param("delta") int delta);
}
