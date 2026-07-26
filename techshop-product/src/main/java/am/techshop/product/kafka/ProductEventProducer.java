package am.techshop.product.kafka;

import am.techshop.common.event.PriceDropEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, PriceDropEvent> kafkaTemplate;

    public void sendPriceDropEvent(PriceDropEvent event) {
        kafkaTemplate.send("price-drop", event);
    }
}
