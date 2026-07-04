# ADR-003: Contadores desnormalizados actualizados por UPDATE atómico; Redis reservado ao contador de notificações não lidas

## Contexto

As entidades `Post` e `Comment` expõem campos `likeCount` e `commentCount` que são apresentados em várias respostas da API (detalhe do post, listagem de comentários, feed). Calcular estes valores por `COUNT` em cada leitura seria caro, especialmente em listagens com muitos itens. Era necessário um mecanismo eficiente que evitasse consultas agregadas em cada leitura e, ao mesmo tempo, resolvesse concorrência sem condições de corrida.

Adicionalmente, o sistema de notificações precisa de manter um contador de notificações não lidas por utilizador — um valor efémero, sem necessidade de persistência transacional, mas que deve ser actualizado com frequência.

## Decisão

Adotaram-se duas estratégias distintas consoante a natureza do contador:

**Contadores desnormalizados em SQL (`like_count`, `comment_count`)** — colunas nas tabelas `posts` e `comments` actualizadas exclusivamente por `UPDATE` atómico no SQL, sem ler primeiro o valor atual:

```java
@Modifying
@Query("UPDATE Post p SET p.likeCount = p.likeCount + :delta WHERE p.id = :id")
int incrementLikeCount(@Param("id") UUID id, @Param("delta") int delta);
```

Este padrão elimina condições de corrida (dois incrementos concorrentes nunca perdem um ao outro) e evita o custo de um `SELECT` antes do `UPDATE`.

**Contador de não lidas em Redis** — a chave `unread:<recipientId>` é gerida com operações `INCR`, `DECR` e `DEL` do Redis, sem qualquer persistência em PostgreSQL:

```java
redisTemplate.opsForValue().increment(UNREAD_KEY_PREFIX + recipientId);
```

A escolha por Redis justifica-se pela natureza efémera do contador — não há consequências críticas se um valor for perdido num reinício, e a operação `INCR` é mais leve que um UPDATE transacional.

## Consequências

- **Positivas**: leituras de posts e comentários não necessitam de `JOIN` ou `COUNT` à tabela de likes; a actualização atómica via `SET col = col + delta` é imune a corridas de leitura-escrita; o contador Redis não adiciona pressão à base de dados relacional.
- **Negativas**: os contadores SQL podem divergir do estado real em cenários de erro (ex.: like persistido mas `incrementLikeCount` falha, ou vice-versa — embora ambos estejam na mesma transação na implementação atual); os contadores Redis de notificações são perdidos se Redis for reiniciado sem persistência configurada.
- **Neutras**: qualquer correção de divergência nos contadores SQL exigiria um job de reconciliação periódica (atualmente não implementado).

## Alternativas Rejeitadas

- **Calcular `COUNT` em cada leitura**: rejeitado por impacto no desempenho do feed e listagens — cada resposta teria de executar agregados SQL adicionais, multiplicando o custo por post exibido.
- **Redis para todos os contadores (incluindo likes/comments)**: rejeitado porque a perda destes contadores num reinício do Redis obrigaria a uma reconciliação em massa; além disso, o padrão `INCR` Redis não oferece garantias transacionais quando combinado com a persistência JPA do like.
- **Trigger de base de dados para manter contadores**: rejeitado por manter a lógica de negócio fora do código da aplicação, dificultando a compreensão e a manutenção.
