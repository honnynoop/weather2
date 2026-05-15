package com.mds.weather.service;

import com.mds.weather.domain.WeatherSalesCorr;
import com.mds.weather.repository.WeatherSalesCorrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 날씨-매출 인사이트 리포트 생성 서비스
 *
 * <p>analytics.weather_sales_corr 집계 데이터를 llama3.2에 전달하여
 * 사람이 읽을 수 있는 비즈니스 인사이트 리포트를 자동 생성합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherReportService {

    private final ChatClient                chatClient;
    private final WeatherSalesCorrRepository corrRepo;

    /**
     * 지정 일수 기간의 날씨-매출 AI 리포트 생성
     *
     * @param days 분석 기간 (예: 7 → 최근 7일)
     * @return 마크다운 형식 AI 리포트
     */
    public String generateReport(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        LocalDate to   = LocalDate.now();

        List<WeatherSalesCorr> data = corrRepo.findBetween(from, to);

        if (data.isEmpty()) {
            return "분석 데이터가 없습니다. " +
                   "Airflow weather_etl_pipeline과 weatherAnalysisJob을 먼저 실행하세요.";
        }

        // 데이터를 텍스트로 변환
        String dataText = data.stream()
                .map(WeatherSalesCorr::toText)
                .collect(Collectors.joining("\n"));

        log.info("[Report] {}일치 데이터 {}건으로 리포트 생성 중...", days, data.size());

        return chatClient.prompt()
                .user(u -> u.text("""
                        아래는 최근 {days}일간의 날씨별 매출 분석 데이터입니다.

                        ## 원시 데이터
                        {data}

                        ## 리포트 작성 지침
                        다음 항목을 포함한 마케팅 인사이트 리포트를 작성해주세요:

                        1. **핵심 요약** (3~5문장)
                        2. **날씨와 매출의 상관관계** (어떤 날씨에 매출이 높았는지)
                        3. **도시별 특이사항** (도시마다 다른 패턴이 있다면)
                        4. **인기 카테고리 분석** (날씨별로 어떤 상품이 잘 팔렸는지)
                        5. **마케팅 제안** (향후 날씨 예보에 따른 프로모션 전략 3가지)

                        마크다운 형식으로 작성하고, 구체적인 수치를 반드시 포함하세요.
                        """)
                        .param("days", String.valueOf(days))
                        .param("data", dataText))
                .call()
                .content();
    }

    /**
     * 특정 도시의 날씨별 매출 요약 리포트
     */
    public String generateCityReport(String city) {
        List<WeatherSalesCorr> data = corrRepo.findByCityOrderByCalcDateDesc(city);
        if (data.isEmpty()) {
            return city + " 데이터가 없습니다.";
        }

        String dataText = data.stream()
                .limit(14)   // 최근 2주
                .map(WeatherSalesCorr::toText)
                .collect(Collectors.joining("\n"));

        return chatClient.prompt()
                .user(u -> u.text("""
                        {city}의 최근 날씨별 매출 데이터:
                        {data}

                        이 도시의 날씨 패턴이 매출에 미치는 영향을
                        3~5문장으로 간결하게 분석해주세요.
                        특히 어떤 날씨 조건에서 매출이 가장 높았는지,
                        어떤 상품 카테고리가 날씨와 연관성이 큰지 강조하세요.
                        """)
                        .param("city", city)
                        .param("data", dataText))
                .call()
                .content();
    }
}
