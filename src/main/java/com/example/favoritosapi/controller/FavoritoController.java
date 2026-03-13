package com.example.favoritosapi.controller;

import com.example.favoritosapi.model.Favorito;
import com.example.favoritosapi.repository.FavoritoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favoritos")
public class FavoritoController {
    private final FavoritoRepository repository;

    public FavoritoController(FavoritoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Favorito> listar() {
        return repository.findAll(); // SELECT * FROM favoritos;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Favorito> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Favorito> criar(@RequestBody Favorito favorito) {
        Favorito salvo = repository.save(favorito);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Favorito> atualizar(@PathVariable Long id, @RequestBody Favorito favorito) {
        return repository.findById(id)
                .map(fav -> {
                    fav.setTitulo(favorito.getTitulo());
                    fav.setUrl(favorito.getUrl());
                    Favorito salvo = repository.save(fav);
                    return ResponseEntity.ok(salvo);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Favorito> excluir(@PathVariable Long id) {
        if(!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
