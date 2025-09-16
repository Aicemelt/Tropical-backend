package com.tropical.backend.common.exception;

import com.tropical.backend.auth.dto.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 *
 * <p>
 * Spring MVC에서 발생하는 예외들을 일관된 JSON 형식으로 처리합니다.
 * Security 필터에서 처리되지 않는 Controller 단의 예외들을 담당합니다.
 * </p>
 *
 * @author 왕택준, 신동준
 * @version 0.1
 * @since 2025.09.14
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Spring Security Authorization 예외 처리 (403 Forbidden)
     *
     * @param ex      권한 부족 예외
     * @param request 웹 요청 정보
     * @return 권한 부족 에러 응답
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException ex, WebRequest request) {

        String path = getPath(request);
        String errorMessage = "접근 권한이 없습니다";

        // 경로별 맞춤 메시지
        if (path.contains("/admin")) {
            errorMessage = "관리자 권한이 필요합니다";
        } else if (path.contains("/api/users/") && !path.endsWith("/me")) {
            errorMessage = "다른 사용자의 정보에 접근할 수 없습니다";
        }

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message(errorMessage)
                .errorCode("FORBIDDEN")
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();

        log.warn("권한 부족 - Path: {}, Message: {}", path, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Bean Validation 실패 처리 (400 Bad Request)
     *
     * @param ex      검증 실패 예외
     * @param request 웹 요청 정보
     * @return 검증 실패 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        // 첫 번째 에러 메시지를 주 메시지로 사용
        String mainMessage = "입력값 검증에 실패했습니다";
        if (!validationErrors.isEmpty()) {
            mainMessage = validationErrors.values().iterator().next();
        }

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message(mainMessage)
                .errorCode("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .path(getPath(request))
                .details(validationErrors) // Map<String, String>으로 안전한 직렬화
                .build();

        log.warn("검증 실패 - Path: {}, Errors: {}", getPath(request), validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * IllegalArgumentException 처리 (400 Bad Request)
     *
     * @param ex      잘못된 인수 예외
     * @param request 웹 요청 정보
     * @return 잘못된 요청 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message(ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다")
                .errorCode("BAD_REQUEST")
                .timestamp(LocalDateTime.now())
                .path(getPath(request))
                .build();

        log.warn("잘못된 인수 - Path: {}, Message: {}", getPath(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * RuntimeException 처리 (500 Internal Server Error)
     *
     * @param ex      런타임 예외
     * @param request 웹 요청 정보
     * @return 서버 에러 응답
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        // AuthorizationDeniedException은 이미 위에서 처리되므로 제외
        if (ex instanceof AuthorizationDeniedException) {
            throw ex; // 다시 던져서 위의 핸들러가 처리하도록
        }

        String path = getPath(request);
        String errorMessage = "서버 내부 오류가 발생했습니다";
        String errorCode = "INTERNAL_SERVER_ERROR";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // 일정/일기 관련 비즈니스 로직 예외 처리
        if (ex.getMessage() != null) {
            String message = ex.getMessage();

            // 일기 중복 날짜 오류
            if (message.contains("해당 날짜에 이미 일기가 존재합니다")) {
                errorMessage = message;
                errorCode = "DIARY_DATE_DUPLICATE";
                status = HttpStatus.CONFLICT; // 409 Conflict
                log.warn("일기 중복 날짜 - Path: {}, Message: {}", path, message);
            }
            // 일정/일기 조회 실패
            else if (message.contains("일정을 찾을 수 없습니다") || message.contains("일기를 찾을 수 없습니다")) {
                errorMessage = message;
                errorCode = "RESOURCE_NOT_FOUND";
                status = HttpStatus.NOT_FOUND; // 404 Not Found
                log.warn("리소스 조회 실패 - Path: {}, Message: {}", path, message);
            }
            // 사용자 조회 실패
            else if (message.contains("사용자를 찾을 수 없습니다")) {
                errorMessage = message;
                errorCode = "USER_NOT_FOUND";
                status = HttpStatus.NOT_FOUND; // 404 Not Found
                log.warn("사용자 조회 실패 - Path: {}, Message: {}", path, message);
            }
            // 기타 RuntimeException
            else {
                log.error("서버 에러 - Path: {}, Message: {}", path, ex.getMessage(), ex);
            }
        } else {
            log.error("서버 에러 - Path: {}, Message: {}", path, ex.getMessage(), ex);
        }

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message(errorMessage)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * UnsupportedOperationException 처리 (501 Not Implemented)
     *
     * 일정/일기 서비스에서 사용되지 않는 레거시 메서드 호출 시 발생
     *
     * @param ex      지원하지 않는 연산 예외
     * @param request 웹 요청 정보
     * @return 지원하지 않는 기능 에러 응답
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedOperationException(
            UnsupportedOperationException ex, WebRequest request) {

        String path = getPath(request);
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "지원하지 않는 기능입니다";

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message(errorMessage)
                .errorCode("NOT_IMPLEMENTED")
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();

        log.warn("지원하지 않는 기능 호출 - Path: {}, Message: {}", path, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
    }

    /**
     * 일반적인 예외 처리 (500 Internal Server Error)
     *
     * @param ex      일반 예외
     * @param request 웹 요청 정보
     * @return 서버 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .message("예상치 못한 오류가 발생했습니다")
                .errorCode("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .path(getPath(request))
                .build();

        log.error("예상치 못한 에러 - Path: {}, Type: {}, Message: {}",
                getPath(request), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 웹 요청에서 요청 경로 추출
     *
     * @param request 웹 요청 정보
     * @return 요청 경로
     */
    private String getPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}