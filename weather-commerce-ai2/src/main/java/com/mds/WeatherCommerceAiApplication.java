package com.mds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Weather × Commerce AI 메인 애플리케이션
 *
 * <p>구성 요소:
 * <ul>
 *   <li>Spring Batch  — 날씨-매출 분석, 임베딩 인덱스 갱신 배치 Job</li>
 *   <li>Spring AI     — Ollama LLM 기반 상품 추천, RAG 채팅, 리포트 생성</li>
 *   <li>Spring Boot   — REST API (포트 8090)</li>
 * </ul>
 *
 * <p>컴포넌트 스캔: com.mds (및 하위 패키지 com.mds.weather.** 포함)
 *
 * <p>인프라 연동:
 * <ul>
 *   <li>PostgreSQL airflow DB — weather / raw / analytics 스키마</li>
 *   <li>Ollama — llama3.2 (채팅), nomic-embed-text (임베딩)</li>
 *   <li>OpenWeatherMap API — 1시간마다 실시간 날씨 수집</li>
 * </ul>
 *
 * <p>※ @EnableScheduling — WeatherFetchScheduler(1h), BatchScheduler 활성화
 */
@SpringBootApplication
@EnableScheduling
public class WeatherCommerceAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherCommerceAiApplication.class, args);
    }
}
