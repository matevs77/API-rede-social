package com.rede_social_api.follow.service;

import com.rede_social_api.common.exception.ApiException;
import com.rede_social_api.common.pagination.CompositeCursor;
import com.rede_social_api.common.pagination.CursorCodec;
import com.rede_social_api.follow.dto.response.FollowActionResponse;
import com.rede_social_api.follow.dto.response.FollowListResponse;
import com.rede_social_api.follow.entity.Follow;
import com.rede_social_api.follow.entity.FollowStatus;
import com.rede_social_api.follow.repository.FollowRepository;
import com.rede_social_api.notification.entity.NotificationType;
import com.rede_social_api.notification.service.NotificationService;
import com.rede_social_api.user.dto.response.UserSummary;
import com.rede_social_api.user.entity.User;
import com.rede_social_api.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CursorCodec cursorCodec;

    public FollowService(
            FollowRepository followRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            CursorCodec cursorCodec) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.cursorCodec = cursorCodec;
    }

    @Transactional
    public FollowActionResponse follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF", "Cannot follow yourself");
        }
        if (followRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_FOLLOWING", "Already following this user");
        }

        User target = userRepository.findById(followingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        FollowStatus status = target.isPrivateProfile() ? FollowStatus.PENDING : FollowStatus.ACCEPTED;
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .status(status)
                .build();
        followRepository.save(follow);

        if (status == FollowStatus.ACCEPTED) {
            userRepository.incrementFollowerCount(followingId, 1);
            userRepository.incrementFollowingCount(followerId, 1);
            notificationService.notify(followingId, followerId, NotificationType.NEW_FOLLOWER, follow.getId());
            return new FollowActionResponse(FollowStatus.ACCEPTED, "Now following user");
        }

        notificationService.notify(followingId, followerId, NotificationType.FOLLOW_REQUEST, follow.getId());
        return new FollowActionResponse(FollowStatus.PENDING, "Follow request sent");
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOLLOWING", "Not following this user"));

        followRepository.delete(follow);

        if (follow.getStatus() == FollowStatus.ACCEPTED) {
            userRepository.incrementFollowerCount(followingId, -1);
            userRepository.incrementFollowingCount(followerId, -1);
        }
    }

    @Transactional
    public FollowActionResponse acceptFollowRequest(UUID currentUserId, UUID followId) {
        int updated = followRepository.acceptFollowRequest(followId, currentUserId);
        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FOLLOW_NOT_FOUND", "Follow request not found");
        }

        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FOLLOW_NOT_FOUND", "Follow request not found"));

        userRepository.incrementFollowerCount(follow.getFollowingId(), 1);
        userRepository.incrementFollowingCount(follow.getFollowerId(), 1);
        notificationService.notify(
                follow.getFollowerId(),
                currentUserId,
                NotificationType.FOLLOW_ACCEPTED,
                follow.getId()
        );

        return new FollowActionResponse(FollowStatus.ACCEPTED, "Follow request accepted");
    }

    @Transactional(readOnly = true)
    public List<UUID> getAcceptedFollowingIds(UUID followerId) {
        return followRepository.findAcceptedFollowingIds(followerId);
    }

    @Transactional(readOnly = true)
    public FollowListResponse getFollowers(UUID userId, String cursor, int limit) {
        return mapFollowList(followRepository.findFollowers(
                userId,
                decodeCreatedAt(cursor),
                decodeId(cursor),
                limit + 1
        ), limit);
    }

    @Transactional(readOnly = true)
    public FollowListResponse getFollowing(UUID userId, String cursor, int limit) {
        return mapFollowList(followRepository.findFollowing(
                userId,
                decodeCreatedAt(cursor),
                decodeId(cursor),
                limit + 1
        ), limit);
    }

    private FollowListResponse mapFollowList(List<Object[]> rows, int limit) {
        List<UserSummary> items = new ArrayList<>();
        String nextCursor = null;

        int count = Math.min(rows.size(), limit);
        for (int i = 0; i < count; i++) {
            Object[] row = rows.get(i);
            items.add(new UserSummary(
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3]
            ));
        }

        if (rows.size() > limit) {
            Object[] last = rows.get(limit - 1);
            nextCursor = cursorCodec.encode((Instant) last[4], (UUID) last[5]);
        }

        return new FollowListResponse(items, nextCursor);
    }

    private Instant decodeCreatedAt(String cursor) {
        CompositeCursor decoded = cursorCodec.decode(cursor);
        return decoded != null ? decoded.createdAt() : null;
    }

    private UUID decodeId(String cursor) {
        CompositeCursor decoded = cursorCodec.decode(cursor);
        return decoded != null ? decoded.id() : null;
    }
}
