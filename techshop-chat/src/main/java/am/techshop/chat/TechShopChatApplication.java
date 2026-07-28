package am.techshop.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TechShopChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopChatApplication.class, args);
    }
}
