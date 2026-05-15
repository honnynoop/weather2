package com.mds.weather.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenWeatherMap Current Weather API 응답 매핑 DTO
 *
 * <p>API 엔드포인트:
 * {@code GET https://api.openweathermap.org/data/2.5/weather
 *          ?q={city}&appid={key}&units=metric&lang=kr}
 *
 * <p>모든 중첩 레코드는 JSON 필드명이 다를 수 있으므로
 * {@code @JsonProperty}로 명시합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherResponse(

        /** 날씨 상태 목록 (보통 1건) */
        List<WeatherItem> weather,

        /** 기온·기압·습도 등 주요 수치 */
        Main main,

        /** 가시거리 (미터) */
        Integer visibility,

        /** 풍속·풍향 */
        Wind wind,

        /** 운량 */
        Clouds clouds,

        /** 도시명 (영문) */
        String name,

        /** 국가 코드 등 부가 정보 */
        Sys sys,

        /** 측정 시각 (Unix timestamp, UTC) */
        Long dt

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherItem(
            Integer id,
            String main,
            String description,
            String icon
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Main(
            Double temp,
            @JsonProperty("feels_like") Double feelsLike,
            @JsonProperty("temp_min")   Double tempMin,
            @JsonProperty("temp_max")   Double tempMax,
            Integer pressure,
            Integer humidity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Wind(
            Double speed,
            Integer deg
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Clouds(
            Integer all           // 운량 %
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sys(
            String country,
            Long sunrise,         // Unix timestamp
            Long sunset           // Unix timestamp
    ) {}
}
