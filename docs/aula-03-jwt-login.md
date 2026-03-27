# Aula 03 - JWT e Autenticacao (Login)

## Objetivo
Configurar autenticacao JWT no projeto para que usuarios possam fazer login na rota `POST /entrar`
e receber um token que sera usado para acessar rotas protegidas.

## O que e JWT?
JWT (JSON Web Token) e um padrao para transmitir informacoes de forma segura entre partes como um objeto JSON.
O token e composto por tres partes separadas por ponto: `header.payload.signature`.

Fluxo:
1. Usuario faz login com email e senha
2. Servidor valida as credenciais e gera um token JWT
3. Cliente envia o token no header `Authorization: Bearer <token>` nas proximas requisicoes
4. Servidor valida o token e identifica o usuario

---

## Passo 1 - Adicionar dependencia JJWT no pom.xml

Adicionar dentro de `<dependencies>`:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

> Apos adicionar, execute `mvn clean install` ou atualize o projeto na IDE.

## Passo 2 - Adicionar chave secreta no application.properties

**Arquivo**: `src/main/resources/application.properties`

Adicionar:

```properties
jwt.secret=suaChaveSecretaAquiDeveSerLongaESegura123456789ABCDEF
```

> **Importante**: Em producao, essa chave deve ser uma variavel de ambiente, nunca no codigo.
> Para desenvolvimento, pode ficar no properties.

## Passo 3 - Criar o JwtUtil

**Arquivo**: `src/main/java/com/example/favoritosapi/config/JwtUtil.java`

```java
package com.example.favoritosapi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration = 86400000; // 24 horas em milissegundos

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Long usuarioId, String email) {
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extrairUsuarioId(String token) {
        return Long.parseLong(extrairClaims(token).getSubject());
    }

    public boolean tokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

> **O que cada metodo faz:**
> - `gerarToken()`: Cria um JWT com o id do usuario no subject e email como claim.
> - `extrairClaims()`: Le os dados do token (decodifica).
> - `extrairUsuarioId()`: Pega o id do usuario que esta no subject do token.
> - `tokenValido()`: Verifica se o token e valido (assinatura e expiracao).

## Passo 4 - Criar o JwtFilter

**Arquivo**: `src/main/java/com/example/favoritosapi/config/JwtFilter.java`

```java
package com.example.favoritosapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Remove "Bearer "

            if (jwtUtil.tokenValido(token)) {
                Long usuarioId = jwtUtil.extrairUsuarioId(token);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                usuarioId, null, Collections.emptyList()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

> **Como funciona:**
> 1. Intercepta toda requisicao HTTP antes de chegar no controller.
> 2. Procura o header `Authorization` com o formato `Bearer <token>`.
> 3. Se o token for valido, coloca o `usuarioId` no `SecurityContext`.
> 4. Isso permite que qualquer controller acesse o id do usuario logado.
>
> **O que e `UsernamePasswordAuthenticationToken`?**
> E a forma do Spring Security representar um usuario autenticado.
> Colocamos o `usuarioId` como `principal` (primeiro parametro).

## Passo 5 - Criar DTOs de Login

**Arquivo**: `src/main/java/com/example/favoritosapi/dto/LoginRequest.java`

```java
package com.example.favoritosapi.dto;

public record LoginRequest(String email, String senha) {
}
```

**Arquivo**: `src/main/java/com/example/favoritosapi/dto/LoginResponse.java`

```java
package com.example.favoritosapi.dto;

public record LoginResponse(String token) {
}
```

## Passo 6 - Criar a rota POST /entrar

Adicionar no `UsuarioController`:

```java
@PostMapping("/entrar")
public ResponseEntity<?> entrar(@RequestBody LoginRequest request) {
    Optional<Usuario> optUsuario = repository.findByEmail(request.email());

    if (optUsuario.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("erro", "Email ou senha invalidos"));
    }

    Usuario usuario = optUsuario.get();

    if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("erro", "Email ou senha invalidos"));
    }

    String token = jwtUtil.gerarToken(usuario.getId(), usuario.getEmail());
    return ResponseEntity.ok(new LoginResponse(token));
}
```

**Adicionar os imports e o campo `jwtUtil` no UsuarioController:**

```java
import com.example.favoritosapi.config.JwtUtil;
import com.example.favoritosapi.dto.LoginRequest;
import com.example.favoritosapi.dto.LoginResponse;
import java.util.Optional;

@RestController
public class UsuarioController {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UsuarioController(UsuarioRepository repository,
                             PasswordEncoder passwordEncoder,
                             JwtUtil jwtUtil) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // ... metodo cadastrar da Aula 01
    // ... metodo entrar acima
}
```

> **Por que a mensagem de erro e generica ("Email ou senha invalidos")?**
> Por seguranca! Se dissermos "email nao encontrado", um atacante saberia quais emails existem no sistema.
> Sempre retorne a mesma mensagem para email errado e senha errada.

## Passo 7 - Atualizar o SecurityConfig

**Arquivo**: `src/main/java/com/example/favoritosapi/config/SecurityConfig.java`

```java
package com.example.favoritosapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/cadastrar", "/entrar").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> **Mudancas:**
> - `SessionCreationPolicy.STATELESS`: Desabilita sessoes HTTP (JWT e stateless).
> - `/cadastrar` e `/entrar` sao publicas (nao precisam de token).
> - `anyRequest().authenticated()`: Todas as outras rotas exigem token.
> - `addFilterBefore()`: Registra o JwtFilter para rodar antes do filtro padrao do Spring Security.

---

## Testando

### 1. Cadastrar usuario (rota publica):
```bash
curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Joao", "email": "joao@email.com", "senha": "123456"}'
```

### 2. Fazer login:
```bash
curl -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "joao@email.com", "senha": "123456"}'
```

### Resposta esperada (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJqb2FvQGVtYWlsLmNvbSIsImlhdCI6..."
}
```

### 3. Acessar rota protegida SEM token (401 Unauthorized):
```bash
curl http://localhost:8080/favoritos
```

### 4. Acessar rota protegida COM token:
```bash
curl http://localhost:8080/favoritos \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## Resumo dos arquivos criados/alterados

| Acao    | Arquivo                                    |
|---------|--------------------------------------------|
| Alterar | `pom.xml` (dependencias jjwt)              |
| Alterar | `application.properties` (jwt.secret)      |
| Criar   | `config/JwtUtil.java`                      |
| Criar   | `config/JwtFilter.java`                    |
| Criar   | `dto/LoginRequest.java`                    |
| Criar   | `dto/LoginResponse.java`                   |
| Alterar | `controller/UsuarioController.java`        |
| Alterar | `config/SecurityConfig.java`               |
