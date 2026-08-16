package am.techshop.notification.i18n;

import am.techshop.common.enums.Language;

import java.math.BigDecimal;

public final class AccountMessages {

    private AccountMessages() {}

    public static String chatReplyMessage(String messagePreview, Language language) {
        return switch (language) {
            case HY -> "Ձեր աջակցության չաթում նոր պատասխան կա՝ %1$s".formatted(messagePreview);
            case EN -> "New reply in your support conversation: %1$s".formatted(messagePreview);
            case RU -> "Новый ответ в вашем чате поддержки: %1$s".formatted(messagePreview);
        };
    }

    public static String priceDropMessage(String productName, BigDecimal oldPrice, BigDecimal newPrice, Language language) {
        return switch (language) {
            case HY -> "%1$s-ի գինը իջել է. %2$s → %3$s"
                    .formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString());
            case EN -> "Price drop for %1$s: %2$s → %3$s"
                    .formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString());
            case RU -> "Цена на %1$s снизилась: %2$s → %3$s"
                    .formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString());
        };
    }
}
