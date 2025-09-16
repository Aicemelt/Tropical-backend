package com.tropical.backend.common.util;

import jakarta.servlet.http.Cookie;

/**
 * JWT 토큰 쿠키 관리를 위한 유틸리티 클래스
 *
 * <p>
 * JWT 토큰을 HttpOnly 쿠키로 안전하게 설정하고 삭제하는 기능을 제공합니다.
 * 보안을 위해 HttpOnly 플래그를 설정하여 JavaScript에서 접근을 차단합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.15
 */
public class CookieUtil {

    /**
     * JWT 토큰 쿠키 생성
     *
     * @param name          쿠키 이름 (예: ACCESS_TOKEN, REFRESH_TOKEN)
     * @param value         JWT 토큰 값
     * @param maxAgeSeconds 쿠키 만료 시간 (초)
     * @return 생성된 Cookie 객체
     */
    public static Cookie build(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);    // JavaScript 접근 차단
        cookie.setPath("/");         // 전역 경로
        cookie.setMaxAge(maxAgeSeconds);

        // HTTPS 환경에서는 Secure 플래그 활성화
        // 개발 환경에서는 비활성화, 운영 환경에서는 활성화 권장
        // cookie.setSecure(true);

        // 필요 시 도메인 지정 (크로스 도메인 환경)
        // cookie.setDomain("localhost");

        return cookie;
    }

    /**
     * JWT 토큰 쿠키 삭제 (만료 처리)
     *
     * @param name 삭제할 쿠키 이름
     * @return 만료 처리된 Cookie 객체
     */
    public static Cookie expire(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        return cookie;
    }
}