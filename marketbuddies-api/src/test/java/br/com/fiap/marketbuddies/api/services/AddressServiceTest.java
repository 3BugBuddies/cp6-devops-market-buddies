package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.clients.ViaCepClient;
import br.com.fiap.marketbuddies.api.dtos.AddressDto;
import br.com.fiap.marketbuddies.api.dtos.AddressFormDto;
import br.com.fiap.marketbuddies.api.entities.AddressEntity;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.AddressRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AddressServiceTest {

    private static final Long ANA = 2L;
    private static final Long OUTRO_CLIENTE = 3L;

    private final AddressRepository addressRepository = mock(AddressRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ViaCepClient viaCepClient = mock(ViaCepClient.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final AddressService service = new AddressService(addressRepository, orderRepository, userRepository, viaCepClient);

    @Test
    void normalizaCepComHifen() {
        assertThat(AddressService.normalizeCep("01310-100")).isEqualTo("01310100");
    }

    @Test
    void normalizaCepSemHifenEComEspacos() {
        assertThat(AddressService.normalizeCep(" 01310100 ")).isEqualTo("01310100");
    }

    @Test
    void recusaCepComMenosDeOitoDigitos() {
        assertThatThrownBy(() -> AddressService.normalizeCep("01310-10"))
                .isInstanceOf(FieldValidationException.class)
                .hasMessage("Informe um CEP com 8 dígitos.")
                .extracting("field").isEqualTo("cep");
    }

    @Test
    void criaEnderecoComCepNormalizadoECamposLimpos() {
        UserEntity ana = new UserEntity();
        ana.setId(ANA);
        when(userRepository.getReferenceById(ANA)).thenReturn(ana);
        when(addressRepository.save(any(AddressEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AddressDto created = service.create(ANA, form("01310-100"));

        ArgumentCaptor<AddressEntity> saved = ArgumentCaptor.forClass(AddressEntity.class);
        verify(addressRepository).save(saved.capture());
        assertThat(saved.getValue().getCustomer()).isSameAs(ana);
        assertThat(created.cep()).isEqualTo("01310100");
        assertThat(created.street()).isEqualTo("Avenida Paulista");
        assertThat(created.complement()).isNull();
    }

    @Test
    void criacaoComCepInvalidoNaoSalva() {
        assertThatThrownBy(() -> service.create(ANA, form("1234")))
                .isInstanceOf(FieldValidationException.class);
        verify(addressRepository, never()).save(any());
    }

    @Test
    void getDeEnderecoDeOutroClienteRespondeNaoEncontrado() {
        when(addressRepository.findByIdAndCustomerId(10L, OUTRO_CLIENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(10L, OUTRO_CLIENTE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Endereço 10 não encontrado");
    }

    @Test
    void updateDeEnderecoDeOutroClienteRespondeNaoEncontradoESemSalvar() {
        when(addressRepository.findByIdAndCustomerId(10L, OUTRO_CLIENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, OUTRO_CLIENTE, form("01310100")))
                .isInstanceOf(NotFoundException.class);
        verify(addressRepository, never()).save(any());
    }

    @Test
    void deleteDeEnderecoDeOutroClienteRespondeNaoEncontradoESemApagar() {
        when(addressRepository.findByIdAndCustomerId(10L, OUTRO_CLIENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, OUTRO_CLIENTE))
                .isInstanceOf(NotFoundException.class);
        verify(addressRepository, never()).delete(any());
        verify(addressRepository, never()).deleteById(any());
        verify(orderRepository, never()).detachAddress(any());
    }

    @Test
    void deleteDoDonoDesvinculaOsPedidosEApagaOEndereco() {
        AddressEntity address = new AddressEntity();
        address.setId(10L);
        when(addressRepository.findByIdAndCustomerId(10L, ANA)).thenReturn(Optional.of(address));

        service.delete(10L, ANA);

        InOrder ordem = inOrder(orderRepository, addressRepository);
        ordem.verify(orderRepository).detachAddress(10L);
        ordem.verify(addressRepository).delete(address);
    }

    @Test
    void buscaCepNormalizaAntesDeChamarOViaCep() {
        AddressDto partial = AddressDto.lookup("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");
        when(viaCepClient.find("01310100")).thenReturn(Optional.of(partial));

        assertThat(service.lookupCep("01310-100")).isEqualTo(partial);
    }

    @Test
    void buscaCepInexistenteRespondeNaoEncontrado() {
        when(viaCepClient.find("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookupCep("99999-999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("CEP não encontrado");
    }

    @Test
    void buscaCepInvalidoNaoChamaOViaCep() {
        assertThatThrownBy(() -> service.lookupCep("123"))
                .isInstanceOf(FieldValidationException.class);
        verifyNoInteractions(viaCepClient);
    }

    private static AddressFormDto form(String cep) {
        return new AddressFormDto("Casa", cep, " Avenida Paulista ", "1000", "  ", "Bela Vista", "São Paulo", "SP");
    }
}
