package am.techshop.notification.i18n;

import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class OrderMessages {

    private OrderMessages() {}

    private static final int MAX_LISTED_PRODUCTS = 2;

    private static final Map<Language, Map<OrderStatus, String>> STATUS_NAMES = new EnumMap<>(Language.class);
    private static final Map<Language, Map<PaymentMethod, String>> PAYMENT_METHOD_NAMES = new EnumMap<>(Language.class);

    static {
        STATUS_NAMES.put(Language.HY, Map.of(
                OrderStatus.PENDING, "Սպասման մեջ",
                OrderStatus.PAID, "Վճարված",
                OrderStatus.PROCESSING, "Մշակման մեջ",
                OrderStatus.SHIPPED, "Առաքվել է",
                OrderStatus.DELIVERED, "Հասցվել է",
                OrderStatus.CANCELLED, "Չեղարկված",
                OrderStatus.REFUNDED, "Վերադարձված"));
        STATUS_NAMES.put(Language.EN, Map.of(
                OrderStatus.PENDING, "Pending",
                OrderStatus.PAID, "Paid",
                OrderStatus.PROCESSING, "Processing",
                OrderStatus.SHIPPED, "Shipped",
                OrderStatus.DELIVERED, "Delivered",
                OrderStatus.CANCELLED, "Cancelled",
                OrderStatus.REFUNDED, "Refunded"));
        STATUS_NAMES.put(Language.RU, Map.of(
                OrderStatus.PENDING, "В ожидании",
                OrderStatus.PAID, "Оплачен",
                OrderStatus.PROCESSING, "В обработке",
                OrderStatus.SHIPPED, "Отправлен",
                OrderStatus.DELIVERED, "Доставлен",
                OrderStatus.CANCELLED, "Отменён",
                OrderStatus.REFUNDED, "Возвращён"));

        PAYMENT_METHOD_NAMES.put(Language.HY, Map.of(
                PaymentMethod.IDRAM, "Idram",
                PaymentMethod.TELCELL, "Telcell",
                PaymentMethod.ROKET_LINE, "Roket Line",
                PaymentMethod.INSTALLMENT, "ապառիկ վճարում"));
        PAYMENT_METHOD_NAMES.put(Language.EN, Map.of(
                PaymentMethod.IDRAM, "Idram",
                PaymentMethod.TELCELL, "Telcell",
                PaymentMethod.ROKET_LINE, "Roket Line",
                PaymentMethod.INSTALLMENT, "installment"));
        PAYMENT_METHOD_NAMES.put(Language.RU, Map.of(
                PaymentMethod.IDRAM, "Idram",
                PaymentMethod.TELCELL, "Telcell",
                PaymentMethod.ROKET_LINE, "Roket Line",
                PaymentMethod.INSTALLMENT, "рассрочка"));
    }

    public static String statusDisplayName(OrderStatus status, Language language) {
        return STATUS_NAMES.get(language).get(status);
    }

    public static String paymentMethodDisplayName(PaymentMethod paymentMethod, Language language) {
        return PAYMENT_METHOD_NAMES.get(language).get(paymentMethod);
    }

    public static String inAppBaseMessage(Long orderId, OrderStatus status, Language language, List<String> productNames) {
        boolean justCreated = status == OrderStatus.PENDING;
        String products = productsClause(productNames, language);
        return switch (language) {
            case HY -> justCreated
                    ? "Ձեր՝ #%s պատվերը%s ձևակերպվել է:".formatted(orderId, products)
                    : "Ձեր՝ #%s պատվերի%s կարգավիճակը փոխվել է. %s:".formatted(orderId, products, statusDisplayName(status, language));
            case RU -> justCreated
                    ? "Ваш заказ №%s%s оформлен.".formatted(orderId, products)
                    : "Статус вашего заказа №%s%s изменён на: %s.".formatted(orderId, products, statusDisplayName(status, language));
            case EN -> justCreated
                    ? "Your order #%s%s has been created.".formatted(orderId, products)
                    : "Your order #%s%s status changed to %s.".formatted(orderId, products, statusDisplayName(status, language));
        };
    }

    private static String productsClause(List<String> productNames, Language language) {
        String list = formatProductList(productNames, language);
        return list.isEmpty() ? "" : " (%s)".formatted(list);
    }

    private static String formatProductList(List<String> productNames, Language language) {
        if (productNames == null || productNames.isEmpty()) {
            return "";
        }
        if (productNames.size() <= MAX_LISTED_PRODUCTS) {
            String conjunction = switch (language) {
                case HY -> " և ";
                case RU -> " и ";
                case EN -> " and ";
            };
            return String.join(conjunction, productNames);
        }
        String first = productNames.get(0);
        int more = productNames.size() - 1;
        return switch (language) {
            case HY -> "%s և ևս %d ապրանք".formatted(first, more);
            case RU -> "%s и ещё %d %s".formatted(first, more, russianProductWord(more));
            case EN -> "%s and %d more".formatted(first, more);
        };
    }

    private static String russianProductWord(int count) {
        int mod100 = count % 100;
        int mod10 = count % 10;
        if (mod10 == 1 && mod100 != 11) {
            return "товар";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "товара";
        }
        return "товаров";
    }

    public static String inAppNoteLabel(Language language) {
        return switch (language) {
            case HY -> "Նշում";
            case RU -> "Примечание";
            case EN -> "Note";
        };
    }

    public static String translateSystemNote(String note, Language language) {
        if (note == null) {
            return null;
        }
        if (note.equals("Order created from cart")) {
            return switch (language) {
                case HY -> "Պատվերը ձևակերպվել է զամբյուղից";
                case RU -> "Заказ оформлен из корзины";
                case EN -> note;
            };
        }
        if (note.equals("Cancelled by customer")) {
            return switch (language) {
                case HY -> "Չեղարկվել է հաճախորդի կողմից";
                case RU -> "Отменён покупателем";
                case EN -> note;
            };
        }
        String paymentVerifiedPrefix = "Payment verified via ";
        if (note.startsWith(paymentVerifiedPrefix)) {
            String methodName = note.substring(paymentVerifiedPrefix.length());
            PaymentMethod method = matchPaymentMethod(methodName);
            String translatedMethod = method != null ? paymentMethodDisplayName(method, language) : methodName;
            return switch (language) {
                case HY -> "Վճարումը հաստատվել է %s-ի միջոցով".formatted(translatedMethod);
                case RU -> "Оплата подтверждена через %s".formatted(translatedMethod);
                case EN -> "Payment verified via %s".formatted(translatedMethod);
            };
        }
        return note;
    }

    private static PaymentMethod matchPaymentMethod(String rawName) {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.name().equals(rawName)) {
                return method;
            }
        }
        return null;
    }

    public static String emailGreeting(String name, Language language) {
        return switch (language) {
            case HY -> "Բարև, %s,".formatted(name);
            case RU -> "Здравствуйте, %s,".formatted(name);
            case EN -> "Hello %s,".formatted(name);
        };
    }

    public static String emailSubjectCreated(Language language) {
        return switch (language) {
            case HY -> "Ձեր պատվերն ընդունված է — TechShop AM";
            case RU -> "Ваш заказ получен — TechShopArmenia";
            case EN -> "Your order has been received — TechShopArmenia";
        };
    }

    public static String emailSubjectStatusUpdate(Long orderId, Language language) {
        return switch (language) {
            case HY -> "Պատվեր #%s-ի կարգավիճակի թարմացում — TechShop AM".formatted(orderId);
            case RU -> "Обновление статуса заказа №%s — TechShopArmenia".formatted(orderId);
            case EN -> "Order #%s status update — TechShopArmenia".formatted(orderId);
        };
    }

    public static String emailBodyCreated(Long orderId, Language language, List<String> productNames) {
        String products = productsClause(productNames, language);
        return switch (language) {
            case HY -> "Ձեր՝ #%s պատվերը%s հաջողությամբ ընդունվել է:\n".formatted(orderId, products);
            case RU -> "Ваш заказ №%s%s успешно получен.\n".formatted(orderId, products);
            case EN -> "Your order #%s%s has been successfully received.\n".formatted(orderId, products);
        };
    }

    public static String emailBodyStatusUpdate(Long orderId, OrderStatus status, Language language, List<String> productNames) {
        String statusName = statusDisplayName(status, language);
        String products = productsClause(productNames, language);
        return switch (language) {
            case HY -> "Ձեր՝ #%s պատվերի%s կարգավիճակն այժմ է. %s:\n".formatted(orderId, products, statusName);
            case RU -> "Статус вашего заказа №%s%s сейчас: %s.\n".formatted(orderId, products, statusName);
            case EN -> "Your order #%s%s status is now: %s.\n".formatted(orderId, products, statusName);
        };
    }

    public static String emailPaymentMethodLine(PaymentMethod paymentMethod, Language language) {
        String name = paymentMethodDisplayName(paymentMethod, language);
        return switch (language) {
            case HY -> "Վճարման եղանակ՝ %s\n".formatted(name);
            case RU -> "Способ оплаты: %s\n".formatted(name);
            case EN -> "Payment method: %s\n".formatted(name);
        };
    }

    public static String emailInstallmentLine(String bankName, int durationMonths, Object monthlyPayment, Language language) {
        return switch (language) {
            case HY -> "Դուք ընտրել եք ապառիկ վճարում %s-ի միջոցով, %d ամսով, ամսական %s դրամ\n"
                    .formatted(bankName, durationMonths, monthlyPayment);
            case RU -> "Вы выбрали рассрочку через %s, на %d мес., ежемесячный платёж %s դրամ\n"
                    .formatted(bankName, durationMonths, monthlyPayment);
            case EN -> "You chose installment payment via %s, over %d months, %s AMD/month\n"
                    .formatted(bankName, durationMonths, monthlyPayment);
        };
    }

    public static String emailTotalLine(Object totalPrice, Language language) {
        return switch (language) {
            case HY -> "Ընդհանուր գումարը՝ %s դրամ\n".formatted(totalPrice);
            case RU -> "Общая сумма: %s դրամ\n".formatted(totalPrice);
            case EN -> "Total amount: %s AMD\n".formatted(totalPrice);
        };
    }

    public static String emailThanks(Language language) {
        return switch (language) {
            case HY -> "Շնորհակալություն TechShop AM-ից գնումներ կատարելու համար:";
            case RU -> "Спасибо, что совершаете покупки в TechShopArmenia!";
            case EN -> "Thank you for shopping at TechShopArmenia!";
        };
    }
}
