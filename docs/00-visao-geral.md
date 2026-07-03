# Visão Geral

## Propósito

A **Rede Social API** é uma API RESTful que fornece funcionalidades completas de uma rede social simplificada: gestão de utilizadores, publicações, seguidores, feed personalizado, mensagens directas e notificações em tempo real.

## Objectivos

- Oferecer uma plataforma de interacção social com perfis públicos/privados
- Entregar um feed de publicações optimizado com paginação por cursor
- Suportar comunicação em tempo real via WebSocket
- Garantir segurança com autenticação JWT e controlo de acesso ao nível da query
- Manter consistência de dados com contadores desnormalizados actualizados atomicamente

## Público-alvo

- Desenvolvedores que queiram integrar uma rede social em aplicações front-end
- Equipas que necessitem de uma API social com capacidades de tempo real

## Requisitos Funcionais

| Funcionalidade       | Descrição                                              |
| -------------------- | ------------------------------------------------------ |
| Registo e Login      | Criação de conta com username único, autenticação JWT  |
| Perfis               | Edição de bio, avatar, localização; perfis privados    |
| Publicações          | Criar, editar, eliminar posts com texto e URLs media   |
| Comentários          | Comentar publicações, editar e eliminar próprios        |
| Gostos (Likes)       | Dar/retirar like em posts e comentários                |
| Seguimento           | Seguir/deixar de seguir; aprovação em perfis privados  |
| Feed                 | Listar posts de quem se segue, ordenado por data       |
| Mensagens Directas   | Chat com histórico, suporta múltiplos participantes    |
| Notificações         | Alertas em tempo real (novo seguidor, like, comentário) |
| Pesquisa             | Pesquisa de utilizadores por username (fuzzy)          |

## Requisitos Não Funcionais

| Requisito            | Estratégia                                            |
| -------------------- | ----------------------------------------------------- |
| Escalabilidade Feed  | Pull model (fan-out on read) com índices compostos    |
| Tempo Real           | WebSocket + STOMP com SimpleBroker                   |
| Privacidade          | Perfis privados com aprovação explícita de seguimento |
| Paginação            | Cursor composto (createdAt, id) para evitar offsets   |
| Performance          | Contadores desnormalizados, actualizações atómicas    |
| Segurança            | JWT, ownership nas queries, isolamento de domínios    |
| Testabilidade        | Testcontainers para testes de integração isolados     |

## Stack Tecnológica

| Tecnologia           | Versão       | Finalidade                         |
| -------------------- | ------------ | ---------------------------------- |
| Java                 | 21           | Runtime                            |
| Spring Boot          | 3.4.4        | Framework Web                      |
| Spring Data JPA      | -            | Acesso a dados PostgreSQL          |
| Spring Security      | -            | Autenticação e autorização         |
| Spring WebSocket     | -            | Suporte STOMP                      |
| Spring Data Redis    | -            | Cache e contadores temporários     |
| PostgreSQL           | 16           | Banco de dados relacional          |
| Flyway               | -            | Migrações de base de dados         |
| Redis                | 7            | Cache, notificações não lidas      |
| JWT (jjwt)           | 0.12.6       | Tokens de acesso e refresh         |
| Lombok               | 1.18.38      | Redução de boilerplate             |
| Springdoc OpenAPI    | 2.8.5        | Documentação automática da API     |
| JUnit 5              | -            | Testes unitários                   |
| Testcontainers       | 1.21.3       | Contâineres PostgreSQL/Redis p/ testes |
| Docker Compose       | -            | Infraestrutura local               |

## Convenções do Projecto

O projecto segue regras rigorosas definidas em `.cursorrules`, documentadas em `docs/05-fluxo-regras.md`:

1. **Isolamento de domínios** — cada domínio acede apenas aos seus próprios Repositories
2. **Autorização nas queries** — toda query que retorna dados visíveis a terceiros recebe `viewerId`
3. **Ownership em UPDATE/DELETE** — a verificação de dono é feita na própria query SQL
4. **Contadores no PostgreSQL** — Redis nunca armazena contadores persistentes
5. **Cursor pagination** — sempre composto `(createdAt, id)` com ordenação descendente
6. **Testes de integração obrigatórios** — com Testcontainers, cobrindo 4 cenários mínimos
