package am.techshop.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOrderCreated(String to, String userName, Long orderId, String totalPrice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Ձեր պատվերը ստացվել է — TechShopArmenia");
        message.setText(
                "Բարև " + userName + ",\n\n" +
                        "Ձեր #" + orderId + " պատվերը հաջողությամբ ստացվել է։\n" +
                        "Ընդհանուր գումար՝ " + totalPrice + " AMD\n\n" +
                        "Շնորհակալություն TechShopArmenia-ում գնումների համար։"
        );
        mailSender.send(message);
    }

    public void sendWelcome(String to, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Բարի գալուստ TechShopArmenia");
        message.setText(
                "Բարև " + userName + ",\n\n" +
                        "Դուք հաջողությամբ գրանցվել եք TechShopArmenia-ում։\n" +
                        "Հաճելի գնումներ։"
        );
        mailSender.send(message);
    }
}