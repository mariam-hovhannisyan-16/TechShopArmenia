package am.techshop.cart.mapper;

import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.CartResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "totalPrice", expression = "java(cart.getTotalPrice())")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "totalPrice", expression = "java(item.getTotalPrice())")
    CartItemResponse toItemResponse(CartItem item);
}