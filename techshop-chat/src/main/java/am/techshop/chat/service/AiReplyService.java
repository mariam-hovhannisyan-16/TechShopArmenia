package am.techshop.chat.service;

import am.techshop.chat.config.AnthropicProperties;
import am.techshop.chat.entity.Message;
import am.techshop.common.enums.MessageSender;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AiReplyService {

    private static final String SYSTEM_PROMPT = """
            You are the customer support assistant for TechShop AM, an online electronics store in Armenia.

            Store facts:
            - Categories: smartphones, laptops, TVs, home appliances, audio, gaming, and accessories.
            - Delivery: within Yerevan in 1-2 business days, other regions of Armenia in 2-5 business days.
            - Returns: 14-day return policy from the delivery date, item must be in original condition.
            - Warranty: 2-year manufacturer warranty on all electronics sold through TechShop AM.
            - Payment methods: Idram, Telcell Wallet, Roket Line, and installment (ապառիկ) plans through partner banks.

            Reply in the same language the customer writes in (Armenian, Russian, or English), briefly and helpfully,
            in no more than a few short sentences.
            You do not have access to a specific customer's order, account, or payment records — if asked about the
            status of a specific order, a refund, or an account issue, tell the customer a human agent will help and
            suggest they request one. Never invent order numbers, prices, or delivery dates you don't know.
            """;

    private static final int MAX_REPLY_LENGTH = 2000;

    private final AnthropicProperties properties;
    private final AnthropicClient client;

    public AiReplyService(AnthropicProperties properties) {
        this.properties = properties;
        this.client = properties.isConfigured()
                ? AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build()
                : null;
    }

    public boolean isEnabled() {
        return client != null;
    }

    public Optional<String> generateReply(List<Message> history) {
        if (!isEnabled()) {
            log.info("[AiReplyService] (no Anthropic API key configured) Skipping bot auto-reply");
            return Optional.empty();
        }

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(properties.model())
                    .maxTokens(1024L)
                    .system(SYSTEM_PROMPT)
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                    .messages(toAnthropicMessages(history))
                    .build();

            com.anthropic.models.messages.Message response = client.messages().create(params);

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(block -> block.text())
                    .findFirst()
                    .orElse(null);

            return Optional.ofNullable(text)
                    .filter(t -> !t.isBlank())
                    .map(t -> t.length() > MAX_REPLY_LENGTH ? t.substring(0, MAX_REPLY_LENGTH) : t);
        } catch (Exception ex) {
            log.error("[AiReplyService] Failed to generate bot reply: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private List<MessageParam> toAnthropicMessages(List<Message> history) {
        List<MessageParam.Role> roles = new ArrayList<>();
        List<StringBuilder> texts = new ArrayList<>();
        for (Message message : history) {
            MessageParam.Role role = message.getSender() == MessageSender.CUSTOMER
                    ? MessageParam.Role.USER
                    : MessageParam.Role.ASSISTANT;

            if (!roles.isEmpty() && roles.get(roles.size() - 1) == role) {
                texts.get(texts.size() - 1).append('\n').append(message.getText());
            } else {
                roles.add(role);
                texts.add(new StringBuilder(message.getText()));
            }
        }

        List<MessageParam> messages = new ArrayList<>();
        for (int i = 0; i < roles.size(); i++) {
            messages.add(MessageParam.builder().role(roles.get(i)).content(texts.get(i).toString()).build());
        }
        return messages;
    }
}
