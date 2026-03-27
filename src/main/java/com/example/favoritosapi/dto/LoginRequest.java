package com.example.favoritosapi.dto;

/**
 * DTO para receber as credenciais de login do usuário.
 * Contém apenas email e senha — sem expor dados internos da entidade.
 */
public record LoginRequest(
        String email,
        String senha
) {}
