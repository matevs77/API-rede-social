# Arquitectura

## Visão Geral

A aplicação segue uma **arquitectura monolítica modular** com separação estrita por domínios. Cada domínio possui os seus próprios `controller`, `service`, `repository`, `entity` e `dto`. A comunicação entre domínios ocorre exclusivamente através de Services.

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP / WebSocket                         │
├─────────────────────────────────────────────────────────────────┤
│                     Spring Security (JWT)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────────┐  │
│  │  Auth    │ │  User    │ │  Post    │ │  Comment / Like    │  │
│  │  Domain  │ │  Domain  │ │  Domain  │ │  Domain            │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────────┬───────────┘  │
│       │            │            │                 │               │
│  ┌────▼────────────▼────────────▼─────────────────▼───────────┐  │
│  │                    Application Layer                        │  │
│  │         (Services comunicam entre si, nunca Repos)          │  │
│  └─────────────────────────────────────────────────────────────┘  │
│       │            │            │                 │               │
│  ┌────▼────────────▼────────────▼─────────────────▼───────────┐  │
│  │                    Repository Layer                         │  │
│  │          (Apenas o Repository do próprio domínio)           │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │  PostgreSQL   │  │    Redis     │  │  WebSocket (STOMP)   │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Regras de Arquitectura

### 1. Isolamento de Domínios

- Um Service **nunca** acede a um Repository de outro domínio
- Comunicação cross-domain é feita exclusivamente via Service
- Excepção: acesso **read-only** ao `UserRepository` para dados públicos (username, avatar, displayName)

### 2. Camadas

```
Controller  →  Service  →  Repository  →  Database
     │             │
     │             └── outros Services (cross-domain)
     └── DTOs de request/response
```

- **Controllers** — recebem requests, delegam a Services, retornam responses
- **Services** — lógica de negócio, orquestração, comunicação entre domínios
- **Repositories** — acesso a dados com regras de visibilidade incorporadas nas queries
- **Entities** — modelação JPA do banco relacional

### 3. Autorização

A autorização é aplicada em duas camadas:

1. **Spring Security** — verifica se o request tem um JWT válido (endpoints protegidos)
2. **Repository queries** — toda query que retorna dados visíveis a terceiros recebe `viewerId`

```sql
-- Correcto: visibilidade filtrada na query
SELECT p FROM Post p WHERE p.author IN :followedUsers AND p.createdAt < :cursor
ORDER BY p.createdAt DESC, p.id DESC
```

### 4. Ownership

Toda operação de UPDATE ou DELETE em recursos do utilizador valida ownership **dentro da query SQL**, nunca em duas etapas.

```sql
-- Correcto: ownership na própria query
UPDATE posts SET content = :content WHERE id = :id AND author_id = :currentUserId

-- Incorrecto:
-- Post post = postRepository.findById(id);
-- if (!post.getAuthor().equals(currentUser)) throw ...;
-- postRepository.save(post);
```

## Fluxo de Dados do Feed (Pull Model)

```
1. GET /api/feed?cursor=...&limit=20
2. FeedController.findFeed(currentUser, cursor, limit)
3. FeedService.getFeed(currentUserId, cursor, limit)
4. FeedService → FollowService.getFollowedUserIds(currentUserId)
5. FeedService → PostRepository.findFeedPosts(followedIds, cursor, limit)
6. Retorna CursorPage<PostResponse>
```

## Fluxo de Notificações em Tempo Real

```
1. User A dá like no post do User B
2. LikeService.toggleLike(currentUser, targetId, targetType)
3. LikeService → NotificationService.notify(recipientId, type, referenceId)
4. NotificationService.save() + sendToUser(userId, /queue/notifications)
5. User B recebe notificação via WebSocket STOMP
```

## Tecnologias por Camada

| Camada       | Tecnologia                                       |
| ------------ | ------------------------------------------------ |
| HTTP         | Spring Web MVC (RestControllers)                 |
| Segurança    | Spring Security + JWT Filter                     |
| ORM          | Spring Data JPA / Hibernate                      |
| Migrações    | Flyway                                           |
| Cache        | Spring Data Redis (RedisTemplate)                |
| Tempo Real   | Spring WebSocket + STOMP (SimpleBroker)          |
| Documentação | Springdoc OpenAPI (springdoc-openapi-starter-webmvc-ui) |
| Testes       | JUnit 5 + Testcontainers (PostgreSQL + Redis)    |
| Build        | Maven + spring-boot-maven-plugin                 |
