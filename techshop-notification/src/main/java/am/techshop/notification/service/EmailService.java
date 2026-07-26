package am.techshop.notification.service;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.notification.email.EmailTemplates;
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
                                        PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan) {
        boolean justCreated = status == OrderStatus.PENDING;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(justCreated
                ? "Your order has been received — TechShopArmenia"
                : "Order #%s status update — TechShopArmenia".formatted(orderId));
        message.setText(
                """
                Hello %s,

                %s%sTotal amount: %s AMD

                Thank you for shopping at TechShopArmenia!""".formatted(
                                userName,
                                justCreated
                                        ? "Your order #%s has been successfully received.\n".formatted(orderId)
                                        : "Your order #%s status is now: %s.\n".formatted(orderId, status),
                                paymentMethodSummary(paymentMethod, installmentPlan),
                                totalPrice)
        );
        send(message);
    }

    private String paymentMethodSummary(PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan) {
        if (paymentMethod == null) {
            return "";
        }
        if (paymentMethod == PaymentMethod.INSTALLMENT && installmentPlan != null) {
            return "Դուք ընտրել եք ապառիկ վճարում %s-ի միջոցով, %d ամսով, ամսական %s դրամ\n"
                    .formatted(installmentPlan.bankName(), installmentPlan.durationMonths(), installmentPlan.monthlyPayment());
        }
        return "Վճարման եղանակ՝ %s\n".formatted(paymentMethodDisplayName(paymentMethod));
    }

    private String paymentMethodDisplayName(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case IDRAM -> "Idram";
            case TELCELL -> "Telcell";
            case ROKET_LINE -> "Roket Line";
            case INSTALLMENT -> "ապառիկ վճարում";
        };
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
