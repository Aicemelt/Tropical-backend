package com.tropical.backend.auth.controller;

import com.tropical.backend.auth.dto.request.TermsRequest;
import com.tropical.backend.auth.dto.response.TermsResponse;
import com.tropical.backend.auth.dto.response.TermsSummaryResponse;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import com.tropical.backend.auth.service.TermsService;
import io.swagger.v3.oas.annotations.Operation;
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
 *   <li>필수/선택 약관 구분 조회 - UI 구성 최적화</li>
 * </ul>
 *
 * <p>접근 권한:</p>
 * <ul>
 *   <li>모든 API는 인증 없이 접근 가능 (Public API)</li>
 *   <li>회원가입 전에도 약관 내용을 미리 확인할 수 있음</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Tag(name = "Terms", description = "약관 및 동의서 조회 API")
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    /**
     * 모든 활성 약관 조회
     *
     * <p>
     * 온보딩 페이지에서 사용자에게 보여줄 모든 약관을 한 번에 조회합니다.
     * 필수 동의와 선택 동의를 구분하여 UI를 구성할 수 있도록 정보를 제공합니다.
     * </p>
     *
     * <p>사용 시나리오:</p>
     * <ul>
     *   <li>온보딩 페이지 초기 로딩 시</li>
     *   <li>회원가입 전 약관 미리보기</li>
     *   <li>약관 동의 체크박스와 함께 표시</li>
     * </ul>
     *
     * @return 모든 활성 약관의 Map (ConsentType -> TermsResponse)
     */
    @GetMapping
    @Operation(
            summary = "모든 활성 약관 조회",
            description = "온보딩에서 사용할 모든 활성 약관을 조회합니다. 필수 동의와 선택 동의를 모두 포함합니다."
    )
    public ResponseEntity<Map<ConsentType, TermsResponse>> getAllActiveTerms() {
        return ResponseEntity.ok(termsService.getAllActiveTerms());
    }

    /**
     * 필수 동의 약관만 조회
     *
     * <p>
     * 필수 동의 항목들의 약관만 조회합니다.
     * 서비스 이용을 위해 반드시 동의해야 하는 약관들을 별도로 강조하여 표시할 때 사용됩니다.
     * </p>
     *
     * @return 필수 동의 약관의 Map
     */
    @GetMapping("/required")
    @Operation(
            summary = "필수 동의 약관 조회",
            description = "서비스 이용을 위해 필수로 동의해야 하는 약관들을 조회합니다."
    )
    public ResponseEntity<Map<ConsentType, TermsResponse>> getRequiredTerms() {
        return ResponseEntity.ok(termsService.getRequiredTerms());
    }

    /**
     * 선택 동의 약관만 조회
     *
     * <p>
     * AI 개인화 서비스를 위한 선택 동의 약관들만 조회합니다.
     * 사용자가 원하는 개인화 수준을 선택할 수 있도록 정보를 제공합니다.
     * </p>
     *
     * @return 선택 동의 약관의 Map
     */
    @GetMapping("/optional")
    @Operation(
            summary = "선택 동의 약관 조회",
            description = "AI 개인화 서비스를 위한 선택적 동의 약관들을 조회합니다."
    )
    public ResponseEntity<Map<ConsentType, TermsResponse>> getOptionalTerms() {
        return ResponseEntity.ok(termsService.getOptionalTerms());
    }

    /**
     * 특정 약관 상세 조회
     *
     * <p>
     * 마이페이지에서 "약관 다시 보기" 버튼을 클릭했을 때 사용됩니다.
     * 개별 약관의 상세 내용을 모달이나 새 페이지로 표시할 수 있습니다.
     * </p>
     *
     * <p>사용 시나리오:</p>
     * <ul>
     *   <li>마이페이지에서 개별 약관 재확인</li>
     *   <li>약관 변경 알림 시 새 약관 내용 표시</li>
     *   <li>고객센터에서 약관 관련 문의 답변</li>
     * </ul>
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
        return termsService.getTermsByType(consentType) // Optional<TermsResponse>
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 모든 활성 약관 요약 목록 조회
     *
     * <p>
     * 온보딩 페이지에서 체크박스 목록을 구성할 때 사용됩니다.
     * 약관의 상세 내용 없이 제목, 버전, 필수/선택 여부만 제공하여
     * 가벼운 응답을 보장하고 프론트엔드에서 빠르게 UI를 구성할 수 있도록 합니다.
     * </p>
     *
     * @return 200 OK + 모든 활성 약관의 요약 정보 목록
     */
    @GetMapping("/summary")
    @Operation(
            summary = "모든 활성 약관 요약 목록 조회",
            description = "온보딩과 마이페이지에서 사용할 약관 요약 정보를 조회합니다. 상세 내용 없이 메타데이터만 제공합니다."
    )
    public ResponseEntity<List<TermsSummaryResponse>> getAllActiveTermsSummary() {
        return ResponseEntity.ok(termsService.getAllActiveTermsSummary());
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
     * @return 201 Created + 생성된 약관 정보 (ResponseEntity.body에 TermsResponse 포함)
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
        URI location = URI.create(String.format("/api/terms/%s", saved.consentType().name()));
        return ResponseEntity.created(location).body(saved);
    }
}