# ADR-004: Autenticação WebSocket validada no handshake STOMP via HandshakeInterceptor

## Contexto

O sistema envia notificações e mensagens em tempo real através de WebSocket com o subprotocolo STOMP. É necessário garantir que apenas utilizadores autenticados podem estabelecer ligações WebSocket e que a identidade do utilizador fica disponível durante todo o ciclo de vida da sessão para encaminhamento correcto de mensagens para filas pessoais (`/queue/*`).

O mecanismo de autenticação existente na API REST (filtro JWT) não se aplica diretamente ao WebSocket porque o handshake HTTP inicial não passa pelo `JwtAuthenticationFilter` da mesma forma (o WebSocket não envia o token num cabeçalho `Authorization` padronizado em todos os clientes).

## Decisão

A autenticação WebSocket é realizada em duas fases:

1. **Handshake HTTP → WebSocket (`HandshakeInterceptor`)**: durante o upgrade de HTTP para WebSocket, um `HandshakeInterceptor` anónimo extrai o token JWT dos parâmetros da query string (`?token=...`) ou do cabeçalho `Authorization` (via `JwtAuthenticationFilter.extractToken`), valida a assinatura, verifica que não é um refresh token e armazena o `userId` nos atributos da sessão WebSocket. Se o token for inválido ou ausente, o interceptor retorna `false`, rejeitando a ligação.

```java
private HandshakeInterceptor jwtHandshakeInterceptor() {
    return new HandshakeInterceptor() {
        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String token = JwtAuthenticationFilter.extractToken(servletRequest.getServletRequest());
            if (token != null && jwtProvider.isValid(token) && !jwtProvider.isRefreshToken(token)) {
                attributes.put("userId", jwtProvider.extractUserId(token).toString());
                return true;
            }
            return false;
        }
    };
}
```

2. **Canal de entrada STOMP (`ChannelInterceptor`)**: no momento do comando `CONNECT` do STOMP, um `WebSocketAuthChannelInterceptor` lê o `userId` dos atributos da sessão, cria um `AuthenticatedUser` e define a autenticação no `SecurityContextHolder` do Spring, permitindo que os métodos anotados com `@CurrentUser` funcionem em destinos STOMP.

## Consequências

- **Positivas**: a rejeição da ligação WebSocket ocorre o mais cedo possível (antes de qualquer frame STOMP ser processado); o token é validado com o mesmo `JwtProvider` usado na API REST, não duplicando lógica de validação; o `userId` fica disponível nos atributos da sessão durante toda a ligação, permitindo o roteamento correcto para filas `/user/{userId}/queue/*`.
- **Negativas**: o token tem de ser enviado na query string ou num cabeçalho durante o handshake, o que pode não ser suportado por todos os clientes WebSocket; o token JWT no WebSocket não pode ser renovado sem reconexão (a menos que o cliente feche e reabra a ligação com um novo token).
- **Neutras**: a separação em dois interceptors (handshake + canal STOMP) segue a separação de responsabilidades entre o protocolo WebSocket e o subprotocolo STOMP.

## Alternativas Rejeitadas

- **Autenticar apenas no `ChannelInterceptor` STOMP, aceitando qualquer handshake HTTP**: rejeitado porque permitiria que clientes não autenticados mantivessem ligações WebSocket abertas, consumindo recursos do servidor desnecessariamente.
- **Enviar o token num header STOMP personalizado (ex.: `Authorization` no frame `CONNECT`)**: rejeitado porque nem todos os clientes STOMP suportam headers personalizados no comando CONNECT; além disso, a validação ocorreria mais tarde, desperdiçando o recurso do handshake já aceite.
- **Usar o mesmo `JwtAuthenticationFilter` do HTTP para WebSocket**: rejeitado porque o `OncePerRequestFilter` do Spring Security só é executado para pedidos HTTP, não para o upgrade WebSocket, que tem o seu próprio pipeline.
