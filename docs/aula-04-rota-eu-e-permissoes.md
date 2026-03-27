# Aula 04 - Rota /eu e Permissoes de Acesso

## Objetivo
1. Criar a rota `GET /eu` para exibir informacoes do usuario logado.
2. Refatorar o CRUD de favoritos para que usuarios so acessem seus proprios favoritos.

## Contexto
Com o JWT configurado na Aula 03, agora temos o `usuarioId` disponivel em qualquer controller
atraves do `SecurityContextHolder`. Vamos usar isso para:
- Mostrar os dados do usuario logado
- Filtrar favoritos por usuario
- Impedir que um usuario acesse favoritos de outro

---

## Parte 1: Rota GET /eu

### Passo 1 - Adicionar a rota no UsuarioController

Adicionar no `UsuarioController`:

```java
@GetMapping("/eu")
public ResponseEntity<?> eu() {
    Long usuarioId = (Long) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();

    return repository.findById(usuarioId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

**Adicionar o import:**
```java
import org.springframework.security.core.context.SecurityContextHolder;
```

> **Como funciona?**
> Na Aula 03, o `JwtFilter` coloca o `usuarioId` como `principal` no `SecurityContext`.
> Aqui so pegamos ele de volta com `getAuthentication().getPrincipal()`.
> Como `senha` tem `@JsonIgnore`, ela nao aparece na resposta.

### Testando a rota /eu:
```bash
curl http://localhost:8080/eu \
  -H "Authorization: Bearer <seu-token-aqui>"
```

### Resposta esperada (200 OK):
```json
{
  "id": 1,
  "nome": "Joao",
  "email": "joao@email.com",
  "cargo": "padrao"
}
```

---

## Parte 2: Permissoes nos Favoritos

### Passo 2 - Refatorar o FavoritoController

Agora o usuario vem do token JWT, nao mais do corpo da requisicao.
Cada operacao so funciona com favoritos do usuario logado.

**Atualizar `FavoritoRequest`** para remover o campo `idUsuario` (agora vem do token):

**Arquivo**: `src/main/java/com/example/favoritosapi/dto/FavoritoRequest.java`

```java
package com.example.favoritosapi.dto;

public record FavoritoRequest(String titulo, String url) {
}
```

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favoritos")
public class FavoritoController {
    private final FavoritoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public FavoritoController(FavoritoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    private Long getUsuarioLogadoId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    @GetMapping
    public List<Favorito> listar() {
        Long usuarioId = getUsuarioLogadoId();
        return repository.findByUsuarioId(usuarioId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorito> buscarPorId(@PathVariable Long id) {
        Long usuarioId = getUsuarioLogadoId();

        return repository.findById(id)
                .filter(fav -> fav.getUsuario().getId().equals(usuarioId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody FavoritoRequest request) {
        Long usuarioId = getUsuarioLogadoId();

        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
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
        Long usuarioId = getUsuarioLogadoId();

        return repository.findById(id)
                .filter(fav -> fav.getUsuario().getId().equals(usuarioId))
                .map(fav -> {
                    fav.setTitulo(request.titulo());
                    fav.setUrl(request.url());
                    Favorito salvo = repository.save(fav);
                    return ResponseEntity.ok((Object) salvo);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        Long usuarioId = getUsuarioLogadoId();

        return repository.findById(id)
                .filter(fav -> fav.getUsuario().getId().equals(usuarioId))
                .map(fav -> {
                    repository.delete(fav);
                    return ResponseEntity.noContent().<Void>build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
```

> **Mudancas importantes:**
>
> 1. **`getUsuarioLogadoId()`**: Metodo auxiliar que pega o id do usuario do token JWT.
>
> 2. **`listar()`**: Agora usa `findByUsuarioId()` - so retorna favoritos do usuario logado.
>    Antes retornava TODOS os favoritos de TODOS os usuarios.
>
> 3. **`buscarPorId()`**: Alem de buscar pelo id do favorito, verifica se o favorito
>    pertence ao usuario logado com `.filter()`. Se nao pertence, retorna 404.
>
> 4. **`criar()`**: O usuario vem do token, nao mais do corpo da requisicao.
>    Nao precisa mais enviar `idUsuario` no JSON.
>
> 5. **`atualizar()`**: Verifica se o favorito pertence ao usuario antes de atualizar.
>
> 6. **`excluir()`**: Verifica se o favorito pertence ao usuario antes de excluir.
>
> **Seguranca**: Se o usuario tentar acessar/editar/excluir um favorito que nao e dele,
> recebe `404 Not Found` (e nao `403 Forbidden`). Isso e intencional - nao revelamos
> que o recurso existe para outro usuario.

---

## Testando

### Cenario completo:

#### 1. Cadastrar dois usuarios:
```bash
curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Joao", "email": "joao@email.com", "senha": "123456"}'

curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Maria", "email": "maria@email.com", "senha": "654321"}'
```

#### 2. Login com Joao:
```bash
curl -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "joao@email.com", "senha": "123456"}'
```
> Guarde o token retornado. Chamaremos de `TOKEN_JOAO`.

#### 3. Criar favorito com Joao:
```bash
curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_JOAO" \
  -d '{"titulo": "Google", "url": "https://google.com"}'
```
> Note que nao enviamos mais `idUsuario` - o sistema sabe quem e pelo token.

#### 4. Listar favoritos do Joao:
```bash
curl http://localhost:8080/favoritos \
  -H "Authorization: Bearer TOKEN_JOAO"
```
> Retorna apenas os favoritos do Joao.

#### 5. Login com Maria e tentar acessar favorito do Joao:
```bash
curl -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@email.com", "senha": "654321"}'
```
> Guarde o token. Chamaremos de `TOKEN_MARIA`.

```bash
curl http://localhost:8080/favoritos/1 \
  -H "Authorization: Bearer TOKEN_MARIA"
```
> Retorna `404 Not Found` - Maria nao consegue ver o favorito do Joao.

#### 6. Ver informacoes do usuario logado:
```bash
curl http://localhost:8080/eu \
  -H "Authorization: Bearer TOKEN_JOAO"
```
```json
{
  "id": 1,
  "nome": "Joao",
  "email": "joao@email.com",
  "cargo": "padrao"
}
```

---

## Resumo dos arquivos criados/alterados

| Acao    | Arquivo                                    |
|---------|--------------------------------------------|
| Alterar | `controller/UsuarioController.java`        |
| Alterar | `controller/FavoritoController.java`       |
| Alterar | `dto/FavoritoRequest.java`                 |
