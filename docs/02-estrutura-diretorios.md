# Estrutura de Diretórios

```
rede-social-api/
│
├── pom.xml                          # Configuração Maven (Spring Boot 3.4.4)
├── Dockerfile                       # Multi-stage build (J21 alpine)
├── docker-compose.yml               # PostgreSQL + Redis + API
├── .gitignore
├── .cursorrules                     # Regras de arquitectura para assistente IA
├── Inicial.md                       # Documento original dos requisitos
│
├── src/
│   ├── main/
│   │   ├── java/com/rede_social_api/
│   │   │   │
│   │   │   ├── RedeSocialApiApplication.java    # Entry point @SpringBootApplication
│   │   │   │
│   │   │   ├── auth/                            # Domínio: Autenticação
│   │   │   │   ├── controller/AuthController.java
│   │   │   │   ├── service/AuthService.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/
│   │   │   │       │   ├── RegisterRequest.java
│   │   │   │       │   ├── LoginRequest.java
│   │   │   │       │   └── RefreshTokenRequest.java
│   │   │   │       └── response/
│   │   │   │           └── AuthResponse.java
│   │   │   │
│   │   │   ├── user/                            # Domínio: Utilizadores
│   │   │   │   ├── controller/UserController.java
│   │   │   │   ├── service/UserService.java
│   │   │   │   ├── repository/UserRepository.java
│   │   │   │   ├── entity/User.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/UpdateProfileRequest.java
│   │   │   │       └── response/
│   │   │   │           ├── UserPublicResponse.java
│   │   │   │           ├── UserSearchResponse.java
│   │   │   │           └── UserSummary.java
│   │   │   │
│   │   │   ├── post/                            # Domínio: Publicações
│   │   │   │   ├── controller/PostController.java
│   │   │   │   ├── service/PostService.java
│   │   │   │   ├── repository/PostRepository.java
│   │   │   │   ├── entity/Post.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/CreatePostRequest.java
│   │   │   │       ├── request/UpdatePostRequest.java
│   │   │   │       └── response/PostResponse.java
│   │   │   │
│   │   │   ├── comment/                         # Domínio: Comentários
│   │   │   │   ├── controller/CommentController.java
│   │   │   │   ├── service/CommentService.java
│   │   │   │   ├── repository/CommentRepository.java
│   │   │   │   ├── entity/Comment.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/CreateCommentRequest.java
│   │   │   │       ├── request/UpdateCommentRequest.java
│   │   │   │       └── response/CommentResponse.java
│   │   │   │
│   │   │   ├── like/                            # Domínio: Gostos
│   │   │   │   ├── controller/LikeController.java
│   │   │   │   ├── service/LikeService.java
│   │   │   │   ├── repository/LikeRepository.java
│   │   │   │   ├── entity/Like.java
│   │   │   │   ├── entity/LikeTargetType.java
│   │   │   │   └── dto/response/LikeToggleResponse.java
│   │   │   │
│   │   │   ├── follow/                          # Domínio: Seguimento
│   │   │   │   ├── controller/FollowController.java
│   │   │   │   ├── service/FollowService.java
│   │   │   │   ├── repository/FollowRepository.java
│   │   │   │   ├── entity/Follow.java
│   │   │   │   ├── entity/FollowStatus.java
│   │   │   │   └── dto/response/
│   │   │   │       ├── FollowActionResponse.java
│   │   │   │       ├── FollowResponse.java
│   │   │   │       └── FollowListResponse.java
│   │   │   │
│   │   │   ├── feed/                            # Domínio: Feed
│   │   │   │   ├── controller/FeedController.java
│   │   │   │   ├── service/FeedService.java
│   │   │   │   └── dto/response/FeedResponse.java
│   │   │   │
│   │   │   ├── message/                         # Domínio: Mensagens
│   │   │   │   ├── controller/MessageController.java
│   │   │   │   ├── service/MessageService.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── MessageRepository.java
│   │   │   │   │   ├── ConversationRepository.java
│   │   │   │   │   └── ConversationParticipantRepository.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Message.java
│   │   │   │   │   ├── Conversation.java
│   │   │   │   │   └── ConversationParticipant.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/
│   │   │   │       │   ├── SendMessageRequest.java
│   │   │   │       │   └── CreateConversationRequest.java
│   │   │   │       └── response/
│   │   │   │           ├── MessageResponse.java
│   │   │   │           ├── MessageListResponse.java
│   │   │   │           └── ConversationResponse.java
│   │   │   │
│   │   │   ├── notification/                    # Domínio: Notificações
│   │   │   │   ├── controller/NotificationController.java
│   │   │   │   ├── service/NotificationService.java
│   │   │   │   ├── repository/NotificationRepository.java
│   │   │   │   ├── entity/Notification.java
│   │   │   │   ├── entity/NotificationType.java
│   │   │   │   └── dto/
│   │   │   │       ├── request/MarkReadRequest.java
│   │   │   │       └── response/
│   │   │   │           ├── NotificationResponse.java
│   │   │   │           └── NotificationListResponse.java
│   │   │   │
│   │   │   ├── websocket/                       # Domínio: WebSocket
│   │   │   │   ├── config/
│   │   │   │   │   ├── WebSocketConfig.java
│   │   │   │   │   └── WebSocketAuthChannelInterceptor.java
│   │   │   │   └── dto/
│   │   │   │       ├── WsNotificationPayload.java
│   │   │   │       └── WsMessagePayload.java
│   │   │   │
│   │   │   └── common/                          # Domínio: Transversal
│   │   │       ├── config/
│   │   │       │   ├── JpaConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── SecurityConfig.java
│   │   │       ├── security/
│   │   │       │   ├── JwtProvider.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── AuthenticatedUser.java
│   │   │       │   └── CurrentUser.java
│   │   │       ├── pagination/
│   │   │       │   ├── CursorPage.java
│   │   │       │   ├── CompositeCursor.java
│   │   │       │   └── CursorCodec.java
│   │   │       └── exception/
│   │   │           ├── ApiException.java
│   │   │           ├── ApiErrorResponse.java
│   │   │           └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                  # Config principal
│   │       ├── application-dev.yml              # Perfil de desenvolvimento
│   │       └── db/migration/
│   │           └── V1__init.sql                 # Migração inicial Flyway
│   │
│   └── test/
│       ├── java/com/rede_social_api/
│       │   ├── integration/                     # Testes de integração
│       │   │   ├── AbstractIntegrationTest.java
│       │   │   ├── AuthUserIntegrationTest.java
│       │   │   ├── PostIntegrationTest.java
│       │   │   ├── FeedIntegrationTest.java
│       │   │   ├── FollowIntegrationTest.java
│       │   │   ├── CommentLikeIntegrationTest.java
│       │   │   ├── MessageNotificationIntegrationTest.java
│       │   │   └── PostgresRedisContainers.java
│       │   └── support/
│       │       └── TestFixtures.java
│       │
│       └── resources/
│           └── application-test.yml             # Perfil de teste
│
└── docs/
    ├── 00-visao-geral.md
    ├── 01-arquitectura.md
    ├── 02-estrutura-diretorios.md
    ├── 03-entidades.md
    ├── 04-DTO.md
    ├── 05-fluxo-regras.md
    ├── 06-api-rest.md
    ├── 07-persistencia-flyway.md
    ├── 08-seguranca-jwt.md
    ├── 09-websockets-stomp.md
    ├── 10-cache-redis.md
    ├── 11-observabilidade-actuator.md
    ├── 12-docker-deploy.md
    └── adr/
        ├── adr-01
        ├── adr-02
        ├── adr-03
        ├── adr-04
        └── adr-05
```
