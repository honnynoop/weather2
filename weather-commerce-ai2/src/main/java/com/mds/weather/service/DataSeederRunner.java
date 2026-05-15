package com.mds.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 날씨 데이터 자동 시딩
 *
 * <p>weather.weather_data 테이블이 비어있으면 자동으로
 * 과거 N일치 Mock 날씨 데이터를 생성합니다.
 *
 * <p>설정 (application.yml):
 * <pre>
 *   app.mock-weather.auto-seed-on-startup: true
 *   app.mock-weather.seed-days: 7
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeederRunner implements ApplicationRunner {

    private final MockWeatherService mockWeatherService;

    @Value("${app.mock-weather.auto-seed-on-startup:true}")
    private boolean autoSeed;

    @Value("${app.mock-weather.seed-days:7}")
    private int seedDays;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoSeed) {
            log.info("[DataSeeder] auto-seed 비활성화 — 스킵");
            return;
        }

        int existing = mockWeatherService.countRecords();

        if (existing == 0) {
            log.info("┌─────────────────────────────────────────");
            log.info("│  [DataSeeder] 날씨 데이터 없음 감지");
            log.info("│  {}일치 Mock 데이터 자동 생성 시작...", seedDays);
            log.info("└─────────────────────────────────────────");

            long start = System.currentTimeMillis();
            int inserted = mockWeatherService.seedHistory(seedDays);
            long elapsed = System.currentTimeMillis() - start;

            log.info("┌─────────────────────────────────────────");
            log.info("│  [DataSeeder] 시딩 완료!");
            log.info("│  생성 건수: {}건", inserted);
            log.info("│  소요 시간: {}ms", elapsed);
            log.info("│  AI API 사용 가능: http://localhost:8090");
            log.info("└─────────────────────────────────────────");
        } else {
            log.info("[DataSeeder] 기존 날씨 데이터 {}건 — 시딩 스킵", existing);
        }
    }
}
