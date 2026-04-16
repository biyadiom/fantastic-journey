package com.fantastic.springai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;

import com.fantastic.springai.dto.ChatTraceResult;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant analytique pour une plateforme e-commerce (schéma "shop").

            Tu peux répondre à des questions sur :
            - Les utilisateurs et leurs statistiques d'achat
            - Les commandes (statuts, montants, historique)
            - Les produits (stocks, ventes, catégories, prix)
            - Les vendeurs (performances, notes, ventes)
            - Les avis clients et notes
            - Les paiements et méthodes de paiement
            - Les catégories de produits

            Règles strictes :
            - Réponds toujours en français
            - Formate les montants avec € et 2 décimales
            - Formate les dates en dd/MM/yyyy
            - Tu ne peux PAS modifier, créer ou supprimer des données
            - Si une question est ambiguë, demande une précision avant d'appeler un outil
            - Si tu ne trouves pas de résultat, dis-le clairement
            - Utilise toujours les outils disponibles, ne devine jamais les données
            """;

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatTraceResult askWithTrace(String question) {
        ToolInvocationTraceHolder.clear();
        try {
            ChatClient.CallResponseSpec spec = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(question)
                    .call();
            // Un seul appel : content() puis chatResponse() épuise la chaîne d'advisors (IllegalStateException).
            ChatResponse chatResponse = spec.chatResponse();
            String text = extractAssistantText(chatResponse);
            List<String> llmTools = extractLlmToolCalls(chatResponse);
            List<String> executed = new ArrayList<>(ToolInvocationTraceHolder.snapshot());
            String footer = buildTraceFooter(llmTools, executed);
            return new ChatTraceResult(text, text + footer, llmTools, executed, null);
        } finally {
            ToolInvocationTraceHolder.clear();
        }
    }

    public ChatTraceResult askWithHistoryAndTrace(List<Message> history, String question) {
        ToolInvocationTraceHolder.clear();
        try {
            ChatClient.CallResponseSpec spec = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(history)
                    .user(question)
                    .call();
            ChatResponse chatResponse = spec.chatResponse();
            String text = extractAssistantText(chatResponse);
            List<String> llmTools = extractLlmToolCalls(chatResponse);
            List<String> executed = new ArrayList<>(ToolInvocationTraceHolder.snapshot());
            String footer = buildTraceFooter(llmTools, executed);
            return new ChatTraceResult(text, text + footer, llmTools, executed, null);
        } finally {
            ToolInvocationTraceHolder.clear();
        }
    }

    /**
     * Dernier segment de texte assistant non vide (réponse finale après tours d'outils éventuels).
     */
    private static String extractAssistantText(ChatResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return "";
        }
        List<Generation> gens = response.getResults();
        for (int i = gens.size() - 1; i >= 0; i--) {
            AssistantMessage msg = gens.get(i).getOutput();
            if (msg != null) {
                String t = msg.getText();
                if (t != null && !t.isBlank()) {
                    return t;
                }
            }
        }
        return "";
    }

    private static List<String> extractLlmToolCalls(ChatResponse response) {
        List<String> out = new ArrayList<>();
        if (response == null || response.getResults() == null) {
            return out;
        }
        for (Generation gen : response.getResults()) {
            AssistantMessage msg = gen.getOutput();
            if (msg == null || !msg.hasToolCalls()) {
                continue;
            }
            for (AssistantMessage.ToolCall tc : msg.getToolCalls()) {
                out.add(tc.name() + "(" + tc.arguments() + ")");
            }
        }
        return out;
    }

    private static String buildTraceFooter(List<String> llmToolCalls, List<String> executedServiceMethods) {
        if (llmToolCalls.isEmpty() && executedServiceMethods.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n---\n");
        sb.append("Traçage (ce mode n’utilise pas de SQL généré par le LLM ; accès données via outils Java → JPA / Spring Data)\n");
        if (!llmToolCalls.isEmpty()) {
            sb.append("Outils demandés par le modèle :\n");
            for (String line : llmToolCalls) {
                sb.append("  • ").append(line).append('\n');
            }
        }
        if (!executedServiceMethods.isEmpty()) {
            sb.append("Méthodes exécutées (service) :\n");
            for (String line : executedServiceMethods) {
                sb.append("  • ").append(line).append('\n');
            }
        }
        sb.append("SQL Hibernate : voir les logs si spring.jpa.show-sql=true (requêtes traduites depuis le JPQL).\n");
        return sb.toString();
    }
}
