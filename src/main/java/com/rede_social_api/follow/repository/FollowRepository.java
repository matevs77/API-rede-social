package com.rede_social_api.follow.repository;

import com.rede_social_api.follow.entity.Follow;
import com.rede_social_api.follow.entity.FollowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingIdAndStatus(UUID followerId, UUID followingId, FollowStatus status);

    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :followerId AND f.status = 'ACCEPTED'")
    List<UUID> findAcceptedFollowingIds(@Param("followerId") UUID followerId);

    @Modifying
    @Query("""
            UPDATE Follow f SET f.status = 'ACCEPTED', f.updatedAt = CURRENT_TIMESTAMP
            WHERE f.id = :followId AND f.followingId = :currentUserId AND f.status = 'PENDING'
            """)
    int acceptFollowRequest(@Param("followId") UUID followId, @Param("currentUserId") UUID currentUserId);

    @Modifying
    @Query("""
            DELETE FROM Follow f
            WHERE f.followerId = :followerId AND f.followingId = :followingId
            AND f.status = :status
            """)
    int deleteByPairAndStatus(
            @Param("followerId") UUID followerId,
            @Param("followingId") UUID followingId,
            @Param("status") FollowStatus status);

    @Query(value = """
            SELECT u.id, u.username, u.display_name, u.avatar_url, f.created_at, f.id
            FROM follows f
            JOIN users u ON u.id = f.follower_id
            WHERE f.following_id = :userId
              AND f.status = 'ACCEPTED'
              AND (:cursorCreatedAt IS NULL OR (f.created_at, f.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
            ORDER BY f.created_at DESC, f.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findFollowers(
            @Param("userId") UUID userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT u.id, u.username, u.display_name, u.avatar_url, f.created_at, f.id
            FROM follows f
            JOIN users u ON u.id = f.following_id
            WHERE f.follower_id = :userId
              AND f.status = 'ACCEPTED'
              AND (:cursorCreatedAt IS NULL OR (f.created_at, f.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
            ORDER BY f.created_at DESC, f.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findFollowing(
            @Param("userId") UUID userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);
}
