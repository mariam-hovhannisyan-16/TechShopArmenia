package am.techshop.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "am.techshop")
@ConfigurationPropertiesScan
public class TechShopUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopUserApplication.class, args);
    }
}