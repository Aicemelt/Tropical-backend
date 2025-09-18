package com.tropical.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 시간 관련 설정 클래스.
 *
 * <p>
 * 애플리케이션 전역에서 {@link Clock} 빈을 주입받아
 * 현재 시각이나 연도 계산에 사용합니다.
 * </p>
 *
 * <p>
 * 기본적으로 서버의 시스템 타임존을 사용하며,
 * 테스트 환경에서는 {@link java.time.Clock#fixed} 등을 이용해
 * 고정된 시간을 주입할 수 있습니다.
 * </p>
 *
 * <p>주요 목적:</p>
 * <ul>
 *   <li>현재 시각을 서비스/도메인 로직에서 주입식으로 관리</li>
 *   <li>테스트 코드에서 시각 제어 용이</li>
 *   <li>동적 상한 연도 계산 (현재년도+2) 등에 활용</li>
 * </ul>
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.16
 */
@Configuration
public class TimeConfig {

    /**
     * {@link Clock} 빈 등록.
     *
     * @return 시스템 기본 타임존을 사용하는 Clock 인스턴스
     */
    @Bean
    public Clock clock() {
        // 서버 기본 타임존 사용 (테스트에서는 FixedClock으로 대체 가능)
        return Clock.systemDefaultZone();
    }
}
