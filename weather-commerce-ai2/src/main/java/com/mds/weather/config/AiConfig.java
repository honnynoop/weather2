package com.mds.weather.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 설정
 *
 * <p>OllamaChatModel, OllamaEmbeddingModel, PgVectorStore 는
 * Spring Boot autoconfiguration 이 자동으로 생성합니다 (application.yml 기반).
 * 여기서는 ChatClient 빈만 수동 등록합니다.
 */
@Configuration
public class AiConfig {

    /**
     * ChatClient — 모든 AI 서비스에서 주입받아 사용하는 고수준 클라이언트
     *
     * <p>기본 시스템 프롬프트를 설정하여 한국어 응답과
     * 이커머스 분석 맥락을 모든 대화에 적용합니다.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 한국 이커머스 날씨 데이터 분석 AI 어시스턴트입니다.
                        날씨 정보와 판매 데이터를 바탕으로 실용적인 인사이트를 제공합니다.
                        항상 한국어로 답변하고, 구체적인 수치와 근거를 포함하세요.
                        """)
                .build();
    }
}
