package com.mds.weather.scheduler;

import com.mds.weather.service.WeatherFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * OpenWeatherMap API 1시간 주기 날씨 수집 스케줄러
 *
 * <p>매 정각마다 설정된 도시들의 현재 날씨를 조회하여
 * {@code weather.weather_data} 테이블에 저장합니다.
 *
 * <p>스케줄: {@code 0 0 * * * *} — 매 시 00분 00초 실행
 *
 * <p>활성화 조건: {@code weather.api.key}가 설정된 경우에만 동작
 * (키가 없으면 WeatherFetchService 빈 생성 자체를 막지 않고
 *  fetch 시 RestClient에서 401 예외가 발생하므로 로그로 확인 가능)
 *
 * @see WeatherFetchService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherFetchScheduler {

    private final WeatherFetchService weatherFetchService;

    /**
     * 1시간마다 모든 대상 도시의 날씨 수집
     *
     * <p>cron 표현식 필드 순서: 초 분 시 일 월 요일
     * <ul>
     *   <li>{@code 0 0 * * * *} → 매 시 0분 0초 실행</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 * * * *")
    public void fetchWeather() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("━━━ [WeatherFetchScheduler] 수집 시작: {} ━━━", startTime);

        try {
            int savedCount = weatherFetchService.fetchAllCities();
            log.info("━━━ [WeatherFetchScheduler] 수집 완료: {}개 도시 저장 ({}) ━━━",
                    savedCount, LocalDateTime.now());
        } catch (Exception e) {
            log.error("━━━ [WeatherFetchScheduler] 수집 중 오류: {} ━━━", e.getMessage(), e);
        }
    }
}
