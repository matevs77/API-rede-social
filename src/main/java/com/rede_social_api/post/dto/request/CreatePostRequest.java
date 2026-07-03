package com.rede_social_api.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotBlank @Size(max = 5000) String content,
        List<@Size(max = 500) String> mediaUrls
) {}
