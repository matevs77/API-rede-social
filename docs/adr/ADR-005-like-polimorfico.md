# ADR-005: Modelação polimórfica de Like (target_id / target_type) sem chave estrangeira

## Contexto

O sistema permite que utilizadores expressem "gosto" em dois tipos de recurso: **posts** e **comentários**. Era necessário um modelo único para armazenar estas interações, evitando tabelas separadas (`post_likes`, `comment_likes`) que duplicariam estrutura e lógica.

A base de dados relacional não oferece suporte nativo a chaves estrangeiras polimórficas — uma foreign key só pode apontar para uma tabela específica. Como `target_id` pode referir-se a `posts.id` ou `comments.id`, não é possível declarar uma constraint `FOREIGN KEY (target_id) REFERENCES ...` que cubra ambas.

## Decisão

Modelou-se a entidade `Like` com o padrão **Single Table Inheritance** explícito via coluna discriminadora:

```java
@Entity
@Table(name = "likes")
public class Like {
    @Id private UUID id;
    @Column(name = "user_id") private UUID userId;
    @Column(name = "target_id") private UUID targetId;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private LikeTargetType targetType;  // POST ou COMMENT
    @Column(name = "created_at") private Instant createdAt;
}
```

Não existe qualquer restrição de integridade referencial (`FOREIGN KEY`) nas colunas `target_id` ou `user_id` — a consistência é mantida exclusivamente pela lógica da aplicação.

O custo assumido desta abordagem é a necessidade de **limpeza explícita** de registos órfãos quando o recurso alvo é eliminado. Essa limpeza é feita por queries nativas `DELETE FROM likes WHERE target_type = 'POST' AND target_id = :id` (e equivalente para `COMMENT`), executadas na mesma transação que elimina o recurso principal, antes da eliminação deste:

```java
@Transactional
public void deletePost(UUID postId, UUID authorId) {
    postRepository.deleteLikesByPostId(postId);
    int deleted = postRepository.deleteOwnedPost(postId, authorId);
    if (deleted == 0) throw new ApiException(NOT_FOUND, ...);
}
```

## Consequências

- **Positivas**: uma única tabela e uma única entidade JPA para gerir gostos em posts e comentários; a lógica de toggle (like/unlike) é genérica, parametrizada apenas pelo `targetType`; adicionar um novo tipo de alvo (ex.: `REPLY`) requer apenas estender o enum e adicionar a limpeza correspondente.
- **Negativas**: ausência de integridade referencial a nível da base de dados — é possível (embora a aplicação impeça) inserir um Like com `target_id` de um recurso inexistente; a limpeza de órfãos depende de código aplicacional explícito em cada método de eliminação; não é possível usar `ON DELETE CASCADE` do PostgreSQL.
- **Neutras**: as queries de limpeza são nativas (JPQL não suporta `DELETE` com referência a tabela não mapeada como entidade); a transação garante que, se a eliminação do recurso falhar, os likes não são perdidos.

## Alternativas Rejeitadas

- **Tabelas separadas `post_likes` e `comment_likes`**: rejeitado por duplicação de esquema e lógica de serviço — todos os endpoints de toggle seriam duplicados, e operações como "listar IDs que o utilizador gostou" exigiriam `UNION` entre duas tabelas.
- **Chave estrangeira polimórfica via tabela de ligação (`likes` → `likable` → `posts|comments`)**: rejeitado por complexidade acrescida sem benefício real — a indireção não traria integridade referencial porque a tabela `likable` também seria polimórfica.
- **`@Any` do Hibernate**: rejeitado por ser um mecanismo proprietário, pouco usado e com suporte limitado em ferramentas de migração (Flyway não geraria o esquema correto de forma transparente).
