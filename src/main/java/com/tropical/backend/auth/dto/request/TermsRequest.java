package com.tropical.backend.auth.dto.request;

import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 약관 생성 요청 DTO
 *
 * <p>
 * 관리자가 새로운 약관 버전을 등록할 때 사용되는 요청 객체입니다.
 * 컨트롤러에서 입력 검증(@Valid)과 OpenAPI 문서화를 위해 정의됩니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Schema(name = "TermsRequest", description = "새로운 약관 생성 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsRequest {

    /**
     * 동의 항목 타입
     * <p>어떤 종류의 약관(서비스 이용약관, 개인정보 처리방침 등)인지 구분합니다.</p>
     */
    @Schema(description = "동의 항목 타입", example = "TERMS_OF_SERVICE")
    @NotNull
    private ConsentType consentType;

    /**
     * 약관 제목
     * <p>프론트엔드에서 모달이나 페이지의 헤더로 표시됩니다.</p>
     */
    @Schema(description = "약관 제목", example = "서비스 이용약관")
    @NotBlank
    private String title;

    /**
     * 약관 본문
     *
     * <p>
     * HTML 또는 Markdown 형식의 약관 전문을 포함합니다.
     * 사용자에게 표시될 실제 약관 내용입니다.
     * </p>
     */
    @Schema(description = "약관 본문 (HTML/Markdown)", example = "제1조 (목적)\n본 약관은...")
    @NotBlank
    private String content;

    /**
     * 약관 버전
     * <p>법무팀에서 관리하는 버전 문자열입니다.</p>
     */
    @Schema(description = "약관 버전", example = "1.2")
    @NotBlank
    private String version;
}
