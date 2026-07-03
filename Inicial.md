Projecto 7 — API de Rede Social Simplificada

Stack Core	Spring Boot 3.x · PostgreSQL · Redis · WebSocket · Docker
Tempo Est.	8 a 14 semanas
Diferencial	Feed de publicações, seguimento de utilizadores, mensagens, notificações em tempo real

Introdução ao Projecto
A API de Rede Social Simplificada é o projecto mais ambicioso desta lista. Cobre um domínio de altíssima complexidade: feed personalizado, grafo de seguidores, notificações em tempo real via WebSocket e mensagens directas. Mesmo numa versão simplificada, demonstra conhecimento de arquitectura orientada a eventos, optimização de consultas em grafos relacionais e comunicação bidirecional. Este projecto posiciona o candidato acima da média dos programadores júnior.
Requisitos Funcionais e Não Funcionais
Requisitos Funcionais
    • Perfis: Registo, edição de perfil (bio, avatar, localização), visualização de perfil público.
    • Publicações: Criar posts com texto e imagens (URLs), editar e eliminar os próprios posts.
    • Seguimento: Seguir/deixar de seguir utilizadores; listar seguidores e a quem se segue.
    • Feed: Feed personalizado com publicações de utilizadores seguidos, ordenado por data.
    • Reacções: Dar/retirar 'gosto' em publicações e comentários.
    • Mensagens: Chat directo entre utilizadores com histórico de mensagens.
    • Notificações: Notificações em tempo real (novo seguidor, gosto, comentário, mensagem).
Requisitos Não Funcionais
    • Escalabilidade do Feed: Fan-out on read (pull model) para utilizadores com muitos seguidores.
    • Tempo Real: WebSocket com STOMP para notificações e mensagens.
    • Privacidade: Perfis privados requerem aprovação de seguimento.
    • Paginação de cursor: Feed e mensagens com cursor pagination para consistência.
Arquitectura Geral
    • PostService: CRUD de publicações, cálculo de contadores (gostos, comentários) via Redis.
    • FeedService: Construção do feed via query de posts de utilizadores seguidos com paginação de cursor.
    • NotificationService: Criação e entrega de notificações; push via WebSocket com STOMP.
    • MessageService: Gestão de conversas e mensagens; entrega em tempo real.
    • FollowService: Gestão do grafo de seguimento.
Tecnologias Principais

Spring Boot 3.x		PostgreSQL		Redis		WebSocket

STOMP		Spring Security		JWT		Lombok

JUnit 5		Testcontainers		Docker Compose		Springdoc

Passos de Implementação Detalhados
1. Modelagem de Entidades
    • User: id, username (único), displayName, bio, avatarUrl, isPrivate (boolean).
    • Post: id, author (ManyToOne), content, mediaUrls (ElementCollection), createdAt, likeCount (contador desnormalizado).
    • Follow: id, follower (ManyToOne User), following (ManyToOne User), status (PENDING/ACCEPTED — para perfis privados).
    • Notification: id, recipient (ManyToOne), type (enum: NEW_FOLLOWER, LIKE, COMMENT, MESSAGE), referenceId, read (boolean), createdAt.
    • Message: id, conversation (ManyToOne), sender (ManyToOne), content, sentAt, readAt.
    • Conversation: id, participants (ManyToMany User).
2. Feed Personalizado
    • Pull model: ao solicitar o feed, executar query que busca posts de utilizadores seguidos ordenados por createdAt DESC com cursor.
    • Cursor: o cliente envia o createdAt do último post recebido; a query usa WHERE p.createdAt < :cursor.
    • Índice composto em Post(authorId, createdAt DESC) é essencial para performance do feed.
    • Para utilizadores com muitos seguidores (> 1000), considerar fan-out parcial com cache Redis.
3. WebSocket com STOMP
    • Configurar WebSocketMessageBrokerConfigurer com SimpleBroker para /topic e /queue.
    • Canal /user/queue/notifications — notificações pessoais para um utilizador específico.
    • Canal /user/queue/messages — mensagens directas em tempo real.
    • Autenticar conexões WebSocket via token JWT passado no header de handshake.
4. Notificações
    • Ao ocorrer evento (gosto, comentário, seguimento): criar registo Notification no banco.
    • Enviar via SimpMessagingTemplate.convertAndSendToUser() para o utilizador destinatário.
    • GET /api/notifications — listagem paginada das notificações; PATCH /api/notifications/read-all.
    • Contador de notificações não lidas em Redis (INCR/DECR) para performance.
5. Contadores Desnormalizados
    • Manter likeCount, commentCount e followerCount nas entidades (campos int).
    • Actualizar via UPDATE atómico: UPDATE posts SET like_count = like_count + 1 WHERE id = ?.
    • Evitar COUNT(*) em tempo de execução para campos de alta frequência.
Considerações Avançadas
    • Implementar pesquisa de utilizadores por username com pg_trgm para pesquisa fuzzy.
    • Adicionar Stories (publicações com expiração em 24h) usando job de limpeza @Scheduled.
    • Este projecto é candidato a explorar Kafka ou RabbitMQ para processamento assíncrono de notificações numa fase posterior.
