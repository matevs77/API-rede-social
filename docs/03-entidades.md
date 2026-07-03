# Entidades

## Relacionamentos

```
User 1──N Post                          (author)
User 1──N Comment                       (author)
User 1──N Like                          (user)
User 1──N Follow (follower)             (quem segue)
User 1──N Follow (following)            (quem é seguido)
User 1──N Notification (recipient)      (destinatário)
User 1──N Notification (actor)          (quem gerou o evento)
User 1──N ConversationParticipant       (participante)
User 1──N Message (sender)              (remetente)

Post 1──N Comment
Post 1──N Like                          (target)
Post 1──N post_media                    (URLs)

Comment 1──N Like                       (target)

Conversation 1──N ConversationParticipant
Conversation 1──N Message

Notification 1──1 User (recipient)
Notification 1──1 User (actor)
```

## Tabelas

### `users`

| Coluna          | Tipo         | Restrições                       |
| --------------- | ------------ | -------------------------------- |
| id              | UUID PK      |                                  |
| username        | VARCHAR(50)  | NOT NULL, UNIQUE                 |
| email           | VARCHAR(255) | NOT NULL, UNIQUE                 |
| password_hash   | VARCHAR(255) | NOT NULL                         |
| display_name    | VARCHAR(100) | NOT NULL                         |
| bio             | TEXT         | nullable                         |
| avatar_url      | VARCHAR(500) | nullable                         |
| location        | VARCHAR(100) | nullable                         |
| is_private      | BOOLEAN      | DEFAULT FALSE                    |
| follower_count  | INT          | NOT NULL DEFAULT 0               |
| following_count | INT          | NOT NULL DEFAULT 0               |
| created_at      | TIMESTAMPTZ  | NOT NULL                         |
| updated_at      | TIMESTAMPTZ  | NOT NULL                         |

Índices: `idx_users_username_trgm` (GIN com `pg_trgm`) para pesquisa fuzzy.

### `posts`

| Coluna       | Tipo        | Restrições                       |
| ------------ | ----------- | -------------------------------- |
| id           | UUID PK     |                                  |
| author_id    | UUID        | FK → users(id) ON DELETE CASCADE |
| content      | TEXT        | NOT NULL                         |
| like_count   | INT         | NOT NULL DEFAULT 0               |
| comment_count| INT         | NOT NULL DEFAULT 0               |
| created_at   | TIMESTAMPTZ | NOT NULL                         |
| updated_at   | TIMESTAMPTZ | NOT NULL                         |

Índices:
- `idx_posts_author_created_id` → `(author_id, created_at DESC, id DESC)`
- `idx_posts_created_id` → `(created_at DESC, id DESC)` — para feed global

### `post_media`

| Coluna    | Tipo         | Restrições                       |
| --------- | ------------ | -------------------------------- |
| post_id   | UUID         | FK → posts(id) ON DELETE CASCADE |
| media_url | VARCHAR(500) |                                  |

PK composta: `(post_id, media_url)`.

### `comments`

| Coluna     | Tipo        | Restrições                       |
| ---------- | ----------- | -------------------------------- |
| id         | UUID PK     |                                  |
| post_id    | UUID        | FK → posts(id) ON DELETE CASCADE |
| author_id  | UUID        | FK → users(id) ON DELETE CASCADE |
| content    | TEXT        | NOT NULL                         |
| like_count | INT         | NOT NULL DEFAULT 0               |
| created_at | TIMESTAMPTZ | NOT NULL                         |
| updated_at | TIMESTAMPTZ | NOT NULL                         |

Índice: `idx_comments_post_created_id` → `(post_id, created_at DESC, id DESC)`.

### `likes`

| Coluna      | Tipo        | Restrições                       |
| ----------- | ----------- | -------------------------------- |
| id          | UUID PK     |                                  |
| user_id     | UUID        | FK → users(id) ON DELETE CASCADE |
| target_id   | UUID        | NOT NULL                         |
| target_type | VARCHAR(20) | NOT NULL (POST / COMMENT)        |
| created_at  | TIMESTAMPTZ | NOT NULL                         |

Unique: `(user_id, target_type, target_id)` — um like por user/target.
Índice: `idx_likes_target` → `(target_type, target_id)`.

### `follows`

| Coluna       | Tipo        | Restrições                       |
| ------------ | ----------- | -------------------------------- |
| id           | UUID PK     |                                  |
| follower_id  | UUID        | FK → users(id) ON DELETE CASCADE |
| following_id | UUID        | FK → users(id) ON DELETE CASCADE |
| status       | VARCHAR(20) | NOT NULL (PENDING / ACCEPTED)    |
| created_at   | TIMESTAMPTZ | NOT NULL                         |
| updated_at   | TIMESTAMPTZ | NOT NULL                         |

Unique: `(follower_id, following_id)`.
Check: `follower_id <> following_id`.
Índices:
- `idx_follows_follower_status` → `(follower_id, status)`
- `idx_follows_following_status` → `(following_id, status)`

### `notifications`

| Coluna       | Tipo        | Restrições                       |
| ------------ | ----------- | -------------------------------- |
| id           | UUID PK     |                                  |
| recipient_id | UUID        | FK → users(id) ON DELETE CASCADE |
| actor_id     | UUID        | FK → users(id) ON DELETE CASCADE |
| type         | VARCHAR(30) | NOT NULL (enum NotificationType) |
| reference_id | UUID        | nullable (id do recurso alvo)   |
| read         | BOOLEAN     | NOT NULL DEFAULT FALSE           |
| created_at   | TIMESTAMPTZ | NOT NULL                         |

Índice: `idx_notifications_recipient_created_id` → `(recipient_id, created_at DESC, id DESC)`.

### `conversations`

| Coluna     | Tipo        | Restrições                       |
| ---------- | ----------- | -------------------------------- |
| id         | UUID PK     |                                  |
| created_at | TIMESTAMPTZ | NOT NULL                         |
| updated_at | TIMESTAMPTZ | NOT NULL                         |

### `conversation_participants`

| Coluna          | Tipo | Restrições                                    |
| --------------- | ---- | --------------------------------------------- |
| conversation_id | UUID | FK → conversations(id) ON DELETE CASCADE      |
| user_id         | UUID | FK → users(id) ON DELETE CASCADE              |

PK composta: `(conversation_id, user_id)`.
Índice: `idx_conv_participants_user` → `(user_id)`.

### `messages`

| Coluna          | Tipo        | Restrições                                    |
| --------------- | ----------- | --------------------------------------------- |
| id              | UUID PK     |                                               |
| conversation_id | UUID        | FK → conversations(id) ON DELETE CASCADE      |
| sender_id       | UUID        | FK → users(id) ON DELETE CASCADE              |
| content         | TEXT        | NOT NULL                                      |
| sent_at         | TIMESTAMPTZ | NOT NULL                                      |
| read_at         | TIMESTAMPTZ | nullable                                      |

Índice: `idx_messages_conv_sent_id` → `(conversation_id, sent_at DESC, id DESC)`.

## Notas sobre Contadores

Os campos `like_count`, `comment_count`, `follower_count` e `following_count` são **contadores desnormalizados** armazenados nas respectivas entidades. São actualizados atomicamente:

```sql
UPDATE posts SET like_count = like_count + 1 WHERE id = ?;
UPDATE posts SET like_count = like_count - 1 WHERE id = ? AND like_count > 0;
UPDATE users SET follower_count = follower_count + 1 WHERE id = ?;
```

Esta estratégia evita `COUNT(*)` em tempo real para campos de alta frequência, mantendo a consistência através de actualizações atómicas no PostgreSQL.
