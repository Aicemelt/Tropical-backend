package com.tropical.backend.config.web;

import com.tropical.backend.config.ConsentTypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 웹 설정 클래스
 *
 * <p>
 * Spring Boot의 기본 웹 설정을 커스터마이징하여 프로젝트 요구사항에 맞는
 * 웹 관련 설정을 추가합니다. WebMvcConfigurer를 구현하여 Spring Boot의
 * 자동 설정을 유지하면서 필요한 설정만 오버라이드합니다.
 * </p>
 *
 * <p>주요 설정:</p>
 * <ul>
 *   <li>커스텀 타입 컨버터 등록 - PathVariable enum 처리 개선</li>
 *   <li>포매터 등록 - 데이터 타입 변환 규칙 정의</li>
 *   <li>향후 확장: CORS 설정, 인터셉터, 리소스 핸들러 등</li>
 * </ul>
 *
 * <p>설계 원칙:</p>
 * <ul>
 *   <li>Spring Boot 자동 설정 활용 - 필요한 부분만 커스터마이징</li>
 *   <li>타입 안전성 확보 - 컴파일 타임 에러 검출</li>
 *   <li>API 일관성 유지 - 동일한 데이터 타입의 통일된 처리</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * ConsentType enum 변환을 위한 커스텀 컨버터
     *
     * <p>PathVariable에서 문자열을 ConsentType enum으로 변환할 때 사용됩니다.</p>
     */
    private final ConsentTypeConverter consentTypeConverter;

    /**
     * 커스텀 포매터 및 컨버터 등록
     *
     * <p>
     * Spring MVC의 타입 변환 시스템에 프로젝트 전용 컨버터를 등록합니다.
     * 이를 통해 PathVariable, RequestParam 등에서 문자열을 커스텀 타입으로
     * 자동 변환할 수 있으며, 타입 안전성과 코드 간결성을 확보합니다.
     * </p>
     *
     * <p>등록된 컨버터:</p>
     * <ul>
     *   <li>ConsentTypeConverter: 문자열 → ConsentType enum 변환</li>
     *   <li>UPPER_CASE 및 camelCase 형식 모두 지원</li>
     *   <li>API 일관성 확보를 위한 핵심 컴포넌트</li>
     * </ul>
     *
     * <p>적용 범위:</p>
     * <ul>
     *   <li>@PathVariable ConsentType 파라미터</li>
     *   <li>@RequestParam ConsentType 파라미터</li>
     *   <li>기타 Spring MVC 바인딩 과정에서 문자열 → ConsentType 변환</li>
     * </ul>
     *
     * @param registry 포매터 및 컨버터 등록을 위한 레지스트리
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // ConsentType enum 변환 컨버터 등록
        // PathVariable에서 "termsOfService" → ConsentType.TERMS_OF_SERVICE 자동 변환 지원
        registry.addConverter(consentTypeConverter);

        // 향후 추가 컨버터/포매터 등록 위치
        // 예: LocalDate, LocalDateTime 등의 커스텀 포매터
    }

    /*
     * 향후 확장 가능한 설정들:
     *
     * @Override
     * public void addCorsMappings(CorsRegistry registry) {
     *     // CORS 세부 설정 (현재는 CorsConfig에서 처리)
     * }
     *
     * @Override
     * public void addInterceptors(InterceptorRegistry registry) {
     *     // 요청 인터셉터 등록 (로깅, 인증 등)
     * }
     *
     * @Override
     * public void addResourceHandlers(ResourceHandlerRegistry registry) {
     *     // 정적 리소스 핸들링 커스터마이징
     * }
     */
}