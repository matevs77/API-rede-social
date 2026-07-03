package com.rede_social_api.like.repository;

import com.rede_social_api.like.entity.Like;
import com.rede_social_api.like.entity.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByUserIdAndTargetTypeAndTargetId(UUID userId, LikeTargetType targetType, UUID targetId);

    @Query("SELECT l.targetId FROM Like l WHERE l.userId = :userId AND l.targetType = :targetType AND l.targetId IN :targetIds")
    List<UUID> findLikedTargetIds(
            @Param("userId") UUID userId,
            @Param("targetType") LikeTargetType targetType,
            @Param("targetIds") List<UUID> targetIds);

    void deleteByUserIdAndTargetTypeAndTargetId(UUID userId, LikeTargetType targetType, UUID targetId);
}
