package com.mdkj.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 客服 ChatClient
 */
@Configuration
@EnableConfigurationProperties(CustomerServiceAiProperties.class)
public class CustomerServiceAiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.openai", name = "api-key")
    public ChatClient customerServiceChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
