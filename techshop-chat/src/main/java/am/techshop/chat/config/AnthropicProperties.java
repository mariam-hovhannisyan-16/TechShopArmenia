package am.techshop.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(String apiKey, @DefaultValue("claude-opus-4-8") String model) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
