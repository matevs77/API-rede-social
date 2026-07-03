# Testes de Software

## Stack de Testes

| Tecnologia       | Versão   | Finalidade                               |
| ---------------- | -------- | ---------------------------------------- |
| JUnit 5          | -        | Framework de testes                      |
| Testcontainers   | 1.21.3   | Provisionamento de PostgreSQL e Redis    |
| Spring Boot Test | -        | MockMvc, context loading, propriedades   |
| Spring Security Test | -    | Testes de autorização                    |
| docker-java      | 3.5.2    | Cliente Docker para Testcontainers       |

## Estrutura

```
src/test/
├── java/com/rede_social_api/
│   ├── integration/
│   │   ├── AbstractIntegrationTest.java        ← Base para todos os testes
│   │   ├── PostgresRedisContainers.java        ← Singleton dos contentores
│   │   ├── AuthUserIntegrationTest.java        ← Registo, login, perfil
│   │   ├── PostIntegrationTest.java            ← CRUD de publicações
│   │   ├── FeedIntegrationTest.java            ← Feed personalizado
│   │   ├── FollowIntegrationTest.java          ← Seguimento e privacidade
│   │   ├── CommentLikeIntegrationTest.java     ← Comentários e gostos
│   │   └── MessageNotificationIntegrationTest.java  ← Mensagens e notificações
│   └── support/
│       └── TestFixtures.java                   ← Helpers de teste
└── resources/
    └── application-test.yml                    ← Config para testes
```

## Infraestrutura de Teste

### PostgresRedisContainers

Classe singleton que gere contentores Docker partilhados entre todos os testes:

```java
private static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("rede_social")
        .withUsername("rede_social")
        .withPassword("rede_social");

private static final GenericContainer<?> REDIS =
    new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
```

Os contentores são iniciados **uma vez** por suite de testes (`started` flag) e partilhados entre todas as classes de teste para eficiência.

### AbstractIntegrationTest

Classe base abstracta que:
1. Verifica se Docker está disponível (`DockerClientFactory.instance().client()`)
2. Inicia contentores PostgreSQL e Redis via Testcontainers
3. Injecta dinamicamente as propriedades de conexão via `@DynamicPropertySource`
4. Fallback para instâncias locais se Docker não estiver disponível

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    // ...
}
```

### TestFixtures

Classe utilitária com helpers reutilizáveis:

| Método                              | Descrição                              |
| ----------------------------------- | -------------------------------------- |
| `registerUser(mockMvc, mapper, suffix)` | Regista utilizador e retorna AuthResponse |
| `loginUser(mockMvc, mapper, username)`  | Faz login e retorna AuthResponse      |
| `bearer(token)`                     | Formata `"Bearer " + token`           |

## Padrões de Teste

Cada classe de teste estende `AbstractIntegrationTest` e usa `@AutoConfigureMockMvc` para injecção do `MockMvc`.

### Cenários Mínimos por Endpoint

A regra do projecto (`.cursorrules`) exige **4 cenários mínimos** por endpoint protegido:

| Cenário                    | Descrição                                              | Exemplo (PostIntegrationTest)                  |
| -------------------------- | ------------------------------------------------------ | ---------------------------------------------- |
| **owner access**           | O dono do recurso acede e opera com sucesso            | `ownerCanCreateAndUpdatePost()`                |
| **unauthorized**           | Request sem token JWT                                  | `unauthorizedUserCannotAccessProtectedPostEndpoint()` |
| **other user**             | Outro utilizador tenta modificar/eliminar              | `otherUserCannotDeletePost()`                  |
| **not found**              | Recurso inexistente retorna 404                        | `getNonExistentPostReturns404()`               |

### Testes Implementados

| Classe de Teste                    | Cenários                                                    |
| ---------------------------------- | ----------------------------------------------------------- |
| **AuthUserIntegrationTest**        | - Registo + login + acesso a perfil próprio<br>- Perfil público visível sem auth<br>- Edição de perfil requer auth<br>- Pesquisa de users requer auth |
| **PostIntegrationTest**            | - Owner cria e actualiza post<br>- Sem token → 401<br>- Outro user tenta apagar → 404<br>- Post inexistente → 404 |
| **FeedIntegrationTest**            | - Feed mostra posts de quem se segue<br>- Feed sem auth → 401 |
| **FollowIntegrationTest**          | - Perfil público → follow ACCEPTED imediato<br>- Perfil privado → PENDING<br>- Não pode seguir-se a si próprio<br>- Follow sem auth → 401 |
| **CommentLikeIntegrationTest**     | - Owner comenta e dá like num post<br>- Outro user tenta editar comentário → 404<br>- Unlike decrementa contador |
| **MessageNotificationIntegrationTest** | - Enviar mensagem cria conversa e notificação<br>- Não-participante não pode enviar → 403<br>- Marcar notificações como lidas actualiza contador |

## Configuração de Teste

`application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rede_social
    username: rede_social
    password: rede_social
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    show-sql: false

app:
  jwt:
    secret: test-secret-key-with-at-least-256-bits-for-jwt-signing-ok
```

**Nota:** As propriedades `spring.datasource.url`, `username`, `password` e `redis.*` são sobrescritas dinamicamente pelo `AbstractIntegrationTest` quando o Docker está disponível.

## Execução de Testes

### Pré-requisitos

- Docker em execução (para Testcontainers)
- Ou PostgreSQL + Redis locais (fallback)

### Comandos

```bash
# Todos os testes
mvn verify

# Apenas testes de integração
mvn test -Dtest="*IntegrationTest"

# Teste específico
mvn test -Dtest="PostIntegrationTest"

# Com log de SQL
mvn test -Dtest="FollowIntegrationTest" -Dspring.jpa.show-sql=true
```

### Configuração Maven (Surefire)

No `pom.xml`, a variável `DOCKER_API_VERSION` é definida para compatibilidade com a versão da API Docker:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <environmentVariables>
            <DOCKER_API_VERSION>1.44</DOCKER_API_VERSION>
        </environmentVariables>
    </configuration>
</plugin>
```

## Boas Práticas

1. **Testes isolados** — cada teste cria os seus próprios dados (via `TestFixtures`)
2. **Sem ordem fixa** — testes podem ser executados em qualquer ordem
3. **Dados efémeros** — o banco é limpo entre execuções (contentores são recriados)
4. **Registo como Setup** — users são criados dentro de cada método `@Test`
5. **Validação de resposta** — uso de `jsonPath` para verificar campos específicos
6. **Testes de autorização** — cada endpoint protegido testa pelo menos: owner, unauthorized, other user, not found

## Limitações Actuais

- Testes de WebSocket não implementados (exigem cliente STOMP )
- Testes de rate limiting não implementados
- Cobertura de cache Redis não testada directamente
- Testes de concorrência (contadores atómicos) não implementados
