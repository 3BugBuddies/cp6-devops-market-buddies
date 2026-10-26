package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.repositories.DeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Frete do pedido: a zona do CEP dá taxa e prazo; a partir de {@link #FREE_SHIPPING_FROM} em itens, é grátis. */
@Service
@RequiredArgsConstructor
public class ShippingCalculator {

    public static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("100.00");

    private final DeliveryZoneRepository deliveryZoneRepository;

    public record Quote(String zoneName, BigDecimal fee, Integer deliveryDays, boolean free) {
    }

    public static BigDecimal fee(BigDecimal zoneFee, BigDecimal subtotal) {
        BigDecimal fee = subtotal.compareTo(FREE_SHIPPING_FROM) >= 0 ? BigDecimal.ZERO : zoneFee;
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    /** Sem zona para o CEP → 422 "Não entregamos no CEP …". */
    @Transactional(readOnly = true)
    public Quote quote(String cep, BigDecimal subtotal) {
        DeliveryZoneEntity zone = findZone(cep);
        BigDecimal fee = fee(zone.getFee(), subtotal);
        return new Quote(zone.getName(), fee, zone.getDeliveryDays(), fee.signum() == 0);
    }

    @Transactional(readOnly = true)
    public DeliveryZoneEntity findZone(String cep) {
        return deliveryZoneRepository.findCovering(cep)
                .orElseThrow(() -> new BusinessException("Não entregamos no CEP " + formatCep(cep) + "."));
    }

    private static String formatCep(String cep) {
        return cep != null && cep.length() == 8 ? cep.substring(0, 5) + "-" + cep.substring(5) : cep;
    }
}
