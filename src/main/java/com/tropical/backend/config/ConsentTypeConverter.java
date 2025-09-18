package com.tropical.backend.config;

import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * ConsentType enum을 위한 PathVariable 커스텀 컨버터
 *
 * <p>
 * Spring MVC에서 PathVariable로 전달되는 문자열을 ConsentType enum으로 변환합니다.
 * 기존 UPPER_CASE 형식과 새로운 camelCase 형식을 모두 지원하여
 * URL과 JSON에서 일관된 enum 값 사용을 가능하게 합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>UPPER_CASE 형식 지원 - 기존 호환성 유지 (TERMS_OF_SERVICE)</li>
 *   <li>camelCase 형식 지원 - 새로운 일관성 확보 (termsOfService)</li>
 *   <li>잘못된 형식 입력 시 명확한 예외 메시지 제공</li>
 *   <li>대소문자 구분하여 정확한 매핑 보장</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Component
public class ConsentTypeConverter implements Converter<String, ConsentType> {

    /**
     * 문자열을 ConsentType enum으로 변환
     *
     * <p>
     * PathVariable로 전달된 문자열을 ConsentType enum 값으로 변환합니다.
     * 기존 UPPER_CASE 형식과 새로운 camelCase 형식을 모두 지원하며,
     * 변환 실패 시 명확한 오류 메시지를 제공합니다.
     * </p>
     *
     * <p>지원 형식:</p>
     * <ul>
     *   <li>UPPER_CASE: TERMS_OF_SERVICE, PRIVACY_POLICY 등</li>
     *   <li>camelCase: termsOfService, privacyPolicy 등</li>
     * </ul>
     *
     * @param source 변환할 문자열 (PathVariable 값)
     * @return 변환된 ConsentType enum 값
     * @throws IllegalArgumentException 입력값이 null, 빈 문자열이거나 유효하지 않은 형식인 경우
     */
    @Override
    public ConsentType convert(String source) {
        // 입력값 유효성 검증
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("ConsentType cannot be null or empty");
        }

        // 1차 시도: 대문자 상수명으로 직접 매칭 (기존 호환성 유지)
        try {
            return ConsentType.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 2차 시도: camelCase 형식으로 JsonProperty 값과 매칭
            for (ConsentType type : ConsentType.values()) {
                if (getJsonPropertyValue(type).equals(source)) {
                    return type;
                }
            }

            // 모든 시도 실패 시 예외 발생
            throw new IllegalArgumentException("Invalid ConsentType: " + source +
                                               ". Supported formats: UPPER_CASE (e.g., TERMS_OF_SERVICE) or camelCase (e.g., termsOfService)");
        }
    }

    /**
     * ConsentType enum의 JsonProperty 값을 추출
     *
     * <p>
     * 각 ConsentType enum 상수에 대응하는 JsonProperty 어노테이션 값을 반환합니다.
     * JSON 직렬화/역직렬화 시 사용되는 camelCase 형식 문자열을 제공하여
     * PathVariable에서도 동일한 형식을 사용할 수 있도록 지원합니다.
     * </p>
     *
     * <p>매핑 규칙:</p>
     * <ul>
     *   <li>TERMS_OF_SERVICE → "termsOfService"</li>
     *   <li>PRIVACY_POLICY → "privacyPolicy"</li>
     *   <li>CALENDAR_PERSONALIZATION → "calendarPersonalization"</li>
     *   <li>DIARY_PERSONALIZATION → "diaryPersonalization"</li>
     *   <li>TODO_PERSONALIZATION → "todoPersonalization"</li>
     *   <li>BUCKET_PERSONALIZATION → "bucketPersonalization"</li>
     * </ul>
     *
     * @param type JsonProperty 값을 추출할 ConsentType enum
     * @return 해당 enum의 JsonProperty camelCase 문자열
     */
    private String getJsonPropertyValue(ConsentType type) {
        return switch (type) {
            case TERMS_OF_SERVICE -> "termsOfService";
            case PRIVACY_POLICY -> "privacyPolicy";
            case CALENDAR_PERSONALIZATION -> "calendarPersonalization";
            case DIARY_PERSONALIZATION -> "diaryPersonalization";
            case TODO_PERSONALIZATION -> "todoPersonalization";
            case BUCKET_PERSONALIZATION -> "bucketPersonalization";
        };
    }
}