CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    bio             TEXT,
    avatar_url      VARCHAR(500),
    location        VARCHAR(100),
    is_private      BOOLEAN      NOT NULL DEFAULT FALSE,
    follower_count  INT          NOT NULL DEFAULT 0,
    following_count INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_users_username_trgm ON users USING gin (username gin_trgm_ops);

CREATE TABLE posts (
    id             UUID PRIMARY KEY,
    author_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content        TEXT         NOT NULL,
    like_count     INT          NOT NULL DEFAULT 0,
    comment_count  INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE TABLE post_media (
    post_id    UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    media_url  VARCHAR(500) NOT NULL,
    PRIMARY KEY (post_id, media_url)
);

CREATE INDEX idx_posts_author_created_id ON posts (author_id, created_at DESC, id DESC);
CREATE INDEX idx_posts_created_id ON posts (created_at DESC, id DESC);

CREATE TABLE comments (
    id         UUID PRIMARY KEY,
    post_id    UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content    TEXT        NOT NULL,
    like_count INT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_comments_post_created_id ON comments (post_id, created_at DESC, id DESC);

CREATE TABLE likes (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id   UUID        NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_likes_user_target UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_likes_target ON likes (target_type, target_id);

CREATE TABLE follows (
    id           UUID PRIMARY KEY,
    follower_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_follows_pair UNIQUE (follower_id, following_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> following_id)
);

CREATE INDEX idx_follows_follower_status ON follows (follower_id, status);
CREATE INDEX idx_follows_following_status ON follows (following_id, status);

CREATE TABLE notifications (
    id           UUID PRIMARY KEY,
    recipient_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(30) NOT NULL,
    reference_id UUID,
    read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notifications_recipient_created_id ON notifications (recipient_id, created_at DESC, id DESC);

CREATE TABLE conversations (
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conv_participants_user ON conversation_participants (user_id);

CREATE TABLE messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content         TEXT        NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL,
    read_at         TIMESTAMPTZ
);

CREATE INDEX idx_messages_conv_sent_id ON messages (conversation_id, sent_at DESC, id DESC);
