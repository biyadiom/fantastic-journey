package com.fantastic.springai.dto;

import java.util.List;

/**
 * Réponse chat enrichie : texte du modèle + traçage des outils / exécutions backend.
 */
public record ChatTraceResult(
        /** Texte renvoyé par le modèle (sans bloc de traçage). */
        String answerText,
        /** Même texte avec le bloc de traçage en fin (affichage). */
        String answerWithTrace,
        /** Noms d'outils + arguments tels que le LLM les a demandés (si présents dans la réponse). */
        List<String> llmToolCalls,
        /** Appels réellement exécutés sur {@code DatabaseToolsService} (JPQL / Spring Data via JPA). */
        List<String> executedServiceMethods,
        /**
         * Toujours null dans cette application : aucun SQL n'est généré par le LLM (outils Java uniquement).
         * Les requêtes SQL réelles sont produites par Hibernate à partir du JPQL ; activer show-sql pour les voir dans les logs.
         */
        String generatedSql) {
}
