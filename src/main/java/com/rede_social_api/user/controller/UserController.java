package com.rede_social_api.user.controller;

import com.rede_social_api.common.security.AuthenticatedUser;
import com.rede_social_api.common.security.CurrentUser;
import com.rede_social_api.user.dto.request.UpdateProfileRequest;
import com.rede_social_api.user.dto.response.UserPublicResponse;
import com.rede_social_api.user.dto.response.UserSearchResponse;
import com.rede_social_api.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserPublicResponse getMyProfile(@CurrentUser AuthenticatedUser currentUser) {
        return userService.getMyProfile(currentUser.getId());
    }

    @PatchMapping("/me")
    public UserPublicResponse updateProfile(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser.getId(), request);
    }

    @GetMapping("/{username}")
    public UserPublicResponse getPublicProfile(
            @PathVariable String username,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        UUID viewerId = currentUser != null ? currentUser.getId() : null;
        return userService.getPublicProfile(username, viewerId);
    }

    @GetMapping("/search")
    public List<UserSearchResponse> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return userService.searchUsers(q, limit);
    }
}
