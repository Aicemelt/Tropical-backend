package com.tropical.backend.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 에러 응답 공통 DTO
 *
 * <p>
 * 모든 API 에러 응답에 사용되는 표준 형식입니다.
 * 프론트엔드에서 일관된 에러 처리를 할 수 있도록 동일한 구조를 제공합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.14
 */
@Getter
@Builder
public class ApiErrorResponse {

    /**
     * 요청 성공 여부 (에러 응답에서는 항상 false)
     */
    @Builder.Default
    private boolean success = false;

    /**
     * 에러 메시지 (사용자에게 표시할 수 있는 메시지)
     */
    private String message;

    /**
     * 에러 코드 (프론트엔드에서 에러 타입 구분용)
     * 예: UNAUTHORIZED, FORBIDDEN, BAD_REQUEST, VALIDATION_ERROR
     */
    private String errorCode;

    /**
     * 에러 발생 시간 (ISO-8601 형식)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * 에러가 발생한 API 경로
     */
    private String path;

    /**
     * 추가 세부 정보 (개발/디버깅용, 선택적)
     */
    private Object details;
}