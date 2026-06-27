package am.techshop.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

    private PriceCalculator() {}

    public static BigDecimal calculateTotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}