package com.fantastic.springai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Garde un historique court par conversation (identifiant côté client).
 */
@Component
public class DatabaseConversationStore {

    private static final int MAX_TURNS = 20;

    private record Turn(String userMessage, String assistantReply) {
    }

    private final Map<String, List<Turn>> conversations = new ConcurrentHashMap<>();

    public String newConversationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * @param incoming id fourni par le client ; vide ou null → nouveau UUID
     */
    public String resolveConversationId(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return newConversationId();
        }
        return incoming;
    }

    public String formatHistoryForPrompt(String conversationId) {
        List<Turn> turns = conversations.get(conversationId);
        if (turns == null || turns.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Historique de la conversation :\n");
        for (Turn t : turns) {
            sb.append("Utilisateur : ").append(t.userMessage()).append('\n');
            sb.append("Assistant : ").append(t.assistantReply()).append("\n\n");
        }
        return sb.toString();
    }

    public void append(String conversationId, String userMessage, String assistantReply) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        conversations.compute(conversationId, (id, list) -> {
            List<Turn> turns = list != null ? new ArrayList<>(list) : new ArrayList<>();
            turns.add(new Turn(userMessage, assistantReply));
            while (turns.size() > MAX_TURNS) {
                turns.remove(0);
            }
            return turns;
        });
    }
}
