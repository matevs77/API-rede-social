# ADR-002: Cursor composto com comparação de tuplas nativa do PostgreSQL

## Contexto

Os endpoints de listagem de posts (feed, perfil), comentários e notificações precisam de paginação com cursor para garantir desempenho consistente em grandes volumes de dados. A paginação baseada em `OFFSET` degrada-se com o crescimento da tabela porque o PostgreSQL continua a ler e descartar linhas. Era necessário um mecanismo de keyset pagination que usasse os índices existentes e mantivesse latência constante independentemente da profundidade da página.

## Decisão

Adotou-se paginação por **cursor composto `(created_at, id)`** com comparação de tuplas nativa do PostgreSQL. Cada página seguinte é obtida comparando o par ordenado do último elemento da página anterior:

```sql
WHERE (p.created_at, p.id) < (:cursorCreatedAt, :cursorId::uuid)
ORDER BY p.created_at DESC, p.id DESC
LIMIT :limit
```

O cursor é serializado como um objeto JSON (`{"createdAt":"...","id":"..."}`) codificado em Base64 URL-safe sem padding, através da classe `CursorCodec`. A resposta da API inclui o campo `nextCursor` (ou `null` quando não há mais páginas) e um booleano `hasMore`.

## Consequências

- **Positivas**: a comparação de tuplas é resolvida pelo índice composto `(created_at DESC, id DESC)` existente nas tabelas `posts`, `comments` e `notifications`, resultando em consultas com tempo constante independentemente do número de páginas já percorridas; a codificação Base64 URL-safe evita problemas com caracteres especiais em query strings.
- **Negativas**: exige que o cliente passe o cursor exato devolvido na resposta anterior (não permite saltar páginas arbitrariamente); o cursor expõe o timestamp de criação e o ID do último item (informação que, embora não sensível, pode ser considerada um vazamento de implementação).
- **Neutras**: cada recurso paginável repete o mesmo padrão SQL e o mesmo uso de `CursorCodec`, o que é aceite como redundância explícita em vez de uma camada de abstração genérica.

## Alternativas Rejeitadas

- **OFFSET/LIMIT clássico**: rejeitado porque o custo da consulta cresce linearmente com o número de linhas percorridas. Em tabelas com dezenas de milhares de registos, páginas profundas tornam-se lentas.
- **Cursor único com campo `id` apenas (keyset pagination simples)**: rejeitado porque a ordenação por `created_at` permite valores repetidos (vários recursos criados no mesmo instante), o que pode fazer com que elementos sejam saltados ou repetidos entre páginas. O par `(created_at, id)` garante desempate determinístico.
- **GraphQL Connections specification**: rejeitado por complexidade desnecessária — exigiria estrutura `edges/node/cursor` em todas as respostas, quando o modelo atual com `items` e `nextCursor` é suficiente para os consumidores da API.
