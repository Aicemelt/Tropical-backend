package com.tropical.backend.config;

import com.tropical.backend.auth.entity.Terms;
import com.tropical.backend.auth.entity.UserConsent.ConsentType;
import com.tropical.backend.auth.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 약관 데이터 초기화 컴포넌트
 *
 * <p>
 * 애플리케이션 시작 시 필요한 약관 데이터를 자동으로 생성합니다.
 * 이미 존재하는 약관은 중복 생성하지 않으며, 누락된 약관만 추가합니다.
 * ConsentType의 required 속성에 따라 제목에 [필수]/[선택] 접두사를 자동으로 추가합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.18
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TermsDataInitializer {

    private final TermsRepository termsRepository;

    /**
     * 애플리케이션 시작 완료 후 약관 데이터 초기화
     *
     * <p>
     * ApplicationReadyEvent를 사용하여 모든 빈이 완전히 초기화된 후 실행됩니다.
     * 기존 약관이 있는지 확인하고, 없는 경우에만 새로 생성합니다.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeTermsData() {
        log.info("약관 데이터 초기화 시작");

        // 필수 동의 약관 생성
        createTermsIfNotExists(ConsentType.TERMS_OF_SERVICE, "서비스 이용약관", getTermsOfServiceContent());
        createTermsIfNotExists(ConsentType.PRIVACY_POLICY, "개인정보처리방침", getPrivacyPolicyContent());
        createTermsIfNotExists(ConsentType.CALENDAR_PERSONALIZATION, "일정 기반 AI 추천 서비스 동의", getCalendarPersonalizationContent());

        // 선택 동의 약관 생성
        createTermsIfNotExists(ConsentType.DIARY_PERSONALIZATION, "일기 기반 AI 추천 서비스 동의", getDiaryPersonalizationContent());
        createTermsIfNotExists(ConsentType.TODO_PERSONALIZATION, "할일 기반 AI 추천 서비스 동의", getTodoPersonalizationContent());
        createTermsIfNotExists(ConsentType.BUCKET_PERSONALIZATION, "버킷리스트 기반 AI 추천 서비스 동의", getBucketPersonalizationContent());

        log.info("약관 데이터 초기화 완료");
    }

    /**
     * 약관이 존재하지 않는 경우에만 생성
     * ConsentType의 required 속성에 따라 제목에 [필수]/[선택] 접두사를 자동 추가
     */
    private void createTermsIfNotExists(ConsentType consentType, String title, String content) {
        boolean exists = termsRepository.findByConsentTypeAndActiveTrue(consentType).isPresent();

        if (!exists) {
            String finalTitle = addRequiredPrefix(consentType, title);
            Terms terms = Terms.createTerms(consentType, finalTitle, content, "1.0");
            termsRepository.save(terms);
            log.info("약관 생성 완료 - 타입: {}, 제목: {}", consentType, finalTitle);
        } else {
            log.debug("약관 이미 존재 - 타입: {}", consentType);
        }
    }

    /**
     * ConsentType의 required 속성에 따라 제목에 접두사 추가
     *
     * @param consentType 동의 타입
     * @param title       원본 제목
     * @return 접두사가 추가된 제목
     */
    private String addRequiredPrefix(ConsentType consentType, String title) {
        String prefix = consentType.isRequired() ? "[필수] " : "[선택] ";
        return prefix + title;
    }

    // ===== 약관 내용 정의 메서드들 =====

    private String getTermsOfServiceContent() {
        return """
                # 서비스 이용약관
                
                ## 제1조 (목적)
                본 약관은 Tropical(이하 "회사")이 제공하는 캘린더 서비스(이하 "서비스")의 이용조건 및 절차를 정함을 목적으로 합니다.
                
                ## 제2조 (정의)
                1) "서비스"란 회사가 제공하는 캘린더 및 그 부가 기능 일체를 말합니다.  
                2) "회원"이란 본 약관에 동의하고 회사와 이용계약을 체결한 자를 말합니다.
                
                ## 제3조 (약관의 효력 및 변경)
                1) 본 약관은 서비스 화면에 게시하거나 기타 방법으로 공지함으로써 효력이 발생합니다.  
                2) 회사는 관련 법령을 위반하지 않는 범위에서 약관을 변경할 수 있으며, 변경 시 사전 공지합니다.
                
                ## 제4조 (서비스 제공 및 변경)
                1) 회사는 연중무휴, 1일 24시간 서비스를 제공합니다. 단, 정기점검 등 불가피한 경우 서비스가 일시 중단될 수 있습니다.  
                2) 서비스의 내용, 운영상 또는 기술상 필요에 따라 변경·중단될 수 있으며, 이 경우 사전에 공지합니다.
                
                ## 제5조 (회원의 의무)
                1) 회원은 관계 법령, 약관, 운영정책 등을 준수하여야 합니다.  
                2) 타인의 개인정보 침해, 서비스의 안정적 운영을 방해하는 행위 등은 금지됩니다.
                
                ## 제6조 (계정 관리)
                계정 및 비밀번호 관리 책임은 회원에게 있으며, 분실·도용에 대한 주의의무를 다해야 합니다.
                
                ## 제7조 (손해배상 및 면책)
                1) 회사는 회사의 고의 또는 중대한 과실이 없는 한 서비스 이용과 관련하여 발생한 손해에 대하여 책임을 지지 않습니다.  
                2) 불가항력(천재지변, IDC 장애 등)에 의한 서비스 중단에 대해서도 면책됩니다.
                
                ## 제8조 (준거법 및 관할법원)
                본 약관과 서비스 이용에 관한 분쟁은 대한민국 법을 준거법으로 하며, 관할법원은 민사소송법에 따릅니다.
                """;
    }

    private String getPrivacyPolicyContent() {
        return """
                # 개인정보처리방침
                
                본 방침은 개인정보 보호법, 정보통신망 이용촉진 및 정보보호 등에 관한 법률 등 관계 법령을 준수합니다.
                
                ## 1. 처리 목적
                - 회원가입 및 본인확인, 서비스 제공/개선, 고객 상담, 맞춤형 추천 제공
                
                ## 2. 처리·보유 기간
                - 회원정보(이메일, 닉네임 등): 회원 탈퇴 시까지  
                - 서비스 이용기록/접속기록: 3년 보관 (관련 법령에 따라)  
                - 결제/거래 관련 기록: 5년 보관 (전자상거래법 등 관련 법령에 따라)
                
                ## 3. 수집 항목
                - 필수: 이메일, 닉네임, 비밀번호(로컬 가입 시)  
                - 선택: 프로필 정보, 알림/개인화 설정 값
                
                ## 4. 제3자 제공
                원칙적으로 제3자에게 제공하지 않습니다. 다만 다음의 경우 예외로 합니다.  
                - 정보주체의 동의가 있는 경우  
                - 법령에 근거한 요청이 있는 경우
                
                ## 5. 처리의 위탁
                서비스 운영을 위해 일부 업무를 위탁할 수 있습니다.  
                - 수탁업체: AWS(데이터 보관/호스팅)  
                - 위탁업무: 인프라 운영 및 데이터 보관  
                위탁계약 체결 시 개인정보 보호 관련 의무를 규정합니다.
                
                ## 6. 정보주체의 권리
                이용자는 언제든지 개인정보 열람·정정·삭제·처리정지 등을 요구할 수 있습니다.
                
                ## 7. 개인정보 파기
                보유기간 경과 또는 처리 목적 달성 시 지체 없이 파기합니다.
                
                ## 8. 개인정보 보호책임자
                - 성명/연락처: (추후 기재)  
                - 이메일: (추후 기재)
                
                ## 9. 고지의 의무
                본 방침이 변경되는 경우 서비스 공지사항 등을 통해 공지합니다.
                """;
    }

    private String getCalendarPersonalizationContent() {
        return """
                # 일정 기반 AI 추천 서비스 동의
                
                ## 서비스 소개
                일정 데이터(제목/시간/장소/참석자 등)와 패턴을 분석하여 개인화된 추천과 스몰토크를 제공합니다.
                
                ## 수집 항목 및 이용 목적
                - 수집 항목: 일정 제목/시간/장소/참석자, 카테고리/태그, 사용 패턴
                - 이용 목적: 개인화 추천 제공, 일정 관리 효율화, 서비스 품질 개선
                
                ## 제3자 제공
                - 원칙적으로 제공하지 않음  
                - 법령 근거 또는 이용자 별도 동의 시에만 제공
                
                ## 처리의 위탁
                - 수탁업체: AWS(인프라/데이터 보관)  
                - 위탁업무: 안정적 서비스 제공을 위한 인프라 운영
                
                ## 보유 및 이용 기간
                - 동의 철회 또는 회원 탈퇴 시 즉시 삭제  
                - 다만, 관련 법령에 따라 필요한 경우 해당 기간 보관
                
                ## 동의 거부권 및 불이익
                본 동의는 서비스의 핵심 기능 제공을 위한 필수 동의입니다. 미동의 시 서비스 이용이 불가능합니다.
                """;
    }

    private String getDiaryPersonalizationContent() {
        return """
                # 일기 기반 AI 추천 서비스 동의
                
                ## 서비스 소개
                일기 텍스트와 작성 패턴을 분석하여 감정 상태에 맞는 개인화 추천을 제공합니다.
                
                ## 수집 항목 및 이용 목적
                - 수집 항목: 일기 텍스트, 작성 시간/빈도, 감정 태그, 이미지 메타데이터(선택)
                - 이용 목적: 감정 상태 분석, 개인화 대화/활동 추천, 웰빙 지원
                
                ## 제3자 제공
                - 원칙적으로 제공하지 않음  
                - 법령 근거 또는 이용자 별도 동의 시에만 제공
                
                ## 처리의 위탁
                - 수탁업체: AWS(인프라/데이터 보관)  
                - 위탁업무: 안정적 서비스 제공을 위한 인프라 운영
                
                ## 보유 및 이용 기간
                - 동의 철회 또는 회원 탈퇴 시 즉시 삭제  
                - 단, 관계 법령상 의무 보관 대상 데이터는 해당 기간 보관
                
                ## 동의 거부권 및 불이익
                선택 동의입니다. 미동의해도 기본 서비스 이용은 가능합니다.
                """;
    }

    private String getTodoPersonalizationContent() {
        return """
                # 할일 기반 AI 추천 서비스 동의
                
                ## 서비스 소개
                투두리스트 패턴을 분석하여 생산성 향상을 위한 개인화 추천을 제공합니다.
                
                ## 수집 항목 및 이용 목적
                - 수집 항목: 할일 내용/카테고리, 완료 패턴, 소요시간, 우선순위, 마감일, 메모
                - 이용 목적: 업무 패턴 분석, 효율성 개선 제안, 개인화 조언 제공
                
                ## 제3자 제공
                - 원칙적으로 제공하지 않음  
                - 법령 근거 또는 이용자 별도 동의 시에만 제용
                
                ## 처리의 위탁
                - 수탁업체: AWS(인프라/데이터 보관)  
                - 위탁업무: 안정적 서비스 제공을 위한 인프라 운영
                
                ## 보유 및 이용 기간
                - 동의 철회 또는 회원 탈퇴 시 즉시 삭제  
                - 단, 관계 법령상 의무 보관 대상 데이터는 해당 기간 보관
                
                ## 동의 거부권 및 불이익
                선택 동의입니다. 미동의해도 기본 서비스 이용은 가능합니다.
                """;
    }

    private String getBucketPersonalizationContent() {
        return """
                # 버킷리스트 기반 AI 추천 서비스 동의
                
                ## 서비스 소개
                버킷리스트 데이터로 관심사/목표를 파악하고 개인화된 활동/경험을 추천합니다.
                
                ## 수집 항목 및 이용 목적
                - 수집 항목: 버킷 항목/카테고리, 계획/진척, 관심사, 메모/감정표현
                - 이용 목적: 목표 달성 로드맵, 관련 경험 추천, 동기부여 메시지
                
                ## 제3자 제공
                - 원칙적으로 제공하지 않음  
                - 법령 근거 또는 이용자 별도 동의 시에만 제공
                
                ## 처리의 위탁
                - 수탁업체: AWS(인프라/데이터 보관)  
                - 위탁업무: 안정적 서비스 제공을 위한 인프라 운영
                
                ## 보유 및 이용 기간
                - 동의 철회 또는 회원 탈퇴 시 즉시 삭제  
                - 단, 관계 법령상 의무 보관 대상 데이터는 해당 기간 보관
                
                ## 동의 거부권 및 불이익
                선택 동의입니다. 미동의해도 기본 서비스 이용은 가능합니다.
                """;
    }
}