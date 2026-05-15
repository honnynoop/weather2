package com.mds.weather.batch;

import com.mds.weather.domain.WeatherData;
import com.mds.weather.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

/**
 * Job 2: PgVector 임베딩 인덱스 갱신 배치
 *
 * <pre>
 *   weather.weather_data (미처리 데이터)
 *      ↓  toText() 변환
 *   VectorStore.add(documents)  →  pgvector.vector_store 테이블
 *      ↓
 *   analytics.embedding_log  중복 방지 기록
 * </pre>
 *
 * <p>[수정] 사용하지 않는 import 제거:
 * - LocalDateTime (변수 선언 없이 메서드 체이닝만 사용)
 *
 * <p>[수정] Document 메타데이터 source_id 타입:
 * - Long → String.valueOf() 변환 (Spring AI 메타데이터 String 권장)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingIndexBatchConfig {

    private final WeatherDataRepository      weatherRepo;
    private final VectorStore                vectorStore;
    private final JdbcTemplate               jdbc;
    private final JobRepository              jobRepository;
    private final PlatformTransactionManager txManager;

    @Bean
    public Job embeddingIndexJob() {
        return new JobBuilder("embeddingIndexJob", jobRepository)
                .start(embeddingStep())
                .build();
    }

    @Bean
    public Step embeddingStep() {
        return new StepBuilder("embeddingStep", jobRepository)
                .tasklet(embeddingTasklet(), txManager)
                .build();
    }

    @Bean
    public Tasklet embeddingTasklet() {
        return (contribution, chunkContext) -> {
            List<WeatherData> notIndexed = weatherRepo.findNotEmbedded(100);

            if (notIndexed.isEmpty()) {
                log.info("[EmbeddingJob] 새로 임베딩할 데이터 없음");
                return RepeatStatus.FINISHED;
            }

            log.info("[EmbeddingJob] {}건 임베딩 처리 시작...", notIndexed.size());

            List<Document> documents = notIndexed.stream()
                    .map(w -> new Document(
                            w.toText(),
                            Map.of(
                                "source_id",   String.valueOf(w.getId()),  // Long → String
                                "source_type", "weather",
                                "city",        w.getCity(),
                                "collected_at", w.getCollectedAt().toString()
                            )
                    ))
                    .toList();

            vectorStore.add(documents);

            String logSql = """
                    INSERT INTO analytics.embedding_log (source_type, source_id, embedded_at)
                    VALUES ('weather', ?, NOW())
                    ON CONFLICT (source_type, source_id) DO NOTHING
                    """;
            for (WeatherData w : notIndexed) {
                jdbc.update(logSql, w.getId());
            }

            log.info("[EmbeddingJob] {}건 임베딩 완료 → vector_store 저장 ✓", notIndexed.size());
            return RepeatStatus.FINISHED;
        };
    }
}
