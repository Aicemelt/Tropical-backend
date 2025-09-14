package com.tropical.backend.config.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 담당 컴포넌트
 *
 * <p>
 * Access Token과 Refresh Token의 생성, 검증, 파싱을 담당하는 핵심 JWT 처리 클래스입니다.
 * application.yml의 JWT 설정값을 사용하여 토큰을 안전하게 관리합니다.
 * JJWT 0.12.x 버전에 맞춰 구현되었습니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>JWT Access Token 생성 및 검증</li>
 *   <li>JWT Refresh Token 생성 및 검증</li>
 *   <li>토큰에서 사용자 정보 추출</li>
 *   <li>토큰 만료 시간 관리</li>
 *   <li>토큰 서명 검증</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.13
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    /**
     * JWT 설정값을 주입받아 초기화
     *
     * @param secret                   JWT 서명에 사용할 비밀키
     * @param accessTokenExpirationMs  Access Token 만료 시간 (밀리초)
     * @param refreshTokenExpirationMs Refresh Token 만료 시간 (밀리초)
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;

        log.info("JWT Token Provider 초기화 완료 - Access Token 만료: {}ms, Refresh Token 만료: {}ms",
                accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    /**
     * Access Token 생성
     *
     * <p>
     * 사용자 인증 후 API 접근을 위한 단기 유효 토큰을 생성합니다.
     * 사용자 ID와 이메일을 클레임에 포함합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param email  사용자 이메일
     * @return 생성된 Access Token
     */
    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("tokenType", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiryDate)  // setExpirationTime → setExpiration
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.debug("Access Token 생성 완료 - 사용자 ID: {}, 만료 시간: {}", userId, expiryDate);
        return token;
    }

    /**
     * Refresh Token 생성
     *
     * <p>
     * Access Token 갱신을 위한 장기 유효 토큰을 생성합니다.
     * 보안을 위해 사용자 ID만 포함하고 추가 정보는 제외합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("tokenType", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiryDate)  // setExpirationTime → setExpiration
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.debug("Refresh Token 생성 완료 - 사용자 ID: {}, 만료 시간: {}", userId, expiryDate);
        return token;
    }

    /**
     * 토큰에서 사용자 ID 추출
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 토큰에서 사용자 이메일 추출
     *
     * @param token JWT 토큰
     * @return 사용자 이메일
     * @throws JwtException 토큰이 유효하지 않거나 이메일 클레임이 없는 경우
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * 토큰 타입 확인
     *
     * @param token JWT 토큰
     * @return 토큰 타입 (ACCESS 또는 REFRESH)
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tokenType", String.class);
    }

    /**
     * 토큰 유효성 검증
     *
     * <p>
     * 토큰의 서명, 만료 시간, 형식 등을 종합적으로 검증합니다.
     * 검증 실패 시 구체적인 실패 사유를 로그에 기록합니다.
     * </p>
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 무효하면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()  // parserBuilder → parser
                    .verifyWith(secretKey)  // setSigningKey → verifyWith
                    .build()
                    .parseSignedClaims(token);  // parseClaimsJws → parseSignedClaims
            return true;

        } catch (MalformedJwtException e) {
            log.warn("JWT 토큰 형식 오류: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰 만료: 만료 시간 = {}", e.getClaims().getExpiration());
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있거나 null: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT 서명 검증 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류: {}", e.getMessage(), e);
        }

        return false;
    }

    /**
     * Access Token 타입 검증
     *
     * @param token 검증할 토큰
     * @return Access Token이면 true
     */
    public boolean isAccessToken(String token) {
        try {
            String tokenType = getTokenType(token);
            return "ACCESS".equals(tokenType);
        } catch (Exception e) {
            log.warn("토큰 타입 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh Token 타입 검증
     *
     * @param token 검증할 토큰
     * @return Refresh Token이면 true
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = getTokenType(token);
            return "REFRESH".equals(tokenType);
        } catch (Exception e) {
            log.warn("토큰 타입 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰 만료 시간 확인
     *
     * @param token JWT 토큰
     * @return 만료 시간 (Date)
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @param token JWT 토큰
     * @return 만료되었으면 true
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.warn("토큰 만료 확인 중 오류: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 토큰에서 Claims 추출
     *
     * <p>
     * 내부적으로 사용되는 메서드로, 토큰을 파싱하여 클레임을 추출합니다.
     * 토큰이 유효하지 않은 경우 예외가 발생합니다.
     * </p>
     *
     * @param token JWT 토큰
     * @return 토큰의 Claims 객체
     * @throws JwtException 토큰 파싱 실패 시
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()  // parserBuilder → parser
                .verifyWith(secretKey)  // setSigningKey → verifyWith
                .build()
                .parseSignedClaims(token)  // parseClaimsJws → parseSignedClaims
                .getPayload();  // getBody → getPayload
    }

    /**
     * 토큰 생성 시간 확인
     *
     * @param token JWT 토큰
     * @return 토큰 생성 시간
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getIssuedAt();
    }

    /**
     * 토큰의 남은 유효 시간 계산 (밀리초)
     *
     * @param token JWT 토큰
     * @return 남은 유효 시간 (밀리초), 만료된 경우 0
     */
    public long getRemainingValidityInMs(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            log.warn("토큰 유효시간 계산 실패: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 이메일 인증용 JWT 토큰 생성
     *
     * <p>
     * 회원가입 시 이메일 인증을 위한 단기 유효 토큰을 생성합니다.
     * 30분의 짧은 유효기간을 가지며, 인증 목적임을 명시합니다.
     * </p>
     *
     * @param email 인증할 이메일
     * @return 이메일 인증용 JWT 토큰
     */
    public String createEmailVerificationToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (30 * 60 * 1000)); // 30분

        String token = Jwts.builder()
                .setSubject(email)
                .claim("purpose", "email_verification")
                .claim("tokenType", "EMAIL_VERIFY")
                .setIssuedAt(now)
                .setExpiration(expiryDate)  // setExpirationTime → setExpiration
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.debug("이메일 인증 토큰 생성 완료 - 이메일: {}, 만료 시간: {}", email, expiryDate);
        return token;
    }

    /**
     * 이메일 인증 토큰에서 이메일 추출
     *
     * @param token 이메일 인증 토큰
     * @return 인증할 이메일 주소
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public String getEmailFromVerificationToken(String token) {
        Claims claims = getClaimsFromToken(token);

        // 토큰 목적 확인
        String purpose = claims.get("purpose", String.class);
        if (!"email_verification".equals(purpose)) {
            throw new IllegalArgumentException("이메일 인증 토큰이 아닙니다");
        }

        return claims.getSubject();
    }
}