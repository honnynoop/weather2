package com.mds.weather.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Job 1: 날씨-매출 상관관계 분석 배치
 *
 * <pre>
 *   [Reader]  weather.weather_data + raw.orders JOIN (오늘 날짜)
 *      ↓
 *   [Processor]  WeatherSalesRow → 검증/로깅
 *      ↓
 *   [Writer]  analytics.weather_sales_corr UPSERT
 * </pre>
 *
 * <p>[수정] 사용하지 않는 import 제거:
 * - DataClassRowMapper (수동 rowMapper 사용)
 * - LocalDate (SQL에서 CURRENT_DATE 직접 사용)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeatherAnalysisBatchConfig {

    private final DataSource                 dataSource;
    private final JdbcTemplate               jdbc;
    private final JobRepository              jobRepository;
    private final PlatformTransactionManager txManager;

    @Bean
    public Job weatherAnalysisJob() {
        return new JobBuilder("weatherAnalysisJob", jobRepository)
                .start(weatherAnalysisStep())
                .listener(jobExecutionListener())
                .build();
    }

    @Bean
    public Step weatherAnalysisStep() {
        return new StepBuilder("weatherAnalysisStep", jobRepository)
                .<WeatherSalesRow, WeatherSalesRow>chunk(10, txManager)
                .reader(weatherSalesReader())
                .processor(weatherSalesProcessor())
                .writer(weatherSalesWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<WeatherSalesRow> weatherSalesReader() {
        String sql = """
                SELECT
                    w.city,
                    w.description                                     AS weather_desc,
                    AVG(w.temperature_c)                              AS avg_temp_c,
                    COUNT(DISTINCT o.order_id)                        AS total_orders,
                    COALESCE(
                        SUM(o.quantity * o.unit_price * (1 - COALESCE(o.discount_rate, 0))),
                        0
                    )                                                 AS total_revenue,
                    COALESCE(
                        (SELECT p2.category
                         FROM raw.orders o2
                         JOIN raw.products p2 ON o2.product_id = p2.product_id
                         JOIN raw.customers c2 ON o2.customer_id = c2.customer_id
                         WHERE o2.order_date = CURRENT_DATE
                           AND o2.status = 'completed'
                           AND c2.city LIKE (
                               CASE w.city
                                   WHEN 'Seoul'   THEN '서울%'
                                   WHEN 'Busan'   THEN '부산%'
                                   WHEN 'Incheon' THEN '인천%'
                                   WHEN 'Daegu'   THEN '대구%'
                                   ELSE w.city || '%'
                               END)
                         GROUP BY p2.category
                         ORDER BY SUM(o2.quantity) DESC
                         LIMIT 1),
                        '데이터 없음'
                    )                                                 AS top_category
                FROM weather.weather_data w
                LEFT JOIN raw.customers c ON c.city LIKE (
                    CASE w.city
                        WHEN 'Seoul'   THEN '서울%'
                        WHEN 'Busan'   THEN '부산%'
                        WHEN 'Incheon' THEN '인천%'
                        WHEN 'Daegu'   THEN '대구%'
                        ELSE w.city || '%'
                    END)
                LEFT JOIN raw.orders o ON o.customer_id = c.customer_id
                    AND o.order_date = CURRENT_DATE
                    AND o.status = 'completed'
                WHERE DATE(w.collected_at) = CURRENT_DATE
                GROUP BY w.city, w.description
                ORDER BY w.city
                """;

        return new JdbcCursorItemReaderBuilder<WeatherSalesRow>()
                .name("weatherSalesReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper((rs, i) -> new WeatherSalesRow(
                        rs.getString("city"),
                        rs.getString("weather_desc"),
                        rs.getBigDecimal("avg_temp_c"),
                        rs.getInt("total_orders"),
                        rs.getBigDecimal("total_revenue"),
                        rs.getString("top_category")
                ))
                .build();
    }

    @Bean
    public ItemProcessor<WeatherSalesRow, WeatherSalesRow> weatherSalesProcessor() {
        return row -> {
            log.info("[Batch] 처리 중: {} | {}°C | 주문 {}건 | {}원",
                    row.city(), row.avgTempC(), row.totalOrders(), row.totalRevenue());
            return row;
        };
    }

    @Bean
    public ItemWriter<WeatherSalesRow> weatherSalesWriter() {
        return chunk -> {
            String upsertSql = """
                    INSERT INTO analytics.weather_sales_corr
                        (city, weather_desc, avg_temp_c, total_orders, total_revenue, top_category, calc_date, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, NOW())
                    ON CONFLICT (city, calc_date)
                    DO UPDATE SET
                        weather_desc  = EXCLUDED.weather_desc,
                        avg_temp_c    = EXCLUDED.avg_temp_c,
                        total_orders  = EXCLUDED.total_orders,
                        total_revenue = EXCLUDED.total_revenue,
                        top_category  = EXCLUDED.top_category,
                        created_at    = NOW()
                    """;
            for (WeatherSalesRow row : chunk.getItems()) {
                jdbc.update(upsertSql,
                        row.city(), row.weatherDesc(), row.avgTempC(),
                        row.totalOrders(), row.totalRevenue(), row.topCategory());
            }
            log.info("[Batch] analytics.weather_sales_corr {}건 저장 완료", chunk.size());
        };
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution je) {
                log.info("=== [WeatherAnalysisJob] 시작: {} ===", LocalDateTime.now());
            }
            @Override
            public void afterJob(JobExecution je) {
                log.info("=== [WeatherAnalysisJob] 완료: {} | 상태: {} ===",
                        LocalDateTime.now(), je.getStatus());
            }
        };
    }

    /** 내부 DTO */
    public record WeatherSalesRow(
            String city,
            String weatherDesc,
            BigDecimal avgTempC,
            int totalOrders,
            BigDecimal totalRevenue,
            String topCategory
    ) {}
}
