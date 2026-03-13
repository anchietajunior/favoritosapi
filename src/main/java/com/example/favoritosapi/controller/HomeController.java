package com.example.favoritosapi.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@RestController
public class HomeController {
    // Caminho no browser que o usuário acessa
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("mensagem", "api_favoritos");
    }
}
