# Favoritos API

API REST para gerenciamento de links favoritos com autenticação JWT, construída com Spring Boot 4.

## Tecnologias

- **Java 25** + **Spring Boot 4.0.3**
- **Spring Security** + **JWT (JJWT 0.12.6)** — autenticação stateless
- **Spring Data JPA** + **Hibernate** — persistência de dados
- **MySQL** — banco de dados principal
- **H2** — banco em memória para testes
- **Lombok** — redução de boilerplate
- **Springdoc OpenAPI** — documentação Swagger
- **JUnit 5** + **MockMvc** — testes automatizados

## Arquitetura em Camadas

```
src/main/java/com/example/favoritosapi/
├── config/          # Configurações (Security, JWT)
│   ├── SecurityConfig.java    — Regras de segurança e filtros
│   ├── JwtUtil.java           — Geração e validação de tokens JWT
│   └── JwtFilter.java         — Filtro que intercepta requisições HTTP
├── controller/      # Endpoints REST
│   ├── HomeController.java    — Rota raiz (/)
│   ├── UsuarioController.java — Cadastro, login e perfil (/eu)
│   └── FavoritoController.java — CRUD de favoritos
├── dto/             # Objetos de transferência (Records)
│   ├── CadastroRequest.java   — Dados de cadastro
│   ├── LoginRequest.java      — Credenciais de login
│   ├── LoginResponse.java     — Token JWT
│   └── FavoritoRequest.java   — Dados do favorito
├── model/           # Entidades JPA
│   ├── Usuario.java           — Tabela usuarios
│   └── Favorito.java          — Tabela favoritos
└── repository/      # Acesso ao banco de dados
    ├── UsuarioRepository.java — Queries de usuário
    └── FavoritoRepository.java — Queries de favoritos
```

## Funcionalidades

### Autenticação
- Cadastro de usuários com senha criptografada (BCrypt)
- Login com email/senha retornando token JWT (24h de validade)
- Rotas protegidas — apenas usuários autenticados acessam os favoritos
- Endpoint `/eu` para consultar dados do usuário logado

### CRUD de Favoritos
- Criar, listar, buscar, atualizar e excluir favoritos
- Cada usuário acessa apenas seus próprios favoritos (isolamento por JWT)
- Timestamps automáticos de criação e atualização

## Endpoints

### Rotas Públicas

| Método | Rota         | Descrição                          |
|--------|--------------|------------------------------------|
| GET    | `/`          | Mensagem de boas-vindas            |
| POST   | `/cadastrar` | Cadastrar novo usuário             |
| POST   | `/entrar`    | Login (retorna token JWT)          |

### Rotas Protegidas (requer `Authorization: Bearer <token>`)

| Método | Rota              | Descrição                          |
|--------|-------------------|------------------------------------|
| GET    | `/eu`             | Dados do usuário autenticado       |
| GET    | `/favoritos`      | Listar favoritos do usuário        |
| GET    | `/favoritos/{id}` | Buscar favorito por ID             |
| POST   | `/favoritos`      | Criar novo favorito                |
| PUT    | `/favoritos/{id}` | Atualizar favorito                 |
| DELETE | `/favoritos/{id}` | Excluir favorito                   |

## Como Executar

### Pré-requisitos
- Java 25+
- MySQL rodando na porta 3306
- Banco `api_favoritos` criado

### Rodar a aplicação
```bash
./mvnw spring-boot:run
```

### Rodar os testes
```bash
./mvnw test -Dspring.profiles.active=test
```

### Acessar documentação Swagger
```
http://localhost:8080/swagger-ui.html
```

## Guia Passo a Passo — Testando a API

Abaixo está o fluxo completo para testar todas as funcionalidades da API usando `curl` no terminal.
Você também pode usar o **Swagger UI** em `http://localhost:8080/swagger-ui.html` ou ferramentas como **Postman** e **Insomnia**.

### 1. Cadastrar um usuário

Crie um novo usuário informando nome, email e senha. A senha será criptografada automaticamente com BCrypt.

```bash
curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "João Silva", "email": "joao@email.com", "senha": "123456"}'
```

**Resposta esperada (201 Created):**
```json
{
  "id": 1,
  "nome": "João Silva",
  "email": "joao@email.com",
  "cargo": "padrao"
}
```

> A senha **nunca** aparece na resposta — ela é protegida com `@JsonIgnore`.

Se tentar cadastrar o mesmo email novamente, receberá **409 Conflict**:
```json
{"erro": "Email já cadastrado"}
```

### 2. Fazer login e obter o token JWT

Use as credenciais cadastradas para fazer login. A API retorna um token JWT válido por 24 horas.

```bash
curl -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "joao@email.com", "senha": "123456"}'
```

**Resposta esperada (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJqb2FvQGVtYWlsLmNvbSIsImlhdCI6MTcxMTU..."
}
```

> **Copie este token** — ele será usado em todas as requisições autenticadas a seguir.

Se o email não existir ou a senha estiver errada, receberá **401 Unauthorized**:
```json
{"erro": "Credenciais inválidas"}
```

### 3. Consultar dados do usuário logado

Use o token no header `Authorization` para acessar a rota `/eu`.

```bash
curl -X GET http://localhost:8080/eu \
  -H "Authorization: Bearer <cole-seu-token-aqui>"
```

**Resposta esperada (200 OK):**
```json
{
  "id": 1,
  "nome": "João Silva",
  "email": "joao@email.com",
  "cargo": "padrao"
}
```

> Sem o token, a resposta será **403 Forbidden**.

### 4. Criar um favorito

Envie `titulo` e `url` no corpo da requisição. O favorito será associado automaticamente ao usuário do token.

```bash
curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <cole-seu-token-aqui>" \
  -d '{"titulo": "Google", "url": "https://google.com"}'
```

**Resposta esperada (201 Created):**
```json
{
  "id": 1,
  "titulo": "Google",
  "url": "https://google.com",
  "criadoEm": "2024-03-27T14:30:00",
  "atualizadoEm": "2024-03-27T14:30:00"
}
```

Crie mais alguns para testar a listagem:
```bash
curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <cole-seu-token-aqui>" \
  -d '{"titulo": "GitHub", "url": "https://github.com"}'

curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <cole-seu-token-aqui>" \
  -d '{"titulo": "Stack Overflow", "url": "https://stackoverflow.com"}'
```

### 5. Listar todos os favoritos do usuário

Retorna apenas os favoritos do usuário autenticado — outros usuários não conseguem ver seus dados.

```bash
curl -X GET http://localhost:8080/favoritos \
  -H "Authorization: Bearer <cole-seu-token-aqui>"
```

**Resposta esperada (200 OK):**
```json
[
  {"id": 1, "titulo": "Google", "url": "https://google.com", "criadoEm": "...", "atualizadoEm": "..."},
  {"id": 2, "titulo": "GitHub", "url": "https://github.com", "criadoEm": "...", "atualizadoEm": "..."},
  {"id": 3, "titulo": "Stack Overflow", "url": "https://stackoverflow.com", "criadoEm": "...", "atualizadoEm": "..."}
]
```

### 6. Buscar um favorito por ID

```bash
curl -X GET http://localhost:8080/favoritos/1 \
  -H "Authorization: Bearer <cole-seu-token-aqui>"
```

**Resposta esperada (200 OK):**
```json
{
  "id": 1,
  "titulo": "Google",
  "url": "https://google.com",
  "criadoEm": "2024-03-27T14:30:00",
  "atualizadoEm": "2024-03-27T14:30:00"
}
```

> Se o ID não existir ou pertencer a outro usuário, retorna **404 Not Found**.

### 7. Atualizar um favorito

```bash
curl -X PUT http://localhost:8080/favoritos/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <cole-seu-token-aqui>" \
  -d '{"titulo": "Google Brasil", "url": "https://google.com.br"}'
```

**Resposta esperada (200 OK):**
```json
{
  "id": 1,
  "titulo": "Google Brasil",
  "url": "https://google.com.br",
  "criadoEm": "2024-03-27T14:30:00",
  "atualizadoEm": "2024-03-27T14:35:00"
}
```

> Note que o campo `atualizadoEm` muda, mas `criadoEm` permanece o mesmo.

### 8. Excluir um favorito

```bash
curl -X DELETE http://localhost:8080/favoritos/1 \
  -H "Authorization: Bearer <cole-seu-token-aqui>"
```

**Resposta esperada: 204 No Content** (sem corpo na resposta).

Para confirmar que foi excluído, tente buscar novamente:
```bash
curl -X GET http://localhost:8080/favoritos/1 \
  -H "Authorization: Bearer <cole-seu-token-aqui>"
```
Retornará **404 Not Found**.

### 9. Testando isolamento entre usuários

Cadastre um segundo usuário e obtenha o token dele:

```bash
curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Maria", "email": "maria@email.com", "senha": "654321"}'

curl -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "maria@email.com", "senha": "654321"}'
```

Agora tente acessar os favoritos do João usando o token da Maria:
```bash
curl -X GET http://localhost:8080/favoritos/2 \
  -H "Authorization: Bearer <token-da-maria>"
```

**Resposta: 404 Not Found** — Maria não tem acesso aos favoritos do João.

A listagem de `/favoritos` com o token da Maria retornará uma lista vazia, pois ela ainda não criou nenhum favorito.

### Dica: salvar o token em uma variável

Para facilitar os testes no terminal, salve o token em uma variável:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "joao@email.com", "senha": "123456"}' | jq -r '.token')

echo $TOKEN

# Agora use $TOKEN nas requisições:
curl -X GET http://localhost:8080/favoritos \
  -H "Authorization: Bearer $TOKEN"
```

> Requer `jq` instalado. No Mac: `brew install jq`. No Ubuntu: `sudo apt install jq`.

## Testes Automatizados

A aplicação possui **20 testes de integração** cobrindo:

- **Cadastro de usuário** — sucesso, email duplicado (409)
- **Login JWT** — sucesso com token, email inexistente (401), senha errada (401)
- **Rota protegida /eu** — acesso autenticado, acesso sem token (403)
- **CRUD de favoritos** — criar, listar, buscar, atualizar, excluir
- **Isolamento entre usuários** — um usuário não acessa favoritos de outro (404)
- **Proteção de rotas** — acesso sem token retorna 403
