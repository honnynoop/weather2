package com.mds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트 로딩 테스트
 *
 * <p>전체 컨텍스트 로딩을 검증합니다.
 * 실행 시 PostgreSQL(localhost:5432)과 Ollama(localhost:11434)가
 * 필요합니다. CI/CD 환경에서는 Testcontainers 또는 Mock 설정을 추가하세요.
 *
 * <p>@ActiveProfiles("local") — application.yml local 프로파일 사용
 */
@SpringBootTest
@ActiveProfiles("local")
class WeatherCommerceAiApplicationTests {

    @Test
    void contextLoads() {
        // 애플리케이션 컨텍스트가 오류 없이 로드되는지 확인
    }
}
