# DTOs

## Domínio `auth`

### Requests

**RegisterRequest**
```json
{
  "username": "joaosilva",
  "email": "joao@email.com",
  "password": "securePassword123",
  "displayName": "João Silva"
}
```

**LoginRequest**
```json
{
  "username": "joaosilva",
  "password": "securePassword123"
}
```

**RefreshTokenRequest**
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

### Responses

**AuthResponse**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresInMs": 3600000,
  "user": {
    "id": "uuid",
    "username": "joaosilva",
    "displayName": "João Silva",
    "avatarUrl": null
  }
}
```

## Domínio `user`

### Requests

**UpdateProfileRequest**
```json
{
  "displayName": "João Atualizado",
  "bio": "Desenvolvedor de software",
  "avatarUrl": "https://example.com/avatar.jpg",
  "location": "Lisboa",
  "isPrivate": false
}
```

### Responses

**UserPublicResponse**
```json
{
  "id": "uuid",
  "username": "joaosilva",
  "displayName": "João Silva",
  "bio": "Desenvolvedor de software",
  "avatarUrl": "https://example.com/avatar.jpg",
  "location": "Lisboa",
  "isPrivate": false,
  "followerCount": 42,
  "followingCount": 15,
  "createdAt": "2026-01-01T10:00:00Z"
}
```

**UserSearchResponse**
```json
{
  "users": [
    {
      "id": "uuid",
      "username": "joaosilva",
      "displayName": "João Silva",
      "avatarUrl": null
    }
  ],
  "query": "joao"
}
```

**UserSummary**
```json
{
  "id": "uuid",
  "username": "joaosilva",
  "displayName": "João Silva",
  "avatarUrl": null
}
```

## Domínio `post`

### Requests

**CreatePostRequest**
```json
{
  "content": "O meu primeiro post!",
  "mediaUrls": ["https://example.com/foto1.jpg"]
}
```

**UpdatePostRequest**
```json
{
  "content": "Conteúdo actualizado",
  "mediaUrls": ["https://example.com/nova-foto.jpg"]
}
```

### Responses

**PostResponse**
```json
{
  "id": "uuid",
  "content": "O meu primeiro post!",
  "mediaUrls": ["https://example.com/foto1.jpg"],
  "author": {
    "id": "uuid",
    "username": "joaosilva",
    "displayName": "João Silva",
    "avatarUrl": null
  },
  "likeCount": 5,
  "commentCount": 2,
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z",
  "likedByMe": true
}
```

## Domínio `comment`

### Requests

**CreateCommentRequest**
```json
{
  "content": "Excelente post!"
}
```

**UpdateCommentRequest**
```json
{
  "content": "Comentário editado"
}
```

### Responses

**CommentResponse**
```json
{
  "id": "uuid",
  "content": "Excelente post!",
  "author": {
    "id": "uuid",
    "username": "maria",
    "displayName": "Maria",
    "avatarUrl": null
  },
  "postId": "uuid",
  "likeCount": 3,
  "likedByMe": false,
  "createdAt": "2026-01-01T11:00:00Z",
  "updatedAt": "2026-01-01T11:00:00Z"
}
```

## Domínio `like`

### Responses

**LikeToggleResponse**
```json
{
  "liked": true,
  "likeCount": 6,
  "targetId": "uuid",
  "targetType": "POST"
}
```

## Domínio `follow`

### Responses

**FollowActionResponse**
```json
{
  "status": "ACCEPTED",
  "following": true,
  "followerCount": 43
}
```

**FollowResponse** (entrada individual)
```json
{
  "user": {
    "id": "uuid",
    "username": "maria",
    "displayName": "Maria",
    "avatarUrl": null
  },
  "status": "ACCEPTED",
  "createdAt": "2026-01-01T10:30:00Z"
}
```

**FollowListResponse**
```json
{
  "followers": [ /* FollowResponse[] */ ],
  "totalCount": 42
}
```

## Domínio `feed`

### Responses

**FeedResponse** (usa `CursorPage<PostResponse>`)
```json
{
  "items": [ /* PostResponse[] */ ],
  "nextCursor": "base64encoded-cursor-value",
  "hasMore": true
}
```

## Domínio `message`

### Requests

**SendMessageRequest**
```json
{
  "conversationId": "uuid",
  "content": "Olá, tudo bem?"
}
```

**CreateConversationRequest**
```json
{
  "participantIds": ["uuid-do-outro-utilizador"]
}
```

### Responses

**MessageResponse**
```json
{
  "id": "uuid",
  "conversationId": "uuid",
  "sender": {
    "id": "uuid",
    "username": "joaosilva",
    "displayName": "João Silva",
    "avatarUrl": null
  },
  "content": "Olá, tudo bem?",
  "sentAt": "2026-01-01T12:00:00Z",
  "readAt": null
}
```

**MessageListResponse** (usa `CursorPage<MessageResponse>`)
```json
{
  "items": [ /* MessageResponse[] */ ],
  "nextCursor": "base64encoded-cursor-value",
  "hasMore": true
}
```

**ConversationResponse**
```json
{
  "id": "uuid",
  "participants": [ /* UserSummary[] */ ],
  "lastMessage": {
    "content": "Olá, tudo bem?",
    "sentAt": "2026-01-01T12:00:00Z",
    "senderId": "uuid"
  },
  "createdAt": "2026-01-01T10:00:00Z"
}
```

## Domínio `notification`

### Requests

**MarkReadRequest**
```json
{
  "notificationIds": ["uuid1", "uuid2"]
}
```

### Responses

**NotificationResponse**
```json
{
  "id": "uuid",
  "type": "LIKE",
  "actor": {
    "id": "uuid",
    "username": "maria",
    "displayName": "Maria",
    "avatarUrl": null
  },
  "referenceId": "uuid-do-post-or-comentario",
  "read": false,
  "createdAt": "2026-01-01T12:30:00Z"
}
```

**NotificationListResponse** (usa `CursorPage<NotificationResponse>`)
```json
{
  "items": [ /* NotificationResponse[] */ ],
  "nextCursor": "base64encoded-cursor-value",
  "hasMore": true
}
```

## Transversais

**CursorPage** (genérico)
```java
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {}
```

**ApiErrorResponse**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "O campo 'content' é obrigatório",
  "timestamp": "2026-01-01T12:00:00Z",
  "path": "/api/posts"
}
```

**Nota:** O cursor é codificado em Base64 contendo `createdAt` e `id` para paginação segura e consistente.
