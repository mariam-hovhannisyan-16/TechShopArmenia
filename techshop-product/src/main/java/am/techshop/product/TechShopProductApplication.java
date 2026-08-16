package am.techshop.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "am.techshop")
@EnableFeignClients
@ConfigurationPropertiesScan
public class TechShopProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopProductApplication.class, args);
    }
}