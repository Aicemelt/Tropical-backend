package com.tropical.backend.auth.controller;

import com.tropical.backend.auth.dto.request.TermsRequest;
import com.tropical.backend.auth.dto.response.TermsResponse;
import com.tropical.backend.auth.dto.response.TermsSummaryResponse;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import com.tropical.backend.auth.service.TermsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 약관 관리 컨트롤러
 *
 * <p>
 * 서비스 이용약관, 개인정보처리방침, 각종 동의서의 내용을 조회하는 REST API를 제공합니다.
 * 온보딩 과정과 마이페이지에서 약관 내용을 확인할 때 사용됩니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>전체 약관 조회 - 온보딩 시 모든 동의서 표시</li>
 *   <li>개별 약관 조회 - 마이페이지에서 약관 재확인</li>
 *   <li>필수/선택 약관 구분 조회 - 쿼리 파라미터로 필터링</li>
 *   <li>약관 요약 정보 - 가벼운 목록 조회</li>
 * </ul>
 *
 * <p>접근 권한:</p>
 * <ul>
 *   <li>모든 조회 API는 인증 없이 접근 가능 (Public API)</li>
 *   <li>회원가입 전에도 약관 내용을 미리 확인할 수 있음</li>
 *   <li>약관 생성은 관리자 권한 필요 (향후 보안 설정)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.18
 */
@Tag(name = "Terms", description = "약관 및 동의서 조회 API")
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    /**
     * 약관 타입 필터링을 위한 열거형
     */
    public enum TermsFilterType {
        /**
         * 모든 약관 조회 (기본값)
         */
        ALL,
        /**
         * 필수 동의 약관만 조회
         */
        REQUIRED,
        /**
         * 선택 동의 약관만 조회
         */
        OPTIONAL
    }

    /**
     * 응답 형식을 위한 열거형
     */
    public enum ResponseFormat {
        /**
         * 상세 정보 포함 (기본값)
         */
        DETAIL,
        /**
         * 요약 정보만 포함
         */
        SUMMARY
    }

    /**
     * 활성 약관 조회 (쿼리 파라미터 기반 필터링)
     *
     * <p>
     * 온보딩 페이지에서 사용자에게 보여줄 약관을 조회합니다.
     * 쿼리 파라미터를 통해 필수/선택 약관을 구분하여 조회하거나,
     * 요약 정보만 가져올 수 있습니다.
     * </p>
     *
     * <p>사용 시나리오:</p>
     * <ul>
     *   <li>GET /api/v1/terms - 모든 활성 약관 조회</li>
     *   <li>GET /api/v1/terms?type=required - 필수 약관만 조회</li>
     *   <li>GET /api/v1/terms?type=optional - 선택 약관만 조회</li>
     *   <li>GET /api/v1/terms?format=summary - 모든 약관 요약 정보</li>
     *   <li>GET /api/v1/terms?type=required&format=summary - 필수 약관 요약 정보</li>
     * </ul>
     *
     * @param type   약관 타입 필터 (all|required|optional, 기본값: all)
     * @param format 응답 형식 (detail|summary, 기본값: detail)
     * @return 필터링된 약관 정보
     */
    @GetMapping
    @Operation(
            summary = "활성 약관 조회",
            description = "쿼리 파라미터를 통해 필수/선택 약관을 구분 조회하거나 요약 정보만 가져올 수 있습니다."
    )
    public ResponseEntity<?> getActiveTerms(
            @Parameter(description = "약관 타입 필터", example = "required")
            @RequestParam(defaultValue = "ALL") TermsFilterType type,
            @Parameter(description = "응답 형식", example = "summary")
            @RequestParam(defaultValue = "DETAIL") ResponseFormat format) {

        // 요약 정보 요청인 경우
        if (format == ResponseFormat.SUMMARY) {
            List<TermsSummaryResponse> summaryList = termsService.getAllActiveTermsSummary();

            // 타입 필터링 적용
            List<TermsSummaryResponse> filteredList = switch (type) {
                case REQUIRED -> summaryList.stream()
                        .filter(TermsSummaryResponse::required)
                        .toList();
                case OPTIONAL -> summaryList.stream()
                        .filter(summary -> !summary.required())
                        .toList();
                case ALL -> summaryList;
            };

            return ResponseEntity.ok(filteredList);
        }

        // 상세 정보 요청인 경우
        Map<ConsentType, TermsResponse> termsMap = switch (type) {
            case REQUIRED -> termsService.getRequiredTerms();
            case OPTIONAL -> termsService.getOptionalTerms();
            case ALL -> termsService.getAllActiveTerms();
        };

        return ResponseEntity.ok(termsMap);
    }

    /**
     * 특정 약관 상세 조회
     *
     * <p>
     * 마이페이지에서 "약관 다시 보기" 버튼을 클릭했을 때 사용됩니다.
     * 개별 약관의 상세 내용을 모달이나 새 페이지로 표시할 수 있습니다.
     * </p>
     *
     * @param consentType 조회할 동의 항목 타입
     * @return 200 OK + TermsResponse / 404 Not Found
     */
    @GetMapping("/{consentType}")
    @Operation(
            summary = "특정 약관 상세 조회",
            description = "지정된 동의 항목의 약관 내용을 상세 조회합니다. 마이페이지에서 약관 재확인 시 사용됩니다."
    )
    public ResponseEntity<TermsResponse> getTermsByType(@PathVariable ConsentType consentType) {
        return termsService.getTermsByType(consentType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 새로운 약관 버전 생성(배포)
     *
     * <p>
     * 관리자가 새로운 약관을 등록할 때 사용됩니다.
     * 동일한 동의 타입(consentType)의 기존 활성 약관은 모두 비활성화되고,
     * 전달된 정보로 신규 약관이 생성되어 활성 상태로 저장됩니다.
     * </p>
     *
     * @param request 생성할 약관 정보 (타입, 제목, 본문, 버전)
     * @return 201 Created + 생성된 약관 정보
     */
    @PostMapping
    @Operation(
            summary = "새 약관 버전 생성(배포)",
            description = "기존 활성 약관을 비활성화한 후, 새 버전을 활성으로 저장합니다. 성공 시 201 Created 반환"
    )
    public ResponseEntity<TermsResponse> createTerms(@Valid @RequestBody TermsRequest request) {
        TermsResponse saved = termsService.createNewTermsVersion(
                request.getConsentType(),
                request.getTitle(),
                request.getContent(),
                request.getVersion()
        );
        URI location = URI.create(String.format("/api/v1/terms/%s", saved.consentType().name()));
        return ResponseEntity.created(location).body(saved);
    }
}