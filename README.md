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

## Exemplos de Uso

### Cadastrar usuário
```bash
curl -X POST http://localhost:8080/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "João", "email": "joao@email.com", "senha": "123456"}'
```

### Fazer login
```bash
curl -X POST http://localhost:8080/entrar \
  -H "Content-Type: application/json" \
  -d '{"email": "joao@email.com", "senha": "123456"}'
```

### Criar favorito (com token)
```bash
curl -X POST http://localhost:8080/favoritos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <seu-token>" \
  -d '{"titulo": "Google", "url": "https://google.com"}'
```

## Testes Automatizados

A aplicação possui **20 testes de integração** cobrindo:

- **Cadastro de usuário** — sucesso, email duplicado (409)
- **Login JWT** — sucesso com token, email inexistente (401), senha errada (401)
- **Rota protegida /eu** — acesso autenticado, acesso sem token (403)
- **CRUD de favoritos** — criar, listar, buscar, atualizar, excluir
- **Isolamento entre usuários** — um usuário não acessa favoritos de outro (404)
- **Proteção de rotas** — acesso sem token retorna 403
