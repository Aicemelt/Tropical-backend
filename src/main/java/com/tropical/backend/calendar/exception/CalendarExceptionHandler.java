package com.tropical.backend.calendar.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 캘린더/공휴일 API 전용 예외 처리 핸들러.
 *
 * <p>
 * 캘린더 모듈에서 발생하는 예외를 전문적으로 처리합니다.
 * 전역 핸들러보다 높은 우선순위를 가져 캘린더 관련 요청을 우선 처리합니다.
 * </p>
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.17
 */
@Slf4j
@Order(1) // 전역 핸들러보다 높은 우선순위
@RestControllerAdvice(basePackages = "com.tropical.backend.calendar")
public class CalendarExceptionHandler {

    /**
     * 타임스탬프 포맷터 (ISO-8601 형식, 초 단위까지).
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 클라이언트 요청 오류 (400 Bad Request) 처리.
     *
     * <p>
     * 캘린더 API에서 발생하는 잘못된 파라미터, Bean Validation 실패,
     * 필수 파라미터 누락 등을 처리합니다.
     * </p>
     *
     * @param exception 발생한 예외 객체
     * @return 400 상태 코드와 에러 정보를 포함한 ResponseEntity
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            BindException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
        String errorMessage = extractUserFriendlyMessage(exception);

        log.warn("캘린더 API 클라이언트 요청 오류 (400 Bad Request): {} - {}",
                exception.getClass().getSimpleName(), errorMessage);

        Map<String, Object> errorResponse = Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", errorMessage,
                "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 서버 내부 오류 (500 Internal Server Error) 처리.
     *
     * <p>
     * 캘린더 API에서 발생한 예상하지 못한 모든 예외를 처리하는 최종 안전망입니다.
     * </p>
     *
     * @param exception 발생한 예외 객체
     * @return 500 상태 코드와 일반적인 에러 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleInternalServerError(Exception exception) {
        log.error("캘린더 API에서 예상치 못한 서버 내부 오류 발생 - {}: {}",
                exception.getClass().getSimpleName(), exception.getMessage(), exception);

        Map<String, Object> errorResponse = Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                "timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 예외로부터 사용자 친화적인 에러 메시지를 추출합니다.
     *
     * @param exception 메시지를 추출할 예외 객체
     * @return 클라이언트에게 제공할 사용자 친화적인 에러 메시지
     */
    private String extractUserFriendlyMessage(Exception exception) {
        // 파라미터 타입 불일치 (예: 문자열을 숫자로 변환 실패)
        if (exception instanceof MethodArgumentTypeMismatchException ex) {
            String paramName = ex.getName();
            String expectedType = ex.getRequiredType() != null ?
                    ex.getRequiredType().getSimpleName() : "올바른 형식";
            return String.format("파라미터 '%s'의 값이 올바르지 않습니다. %s 형식이어야 합니다.",
                    paramName, expectedType);
        }

        // 필수 파라미터 누락
        if (exception instanceof MissingServletRequestParameterException ex) {
            return String.format("필수 파라미터 '%s'가 누락되었습니다.", ex.getParameterName());
        }

        // HTTP 메시지 파싱 실패 (JSON 오류, 날짜 형식 오류 등)
        if (exception instanceof HttpMessageNotReadableException) {
            String originalMessage = exception.getMessage();
            if (originalMessage != null && originalMessage.contains("LocalDate")) {
                return "날짜 형식이 올바르지 않습니다. yyyy-MM-dd 형식을 사용해주세요. (예: 2025-01-01)";
            }
            return "요청 데이터 형식이 올바르지 않습니다.";
        }

        // Bean Validation 오류들은 이미 사용자 친화적인 메시지를 제공
        if (exception instanceof MethodArgumentNotValidException ||
            exception instanceof BindException) {
            return exception.getMessage();
        }

        // IllegalArgumentException은 비즈니스 로직에서 의도적으로 던진 것이므로 메시지 그대로 사용
        return exception.getMessage() != null ? exception.getMessage() : "잘못된 요청입니다.";
    }
}