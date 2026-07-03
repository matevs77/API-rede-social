package com.rede_social_api.message.repository;

import com.rede_social_api.message.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query(value = """
            SELECT c.* FROM conversations c
            JOIN conversation_participants cp1 ON cp1.conversation_id = c.id AND cp1.user_id = :userId1
            JOIN conversation_participants cp2 ON cp2.conversation_id = c.id AND cp2.user_id = :userId2
            LIMIT 1
            """, nativeQuery = true)
    Optional<Conversation> findDirectConversation(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    @Query(value = """
            SELECT c.* FROM conversations c
            JOIN conversation_participants cp ON cp.conversation_id = c.id
            WHERE cp.user_id = :userId
            ORDER BY c.updated_at DESC
            """, nativeQuery = true)
    List<Conversation> findByParticipant(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COUNT(*) > 0 FROM conversation_participants cp
            WHERE cp.conversation_id = :conversationId AND cp.user_id = :userId
            """, nativeQuery = true)
    boolean isParticipant(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
