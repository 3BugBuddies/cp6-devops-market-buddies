package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.PaymentProperties;
import br.com.fiap.marketbuddies.api.dtos.PaymentRequestDto;
import br.com.fiap.marketbuddies.api.dtos.PaymentResponseDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranquiloBankServiceTest {

    private final TranquiloBankService bank =
            new TranquiloBankService(new PaymentProperties(new BigDecimal("500.00"), Duration.ZERO));

    @Test
    void aprovaAbaixoDoLimiteComCodigoTranquiloBank() {
        PaymentResponseDto response = bank.authorize(request("499.99"));

        assertThat(response.approved()).isTrue();
        assertThat(response.orderId()).isEqualTo(7L);
        assertThat(response.authorizationCode()).matches("TB-[0-9A-F]{8}");
        assertThat(response.reason()).isNull();
    }

    @Test
    void aprovaExatamenteNoLimite() {
        assertThat(bank.authorize(request("500.00")).approved()).isTrue();
    }

    @Test
    void recusaUmCentavoAcimaDoLimite() {
        PaymentResponseDto response = bank.authorize(request("500.01"));

        assertThat(response.approved()).isFalse();
        assertThat(response.authorizationCode()).isNull();
        assertThat(response.reason()).isEqualTo("Valor acima do limite de R$ 500,00");
    }

    private static PaymentRequestDto request(String total) {
        return new PaymentRequestDto(7L, "ana@exemplo.com", "Ana Souza", new BigDecimal(total), List.of(),
                LocalDateTime.now());
    }
}
