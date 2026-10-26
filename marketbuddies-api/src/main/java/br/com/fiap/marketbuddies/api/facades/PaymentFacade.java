package br.com.fiap.marketbuddies.api.facades;

import br.com.fiap.marketbuddies.api.dtos.OrderDto;
import br.com.fiap.marketbuddies.api.dtos.OrderItemRequestDto;
import br.com.fiap.marketbuddies.api.dtos.PaymentResponseDto;
import br.com.fiap.marketbuddies.api.enums.OrderOrigin;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import br.com.fiap.marketbuddies.api.enums.StatusEmail;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.services.EmailService;
import br.com.fiap.marketbuddies.api.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    private static final String DEFAULT_REFUSAL = "Pagamento recusado pelo banco";
    public static final String EXPIRED_REASON = "Tempo esgotado";

    /**
     * Publica o pedido de pagamento para o {@code PaymentRequestConsumer}, que só o processa depois do commit
     * de {@code createOrder}.
     */
    public OrderDto requestPayment(Long customerId, List<OrderItemRequestDto> items, OrderOrigin origin) {
        OrderService.CreatedOrder created = orderService.createOrder(customerId, items, origin);
        eventPublisher.publishEvent(created.paymentRequest());
        log.info("Pedido {} enviado para pagamento", created.order().id());
        return created.order();
    }

    public void paymentApproved(PaymentResponseDto response) {
        if (orderService.markApproved(response.orderId(), response.authorizationCode())) {
            emailService.sendConfirmation(response.orderId());
        }
    }

    public void paymentRefused(PaymentResponseDto response) {
        String reason = response.reason() == null || response.reason().isBlank() ? DEFAULT_REFUSAL : response.reason();
        orderService.markRefused(response.orderId(), reason);
    }

    public boolean expire(Long orderId) {
        return orderService.markRefused(orderId, EXPIRED_REASON);
    }

    public StatusEmail resendEmail(Long orderId) {
        OrderDto order = orderService.getOrder(orderId, null, true);
        if (order.status() != OrderStatus.APPROVED) {
            throw new BusinessException("Só pedido aprovado recebe e-mail de confirmação");
        }
        return emailService.sendConfirmation(orderId);
    }
}
