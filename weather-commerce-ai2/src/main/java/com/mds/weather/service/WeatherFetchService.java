package com.mds.weather.service;

import com.mds.weather.config.WeatherApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * OpenWeatherMap Current Weather API 수집 서비스
 *
 * <p>[수정] @Value("${weather.api.cities}") List&lt;String&gt; 제거
 * → YAML 리스트는 @Value 로 바인딩 불가 (PlaceholderResolutionException 발생)
 * → WeatherApiProperties (@ConfigurationProperties) 주입으로 변경
 */
@Slf4j
@Service
public class WeatherFetchService {

    private static final String INSERT_SQL = """
            INSERT INTO weather.weather_data
                (city, country, description,
                 temperature_c, feels_like_c, temp_min_c, temp_max_c,
                 pressure_hpa, humidity_pct,
                 wind_speed_ms, wind_deg,
                 visibility_m, cloudiness_pct,
                 time_of_record, sunrise_time, sunset_time, collected_at)
            VALUES (?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?,
                    ?, ?, ?, NOW())
            """;

    private final RestClient           restClient;
    private final JdbcTemplate         jdbc;
    private final WeatherApiProperties props;   // ← @Value 대신 Properties 클래스 사용

    public WeatherFetchService(
            RestClient.Builder restClientBuilder,
            JdbcTemplate jdbc,
            WeatherApiProperties props           // Spring이 자동 주입
    ) {
        this.jdbc  = jdbc;
        this.props = props;
        this.restClient = restClientBuilder
                .baseUrl(props.getBaseUrl())
                .build();
    }

    /**
     * 설정된 모든 도시의 현재 날씨를 조회하고 DB에 저장.
     *
     * @return 성공적으로 저장된 도시 수
     */
    public int fetchAllCities() {
        List<String> cities = props.getCities();
        int savedCount = 0;
        for (String city : cities) {
            try {
                fetchAndSave(city);
                savedCount++;
                log.info("[WeatherFetch] 저장 완료: {}", city);
            } catch (Exception e) {
                log.error("[WeatherFetch] 실패: city={} | {}", city, e.getMessage());
            }
        }
        return savedCount;
    }

    /** 단일 도시 날씨 조회 + DB 저장 */
    public void fetchAndSave(String city) {
        OpenWeatherResponse resp = restClient.get()
                .uri("/weather?q={city}&appid={key}&units=metric&lang=kr",
                        city, props.getKey())
                .retrieve()
                .body(OpenWeatherResponse.class);

        if (resp == null) {
            throw new IllegalStateException("API 응답이 null입니다: city=" + city);
        }

        saveToDb(resp);
        log.debug("[WeatherFetch] {} → {}°C, {}",
                resp.name(),
                resp.main() != null ? resp.main().temp() : "N/A",
                resp.weather() != null && !resp.weather().isEmpty()
                        ? resp.weather().get(0).description() : "N/A");
    }

    // ── private 헬퍼 ─────────────────────────────────────────

    private void saveToDb(OpenWeatherResponse r) {
        OpenWeatherResponse.Main   main   = r.main();
        OpenWeatherResponse.Wind   wind   = r.wind();
        OpenWeatherResponse.Clouds clouds = r.clouds();
        OpenWeatherResponse.Sys    sys    = r.sys();

        String description = (r.weather() != null && !r.weather().isEmpty())
                ? r.weather().get(0).description() : "알 수 없음";

        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDateTime timeOfRecord = toKst(r.dt(), kst);
        LocalDateTime sunriseTime  = toKst(sys != null ? sys.sunrise() : null, kst);
        LocalDateTime sunsetTime   = toKst(sys != null ? sys.sunset()  : null, kst);

        jdbc.update(INSERT_SQL,
                r.name(),
                sys  != null ? sys.country()  : null,
                description,
                toBigDecimal(main != null ? main.temp()      : null),
                toBigDecimal(main != null ? main.feelsLike() : null),
                toBigDecimal(main != null ? main.tempMin()   : null),
                toBigDecimal(main != null ? main.tempMax()   : null),
                main != null ? main.pressure() : null,
                main != null ? main.humidity() : null,
                toBigDecimal(wind != null ? wind.speed() : null),
                wind   != null ? wind.deg()   : null,
                r.visibility(),
                clouds != null ? clouds.all() : null,
                timeOfRecord, sunriseTime, sunsetTime
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private LocalDateTime toKst(Long unixTimestamp, ZoneId zone) {
        if (unixTimestamp == null) return null;
        return Instant.ofEpochSecond(unixTimestamp).atZone(zone).toLocalDateTime();
    }
}
