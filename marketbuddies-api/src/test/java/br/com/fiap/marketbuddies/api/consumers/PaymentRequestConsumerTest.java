package br.com.fiap.marketbuddies.api.consumers;

import br.com.fiap.marketbuddies.api.configs.PaymentProperties;
import br.com.fiap.marketbuddies.api.dtos.PaymentRequestDto;
import br.com.fiap.marketbuddies.api.dtos.PaymentResponseDto;
import br.com.fiap.marketbuddies.api.facades.PaymentFacade;
import br.com.fiap.marketbuddies.api.services.TranquiloBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRequestConsumerTest {

    private final TranquiloBankService bank = mock(TranquiloBankService.class);
    private final PaymentFacade facade = mock(PaymentFacade.class);
    private PaymentRequestConsumer consumer;

    private final PaymentRequestDto request = new PaymentRequestDto(3L, "ana@exemplo.com", "Ana Souza",
            new BigDecimal("127.90"), List.of(), LocalDateTime.now());

    @BeforeEach
    void setUp() {
        consumer = new PaymentRequestConsumer(bank, facade, new PaymentProperties(new BigDecimal("500.00"), Duration.ZERO));
    }

    @Test
    void aprovacaoDoBancoVaiParaPaymentApproved() {
        PaymentResponseDto approved = new PaymentResponseDto(3L, true, "TB-1A2B3C4D", null, LocalDateTime.now());
        when(bank.authorize(request)).thenReturn(approved);

        consumer.receive(request);

        verify(facade).paymentApproved(approved);
        verify(facade, never()).paymentRefused(any());
    }

    @Test
    void recusaDoBancoVaiParaPaymentRefused() {
        PaymentResponseDto refused = new PaymentResponseDto(3L, false, null, "Valor acima do limite de R$ 500,00",
                LocalDateTime.now());
        when(bank.authorize(request)).thenReturn(refused);

        consumer.receive(request);

        verify(facade).paymentRefused(refused);
        verify(facade, never()).paymentApproved(any());
    }
}
