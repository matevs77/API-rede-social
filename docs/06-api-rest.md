# API REST

## Convenções

- Base URL: `http://localhost:8080/api`
- Formato: JSON
- Autenticação: `Authorization: Bearer <accessToken>`
- Paginação por cursor: parâmetros `cursor` (string opaca) e `limit` (int, default 20, max 100)
- Erros: `ApiErrorResponse { status, error, message, timestamp, path }`

## Endpoints

### Autenticação

| Método | Path              | Auth | Descrição                    |
| ------ | ----------------- | ---- | ---------------------------- |
| POST   | /api/auth/register| Não  | Registar novo utilizador     |
| POST   | /api/auth/login   | Não  | Login (username + password)  |
| POST   | /api/auth/refresh | Não  | Renovar access token         |

### Utilizadores

| Método | Path                        | Auth | Descrição                          |
| ------ | --------------------------- | ---- | ---------------------------------- |
| GET    | /api/users/{username}       | Não* | Ver perfil público                 |
| PUT    | /api/users/me               | Sim  | Editar próprio perfil              |
| GET    | /api/users/search?q={query} | Sim  | Pesquisar utilizadores (pg_trgm)   |
| GET    | /api/users/{id}/followers   | Sim  | Lista de seguidores                |
| GET    | /api/users/{id}/following   | Sim  | Lista de seguidos                  |
| GET    | /api/users/{id}/posts       | Sim  | Publicações de um utilizador       |

*\* Sem auth para perfis públicos, mas viewerId opcional para verificar privacidade.*

### Publicações

| Método | Path               | Auth | Descrição                          |
| ------ | ------------------ | ---- | ---------------------------------- |
| POST   | /api/posts         | Sim  | Criar publicação                   |
| GET    | /api/posts/{id}    | Sim  | Ver publicação                     |
| PUT    | /api/posts/{id}    | Sim  | Editar própria publicação          |
| DELETE | /api/posts/{id}    | Sim  | Eliminar própria publicação        |

### Feed

| Método | Path               | Auth | Descrição                          |
| ------ | ------------------ | ---- | ---------------------------------- |
| GET    | /api/feed          | Sim  | Feed personalizado (paginado)      |

### Comentários

| Método | Path                               | Auth | Descrição                          |
| ------ | ---------------------------------- | ---- | ---------------------------------- |
| POST   | /api/posts/{postId}/comments       | Sim  | Comentar publicação                |
| GET    | /api/posts/{postId}/comments       | Sim  | Listar comentários (paginado)      |
| PUT    | /api/comments/{id}                 | Sim  | Editar próprio comentário          |
| DELETE | /api/comments/{id}                 | Sim  | Eliminar próprio comentário        |

### Gostos

| Método | Path               | Auth | Descrição                          |
| ------ | ------------------ | ---- | ---------------------------------- |
| POST   | /api/likes         | Sim  | Dar/retirar like (toggle)          |

### Seguimento

| Método | Path                            | Auth | Descrição                          |
| ------ | ------------------------------- | ---- | ---------------------------------- |
| POST   | /api/follow/{userId}            | Sim  | Seguir utilizador                  |
| DELETE | /api/follow/{userId}            | Sim  | Deixar de seguir                   |
| PUT    | /api/follow/{followerId}/approve| Sim  | Aprovar pedido de seguimento       |

### Mensagens

| Método | Path                              | Auth | Descrição                          |
| ------ | --------------------------------- | ---- | ---------------------------------- |
| POST   | /api/conversations                | Sim  | Criar conversa                     |
| GET    | /api/conversations                | Sim  | Listar conversas do utilizador     |
| GET    | /api/conversations/{id}           | Sim  | Ver detalhes da conversa           |
| GET    | /api/conversations/{id}/messages  | Sim  | Listar mensagens (paginado)        |
| POST   | /api/messages                     | Sim  | Enviar mensagem                    |

### Notificações

| Método | Path                              | Auth | Descrição                          |
| ------ | --------------------------------- | ---- | ---------------------------------- |
| GET    | /api/notifications                | Sim  | Listar notificações (paginado)     |
| PATCH  | /api/notifications/read           | Sim  | Marcar notificações específicas    |
| PATCH  | /api/notifications/read-all       | Sim  | Marcar todas como lidas            |

## Códigos de Erro

| Status | Significado                    |
| ------ | ------------------------------ |
| 400    | Bad Request (validação)        |
| 401    | Unauthorized (token inválido)  |
| 403    | Forbidden (sem permissão)      |
| 404    | Not Found                      |
| 409    | Conflict (duplicado)           |
| 422    | Unprocessable Entity           |
| 500    | Internal Server Error          |

## Exemplos de Requests

### Registar utilizador

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joaosilva",
    "email": "joao@email.com",
    "password": "Pass123!",
    "displayName": "João Silva"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joaosilva",
    "password": "Pass123!"
  }'
```

### Criar publicação (autenticado)

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{
    "content": "Olá rede social!",
    "mediaUrls": ["https://example.com/foto.jpg"]
  }'
```

### Obter feed com paginação

```bash
curl "http://localhost:8080/api/feed?limit=20" \
  -H "Authorization: Bearer <accessToken>"

# Segunda página (usar nextCursor do response anterior)
curl "http://localhost:8080/api/feed?cursor=<nextCursor>&limit=20" \
  -H "Authorization: Bearer <accessToken>"
```

### Seguir utilizador

```bash
curl -X POST http://localhost:8080/api/follow/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <accessToken>"
```

## Swagger UI

A documentação interactiva está disponível em:

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/api-docs    (OpenAPI spec JSON)
```
