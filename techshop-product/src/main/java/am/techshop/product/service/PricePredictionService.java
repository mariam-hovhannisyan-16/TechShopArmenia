package am.techshop.product.service;

import am.techshop.common.dto.response.PricePredictionResponse;
import am.techshop.product.config.AnthropicProperties;
import am.techshop.product.entity.PriceHistory;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PricePredictionService {

    public static final int MIN_HISTORY_RECORDS = 3;

    private static final Duration CACHE_TTL = Duration.ofHours(4);
    private static final int MAX_PREDICTION_LENGTH = 500;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String SYSTEM_PROMPT = """
            You are a pricing trend analyst for TechShop AM, an online electronics store in Armenia.
            You will be given a chronological list of price changes for one product, in Armenian Dram (֏).

            Based only on the actual trend in this data - consistently rising, consistently falling,
            volatile, or stable - write a single short prediction sentence in Armenian about whether the
            price is likely to rise, fall, or stay stable soon. Include an approximate percentage only if
            the trend clearly supports one. Do not invent a trend the data doesn't support, and do not say
            anything besides the prediction itself - no greetings, no explanations, no disclaimers. One
            short sentence only.
            """;

    private final AnthropicProperties properties;
    private final AnthropicClient client;
    private final Map<Long, CachedPrediction> cache = new ConcurrentHashMap<>();

    public PricePredictionService(AnthropicProperties properties) {
        this.properties = properties;
        this.client = properties.isConfigured()
                ? AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build()
                : null;
    }

    public boolean isEnabled() {
        return client != null;
    }

    public PricePredictionResponse getPrediction(Long productId, List<PriceHistory> history) {
        if (history.size() < MIN_HISTORY_RECORDS) {
            return new PricePredictionResponse(null, "insufficient_history", null);
        }

        CachedPrediction cached = cache.get(productId);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.response();
        }

        if (!isEnabled()) {
            log.info("[PricePredictionService] (no Anthropic API key configured) Skipping price prediction");
            return cacheAndReturn(productId, new PricePredictionResponse(null, "prediction_unavailable", null));
        }

        PricePredictionResponse response = generatePrediction(history)
                .map(text -> new PricePredictionResponse(text, null, LocalDateTime.now()))
                .orElse(new PricePredictionResponse(null, "prediction_unavailable", null));

        return cacheAndReturn(productId, response);
    }

    Optional<String> generatePrediction(List<PriceHistory> history) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(properties.model())
                    .maxTokens(200L)
                    .system(SYSTEM_PROMPT)
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                    .messages(List.of(MessageParam.builder()
                            .role(MessageParam.Role.USER)
                            .content(buildHistoryPrompt(history))
                            .build()))
                    .build();

            Message response = client.messages().create(params);

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(block -> block.text())
                    .findFirst()
                    .orElse(null);

            return Optional.ofNullable(text)
                    .filter(t -> !t.isBlank())
                    .map(t -> t.length() > MAX_PREDICTION_LENGTH ? t.substring(0, MAX_PREDICTION_LENGTH) : t);
        } catch (Exception ex) {
            log.error("[PricePredictionService] Failed to generate price prediction: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private String buildHistoryPrompt(List<PriceHistory> history) {
        StringBuilder sb = new StringBuilder("Price history for this product, oldest to newest (Armenian Dram, ֏):\n");
        for (PriceHistory entry : history) {
            sb.append(entry.getChangedAt().format(DATE_FORMAT))
                    .append(": ").append(entry.getOldPrice())
                    .append(" ֏ -> ").append(entry.getNewPrice()).append(" ֏\n");
        }
        return sb.toString();
    }

    private PricePredictionResponse cacheAndReturn(Long productId, PricePredictionResponse response) {
        cache.put(productId, new CachedPrediction(response, Instant.now().plus(CACHE_TTL)));
        return response;
    }

    private record CachedPrediction(PricePredictionResponse response, Instant expiresAt) {}
}
