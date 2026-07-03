# Rede Social API

API RESTful de uma rede social simplificada com feed personalizado, mensagens diretas e notificações em tempo real.

## Stack

| Camada            | Tecnologia                                   |
| ----------------- | -------------------------------------------- |
| Runtime           | Java 21                                      |
| Framework         | Spring Boot 3.4.4                            |
| Build             | Maven                                        |
| Banco de Dados    | PostgreSQL 16 + Flyway (migrações)           |
| Cache & Auxiliar  | Redis 7                                      |
| Tempo Real        | WebSocket + STOMP                            |
| Autenticação      | JWT (access token + refresh token)           |
| Documentação API  | Springdoc OpenAPI (Swagger UI)               |
| Testes            | JUnit 5 + Testcontainers                     |
| Infraestrutura    | Docker Compose                               |

## Pré-requisitos

- Java 21+
- Docker & Docker Compose
- Maven (opcional, pode usar o wrapper)

## Quick Start

```bash
# Iniciar dependências (PostgreSQL + Redis)
docker compose up -d postgres redis

# Compilar e executar
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Ou tudo com Docker
docker compose up --build
```

A API estará disponível em `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI spec: `http://localhost:8080/api-docs`

## Ambientes

| Perfil     | Uso                  | Ficheiro de configuração       |
| ---------- | -------------------- | ------------------------------ |
| `dev`      | Desenvolvimento local | `application-dev.yml`          |
| `test`     | Testes automatizados | `application-test.yml`         |
| (default)  | Produção             | `application.yml`              |

## Projecto base

Este projecto foi desenvolvido como **Projecto 7 — API de Rede Social Simplificada**, com os seguintes requisitos:

### Funcionais

- Perfis: registo, edição (bio, avatar, localização), visualização pública
- Publicações: criar posts com texto e URLs de media, editar/eliminar
- Seguimento: seguir/deixar de seguir, listar seguidores, aprovação em perfis privados
- Feed personalizado: publicações de utilizadores seguidos ordenadas por data
- Reacções: dar/retirar "gosto" em publicações e comentários
- Mensagens: chat directo entre utilizadores com histórico
- Notificações: notificações em tempo real (novo seguidor, gosto, comentário, mensagem)

### Não Funcionais

- Feed pull model (fan-out on read) com paginação de cursor
- WebSocket + STOMP para notificações e mensagens em tempo real
- Perfis privados com aprovação de seguimento
- Cursor pagination composto `(createdAt, id)` para consistência
- Contadores desnormalizados actualizados atomicamente no PostgreSQL
- Isolamento de domínios com comunicação apenas via Services

## Domínios

```
auth         → Registo, login, refresh de token
user         → Perfis, pesquisa por username (pg_trgm)
post         → CRUD de publicações
comment      → Comentários em publicações
like         → Like/unlike em posts e comentários
follow       → Seguir/deixar de seguir, aprovação
feed         → Feed personalizado (pull model)
message      → Mensagens directas com conversas
notification → Notificações em tempo real
websocket    → Configuração STOMP e autenticação
common       → Segurança, paginação, exceptions, configs
```

## Scripts úteis

```bash
# Compilar
mvn clean package -DskipTests

# Correr testes (requer Docker para Testcontainers)
mvn verify

# Correr apenas testes de integração
mvn test -pl . -Dtest="*IntegrationTest"
```

## Estrutura do repositório

```
.
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/rede_social_api/
│   │   │   ├── auth/
│   │   │   ├── user/
│   │   │   ├── post/
│   │   │   ├── comment/
│   │   │   ├── like/
│   │   │   ├── follow/
│   │   │   ├── feed/
│   │   │   ├── message/
│   │   │   ├── notification/
│   │   │   ├── websocket/
│   │   │   └── common/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/V1__init.sql
│   └── test/
│       └── java/com/rede_social_api/
│           └── integration/
└── docs/
    ├── 00-visao-geral.md
    ├── 01-arquitectura.md
    ├── 02-estrutura-diretorios.md
    ├── 03-entidades.md
    ├── 04-DTO.md
    ├── 05-fluxo-regras.md
    ├── 06-api-rest.md
    ├── 07-persistencia-flyway.md
    ├── 08-seguranca-jwt.md
    ├── 09-websockets-stomp.md
    ├── 10-cache-redis.md
    ├── 11-observabilidade-actuator.md
    ├── 12-docker-deploy.md
    └── adr/
        ├── adr-01
        ├── adr-02
        ├── adr-03
        ├── adr-04
        └── adr-05
```
# API-rede-social
