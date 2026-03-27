package com.example.favoritosapi.controller;

import com.example.favoritosapi.dto.FavoritoRequest;
import com.example.favoritosapi.model.Favorito;
import com.example.favoritosapi.model.Usuario;
import com.example.favoritosapi.repository.FavoritoRepository;
import com.example.favoritosapi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsável pelo CRUD de favoritos.
 * Todas as operações são filtradas pelo usuário autenticado via JWT.
 * O ID do usuário é injetado automaticamente pelo JwtFilter como atributo da requisição.
 */
@RestController
@RequestMapping("/favoritos")
public class FavoritoController {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;

    public FavoritoController(FavoritoRepository favoritoRepository, UsuarioRepository usuarioRepository) {
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Lista apenas os favoritos do usuário autenticado
    @GetMapping
    public List<Favorito> listar(@RequestAttribute("usuarioId") Long usuarioId) {
        return favoritoRepository.findByUsuarioId(usuarioId);
    }

    // Busca um favorito por ID — retorna 404 se não pertencer ao usuário autenticado
    @GetMapping("/{id}")
    public ResponseEntity<Favorito> buscarPorId(@PathVariable Long id,
                                                @RequestAttribute("usuarioId") Long usuarioId) {
        return favoritoRepository.findById(id)
                .filter(fav -> fav.getUsuario().getId().equals(usuarioId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo favorito associado ao usuário autenticado.
     * Recebe um DTO (FavoritoRequest) em vez da entidade diretamente,
     * assim o cliente não precisa enviar o ID do usuário.
     */
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody FavoritoRequest request,
                                   @RequestAttribute("usuarioId") Long usuarioId) {
        // Busca o usuário no banco para associar ao favorito
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Favorito favorito = new Favorito();
        favorito.setTitulo(request.titulo());
        favorito.setUrl(request.url());
        favorito.setUsuario(usuario);

        Favorito salvo = favoritoRepository.save(favorito);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // Atualiza um favorito — verifica se pertence ao usuário autenticado antes de alterar
    @PutMapping("/{id}")
    public ResponseEntity<Favorito> atualizar(@PathVariable Long id,
                                              @RequestBody FavoritoRequest request,
                                              @RequestAttribute("usuarioId") Long usuarioId) {
        return favoritoRepository.findById(id)
                .filter(fav -> fav.getUsuario().getId().equals(usuarioId))
                .map(fav -> {
                    fav.setTitulo(request.titulo());
                    fav.setUrl(request.url());
                    Favorito salvo = favoritoRepository.save(fav);
                    return ResponseEntity.ok(salvo);
                }).orElse(ResponseEntity.notFound().build());
    }

    // Exclui um favorito — verifica se pertence ao usuário autenticado antes de remover
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                        @RequestAttribute("usuarioId") Long usuarioId) {
        return favoritoRepository.findById(id)
                .filter(fav -> fav.getUsuario().getId().equals(usuarioId))
                .map(fav -> {
                    favoritoRepository.delete(fav);
                    return ResponseEntity.noContent().<Void>build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
