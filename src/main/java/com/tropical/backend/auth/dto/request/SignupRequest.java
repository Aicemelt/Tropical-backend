package com.tropical.backend.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 계정 회원가입 요청 DTO (온보딩 정보 포함)
 *
 * <p>
 * 이메일과 비밀번호를 사용하는 로컬 계정 회원가입 시 클라이언트에서 전송하는
 * 요청 데이터를 담는 DTO입니다. 온보딩 정보(약관 동의)도 함께 받습니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.15
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    @NotBlank(message = "이메일은 필수 입력 항목입니다")
    @Email(message = "유효한 이메일 형식을 입력해주세요")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[!-~]{8,20}$",
            message = "비밀번호는 영문 대/소문자, 숫자, 특수문자를 모두 포함해야 합니다"
    )
    private String password;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요")
    private String nickname;

    // ===== 온보딩(동의) 정보 추가 =====

    @Valid
    @NotNull(message = "필수 동의 정보가 필요합니다")
    private RequiredConsents requiredConsents;

    @Valid
    private OptionalConsents optionalConsents; // null 허용

    /**
     * 필수 동의 항목
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequiredConsents {
        @AssertTrue(message = "이용약관 동의는 필수입니다")
        @NotNull(message = "이용약관 동의 여부를 선택해주세요")
        private Boolean termsOfService;

        @AssertTrue(message = "개인정보처리방침 동의는 필수입니다")
        @NotNull(message = "개인정보처리방침 동의 여부를 선택해주세요")
        private Boolean privacyPolicy;

        @AssertTrue(message = "일정 기반 추천 동의는 필수입니다")
        @NotNull(message = "일정 기반 추천 동의 여부를 선택해주세요")
        private Boolean calendarPersonalization;
    }

    /**
     * 선택 동의 항목
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionalConsents {
        private Boolean diaryPersonalization;   // 일기 기반 추천 동의
        private Boolean todoPersonalization;    // 할일 기반 추천 동의
        private Boolean bucketPersonalization;  // 버킷리스트 기반 추천 동의
    }
}