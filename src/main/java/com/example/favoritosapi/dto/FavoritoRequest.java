package com.example.favoritosapi.dto;

/**
 * DTO para receber os dados de criação/atualização de um favorito.
 * O usuário é identificado pelo token JWT, sem precisar enviar idUsuario.
 */
public record FavoritoRequest(
        String titulo,
        String url
) {}
