package com.fantastic.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fantastic.springai.service.DatabaseToolsService;

@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, DatabaseToolsService databaseToolsService) {
        return chatClientBuilder.defaultTools(databaseToolsService).build();
    }
}
