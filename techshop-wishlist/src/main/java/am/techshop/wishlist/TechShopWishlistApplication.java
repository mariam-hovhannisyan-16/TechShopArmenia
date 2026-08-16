package am.techshop.wishlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "am.techshop")
@EnableFeignClients
public class TechShopWishlistApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopWishlistApplication.class, args);
    }
}
