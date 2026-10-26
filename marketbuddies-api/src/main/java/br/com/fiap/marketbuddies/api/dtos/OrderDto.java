package br.com.fiap.marketbuddies.api.dtos;

import br.com.fiap.marketbuddies.api.entities.DeliveryAddress;
import br.com.fiap.marketbuddies.api.entities.OrderEntity;
import br.com.fiap.marketbuddies.api.entities.OrderItemEntity;
import br.com.fiap.marketbuddies.api.enums.DeliveryStatus;
import br.com.fiap.marketbuddies.api.enums.OrderOrigin;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import br.com.fiap.marketbuddies.api.enums.StatusEmail;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ordem explícita: sem ela o Jackson 3 põe os componentes do record na ordem do construtor. */
@JsonPropertyOrder({"authorizationCode", "createdAt", "customerEmail", "customerName", "delivery", "id", "items", "origin",
        "refusalReason", "respondedAt", "status", "statusEmail", "totalAmount"})
public record OrderDto(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        String customerEmail,
        String customerName,
        OrderOrigin origin,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        String authorizationCode,
        String refusalReason,
        StatusEmail statusEmail,
        Delivery delivery,
        List<Item> items) {

    public static OrderDto from(OrderEntity order) {
        return from(order, null);
    }

    public static OrderDto from(OrderEntity order, StatusEmail statusEmail) {
        return new OrderDto(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCustomer().getEmail(),
                order.getCustomer().getFullName(),
                order.getOrigin(),
                order.getCreatedAt(),
                order.getRespondedAt(),
                order.getAuthorizationCode(),
                order.getRefusalReason(),
                statusEmail,
                Delivery.from(order),
                order.getItems().stream().map(Item::from).toList());
    }

    /**
     * Endereço copiado, frete e andamento da entrega. Nulo só em pedido anterior à entrega; {@code status} fica nulo
     * até a aprovação.
     */
    public record Delivery(
            Long addressId,
            String cep,
            String street,
            String number,
            String complement,
            String district,
            String city,
            String uf,
            BigDecimal shippingFee,
            Integer deliveryDays,
            DeliveryStatus status,
            LocalDateTime updatedAt) {

        static Delivery from(OrderEntity order) {
            DeliveryAddress copy = order.getDeliveryAddress();
            if (copy == null && order.getDeliveryStatus() == null) {
                return null;
            }
            boolean hasCopy = copy != null;
            return new Delivery(
                    order.getAddress() == null ? null : order.getAddress().getId(),
                    hasCopy ? copy.getCep() : null,
                    hasCopy ? copy.getStreet() : null,
                    hasCopy ? copy.getNumber() : null,
                    hasCopy ? copy.getComplement() : null,
                    hasCopy ? copy.getDistrict() : null,
                    hasCopy ? copy.getCity() : null,
                    hasCopy ? copy.getUf() : null,
                    order.getShippingFee(),
                    order.getDeliveryDays(),
                    order.getDeliveryStatus(),
                    order.getDeliveryUpdatedAt());
        }

        @JsonIgnore
        public boolean isStarted() {
            return status != null;
        }

        @JsonIgnore
        public boolean isFree() {
            return shippingFee != null && shippingFee.signum() == 0;
        }
    }

    public record Item(
            String productName,
            Integer quantity,
            BigDecimal unitPrice) {

        static Item from(OrderItemEntity entity) {
            return new Item(entity.getProductName(), entity.getQuantity(), entity.getUnitPrice());
        }
    }
}
