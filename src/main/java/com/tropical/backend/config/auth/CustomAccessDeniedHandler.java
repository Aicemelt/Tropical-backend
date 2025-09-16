package com.tropical.backend.config.auth;

import com.tropical.backend.common.util.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 접근 권한 부족 처리 핸들러 (403 Forbidden)
 *
 * <p>
 * 인증된 사용자가 권한이 없는 리소스에 접근할 때 호출됩니다.
 * 일관된 JSON 형식의 403 에러 응답을 제공합니다.
 * </p>
 *
 * <p>발생 시나리오:</p>
 * <ul>
 *   <li>ROLE_USER가 ROLE_ADMIN 권한이 필요한 리소스 접근</li>
 *   <li>@PreAuthorize 조건을 만족하지 않는 경우</li>
 *   <li>특정 메서드 수준 보안 규칙 위반</li>
 *   <li>사용자별 데이터 접근 권한 부족</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.14
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 접근 권한 부족 시 호출되는 메서드
     *
     * @param request               HTTP 요청 객체
     * @param response              HTTP 응답 객체
     * @param accessDeniedException 접근 거부 예외 정보
     * @throws IOException 응답 작성 실패 시
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String requestUri = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");

        log.warn("인가 실패 - URI: {}, 에러: {}, User-Agent: {}",
                requestUri,
                accessDeniedException.getMessage(),
                userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "unknown");

        // 요청 경로에 따른 세부 메시지 커스터마이징 (선택적)
        String errorMessage = "접근 권한이 없습니다";

        if (requestUri.contains("/admin")) {
            errorMessage = "관리자 권한이 필요합니다";
        } else if (requestUri.contains("/api/users/") && !requestUri.endsWith("/me")) {
            errorMessage = "다른 사용자의 정보에 접근할 수 없습니다";
        }

        ErrorResponseWriter.write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                errorMessage,
                "FORBIDDEN"
        );
    }
}