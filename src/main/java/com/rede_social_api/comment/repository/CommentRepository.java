package com.rede_social_api.comment.repository;

import com.rede_social_api.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query(value = """
            SELECT c.* FROM comments c
            JOIN posts p ON p.id = c.post_id
            JOIN users u ON u.id = p.author_id
            WHERE c.post_id = :postId
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
              AND (:cursorCreatedAt IS NULL OR (c.created_at, c.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Comment> findVisibleByPost(
            @Param("postId") UUID postId,
            @Param("viewerId") UUID viewerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT c.* FROM comments c
            JOIN posts p ON p.id = c.post_id
            JOIN users u ON u.id = p.author_id
            WHERE c.id = :commentId
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
    Optional<Comment> findVisibleById(@Param("commentId") UUID commentId, @Param("viewerId") UUID viewerId);

    @Modifying
    @Query("UPDATE Comment c SET c.content = :content, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id AND c.authorId = :authorId")
    int updateOwnedComment(@Param("id") UUID id, @Param("authorId") UUID authorId, @Param("content") String content);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.id = :id AND c.authorId = :authorId")
    int deleteOwnedComment(@Param("id") UUID id, @Param("authorId") UUID authorId);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + :delta WHERE c.id = :id")
    int incrementLikeCount(@Param("id") UUID id, @Param("delta") int delta);

    @Modifying
    @Query(value = "DELETE FROM likes WHERE target_type = 'COMMENT' AND target_id = :id", nativeQuery = true)
    int deleteLikesByCommentId(@Param("id") UUID id);
}
