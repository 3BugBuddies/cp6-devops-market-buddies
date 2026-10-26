package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.PaymentProperties;
import br.com.fiap.marketbuddies.api.dtos.PaymentRequestDto;
import br.com.fiap.marketbuddies.api.dtos.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** O Tranquilo Bank do CP5, agora dentro da API: mesma regra de limite e mesmo formato de código. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranquiloBankService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final PaymentProperties paymentProperties;

    public PaymentResponseDto authorize(PaymentRequestDto request) {
        BigDecimal limit = paymentProperties.approvalLimit();
        String amountText = format(request.totalAmount());
        LocalDateTime now = LocalDateTime.now();

        if (request.totalAmount().compareTo(limit) > 0) {
            String reason = "Valor acima do limite de R$ " + format(limit);
            log.info("Pedido {} · R$ {} · RECUSADO ({})", request.orderId(), amountText, reason);
            return new PaymentResponseDto(request.orderId(), false, null, reason, now);
        }

        String code = "TB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        log.info("Pedido {} · R$ {} · APROVADO {}", request.orderId(), amountText, code);
        return new PaymentResponseDto(request.orderId(), true, code, null, now);
    }

    private static String format(BigDecimal value) {
        NumberFormat nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }
}
