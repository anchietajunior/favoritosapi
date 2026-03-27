package com.example.favoritosapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro JWT que intercepta todas as requisições HTTP.
 * Extrai o token do header "Authorization", valida e injeta o usuário
 * no contexto de segurança do Spring para que os endpoints protegidos
 * possam identificar o usuário autenticado.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extrai o header Authorization da requisição
        String authHeader = request.getHeader("Authorization");

        // Verifica se o header existe e começa com "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Remove o prefixo "Bearer " para obter apenas o token
            String token = authHeader.substring(7);

            if (jwtUtil.tokenValido(token)) {
                Long usuarioId = jwtUtil.extrairUsuarioId(token);

                // Cria um objeto de autenticação do Spring Security com o ID do usuário
                var auth = new UsernamePasswordAuthenticationToken(
                        usuarioId, null, Collections.emptyList()
                );

                // Injeta a autenticação no contexto do Spring Security
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Salva o ID do usuário como atributo da requisição para fácil acesso nos controllers
                request.setAttribute("usuarioId", usuarioId);
            }
        }

        // Continua a cadeia de filtros (passa para o próximo filtro ou controller)
        filterChain.doFilter(request, response);
    }
}
