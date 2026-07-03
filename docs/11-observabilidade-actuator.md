# Observabilidade e Actuator

## Visão Geral

A API utiliza **Spring Boot Actuator** e **Springdoc OpenAPI** para fornecer endpoints de monitorização, métricas e documentação.

## Endpoints Actuator

Os endpoints Actuator estão expostos na porta `8080` sob o path base `/actuator`.

| Endpoint              | Descrição                          |
| --------------------- | ---------------------------------- |
| `/actuator/health`    | Estado da aplicação e dependências |
| `/actuator/info`      | Informações da aplicação           |
| `/actuator/metrics`   | Métricas (JVM, requests, etc.)     |
| `/actuator/prometheus`| Métricas no formato Prometheus     |
| `/actuator/loggers`   | Configuração de logs em runtime    |
| `/actuator/env`       | Propriedades do ambiente           |

### Health Checks

O Actuator verifica a saúde das dependências:

- **PostgreSQL** — via `DataSourceHealthIndicator`
- **Redis** — via `RedisHealthIndicator`
- **Disco** — via `DiskSpaceHealthIndicator`

Exemplo de resposta `/actuator/health`:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

## Métricas

- **JVM**: memória, threads, garbage collection
- **Tomcat**: conexões activas, requests processados
- **HikariCP**: conexões activas, pendentes, tempo de aquisição
- **Métricas customizadas** (por implementar): latência por endpoint, taxa de erros

## Documentação da API (Springdoc OpenAPI)

A documentação interactiva da API é gerada automaticamente pelo Springdoc OpenAPI:

| Recurso              | URL                                          |
| -------------------- | -------------------------------------------- |
| Swagger UI           | `http://localhost:8080/swagger-ui.html`       |
| OpenAPI JSON         | `http://localhost:8080/api-docs`              |

Configuração em `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### Informações incluídas na Spec

- Endpoints agrupados por domínio (Auth, User, Post, etc.)
- Schemas de request/response
- Esquema de autenticação Bearer JWT
- Códigos de erro possíveis

## Logging

### Perfil de Desenvolvimento (`application-dev.yml`)

```yaml
logging:
  level:
    com.rede_social_api: DEBUG
```

### Produção

O nível de logging recomendado para produção é `INFO` (definido no `application.yml` principal), com `WARN` para bibliotecas externas.

### Correlação

Cada request recebe um identificador único via `MDC` para correlação em logs. (A implementar: adicionar `X-Request-Id` nos headers e logs.)

## Dependência Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

## Exemplos de Uso

### Verificar saúde da API

```bash
curl http://localhost:8080/actuator/health
```

### Obter métricas JVM

```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Aceder ao Swagger UI

```bash
open http://localhost:8080/swagger-ui.html
```
