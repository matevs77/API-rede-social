# Cache e Redis

## Visão Geral

O Redis é utilizado como **cache auxiliar e contador temporário**. Nunca armazena dados persistentes ou contadores principais.

## Configuração

**RedisConfig** (`common/config/RedisConfig.java`):

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

Configuração em `application.yml`:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Em produção (Docker Compose), o host é `redis` (nome do serviço).

## O que o Redis armazena

| Dado                          | Chave                         | Tipo      | TTL           |
| ----------------------------- | ----------------------------- | --------- | ------------- |
| Notificações não lidas        | `unread:{userId}`             | INT       | Indefinido    |
| Rate limiting                 | `rate:{userId}:{endpoint}`    | INT       | Janela (ex: 1min) |
| Cache temporário              | `cache:{entity}:{id}`         | STRING    | Variável      |
| Sessão WebSocket              | (gerido pelo Spring)          | -         | -             |

## O que o Redis NÃO armazena

- `likeCount` (está no PostgreSQL)
- `followerCount` (está no PostgreSQL)
- `followingCount` (está no PostgreSQL)
- `commentCount` (está no PostgreSQL)
- Qualquer contador persistente

## Uso Detalhado

### 1. Contador de Notificações Não Lidas

O `NotificationService` mantém um contador em Redis do número de notificações não lidas por utilizador:

```java
// Ao criar notificação
redisTemplate.opsForValue().increment("unread:" + recipientId);

// Ao marcar como lidas
redisTemplate.opsForValue().decrement("unread:" + userId, count);

// Ao marcar todas como lidas
redisTemplate.delete("unread:" + userId);
```

Este contador evita uma query `COUNT(*)` na base de dados cada vez que o utilizador abre a aplicação.

### 2. Rate Limiting

O Redis é usado para limitar requisições por utilizador:

```java
// Verificar rate limit (ex: 10 requests/min por endpoint)
String key = "rate:" + userId + ":" + endpoint;
Long count = redisTemplate.opsForValue().increment(key);
if (count == 1) {
    redisTemplate.expire(key, 1, TimeUnit.MINUTES);
}
if (count > 10) {
    throw new RateLimitException();
}
```

### 3. Cache Temporário

Para dados consultados frequentemente que mudam com pouca frequência:

- Perfis públicos de utilizadores (cache curto: 5 min)
- Lista de IDs de seguidos para feed (cache ainda não implementado, planeado para perfis com > 1000 seguidores)

## Integração com Docker Compose

```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 5s
    timeout: 5s
    retries: 5
```

## Notas Técnicas

- **RedisTemplate** configurado com `StringRedisSerializer` para simplicidade
- A conexão é configurada via `RedisConnectionFactory` auto-configurado pelo Spring Boot
- Em testes de integração, o Redis é provisionado via **Testcontainers**
- Para produção, considerar Redis Sentinel ou Redis Cluster para alta disponibilidade
