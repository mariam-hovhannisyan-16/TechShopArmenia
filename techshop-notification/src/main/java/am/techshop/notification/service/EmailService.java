package am.techshop.notification.service;

import am.techshop.common.enums.OrderStatus;
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
        String text = "Բարև, " + userName + ":\n\n" +
                "Շնորհակալություն TechShop AM-ում գրանցվելու համար: Ձեր հաշիվը ակտիվացնելու համար խնդրում ենք հետևել այս հղմանը.\n\n" +
                link + "\n\n" +
                "Հղումն ուժի մեջ է 24 ժամ:\n\n" +
                "Եթե դուք չեք գրանցվել TechShop AM-ում, պարզապես անտեսեք այս նամակը:";

        sendHtml(to, "Հաստատեք ձեր հաշիվը TechShop AM-ում", html, text);
    }

    public void sendPasswordResetEmail(String to, String userName, String resetToken) {
        String link = frontendUrl + "/reset-password?token=" + resetToken;
        String html = EmailTemplates.passwordResetEmail(userName, link);
        String text = "Բարև, " + userName + ":\n\n" +
                "Մենք ստացել ենք ձեր հաշվի գաղտնաբառը վերականգնելու հայտը: Նոր գաղտնաբառ սահմանելու համար հետևեք այս հղմանը.\n\n" +
                link + "\n\n" +
                "Հղումն ուժի մեջ է 1 ժամ:\n\n" +
                "Եթե դուք չեք հայցել գաղտնաբառի վերականգնում, պարզապես անտեսեք այս նամակը:";

        sendHtml(to, "Գաղտնաբառի վերականգնում TechShop AM-ում", html, text);
    }

    public void sendWelcome(String to, String userName) {
        String html = EmailTemplates.welcomeEmail(userName, frontendUrl);
        String text = "Բարև, " + userName + ":\n\n" +
                "Ձեր հաշիվը հաստատված է, և դուք այժմ կարող եք օգտվել TechShop AM-ի բոլոր հնարավորություններից: Բարի գնումներ:";

        sendHtml(to, "Բարի գալուստ TechShop AM", html, text);
    }

    public void sendOrderStatusChanged(String to, String userName, Long orderId, OrderStatus status, BigDecimal totalPrice) {
        boolean justCreated = status == OrderStatus.PENDING;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(justCreated
                ? "Your order has been received — TechShopArmenia"
                : "Order #" + orderId + " status update — TechShopArmenia");
        message.setText(
                "Hello " + userName + ",\n\n" +
                        (justCreated
                                ? "Your order #" + orderId + " has been successfully received.\n"
                                : "Your order #" + orderId + " status is now: " + status + ".\n") +
                        "Total amount: " + totalPrice + " AMD\n\n" +
                        "Thank you for shopping at TechShopArmenia!"
        );
        send(message);
    }

    private void send(SimpleMailMessage message) {
        if (isDevMode()) {
            logDevModeSend(String.join(", ", message.getTo()), message.getSubject(), message.getText());
            return;
        }

        try {
            // Without an explicit From, JavaMail falls back to deriving one from
            // the container's local hostname (e.g. "root@3f8a2b91c4d2") instead of
            // the authenticated account — Gmail then silently drops or spam-filters
            // the message rather than bouncing it, since the SMTP transaction
            // itself still completes "successfully" from JavaMail's point of view.
            message.setFrom(mailUsername);
            mailSender.send(message);
            log.info("[EmailService] Sent email to {} (subject: {})", message.getTo(), message.getSubject());
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", message.getTo(), ex.getMessage(), ex);
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
            log.info("[EmailService] Sent email to {} (subject: {})", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    private void logDevModeSend(String to, String subject, String body) {
        log.info("[EmailService] (dev mode, no SMTP configured) Not sending email — printing instead:\n" +
                "  To:      {}\n  Subject: {}\n  ---\n{}\n  ---", to, subject, body);
    }

    private boolean isDevMode() {
        return mailUsername == null || mailUsername.isBlank();
    }
}
