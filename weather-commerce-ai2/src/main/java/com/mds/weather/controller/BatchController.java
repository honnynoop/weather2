package com.mds.weather.controller;

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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 배치 Job 수동 실행 API
 *
 * <p>엔드포인트:
 * <pre>
 *   POST /api/batch/run/weather-analysis   날씨-매출 분석 Job 실행
 *   POST /api/batch/run/embedding-index    RAG 벡터 인덱스 생성 Job 실행
 * </pre>
 *
 * <p>실행 순서:
 * <ol>
 *   <li>{@code POST /api/mock/seed} — 날씨 데이터 먼저 생성</li>
 *   <li>{@code POST /api/batch/run/weather-analysis} — 날씨-매출 분석</li>
 *   <li>{@code POST /api/batch/run/embedding-index} — RAG 인덱스 생성</li>
 * </ol>
 *
 * <p>주의: {@code spring.batch.job.enabled=false} 설정 필수
 * (미설정 시 앱 시작 시 Job이 자동 실행됩니다)
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Tag(
    name        = "배치",
    description = "날씨-매출 분석 및 RAG 임베딩 인덱스 생성 배치 Job 수동 트리거. "
                + "`POST /api/mock/seed` 로 데이터 생성 후 실행하세요."
)
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job         weatherAnalysisJob;   // Bean 이름: weatherAnalysisJob
    private final Job         embeddingIndexJob;    // Bean 이름: embeddingIndexJob

    // =========================================================================
    //  POST /api/batch/run/{jobName}  —  Job 수동 실행
    // =========================================================================

    /**
     * 배치 Job 즉시 수동 실행
     *
     * <p>지정된 Job을 즉시 실행합니다. 각 실행마다 고유한 {@code runAt} 파라미터로
     * 중복 실행 방지가 적용됩니다.
     *
     * @param jobName 실행할 Job 이름 ({@code weather-analysis} | {@code embedding-index})
     * @return Job 실행 결과 JSON (status: COMPLETED | FAILED | STARTED)
     * @throws Exception JobLauncher 실행 오류
     */
    @Operation(
        summary     = "배치 Job 수동 실행",
        description = """
            배치 Job을 즉시 실행합니다.

            | Job 이름 | 설명 | 선행 조건 |
            |---|---|---|
            | `weather-analysis` | 날씨-매출 상관관계 분석 → 리포트 사전 조건 | mock/seed 완료 |
            | `embedding-index`  | RAG 벡터 인덱스 생성 → 채팅 사전 조건     | weather-analysis 완료 |

            **권장 실행 순서**
            ```
            POST /api/batch/run/weather-analysis
            POST /api/batch/run/embedding-index
            ```

            **주의**: 동일 Job을 연속 실행하면 `runAt` 파라미터가 달라 항상 새 JobInstance가 생성됩니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Job 실행 완료 (status 값으로 결과 확인)",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = {
                    @ExampleObject(
                        name  = "weather-analysis 성공",
                        value = """
                            {
                              "jobName":   "weather-analysis",
                              "status":    "COMPLETED",
                              "startTime": "2024-11-20T10:30:00"
                            }
                            """
                    ),
                    @ExampleObject(
                        name  = "embedding-index 성공",
                        value = """
                            {
                              "jobName":   "embedding-index",
                              "status":    "COMPLETED",
                              "startTime": "2024-11-20T10:35:00"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "알 수 없는 Job 이름",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples  = @ExampleObject(
                    value = """
                        {
                          "error":   "알 수 없는 Job: unknown-job",
                          "hint":    "사용 가능: weather-analysis, embedding-index"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description  = "날씨 데이터 없음 — `POST /api/mock/seed` 를 먼저 실행하세요"
        ),
        @ApiResponse(responseCode = "500", description = "Job 실행 중 내부 오류")
    })
    @PostMapping("/run/{jobName}")
    public ResponseEntity<Map<String, Object>> runBatch(

            @Parameter(
                description = "실행할 배치 Job 이름",
                example     = "weather-analysis",
                required    = true,
                schema      = @Schema(
                    type             = "string",
                    allowableValues  = {"weather-analysis", "embedding-index"}
                )
            )
            @PathVariable String jobName) throws Exception {

        // ── Job 선택 ───────────────────────────────────────────────────────────
        Job job = switch (jobName) {
            case "weather-analysis" -> weatherAnalysisJob;
            case "embedding-index"  -> embeddingIndexJob;
            default -> throw new IllegalArgumentException(
                    "알 수 없는 Job: " + jobName +
                    " (사용 가능: weather-analysis, embedding-index)");
        };

        // ── 실행 파라미터 (매 실행마다 고유한 파라미터로 JobInstance 분리) ───────
        JobParameters params = new JobParametersBuilder()
                .addString("runAt",       LocalDateTime.now().toString())
                .addString("triggeredBy", "manual-api")
                .toJobParameters();

        log.info("[Batch] 수동 실행 시작: jobName='{}'", jobName);
        JobExecution execution = jobLauncher.run(job, params);
        log.info("[Batch] 실행 결과: jobName='{}', status='{}'",
                 jobName, execution.getStatus());

        return ResponseEntity.ok(Map.of(
                "jobName",   jobName,
                "status",    execution.getStatus().toString(),
                "startTime", LocalDateTime.now().toString()
        ));
    }
}
