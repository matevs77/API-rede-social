## Estrutura de Pacotes

Cada domínio de negócio constitui um pacote autónomo sob `com.rede_social_api`,
contendo obrigatoriamente as subpastas:

- `controller/` — classes anotadas com `@RestController`
- `service/` — lógica de negócio
- `repository/` — interfaces `JpaRepository`
- `entity/` — entidades JPA
- `dto/request/` e `dto/response/` — contratos de entrada e saída

Exemplo: o domínio `follow` estrutura-se como
`follow/controller/FollowController.java`, `follow/service/FollowService.java`, etc.

Pacotes transversais (`common/`) alojam apenas código sem pertença a um único
domínio: segurança, paginação, excepções e configuração.

## Sufixos Obrigatórios

| Sufixo | Camada | Exemplo |
|---|---|---|
| `Controller` | Recepção HTTP | `PostController` |
| `Service` | Lógica de negócio | `PostService` |
| `Repository` | Acesso a dados | `PostRepository` |
| `Request` | DTO de entrada | `CreatePostRequest` |
| `Response` | DTO de saída | `PostResponse` |
| `Config` | Configuração Spring | `SecurityConfig` |

## Entidades

- Toda entidade gera o seu próprio `UUID` no método `@PrePersist`, nunca
  delegando essa responsabilidade à base de dados:

```java
  @PrePersist
  void onCreate() {
      if (id == null) {
          id = UUID.randomUUID();
      }
      Instant now = Instant.now();
      createdAt = now;
      updatedAt = now;
  }
```

- Relações entre agregados são representadas por campos `UUID` simples
  (por exemplo, `Post.authorId`), não por `@ManyToOne`. Esta escolha evita
  problemas de N+1 e mantém as entidades desacopladas entre domínios,
  coerente com a regra de isolamento do `.cursorrules`.

- Anotações Lombok utilizadas de forma obrigatória em toda entidade:
  `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.

## Repositórios

- Consultas que devolvem dados potencialmente visíveis a terceiros recebem
  sempre `viewerId` como parâmetro explícito, com a regra de visibilidade
  incorporada na cláusula `WHERE`, nunca filtrada apenas em `Service`.

- Consultas de escrita (`UPDATE`/`DELETE`) sobre recursos pertencentes a um
  utilizador confirmam a posse na própria query, através do padrão
  `*ByIdAndAuthorId` ou equivalente:

```java
  @Modifying
  @Query("UPDATE Post p SET p.content = :content WHERE p.id = :id AND p.authorId = :authorId")
  int updateOwnedPost(...);
```

- Paginação usa sempre cursor composto `(createdAt, id)`, comparado por
  tuplas nativas do PostgreSQL:

```sql
  AND (:cursorCreatedAt IS NULL OR (p.created_at, p.id) < (:cursorCreatedAt, CAST(:cursorId AS uuid)))
  ORDER BY p.created_at DESC, p.id DESC
```

- Consultas com lógica de visibilidade complexa (privacidade, seguimento)
  usam `nativeQuery = true`; consultas simples de escrita usam JPQL.

## DTOs

- Todo DTO é implementado como `record` Java, nunca como classe com
  *setters* mutáveis.
- Validação aplicada directamente nos campos do `record`, com anotações
  Jakarta Bean Validation (`@NotBlank`, `@Size`, etc.).
- Nenhum DTO de resposta expõe `password`, `passwordHash` ou qualquer token.
- Entidades JPA nunca são devolvidas directamente por um `Controller`;
  a conversão para DTO ocorre sempre no `Service`.

## Testes

- Testes de integração residem em `src/test/java/.../integration/`, usam
  Testcontainers (PostgreSQL e Redis reais) e estendem
  `AbstractIntegrationTest`.
- Todo endpoint protegido possui, no mínimo, quatro cenários testados:
  1. acesso pelo dono do recurso;
  2. acesso sem autenticação (`401`);
  3. acesso autenticado por outro utilizador (`403` ou `404`, conforme o
     recurso deva ou não revelar a sua existência);
  4. acesso a recurso inexistente (`404`).
- Nomenclatura de métodos de teste descreve o cenário, não a acção técnica:
  `otherUserCannotDeletePost`, não `testDelete2`.

## Migrações

- Ficheiro nomeado segundo `V{n}__descricao_em_snake_case.sql`.
- Nunca alterar uma migração já submetida ao repositório; qualquer correcção
  gera uma nova migração.
- `ddl-auto` mantém-se sempre como `validate`; o schema é gerido
  exclusivamente pelo Flyway.
- Toda tabela que sirva de base a uma listagem paginada por cursor possui
  índice composto `(coluna_relevante, created_at DESC, id DESC)`. 

## Commits

- Seguem o padrão *Conventional Commits*: `feat:`, `fix:`, `refactor:`,
  `test:`, `docs:`.
- Cada commit corresponde a uma única alteração funcional ou de correcção,
  nunca a um conjunto de mudanças não relacionadas entre si.

# Convenções do Projecto

## 1. Estrutura de Pacotes
## 2. Sufixos Obrigatórios
## 3. Entidades
## 4. Repositórios
## 5. DTOs
## 6. Testes
## 7. Migrações
## 8. Commits