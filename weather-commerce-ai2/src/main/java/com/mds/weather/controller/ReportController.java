package com.mds.weather.controller;

import com.mds.weather.service.WeatherReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 인사이트 리포트 API
 *
 * <p>엔드포인트:
 * <pre>
 *   GET /api/report?days=7        최근 N일 전체 리포트
 *   GET /api/report/city?city=Seoul  도시별 리포트
 * </pre>
 *
 * <p>사전 조건:
 * <ol>
 *   <li>{@code POST /api/mock/seed} — 날씨 데이터 생성</li>
 *   <li>{@code POST /api/batch/run/weather-analysis} — 날씨-매출 분석 배치 <strong>(필수)</strong></li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(
    name        = "리포트",
    description = "날씨·매출 분석 기반 AI 인사이트 리포트. "
                + "`POST /api/batch/run/weather-analysis` 실행 후 사용 가능합니다."
)
public class ReportController {

    private final WeatherReportService reportService;

    // =========================================================================
    //  GET /api/report  —  기간별 전체 리포트
    // =========================================================================

    /**
     * 기간별 전체 AI 인사이트 리포트
     *
     * <p>최근 N일간 날씨·매출 데이터를 종합 분석한 리포트를 생성합니다.
     *
     * @param days 분석 기간(일수). 기본값 7, 최대 90
     * @return 리포트 JSON
     */
    @Operation(
        summary     = "기간별 AI 인사이트 리포트",
        description = """
            최근 **N일**간의 날씨·매출 데이터를 종합 분석한 AI 리포트를 생성합니다.

            **리포트 포함 내용**
            - 기간 내 날씨 요약 (평균 기온·강수일수·맑은 날 비율)
            - 날씨별 매출 상관관계 분석
            - 카테고리별 날씨 민감도 분석
            - 마케팅 시사점 및 재고 전략 제언

            **사전 조건**: `POST /api/batch/run/weather-analysis` 실행 완료 필수

            **권장 분석 기간**: 7일 (주간) · 30일 (월간) · 90일 (분기)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "리포트 생성 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    name  = "7일 리포트 응답",
                    value = """
                        {
                          "period": "7일",
                          "report": "## 최근 7일 날씨-매출 분석 리포트\\n\\n### 핵심 인사이트\\n1. 강수일(3일) 우산·레인코트 매출 +234%\\n2. 맑은 날(4일) 야외용품·스포츠 매출 +89%\\n\\n### 마케팅 제언\\n- 기상청 API 연동 예보 기반 선제적 재고 확보 권장...",
                          "generatedAt": "2024-11-20T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "잘못된 파라미터 (days ≤ 0 또는 days > 90)"
        ),
        @ApiResponse(
            responseCode = "503",
            description  = "분석 배치 미실행 — `POST /api/batch/run/weather-analysis` 를 먼저 실행하세요"
        ),
        @ApiResponse(responseCode = "500", description = "AI 서비스 내부 오류")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> report(

            @Parameter(
                description = "분석 기간(일수). 기본값 7, 최대 90",
                example     = "7",
                schema      = @Schema(
                    type             = "integer",
                    minimum          = "1",
                    maximum          = "90",
                    defaultValue     = "7"
                )
            )
            @RequestParam(defaultValue = "7") int days) {

        log.info("[Report] 기간별 리포트 요청: 최근 {}일", days);

        String report = reportService.generateReport(days);

        return ResponseEntity.ok(Map.of(
                "period",      days + "일",
                "report",      report,
                "generatedAt", LocalDateTime.now().toString()
        ));
    }

    // =========================================================================
    //  GET /api/report/city  —  도시별 리포트
    // =========================================================================

    /**
     * 도시별 AI 인사이트 리포트
     *
     * <p>특정 도시의 날씨·매출 상관관계를 집중 분석한 리포트를 생성합니다.
     *
     * @param city 분석 대상 도시 (Seoul | Busan | Daegu | Incheon)
     * @return 도시별 리포트 JSON
     */
    @Operation(
        summary     = "도시별 AI 인사이트 리포트",
        description = """
            **특정 도시**의 날씨·매출 상관관계를 집중 분석한 리포트를 생성합니다.

            도시별 기후 특성과 소비 패턴 차이를 파악하여
            지역 맞춤 마케팅 전략 수립에 활용할 수 있습니다.

            **지원 도시**: Seoul · Busan · Daegu · Incheon

            **사전 조건**: `POST /api/batch/run/weather-analysis` 실행 완료 필수
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "도시별 리포트 생성 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    name  = "서울 리포트 응답",
                    value = """
                        {
                          "city":   "Seoul",
                          "report": "## 서울 날씨-매출 분석 리포트\\n\\n### 서울 기후 특성\\n사계절 뚜렷, 여름 고온다습, 겨울 한파...\\n\\n### 매출 인사이트\\n- 폭염(35°C↑): 냉방용품 +312%\\n- 강설: 방한용품 +267%...",
                          "generatedAt": "2024-11-20T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "지원하지 않는 도시명"),
        @ApiResponse(
            responseCode = "503",
            description  = "분석 배치 미실행 — `POST /api/batch/run/weather-analysis` 를 먼저 실행하세요"
        ),
        @ApiResponse(responseCode = "500", description = "AI 서비스 내부 오류")
    })
    @GetMapping("/city")
    public ResponseEntity<Map<String, Object>> cityReport(

            @Parameter(
                description = "분석 대상 도시명",
                example     = "Seoul",
                required    = true,
                schema      = @Schema(allowableValues = {"Seoul", "Busan", "Daegu", "Incheon"})
            )
            @RequestParam String city) {

        log.info("[Report] 도시별 리포트 요청: city='{}'", city);

        String report = reportService.generateCityReport(city);

        return ResponseEntity.ok(Map.of(
                "city",        city,
                "report",      report,
                "generatedAt", LocalDateTime.now().toString()
        ));
    }
}
