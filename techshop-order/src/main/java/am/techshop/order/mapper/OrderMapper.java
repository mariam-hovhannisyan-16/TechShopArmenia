package am.techshop.order.mapper;

import am.techshop.common.dto.request.AddressRequest;
import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.InstallmentDetailsRequest;
import am.techshop.common.dto.response.AddressResponse;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.dto.response.OrderItemResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.util.PriceCalculator;
import am.techshop.order.entity.Address;
import am.techshop.order.entity.InstallmentPlan;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

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

    InstallmentPlanResponse toInstallmentPlanResponse(InstallmentPlan plan);

    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "monthlyPayment", ignore = true)
    @Mapping(target = "durationMonths", source = "months")
    InstallmentPlan toInstallmentPlan(InstallmentDetailsRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(target = "paymentReference", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "notes", source = "request.notes")
    @Mapping(target = "installmentPlan", source = "request.installmentDetails")
    Order toEntity(CheckoutRequest request, Long userId, BigDecimal totalPrice, Address shippingAddress, Address billingAddress);

    @Mapping(target = "id", ignore = true)
    OrderItem toOrderItem(CartItemResponse cartItem, Order order);
}
