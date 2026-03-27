package com.example.favoritosapi.dto;

/**
 * DTO para receber os dados de cadastro de um novo usuário.
 * Usa Java Record para criar um objeto imutável com serialização automática.
 */
public record CadastroRequest(
        String nome,
        String email,
        String senha
) {}
