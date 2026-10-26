package br.com.fiap.marketbuddies.api.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** {@code step}: tempo mínimo em cada etapa da entrega antes de avançar para a próxima. */
@ConfigurationProperties("marketbuddies.delivery")
public record DeliveryProperties(@DefaultValue("20s") Duration step) {
}
