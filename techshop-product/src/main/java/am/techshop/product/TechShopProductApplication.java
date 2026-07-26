package am.techshop.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TechShopProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopProductApplication.class, args);
    }
}