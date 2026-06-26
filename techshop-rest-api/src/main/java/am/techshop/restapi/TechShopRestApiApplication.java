package am.techshop.restapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = "am.techshop")
@EntityScan(basePackages = "am.techshop")
@EnableJpaRepositories(basePackages = "am.techshop")
public class TechShopRestApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechShopRestApiApplication.class, args);
    }
}