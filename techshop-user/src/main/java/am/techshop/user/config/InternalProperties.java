package am.techshop.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "internal")
public record InternalProperties(
        @DefaultValue("false") boolean requireEmailVerification) {
}
