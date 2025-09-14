package com.tropical.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Spring Security 권한 부족(403 Forbidden) 처리 핸들러
 *
 * <p>
 * 인증은 완료되었지만 특정 리소스에 접근할 권한이 없을 때
 * 403 Forbidden 응답을 반환하는 커스텀 핸들러입니다.
 * JwtAuthenticationEntryPoint와 일관된 JSON 응답 포맷을 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>권한 부족 시 403 HTTP 상태 코드 반환</li>
 *   <li>일관된 JSON 에러 응답 형식 제공</li>
 *   <li>요청 경로 및 타임스탬프 포함</li>
 *   <li>프론트엔드 에러 처리 표준화 지원</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    // 스프링 부트가 자동 구성한 ObjectMapper 빈 주입 (JavaTimeModule 포함)
    private final ObjectMapper objectMapper;

    /**
     * 권한 부족 시 403 Forbidden 응답 처리
     *
     * <p>
     * 사용자가 인증은 되었지만 특정 리소스에 접근할 권한이 없을 때 호출됩니다.
     * JwtAuthenticationEntryPoint와 동일한 JSON 응답 형식을 사용하여
     * 프론트엔드에서 일관된 에러 처리가 가능합니다.
     * </p>
     *
     * @param request  HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param ex       권한 부족 예외
     * @throws IOException 응답 작성 중 I/O 오류 발생 시
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        log.warn("권한 부족 접근 - URI: {}, 에러: {}", request.getRequestURI(), ex.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "권한이 없습니다",
                "errorCode", "FORBIDDEN",
                "timestamp", LocalDateTime.now(),
                "path", request.getRequestURI()
        );

        // ObjectMapper를 사용하여 안전한 JSON 직렬화
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}