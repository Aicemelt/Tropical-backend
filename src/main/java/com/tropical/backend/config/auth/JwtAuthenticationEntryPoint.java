package com.tropical.backend.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * JWT 인증 실패 시 처리하는 EntryPoint
 *
 * <p>
 * 인증이 필요한 엔드포인트에 유효하지 않은 토큰으로 접근하거나
 * 토큰 없이 접근할 때 401 Unauthorized 응답을 반환합니다.
 * </p>
 * <p>
 * 수정사항:
 * - ❌ 기존: private final ObjectMapper objectMapper = new ObjectMapper();
 * - ✅ 변경: 스프링 빈으로 주입받아 JavaTimeModule이 포함된 ObjectMapper 사용
 * - LocalDateTime 직렬화 문제 완전 해결
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.13
 */
@Slf4j
@Component
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // 스프링 부트가 자동 구성한 ObjectMapper 빈 주입 (JavaTimeModule 포함)
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("인증 실패 - URI: {}, 에러: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "인증이 필요합니다",
                "errorCode", "UNAUTHORIZED",
                "timestamp", LocalDateTime.now(), // 이제 정상적으로 직렬화됨
                "path", request.getRequestURI()
        );

        // ObjectMapper.writeValue()를 사용하면 더 안전함
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}