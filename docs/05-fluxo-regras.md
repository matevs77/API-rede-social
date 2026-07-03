# Fluxos e Regras de Negócio

## Fluxos Principais

### 1. Registo e Autenticação

```
[Registo]
  1. POST /api/auth/register { username, email, password, displayName }
  2. AuthService.register()
     - Valida unicidade de username e email
     - Hash da password (BCrypt)
     - Cria utilizador no banco
     - Gera access token + refresh token
  3. Retorna AuthResponse

[Login]
  1. POST /api/auth/login { username, password }
  2. AuthService.login()
     - Verifica credenciais
     - Gera novo par de tokens
  3. Retorna AuthResponse

[Refresh]
  1. POST /api/auth/refresh { refreshToken }
  2. AuthService.refresh()
     - Valida refresh token
     - Gera novo access token + novo refresh token
     - Invalida refresh token anterior
  3. Retorna AuthResponse
```

### 2. Gestão de Perfil

```
[Editar Perfil]
  1. PUT /api/users/me { displayName, bio, avatarUrl, location, isPrivate }
  2. UserService.updateProfile(currentUserId, request)
     - Apenas o próprio pode editar
     - Actualiza campos fornecidos
  3. Retorna UserPublicResponse

[Ver Perfil Público]
  1. GET /api/users/{username}
  2. UserService.getPublicProfile(username, currentUserId)
     - Se perfil privado e não é seguidor aprovado → dados limitados
  3. Retorna UserPublicResponse

[Pesquisar Utilizadores]
  1. GET /api/users/search?q={query}
  2. UserRepository.findByUsernameContainingIgnoreCase(query)
     - Usa índice GIN com pg_trgm para fuzzy search
  3. Retorna UserSearchResponse
```

### 3. Publicações

```
[Criar Post]
  1. POST /api/posts { content, mediaUrls }
  2. PostService.createPost(currentUser, request)
     - Cria post com autor = currentUser
  3. Retorna PostResponse

[Editar Post]
  1. PUT /api/posts/{id} { content, mediaUrls }
  2. PostService.updatePost(postId, currentUserId, request)
     - Query: UPDATE posts SET ... WHERE id = ? AND author_id = ?
     - Se 0 linhas afectadas → 404 ou 403
  3. Retorna PostResponse

[Eliminar Post]
  1. DELETE /api/posts/{id}
  2. PostService.deletePost(postId, currentUserId)
     - Query: DELETE FROM posts WHERE id = ? AND author_id = ?
     - Se 0 linhas afectadas → 404 ou 403

[Listar Posts de um Utilizador]
  1. GET /api/users/{userId}/posts?cursor=...&limit=20
  2. PostRepository.findVisiblePostsByAuthor(authorId, viewerId, cursor, limit)
     - Se perfil privado e não autorizado → 0 resultados
  3. Retorna CursorPage<PostResponse>
```

### 4. Feed Personalizado

```
[Obter Feed]
  1. GET /api/feed?cursor=...&limit=20
  2. FeedService.getFeed(currentUserId, cursor, limit)
  3. FeedService → FollowService.getFollowedUserIds(currentUserId)
     - Busca IDs de utilizadores seguidos com status ACCEPTED
  4. FeedService → PostRepository.findFeedPosts(followedIds, cursor, limit)
     - Query com composite cursor pagination
     - Ordenação: created_at DESC, id DESC
  5. Retorna CursorPage<PostResponse>
```

**Pull Model**: O feed é montado no momento da leitura. Para utilizadores com muitos seguidores (> 1000), considerar fan-out parcial com cache Redis.

### 5. Gostos (Likes)

```
[Toggle Like]
  1. POST /api/likes { targetId, targetType }
  2. LikeService.toggleLike(currentUserId, targetId, targetType)
     - Se já existe like → remove + decrementa contador
     - Se não existe → cria + incrementa contador
     - Contador actualizado atomicamente:
       UPDATE posts SET like_count = like_count + 1 WHERE id = ?
  3. Se new like → NotificationService.notify(recipient, LIKE, ...)
  4. Retorna LikeToggleResponse
```

### 6. Comentários

```
[Criar Comentário]
  1. POST /api/posts/{postId}/comments { content }
  2. CommentService.createComment(postId, currentUserId, request)
     - Incrementa comment_count do post atomicamente
     - NotificationService.notify(authorPost, COMMENT, ...)
  3. Retorna CommentResponse

[Editar Comentário]
  1. PUT /api/comments/{id} { content }
  2. CommentService.updateComment(commentId, currentUserId, request)
     - UPDATE comments SET ... WHERE id = ? AND author_id = ?

[Eliminar Comentário]
  1. DELETE /api/comments/{id}
  2. CommentService.deleteComment(commentId, currentUserId)
     - DELETE FROM comments WHERE id = ? AND author_id = ?
     - Decrementa comment_count do post atomicamente
```

### 7. Seguimento

```
[Seguir Utilizador]
  1. POST /api/follow/{targetUserId}
  2. FollowService.follow(currentUserId, targetUserId)
     - Se perfil público → status = ACCEPTED
     - Se perfil privado → status = PENDING (aguarda aprovação)
     - Incrementa following_count do seguidor
     - Incrementa follower_count do alvo (só se ACCEPTED)
     - NotificationService.notify(targetUser, NEW_FOLLOWER, ...)
  3. Retorna FollowActionResponse

[Deixar de Seguir]
  1. DELETE /api/follow/{targetUserId}
  2. FollowService.unfollow(currentUserId, targetUserId)
     - Remove o registo de follow
     - Decrementa contadores

[Aprovar Seguimento (perfil privado)]
  1. PUT /api/follow/{followerId}/approve
  2. FollowService.approveFollow(targetUserId, followerId)
     - UPDATE follows SET status = 'ACCEPTED' WHERE ...
     - Incrementa follower_count do target
     - Incrementa following_count do follower

[Listar Seguidores / Seguidos]
  1. GET /api/users/{userId}/followers?status=ACCEPTED
  2. GET /api/users/{userId}/following?status=ACCEPTED
  3. FollowRepository.findByFollower/Following com filtro de status
```

### 8. Mensagens Directas

```
[Criar Conversa]
  1. POST /api/conversations { participantIds }
  2. MessageService.createConversation(currentUserId, participantIds)
     - Cria conversa com participantes
     - Verifica se conversa já existe entre os mesmos utilizadores
  3. Retorna ConversationResponse

[Enviar Mensagem]
  1. POST /api/messages { conversationId, content }
  2. MessageService.sendMessage(currentUserId, conversationId, request)
     - Verifica se o utilizador é participante da conversa
     - Cria mensagem
     - Envia via WebSocket (SimpMessagingTemplate)
     - NotificationService.notify(participants, MESSAGE, ...)
  3. Retorna MessageResponse

[Listar Mensagens]
  1. GET /api/conversations/{id}/messages?cursor=...&limit=50
  2. MessageRepository.findByConversation(conversationId, cursor, limit)
  3. Retorna MessageListResponse (CursorPage)

[Listar Conversas]
  1. GET /api/conversations
  2. MessageService.getUserConversations(currentUserId)
```

### 9. Notificações

```
[Listar Notificações]
  1. GET /api/notifications?cursor=...&limit=20
  2. NotificationRepository.findByRecipient(recipientId, cursor, limit)

[Marcar como Lidas]
  1. PATCH /api/notifications/read-all
  2. NotificationService.markAllRead(currentUserId)
     - UPDATE notifications SET read = true WHERE recipient_id = ?
     - Decrementa contador no Redis

[Marcar Específicas como Lidas]
  1. PATCH /api/notifications/read { notificationIds }
  2. NotificationService.markRead(currentUserId, notificationIds)
```

## Regras de Negócio

### Regra 1: Isolamento de Domínios
- Nenhum Service acede a Repository de outro domínio
- Excepção: UserRepository para leitura de dados públicos
- Cross-domain sempre através do Service correspondente

### Regra 2: Autorização nas Queries
- Toda query que retorna dados visíveis a terceiros recebe `viewerId`
- A filtragem de visibilidade é feita dentro da query JPA/SQL
- Nunca filtrar permissões apenas na camada Service

### Regra 3: Ownership em UPDATE/DELETE
- UPDATE/DELETE em recursos do utilizador verificam ownership na própria query
- Se 0 linhas afectadas → recurso não encontrado ou não autorizado

### Regra 4: Contadores no PostgreSQL
- Contadores persistentes vivem apenas no PostgreSQL
- Actualizados atomicamente: `UPDATE ... SET count = count + 1 WHERE ...`
- Redis nunca armazena likeCount, followerCount, etc.

### Regra 5: Cursor Pagination
- Paginação sempre com cursor composto `(createdAt, id)`
- Ordenação: `ORDER BY created_at DESC, id DESC`
- Nunca paginar apenas por timestamp

### Regra 6: Privacidade de Perfis
- Perfis públicos: qualquer um pode seguir (status ACCEPTED imediato)
- Perfis privados: seguimento fica PENDING até aprovação do dono
- Conteúdo de perfil privado só visível para seguidores aprovados

### Regra 7: Testes
- Funcionalidade não está completa sem testes de integração
- Testes com Testcontainers (PostgreSQL + Redis)
- Por endpoint protegido: owner access, unauthorized, outro user, not found
