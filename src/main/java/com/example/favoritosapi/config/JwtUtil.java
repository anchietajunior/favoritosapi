package com.example.favoritosapi.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Classe utilitária para geração e validação de tokens JWT.
 * O token carrega o ID do usuário como "subject" e o email como "claim".
 * Usa HMAC-SHA256 para assinar o token com a chave secreta.
 */
@Component
public class JwtUtil {

    // Chave secreta lida do application.properties
    private final SecretKey chave;

    // Token expira em 24 horas (em milissegundos)
    private static final long EXPIRACAO = 86400000;

    public JwtUtil(@Value("${jwt.secret}") String segredo) {
        // Gera a chave HMAC a partir da string configurada
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um token JWT contendo o ID e email do usuário.
     * O subject do token é o ID do usuário convertido para String.
     */
    public String gerarToken(Long usuarioId, String email) {
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))  // Subject = ID do usuário
                .claim("email", email)                // Claim personalizado com o email
                .issuedAt(new Date())                 // Data de emissão
                .expiration(new Date(System.currentTimeMillis() + EXPIRACAO))  // Data de expiração
                .signWith(chave)                      // Assina com HMAC-SHA256
                .compact();
    }

    /**
     * Valida o token e extrai todas as claims.
     * Lança exceção se o token for inválido ou expirado.
     */
    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)   // Verifica a assinatura com a mesma chave
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrai o ID do usuário a partir do token JWT.
     * O ID está armazenado no campo "subject" do token.
     */
    public Long extrairUsuarioId(String token) {
        return Long.parseLong(extrairClaims(token).getSubject());
    }

    /**
     * Verifica se o token ainda está dentro do prazo de validade.
     */
    public boolean tokenValido(String token) {
        try {
            Claims claims = extrairClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
