package com.example.favoritosapi.repository;

import com.example.favoritosapi.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    // Busca todos os favoritos de um usuário específico pelo ID do usuário
    List<Favorito> findByUsuarioId(Long usuarioId);
}
