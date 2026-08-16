package am.techshop.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "am.techshop")
@EnableFeignClients
public class TechShopCartApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopCartApplication.class, args);
    }
}