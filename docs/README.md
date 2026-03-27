# Favoritos API - Guia de Aulas

## Estrutura do Projeto (estado atual)

```
src/main/java/com/example/favoritosapi/
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── FavoritoController.java
│   └── HomeController.java
├── model/
│   ├── Favorito.java
│   └── Usuario.java
├── repository/
│   └── FavoritoRepository.java
└── FavoritosapiApplication.java
```

## Estrutura do Projeto (apos todas as aulas)

```
src/main/java/com/example/favoritosapi/
├── config/
│   ├── JwtFilter.java          ← Aula 03
│   ├── JwtUtil.java            ← Aula 03
│   └── SecurityConfig.java     ← Alterado nas Aulas 01, 03
├── controller/
│   ├── FavoritoController.java ← Alterado nas Aulas 02, 04
│   ├── HomeController.java
│   └── UsuarioController.java  ← Aula 01, alterado na 03, 04
├── dto/
│   ├── CadastroRequest.java    ← Aula 01
│   ├── FavoritoRequest.java    ← Aula 02, alterado na 04
│   ├── LoginRequest.java       ← Aula 03
│   └── LoginResponse.java      ← Aula 03
├── model/
│   ├── Favorito.java           ← Alterado na Aula 02
│   └── Usuario.java
├── repository/
│   ├── FavoritoRepository.java ← Alterado na Aula 02
│   └── UsuarioRepository.java  ← Aula 01
└── FavoritosapiApplication.java
```

## Aulas

| #  | Aula                                                          | Descricao                                           |
|----|---------------------------------------------------------------|-----------------------------------------------------|
| 00 | [O que sao DTOs](aula-00-o-que-sao-dtos.md)                  | Conceito de DTOs, Java Records e por que sao necessarios |
| 01 | [Cadastro de Usuario](aula-01-cadastro-usuario.md)            | Endpoint POST /cadastrar com hash de senha          |
| 02 | [CRUD de Favoritos com Usuario](aula-02-crud-favoritos-com-usuario.md) | Atualizar CRUD para associar favoritos a usuarios |
| 03 | [JWT e Login](aula-03-jwt-login.md)                           | Autenticacao JWT com rota POST /entrar              |
| 04 | [Rota /eu e Permissoes](aula-04-rota-eu-e-permissoes.md)      | Rota GET /eu e isolamento de favoritos por usuario  |

## Ordem de implementacao

As aulas devem ser seguidas **em ordem** (00 → 01 → 02 → 03 → 04).
Cada aula constroi sobre o que foi feito na anterior.

## Dependencias a adicionar (Aula 03)

```xml
<!-- jjwt (adicionar no pom.xml) -->
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
