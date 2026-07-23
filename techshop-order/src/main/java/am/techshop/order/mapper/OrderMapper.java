package am.techshop.order.mapper;

import am.techshop.common.dto.request.AddressRequest;
import am.techshop.common.dto.response.AddressResponse;
import am.techshop.common.dto.response.OrderItemResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.util.PriceCalculator;
import am.techshop.order.entity.Address;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {PriceCalculator.class})
public interface OrderMapper {

    @Mapping(target = "paymentRedirectUrl", ignore = true)
    OrderResponse toResponse(Order order);

    @Mapping(target = "paymentRedirectUrl", source = "redirectUrl")
    OrderResponse toResponse(Order order, String redirectUrl);

    @Mapping(target = "totalPrice", expression = "java(PriceCalculator.calculateTotal(item.getProductPrice(), item.getQuantity()))")
    OrderItemResponse toItemResponse(OrderItem item);

    AddressResponse toAddressResponse(Address address);

    OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history);

    Address toAddress(AddressRequest request);
}
