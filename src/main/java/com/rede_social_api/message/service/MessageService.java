package com.rede_social_api.message.service;

import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.common.pagination.CompositeCursor;
import com.rede_social_api.common.pagination.CursorCodec;
import com.rede_social_api.message.dto.request.CreateConversationRequest;
import com.rede_social_api.message.dto.request.SendMessageRequest;
import com.rede_social_api.message.dto.response.ConversationResponse;
import com.rede_social_api.message.dto.response.MessageListResponse;
import com.rede_social_api.message.dto.response.MessageResponse;
import com.rede_social_api.message.entity.Conversation;
import com.rede_social_api.message.entity.ConversationParticipant;
import com.rede_social_api.message.entity.Message;
import com.rede_social_api.message.repository.ConversationParticipantRepository;
import com.rede_social_api.message.repository.ConversationRepository;
import com.rede_social_api.message.repository.MessageRepository;
import com.rede_social_api.notification.entity.NotificationType;
import com.rede_social_api.notification.service.NotificationService;
import com.rede_social_api.user.dto.response.UserSummary;
import com.rede_social_api.user.service.UserService;
import com.rede_social_api.websocket.dto.WsMessagePayload;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CursorCodec cursorCodec;

    public MessageService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            UserService userService,
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate,
            CursorCodec cursorCodec) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public ConversationResponse createConversation(UUID currentUserId, CreateConversationRequest request) {
        if (currentUserId.equals(request.participantId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARTICIPANT", "Cannot create conversation with yourself");
        }

        userService.findById(request.participantId());

        Conversation conversation = conversationRepository
                .findDirectConversation(currentUserId, request.participantId())
                .orElseGet(() -> {
                    Conversation newConversation = conversationRepository.save(Conversation.builder().build());
                    participantRepository.save(ConversationParticipant.builder()
                            .conversationId(newConversation.getId())
                            .userId(currentUserId)
                            .build());
                    participantRepository.save(ConversationParticipant.builder()
                            .conversationId(newConversation.getId())
                            .userId(request.participantId())
                            .build());
                    return newConversation;
                });

        return toConversationResponse(conversation, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UUID currentUserId) {
        return conversationRepository.findByParticipant(currentUserId).stream()
                .map(c -> toConversationResponse(c, currentUserId))
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(UUID conversationId, UUID senderId, SendMessageRequest request) {
        if (!conversationRepository.isParticipant(conversationId, senderId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Not a participant of this conversation");
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(request.content())
                .build();
        message = messageRepository.save(message);

        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        conversation.setUpdatedAt(message.getSentAt());
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(message);
        List<UUID> participants = participantRepository.findUserIdsByConversationId(conversationId);

        for (UUID participantId : participants) {
            if (!participantId.equals(senderId)) {
                notificationService.notify(participantId, senderId, NotificationType.MESSAGE, conversationId);
                messagingTemplate.convertAndSendToUser(
                        participantId.toString(),
                        "/queue/messages",
                        new WsMessagePayload(conversationId, response)
                );
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public MessageListResponse getMessages(UUID conversationId, UUID viewerId, String cursor, int limit) {
        if (!conversationRepository.isParticipant(conversationId, viewerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Not a participant of this conversation");
        }

        CompositeCursor compositeCursor = cursorCodec.decode(cursor);
        List<Message> messages = messageRepository.findVisibleByConversation(
                conversationId,
                viewerId,
                compositeCursor != null ? compositeCursor.createdAt() : null,
                compositeCursor != null ? compositeCursor.id() : null,
                limit + 1
        );

        int count = Math.min(messages.size(), limit);
        List<Message> page = messages.subList(0, count);
        List<MessageResponse> items = new ArrayList<>();
        for (int i = page.size() - 1; i >= 0; i--) {
            items.add(toMessageResponse(page.get(i)));
        }

        String nextCursor = null;
        if (messages.size() > limit) {
            Message last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getSentAt(), last.getId());
        }

        return new MessageListResponse(items, nextCursor);
    }

    @Transactional
    public void markConversationAsRead(UUID conversationId, UUID viewerId) {
        if (!conversationRepository.isParticipant(conversationId, viewerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_PARTICIPANT", "Not a participant of this conversation");
        }
        messageRepository.markConversationAsRead(conversationId, viewerId);
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID viewerId) {
        List<UserSummary> participants = participantRepository.findUserIdsByConversationId(conversation.getId()).stream()
                .map(userService::getUserSummary)
                .toList();

        MessageResponse lastMessage = messageRepository.findLastMessage(conversation.getId())
                .map(this::toMessageResponse)
                .orElse(null);

        long unreadCount = messageRepository.countUnread(conversation.getId(), viewerId);

        return new ConversationResponse(conversation.getId(), participants, lastMessage, unreadCount);
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                userService.getUserSummary(message.getSenderId()),
                message.getContent(),
                message.getSentAt(),
                message.getReadAt()
        );
    }
}
