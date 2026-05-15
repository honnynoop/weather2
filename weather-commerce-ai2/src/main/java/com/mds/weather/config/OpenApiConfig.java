package com.mds.weather.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3.x 전역 설정
 *
 * <p>Swagger UI 접속: {@code http://localhost:8080/swagger-ui.html}
 * <p>OpenAPI JSON : {@code http://localhost:8080/v3/api-docs}
 * <p>OpenAPI YAML : {@code http://localhost:8080/v3/api-docs.yaml}
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:weather-ai}")
    private String appName;

    @Bean
    public OpenAPI weatherOpenAPI() {
        return new OpenAPI()
                // ── 기본 정보 ─────────────────────────────────────
                .info(new Info()
                        .title("🌤 Weather AI API")
                        .description("""
                                날씨 기반 AI 상품 추천 · RAG 채팅 · 인사이트 리포트 서비스
                                
                                ## 사용 순서
                                1. `POST /api/mock/seed` — 테스트 날씨 데이터 생성
                                2. `POST /api/batch/run/weather-analysis` — 날씨-매출 분석 배치 실행
                                3. `POST /api/batch/run/embedding-index` — RAG 벡터 인덱스 생성
                                4. `GET  /api/recommend` — AI 상품 추천
                                5. `POST /api/chat` — 날씨-매출 RAG 채팅
                                6. `GET  /api/report` — AI 인사이트 리포트
                                
                                ## 인증
                                현재 버전은 API Key 인증을 사용합니다. `X-API-Key` 헤더에 키를 포함하세요.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("MDS Weather AI Team")
                                .email("dev@mds-weather.ai")
                                .url("https://github.com/mds/weather-ai"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))

                // ── 서버 환경 ─────────────────────────────────────
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8090")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.mds-weather.ai")
                                .description("운영 서버")))

                // ── 태그 (UI 그룹 순서 지정) ──────────────────────
                .tags(List.of(
                        new Tag().name("Mock Data")
                                .description("테스트 날씨 데이터 생성 및 관리"),
                        new Tag().name("AI 추천")
                                .description("날씨 기반 AI 상품 추천"),
                        new Tag().name("RAG 채팅")
                                .description("날씨·매출 데이터 기반 자연어 Q&A"),
                        new Tag().name("리포트")
                                .description("AI 인사이트 리포트 생성"),
                        new Tag().name("배치")
                                .description("분석·임베딩 배치 Job 수동 실행")))

                // ── 보안 스킴 (API Key) ───────────────────────────
                .components(new Components()
                        .addSecuritySchemes("apiKey",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("발급받은 API Key를 입력하세요")))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}