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
        message.setSubject("Your order has been received — TechShopArmenia");
        message.setText(
                "Hello " + userName + ",\n\n" +
                        "Your order #" + orderId + " has been successfully received.\n" +
                        "Total amount: " + totalPrice + " AMD\n\n" +
                        "Thank you for shopping at TechShopArmenia!"
        );
        mailSender.send(message);
    }

    public void sendWelcome(String to, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to TechShopArmenia");
        message.setText(
                "Hello " + userName + ",\n\n" +
                        "You have successfully registered at TechShopArmenia.\n" +
                        "Happy shopping!"
        );
        mailSender.send(message);
    }
}