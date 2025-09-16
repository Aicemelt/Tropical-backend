package com.tropical.backend.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tropical.backend.auth.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 에러 응답 작성 유틸리티
 *
 * <p>
 * Spring Security 핸들러나 전역 예외 처리에서 일관된 JSON 에러 응답을
 * 작성하기 위한 유틸리티 클래스입니다.
 * </p>
 *
 * @author 왕택준
 * @version 1.0
 * @since 2025.09.14
 */
@Slf4j
public final class ErrorResponseWriter {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private ErrorResponseWriter() {
        // 인스턴스 생성 방지
    }

    /**
     * HTTP 응답에 에러 JSON 작성
     *
     * @param request   HTTP 요청 객체
     * @param response  HTTP 응답 객체
     * @param status    HTTP 상태 코드
     * @param message   에러 메시지
     * @param errorCode 에러 코드
     * @throws IOException JSON 작성 실패 시
     */
    public static void write(HttpServletRequest request, HttpServletResponse response,
                             int status, String message, String errorCode) throws IOException {
        write(request, response, status, message, errorCode, null);
    }

    /**
     * HTTP 응답에 에러 JSON 작성 (세부 정보 포함)
     *
     * @param request   HTTP 요청 객체
     * @param response  HTTP 응답 객체
     * @param status    HTTP 상태 코드
     * @param message   에러 메시지
     * @param errorCode 에러 코드
     * @param details   추가 세부 정보
     * @throws IOException JSON 작성 실패 시
     */
    public static void write(HttpServletRequest request, HttpServletResponse response,
                             int status, String message, String errorCode, Object details) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .details(details)
                .build();

        try {
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
            log.debug("에러 응답 작성 완료 - Status: {}, Code: {}, Path: {}", status, errorCode, request.getRequestURI());
        } catch (IOException e) {
            log.error("에러 응답 작성 실패 - Path: {}, Error: {}", request.getRequestURI(), e.getMessage());
            throw e;
        }
    }
}