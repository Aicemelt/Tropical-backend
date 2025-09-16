package com.tropical.backend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 계정 로그인 요청 DTO
 *
 * <p>
 * 이메일과 비밀번호를 사용하는 로컬 계정 로그인 시 클라이언트에서 전송하는
 * 요청 데이터를 담는 DTO입니다.
 * </p>
 *
 * <p>검증 규칙:</p>
 * <ul>
 *   <li>이메일: 유효한 이메일 형식, 필수 입력</li>
 *   <li>비밀번호: 필수 입력 (서버에서 해시 비교)</li>
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
public class LoginRequest {

    /**
     * 사용자 이메일 주소 (로그인 ID)
     *
     * <p>로컬 계정의 로그인 식별자로 사용됩니다.</p>
     */
    @NotBlank(message = "이메일은 필수 입력 항목입니다")
    @Email(message = "유효한 이메일 형식을 입력해주세요")
    private String email;

    /**
     * 사용자 비밀번호 (평문)
     *
     * <p>
     * 서버에서 저장된 해시와 비교하여 인증을 수행합니다.
     * 네트워크 전송 시 HTTPS를 통해 암호화되어 전송됩니다.
     * </p>
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다")
    private String password;
}