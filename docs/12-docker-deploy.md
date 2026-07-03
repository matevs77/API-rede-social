# Docker e Deploy

## Docker Compose

O ficheiro `docker-compose.yml` na raiz do projecto define três serviços:

```yaml
services:
  postgres:      # PostgreSQL 16 Alpine
  redis:         # Redis 7 Alpine
  api:           # Aplicação Spring Boot
```

### Serviços

| Serviço   | Imagem             | Porta  | Depende de        |
| --------- | ------------------ | ------ | ----------------- |
| postgres  | postgres:16-alpine | 5432   | -                 |
| redis     | redis:7-alpine     | 6379   | -                 |
| api       | (build local)      | 8080   | postgres, redis   |

### Variáveis de Ambiente

O serviço `api` recebe as seguintes variáveis:

| Variável                          | Descrição                          |
| --------------------------------- | ---------------------------------- |
| `SPRING_PROFILES_ACTIVE`          | Perfil activo (`dev`)              |
| `SPRING_DATASOURCE_URL`           | URL de conexão ao PostgreSQL       |
| `SPRING_DATASOURCE_USERNAME`      | Utilizador PostgreSQL              |
| `SPRING_DATASOURCE_PASSWORD`      | Password PostgreSQL                |
| `SPRING_DATA_REDIS_HOST`          | Host Redis                         |
| `SPRING_DATA_REDIS_PORT`          | Porta Redis                        |
| `JWT_SECRET`                      | Chave secreta JWT (min 256 bits)   |

### Healthchecks

PostgreSQL e Redis têm healthchecks configurados:

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U rede_social -d rede_social"]
    interval: 5s
    timeout: 5s
    retries: 5

redis:
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 5s
    timeout: 5s
    retries: 5
```

A API só inicia após ambos os serviços estarem saudáveis (`condition: service_healthy`).

## Dockerfile

O `Dockerfile` usa multi-stage build para optimizar o tamanho da imagem:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Nota:** O JAR é copiado para uma imagem JRE (mais leve) e a JDK+Maven são descartados após o build.

## Comandos

### Desenvolvimento Local

```bash
# Iniciar apenas dependências (sem reconstruir a API)
docker compose up -d postgres redis

# Executar API localmente com Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Completo

```bash
# Build + start todos os serviços
docker compose up --build

# Start em background
docker compose up --build -d

# Ver logs
docker compose logs -f api

# Parar tudo
docker compose down

# Parar e remover volumes (limpa dados)
docker compose down -v
```

### Produção

Para produção:

1. Substituir `JWT_SECRET` por uma chave segura (256 bits mín.)
2. Ajustar `SPRING_PROFILES_ACTIVE` para `prod` (criar `application-prod.yml` com configurações de produção)
3. Considerar:
   - Banco de dados gerido (AWS RDS, Cloud SQL, etc.)
   - Redis gerido (ElastiCache, Redis Cloud, etc.)
   - Load balancer à frente da API
   - Múltiplas instâncias para alta disponibilidade
   - CI/CD pipeline com testes de integração

## Requisitos de Sistema

| Recurso      | Mínimo     | Recomendado |
| ------------ | ---------- | ----------- |
| RAM          | 2 GB       | 4 GB        |
| CPU          | 1 core     | 2 cores     |
| Disco        | 1 GB       | 5 GB        |
| Docker       | 24+        | 24+         |
| Docker Compose | 2.20+    | 2.20+       |

## Segurança

- A password do PostgreSQL está hardcoded no `docker-compose.yml` para desenvolvimento
- Em produção, usar segredos externos (Docker secrets, Vault, variáveis de ambiente)
- O `JWT_SECRET` nunca deve estar no código-fonte
- Para produção, remover o Swagger UI ou proteger com autenticação
