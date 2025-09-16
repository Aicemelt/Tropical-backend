package com.tropical.backend.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 이메일 발송 서비스 (HTML 템플릿 지원)
 *
 * <p>
 * Spring Boot Mail을 사용하여 HTML 이메일 인증, 알림 등의 이메일을 발송하는 서비스입니다.
 * Gmail SMTP를 통해 이메일을 발송하며, HTML 템플릿과 로고를 포함한 디자인된 이메일을 제공합니다.
 * 열대 느낌의 주황-노랑-초록 색상을 사용합니다.
 * </p>
 *
 * @author 왕택준
 * @version 0.3
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

    @Value("${app.frontend.base-url:http://localhost:5005}")
    private String frontendBaseUrl;

    /**
     * 이메일 인증 메일 발송 (HTML 템플릿)
     *
     * @param toEmail   수신자 이메일 주소
     * @param verifyUrl 인증 URL (토큰 포함)
     * @throws RuntimeException 이메일 발송 실패 시
     */
    public void sendVerificationMail(String toEmail, String verifyUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] 이메일 인증을 완료해주세요", appName));

            String htmlContent = buildVerificationEmailHtml(verifyUrl);
            helper.setText(htmlContent, true); // true = HTML 모드

            // 배경 없는 로고 이미지 첨부
            try {
                helper.addInline("logo", new ClassPathResource("static/images/TropiCal_logo_no_bg.png"));
            } catch (Exception e) {
                log.warn("로고 이미지 첨부 실패, 로고 없이 이메일 발송: {}", e.getMessage());
            }

            mailSender.send(message);

            log.info("HTML 이메일 인증 메일 발송 완료 - 수신자: {}", toEmail);

        } catch (Exception e) {
            log.error("이메일 인증 메일 발송 실패 - 수신자: {}, 오류: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * HTML 이메일 인증 템플릿 생성 (열대 색상)
     *
     * @param verifyUrl 인증 URL
     * @return HTML 이메일 내용
     */
    private String buildVerificationEmailHtml(String verifyUrl) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s 이메일 인증</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #212121; background-color: #f5f5f5;">
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f5f5f5;">
                        <tr>
                            <td align="center" style="padding: 20px 0;">
                                <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                    <!-- 헤더 -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #FF6B35 0%%, #F7931E 40%%, #32D74B 100%%); padding: 40px 30px; text-align: center;">
                                            <img src="cid:logo" alt="%s 로고" style="width: 120px; height: auto; margin-bottom: 20px; display: block; margin-left: auto; margin-right: auto;">
                                            <h1 style="color: #ffffff; font-size: 28px; font-weight: 600; margin: 0;">이메일 인증</h1>
                                        </td>
                                    </tr>
                
                                    <!-- 콘텐츠 -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <div style="font-size: 18px; color: #212121; margin-bottom: 25px; font-weight: 500;">안녕하세요!</div>
                
                                            <div style="font-size: 16px; color: #616161; margin-bottom: 30px; line-height: 1.7;">
                                                <strong>%s</strong> 서비스에 가입해주셔서 감사합니다.<br>
                                                안전한 서비스 이용을 위해 이메일 인증을 완료해주세요.
                                            </div>
                
                                            <div style="text-align: center; margin: 30px 0;">
                                                <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #FF6B35 0%%, #FFD23F 50%%, #32D74B 100%%); color: #ffffff; padding: 16px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600;">이메일 인증하기</a>
                                            </div>
                
                                            <div style="background-color: #FFF8E7; border: 1px solid #FFD23F; border-left: 4px solid #32D74B; border-radius: 6px; padding: 15px; margin: 25px 0; font-size: 14px; color: #8B4513;">
                                                <strong>⚠️ 중요 안내</strong><br>
                                                • 이 링크는 30분 후에 만료됩니다.<br>
                                                • 본인이 요청하지 않은 경우 이 메일을 무시해주세요.<br>
                                                • 버튼이 작동하지 않으면 아래 링크를 복사해서 사용하세요:<br>
                                                <div style="word-break: break-all; margin-top: 10px; font-family: monospace; background: #f8f9fa; padding: 10px; border-radius: 4px;">
                                                    %s
                                                </div>
                                            </div>
                                        </td>
                                    </tr>
                
                                    <!-- 푸터 -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;">
                                            <p style="font-size: 14px; color: #616161; margin-bottom: 5px;">이 메일은 자동으로 발송된 메일입니다.</p>
                                            <p style="font-size: 14px; font-weight: 600; color: #212121; margin: 0;">Aicemelt 팀</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """, appName, appName, appName, verifyUrl, verifyUrl);
    }

    /**
     * 비밀번호 재설정 메일 발송 (HTML 템플릿)
     */
    public void sendPasswordResetMail(String toEmail, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] 비밀번호 재설정 요청", appName));

            String htmlContent = buildPasswordResetEmailHtml(resetUrl);
            helper.setText(htmlContent, true);

            // 배경 없는 로고 이미지 첨부
            try {
                helper.addInline("logo", new ClassPathResource("static/images/TropiCal_logo_no_bg.png"));
            } catch (Exception e) {
                log.warn("로고 이미지 첨부 실패, 로고 없이 이메일 발송: {}", e.getMessage());
            }

            mailSender.send(message);

            log.info("HTML 비밀번호 재설정 메일 발송 완료 - 수신자: {}", toEmail);

        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패 - 수신자: {}, 오류: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }
    }

    /**
     * HTML 비밀번호 재설정 템플릿 생성 (열대 색상)
     */
    private String buildPasswordResetEmailHtml(String resetUrl) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s 비밀번호 재설정</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #212121; background-color: #f5f5f5; }
                        .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #FF6B35 0%%, #F7931E 40%%, #32D74B 100%%); padding: 40px 30px; text-align: center; }
                        .logo { width: 120px; height: auto; margin-bottom: 20px; }
                        .header h1 { color: #ffffff; font-size: 28px; font-weight: 600; margin: 0; }
                        .content { padding: 40px 30px; }
                        .greeting { font-size: 18px; color: #212121; margin-bottom: 25px; font-weight: 500; }
                        .message { font-size: 16px; color: #616161; margin-bottom: 30px; line-height: 1.7; }
                        .verify-button { display: inline-block; background: linear-gradient(135deg, #FF6B35 0%%, #FFD23F 50%%, #32D74B 100%%); color: #ffffff; padding: 16px 32px; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; margin: 20px 0; box-shadow: 0 3px 12px rgba(255, 107, 53, 0.3); }
                        .warning { background-color: #FFF8E7; border: 1px solid #FFD23F; border-left: 4px solid #32D74B; border-radius: 6px; padding: 15px; margin: 25px 0; font-size: 14px; color: #8B4513; }
                        .footer { background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef; }
                        .footer p { font-size: 14px; color: #616161; margin-bottom: 5px; }
                        .team-name { font-weight: 600; color: #212121; }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="header">
                            <img src="cid:logo" alt="%s 로고" class="logo">
                            <h1>비밀번호 재설정</h1>
                        </div>
                        <div class="content">
                            <div class="greeting">안녕하세요!</div>
                            <div class="message">
                                <strong>%s</strong> 계정의 비밀번호 재설정 요청을 받았습니다.<br>
                                아래 버튼을 클릭하여 새로운 비밀번호를 설정해주세요.
                            </div>
                            <div style="text-align: center;">
                                <a href="%s" class="verify-button">비밀번호 재설정하기</a>
                            </div>
                            <div class="warning">
                                <strong>⚠️ 중요 안내</strong><br>
                                • 이 링크는 1시간 후에 만료됩니다.<br>
                                • 본인이 요청하지 않은 경우 이 메일을 무시해주세요.
                            </div>
                        </div>
                        <div class="footer">
                            <p>이 메일은 자동으로 발송된 메일입니다.</p>
                            <p class="team-name">Aicemelt 팀</p>
                        </div>
                    </div>
                </body>
                </html>
                """, appName, appName, appName, resetUrl);
    }

    /**
     * 테스트 이메일 발송 (HTML)
     */
    public boolean sendTestEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(String.format("[%s] 이메일 발송 테스트", appName));
            helper.setText("<h3>이메일 설정이 정상적으로 작동합니다!</h3><p>HTML 이메일 템플릿이 적용되었습니다.</p><p><strong>Aicemelt 팀</strong></p>", true);

            mailSender.send(message);

            log.info("HTML 테스트 이메일 발송 완료 - 수신자: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("테스트 이메일 발송 실패 - 수신자: {}, 오류: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}