package com.tropical.backend.config.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 전용 설정
 *
 * <p>
 * SecurityConfig에서 PasswordEncoder를 분리하여 순환 의존성 문제를 해결합니다.
 * UserService에서 PasswordEncoder만 필요하므로 별도 Configuration으로 분리했습니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.14
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 비밀번호 암호화 Bean
     *
     * <p>
     * UserService에서 로컬 계정 생성 시 비밀번호 해시화에 사용됩니다.
     * BCrypt 알고리즘을 사용하여 안전한 단방향 암호화를 제공합니다.
     * </p>
     *
     * @return BCrypt 기반 PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}