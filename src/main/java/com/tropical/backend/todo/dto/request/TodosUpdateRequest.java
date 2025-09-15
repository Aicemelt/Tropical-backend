package com.tropical.backend.todo.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.*;
import java.time.LocalDate;

/**
 * Todo 수정 요청 DTO
 *
 * <p>
 * content와 dueDate 중 하나 또는 둘 다 수정 가능합니다.
 * null인 필드는 수정하지 않습니다.
 * </p>
 *
 * @author 백승현
 * @version 1.0
 * @since 2025.09.15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodosUpdateRequest {
    /**
     * 할 일 내용 (null이면 수정하지 않음)
     */
    private String content;

    /**
     * 마감일 (null이면 수정하지 않음, 빈 값으로 설정하려면 별도 처리 필요)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TodayOrFuture(message = "마감일은 오늘 이후 날짜여야 합니다.")
    private LocalDate dueDate;


    /**
     * 오늘 또는 미래 날짜인지 검증하는 커스텀 어노테이션
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = TodayOrFutureValidator.class)
    @Documented
    @interface TodayOrFuture {
        String message() default "날짜는 오늘 이후여야 합니다.";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    /**
     * TodayOrFuture 어노테이션의 검증 로직
     */
    private static class TodayOrFutureValidator implements ConstraintValidator<TodayOrFuture, LocalDate> {
        @Override
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            if (value == null) {
                return true; // null은 다른 어노테이션에서 검증
            }
            return !value.isBefore(LocalDate.now());
        }
    }
}

