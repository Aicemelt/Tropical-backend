package com.tropical.backend.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 관리자 전용 컨트롤러 (403 테스트용)
 *
 * <p>
 * 권한 기반 접근 제어를 테스트하기 위한 관리자 전용 엔드포인트들입니다.
 * ROLE_ADMIN 권한이 없는 사용자가 접근 시 403 Forbidden 응답을 받게 됩니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.14
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    /**
     * 관리자 권한 확인용 핑 엔드포인트
     *
     * <p>
     * ROLE_ADMIN 권한이 있는 사용자만 접근 가능합니다.
     * 일반 사용자(ROLE_USER)가 접근 시 403 Forbidden 응답을 받습니다.
     * </p>
     *
     * @return 간단한 응답 메시지
     */
    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> ping() {
        log.info("관리자 핑 요청 처리");
        return Map.of(
                "success", true,
                "message", "pong - 관리자 권한 확인 완료",
                "timestamp", java.time.LocalDateTime.now()
        );
    }

    /**
     * 관리자 대시보드 정보
     *
     * @return 관리자 대시보드 데이터
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getDashboard() {
        log.info("관리자 대시보드 요청 처리");
        return Map.of(
                "success", true,
                "data", Map.of(
                        "totalUsers", 1234,
                        "activeUsers", 856,
                        "todaySignups", 23,
                        "systemStatus", "healthy"
                ),
                "message", "관리자 대시보드 데이터"
        );
    }

    /**
     * 사용자 관리 (예시)
     *
     * @return 사용자 목록 요약
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getUsers() {
        log.info("관리자 사용자 목록 요청 처리");
        return Map.of(
                "success", true,
                "message", "관리자만 접근 가능한 사용자 관리 기능"
        );
    }
}