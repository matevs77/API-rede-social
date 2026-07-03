package com.rede_social_api.notification.repository;

import com.rede_social_api.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(value = """
            SELECT n.* FROM notifications n
            WHERE n.recipient_id = :recipientId
              AND (:cursorCreatedAt IS NULL OR (n.created_at, n.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
            ORDER BY n.created_at DESC, n.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Notification> findByRecipient(
            @Param("recipientId") UUID recipientId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.id IN :ids")
    int markAsRead(@Param("recipientId") UUID recipientId, @Param("ids") List<UUID> ids);
}
