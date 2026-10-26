package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.DeliveryProperties;
import br.com.fiap.marketbuddies.api.enums.DeliveryStatus;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import br.com.fiap.marketbuddies.api.repositories.EmailRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.ProductRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryProgressTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T18:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 26, 15, 30, 0);

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderService orderService = new OrderService(orderRepository, mock(ProductRepository.class),
            mock(UserRepository.class), mock(EmailRepository.class), clock,
            new DeliveryProperties(Duration.ofSeconds(20)));

    @Test
    void jobDelegaParaOServico() {
        OrderService service = mock(OrderService.class);

        new DeliveryProgressJob(service).advanceDeliveries();

        verify(service).advanceDeliveries();
    }

    @Test
    void avancaSoQuemEstaNaEtapaHaVinteSegundosEAUltimaEtapaPrimeiro() {
        when(orderRepository.advanceDelivery(any(), any(), any(), any())).thenReturn(1);

        int advanced = orderService.advanceDeliveries();

        LocalDateTime limit = now.minusSeconds(20);
        InOrder order = inOrder(orderRepository);
        order.verify(orderRepository).advanceDelivery(
                DeliveryStatus.SAIU_PARA_ENTREGA, DeliveryStatus.ENTREGUE, limit, now);
        order.verify(orderRepository).advanceDelivery(
                DeliveryStatus.EM_SEPARACAO, DeliveryStatus.SAIU_PARA_ENTREGA, limit, now);
        order.verifyNoMoreInteractions();
        assertThat(advanced).isEqualTo(2);
    }

    @Test
    void aprovacaoUsaORelogioEAbreAEntrega() {
        when(orderRepository.respond(7L, OrderStatus.APPROVED, "TB-1A2B3C4D", null, now)).thenReturn(1);

        assertThat(orderService.markApproved(7L, "TB-1A2B3C4D")).isTrue();

        InOrder order = inOrder(orderRepository);
        order.verify(orderRepository).respond(7L, OrderStatus.APPROVED, "TB-1A2B3C4D", null, now);
        order.verify(orderRepository).startDelivery(7L, now);
    }

    @Test
    void aprovacaoRepetidaNaoAbreEntrega() {
        when(orderRepository.respond(eq(7L), eq(OrderStatus.APPROVED), eq("TB-1A2B3C4D"), isNull(), any()))
                .thenReturn(0);

        assertThat(orderService.markApproved(7L, "TB-1A2B3C4D")).isFalse();

        verify(orderRepository, never()).startDelivery(anyLong(), any());
    }

    @Test
    void recusaUsaORelogioENaoAbreEntrega() {
        when(orderRepository.respond(7L, OrderStatus.REFUSED, null, "Tempo esgotado", now)).thenReturn(0);

        assertThat(orderService.markRefused(7L, "Tempo esgotado")).isFalse();

        verify(orderRepository).respond(7L, OrderStatus.REFUSED, null, "Tempo esgotado", now);
        verify(orderRepository, never()).startDelivery(anyLong(), any());
    }
}
