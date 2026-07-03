# Persistência e Flyway

## Tecnologia

- **Base de Dados**: PostgreSQL 16
- **ORM**: Spring Data JPA / Hibernate
- **Migrações**: Flyway
- **DDL**: `validate` (Hibernate apenas valida o schema existente; Flyway gere as mudanças)

## Configuração

Em `application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Hibernate não cria/altera tabelas
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Migrações

As migrações estão em `src/main/resources/db/migration/` com o formato `V{número}__{descricao}.sql`.

### V1__init.sql

A migração inicial cria:

1. **Extensão `pg_trgm`** — para pesquisa fuzzy de usernames com índices GIN
2. **Tabelas** — users, posts, post_media, comments, likes, follows, notifications, conversations, conversation_participants, messages
3. **Índices** — todos os índices compostos necessários para performance
4. **Constraints** — UNIQUE, CHECK, Foreign Keys com ON DELETE CASCADE

### Índices Principais

| Índice                                    | Tabela     | Colunas                              | Propósito                        |
| ----------------------------------------- | ---------- | ------------------------------------ | -------------------------------- |
| `idx_users_username_trgm`                 | users      | `username gin_trgm_ops`              | Pesquisa fuzzy de username       |
| `idx_posts_author_created_id`             | posts      | `(author_id, created_at DESC, id)`   | Posts de um autor (paginado)     |
| `idx_posts_created_id`                    | posts      | `(created_at DESC, id)`              | Feed global                      |
| `idx_comments_post_created_id`            | comments   | `(post_id, created_at DESC, id)`     | Comentários de um post           |
| `idx_likes_target`                        | likes      | `(target_type, target_id)`           | Likes de um target               |
| `idx_follows_follower_status`             | follows    | `(follower_id, status)`              | Quem um user segue               |
| `idx_follows_following_status`            | follows    | `(following_id, status)`             | Seguidores de um user            |
| `idx_notifications_recipient_created_id`  | notific.   | `(recipient_id, created_at DESC, id)`| Notificações de um user          |
| `idx_conv_participants_user`              | conv_part. | `(user_id)`                          | Conversas de um user             |
| `idx_messages_conv_sent_id`               | messages   | `(conversation_id, sent_at DESC, id)`| Mensagens de uma conversa        |

## Estratégia de Contadores

Os contadores desnormalizados (`like_count`, `comment_count`, `follower_count`, `following_count`) são actualizados atomicamente via UPDATE directo:

```sql
-- Incrementar like_count de um post
UPDATE posts SET like_count = like_count + 1 WHERE id = :postId;

-- Incrementar follower_count do utilizador alvo
UPDATE users SET follower_count = follower_count + 1 WHERE id = :targetUserId;
```

**Benefícios**:
- Evita `COUNT(*)` em tempo real em tabelas grandes
- Operação atómica (thread-safe a nível de base de dados)
- Sem necessidade de locks pessimistas ou optimistas

## Considerações de Performance

- O índice `idx_posts_author_created_id` é essencial para o feed pull model (busca de posts de utilizadores seguidos ordenados por data)
- O índice `idx_messages_conv_sent_id` garante paginação eficiente do histórico de conversas
- As constraints UNIQUE em `likes` e `follows` previnem duplicados a nível de banco
- `ON DELETE CASCADE` garante consistência referencial sem necessidade de limpeza manual
