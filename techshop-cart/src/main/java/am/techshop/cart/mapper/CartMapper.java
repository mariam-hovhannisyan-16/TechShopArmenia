package am.techshop.cart.mapper;

import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.util.PriceCalculator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;


@Mapper(componentModel = "spring", imports = {PriceCalculator.class})
public interface CartMapper {

    @Mapping(target = "totalPrice", expression = "java(calculateTotal(cart.getItems()))")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "totalPrice", expression = "java(PriceCalculator.calculateTotal(item.getProductPrice(), item.getQuantity()))")
    CartItemResponse toItemResponse(CartItem item);

    @SuppressWarnings("unused")
    default BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(i -> PriceCalculator.calculateTotal(i.getProductPrice(), i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}