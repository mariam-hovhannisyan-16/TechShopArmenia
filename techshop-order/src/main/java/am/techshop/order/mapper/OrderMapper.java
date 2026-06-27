package am.techshop.order.mapper;

import am.techshop.common.dto.response.OrderItemResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.util.PriceCalculator;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {PriceCalculator.class})
public interface OrderMapper {

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "totalPrice", expression = "java(PriceCalculator.calculateTotal(item.getProductPrice(), item.getQuantity()))")
    OrderItemResponse toItemResponse(OrderItem item);
}