package com.mds.weather.controller;

import com.mds.weather.service.WeatherRecommendationService;
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
 * 날씨 기반 AI 상품 추천 API
 *
 * <p>엔드포인트:
 * <pre>
 *   GET /api/recommend?city=Seoul&customerId=1   고객 맞춤 추천
 *   GET /api/recommend/anonymous?city=Busan       비로그인 추천
 * </pre>
 *
 * <p>사전 조건:
 * <ol>
 *   <li>{@code POST /api/mock/seed} — 날씨 데이터 생성</li>
 *   <li>{@code POST /api/batch/run/weather-analysis} — 분석 배치 실행 (선택, 품질 향상)</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
@Tag(
    name        = "AI 추천",
    description = "현재 날씨와 고객 구매 이력을 결합한 AI 상품 추천. "
                + "날씨 데이터가 없으면 `POST /api/mock/seed` 먼저 실행하세요."
)
public class RecommendationController {

    private final WeatherRecommendationService recommendService;

    // =========================================================================
    //  GET /api/recommend  —  고객 맞춤 추천
    // =========================================================================

    /**
     * 고객 맞춤 AI 상품 추천
     *
     * <p>현재 도시 날씨 + 고객 구매 이력을 분석하여 AI가 상품을 추천합니다.
     *
     * @param city       대상 도시 (Seoul | Busan | Daegu | Incheon)
     * @param customerId 고객 ID (raw.customers 테이블 기준)
     * @return 추천 결과 JSON
     */
    @Operation(
        summary     = "고객 맞춤 AI 추천",
        description = """
            현재 도시 날씨 + 고객 구매 이력을 분석하여 AI가 상품을 추천합니다.

            **사전 조건**
            - `POST /api/mock/seed` 로 날씨 데이터를 먼저 생성해야 합니다.
            - `POST /api/batch/run/weather-analysis` 실행 시 추천 품질이 향상됩니다.

            **지원 도시**: Seoul · Busan · Daegu · Incheon
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "추천 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    name  = "서울 맑음 — 여름 추천",
                    value = """
                        {
                          "city":           "Seoul",
                          "customerId":     1,
                          "recommendation": "오늘 서울은 맑고 기온이 32°C입니다. 고객님의 구매 이력을 바탕으로 자외선 차단제(SPF50), 쿨링 스프레이, 아이스 텀블러를 추천드립니다.",
                          "generatedAt":   "2024-11-20T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description  = "잘못된 파라미터 (지원하지 않는 도시 등)"),
        @ApiResponse(responseCode = "404", description  = "고객 ID를 찾을 수 없음"),
        @ApiResponse(responseCode = "503", description  = "날씨 데이터 없음 — POST /api/mock/seed 먼저 실행"),
        @ApiResponse(responseCode = "500", description  = "AI 서비스 내부 오류")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> recommend(

            @Parameter(
                description = "대상 도시명",
                example     = "Seoul",
                required    = true,
                schema      = @Schema(allowableValues = {"Seoul", "Busan", "Daegu", "Incheon"})
            )
            @RequestParam String city,

            @Parameter(
                description = "고객 ID (raw.customers 기준)",
                example     = "1",
                required    = true
            )
            @RequestParam Long customerId) {

        log.info("[Recommend] 고객 맞춤 추천 요청: city={}, customerId={}", city, customerId);

        String result = recommendService.recommend(city, customerId);

        return ResponseEntity.ok(Map.of(
                "city",           city,
                "customerId",     customerId,
                "recommendation", result,
                "generatedAt",    LocalDateTime.now().toString()
        ));
    }

    // =========================================================================
    //  GET /api/recommend/anonymous  —  비로그인 추천
    // =========================================================================

    /**
     * 익명(비로그인) 사용자 AI 상품 추천
     *
     * <p>로그인 없이 현재 날씨 정보만으로 상품을 추천합니다.
     *
     * @param city 대상 도시 (Seoul | Busan | Daegu | Incheon)
     * @return 추천 결과 JSON
     */
    @Operation(
        summary     = "익명 사용자 AI 추천",
        description = """
            로그인 없이 날씨 정보만으로 상품을 추천합니다.

            구매 이력이 없으므로 날씨 조건(기온·강수·습도·풍속)만 반영됩니다.
            개인화 추천을 원하면 `GET /api/recommend` 를 사용하세요.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "추천 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    name  = "부산 맑음 추천",
                    value = """
                        {
                          "city":           "Busan",
                          "recommendation": "부산 현재 맑음(28°C). 강한 자외선이 예상됩니다. 자외선 차단제, 선글라스, 쿨링 타올을 추천드립니다.",
                          "generatedAt":   "2024-11-20T10:30:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
        @ApiResponse(responseCode = "503", description = "날씨 데이터 없음 — POST /api/mock/seed 먼저 실행")
    })
    @GetMapping("/anonymous")
    public ResponseEntity<Map<String, Object>> recommendAnonymous(

            @Parameter(
                description = "대상 도시명",
                example     = "Busan",
                required    = true,
                schema      = @Schema(allowableValues = {"Seoul", "Busan", "Daegu", "Incheon"})
            )
            @RequestParam String city) {

        log.info("[Recommend] 익명 추천 요청: city={}", city);

        String result = recommendService.recommendByWeatherOnly(city);

        return ResponseEntity.ok(Map.of(
                "city",           city,
                "recommendation", result,
                "generatedAt",    LocalDateTime.now().toString()
        ));
    }
}
