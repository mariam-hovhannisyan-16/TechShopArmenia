package am.techshop.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "am.techshop")
public class TechShopNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopNotificationApplication.class, args);
    }
}