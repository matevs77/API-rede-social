package com.rede_social_api.notification.service;

import com.rede_social_api.common.pagination.CompositeCursor;
import com.rede_social_api.common.pagination.CursorCodec;
import com.rede_social_api.notification.dto.response.NotificationResponse;
import com.rede_social_api.user.dto.response.UserSummary;
import com.rede_social_api.notification.entity.Notification;
import com.rede_social_api.notification.entity.NotificationType;
import com.rede_social_api.notification.repository.NotificationRepository;
import com.rede_social_api.user.service.UserService;
import com.rede_social_api.websocket.dto.WsNotificationPayload;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final String UNREAD_KEY_PREFIX = "unread:";

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final CursorCodec cursorCodec;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserService userService,
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            CursorCodec cursorCodec) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public void notify(UUID recipientId, UUID actorId, NotificationType type, UUID referenceId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .referenceId(referenceId)
                .read(false)
                .build();
        notification = notificationRepository.save(notification);

        redisTemplate.opsForValue().increment(UNREAD_KEY_PREFIX + recipientId);

        NotificationResponse response = toResponse(notification);
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications",
                new WsNotificationPayload(response)
        );
    }

    @Transactional(readOnly = true)
    public NotificationListResult listNotifications(UUID recipientId, String cursor, int limit) {
        CompositeCursor compositeCursor = cursorCodec.decode(cursor);
        InstantParams params = toInstantParams(compositeCursor);

        List<Notification> fetched = notificationRepository.findByRecipient(
                recipientId, params.createdAt(), params.id(), limit + 1);

        int count = Math.min(fetched.size(), limit);
        List<NotificationResponse> items = fetched.subList(0, count).stream()
                .map(this::toResponse)
                .toList();

        String nextCursor = null;
        if (fetched.size() > limit) {
            Notification last = fetched.get(count - 1);
            nextCursor = cursorCodec.encode(last.getCreatedAt(), last.getId());
        }

        return new NotificationListResult(items, nextCursor, getUnreadCount(recipientId));
    }

    @Transactional(readOnly = true)
    public UserSummary getActorSummary(UUID actorId) {
        return userService.getUserSummary(actorId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        String value = redisTemplate.opsForValue().get(UNREAD_KEY_PREFIX + recipientId);
        return value != null ? Long.parseLong(value) : 0;
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepository.markAllAsRead(recipientId);
        redisTemplate.delete(UNREAD_KEY_PREFIX + recipientId);
    }

    @Transactional
    public void markAsRead(UUID recipientId, List<UUID> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            markAllAsRead(recipientId);
            return;
        }
        int updated = notificationRepository.markAsRead(recipientId, notificationIds);
        if (updated > 0) {
            String key = UNREAD_KEY_PREFIX + recipientId;
            Long current = redisTemplate.opsForValue().decrement(key, updated);
            if (current != null && current < 0) {
                redisTemplate.opsForValue().set(key, "0");
            }
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                userService.getUserSummary(notification.getActorId()),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    private InstantParams toInstantParams(CompositeCursor cursor) {
        if (cursor == null) {
            return new InstantParams(null, null);
        }
        return new InstantParams(cursor.createdAt(), cursor.id());
    }

    private record InstantParams(java.time.Instant createdAt, UUID id) {}

    public record NotificationListResult(
            List<NotificationResponse> items,
            String nextCursor,
            long unreadCount
    ) {}
}
