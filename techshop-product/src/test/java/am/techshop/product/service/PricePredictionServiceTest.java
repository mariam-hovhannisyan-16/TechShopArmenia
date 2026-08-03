package am.techshop.product.service;

import am.techshop.common.dto.response.PricePredictionResponse;
import am.techshop.product.config.AnthropicProperties;
import am.techshop.product.entity.PriceHistory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PricePredictionServiceTest {

    private static final Long PRODUCT_ID = 1L;

    private List<PriceHistory> historyOf(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new PriceHistory((long) i, PRODUCT_ID,
                        BigDecimal.valueOf(100 - i), BigDecimal.valueOf(99 - i),
                        LocalDateTime.now().minusDays(count - i)))
                .toList();
    }

    @Test
    void isEnabled_WhenApiKeyNotConfigured_ReturnsFalse() {
        PricePredictionService service = new PricePredictionService(new AnthropicProperties(null, "claude-opus-4-8"));

        assertFalse(service.isEnabled());
    }

    @Test
    void isEnabled_WhenApiKeyConfigured_ReturnsTrue() {
        PricePredictionService service = new PricePredictionService(new AnthropicProperties("test-key", "claude-opus-4-8"));

        assertTrue(service.isEnabled());
    }

    @Test
    void getPrediction_WhenFewerThanThreeRecords_ReturnsInsufficientHistoryWithoutCallingAi() {
        PricePredictionService service = spy(new PricePredictionService(new AnthropicProperties("test-key", "claude-opus-4-8")));

        PricePredictionResponse result = service.getPrediction(PRODUCT_ID, historyOf(2));

        assertNull(result.prediction());
        assertEquals("insufficient_history", result.reason());
        verify(service, never()).generatePrediction(anyList());
    }

    @Test
    void getPrediction_WhenNotConfigured_ReturnsPredictionUnavailable() {
        PricePredictionService service = new PricePredictionService(new AnthropicProperties(null, "claude-opus-4-8"));

        PricePredictionResponse result = service.getPrediction(PRODUCT_ID, historyOf(3));

        assertNull(result.prediction());
        assertEquals("prediction_unavailable", result.reason());
    }

    @Test
    void getPrediction_WhenAiCallFails_ReturnsPredictionUnavailable() {
        PricePredictionService service = spy(new PricePredictionService(new AnthropicProperties("test-key", "claude-opus-4-8")));
        doReturn(Optional.empty()).when(service).generatePrediction(anyList());

        PricePredictionResponse result = service.getPrediction(PRODUCT_ID, historyOf(3));

        assertNull(result.prediction());
        assertEquals("prediction_unavailable", result.reason());
    }

    @Test
    void getPrediction_OnSuccess_ReturnsThePredictionText() {
        PricePredictionService service = spy(new PricePredictionService(new AnthropicProperties("test-key", "claude-opus-4-8")));
        doReturn(Optional.of("Գինը հավանաբար կնվազի ~5%-ով հաջորդ շաբաթ")).when(service).generatePrediction(anyList());

        PricePredictionResponse result = service.getPrediction(PRODUCT_ID, historyOf(3));

        assertEquals("Գինը հավանաբար կնվազի ~5%-ով հաջորդ շաբաթ", result.prediction());
        assertNull(result.reason());
        assertNotNull(result.generatedAt());
    }

    @Test
    void getPrediction_CachesResultAndDoesNotCallAiAgainWithinTtl() {
        PricePredictionService service = spy(new PricePredictionService(new AnthropicProperties("test-key", "claude-opus-4-8")));
        doReturn(Optional.of("Գինը կայուն է")).when(service).generatePrediction(anyList());
        List<PriceHistory> history = historyOf(3);

        PricePredictionResponse first = service.getPrediction(PRODUCT_ID, history);
        PricePredictionResponse second = service.getPrediction(PRODUCT_ID, history);

        assertEquals(first, second);
        verify(service, times(1)).generatePrediction(anyList());
    }

    @Test
    void getPrediction_ForDifferentProducts_CachesIndependently() {
        PricePredictionService service = spy(new PricePredictionService(new AnthropicProperties("test-key", "claude-opus-4-8")));
        doReturn(Optional.of("prediction")).when(service).generatePrediction(anyList());

        service.getPrediction(1L, historyOf(3));
        service.getPrediction(2L, historyOf(3));

        verify(service, times(2)).generatePrediction(anyList());
    }
}
