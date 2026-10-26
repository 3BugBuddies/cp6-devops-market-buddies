package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.repositories.DeliveryZoneRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShippingCalculatorTest {

    private static final BigDecimal TAXA_CENTRO = new BigDecimal("7.90");

    private final DeliveryZoneRepository zones = mock(DeliveryZoneRepository.class);
    private final ShippingCalculator calculator = new ShippingCalculator(zones);

    @Test
    void freteGratisComecaEm150Reais() {
        assertThat(ShippingCalculator.FREE_SHIPPING_FROM).isEqualByComparingTo("150.00");
    }

    @Test
    void abaixoDoLimiteCobraATaxaDaZona() {
        assertThat(ShippingCalculator.fee(TAXA_CENTRO, new BigDecimal("149.99"))).isEqualByComparingTo("7.90");
    }

    @Test
    void exatamenteNoLimiteOFreteEGratis() {
        assertThat(ShippingCalculator.fee(TAXA_CENTRO, new BigDecimal("150.00"))).isEqualByComparingTo("0.00");
    }

    @Test
    void quoteDevolveZonaTaxaEPrazo() {
        when(zones.findCovering("01310100")).thenReturn(Optional.of(zone("Centro expandido SP", "7.90", 1)));

        ShippingCalculator.Quote quote = calculator.quote("01310100", new BigDecimal("120.00"));

        assertThat(quote.zoneName()).isEqualTo("Centro expandido SP");
        assertThat(quote.fee()).isEqualByComparingTo("7.90");
        assertThat(quote.deliveryDays()).isEqualTo(1);
        assertThat(quote.free()).isFalse();
    }

    @Test
    void cepSemZonaNaoTemEntrega() {
        when(zones.findCovering("13010000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculator.quote("13010000", new BigDecimal("80.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não entregamos no CEP 13010-000.");
    }

    private static DeliveryZoneEntity zone(String name, String fee, int days) {
        DeliveryZoneEntity zone = new DeliveryZoneEntity();
        zone.setName(name);
        zone.setFee(new BigDecimal(fee));
        zone.setDeliveryDays(days);
        return zone;
    }
}
