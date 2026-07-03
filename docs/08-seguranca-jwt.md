# Segurança e JWT

## Visão Geral

A autenticação é baseada em **JWT (JSON Web Token)** com dois tipos de token:
- **Access Token**: curta duração (1h), enviado em todos os requests autenticados
- **Refresh Token**: longa duração (7 dias), usado para obter novos access tokens

## Arquitectura de Segurança

```
┌──────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Client  │────→│  SecurityConfig  │────→│  JWT Filter     │
└──────────┘     └──────────────────┘     └─────────────────┘
                         │                        │
                         │  Rotas públicas         │  Token válido?
                         │  sem filtro             │  Extrai userId
                         ▼                        ▼
                  ┌──────────────────┐     ┌─────────────────┐
                  │  Controller      │←────│  SecurityContext │
                  └──────────────────┘     │  (Authenticated) │
                                           └─────────────────┘
```

## Configuração

**SecurityConfig** (`common/config/SecurityConfig.java`):

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users/{username}").permitAll()
            .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

## Componentes

### JwtProvider

Gera e valida tokens JWT:

- Gera access token com `subject = userId`, `expiration = 1h`
- Gera refresh token com `subject = userId`, claim `type = "refresh"`, `expiration = 7d`
- Valida: assinatura, expiração, se é refresh token (quando aplicável)
- Usa a biblioteca **jjwt** (io.jsonwebtoken) versão 0.12.6

### JwtAuthenticationFilter

Filter que intercepta todos os requests HTTP:

1. Extrai token do header `Authorization: Bearer <token>`
2. Valida o token via `JwtProvider.isValid(token)`
3. Verifica se **não** é refresh token (refresh tokens não autenticam endpoints)
4. Extrai `userId` do token
5. Cria `AuthenticatedUser` no `SecurityContextHolder`

### AuthenticatedUser

Implementa `UserDetails` do Spring Security com o `userId` do utilizador autenticado.

### @CurrentUser

Annotation personalizada com `@AuthenticationPrincipal` para injectar o utilizador autenticado nos Controllers:

```java
@GetMapping("/me")
public ResponseEntity<UserPublicResponse> getProfile(@CurrentUser AuthenticatedUser user) {
    // user.getId() contém o UUID do utilizador autenticado
}
```

## Rotas Públicas vs Protegidas

| Rota                              | Acesso          |
| --------------------------------- | --------------- |
| `/api/auth/**`                    | Público         |
| `GET /api/users/{username}`       | Público         |
| `/swagger-ui/**`, `/api-docs/**`  | Público         |
| `/ws/**`                          | Público (handshake) |
| Todas as outras `/api/**`         | Autenticado      |

## Fluxo de Tokens

```
1. Cliente: POST /api/auth/register → recebe { accessToken, refreshToken }
2. Cliente: Armazena tokens (localStorage / httpOnly cookie)
3. Cliente: GET /api/posts (Authorization: Bearer <accessToken>) → recurso
4. Se 401 → POST /api/auth/refresh { refreshToken } → novos tokens
5. Repetir passo 3 com novo access token
```

## Segurança Adicional

- **BCryptPasswordEncoder** para hash de passwords
- **Refresh tokens são de uso único**: cada refresh gera um novo par e invalida o anterior
- **Estateless**: não há sessão no servidor, cada request é autenticado individualmente
- **CORS**: configurado no SecurityConfig para permitir origens específicas
