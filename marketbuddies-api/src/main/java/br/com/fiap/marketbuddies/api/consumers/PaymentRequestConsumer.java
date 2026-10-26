package br.com.fiap.marketbuddies.api.consumers;

import br.com.fiap.marketbuddies.api.configs.PaymentProperties;
import br.com.fiap.marketbuddies.api.dtos.PaymentRequestDto;
import br.com.fiap.marketbuddies.api.dtos.PaymentResponseDto;
import br.com.fiap.marketbuddies.api.facades.PaymentFacade;
import br.com.fiap.marketbuddies.api.services.TranquiloBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Faz o papel do worker do CP5 sem fila: consome o pedido de pagamento publicado pelo {@link PaymentFacade},
 * em outra thread e só depois do commit, para nunca ler um pedido ainda não gravado.
 * Se esta thread morrer, o {@code OrderExpirationJob} recusa o pedido por tempo esgotado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestConsumer {

    private final TranquiloBankService tranquiloBankService;
    private final PaymentFacade paymentFacade;
    private final PaymentProperties paymentProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void receive(PaymentRequestDto request) {
        log.info("Pedido {} recebido para pagamento · R$ {}", request.orderId(), request.totalAmount());
        if (!waitBank()) {
            return;
        }
        PaymentResponseDto response = tranquiloBankService.authorize(request);
        if (Boolean.TRUE.equals(response.approved())) {
            paymentFacade.paymentApproved(response);
        } else {
            paymentFacade.paymentRefused(response);
        }
    }

    private boolean waitBank() {
        try {
            Thread.sleep(paymentProperties.delay());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Pagamento interrompido; o job de expiração vai recusar o pedido");
            return false;
        }
    }
}
