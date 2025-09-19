package com.tropical.backend.common.util;

import com.tropical.backend.config.TimeConfig;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

/**
 * 연도 유효성 검증기.
 *
 * <p>
 * {@link YearWithin} 어노테이션과 함께 사용되며,
 * 최소 연도(min) 이상, 현재 연도 + aheadYears 이하인지 검증합니다.
 * </p>
 *
 * <p>
 * 검증 로직에서 {@link Clock}을 사용하므로,
 * 테스트 시 {@code FixedClock}을 주입하여
 * 시간 의존성을 제어할 수 있습니다.
 * </p>
 *
 * <p>주요 특징:</p>
 * <ul>
 *   <li>null 값은 유효한 값으로 처리 (원한다면 @NotNull 별도 조합 필요)</li>
 *   <li>현재 연도를 기준으로 동적으로 최대 연도 계산</li>
 *   <li>테스트 환경에서 시각 고정 가능</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Component
@RequiredArgsConstructor
public class YearWithinValidator implements ConstraintValidator<YearWithin, Integer> {

    private final Clock clock; // TimeConfig에서 주입 (현재 연도 계산용)

    private int min;         // 최소 연도 (어노테이션 속성)

    /**
     * 어노테이션 속성 초기화.
     *
     * @param ann YearWithin 어노테이션 인스턴스
     */
    @Override
    public void initialize(YearWithin ann) {
        this.min = ann.min();
    }

    /**
     * 값이 유효한 연도 범위인지 검증합니다.
     *
     * <p>
     * - 값이 null인 경우 true (검증 통과)<br>
     * → null 허용 여부는 @NotNull 조합으로 별도 처리<br>
     * - 값이 min 이상, (현재연도+aheadYears) 이하인지 확인<br>
     * </p>
     *
     * @param value   검증 대상 값
     * @param context 검증 컨텍스트
     * @return 유효하면 true, 아니면 false
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) return true;
        // 어노테이션 속성 대신 TimeConfig 상수 직접 사용
        int max = Year.now(clock).getValue() + TimeConfig.HOLIDAY_AHEAD_YEARS;
        return value >= min && value <= max;
    }
}
