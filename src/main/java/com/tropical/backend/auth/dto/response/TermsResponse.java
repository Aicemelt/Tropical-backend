package com.tropical.backend.auth.dto.response;

import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 약관 조회 응답 DTO
 *
 * <p>
 * 온보딩 과정이나 마이페이지에서 약관 내용을 조회할 때 사용됩니다.
 * 약관 제목, 내용, 버전 정보를 포함하여 프론트엔드에서 모달이나 페이지로 표시할 수 있습니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Schema(name = "TermsResponse", description = "약관 내용 조회 응답")
public record TermsResponse(

        /**
         * 동의 항목 타입
         * <p>어떤 종류의 약관인지 구분하기 위해 사용됩니다</p>
         */
        @Schema(description = "동의 항목 타입", example = "TERMS_OF_SERVICE")
        ConsentType consentType,

        /**
         * 약관 제목
         * <p>약관 모달이나 페이지의 제목으로 사용됩니다</p>
         */
        @Schema(description = "약관 제목", example = "서비스 이용약관")
        String title,

        /**
         * 약관 내용
         * <p>HTML 또는 Markdown 형식의 약관 전문입니다</p>
         */
        @Schema(description = "약관 내용 (HTML/Markdown)", example = "제1조 (목적)\n본 약관은...")
        String content,

        /**
         * 약관 버전
         * <p>법무팀에서 관리하는 약관의 현재 버전입니다</p>
         */
        @Schema(description = "약관 버전", example = "1.2")
        String version,

        /**
         * 약관 최종 수정일
         * <p>사용자에게 약관이 언제 마지막으로 업데이트되었는지 알려줍니다</p>
         */
        @Schema(description = "약관 최종 수정일", example = "2025-09-17T10:30:00")
        LocalDateTime lastUpdated
) {
}