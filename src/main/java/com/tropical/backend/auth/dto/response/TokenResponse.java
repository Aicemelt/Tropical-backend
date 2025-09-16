package com.tropical.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * JWT 토큰 응답 DTO
 *
 * <p>
 * 로그인 성공 시 클라이언트에게 JWT 토큰 정보를 전달하는 응답 DTO입니다.
 * Access Token과 Refresh Token, 그리고 토큰 관련 메타데이터를 포함합니다.
 * </p>
 *
 * <p>포함되는 정보:</p>
 * <ul>
 *   <li>Access Token (API 호출용 단기 토큰)</li>
 *   <li>Refresh Token (토큰 갱신용 장기 토큰)</li>
 *   <li>토큰 타입 (Bearer)</li>
 *   <li>Access Token 만료 시간</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.13
 */
@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {

    /**
     * Access Token
     *
     * <p>
     * API 호출 시 Authorization 헤더에 사용할 JWT 토큰입니다.
     * Bearer {accessToken} 형태로 사용됩니다.
     * </p>
     */
    private String accessToken;

    /**
     * Refresh Token
     *
     * <p>
     * Access Token 갱신을 위한 장기 유효 토큰입니다.
     * 보안상 HttpOnly 쿠키로 저장하는 것이 권장됩니다.
     * </p>
     */
    private String refreshToken;

    /**
     * 토큰 타입
     *
     * <p>OAuth2 표준에 따라 "Bearer"로 고정됩니다.</p>
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access Token 만료 시간
     *
     * <p>
     * 클라이언트에서 토큰 갱신 시점을 판단하기 위한 정보입니다.
     * 실제 만료 시간보다 약간 일찍 갱신하는 것이 권장됩니다.
     * </p>
     */
    private LocalDateTime expiresAt;

    /**
     * 토큰 생성 시간
     */
    private LocalDateTime issuedAt;

    /**
     * 사용자 정보와 함께 토큰 응답 생성
     *
     * <p>
     * 로그인 성공 시 사용자 정보와 함께 토큰 정보를 반환할 때 사용합니다.
     * </p>
     *
     * @param accessToken  Access Token
     * @param refreshToken Refresh Token
     * @param expiresAt    Access Token 만료 시간
     * @param user         사용자 정보
     * @return 토큰과 사용자 정보가 포함된 응답 객체
     */
    public static TokenResponseWithUser withUser(
            String accessToken,
            String refreshToken,
            LocalDateTime expiresAt,
            UserResponse user) {

        return TokenResponseWithUser.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .issuedAt(LocalDateTime.now())
                .user(user)
                .build();
    }

    /**
     * 토큰 정보만 포함하는 응답 생성
     *
     * <p>
     * 토큰 갱신 시 사용자 정보 없이 토큰 정보만 반환할 때 사용합니다.
     * </p>
     *
     * @param accessToken  Access Token
     * @param refreshToken Refresh Token
     * @param expiresAt    Access Token 만료 시간
     * @return 토큰 정보만 포함된 응답 객체
     */
    public static TokenResponse of(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 사용자 정보가 포함된 토큰 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TokenResponseWithUser {
        private String accessToken;
        private String refreshToken;
        @Builder.Default
        private String tokenType = "Bearer";
        private LocalDateTime expiresAt;
        private LocalDateTime issuedAt;
        private UserResponse user;
    }
}