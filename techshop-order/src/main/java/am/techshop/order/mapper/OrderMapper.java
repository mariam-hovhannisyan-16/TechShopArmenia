package am.techshop.order.mapper;

import am.techshop.common.dto.response.OrderItemResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@SuppressWarnings("unused")
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "totalPrice", expression = "java(item.getTotalPrice())")
    OrderItemResponse toItemResponse(OrderItem item);
}