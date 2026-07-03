package com.rede_social_api.user.repository;

import com.rede_social_api.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query(value = """
            SELECT u.* FROM users u
            WHERE similarity(u.username, :query) > 0.1
               OR u.username ILIKE CONCAT('%', :query, '%')
            ORDER BY similarity(u.username, :query) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<User> searchByUsername(@Param("query") String query, @Param("limit") int limit);

    @Query(value = """
            SELECT u.* FROM users u
            WHERE u.username = :username
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
    Optional<User> findVisibleProfileByUsername(@Param("username") String username, @Param("viewerId") UUID viewerId);

    @Query(value = """
            SELECT u.* FROM users u
            WHERE u.username = :username
            """, nativeQuery = true)
    Optional<User> findLimitedProfileByUsername(@Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.followerCount = u.followerCount + :delta WHERE u.id = :userId")
    int incrementFollowerCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE User u SET u.followingCount = u.followingCount + :delta WHERE u.id = :userId")
    int incrementFollowingCount(@Param("userId") UUID userId, @Param("delta") int delta);
}
