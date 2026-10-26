package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.DeliveryProperties;
import br.com.fiap.marketbuddies.api.entities.OrderEntity;
import br.com.fiap.marketbuddies.api.entities.OrderItemEntity;
import br.com.fiap.marketbuddies.api.entities.ProductEntity;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.enums.DeliveryStatus;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.AddressRepository;
import br.com.fiap.marketbuddies.api.repositories.EmailRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.ProductRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCancelTest {

    private static final Long ANA = 1L;
    private static final Long PEDIDO = 42L;

    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final EmailRepository emails = mock(EmailRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private final OrderService service = new OrderService(orders, products, mock(UserRepository.class), emails,
            mock(AddressRepository.class), mock(ShippingCalculator.class), clock,
            new DeliveryProperties(Duration.ofSeconds(20)));

    @Test
    void cancelarEmSeparacaoDevolveOEstoque() {
        OrderEntity order = order(OrderStatus.APPROVED, DeliveryStatus.EM_SEPARACAO);
        when(orders.findDetailedById(PEDIDO)).thenReturn(Optional.of(order));
        when(orders.cancel(PEDIDO)).thenReturn(1);

        service.cancel(PEDIDO, ANA);

        verify(orders).cancel(PEDIDO);
        verify(products).release(5L, 3);
    }

    @Test
    void cancelarDepoisDeSairParaEntregaRecusa() {
        OrderEntity order = order(OrderStatus.APPROVED, DeliveryStatus.SAIU_PARA_ENTREGA);
        when(orders.findDetailedById(PEDIDO)).thenReturn(Optional.of(order));
        when(orders.findById(PEDIDO)).thenReturn(Optional.of(order));
        when(orders.cancel(PEDIDO)).thenReturn(0);

        assertThatThrownBy(() -> service.cancel(PEDIDO, ANA))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O pedido já saiu para entrega e não pode ser cancelado.");
        verify(products, never()).release(anyLong(), anyInt());
    }

    @Test
    void cancelarPedidoDeOutroClienteRespondeNaoEncontrado() {
        when(orders.findDetailedById(PEDIDO)).thenReturn(Optional.of(order(OrderStatus.AWAITING_PAYMENT, null)));

        assertThatThrownBy(() -> service.cancel(PEDIDO, 2L)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).cancel(anyLong());
    }

    @Test
    void aprovacaoDepoisDoCancelamentoEIgnorada() {
        when(orders.respond(eq(PEDIDO), eq(OrderStatus.APPROVED), any(), any(), any())).thenReturn(0);
        when(orders.findById(PEDIDO)).thenReturn(Optional.of(order(OrderStatus.CANCELED, null)));

        assertThat(service.markApproved(PEDIDO, "TB-123")).isFalse();
        verify(orders, never()).startDelivery(anyLong(), any());
    }

    @Test
    void excluirPedidoAprovadoRecusa() {
        when(orders.findDetailedById(PEDIDO)).thenReturn(Optional.of(order(OrderStatus.APPROVED, DeliveryStatus.ENTREGUE)));

        assertThatThrownBy(() -> service.deleteOrder(PEDIDO))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Só pedido recusado ou cancelado pode ser excluído.");
        verify(emails, never()).deleteByOrderId(anyLong());
        verify(orders, never()).delete(any());
    }

    @Test
    void excluirPedidoRecusadoApagaOsEmailsEOPedido() {
        OrderEntity order = order(OrderStatus.REFUSED, null);
        when(orders.findDetailedById(PEDIDO)).thenReturn(Optional.of(order));

        service.deleteOrder(PEDIDO);

        InOrder inOrder = inOrder(emails, orders);
        inOrder.verify(emails).deleteByOrderId(PEDIDO);
        inOrder.verify(orders).delete(order);
    }

    private static OrderEntity order(OrderStatus status, DeliveryStatus delivery) {
        UserEntity ana = new UserEntity();
        ana.setId(ANA);
        ProductEntity banana = new ProductEntity();
        banana.setId(5L);

        OrderEntity order = new OrderEntity();
        order.setId(PEDIDO);
        order.setCustomer(ana);
        order.setStatus(status);
        order.setDeliveryStatus(delivery);
        order.setTotalAmount(new BigDecimal("90.00"));

        OrderItemEntity item = new OrderItemEntity();
        item.setOrder(order);
        item.setProduct(banana);
        item.setProductName("Banana prata (kg)");
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("30.00"));
        order.getItems().add(item);
        return order;
    }
}
