package com.tropical.backend.auth.dto.response;

import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 약관 목록 조회 응답 DTO
 *
 * <p>
 * 온보딩 과정에서 동의 체크박스 목록을 구성하거나,
 * 마이페이지에서 약관 목록을 표시할 때 사용됩니다.
 * 약간의 메타데이터만 포함하여 가벼운 응답을 제공합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Schema(name = "TermsSummaryResponse", description = "약관 목록 조회 응답")
public record TermsSummaryResponse(

        /**
         * 약관 ID
         * <p>약관 상세 조회 시 사용되는 식별자</p>
         */
        @Schema(description = "약관 ID", example = "1")
        Long id,

        /**
         * 동의 항목 타입
         * <p>어떤 종류의 약관인지 구분하기 위해 사용됩니다</p>
         */
        @Schema(description = "동의 항목 타입", example = "TERMS_OF_SERVICE")
        ConsentType consentType,

        /**
         * 약관 제목 (접두사 포함)
         * <p>[필수] 또는 [선택] 접두사가 포함된 제목입니다</p>
         */
        @Schema(description = "약관 제목", example = "[필수] 서비스 이용약관")
        String title,

        /**
         * 약관 버전
         * <p>현재 활성화된 약관의 버전입니다</p>
         */
        @Schema(description = "약관 버전", example = "1.0")
        String version,

        /**
         * 활성화 상태
         * <p>현재 사용 중인 약관인지 여부입니다</p>
         */
        @Schema(description = "활성화 상태", example = "true")
        boolean active,

        /**
         * 필수 동의 여부
         * <p>프론트엔드에서 필수/선택 배지 표시나 유효성 검증에 사용됩니다</p>
         */
        @Schema(description = "필수 동의 여부", example = "true")
        boolean required

) {

    /**
     * Terms 엔티티로부터 TermsSummaryResponse 생성
     *
     * @param terms 약관 엔티티
     * @return TermsSummaryResponse 인스턴스
     */
    public static TermsSummaryResponse from(com.tropical.backend.auth.entity.Terms terms) {
        return new TermsSummaryResponse(
                terms.getId(),
                terms.getConsentType(),
                terms.getTitle(),
                terms.getVersion(),
                terms.isActive(),
                terms.getConsentType().isRequired()
        );
    }
}