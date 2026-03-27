package com.example.favoritosapi.controller;

import com.example.favoritosapi.config.JwtUtil;
import com.example.favoritosapi.dto.CadastroRequest;
import com.example.favoritosapi.dto.LoginRequest;
import com.example.favoritosapi.dto.LoginResponse;
import com.example.favoritosapi.model.Usuario;
import com.example.favoritosapi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UsuarioController {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UsuarioController(UsuarioRepository repository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Endpoint de cadastro de novo usuário.
     * Recebe um DTO para não expor a entidade diretamente.
     * A senha é criptografada com BCrypt antes de ser salva no banco.
     */
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody CadastroRequest request) {
        // Verifica se já existe um usuário com este email
        if (repository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erro", "Email já cadastrado"));
        }

        // Cria o usuário com a senha criptografada usando BCrypt
        String senhaCriptografada = passwordEncoder.encode(request.senha());
        Usuario usuario = new Usuario(request.nome(), request.email(), senhaCriptografada);

        Usuario salvo = repository.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    /**
     * Endpoint de login — valida credenciais e retorna um token JWT.
     * O token é usado nas próximas requisições para identificar o usuário.
     */
    @PostMapping("/entrar")
    public ResponseEntity<?> entrar(@RequestBody LoginRequest request) {
        // Busca o usuário pelo email informado
        var usuarioOpt = repository.findByEmail(request.email());

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Credenciais inválidas"));
        }

        Usuario usuario = usuarioOpt.get();

        // Compara a senha enviada com o hash BCrypt armazenado no banco
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Credenciais inválidas"));
        }

        // Gera o token JWT contendo o ID e email do usuário
        String token = jwtUtil.gerarToken(usuario.getId(), usuario.getEmail());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    /**
     * Endpoint que retorna os dados do usuário autenticado.
     * O ID do usuário é extraído do token JWT pelo filtro de segurança.
     */
    @GetMapping("/eu")
    public ResponseEntity<?> eu(@RequestAttribute("usuarioId") Long usuarioId) {
        return repository.findById(usuarioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
