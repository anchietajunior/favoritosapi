package com.example.favoritosapi.repository;

import com.example.favoritosapi.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
}
