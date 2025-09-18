package com.tropical.backend.common.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 연도 범위 검증 어노테이션.
 *
 * <p>
 * 대상 필드나 파라미터가 지정된 범위의 연도 값인지 검증합니다.
 * - 최소 연도(min) 이상
 * - 현재 연도 + aheadYears 이하
 * </p>
 *
 * <p>
 * {@link YearWithinValidator}를 통해 실제 검증 로직이 수행됩니다.
 * </p>
 *
 * <p>
 * null 값에 대한 처리는 별도의 {@code @NotNull} 어노테이션을 조합하여 제어할 수 있습니다.
 * </p>
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.17
 */
@Documented
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { YearWithinValidator.class })
public @interface YearWithin {

    /**
     * 기본 오류 메시지.
     * {min}, {aheadYears}는 어노테이션 속성 값으로 치환됩니다.
     */
    String message() default "연도는 {min} 이상, 현재 연도 + {aheadYears} 이하이어야 합니다.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** 허용 최소 연도 (기본값: 1900). */
    int min() default 1900;

    /** 현재 연도 기준 허용 연도 오차 (aheadYears) (기본값: 2년). */
    int aheadYears() default 2;
}
