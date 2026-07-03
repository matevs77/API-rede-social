package com.rede_social_api.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String bio,
        @Size(max = 500) String avatarUrl,
        @Size(max = 100) String location,
        Boolean isPrivate
) {}
