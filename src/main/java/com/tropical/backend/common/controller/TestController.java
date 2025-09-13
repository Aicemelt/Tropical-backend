package com.tropical.backend.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 기본 API 동작 확인용 테스트 컨트롤러
 *
 * <p>
 * 애플리케이션의 기본 동작과 API 엔드포인트가 정상적으로 작동하는지
 * 확인하기 위한 간단한 테스트 컨트롤러입니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>루트 경로 '/' 에러 해결</li>
 *   <li>기본 API 응답 확인</li>
 *   <li>애플리케이션 상태 체크</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@RestController
public class TestController {

    /**
     * 루트 경로 기본 응답
     *
     * <p>
     * http://localhost:9005/ 접속 시 404 에러 대신
     * 정상적인 응답을 반환하여 애플리케이션 동작을 확인합니다.
     * </p>
     *
     * @return 애플리케이션 상태 메시지
     */
    @GetMapping("/")
    public String home() {
        return "Tropical Backend is running successfully!";
    }

    /**
     * API 동작 테스트 엔드포인트
     *
     * <p>
     * REST API가 정상적으로 동작하는지 확인하기 위한 테스트 엔드포인트입니다.
     * JSON 응답을 반환하여 Spring Boot의 Jackson 설정도 함께 확인합니다.
     * </p>
     *
     * @return API 동작 상태 정보 (JSON)
     */
    @GetMapping("/api/test")
    public Map<String, Object> apiTest() {
        return Map.of(
                "message", "API is working properly",
                "timestamp", LocalDateTime.now(),
                "status", "OK",
                "version", "0.1.0",
                "environment", "development"
        );
    }

    /**
     * 헬스체크 엔드포인트
     *
     * <p>
     * 애플리케이션의 기본 상태를 확인하는 간단한 헬스체크 엔드포인트입니다.
     * 로드밸런서나 모니터링 도구에서 사용할 수 있습니다.
     * </p>
     *
     * @return 헬스체크 결과
     */
    @GetMapping("/api/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "tropical-backend"
        );
    }
}