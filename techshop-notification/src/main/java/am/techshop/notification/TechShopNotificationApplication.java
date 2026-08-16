package am.techshop.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "am.techshop")
@EnableFeignClients
public class TechShopNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopNotificationApplication.class, args);
    }
}