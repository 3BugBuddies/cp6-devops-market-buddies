package br.com.fiap.marketbuddies.api.dtos;

import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;

import java.math.BigDecimal;

public record DeliveryZoneDto(
        Long id,
        String name,
        String cepStart,
        String cepEnd,
        BigDecimal fee,
        Integer deliveryDays) {

    public static DeliveryZoneDto from(DeliveryZoneEntity zone) {
        return new DeliveryZoneDto(
                zone.getId(),
                zone.getName(),
                zone.getCepStart(),
                zone.getCepEnd(),
                zone.getFee(),
                zone.getDeliveryDays());
    }
}
