package com.fantastic.springai.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/**
 * Erreurs réseau vers Ollama (souvent WSL2 : localhost ≠ machine Windows où Ollama écoute).
 */
@RestControllerAdvice
public class OllamaConnectionExceptionHandler {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> onResourceAccess(ResourceAccessException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Connexion à Ollama impossible (erreur réseau / I/O).");
        body.put("ollamaBaseUrl", ollamaBaseUrl);
        body.put("message", e.getMessage());
        Throwable cause = e.getCause();
        if (cause != null) {
            body.put("cause", cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
        body.put("hint", """
                1) Vérifiez qu'Ollama est démarré (ollama serve) et que le modèle llama3.2 est présent.
                2) Sous WSL2, si Ollama tourne sur Windows, localhost:11434 depuis Linux ne cible pas Windows. \
                Définissez par exemple : export OLLAMA_BASE_URL=http://$(awk '/nameserver/{print $2; exit}' /etc/resolv.conf):11434
                3) Sur Windows, si besoin, faites écouter Ollama sur toutes les interfaces (variable d'environnement OLLAMA_HOST=0.0.0.0 selon la doc Ollama).
                """);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
