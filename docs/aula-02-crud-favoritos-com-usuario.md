# Aula 02 - CRUD de Favoritos com Usuario

## Objetivo
Atualizar o CRUD de favoritos para que cada favorito pertenca a um usuario.

## Contexto
O relacionamento entre `Favorito` e `Usuario` **ja esta configurado** nas entities:

- `Favorito` tem `@ManyToOne` com `@JoinColumn(name = "usuario_id", nullable = false)`
- `Usuario` tem `@OneToMany(mappedBy = "usuario")`

O problema atual e que o `FavoritoController` nao lida com o usuario.
Para criar um favorito, precisamos associar ele a um usuario existente.

> **Nesta aula**, vamos receber o `id_usuario` via corpo da requisicao.
> **Na Aula 04**, vamos refatorar para pegar o usuario automaticamente do token JWT.

---

## Passo 1 - Criar o FavoritoRequest (Record)

**Arquivo**: `src/main/java/com/example/favoritosapi/dto/FavoritoRequest.java`

```java
package com.example.favoritosapi.dto;

public record FavoritoRequest(String titulo, String url, Long idUsuario) {
}
```

> **Por que um DTO aqui?**
> A entity `Favorito` tem um campo `Usuario usuario` (o objeto inteiro).
> No JSON da requisicao, queremos receber apenas o `idUsuario` (um numero).
> O DTO faz essa ponte - recebemos o id e buscamos o usuario no banco.

## Passo 2 - Atualizar o FavoritoRepository

**Arquivo**: `src/main/java/com/example/favoritosapi/repository/FavoritoRepository.java`

Adicionar metodo para buscar favoritos por usuario:

```java
package com.example.favoritosapi.repository;

import com.example.favoritosapi.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByUsuarioId(Long usuarioId);
}
```

> O Spring Data JPA gera a query automaticamente a partir do nome do metodo:
> `findByUsuarioId` → `SELECT * FROM favoritos WHERE usuario_id = ?`

## Passo 3 - Atualizar o FavoritoController

**Arquivo**: `src/main/java/com/example/favoritosapi/controller/FavoritoController.java`

```java
package com.example.favoritosapi.controller;

import com.example.favoritosapi.dto.FavoritoRequest;
import com.example.favoritosapi.model.Favorito;
import com.example.favoritosapi.model.Usuario;
import com.example.favoritosapi.repository.FavoritoRepository;
import com.example.favoritosapi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favoritos")
public class FavoritoController {
    private final FavoritoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public FavoritoController(FavoritoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Favorito> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorito> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody FavoritoRequest request) {
        Usuario usuario = usuarioRepository.findById(request.idUsuario())
                .orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", "Usuario nao encontrado"));
        }

        Favorito favorito = new Favorito();
        favorito.setTitulo(request.titulo());
        favorito.setUrl(request.url());
        favorito.setUsuario(usuario);

        Favorito salvo = repository.save(favorito);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody FavoritoRequest request) {
        return repository.findById(id)
                .map(fav -> {
                    fav.setTitulo(request.titulo());
                    fav.setUrl(request.url());
                    Favorito salvo = repository.save(fav);
                    return ResponseEntity.ok((Object) salvo);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

> **Mudancas principais:**
> - `criar()` agora recebe `FavoritoRequest` em vez de `Favorito`.
> - Buscamos o `Usuario` pelo `idUsuario` antes de criar o favorito.
> - `atualizar()` tambem recebe `FavoritoRequest` (so atualiza titulo e url, nao muda o dono).
> - Injetamos `UsuarioRepository` no construtor.

## Passo 4 - Evitar referencia circular no JSON

Quando o Jackson serializa um `Favorito`, ele tenta serializar o `Usuario` dentro dele,
que por sua vez tem uma lista de `Favorito`... causando um loop infinito.

A entity `Usuario` ja tem `@JsonIgnore` na lista de favoritos, o que resolve um lado.
Mas no `Favorito`, o campo `usuario` vai serializar o objeto `Usuario` inteiro.

Para manter simples, adicione `@JsonIgnore` no campo `usuario` da entity `Favorito`:

**Arquivo**: `src/main/java/com/example/favoritosapi/model/Favorito.java`

Adicionar `@JsonIgnore` no campo `usuario`:

```java
@JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "usuario_id", nullable = false)
private Usuario usuario;
```

> Assim o JSON de resposta do favorito nao inclui os dados do usuario.
> Na Aula 04, quando tivermos JWT, o usuario sera identificado pelo token.

---

## Testando

### Criar favorito (precisa ter um usuario cadastrado antes - Aula 01):
```bash
curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Google", "url": "https://google.com", "idUsuario": 1}'
```

### Resposta esperada (201 Created):
```json
{
  "id": 1,
  "titulo": "Google",
  "url": "https://google.com",
  "criadoEm": "2026-03-20T10:30:00",
  "atualizadoEm": "2026-03-20T10:30:00"
}
```

### Atualizar favorito:
```bash
curl -X PUT http://localhost:8080/favoritos/1 \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Google Brasil", "url": "https://google.com.br"}'
```

> Note que no PUT nao precisamos enviar `idUsuario` - o dono do favorito nao muda.

### Usuario nao encontrado (404):
```bash
curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Google", "url": "https://google.com", "idUsuario": 999}'
```

```json
{
  "erro": "Usuario nao encontrado"
}
```

---

## Resumo dos arquivos criados/alterados

| Acao    | Arquivo                                     |
|---------|---------------------------------------------|
| Criar   | `dto/FavoritoRequest.java`                  |
| Alterar | `repository/FavoritoRepository.java`        |
| Alterar | `controller/FavoritoController.java`        |
| Alterar | `model/Favorito.java` (adicionar JsonIgnore)|
