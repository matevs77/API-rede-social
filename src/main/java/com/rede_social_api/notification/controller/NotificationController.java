package com.rede_social_api.notification.controller;

import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.notification.dto.request.MarkReadRequest;
import com.rede_social_api.notification.dto.response.NotificationListResponse;
import com.rede_social_api.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponse listNotifications(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        NotificationService.NotificationListResult result =
                notificationService.listNotifications(currentUser.getId(), cursor, limit);
        return new NotificationListResponse(result.items(), result.nextCursor(), result.unreadCount());
    }

    @PatchMapping("/read-all")
    public void markAsRead(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestBody(required = false) MarkReadRequest request) {
        if (request == null || request.notificationIds() == null || request.notificationIds().isEmpty()) {
            notificationService.markAllAsRead(currentUser.getId());
        } else {
            notificationService.markAsRead(currentUser.getId(), request.notificationIds());
        }
    }
}
