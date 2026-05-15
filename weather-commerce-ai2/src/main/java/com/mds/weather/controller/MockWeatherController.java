package com.mds.weather.controller;

import com.mds.weather.service.MockWeatherService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 날씨 데이터 관리 API
 *
 * <p>엔드포인트:
 * <pre>
 *   GET  /api/mock/status           현재 데이터 현황 + AI API 준비 상태
 *   POST /api/mock/generate         지금 즉시 날씨 생성 (4개 도시)
 *   POST /api/mock/seed?days=7      N일치 히스토리 시딩
 *   GET  /api/mock/latest           최신 도시별 날씨 요약
 * </pre>
 *
 * <p>AI API 사용 전 반드시 이 컨트롤러의 API를 먼저 실행하여 날씨 데이터를 생성하세요.
 */
@Slf4j
@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
@Tag(
    name        = "Mock Data",
    description = "테스트용 날씨 데이터 생성·시딩·조회. "
                + "AI API 사용 전 `POST /api/mock/seed` 를 반드시 먼저 실행하세요."
)
public class MockWeatherController {

    private final MockWeatherService mockWeatherService;

    // =========================================================================
    //  GET /api/mock/status  —  전체 상태 확인
    // =========================================================================

    /**
     * 전체 데이터 현황 및 AI API 준비 상태 확인
     *
     * <p>날씨 레코드 수와 각 AI API(추천·채팅·리포트) 사용 가능 여부를 반환합니다.
     *
     * @return 상태 현황 JSON
     */
    @Operation(
        summary     = "데이터 현황 & AI API 준비 상태 확인",
        description = """
            현재 날씨 레코드 수와 AI API 사용 가능 여부를 확인합니다.

            **status 값**
            - `✅ READY` — 데이터 있음, AI API 즉시 사용 가능
            - `⚠️ NO_DATA` — `POST /api/mock/seed` 먼저 실행 필요

            이 엔드포인트로 **첫 진입점 체크** 후 다음 단계를 진행하세요.
            """
    )
    @ApiResponse(
        responseCode = "200",
        description  = "상태 반환 성공",
        content      = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            examples  = {
                @ExampleObject(
                    name  = "데이터 있음 (READY)",
                    value = """
                        {
                          "status":       "✅ READY",
                          "totalRecords": 112,
                          "cities":       ["Seoul", "Busan", "Daegu", "Incheon"],
                          "checkedAt":    "2024-11-20T10:30:00",
                          "aiApis": {
                            "recommend": "✅ 사용 가능",
                            "chat":      "✅ 사용 가능 (배치 후 RAG 가능)",
                            "report":    "✅ 사용 가능 (배치 후)"
                          },
                          "hint": "POST /api/batch/run/weather-analysis 실행 후 /api/report 사용 가능"
                        }
                        """
                ),
                @ExampleObject(
                    name  = "데이터 없음 (NO_DATA)",
                    value = """
                        {
                          "status":       "⚠️ NO_DATA",
                          "totalRecords": 0,
                          "cities":       ["Seoul", "Busan", "Daegu", "Incheon"],
                          "checkedAt":    "2024-11-20T10:30:00",
                          "aiApis": {
                            "recommend": "❌ 데이터 필요",
                            "chat":      "❌ 데이터 필요",
                            "report":    "❌ 데이터 필요"
                          },
                          "hint": "POST /api/mock/seed 를 먼저 실행하세요"
                        }
                        """
                )
            }
        )
    )
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {

        int     total = mockWeatherService.countRecords();
        boolean ready = total > 0;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status",       ready ? "✅ READY" : "⚠️ NO_DATA");
        res.put("totalRecords", total);
        res.put("cities",       MockWeatherService.CITIES);
        res.put("checkedAt",    LocalDateTime.now().toString());
        res.put("aiApis", Map.of(
            "recommend", ready ? "✅ 사용 가능"              : "❌ 데이터 필요",
            "chat",      ready ? "✅ 사용 가능 (배치 후 RAG 가능)" : "❌ 데이터 필요",
            "report",    ready ? "✅ 사용 가능 (배치 후)"     : "❌ 데이터 필요"
        ));
        res.put("hint", ready
            ? "POST /api/batch/run/weather-analysis 실행 후 /api/report 사용 가능"
            : "POST /api/mock/seed 를 먼저 실행하세요");

        return ResponseEntity.ok(res);
    }

    // =========================================================================
    //  POST /api/mock/generate  —  현재 시각 날씨 즉시 생성
    // =========================================================================

    /**
     * 4개 도시 현재 시각 기준 날씨 즉시 생성
     *
     * <p>현재 시각을 기준으로 Seoul·Busan·Daegu·Incheon 날씨 데이터를 각 1건씩 생성합니다.
     *
     * @return 생성 결과 JSON
     */
    @Operation(
        summary     = "현재 날씨 즉시 생성 (4개 도시)",
        description = """
            현재 시각 기준으로 4개 도시의 날씨 데이터를 즉시 1건씩 생성합니다.

            히스토리 없이 현재 날씨만 빠르게 생성할 때 사용합니다.
            충분한 히스토리 데이터를 원하면 `POST /api/mock/seed` 를 사용하세요.

            **생성 항목**: 기온 · 체감온도 · 강수량 · 습도 · 풍속 · 날씨 상태
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "4개 도시 날씨 생성 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    value = """
                        {
                          "message":         "4개 도시 현재 날씨 생성 완료",
                          "citiesGenerated": 4,
                          "cities":          ["Seoul", "Busan", "Daegu", "Incheon"],
                          "generatedAt":     "2024-11-20T10:30:00",
                          "nextStep":        "GET /api/mock/latest 로 결과 확인"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "날씨 데이터 생성 중 오류")
    })
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate() {

        log.info("[MockAPI] 날씨 즉시 생성 요청");
        int count = mockWeatherService.generateAll();

        return ResponseEntity.ok(Map.of(
            "message",         "4개 도시 현재 날씨 생성 완료",
            "citiesGenerated", count,
            "cities",          MockWeatherService.CITIES,
            "generatedAt",     LocalDateTime.now().toString(),
            "nextStep",        "GET /api/mock/latest 로 결과 확인"
        ));
    }

    // =========================================================================
    //  POST /api/mock/seed  —  N일치 히스토리 시딩
    // =========================================================================

    /**
     * N일치 과거 날씨 데이터 일괄 생성 (6시간 간격)
     *
     * <p>과거 N일을 6시간 간격(00:00, 06:00, 12:00, 18:00)으로
     * 4개 도시의 날씨 데이터를 일괄 생성합니다.
     *
     * <p>생성 레코드 수 = days × 4(시간대) × 4(도시)
     * <p>예: days=7 → 7 × 4 × 4 = 112건
     *
     * @param days 생성할 과거 일수 (기본 7, 최대 30)
     * @return 시딩 결과 및 다음 단계 안내 JSON
     */
    @Operation(
        summary     = "N일치 날씨 히스토리 시딩",
        description = """
            과거 N일을 **6시간 간격**으로 4개 도시의 날씨 데이터를 일괄 생성합니다.

            **생성 레코드 수** = 일수 × 4(시간대: 00·06·12·18시) × 4(도시)

            | days | 생성 레코드 |
            |---|---|
            | 7일  | 112건 |
            | 14일 | 224건 |
            | 30일 | 480건 |

            **실행 후 반드시 다음 단계를 순서대로 진행하세요**
            1. `POST /api/batch/run/weather-analysis`
            2. `POST /api/batch/run/embedding-index`
            3. AI API 사용
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "히스토리 시딩 완료",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    name  = "7일 시딩 성공",
                    value = """
                        {
                          "message":         "7일치 날씨 히스토리 생성 완료",
                          "daysGenerated":   7,
                          "recordsInserted": 112,
                          "elapsedMs":       342,
                          "nextSteps": [
                            "1. POST /api/batch/run/weather-analysis (날씨-매출 분석)",
                            "2. POST /api/batch/run/embedding-index  (RAG 인덱스 생성)",
                            "3. GET  /api/recommend?city=Seoul&customerId=1",
                            "4. POST /api/chat  {\\"question\\":\\"비 올때 잘 팔린 상품은?\\"}",
                            "5. GET  /api/report?days=7"
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "잘못된 파라미터 (days ≤ 0 또는 days > 30)"
        ),
        @ApiResponse(responseCode = "500", description = "시딩 중 DB 오류")
    })
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed(

            @Parameter(
                description = "생성할 과거 일수 (기본 7일, 최대 30일)",
                example     = "7",
                schema      = @Schema(
                    type         = "integer",
                    minimum      = "1",
                    maximum      = "30",
                    defaultValue = "7"
                )
            )
            @RequestParam(defaultValue = "7") int days) {

        log.info("[MockAPI] {}일치 히스토리 시딩 시작", days);
        long start = System.currentTimeMillis();
        int  count = mockWeatherService.seedHistory(days);
        long ms    = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
            "message",         days + "일치 날씨 히스토리 생성 완료",
            "daysGenerated",   days,
            "recordsInserted", count,
            "elapsedMs",       ms,
            "nextSteps",       List.of(
                "1. POST /api/batch/run/weather-analysis (날씨-매출 분석)",
                "2. POST /api/batch/run/embedding-index  (RAG 인덱스 생성)",
                "3. GET  /api/recommend?city=Seoul&customerId=1",
                "4. POST /api/chat  {\"question\":\"비 올때 잘 팔린 상품은?\"}",
                "5. GET  /api/report?days=7"
            )
        ));
    }

    // =========================================================================
    //  GET /api/mock/latest  —  최신 날씨 조회
    // =========================================================================

    /**
     * 도시별 최신 날씨 1건씩 요약 반환
     *
     * <p>4개 도시 각각의 가장 최신 날씨 레코드를 반환합니다.
     *
     * @return 도시별 최신 날씨 목록 JSON
     */
    @Operation(
        summary     = "도시별 최신 날씨 조회",
        description = """
            4개 도시의 **가장 최신 날씨 레코드** 1건씩을 반환합니다.

            `POST /api/mock/generate` 또는 `POST /api/mock/seed` 실행 후
            데이터가 정상 생성되었는지 확인할 때 사용합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "최신 날씨 조회 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    value = """
                        {
                          "count": 4,
                          "weather": [
                            {
                              "city":        "Seoul",
                              "temp":        22.5,
                              "feelsLike":   21.0,
                              "condition":   "맑음",
                              "humidity":    55,
                              "windSpeed":   3.2,
                              "rainfall":    0.0,
                              "recordedAt":  "2024-11-20T09:00:00"
                            },
                            {
                              "city":        "Busan",
                              "temp":        25.1,
                              "feelsLike":   26.3,
                              "condition":   "구름조금",
                              "humidity":    72,
                              "windSpeed":   5.8,
                              "rainfall":    0.0,
                              "recordedAt":  "2024-11-20T09:00:00"
                            }
                          ],
                          "queriedAt": "2024-11-20T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "200",
            description  = "날씨 데이터 없음 (count=0) — POST /api/mock/seed 먼저 실행"
        )
    })
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> latest() {

        List<Map<String, Object>> rows = mockWeatherService.getLatestSummary();

        return ResponseEntity.ok(Map.of(
            "count",     rows.size(),
            "weather",   rows,
            "queriedAt", LocalDateTime.now().toString()
        ));
    }
}
