package com.tropical.backend.auth.service;

import com.tropical.backend.auth.dto.response.TermsResponse;
import com.tropical.backend.auth.dto.response.TermsSummaryResponse;
import com.tropical.backend.auth.entity.Terms;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import com.tropical.backend.auth.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 약관 관리 서비스
 *
 * <p>
 * 서비스 이용약관, 개인정보처리방침, 각종 동의서의 내용을 관리하고 제공합니다.
 * 온보딩 시 동의서 표시, 마이페이지에서 약관 재확인, 법무팀의 약관 버전 관리 등의
 * 핵심 기능을 담당합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>활성 약관 조회 - 온보딩 및 동의 과정에서 사용</li>
 *   <li>특정 약관 상세 조회 - 마이페이지 약관 재확인</li>
 *   <li>약관 버전 관리 - 법무팀의 약관 업데이트 지원</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.17
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TermsService {

    private final TermsRepository termsRepository;

    /**
     * 모든 활성 약관 조회
     *
     * <p>
     * 온보딩 과정에서 사용자에게 보여줄 모든 동의서를 한 번에 조회합니다.
     * 필수 동의와 선택 동의를 구분하여 UI를 구성할 수 있도록 정보를 제공합니다.
     * </p>
     *
     * @return 모든 활성 약관의 Map (ConsentType -> TermsResponse)
     */
    public Map<ConsentType, TermsResponse> getAllActiveTerms() {
        log.debug("모든 활성 약관 조회 시작");

        List<Terms> activeTerms = termsRepository.findByActiveTrueOrderByConsentType();

        Map<ConsentType, TermsResponse> termsMap = activeTerms.stream()
                .collect(Collectors.toMap(
                        Terms::getConsentType,
                        this::convertToResponse,
                        (oldV, newV) -> newV
                ));

        log.info("활성 약관 조회 완료 - 조회된 약관 수: {}", termsMap.size());
        return termsMap;
    }

    /**
     * 특정 동의 타입의 최신 활성 약관 조회
     *
     * <p>
     * 동일 ConsentType에 활성 약관이 다수라도 최신 1건만 반환합니다.
     * 컨트롤러에서 Optional을 404로 매핑합니다.
     * </p>
     *
     * @param consentType 조회할 동의 항목 타입
     * @return Optional<TermsResponse> (없으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public Optional<TermsResponse> getTermsByType(ConsentType consentType) {
        log.debug("특정 약관 조회 - 동의 타입: {}", consentType);
        return termsRepository
                .findByConsentTypeAndActiveTrue(consentType)
                .map(terms -> {
                    TermsResponse response = convertToResponse(terms);
                    log.debug("약관 조회 완료 - 동의 타입: {}, 버전: {}", consentType, response.version());
                    return response;
                });
    }

    /**
     * 필수 동의 약관 목록 조회
     *
     * <p>
     * 온보딩 시 필수로 동의해야 하는 약관들만 조회합니다.
     * 사용자가 필수 동의를 완료하기 전에는 서비스 이용이 불가능함을 알려줄 수 있습니다.
     * </p>
     *
     * @return 필수 동의 약관의 Map
     */
    public Map<ConsentType, TermsResponse> getRequiredTerms() {
        log.debug("필수 동의 약관 조회 시작");

        List<ConsentType> requiredTypes = Arrays.asList(ConsentType.getRequiredConsents());
        Map<ConsentType, TermsResponse> allTerms = getAllActiveTerms();

        Map<ConsentType, TermsResponse> requiredTerms = allTerms.entrySet().stream()
                .filter(entry -> requiredTypes.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        log.info("필수 동의 약관 조회 완료 - 필수 약관 수: {}", requiredTerms.size());
        return requiredTerms;
    }

    /**
     * 선택 동의 약관 목록 조회
     *
     * <p>
     * AI 개인화 서비스를 위한 선택 동의 약관들을 조회합니다.
     * 사용자가 원하는 개인화 수준을 선택할 수 있도록 정보를 제공합니다.
     * </p>
     *
     * @return 선택 동의 약관의 Map
     */
    public Map<ConsentType, TermsResponse> getOptionalTerms() {
        log.debug("선택 동의 약관 조회 시작");

        List<ConsentType> optionalTypes = Arrays.asList(ConsentType.getOptionalConsents());
        Map<ConsentType, TermsResponse> allTerms = getAllActiveTerms();

        Map<ConsentType, TermsResponse> optionalTerms = allTerms.entrySet().stream()
                .filter(entry -> optionalTypes.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        log.info("선택 동의 약관 조회 완료 - 선택 약관 수: {}", optionalTerms.size());
        return optionalTerms;
    }

    /**
     * 모든 활성 약관 요약 목록 조회
     *
     * <p>
     * 온보딩 페이지의 체크박스 목록 구성이나 마이페이지의 약관 목록 표시에 사용됩니다.
     * 동일 ConsentType의 활성 약관이 여러 개 있을 경우 최신 1건만 반환하여 운영 안전성을 보장합니다.
     * 응답 순서는 ConsentType 이름순으로 고정하여 UI 일관성을 확보합니다.
     * </p>
     *
     * @return 모든 활성 약관의 요약 정보 목록 (중복 제거, 정렬 보장)
     */
    @Transactional(readOnly = true)
    public List<TermsSummaryResponse> getAllActiveTermsSummary() {
        log.debug("모든 활성 약관 요약 목록 조회 시작");

        // 활성 약관 전부 조회
        List<Terms> activeTerms = termsRepository.findByActiveTrueOrderByConsentType();

        // 동일 ConsentType 중복이 있으면 createdAt 기준 최신 1건만 선별
        Map<ConsentType, Terms> latestByType = activeTerms.stream()
                .collect(Collectors.toMap(
                        Terms::getConsentType,
                        Function.identity(),
                        // 충돌 시 최신 것으로 선택 (createdAt 비교)
                        (existing, replacement) ->
                                existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement,
                        // EnumMap 사용으로 성능 최적화
                        () -> new EnumMap<>(ConsentType.class)
                ));

        // ConsentType 이름순 정렬 후 요약 DTO 변환
        List<TermsSummaryResponse> summaryList = latestByType.values().stream()
                .sorted(Comparator.comparing(terms -> terms.getConsentType().name()))
                .map(TermsSummaryResponse::from)
                .collect(Collectors.toList());

        log.info("활성 약관 요약 목록 조회 완료 - 조회된 약관 수: {}, 중복 제거 적용", summaryList.size());
        return summaryList;
    }

    // ===== 관리자 기능 (향후 구현) =====

    /**
     * 새로운 약관 버전 등록
     *
     * <p>
     * 법무팀에서 약관을 수정할 때 사용됩니다.
     * 기존 활성 약관을 비활성화하고 새로운 버전을 활성화합니다.
     * </p>
     *
     * @param consentType 약관 타입
     * @param title       새로운 제목
     * @param content     새로운 내용
     * @param version     새로운 버전
     * @return 등록된 약관 정보
     */
    @Transactional
    public TermsResponse createNewTermsVersion(ConsentType consentType, String title, String content, String version) {
        log.info("새로운 약관 버전 등록 시작 - 동의 타입: {}, 버전: {}", consentType, version);

        // 기존 활성 약관 비활성화
        int deactivatedCount = termsRepository.deactivateByConsentType(consentType);
        log.debug("기존 약관 비활성화 완료 - 비활성화된 약관 수: {}", deactivatedCount);

        // 새로운 약관 등록
        Terms newTerms = Terms.createTerms(consentType, title, content, version);
        Terms savedTerms = termsRepository.save(newTerms);

        log.info("새로운 약관 버전 등록 완료 - ID: {}, 버전: {}", savedTerms.getId(), version);
        return convertToResponse(savedTerms);
    }

    /**
     * Terms 엔티티를 TermsResponse DTO로 변환
     */
    private TermsResponse convertToResponse(Terms terms) {
        return new TermsResponse(
                terms.getConsentType(),
                terms.getTitle(),
                terms.getContent(),
                terms.getVersion(),
                terms.getCreatedAt() // NOTE: updatedAt 필드가 없다면 createdAt을 lastUpdated로 사용
        );
    }
}