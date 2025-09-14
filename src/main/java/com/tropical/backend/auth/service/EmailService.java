package com.tropical.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스
 *
 * <p>
 * Spring Boot Mail을 사용하여 이메일 인증, 알림 등의 이메일을 발송하는 서비스입니다.
 * Gmail SMTP를 통해 이메일을 발송하며, 향후 HTML 템플릿이나 다른 이메일 제공자로 확장 가능합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>이메일 인증 메일 발송</li>
 *   <li>비밀번호 재설정 메일 발송 (향후 구현)</li>
 *   <li>알림 메일 발송 (향후 구현)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Tropical}")
    private String appName;

    @Value("${app.name:Aicemelt}")
    private String teamName;

    /**
     * 이메일 인증 메일 발송
     *
     * <p>
     * 회원가입 시 또는 이메일 인증 재발송 시 호출되는 메서드입니다.
     * 사용자가 클릭할 수 있는 인증 링크가 포함된 메일을 발송합니다.
     * </p>
     *
     * @param toEmail   수신자 이메일 주소
     * @param verifyUrl 인증 URL (토큰 포함)
     * @throws RuntimeException 이메일 발송 실패 시
     */
    public void sendVerificationMail(String toEmail, String verifyUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(String.format("[%s] 이메일 인증을 완료해주세요", appName));

            String content = buildVerificationEmailContent(verifyUrl);
            message.setText(content);

            mailSender.send(message);

            log.info("이메일 인증 메일 발송 완료 - 수신자: {}", toEmail);

        } catch (Exception e) {
            log.error("이메일 인증 메일 발송 실패 - 수신자: {}, 오류: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * 비밀번호 재설정 메일 발송
     *
     * <p>
     * 비밀번호를 잊었을 때 재설정 링크가 포함된 메일을 발송합니다.
     * 향후 비밀번호 재설정 기능 구현 시 사용될 예정입니다.
     * </p>
     *
     * @param toEmail  수신자 이메일 주소
     * @param resetUrl 비밀번호 재설정 URL (토큰 포함)
     * @throws RuntimeException 이메일 발송 실패 시
     */
    public void sendPasswordResetMail(String toEmail, String resetUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(String.format("[%s] 비밀번호 재설정 요청", appName));

            String content = buildPasswordResetEmailContent(resetUrl);
            message.setText(content);

            mailSender.send(message);

            log.info("비밀번호 재설정 메일 발송 완료 - 수신자: {}", toEmail);

        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패 - 수신자: {}, 오류: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * 이메일 인증 메일 내용 구성
     *
     * <p>
     * 텍스트 형식의 이메일 내용을 구성합니다.
     * 향후 HTML 템플릿으로 업그레이드할 수 있습니다.
     * </p>
     *
     * @param verifyUrl 인증 URL
     * @return 이메일 내용
     */
    private String buildVerificationEmailContent(String verifyUrl) {
        return String.format("""
                안녕하세요!
                
                %s 서비스에 가입해주셔서 감사합니다.
                
                이메일 인증을 완료하기 위해 아래 링크를 클릭해주세요:
                
                %s
                
                ※ 이 링크는 30분 후에 만료됩니다.
                ※ 본인이 요청하지 않은 경우 이 메일을 무시해주세요.
                
                감사합니다.
                %s 팀
                """, appName, verifyUrl, teamName);
    }

    /**
     * 비밀번호 재설정 메일 내용 구성
     *
     * <p>
     * 비밀번호 재설정을 위한 텍스트 형식의 이메일 내용을 구성합니다.
     * </p>
     *
     * @param resetUrl 비밀번호 재설정 URL
     * @return 이메일 내용
     */
    private String buildPasswordResetEmailContent(String resetUrl) {
        return String.format("""
                안녕하세요!
                
                %s 계정의 비밀번호 재설정 요청을 받았습니다.
                
                비밀번호를 재설정하려면 아래 링크를 클릭해주세요:
                
                %s
                
                ※ 이 링크는 1시간 후에 만료됩니다.
                ※ 본인이 요청하지 않은 경우 이 메일을 무시해주세요.
                
                감사합니다.
                %s 팀
                """, appName, resetUrl, appName);
    }

    /**
     * 이메일 발송 테스트
     *
     * <p>
     * 개발 환경에서 이메일 설정이 올바른지 확인하기 위한 테스트 메서드입니다.
     * </p>
     *
     * @param toEmail 테스트 이메일 수신자
     * @return 발송 성공 여부
     */
    public boolean sendTestEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(String.format("[%s] 이메일 발송 테스트", appName));
            message.setText("이메일 설정이 정상적으로 작동합니다!");

            mailSender.send(message);

            log.info("테스트 이메일 발송 완료 - 수신자: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("테스트 이메일 발송 실패 - 수신자: {}, 오류: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}