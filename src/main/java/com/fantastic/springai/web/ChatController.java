package com.fantastic.springai.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.fantastic.springai.dto.AskRequest;
import com.fantastic.springai.dto.ChatTraceResult;
import com.fantastic.springai.dto.ConversationRequest;
import com.fantastic.springai.dto.MessageDto;
import com.fantastic.springai.service.ChatService;

@RestController
@RequestMapping(path = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatController {

    private final ChatService chatService;
    private final RestClient restClient;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
        this.restClient = RestClient.create();
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return Map.of("error", "question requise");
        }
        ChatTraceResult r = chatService.askWithTrace(request.question().trim());
        return traceResponse(r);
    }

    @PostMapping("/conversation")
    public Map<String, Object> conversation(@RequestBody ConversationRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return Map.of("error", "question requise");
        }
        List<Message> history = toMessages(request.messages());
        ChatTraceResult r = chatService.askWithHistoryAndTrace(history, request.question().trim());
        return traceResponse(r);
    }

    private static Map<String, Object> traceResponse(ChatTraceResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("answer", r.answerWithTrace());
        m.put("answerText", r.answerText());
        m.put("llmToolCalls", r.llmToolCalls());
        m.put("executedServiceMethods", r.executedServiceMethods());
        m.put("generatedSql", r.generatedSql());
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    private static List<Message> toMessages(List<MessageDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        List<Message> list = new ArrayList<>();
        for (MessageDto m : dtos) {
            if (m.content() == null || m.content().isBlank()) {
                continue;
            }
            String role = m.role() == null ? "user" : m.role().toLowerCase(Locale.ROOT);
            switch (role) {
                case "user" -> list.add(new UserMessage(m.content()));
                case "assistant" -> list.add(new AssistantMessage(m.content()));
                default -> throw new IllegalArgumentException("Rôle non supporté : " + m.role());
            }
        }
        return list;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            String base = ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl.substring(0, ollamaBaseUrl.length() - 1) : ollamaBaseUrl;
            restClient.get()
                    .uri(base + "/api/tags")
                    .retrieve()
                    .toBodilessEntity();
            return Map.of("status", "UP", "ollama", "REACHABLE");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", String.valueOf(e.getMessage()));
        }
    }
}
