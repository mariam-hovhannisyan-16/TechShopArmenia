package am.techshop.notification.service;

import am.techshop.notification.email.EmailTemplates;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    public void sendWelcome(String to, String userName) {
        String html = EmailTemplates.welcomeEmail(userName, frontendUrl);
        String text = "Բարև, " + userName + ":\n\n" +
                "Ձեր հաշիվը հաստատված է, և դուք այժմ կարող եք օգտվել TechShop AM-ի բոլոր հնարավորություններից: Բարի գնումներ:";

        sendHtml(to, "Բարի գալուստ TechShop AM", html, text);
    }

    public void sendOrderCreated(String to, String userName, Long orderId, String totalPrice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your order has been received — TechShopArmenia");
        message.setText(
                "Hello " + userName + ",\n\n" +
                        "Your order #" + orderId + " has been successfully received.\n" +
                        "Total amount: " + totalPrice + " AMD\n\n" +
                        "Thank you for shopping at TechShopArmenia!"
        );
        send(message);
    }

    // No SMTP credentials configured (local dev without a .env) — log the
    // email instead of attempting to send, so registration/verification
    // still works end-to-end with nothing configured. A real send failure
    // (bad creds, SMTP down) is also just logged, never rethrown: mail
    // delivery must never take down the caller (a Kafka listener here).
    private void send(SimpleMailMessage message) {
        if (isDevMode()) {
            log.info("[EmailService] (dev mode, no SMTP configured) Not sending email — printing instead:\n" +
                    "  To:      {}\n  Subject: {}\n  ---\n{}\n  ---", message.getTo(), message.getSubject(), message.getText());
            return;
        }

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send email to {}: {}", message.getTo(), ex.getMessage(), ex);
        }
    }

    // Sends a multipart/alternative message (plain text + branded HTML) so
    // clients that can't or won't render HTML still show something
    // readable. Same dev-mode/non-blocking-failure behavior as send(),
    // just logging the plain-text fallback instead of the HTML.
    private void sendHtml(String to, String subject, String html, String textFallback) {
        if (isDevMode()) {
            log.info("[EmailService] (dev mode, no SMTP configured) Not sending email — printing instead:\n" +
                    "  To:      {}\n  Subject: {}\n  ---\n{}\n  ---", to, subject, textFallback);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textFallback, html);
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    private boolean isDevMode() {
        return mailUsername == null || mailUsername.isBlank();
    }
}
