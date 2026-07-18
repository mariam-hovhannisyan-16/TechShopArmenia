package am.techshop.common.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(PENDING, EnumSet.of(PAID, CANCELLED));
        TRANSITIONS.put(PAID, EnumSet.of(PROCESSING, CANCELLED, REFUNDED));
        TRANSITIONS.put(PROCESSING, EnumSet.of(SHIPPED, CANCELLED));
        TRANSITIONS.put(SHIPPED, EnumSet.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, EnumSet.of(REFUNDED));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
