package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneDto;
import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneFormDto;
import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;
import br.com.fiap.marketbuddies.api.exceptions.ConflictException;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.repositories.DeliveryZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryZoneServiceTest {

    private final DeliveryZoneRepository repository = mock(DeliveryZoneRepository.class);
    private final DeliveryZoneService service = new DeliveryZoneService(repository);

    private final DeliveryZoneEntity centro = zone(1L, "Centro expandido SP", "01000000", "05999999");

    @BeforeEach
    void setUp() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rejeitaFaixaComInicioMaiorQueFim() {
        assertThatThrownBy(() -> service.create(form("Interior", "09000-000", "08999-999")))
                .isInstanceOf(FieldValidationException.class)
                .hasFieldOrPropertyWithValue("field", "cepEnd");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejeitaFaixaQueSobrepoeOutraZonaNaCriacao() {
        when(repository.findOverlapping("05000000", "06500000", -1L)).thenReturn(List.of(centro));

        assertThatThrownBy(() -> service.create(form("Oeste", "05000000", "06500000")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessage("A faixa sobrepõe a zona Centro expandido SP.")
                .hasFieldOrPropertyWithValue("field", "cepStart");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void permiteEditarAZonaDentroDaPropriaFaixa() {
        when(repository.findById(1L)).thenReturn(Optional.of(centro));
        when(repository.findByNameIgnoreCase("Centro expandido SP")).thenReturn(Optional.of(centro));
        when(repository.findOverlapping("01000000", "04999999", 1L)).thenReturn(List.of());

        DeliveryZoneDto updated = service.update(1L, form("Centro expandido SP", "01000-000", "04999-999"));

        assertThat(updated.cepEnd()).isEqualTo("04999999");
        verify(repository).findOverlapping("01000000", "04999999", 1L);
        verify(repository).saveAndFlush(centro);
    }

    @Test
    void rejeitaNaEdicaoFaixaQueSobrepoeOutraZona() {
        DeliveryZoneEntity oeste = zone(2L, "Grande SP oeste", "06000000", "06999999");
        when(repository.findById(1L)).thenReturn(Optional.of(centro));
        when(repository.findOverlapping("01000000", "06100000", 1L)).thenReturn(List.of(oeste));

        assertThatThrownBy(() -> service.update(1L, form("Centro expandido SP", "01000000", "06100000")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessage("A faixa sobrepõe a zona Grande SP oeste.");
    }

    @Test
    void nomeRepetidoViraConflito() {
        when(repository.findByNameIgnoreCase("Centro expandido SP")).thenReturn(Optional.of(centro));

        assertThatThrownBy(() -> service.create(form(" Centro expandido SP ", "20000000", "23799999")))
                .isInstanceOf(ConflictException.class)
                .hasMessage(DeliveryZoneService.DUPLICATE_NAME)
                .hasFieldOrPropertyWithValue("field", "name");

        verify(repository, never()).findOverlapping(anyString(), anyString(), anyLong());
    }

    @Test
    void corridaNoUniqueDoNomeViraConflito() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.create(form("Rio de Janeiro capital", "20000000", "23799999")))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("field", "name");
    }

    @Test
    void normalizaCepComHifenParaOitoDigitos() {
        DeliveryZoneDto created = service.create(form("Rio de Janeiro capital", "20000-000", "23799-999"));

        ArgumentCaptor<DeliveryZoneEntity> saved = ArgumentCaptor.forClass(DeliveryZoneEntity.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCepStart()).isEqualTo("20000000");
        assertThat(saved.getValue().getCepEnd()).isEqualTo("23799999");
        assertThat(created.cepStart()).isEqualTo("20000000");
        verify(repository).findOverlapping(eq("20000000"), eq("23799999"), eq(-1L));
    }

    @Test
    void rejeitaCepQueNaoTemOitoDigitos() {
        assertThatThrownBy(() -> service.create(form("Curto", "2000-000", "23799999")))
                .isInstanceOf(FieldValidationException.class)
                .hasMessage("Informe um CEP com 8 dígitos.")
                .hasFieldOrPropertyWithValue("field", "cepStart");
    }

    private static DeliveryZoneFormDto form(String name, String cepStart, String cepEnd) {
        return new DeliveryZoneFormDto(null, name, cepStart, cepEnd, new BigDecimal("24.90"), 3);
    }

    private static DeliveryZoneEntity zone(Long id, String name, String cepStart, String cepEnd) {
        DeliveryZoneEntity zone = new DeliveryZoneEntity();
        zone.setId(id);
        zone.setName(name);
        zone.setCepStart(cepStart);
        zone.setCepEnd(cepEnd);
        zone.setFee(new BigDecimal("7.90"));
        zone.setDeliveryDays(1);
        return zone;
    }
}
