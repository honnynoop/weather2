package com.mds.weather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mock 날씨 데이터 생성 서비스
 *
 * <p>OpenWeatherMap API 없이 한국 기후 패턴을 기반으로
 * 현실적인 날씨 데이터를 생성하여 weather.weather_data 에 저장합니다.
 *
 * <p>계절별 기온 (서울 기준):
 * <pre>
 *   1월: -2°C   4월: 13°C   7월: 27°C   10월: 15°C
 *   2월:  0°C   5월: 18°C   8월: 28°C   11월:  7°C
 *   3월:  7°C   6월: 23°C   9월: 22°C   12월:  1°C
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockWeatherService {

    private final JdbcTemplate jdbc;

    // ── 수집 도시 ────────────────────────────────────────────
    public static final List<String> CITIES =
            List.of("Seoul", "Busan", "Incheon", "Daegu");

    // ── 도시별 기온 오프셋 (서울 대비) ───────────────────────
    private static final Map<String, Double> CITY_OFFSET = Map.of(
            "Seoul",   0.0,
            "Busan",   2.5,   // 해양성, 겨울 온난
            "Incheon", -1.0,  // 해풍, 체감 낮음
            "Daegu",   3.5    // 분지, 여름 혹서
    );

    // ── 월별 평균 기온 (°C) ──────────────────────────────────
    private static final double[] MONTHLY_BASE_TEMP =
            {-2, 0, 7, 13, 18, 23, 27, 28, 22, 15, 7, 1};

    // ── 월별 기본 습도 (%) ───────────────────────────────────
    private static final int[] MONTHLY_BASE_HUMIDITY =
            {55, 55, 58, 62, 65, 75, 82, 82, 70, 62, 62, 58};

    // ── 월별 일출·일몰 오프셋 (분, 하지 기준) ───────────────
    //    하지(6월): 일출 5:11, 일몰 19:57  →  오프셋=0
    //    동지(12월): 일출 7:44, 일몰 17:17 →  오프셋=+153/-160
    private static final int[] SUNRISE_OFFSET_MIN =
            {93, 75, 42, 6, -22, -40, -30, -2, 28, 58, 83, 100};


    // ─────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────

    /**
     * 모든 도시 현재 시각 날씨 생성 → DB 저장
     *
     * @return 생성된 레코드 수
     */
    public int generateAll() {
        LocalDateTime now = LocalDateTime.now();
        for (String city : CITIES) {
            generate(city, now);
        }
        log.info("[MockWeather] 현재 날씨 {}개 도시 생성 완료", CITIES.size());
        return CITIES.size();
    }

    /**
     * N일치 히스토리 데이터 시딩 (6시간 간격)
     *
     * <p>데이터 없을 때 한 번 실행. 총 N×4×4 레코드 생성.
     *
     * @param days 과거 일수 (예: 7 → 7일치)
     * @return 총 생성 레코드 수
     */
    public int seedHistory(int days) {
        int count = 0;
        for (int d = days; d >= 0; d--) {
            for (int h : new int[]{0, 6, 12, 18}) {
                LocalDateTime dt = LocalDateTime.now()
                        .minusDays(d)
                        .withHour(h).withMinute(0).withSecond(0).withNano(0);
                for (String city : CITIES) {
                    generate(city, dt);
                    count++;
                }
            }
        }
        log.info("[MockWeather] {}일치 히스토리 시딩 완료: {}건", days, count);
        return count;
    }

    /**
     * DB의 현재 날씨 데이터 총 건수 조회
     */
    public int countRecords() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM weather.weather_data", Integer.class);
        return n != null ? n : 0;
    }

    /**
     * 도시별 최신 날씨 요약 조회
     */
    public List<Map<String, Object>> getLatestSummary() {
        return jdbc.queryForList("""
                SELECT DISTINCT ON (city)
                    city, description, temperature_c, humidity_pct,
                    wind_speed_ms, collected_at
                FROM weather.weather_data
                ORDER BY city, collected_at DESC
                """);
    }


    // ─────────────────────────────────────────────────────────
    //  Core Generator
    // ─────────────────────────────────────────────────────────

    /**
     * 특정 도시·시각의 날씨 1건 생성
     */
    public void generate(String city, LocalDateTime at) {
        int month   = at.getMonthValue();        // 1~12
        int mIdx    = month - 1;                 // 0~11

        // ── 기온 계산 ─────────────────────────────────────
        double base   = MONTHLY_BASE_TEMP[mIdx];
        double offset = CITY_OFFSET.getOrDefault(city, 0.0);
        double rand   = (Math.random() - 0.5) * 8.0;   // ±4°C
        double temp       = round1(base + offset + rand);
        double feelsLike  = round1(temp - Math.random() * 3.0);
        double tempMin    = round1(temp - 2.0 - Math.random() * 2.0);
        double tempMax    = round1(temp + 2.0 + Math.random() * 2.0);

        // ── 습도 ─────────────────────────────────────────
        int baseHum = MONTHLY_BASE_HUMIDITY[mIdx];
        int humidity = clamp((int)(baseHum + (Math.random() - 0.5) * 22), 30, 99);

        // ── 기타 기상 요소 ────────────────────────────────
        double windSpeed  = round1(1.0 + Math.random() * 7.0);
        int    windDeg    = (int)(Math.random() * 360);
        int    pressure   = 1005 + (int)(Math.random() * 25);
        int    visibility = humidity > 72 ? 4000 + (int)(Math.random()*6000) : 10000;
        int    cloudiness = humidity > 65
                            ? 55 + (int)(Math.random() * 40)
                            : (int)(Math.random() * 35);

        // ── 날씨 설명 ─────────────────────────────────────
        String description = resolveDescription(temp, humidity, month);

        // ── 일출·일몰 (계절 보정) ─────────────────────────
        int sOff = SUNRISE_OFFSET_MIN[mIdx];
        LocalDateTime sunrise = at.toLocalDate()
                .atTime(5, 11).plusMinutes(sOff);
        LocalDateTime sunset  = at.toLocalDate()
                .atTime(19, 57).minusMinutes(sOff + 40);

        // ── DB 저장 ───────────────────────────────────────
        jdbc.update("""
                INSERT INTO weather.weather_data
                  (city, country, description,
                   temperature_c, feels_like_c, temp_min_c, temp_max_c,
                   pressure_hpa, humidity_pct,
                   wind_speed_ms, wind_deg,
                   visibility_m,  cloudiness_pct,
                   time_of_record, sunrise_time, sunset_time, collected_at)
                VALUES (?,?,?, ?,?,?,?, ?,?, ?,?, ?,?, ?,?,?,?)
                """,
                city, "KR", description,
                temp, feelsLike, tempMin, tempMax,
                pressure, humidity,
                windSpeed, windDeg,
                visibility, cloudiness,
                at, sunrise, sunset,
                LocalDateTime.now()
        );
    }


    // ─────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────

    /** 기온·습도·월로 날씨 설명 결정 */
    private String resolveDescription(double tempC, int humidity, int month) {
        // 겨울 눈
        if ((month == 12 || month <= 2) && tempC < 1.0) {
            if (humidity > 70) return "light snow";
            if (humidity > 60) return "snow";
        }
        // 우천
        if (humidity > 85) return pick("heavy rain", "thunderstorm");
        if (humidity > 75) return "moderate rain";
        if (humidity > 67) return "light rain";
        // 구름
        if (humidity > 60) return "overcast clouds";
        if (humidity > 53) return "broken clouds";
        if (humidity > 45) return "scattered clouds";
        // 맑음
        return pick("clear sky", "few clouds");
    }

    private String pick(String a, String b) {
        return Math.random() < 0.5 ? a : b;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
