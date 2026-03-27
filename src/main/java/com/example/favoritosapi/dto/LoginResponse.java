package com.example.favoritosapi.dto;

/**
 * DTO para retornar o token JWT após login bem-sucedido.
 * Encapsula o token para não expor detalhes internos da autenticação.
 */
public record LoginResponse(
        String token
) {}
