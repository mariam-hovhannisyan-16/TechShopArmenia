package am.techshop.cart.mapper;

import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.ProductResponse;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productPrice", source = "product.price")
    CartItem toEntity(ProductResponse product, int quantity);

    @SuppressWarnings("unused")
    default BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(i -> PriceCalculator.calculateTotal(i.getProductPrice(), i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}