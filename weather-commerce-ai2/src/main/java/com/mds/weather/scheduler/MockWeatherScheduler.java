package com.mds.weather.scheduler;

import com.mds.weather.service.MockWeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mock 날씨 자동 생성 스케줄러
 *
 * <p>기본: 매 정각 4개 도시 날씨 자동 생성
 * <p>설정으로 스케줄 변경 가능:
 * <pre>
 *   app.mock-weather.schedule: "0 0 * * * *"   # 매시간 (기본)
 *   app.mock-weather.schedule: "0 *\/10 * * * *" # 10분마다 (빠른 테스트)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockWeatherScheduler {

    private final MockWeatherService mockWeatherService;

    @Value("${app.mock-weather.enabled:true}")
    private boolean enabled;

    /**
     * 매시간 정각 실행 (cron 설정으로 변경 가능)
     * 기본값: 0 0 * * * * (매 정각)
     * 빠른 테스트: 0 *\/5 * * * * (5분마다)
     */
    @Scheduled(cron = "${app.mock-weather.schedule:0 0 * * * *}")
    public void generateHourly() {
        if (!enabled) return;
        int count = mockWeatherService.generateAll();
        log.debug("[MockWeatherScheduler] {}개 도시 날씨 생성", count);
    }
}
