package com.tropical.backend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 계정 회원가입 요청 DTO
 *
 * <p>
 * 이메일과 비밀번호를 사용하는 로컬 계정 회원가입 시 클라이언트에서 전송하는
 * 요청 데이터를 담는 DTO입니다. Bean Validation을 통해 입력값 검증을 수행합니다.
 * </p>
 *
 * <p>검증 규칙:</p>
 * <ul>
 *   <li>이메일: 유효한 이메일 형식, 필수 입력</li>
 *   <li>비밀번호: 영문 대/소문자, 숫자, 특수문자 조합 10자 이상</li>
 *   <li>닉네임: 2-50자, 필수 입력</li>
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
public class SignupRequest {

    /**
     * 사용자 이메일 주소 (로그인 ID)
     *
     * <p>
     * 로컬 계정의 로그인 ID로 사용되며, 이메일 인증이 필요합니다.
     * 시스템에서 활성 계정 중 이메일 중복은 허용하지 않습니다.
     * </p>
     */
    @NotBlank(message = "이메일은 필수 입력 항목입니다")
    @Email(message = "유효한 이메일 형식을 입력해주세요")
    @Size(max = 255, message = "이메일은 255자를 초과할 수 없습니다")
    private String email;

    /**
     * 사용자 비밀번호 (평문)
     *
     * <p>
     * 서버에서 BCrypt를 사용하여 해시화됩니다.
     * 보안을 위해 영문 대/소문자, 숫자, 특수문자를 모두 포함해야 합니다.
     * </p>
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[!-~]{8,20}$",
            message = "비밀번호는 영문 대/소문자, 숫자, 특수문자를 모두 포함해야 합니다"
    )
    private String password;

    /**
     * 사용자 닉네임
     *
     * <p>
     * 시스템 전체에서 유일해야 하며, 사용자를 식별하는 표시명으로 사용됩니다.
     * 중복된 닉네임으로 가입 시도 시 에러가 발생합니다.
     * </p>
     */
    @NotBlank(message = "닉네임은 필수 입력 항목입니다")
    @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하로 입력해주세요")
    private String nickname;
}