package com.tropical.backend.auth.dto.response;

import com.tropical.backend.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 *
 * <p>
 * 클라이언트에게 사용자 정보를 전달할 때 사용하는 응답 DTO입니다.
 * 보안상 민감한 정보(비밀번호 해시 등)는 제외하고 필요한 정보만 포함합니다.
 * </p>
 *
 * <p>포함되는 정보:</p>
 * <ul>
 *   <li>기본 사용자 정보 (ID, 이메일, 닉네임)</li>
 *   <li>계정 상태 정보 (타입, 인증 여부, 온보딩 완료 여부)</li>
 *   <li>계정 생성 및 마지막 로그인 시간</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    /**
     * 사용자 고유 식별자
     */
    private Long id;

    /**
     * 사용자 이메일 주소
     */
    private String email;

    /**
     * 사용자 닉네임
     */
    private String nickname;

    /**
     * 계정 타입
     *
     * <p>로컬 계정인지 소셜 계정인지 구분합니다.</p>
     */
    private User.AccountType accountType;

    /**
     * 이메일 인증 완료 여부
     *
     * <p>
     * 로컬 계정의 경우 이메일 인증 완료 상태를 나타냅니다.
     * 소셜 계정의 경우 항상 true입니다.
     * </p>
     */
    private Boolean emailVerified;

    /**
     * 온보딩 완료 여부
     *
     * <p>
     * 필수 동의를 모두 완료했는지 여부를 나타냅니다.
     * false인 경우 클라이언트에서 온보딩 페이지로 리다이렉트해야 합니다.
     * </p>
     */
    private Boolean onboardingCompleted;

    /**
     * 계정 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLoginAt;

    /**
     * User 엔터티로부터 UserResponse DTO 생성
     *
     * <p>
     * 엔터티의 모든 정보를 DTO로 변환하되, 보안상 민감한 정보는 제외합니다.
     * 정적 팩토리 메서드 패턴을 사용하여 객체 생성의 의도를 명확히 합니다.
     * </p>
     *
     * @param user 변환할 User 엔터티
     * @return 생성된 UserResponse DTO
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accountType(user.getAccountType())
                .emailVerified(user.getEmailVerified())
                .onboardingCompleted(user.getOnboardingCompleted())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * 간단한 사용자 정보만 포함하는 UserResponse 생성
     *
     * <p>
     * 로그인 응답이나 간단한 프로필 조회 시 사용할 수 있는
     * 최소한의 정보만 포함하는 DTO를 생성합니다.
     * </p>
     *
     * @param user 변환할 User 엔터티
     * @return 기본 정보만 포함된 UserResponse DTO
     */
    public static UserResponse minimal(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accountType(user.getAccountType())
                .onboardingCompleted(user.getOnboardingCompleted())
                .build();
    }
}