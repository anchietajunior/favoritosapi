package com.example.favoritosapi;

import com.example.favoritosapi.dto.CadastroRequest;
import com.example.favoritosapi.dto.LoginRequest;
import com.example.favoritosapi.repository.UsuarioRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para os endpoints de usuário.
 * Usa banco H2 em memória (perfil "test") e MockMvc para simular requisições HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limparBanco() {
        // Limpa o banco antes de cada teste para garantir isolamento
        usuarioRepository.deleteAll();
    }

    // --- Testes de Cadastro ---

    @Test
    @DisplayName("Deve cadastrar um novo usuário com sucesso e retornar 201")
    void deveCadastrarUsuarioComSucesso() throws Exception {
        var request = new CadastroRequest("João Silva", "joao@email.com", "senha123");

        mockMvc.perform(post("/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                // A senha não deve aparecer na resposta (protegida com @JsonIgnore)
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("Deve retornar 409 ao tentar cadastrar email duplicado")
    void deveRetornarConflitoPorEmailDuplicado() throws Exception {
        var request = new CadastroRequest("João Silva", "joao@email.com", "senha123");

        // Primeiro cadastro — sucesso
        mockMvc.perform(post("/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Segundo cadastro com mesmo email — conflito
        mockMvc.perform(post("/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Email já cadastrado"));
    }

    // --- Testes de Login ---

    @Test
    @DisplayName("Deve fazer login com sucesso e retornar um token JWT")
    void deveLogarComSucessoERetornarToken() throws Exception {
        // Cadastra o usuário primeiro
        var cadastro = new CadastroRequest("Maria", "maria@email.com", "senha456");
        mockMvc.perform(post("/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastro)))
                .andExpect(status().isCreated());

        // Faz login com as mesmas credenciais
        var login = new LoginRequest("maria@email.com", "senha456");
        mockMvc.perform(post("/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                // Verifica que o token JWT foi retornado e não está vazio
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Deve retornar 401 ao logar com email inexistente")
    void deveRetornar401ComEmailInexistente() throws Exception {
        var login = new LoginRequest("naoexiste@email.com", "senha123");

        mockMvc.perform(post("/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("Deve retornar 401 ao logar com senha incorreta")
    void deveRetornar401ComSenhaIncorreta() throws Exception {
        // Cadastra o usuário
        var cadastro = new CadastroRequest("Pedro", "pedro@email.com", "senhaCorreta");
        mockMvc.perform(post("/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastro)))
                .andExpect(status().isCreated());

        // Tenta logar com senha errada
        var login = new LoginRequest("pedro@email.com", "senhaErrada");
        mockMvc.perform(post("/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Credenciais inválidas"));
    }

    // --- Testes de Rota Protegida (/eu) ---

    @Test
    @DisplayName("Deve retornar dados do usuário autenticado na rota /eu")
    void deveRetornarDadosDoUsuarioAutenticado() throws Exception {
        // Cadastra e faz login para obter o token
        var cadastro = new CadastroRequest("Ana", "ana@email.com", "senha789");
        mockMvc.perform(post("/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastro)));

        var login = new LoginRequest("ana@email.com", "senha789");
        String resposta = mockMvc.perform(post("/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn().getResponse().getContentAsString();

        // Extrai o token da resposta JSON
        String token = objectMapper.readTree(resposta).get("token").asText();

        // Acessa /eu com o token JWT no header Authorization
        mockMvc.perform(get("/eu")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("Deve negar acesso à rota /eu sem token")
    void deveNegarAcessoAoAcessarEuSemToken() throws Exception {
        // Spring Security retorna 403 (Forbidden) por padrão quando não há autenticação
        mockMvc.perform(get("/eu"))
                .andExpect(status().isForbidden());
    }
}
