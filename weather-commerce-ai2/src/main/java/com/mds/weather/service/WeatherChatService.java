package com.mds.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.stereotype.Service;

/**
 * 날씨 × 매출 RAG(Retrieval-Augmented Generation) 채팅 서비스
 *
 * <p>Spring AI 1.0.0 GA QuestionAnswerAdvisor 생성 방식:
 * <ul>
 *   <li>생성자: new QuestionAnswerAdvisor(VectorStore) — 1개짜리만 존재</li>
 *   <li>2개짜리 (VectorStore, SearchRequest) 생성자 없음</li>
 *   <li>SearchRequest 전달은 반드시 Builder 패턴 사용:
 *       QuestionAnswerAdvisor.builder(vectorStore).searchRequest(...).build()</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherChatService {

    private final ChatClient  chatClient;
    private final VectorStore vectorStore;

    /**
     * RAG 기반 날씨-매출 Q&A
     *
     * @param question 사용자 질문
     * @return LLM 생성 답변
     */
    public String chat(String question) {
        log.debug("[RAG Chat] 질문: {}", question);

        return chatClient.prompt()
                .advisors(
                    QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(5)
                                .similarityThreshold(0.5)
                                .build())
                        .build()
                )
                .user(question)
                .call()
                .content();
    }

    /**
     * 도시 필터를 적용한 RAG 검색
     *
     * @param question 질문
     * @param city     필터 도시 (예: "Seoul")
     */
    public String chatWithCityFilter(String question, String city) {
        log.debug("[RAG Chat] 질문: {} | 도시 필터: {}", question, city);

        return chatClient.prompt()
                .advisors(
                    QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(5)
                                .filterExpression("city == '" + city + "'")
                                .build())
                        .build()
                )
                .user(question + " (도시: " + city + "에 대해서만 답변하세요)")
                .call()
                .content();
    }
}
