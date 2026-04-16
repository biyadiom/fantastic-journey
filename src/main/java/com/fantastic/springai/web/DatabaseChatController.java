package com.fantastic.springai.web;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fantastic.springai.service.DatabaseQaService;
import com.fantastic.springai.service.DatabaseQaService.DatabaseAnswer;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatabaseChatController {

    private final DatabaseQaService databaseQaService;

    public DatabaseChatController(DatabaseQaService databaseQaService) {
        this.databaseQaService = databaseQaService;
    }

    /**
     * Pose une question en langage naturel ; le modèle génère du SQL, interroge PostgreSQL, puis reformule la réponse.
     * Renvoie {@code conversationId} à réutiliser tel quel pour les messages suivants afin de conserver le contexte.
     */
    @PostMapping("/chat/db")
    public Map<String, String> chatOnDatabase(@RequestBody ChatRequest request) {
        DatabaseAnswer answer = databaseQaService.answer(request.message(), request.conversationId());
        String sql = answer.executedSql();
        return Map.of(
                "reply", answer.reply(),
                "executedSql", sql != null ? sql : "",
                "conversationId", answer.conversationId());
    }

    /**
     * @param conversationId id renvoyé par la réponse précédente ; null ou absent pour démarrer une nouvelle conversation
     */
    public record ChatRequest(String message, String conversationId) {
    }
}
