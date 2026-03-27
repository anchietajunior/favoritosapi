# Aula 00 - O que sao DTOs e por que precisamos deles

## O que e um DTO?

**DTO** significa **Data Transfer Object** (Objeto de Transferencia de Dados).
E uma classe simples cuja unica funcao e transportar dados entre camadas da aplicacao.

Um DTO **nao tem logica de negocio**, **nao acessa banco de dados**, **nao faz calculos**.
Ele so carrega dados de um ponto A para um ponto B.

---

## Por que nao usar a Entity diretamente?

### O Problema

Vamos olhar a entity `Usuario`:

```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;

    @JsonIgnore
    private String senha;

    private String cargo = "padrao";

    @JsonIgnore
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorito> favoritos = new ArrayList<>();
}
```

Quando alguem faz `POST /cadastrar`, envia este JSON:
```json
{
  "nome": "Joao",
  "email": "joao@email.com",
  "senha": "123456"
}
```

Se tentarmos receber diretamente como `Usuario`:

```java
@PostMapping("/cadastrar")
public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) {
    // usuario.getSenha() == null !!!
}
```

**A senha chega como `null`!** Porque `@JsonIgnore` impede o Jackson de ler o campo `senha`
do JSON. O `@JsonIgnore` e necessario para a **resposta** (nao queremos expor a senha),
mas ele tambem bloqueia a **requisicao**.

### Outros problemas de usar Entity como request:

1. **Campos indesejados**: O cliente poderia enviar `"id": 999` ou `"cargo": "admin"`
   e manipular dados que nao deveria.

2. **Acoplamento**: Se mudarmos a entity (ex: renomear coluna), a API muda junto.
   O contrato da API deve ser independente da estrutura do banco.

3. **Relacionamentos**: A entity `Favorito` tem `Usuario usuario` (objeto completo).
   No JSON de criacao, queremos receber so `idUsuario` (um numero). Nao faz sentido
   enviar o objeto `Usuario` inteiro so para criar um favorito.

---

## A solucao: DTOs

Criamos classes separadas para representar o que **entra** e o que **sai** da API:

### Exemplo: CadastroRequest

```java
public record CadastroRequest(String nome, String email, String senha) {
}
```

Agora o controller recebe o DTO e converte para Entity:

```java
@PostMapping("/cadastrar")
public ResponseEntity<?> cadastrar(@RequestBody CadastroRequest request) {
    Usuario usuario = new Usuario(
        request.nome(),
        request.email(),
        passwordEncoder.encode(request.senha()) // senha chega corretamente!
    );
    // ...
}
```

**O DTO resolve todos os problemas:**
- `senha` nao tem `@JsonIgnore`, entao o Jackson le normalmente.
- Nao tem campo `id` nem `cargo`, entao o cliente nao pode manipular.
- A entity pode mudar sem quebrar a API.

---

## Java Records como DTOs

A partir do Java 16, temos os **Records** - uma forma concisa de criar classes imutaveis:

```java
// Classe tradicional (precisa de getters, construtor, equals, hashCode, toString)
public class CadastroRequest {
    private final String nome;
    private final String email;
    private final String senha;

    public CadastroRequest(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    // + equals, hashCode, toString...
}

// Record (faz tudo automaticamente em uma linha!)
public record CadastroRequest(String nome, String email, String senha) {}
```

**Records geram automaticamente:**
- Construtor com todos os campos
- Metodos acessores (`nome()`, `email()`, `senha()` - sem prefixo "get")
- `equals()`, `hashCode()`, `toString()`
- Sao **imutaveis** (campos `final`, sem setters)

> Records sao perfeitos para DTOs porque DTOs so precisam carregar dados.

---

## DTOs neste projeto

Neste projeto vamos usar os seguintes DTOs:

| Record             | Tipo     | Campos               | Usado em                        |
|--------------------|----------|----------------------|---------------------------------|
| `CadastroRequest`  | Request  | nome, email, senha   | `POST /cadastrar`               |
| `LoginRequest`     | Request  | email, senha         | `POST /entrar`                  |
| `LoginResponse`    | Response | token                | `POST /entrar` (resposta)       |
| `FavoritoRequest`  | Request  | titulo, url          | `POST /favoritos`, `PUT /favoritos/{id}` |

### Fluxo visual:

```
Cliente                         Servidor
  |                                |
  |-- JSON -----> DTO Request ---->|
  |               (Record)        |--- Controller converte DTO -> Entity
  |                                |--- Repository salva Entity no banco
  |<---- DTO Response / Entity <---|
  |               (Record)        |
```

1. O **JSON da requisicao** e convertido automaticamente para um **Record** (DTO Request).
2. O **Controller** converte o Record em **Entity** e salva no banco.
3. A **Entity salva** (ou um Record de resposta) volta como JSON.

---

## Quando NAO usar DTOs?

Para manter o projeto simples, **nao criamos DTOs desnecessarios**:

- **Resposta de Favorito**: Retornamos a entity `Favorito` diretamente (com `@JsonIgnore` no `usuario`).
- **Resposta de Usuario**: Retornamos a entity `Usuario` diretamente (com `@JsonIgnore` na `senha` e `favoritos`).

Se a entity ja tem `@JsonIgnore` nos campos sensiveis e a resposta e exatamente o que queremos mostrar,
nao precisa de DTO de resposta. Usamos DTOs de resposta apenas quando a resposta difere da entity
(como `LoginResponse` que retorna um `token` que nao existe na entity).

---

## Resumo

| Problema                              | Solucao com DTO                          |
|---------------------------------------|------------------------------------------|
| `@JsonIgnore` bloqueia desserializacao| DTO nao tem `@JsonIgnore`                |
| Cliente pode enviar campos indesejados| DTO so tem os campos permitidos          |
| Entity tem objetos complexos          | DTO tem tipos simples (Long, String)     |
| Mudanca na entity quebra a API        | DTO isola a API da estrutura do banco    |

> **Regra simples**: Use DTO quando o que voce **recebe** ou **retorna** e diferente
> da entity. Use a entity diretamente quando ela ja representa bem o que voce precisa.
