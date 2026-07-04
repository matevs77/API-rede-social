# ADR-001: Arquitectura em pacotes por funcionalidade (package by feature)

## Contexto

A aplicação possui múltiplos domínios de negócio — auth, user, post, comment, like, follow, feed, message, notification, websocket. Cada domínio tem os seus próprios controladores, DTOs de request/response, entidades JPA, repositórios e lógica de serviço. Existiam duas abordagens possíveis para organizar o código-fonte: agrupar por camada técnica (todos os controladores numa pasta `controller/`, todas as entidades numa pasta `entity/`, etc.) ou agrupar por funcionalidade (cada domínio num pacote raiz independente).

## Decisão

Adotou-se **package by feature**. Cada domínio de negócio ocupa um pacote top-level em `com.rede_social_api`, contendo os subpacotes `controller/`, `dto/`, `entity/`, `repository/` e `service/`:

```
com.rede_social_api/
  auth/
    controller/
    dto/
    entity/
    repository/
    service/
  comment/
    controller/
    dto/
    entity/
    repository/
    service/
  post/
    ...
  user/
    ...
  like/
    follow/
    feed/
    message/
    notification/
    websocket/
  common/
    config/
    exception/
    pagination/
    security/
```

Apenas código verdadeiramente transversal (configuração global, tratamento de exceções, paginação com cursor, segurança JWT) reside em `common/`. O domínio `websocket/` também fica num pacote próprio por ser um mecanismo de comunicação distinto.

## Consequências

- **Positivas**: coesão elevada — para adicionar ou modificar uma funcionalidade, o desenvolvedor altera ficheiros dentro de um único pacote; a navegação no IDE é mais rápida; a separação entre domínios é visível na própria estrutura de diretórios; a remoção futura de um domínio corresponde a eliminar um único pacote.
- **Negativas**: duplicação de subpacotes (`controller/`, `dto/`, etc.) em cada domínio; ligeira dificuldade inicial para quem está habituado a package by layer.
- **Neutras**: o isolamento entre domínios é reforçado por convenção — serviços de um domínio não acedem repositórios de outro domínio diretamente, apenas através do Service correspondente (ex.: `CommentService` usa `PostService` para validar visibilidade do post, nunca `PostRepository`).

## Alternativas Rejeitadas

- **Package by layer**: teria criado pastas `controller/`, `service/`, `repository/`, `entity/` na raiz, agrupando todos os artefactos da mesma camada técnica. Rejeitada porque, à medida que o número de domínios cresce, cada pasta tornar-se-ia muito populada e perder-se-ia a noção de pertença a um domínio. Alterações transversais (ex.: adicionar um campo a uma entidade) exigiriam modificar ficheiros em pastas distantes sem relação estrutural entre si.
- **Modularização com multi-módulo Maven**: cada domínio seria um módulo Maven independente. Rejeitada por ser excessiva para a dimensão atual do projeto — aumentaria a complexidade da build, exigiria gestão de versões entre módulos e dificultaria refatorizações rápidas.
