package com.tropical.backend.auth.dto.request;

import com.tropical.backend.auth.entity.UserConsent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 온보딩 동의 완료 요청 DTO
 *
 * <p>
 * 회원가입 후 온보딩 페이지에서 필수 동의와 선택 동의를 입력받아
 * 서버로 전송하는 요청 데이터를 담는 DTO입니다.
 * </p>
 *
 * <p>필수 동의:</p>
 * <ul>
 *   <li>TERMS_OF_SERVICE: 서비스 이용약관</li>
 *   <li>CALENDAR_PERSONALIZATION: 일정 기반 추천 동의</li>
 * </ul>
 *
 * <p>선택 동의:</p>
 * <ul>
 *   <li>DIARY_PERSONALIZATION: 일기 기반 추천 동의</li>
 *   <li>TODO_PERSONALIZATION: 할일 기반 추천 동의</li>
 *   <li>BUCKET_PERSONALIZATION: 버킷리스트 기반 추천 동의</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRequest {

    /**
     * 필수 동의 항목들
     *
     * <p>
     * 서비스 이용을 위한 필수 동의 항목들입니다.
     * 모든 항목이 true여야 온보딩을 완료할 수 있습니다.
     * </p>
     */
    @NotNull(message = "필수 동의 정보는 필수 입력 항목입니다")
    private Map<UserConsent.ConsentType, Boolean> requiredConsents;

    /**
     * 선택 동의 항목들
     *
     * <p>
     * AI 개인화 추천을 위한 선택 동의 항목들입니다.
     * 사용자가 원하는 항목만 동의할 수 있으며, 나중에 마이페이지에서 변경 가능합니다.
     * </p>
     */
    private Map<UserConsent.ConsentType, Boolean> optionalConsents;

    /**
     * 모든 동의 정보를 하나의 맵으로 통합하여 반환
     *
     * <p>
     * Service 계층에서 처리하기 편하도록 필수 동의와 선택 동의를
     * 하나의 맵으로 합쳐서 반환합니다.
     * </p>
     *
     * @return 전체 동의 정보가 담긴 맵
     */
    public Map<UserConsent.ConsentType, Boolean> getAllConsents() {
        Map<UserConsent.ConsentType, Boolean> allConsents = new java.util.HashMap<>();

        if (requiredConsents != null) {
            allConsents.putAll(requiredConsents);
        }

        if (optionalConsents != null) {
            allConsents.putAll(optionalConsents);
        }

        return allConsents;
    }
}