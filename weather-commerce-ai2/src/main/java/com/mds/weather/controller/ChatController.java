package com.mds.weather.controller;

import com.mds.weather.service.WeatherChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// =============================================================================
//  ✅ record — ChatController 클래스 밖 선언 (Lombok 충돌 방지)
// =============================================================================

/**
 * 기본 채팅 요청 바디 — 전체 날씨·매출 데이터 대상
 *
 * @param question 자연어 질문 (한국어 권장)
 */
@Schema(description = "기본 채팅 요청 — 전체 날씨·매출 데이터를 대상으로 질문합니다.")
record ChatRequest(

    @Schema(
        description = "자연어 질문 (한국어 권장)",
        example     = "비 올 때 어떤 상품이 잘 팔렸어?"
    )
    String question

) {}

/**
 * 도시 필터 채팅 요청 바디 — 특정 도시 데이터 대상
 *
 * @param question 자연어 질문
 * @param city     필터링할 도시명
 */
@Schema(description = "도시 필터 채팅 요청 — 특정 도시 데이터만 대상으로 질문합니다.")
record ChatWithCityRequest(

    @Schema(
        description = "자연어 질문 (한국어 권장)",
        example     = "기온이 30도 이상일 때 매출은?"
    )
    String question,

    @Schema(
        description     = "필터링할 도시명",
        example         = "Seoul",
        allowableValues = {"Seoul", "Busan", "Daegu", "Incheon"}
    )
    String city

) {}

// =============================================================================
//  ChatController
// =============================================================================

/**
 * 날씨-매출 RAG 채팅 API
 *
 * <p>엔드포인트:
 * <pre>
 *   POST /api/chat       전체 데이터 기반 RAG Q&amp;A
 *   POST /api/chat/city  도시 필터 RAG Q&amp;A
 * </pre>
 *
 * <p>사전 조건:
 * <ol>
 *   <li>{@code POST /api/mock/seed} — 날씨 데이터 생성</li>
 *   <li>{@code POST /api/batch/run/weather-analysis} — 날씨-매출 분석</li>
 *   <li>{@code POST /api/batch/run/embedding-index} — RAG 벡터 인덱스 생성 (필수)</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(
    name        = "RAG 채팅",
    description = "날씨·매출 벡터 DB 기반 자연어 Q&A. "
                + "`POST /api/batch/run/embedding-index` 실행 후 사용 가능합니다."
)
public class ChatController {

    private final WeatherChatService chatService;

    // =========================================================================
    //  POST /api/chat  —  전체 데이터 RAG Q&A
    // =========================================================================

    /**
     * 날씨·매출 벡터 DB 전체를 대상으로 자연어로 질문합니다.
     *
     * @param req 질문 요청 바디 {@link ChatRequest}
     * @return AI RAG 답변 JSON
     */
    @Operation(
        summary     = "전체 데이터 RAG Q&A",
        description = """
                날씨·매출 벡터 DB **전체**를 대상으로 자연어로 질문합니다.

                **사전 조건**: `POST /api/batch/run/embedding-index` 실행 완료 필수

                **활용 예시 질문**
                - "비 오는 날 가장 많이 팔린 상품 카테고리는?"
                - "기온 30도 이상일 때 매출 트렌드는?"
                - "서울과 부산의 우천 매출 차이는?"
                - "습도가 80% 이상인 날 고객 구매 패턴은?"
                """,

        // ✅ 에러 1 수정: @RequestBody를 파라미터가 아닌 @Operation 안으로 이동
        //    @io.swagger.v3.oas.annotations.parameters.RequestBody 는
        //    파라미터에 단독 선언 시 "disallowed here" 에러 발생
        //    → import 후 @Operation(requestBody = @RequestBody(...)) 패턴 사용
        requestBody = @RequestBody(
            description = "질문 본문",
            required    = true,
            content     = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = ChatRequest.class),

                // ✅ 에러 2 수정: """단일행""" → 이스케이프 일반 문자열
                //    Java 텍스트 블록은 """ 바로 뒤에 개행 필수
                //    """{"key":"val"}"""  ← 컴파일 오류
                //    "{\"key\":\"val\"}"  ← 정상
                examples = @ExampleObject(
                    name  = "우천 매출 질문",
                    value = "{\"question\": \"비 올 때 어떤 상품이 잘 팔렸어?\"}"
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "RAG 답변 성공",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = {
                    @ExampleObject(
                        name  = "우천 매출 질문",
                        value = """
                                {
                                  "question":   "비 올 때 어떤 상품이 잘 팔렸어?",
                                  "answer":     "강수량 10mm 이상인 날 분석 결과, 우산(+234%), 레인부츠(+180%), 방수 재킷(+152%) 순으로 매출이 증가했습니다.",
                                  "answeredAt": "2024-11-20T10:30:00"
                                }
                                """
                    ),
                    @ExampleObject(
                        name  = "고온 매출 질문",
                        value = """
                                {
                                  "question":   "기온 35도 이상일 때 잘 팔리는 건?",
                                  "answer":     "기온 35°C 이상 극서기에는 아이스크림(+312%), 아이스팩(+280%), 쿨링 스프레이(+240%)가 급증합니다.",
                                  "answeredAt": "2024-11-20T10:30:00"
                                }
                                """
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "400",
            description = "요청 본문 누락 또는 형식 오류"),
        @ApiResponse(responseCode = "503",
            description = "임베딩 인덱스 미생성 — `POST /api/batch/run/embedding-index` 먼저 실행"),
        @ApiResponse(responseCode = "500",
            description = "AI 서비스 내부 오류")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @org.springframework.web.bind.annotation.RequestBody ChatRequest req) {

        log.info("[Chat] RAG 채팅 요청: question='{}'", req.question());

        String answer = chatService.chat(req.question());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question",   req.question());
        body.put("answer",     answer);
        body.put("answeredAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(body);
    }

    // =========================================================================
    //  POST /api/chat/city  —  도시 필터 RAG Q&A
    // =========================================================================

    /**
     * 특정 도시 데이터만 필터링하여 자연어로 질문합니다.
     *
     * @param req 도시 필터 질문 요청 바디 {@link ChatWithCityRequest}
     * @return AI RAG 답변 JSON
     */
    @Operation(
        summary     = "도시 필터 RAG Q&A",
        description = """
                **특정 도시** 데이터만 필터링하여 자연어로 질문합니다.

                도시별 날씨·매출 패턴을 비교 분석하거나,
                특정 지역 특성에 집중하여 분석할 때 사용합니다.

                **지원 도시**: Seoul · Busan · Daegu · Incheon

                **사전 조건**: `POST /api/batch/run/embedding-index` 실행 완료 필수
                """,
        requestBody = @RequestBody(
            description = "도시 필터 질문 본문",
            required    = true,
            content     = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = ChatWithCityRequest.class),
                examples  = @ExampleObject(
                    name  = "서울 고온 질문",
                    value = "{\"question\": \"기온이 30도 이상일 때 매출은?\", \"city\": \"Seoul\"}"
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "RAG 답변 성공 (도시 필터 적용)",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    name  = "서울 고온 매출 질문",
                    value = """
                            {
                              "question":   "기온이 30도 이상일 때 매출은?",
                              "city":       "Seoul",
                              "answer":     "서울 기준 기온 30°C 이상인 날의 데이터를 분석한 결과, 냉방용품 매출이 전월 대비 189% 증가하였으며...",
                              "answeredAt": "2024-11-20T10:30:00"
                            }
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "400",
            description = "요청 본문 누락 또는 지원하지 않는 도시"),
        @ApiResponse(responseCode = "503",
            description = "임베딩 인덱스 미생성"),
        @ApiResponse(responseCode = "500",
            description = "AI 서비스 내부 오류")
    })
    @PostMapping("/city")
    public ResponseEntity<Map<String, Object>> chatWithCity(
            @org.springframework.web.bind.annotation.RequestBody ChatWithCityRequest req) {

        log.info("[Chat] 도시 필터 채팅 요청: question='{}', city='{}'",
                 req.question(), req.city());

        String answer = chatService.chatWithCityFilter(req.question(), req.city());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question",   req.question());
        body.put("city",       req.city());
        body.put("answer",     answer);
        body.put("answeredAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(body);
    }
}