package br.com.fiap.marketbuddies.api.dtos;

import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DeliveryZoneFormDto(
        Long id,

        @NotBlank(message = "Informe o nome da zona.")
        @Size(max = 60, message = "O nome pode ter até 60 caracteres.")
        String name,

        @NotBlank(message = "Informe o CEP inicial da faixa.")
        String cepStart,

        @NotBlank(message = "Informe o CEP final da faixa.")
        String cepEnd,

        @NotNull(message = "Informe a taxa de entrega (0 = grátis).")
        @DecimalMin(value = "0.00", message = "A taxa não pode ser negativa.")
        @Digits(integer = 13, fraction = 2, message = "Use no máximo 2 casas decimais na taxa.")
        BigDecimal fee,

        @NotNull(message = "Informe o prazo em dias.")
        @Min(value = 1, message = "O prazo vai de 1 a 30 dias.")
        @Max(value = 30, message = "O prazo vai de 1 a 30 dias.")
        Integer deliveryDays) {

    public static DeliveryZoneFormDto empty() {
        return new DeliveryZoneFormDto(null, null, null, null, null, null);
    }

    public static DeliveryZoneFormDto from(DeliveryZoneEntity zone) {
        return new DeliveryZoneFormDto(
                zone.getId(),
                zone.getName(),
                zone.getCepStart(),
                zone.getCepEnd(),
                zone.getFee(),
                zone.getDeliveryDays());
    }

    public DeliveryZoneFormDto withId(Long newId) {
        return new DeliveryZoneFormDto(newId, name, cepStart, cepEnd, fee, deliveryDays);
    }
}
