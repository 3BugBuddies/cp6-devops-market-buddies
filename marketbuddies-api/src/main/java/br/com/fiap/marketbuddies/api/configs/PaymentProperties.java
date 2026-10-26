package br.com.fiap.marketbuddies.api.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.time.Duration;

/** Regra do Tranquilo Bank embutido: acima do limite recusa; {@code delay} simula a resposta do banco. */
@ConfigurationProperties("marketbuddies.payment")
public record PaymentProperties(
        @DefaultValue("500.00") BigDecimal approvalLimit,
        @DefaultValue("3s") Duration delay) {
}
