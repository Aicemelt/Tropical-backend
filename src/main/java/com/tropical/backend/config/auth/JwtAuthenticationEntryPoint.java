package com.tropical.backend.config.auth;

import com.tropical.backend.common.util.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 실패 처리 핸들러 (401 Unauthorized)
 *
 * <p>
 * 인증이 필요한 리소스에 인증되지 않은 사용자가 접근할 때 호출됩니다.
 * 일관된 JSON 형식의 401 에러 응답을 제공합니다.
 * </p>
 *
 * <p>발생 시나리오:</p>
 * <ul>
 *   <li>JWT 토큰이 없는 경우</li>
 *   <li>JWT 토큰이 유효하지 않은 경우</li>
 *   <li>JWT 토큰이 만료된 경우</li>
 *   <li>JWT 토큰 형식이 잘못된 경우</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.3
 * @since 2025.09.14
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 인증 실패 시 호출되는 메서드
     *
     * @param request       HTTP 요청 객체
     * @param response      HTTP 응답 객체
     * @param authException 인증 예외 정보
     * @throws IOException 응답 작성 실패 시
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String errorMessage = "인증이 필요합니다";
        String requestUri = request.getRequestURI();

        // 인증 예외 타입별 세부 메시지 설정 (선택적)
        if (authException != null) {
            String exceptionMessage = authException.getMessage();
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("expired")) {
                    errorMessage = "토큰이 만료되었습니다";
                } else if (exceptionMessage.contains("malformed")) {
                    errorMessage = "잘못된 토큰 형식입니다";
                } else if (exceptionMessage.contains("signature")) {
                    errorMessage = "유효하지 않은 토큰입니다";
                }
            }
        }

        log.warn("인증 실패 - URI: {}, 에러: {}", requestUri,
                authException != null ? authException.getMessage() : "unknown");

        ErrorResponseWriter.write(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                errorMessage,
                "UNAUTHORIZED"
        );
    }
}