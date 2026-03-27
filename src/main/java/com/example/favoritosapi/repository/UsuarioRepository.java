package com.example.favoritosapi.repository;

import com.example.favoritosapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório JPA para a entidade Usuario.
 * O Spring Data gera automaticamente a implementação das queries
 * a partir dos nomes dos métodos (Query Methods).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca um usuário pelo email — usado no login para verificar credenciais
    Optional<Usuario> findByEmail(String email);

    // Verifica se já existe um usuário com este email — usado no cadastro para evitar duplicatas
    boolean existsByEmail(String email);
}
