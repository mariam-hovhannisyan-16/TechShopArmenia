package am.techshop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TechShopOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopOrderApplication.class, args);
    }
}