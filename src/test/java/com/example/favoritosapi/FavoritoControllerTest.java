package com.example.favoritosapi;

import com.example.favoritosapi.dto.CadastroRequest;
import com.example.favoritosapi.dto.FavoritoRequest;
import com.example.favoritosapi.dto.LoginRequest;
import com.example.favoritosapi.repository.FavoritoRepository;
import com.example.favoritosapi.repository.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o CRUD de favoritos.
 * Cada teste cadastra um usuário, faz login para obter o token JWT,
 * e usa esse token para realizar operações nos favoritos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String tokenUsuario1;
    private String tokenUsuario2;

    @BeforeEach
    void configurar() throws Exception {
        // Limpa o banco antes de cada teste
        favoritoRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Cadastra e faz login do usuário 1
        tokenUsuario1 = cadastrarELogar("User1", "user1@email.com", "senha123");

        // Cadastra e faz login do usuário 2 (para testar isolamento entre usuários)
        tokenUsuario2 = cadastrarELogar("User2", "user2@email.com", "senha456");
    }

    /**
     * Método auxiliar que cadastra um usuário e retorna o token JWT.
     * Usado no @BeforeEach para configurar os testes.
     */
    private String cadastrarELogar(String nome, String email, String senha) throws Exception {
        var cadastro = new CadastroRequest(nome, email, senha);
        mockMvc.perform(post("/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cadastro)));

        var login = new LoginRequest(email, senha);
        String resposta = mockMvc.perform(post("/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resposta).get("token").asText();
    }

    // --- Testes de Criação ---

    @Test
    @DisplayName("Deve criar um favorito com sucesso e retornar 201")
    void deveCriarFavoritoComSucesso() throws Exception {
        var request = new FavoritoRequest("Google", "https://google.com");

        mockMvc.perform(post("/favoritos")
                        .header("Authorization", "Bearer " + tokenUsuario1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Google"))
                .andExpect(jsonPath("$.url").value("https://google.com"))
                // Timestamps devem ser preenchidos automaticamente
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.atualizadoEm").exists());
    }

    @Test
    @DisplayName("Deve negar acesso ao criar favorito sem token")
    void deveNegarAcessoAoCriarSemToken() throws Exception {
        var request = new FavoritoRequest("Google", "https://google.com");

        // Spring Security retorna 403 (Forbidden) por padrão quando não há autenticação
        mockMvc.perform(post("/favoritos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- Testes de Listagem ---

    @Test
    @DisplayName("Deve listar apenas os favoritos do usuário autenticado")
    void deveListarApenasFavoritosDoUsuarioAutenticado() throws Exception {
        // Usuário 1 cria dois favoritos
        criarFavorito("Google", "https://google.com", tokenUsuario1);
        criarFavorito("GitHub", "https://github.com", tokenUsuario1);

        // Usuário 2 cria um favorito
        criarFavorito("Twitter", "https://twitter.com", tokenUsuario2);

        // Usuário 1 deve ver apenas seus 2 favoritos
        mockMvc.perform(get("/favoritos")
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].titulo", containsInAnyOrder("Google", "GitHub")));

        // Usuário 2 deve ver apenas seu 1 favorito
        mockMvc.perform(get("/favoritos")
                        .header("Authorization", "Bearer " + tokenUsuario2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("Twitter"));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o usuário não tem favoritos")
    void deveRetornarListaVazia() throws Exception {
        mockMvc.perform(get("/favoritos")
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Testes de Busca por ID ---

    @Test
    @DisplayName("Deve buscar favorito por ID com sucesso")
    void deveBuscarFavoritoPorId() throws Exception {
        Long id = criarFavorito("Google", "https://google.com", tokenUsuario1);

        mockMvc.perform(get("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Google"))
                .andExpect(jsonPath("$.url").value("https://google.com"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar favorito de outro usuário")
    void deveRetornar404AoBuscarFavoritoDeOutroUsuario() throws Exception {
        // Usuário 1 cria um favorito
        Long id = criarFavorito("Google", "https://google.com", tokenUsuario1);

        // Usuário 2 tenta acessar o favorito do usuário 1 — deve retornar 404
        mockMvc.perform(get("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar favorito inexistente")
    void deveRetornar404ParaFavoritoInexistente() throws Exception {
        mockMvc.perform(get("/favoritos/99999")
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isNotFound());
    }

    // --- Testes de Atualização ---

    @Test
    @DisplayName("Deve atualizar um favorito com sucesso")
    void deveAtualizarFavoritoComSucesso() throws Exception {
        Long id = criarFavorito("Google", "https://google.com", tokenUsuario1);

        var update = new FavoritoRequest("Google BR", "https://google.com.br");

        mockMvc.perform(put("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Google BR"))
                .andExpect(jsonPath("$.url").value("https://google.com.br"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar atualizar favorito de outro usuário")
    void deveRetornar404AoAtualizarFavoritoDeOutroUsuario() throws Exception {
        Long id = criarFavorito("Google", "https://google.com", tokenUsuario1);

        var update = new FavoritoRequest("Hackeado", "https://hacker.com");

        // Usuário 2 tenta atualizar o favorito do usuário 1
        mockMvc.perform(put("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // --- Testes de Exclusão ---

    @Test
    @DisplayName("Deve excluir um favorito com sucesso e retornar 204")
    void deveExcluirFavoritoComSucesso() throws Exception {
        Long id = criarFavorito("Google", "https://google.com", tokenUsuario1);

        // Exclui o favorito
        mockMvc.perform(delete("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isNoContent());

        // Verifica que o favorito foi removido
        mockMvc.perform(get("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar excluir favorito de outro usuário")
    void deveRetornar404AoExcluirFavoritoDeOutroUsuario() throws Exception {
        Long id = criarFavorito("Google", "https://google.com", tokenUsuario1);

        // Usuário 2 tenta excluir o favorito do usuário 1
        mockMvc.perform(delete("/favoritos/" + id)
                        .header("Authorization", "Bearer " + tokenUsuario2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar excluir favorito inexistente")
    void deveRetornar404AoExcluirFavoritoInexistente() throws Exception {
        mockMvc.perform(delete("/favoritos/99999")
                        .header("Authorization", "Bearer " + tokenUsuario1))
                .andExpect(status().isNotFound());
    }

    // --- Método Auxiliar ---

    /**
     * Cria um favorito via API e retorna o ID gerado.
     * Usado como helper nos testes para preparar o cenário.
     */
    private Long criarFavorito(String titulo, String url, String token) throws Exception {
        var request = new FavoritoRequest(titulo, url);

        String resposta = mockMvc.perform(post("/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(resposta);
        return json.get("id").asLong();
    }
}
