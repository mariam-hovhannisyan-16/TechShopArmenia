package am.techshop.notification.service;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.notification.email.EmailTemplates;
import am.techshop.notification.i18n.OrderMessages;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(String to, String userName, String verificationToken) {
        String link = frontendUrl + "/verify-email?token=" + verificationToken;
        String html = EmailTemplates.verificationEmail(userName, link);
        String text = """
                Բարև, %s:

                Շնորհակալություն TechShop AM-ում գրանցվելու համար: Ձեր հաշիվը ակտիվացնելու համար խնդրում ենք հետևել այս հղմանը.

                %s

                Հղումն ուժի մեջ է 24 ժամ:

                Եթե դուք չեք գրանցվել TechShop AM-ում, պարզապես անտեսեք այս նամակը:""".formatted(userName, link);

        sendHtml(to, "Հաստատեք ձեր հաշիվը TechShop AM-ում", html, text);
    }

    public void sendPasswordResetEmail(String to, String userName, String resetToken) {
        String link = frontendUrl + "/reset-password?token=" + resetToken;
        String html = EmailTemplates.passwordResetEmail(userName, link);
        String text = """
                Բարև, %s:

                Մենք ստացել ենք ձեր հաշվի գաղտնաբառը վերականգնելու հայտը: Նոր գաղտնաբառ սահմանելու համար հետևեք այս հղմանը.

                %s

                Հղումն ուժի մեջ է 1 ժամ:

                Եթե դուք չեք հայցել գաղտնաբառի վերականգնում, պարզապես անտեսեք այս նամակը:""".formatted(userName, link);

        sendHtml(to, "Գաղտնաբառի վերականգնում TechShop AM-ում", html, text);
    }

    public void sendWelcome(String to, String userName) {
        String html = EmailTemplates.welcomeEmail(userName, frontendUrl);
        String text = """
                Բարև, %s:

                Ձեր հաշիվը հաստատված է, և դուք այժմ կարող եք օգտվել TechShop AM-ի բոլոր հնարավորություններից: Բարի գնումներ:""".formatted(userName);

        sendHtml(to, "Բարի գալուստ TechShop AM", html, text);
    }

    public void sendOrderStatusChanged(String to, String userName, Long orderId, OrderStatus status, BigDecimal totalPrice,
                                        PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan, Language language) {
        boolean justCreated = status == OrderStatus.PENDING;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(justCreated
                ? OrderMessages.emailSubjectCreated(language)
                : OrderMessages.emailSubjectStatusUpdate(orderId, language));
        message.setText(
                """
                %s

                %s%s%s

                %s""".formatted(
                                OrderMessages.emailGreeting(userName, language),
                                justCreated
                                        ? OrderMessages.emailBodyCreated(orderId, language)
                                        : OrderMessages.emailBodyStatusUpdate(orderId, status, language),
                                paymentMethodSummary(paymentMethod, installmentPlan, language),
                                OrderMessages.emailTotalLine(totalPrice, language),
                                OrderMessages.emailThanks(language))
        );
        send(message);
    }

    private String paymentMethodSummary(PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan, Language language) {
        if (paymentMethod == null) {
            return "";
        }
        if (paymentMethod == PaymentMethod.INSTALLMENT && installmentPlan != null) {
            return OrderMessages.emailInstallmentLine(
                    installmentPlan.bankName(), installmentPlan.durationMonths(), installmentPlan.monthlyPayment(), language);
        }
        return OrderMessages.emailPaymentMethodLine(paymentMethod, language);
    }

    private void send(SimpleMailMessage message) {
        String to = recipients(message);
        if (isDevMode()) {
            logDevModeSend(to, message.getSubject(), message.getText());
            return;
        }

        try {
            message.setFrom(mailUsername);
            mailSender.send(message);
            logSendSuccess(to, message.getSubject());
        } catch (Exception ex) {
            logSendFailure(to, ex);
        }
    }

    private void sendHtml(String to, String subject, String html, String textFallback) {
        if (isDevMode()) {
            logDevModeSend(to, subject, textFallback);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textFallback, html);
            mailSender.send(mimeMessage);
            logSendSuccess(to, subject);
        } catch (Exception ex) {
            logSendFailure(to, ex);
        }
    }

    private static String recipients(SimpleMailMessage message) {
        return String.join(", ", Objects.requireNonNullElse(message.getTo(), new String[0]));
    }

    private void logSendSuccess(String to, String subject) {
        log.info("[EmailService] Sent email to {} (subject: {})", to, subject);
    }

    private void logSendFailure(String to, Exception ex) {
        log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
    }

    private void logDevModeSend(String to, String subject, String body) {
        log.info("""
                [EmailService] (dev mode, no SMTP configured) Not sending email — printing instead:
                  To:      {}
                  Subject: {}
                  ---
                {}
                  ---""", to, subject, body);
    }

    private boolean isDevMode() {
        return mailUsername == null || mailUsername.isBlank();
    }
}
