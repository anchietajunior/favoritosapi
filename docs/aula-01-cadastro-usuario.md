# Aula 01 - Endpoint de Cadastro de Usuario

## Objetivo
Criar o endpoint `POST /cadastrar` para registrar novos usuarios no sistema.

## Contexto
Atualmente o projeto possui a entity `Usuario` mas nao tem repository nem controller para ela.
A entity tem `@JsonIgnore` na senha, o que impede o Jackson de desserializar a senha vinda do JSON.
Por isso, precisamos de um **DTO** (Data Transfer Object) para receber os dados do cadastro.

Vamos usar **Java Records** como DTOs - sao classes imutaveis e concisas, perfeitas para transferencia de dados.

---

## Passo 1 - Criar o pacote `dto`

Criar o pacote `com.example.favoritosapi.dto`.

## Passo 2 - Criar o CadastroRequest (Record)

**Arquivo**: `src/main/java/com/example/favoritosapi/dto/CadastroRequest.java`

```java
package com.example.favoritosapi.dto;

public record CadastroRequest(String nome, String email, String senha) {
}
```

> **Por que um Record?**
> Records sao classes imutaveis do Java que geram automaticamente construtor, getters, equals, hashCode e toString.
> Sao perfeitos para DTOs porque so precisamos transportar dados, sem logica de negocio.

## Passo 3 - Criar o UsuarioRepository

**Arquivo**: `src/main/java/com/example/favoritosapi/repository/UsuarioRepository.java`

```java
package com.example.favoritosapi.repository;

import com.example.favoritosapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

> **Por que `findByEmail` e `existsByEmail`?**
> - `findByEmail`: Sera usado na aula de login (Aula 03) para buscar o usuario pelo email.
> - `existsByEmail`: Sera usado no cadastro para verificar se o email ja esta em uso.

## Passo 4 - Criar o UsuarioController

**Arquivo**: `src/main/java/com/example/favoritosapi/controller/UsuarioController.java`

```java
package com.example.favoritosapi.controller;

import com.example.favoritosapi.dto.CadastroRequest;
import com.example.favoritosapi.model.Usuario;
import com.example.favoritosapi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UsuarioController {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody CadastroRequest request) {
        if (repository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Email ja cadastrado"));
        }

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha())
        );

        Usuario salvo = repository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}
```

> **Pontos importantes:**
> - `passwordEncoder.encode()` faz o hash da senha com BCrypt antes de salvar.
> - O `PasswordEncoder` ja esta configurado como Bean no `SecurityConfig`.
> - A resposta retorna o usuario salvo. Como `senha` tem `@JsonIgnore`, ela nao aparece no JSON de resposta.
> - Verificamos se o email ja existe para evitar duplicatas.
> - Usamos `request.nome()`, `request.email()`, `request.senha()` - essa e a sintaxe de Records (metodos acessores sem o prefixo "get").

## Passo 5 - Atualizar o SecurityConfig

Adicionar a rota `/cadastrar` como publica no `SecurityConfig`:

**Arquivo**: `src/main/java/com/example/favoritosapi/config/SecurityConfig.java`

Alterar de:
```java
.authorizeHttpRequests(auth -> auth
    .anyRequest().permitAll()
);
```

Para:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/cadastrar").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/").permitAll()
    .anyRequest().permitAll() // Sera alterado na Aula 03 para .authenticated()
);
```

> Por enquanto mantemos `anyRequest().permitAll()` porque ainda nao temos JWT configurado.
> Na Aula 03 vamos mudar para `anyRequest().authenticated()`.

---

## Testando

### Com curl:
```bash
curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Joao", "email": "joao@email.com", "senha": "123456"}'
```

### Resposta esperada (201 Created):
```json
{
  "id": 1,
  "nome": "Joao",
  "email": "joao@email.com",
  "cargo": "padrao"
}
```

> Note que `senha` e `favoritos` nao aparecem na resposta por causa do `@JsonIgnore`.

### Tentando cadastrar mesmo email (409 Conflict):
```json
{
  "erro": "Email ja cadastrado"
}
```

---

## Resumo dos arquivos criados/alterados

| Acao    | Arquivo                                    |
|---------|--------------------------------------------|
| Criar   | `dto/CadastroRequest.java`                 |
| Criar   | `repository/UsuarioRepository.java`        |
| Criar   | `controller/UsuarioController.java`        |
| Alterar | `config/SecurityConfig.java`               |
