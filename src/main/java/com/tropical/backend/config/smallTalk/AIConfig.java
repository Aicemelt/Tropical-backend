package com.tropical.backend.config.smallTalk;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 사용을 위한 설정 클래스
 *
 * 이 클래스는 Spring Bean으로 ChatClient를 생성하여 애플리케이션 전역에서
 * AI와의 채팅 기능을 사용할 수 있도록 구성합니다.
 *
 * @author 진도희
 * @since 2025.09.17
 */
@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
