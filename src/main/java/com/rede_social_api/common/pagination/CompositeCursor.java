package com.rede_social_api.common.pagination;

import java.time.Instant;
import java.util.UUID;

public record CompositeCursor(Instant createdAt, UUID id) {

    public static CompositeCursor of(Instant createdAt, UUID id) {
        return new CompositeCursor(createdAt, id);
    }
}
