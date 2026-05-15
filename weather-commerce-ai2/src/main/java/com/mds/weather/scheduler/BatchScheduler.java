package com.mds.weather.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 배치 Job 자동 스케줄러
 *
 * <p>스케줄:
 * <ul>
 *   <li>날씨-매출 분석 Job : 매일 자정 01:00 (Airflow가 0시에 마지막 수집 완료 후)</li>
 *   <li>임베딩 인덱스 Job  : 6시간마다 (최신 날씨 데이터를 RAG 검색에 반영)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job         weatherAnalysisJob;
    private final Job         embeddingIndexJob;

    /**
     * 날씨-매출 상관관계 분석 — 매일 01:00
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void runWeatherAnalysis() {
        log.info("[Scheduler] weatherAnalysisJob 시작: {}", LocalDateTime.now());
        runJob(weatherAnalysisJob, "weatherAnalysis");
    }

    /**
     * 임베딩 인덱스 갱신 — 6시간마다
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void runEmbeddingIndex() {
        log.info("[Scheduler] embeddingIndexJob 시작: {}", LocalDateTime.now());
        runJob(embeddingIndexJob, "embeddingIndex");
    }

    /**
     * 공통 Job 실행 (파라미터에 현재 시간 포함 → 동일 Job 재실행 허용)
     */
    private void runJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("runAt", LocalDateTime.now().toString())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, params);
            log.info("[Scheduler] {} 완료: 상태={}", jobName, execution.getStatus());
        } catch (Exception e) {
            log.error("[Scheduler] {} 실행 실패: {}", jobName, e.getMessage(), e);
        }
    }
}
