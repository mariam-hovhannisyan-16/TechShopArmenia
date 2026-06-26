package am.techshop.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

    private PriceCalculator() {}

    public static BigDecimal calculateTotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateDiscount(BigDecimal price, double discountPercent) {
        BigDecimal discount = price.multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateVat(BigDecimal price, double vatPercent) {
        return price.multiply(BigDecimal.valueOf(vatPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}