package com.rede_social_api.message.repository;

import com.rede_social_api.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query(value = """
            SELECT m.* FROM messages m
            JOIN conversation_participants cp ON cp.conversation_id = m.conversation_id
            WHERE m.conversation_id = :conversationId
              AND cp.user_id = :viewerId
              AND (:cursorSentAt IS NULL OR (m.sent_at, m.id) < (:cursorSentAt, CAST(:cursorId AS uuid)))
            ORDER BY m.sent_at DESC, m.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Message> findVisibleByConversation(
            @Param("conversationId") UUID conversationId,
            @Param("viewerId") UUID viewerId,
            @Param("cursorSentAt") Instant cursorSentAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    @Query(value = """
            SELECT m.* FROM messages m
            WHERE m.conversation_id = :conversationId
            ORDER BY m.sent_at DESC, m.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Message> findLastMessage(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query(value = """
            UPDATE messages m SET read_at = CURRENT_TIMESTAMP
            WHERE m.conversation_id = :conversationId
              AND m.sender_id <> :viewerId
              AND m.read_at IS NULL
              AND EXISTS (
                  SELECT 1 FROM conversation_participants cp
                  WHERE cp.conversation_id = m.conversation_id AND cp.user_id = :viewerId
              )
            """, nativeQuery = true)
    int markConversationAsRead(@Param("conversationId") UUID conversationId, @Param("viewerId") UUID viewerId);

    @Query(value = """
            SELECT COUNT(*) FROM messages m
            WHERE m.conversation_id = :conversationId
              AND m.sender_id <> :viewerId
              AND m.read_at IS NULL
            """, nativeQuery = true)
    long countUnread(@Param("conversationId") UUID conversationId, @Param("viewerId") UUID viewerId);
}
