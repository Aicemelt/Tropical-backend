package com.tropical.backend.config.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT 토큰 생성 및 검증 담당 컴포넌트
 *
 * <p>
 * Access Token, Refresh Token, 이메일 인증 Token의 생성과 검증을 담당하는
 * 핵심 JWT 처리 클래스입니다. JJWT 0.12.x 버전에 맞춰 구현되었으며,
 * 시계 오차 허용과 강화된 보안 정책을 지원합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>JWT Access/Refresh Token 생성 및 검증</li>
 *   <li>이메일 인증용 단기 토큰 생성</li>
 *   <li>토큰 타입별 구분 및 검증</li>
 *   <li>시계 오차(±60초) 허용으로 안정성 향상</li>
 *   <li>Base64/UTF-8 키 지원 및 보안 길이 검증</li>
 *   <li>상세한 예외 로깅으로 디버깅 지원</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.3
 * @since 2025.09.14
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * JWT 서명을 위한 비밀 키
     *
     * <p>Base64 인코딩된 시크릿 권장 (운영 환경에서는 최소 256bit 이상)</p>
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Access Token 만료 시간 (밀리초)
     *
     * <p>기본값: 900000ms (15분)</p>
     */
    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    /**
     * Refresh Token 만료 시간 (밀리초)
     *
     * <p>기본값: 1209600000ms (14일)</p>
     */
    @Value("${jwt.refresh-token-expiration-ms:1209600000}")
    private long refreshTokenExpirationMs;

    /**
     * JWT 서명에 사용할 SecretKey 객체
     */
    private SecretKey secretKey;

    /**
     * Bean 초기화 후 SecretKey 생성
     *
     * <p>
     *
     * @PostConstruct를 사용하여 @Value 주입 완료 후
     * SecretKey를 안전하게 초기화합니다.
     * </p>
     */
    @PostConstruct
    void initKey() {
        this.secretKey = buildKey(jwtSecret);
    }

    /**
     * JWT 서명키 생성
     *
     * <p>
     * Base64 디코딩을 우선 시도하고, 실패 시 UTF-8 폴백을 사용합니다.
     * 운영 환경에서 키 길이가 256bit(32바이트) 미만인 경우 경고를 출력합니다.
     * </p>
     *
     * @param secret 설정 파일의 JWT 시크릿
     * @return HMAC-SHA 알고리즘용 SecretKey
     */
    private SecretKey buildKey(String secret) {
        try {
            // Base64 디코딩 우선 시도
            byte[] decoded = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(decoded);
        } catch (IllegalArgumentException e) {
            // Base64 실패 시 UTF-8 폴백
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            if (raw.length < 32) {
                log.warn("JWT secret 길이가 256bit(32바이트) 미만입니다. 운영에서는 더 긴 키를 권장합니다.");
            }
            return Keys.hmacShaKeyFor(raw);
        }
    }

    // ====================== 토큰 생성 메서드들 ======================

    /**
     * Access Token 생성
     *
     * <p>
     * API 접근을 위한 단기 유효 토큰을 생성합니다.
     * 사용자 ID, 이메일, 토큰 타입을 클레임에 포함합니다.
     * </p>
     *
     * @param userId 사용자 ID (Subject)
     * @param email  사용자 이메일 (Claim)
     * @return 생성된 Access Token
     */
    public String createAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("tokenType", "ACCESS")              // 토큰 타입 구분
                // 하위호환이 필요한 경우 .claim("purpose", "ACCESS") 추가 가능
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(secretKey, Jwts.SIG.HS256) // // JJWT로 통일 및 알고리즘을 HS256으로 명시
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * <p>
     * Access Token 갱신을 위한 장기 유효 토큰을 생성합니다.
     * 보안을 위해 사용자 ID만 포함하고 민감한 정보는 제외합니다.
     * </p>
     *
     * @param userId 사용자 ID (Subject)
     * @return 생성된 Refresh Token
     */
    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpirationMs)))
                .signWith(secretKey, Jwts.SIG.HS256) // JJWT로 통일 및 알고리즘을 HS256으로 명시
                .compact();
    }

    /**
     * 이메일 인증용 토큰 생성
     *
     * <p>
     * 회원가입 시 이메일 인증을 위한 단기 유효 토큰을 생성합니다.
     * 30분의 짧은 유효기간을 가지며, 일회성 사용을 위해 설계되었습니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @param email  인증할 이메일 주소
     * @return 생성된 이메일 인증 토큰
     */
    public String createEmailVerifyToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("tokenType", "EMAIL_VERIFY")
                .claim("purpose", "email_verification")    // 하위호환용(선택사항)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60 * 30))) // 30분 만료
                .signWith(secretKey)
                .compact();
    }

    // ====================== 토큰 파싱 및 검증 메서드들 ======================

    /**
     * JWT 토큰 파싱 및 Claims 추출
     *
     * <p>
     * ±60초 시계 오차를 허용하여 토큰을 파싱합니다.
     * 각 예외 타입별로 구분된 로깅을 제공하여 디버깅을 지원합니다.
     * </p>
     *
     * @param token 파싱할 JWT 토큰
     * @return 토큰의 Claims 객체
     * @throws ExpiredJwtException      토큰이 만료된 경우
     * @throws UnsupportedJwtException  지원하지 않는 토큰 형식
     * @throws MalformedJwtException    잘못된 토큰 형식
     * @throws SignatureException       서명 검증 실패
     * @throws IllegalArgumentException 토큰이 null이거나 빈 문자열
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .clockSkewSeconds(60)              // ±60초 시계 오차 허용
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("JWT bad signature");
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT illegal argument");
            throw e;
        }
    }

    /**
     * 토큰 유효성 검증
     *
     * <p>
     * 토큰의 서명, 만료 시간, 형식을 종합적으로 검증합니다.
     * 예외 발생 시 false를 반환하며, 상세 로깅은 parseClaims에서 처리됩니다.
     * </p>
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 무효하면 false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // 내부에서 예외 타입별 로깅
            return true;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.warn("validateToken failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Access Token 타입 검증
     *
     * @param token 검증할 토큰
     * @return Access Token이면 true
     */
    public boolean isAccessToken(String token) {
        return "ACCESS".equals(String.valueOf(parseClaims(token).get("tokenType")));
    }

    /**
     * Refresh Token 타입 검증
     *
     * @param token 검증할 토큰
     * @return Refresh Token이면 true
     */
    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(String.valueOf(parseClaims(token).get("tokenType")));
    }

    /**
     * JWT 토큰으로부터 Spring Security Authentication 객체 생성
     *
     * <p>
     * 토큰의 Claims에서 사용자 정보를 추출하여 Authentication을 생성합니다.
     * 이메일을 우선 사용하고, 없으면 사용자 ID를 principal로 사용합니다.
     * 프로젝트 요구사항에 따라 UserDetailsService와 연동하도록 수정 가능합니다.
     * </p>
     *
     * @param token JWT 토큰
     * @return Spring Security Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String principal = (String) claims.get("email");
        if (principal == null) {
            principal = "userId:" + claims.getSubject();
        }
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    // ====================== 헬퍼 메서드들 ======================

    /**
     * Access Token 만료 시간을 초 단위로 반환
     *
     * <p>쿠키의 Max-Age 설정에 사용할 수 있는 초 단위 값을 반환합니다.</p>
     *
     * @return Access Token 만료 시간 (초)
     */
    public int getAccessMaxAge() {
        return Math.toIntExact(accessTokenExpirationMs / 1000L);
    }

    /**
     * Refresh Token 만료 시간을 초 단위로 반환
     *
     * <p>쿠키의 Max-Age 설정에 사용할 수 있는 초 단위 값을 반환합니다.</p>
     *
     * @return Refresh Token 만료 시간 (초)
     */
    public int getRefreshMaxAge() {
        return Math.toIntExact(refreshTokenExpirationMs / 1000L);
    }

    /** ====================== 호환 메서드(기존 코드 대응) ====================== */

    /**
     * 토큰에서 userId(subject) 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 email 추출 (ACCESS/EMAIL_VERIFY에 존재, REFRESH에는 없을 수 있음)
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        Object email = claims.get("email");
        return email != null ? email.toString() : null;
    }

    /**
     * 토큰 만료 시각 추출
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration();
    }
}
