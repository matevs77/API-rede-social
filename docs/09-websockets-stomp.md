# WebSockets e STOMP

## Visão Geral

A API usa **WebSocket com STOMP** para entregar notificações e mensagens em tempo real. O broker utilizado é o **SimpleBroker** embutido do Spring, sem necessidade de RabbitMQ/ActiveMQ externo.

## Arquitectura

```
┌──────────┐     WebSocket     ┌─────────────────────────────────┐
│  Client  │◄─────────────────►│  WebSocketConfig                │
└──────────┘     STOMP         │  /ws endpoint                   │
                                │  + HandshakeInterceptor (JWT)   │
                                │  + ChannelInterceptor (auth)    │
                                └──────────┬──────────────────────┘
                                           │
                                ┌──────────▼──────────────────────┐
                                │  Message Broker                 │
                                │  /topic  → broadcast            │
                                │  /queue  → user-specific        │
                                │  /user   → resolve to /queue    │
                                └──────────┬──────────────────────┘
                                           │
                                ┌──────────▼──────────────────────┐
                                │  Application                    │
                                │  SimpMessagingTemplate          │
                                │  convertAndSendToUser()         │
                                └─────────────────────────────────┘
```

## Configuração

**WebSocketConfig** (`websocket/config/WebSocketConfig.java`):

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

## Endpoint

- **Connect**: `ws://localhost:8080/ws` (com SockJS fallback)
- **Prefixos de destino**:
  - `/app` — mensagens para o servidor (ex: `/app/chat.send`)
  - `/topic` — tópicos broadcast (não usado actualmente)
  - `/queue` — filas privadas (prefixed com `/user`)
  - `/user` — prefixo para mensagens direccionadas a um utilizador específico

## Canais

| Canal                          | Tipo     | Descrição                          |
| ------------------------------ | -------- | ---------------------------------- |
| `/user/queue/notifications`    | Privado  | Notificações em tempo real         |
| `/user/queue/messages`         | Privado  | Mensagens directas em tempo real   |

## Autenticação

### Handshake

O `jwtHandshakeInterceptor` extrai o token JWT do header `Authorization` (ou parâmetro `token`) durante o handshake HTTP:

```java
String token = JwtAuthenticationFilter.extractToken(servletRequest.getServletRequest());
if (token != null && jwtProvider.isValid(token) && !jwtProvider.isRefreshToken(token)) {
    UUID userId = jwtProvider.extractUserId(token);
    attributes.put("userId", userId.toString());
    return true;
}
return false;
```

### Canal de Inbound

O `WebSocketAuthChannelInterceptor` autentica cada mensagem STOMP no canal de inbound:

1. Obtém o `userId` dos atributos da sessão (definido no handshake)
2. Cria um `AuthenticatedUser` com esse userId
3. Coloca no contexto de segurança do Spring

```java
@MessageMapping("/chat.send")
@SendTo("/queue/messages")  // prefixado com /user pelo template
public void sendMessage(SimpMessageHeaderAccessor headerAccessor, @Payload WsMessagePayload payload) {
    String userId = headerAccessor.getUser().getName();
    // processa a mensagem...
}
```

## Envio de Notificações

O `NotificationService` usa `SimpMessagingTemplate` para enviar notificações em tempo real:

```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

public void notify(UUID recipientId, NotificationType type, UUID referenceId, UUID actorId) {
    // 1. Persiste notificação no banco
    Notification notification = save(recipientId, type, referenceId, actorId);
    
    // 2. Envia via WebSocket
    WsNotificationPayload payload = new WsNotificationPayload(/*...*/);
    messagingTemplate.convertAndSendToUser(
        recipientId.toString(),
        "/queue/notifications",
        payload
    );
    
    // 3. Incrementa contador de não lidas no Redis
    redisTemplate.opsForValue().increment("unread:" + recipientId);
}
```

## Payloads WebSocket

### WsNotificationPayload

```json
{
  "id": "uuid",
  "type": "LIKE",
  "actorId": "uuid",
  "actorUsername": "maria",
  "actorDisplayName": "Maria",
  "referenceId": "uuid-do-post",
  "read": false,
  "createdAt": "2026-01-01T12:30:00Z"
}
```

### WsMessagePayload

```json
{
  "id": "uuid",
  "conversationId": "uuid",
  "senderId": "uuid",
  "senderUsername": "joaosilva",
  "content": "Olá, tudo bem?",
  "sentAt": "2026-01-01T12:00:00Z"
}
```

## Exemplo de Conexão (JavaScript)

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

const headers = {
  Authorization: 'Bearer ' + accessToken
};

stompClient.connect(headers, function(frame) {
  // Subscrever notificações
  stompClient.subscribe('/user/queue/notifications', function(notification) {
    console.log('Notificação:', JSON.parse(notification.body));
  });

  // Subscrever mensagens
  stompClient.subscribe('/user/queue/messages', function(message) {
    console.log('Mensagem:', JSON.parse(message.body));
  });
});
```
