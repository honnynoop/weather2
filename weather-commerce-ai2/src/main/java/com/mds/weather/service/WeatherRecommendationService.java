package com.mds.weather.service;

import com.mds.weather.domain.SalesOrder;
import com.mds.weather.domain.WeatherData;
import com.mds.weather.repository.SalesOrderRepository;
import com.mds.weather.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 날씨 기반 AI 상품 추천 서비스
 *
 * <p>현재 날씨 + 고객 구매 이력을 Ollama llama3.2에 전달하여
 * 개인화된 상품 추천을 생성합니다.
 *
 * <p>[수정] LazyInitializationException 해결
 * <ul>
 *   <li>원인: open-in-view=false 상태에서 @Transactional 없이
 *       SalesOrder.product (LAZY) 프록시에 접근</li>
 *   <li>해결: @Transactional(readOnly=true) 추가
 *       → 메서드 실행 중 Hibernate 세션이 유지되므로
 *         LAZY 프록시를 세션 안에서 초기화 가능</li>
 *   <li>추가 최적화: SalesOrderRepository에 JOIN FETCH 쿼리 사용
 *       → Product를 한 번의 쿼리로 함께 로드 (N+1 방지)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherRecommendationService {

    private final ChatClient              chatClient;
    private final WeatherDataRepository   weatherRepo;
    private final SalesOrderRepository    orderRepo;

    /**
     * 특정 고객 + 도시 날씨 기반 상품 추천
     *
     * @param city       날씨 조회 도시 (예: "Seoul")
     * @param customerId 고객 ID
     * @return AI 생성 추천 텍스트
     */
    @Transactional(readOnly = true)  // ← 핵심 수정: 세션 유지로 LAZY 프록시 초기화 허용
    public String recommend(String city, Long customerId) {

        // 1. 현재 날씨 조회
        WeatherData weather = weatherRepo
                .findTopByCityOrderByCollectedAtDesc(city)
                .orElseThrow(() -> new IllegalStateException(
                        city + " 날씨 데이터가 없습니다. Airflow DAG를 먼저 실행하세요."));

        // 2. 최근 구매 이력 조회 (JOIN FETCH로 Product 함께 로드 → N+1 방지)
        List<SalesOrder> history = orderRepo
                .findTop10WithProductByCustomerId(customerId);

        String historyText = history.isEmpty()
                ? "구매 이력 없음"
                : history.stream()
                    .map(o -> o.getProduct().getCategory()
                               + " - " + o.getProduct().getName())
                    .collect(Collectors.joining("\n"));

        log.debug("[Recommend] 도시={} 고객={} 날씨={}", city, customerId, weather.getDescription());

        // 3. LLM 프롬프트 구성 및 호출
        return chatClient.prompt()
                .user(u -> u.text("""
                        아래 정보를 바탕으로 고객에게 적합한 상품 3가지를 추천해주세요.

                        ## 현재 날씨 ({city})
                        {weatherSummary}

                        ## 고객 최근 구매 이력
                        {history}

                        ## 추천 형식
                        각 상품에 대해 다음을 포함하세요:
                        - 상품명 (또는 카테고리)
                        - 추천 이유 (날씨와의 연관성)
                        - 간단한 마케팅 문구 (한 문장)
                        """)
                        .param("city",          city)
                        .param("weatherSummary", weather.toText())
                        .param("history",        historyText))
                .call()
                .content();
    }

    /**
     * 도시 날씨만으로 카테고리 추천 (비로그인 사용자용)
     */
    @Transactional(readOnly = true)  // ← 동일하게 적용
    public String recommendByWeatherOnly(String city) {
        WeatherData weather = weatherRepo
                .findTopByCityOrderByCollectedAtDesc(city)
                .orElseThrow(() -> new IllegalStateException(city + " 날씨 데이터 없음"));

        return chatClient.prompt()
                .user(u -> u.text("""
                        현재 {city}의 날씨: {weatherSummary}

                        이 날씨에 어울리는 상품 카테고리 3가지와
                        각각의 추천 이유를 한국어로 간결하게 알려주세요.
                        """)
                        .param("city",          city)
                        .param("weatherSummary", weather.toText()))
                .call()
                .content();
    }
}
