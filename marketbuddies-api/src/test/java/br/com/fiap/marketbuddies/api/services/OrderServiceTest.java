package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.DeliveryProperties;
import br.com.fiap.marketbuddies.api.dtos.OrderItemRequestDto;
import br.com.fiap.marketbuddies.api.entities.AddressEntity;
import br.com.fiap.marketbuddies.api.entities.OrderEntity;
import br.com.fiap.marketbuddies.api.entities.ProductEntity;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.enums.Category;
import br.com.fiap.marketbuddies.api.enums.OrderOrigin;
import br.com.fiap.marketbuddies.api.enums.Role;
import br.com.fiap.marketbuddies.api.exceptions.BusinessException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.AddressRepository;
import br.com.fiap.marketbuddies.api.repositories.EmailRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.ProductRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private static final Long ANA = 1L;
    private static final Long CASA = 10L;

    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final ShippingCalculator shipping = mock(ShippingCalculator.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private final OrderService service = new OrderService(orders, products, users, mock(EmailRepository.class),
            addresses, shipping, clock, new DeliveryProperties(Duration.ofSeconds(20)));

    private final AddressEntity casa = address();

    @BeforeEach
    void setUp() {
        UserEntity ana = new UserEntity();
        ana.setId(ANA);
        ana.setEmail("cliente@marketbuddies.com");
        ana.setFullName("Ana Souza");
        ana.setRole(Role.CUSTOMER);
        when(users.findById(ANA)).thenReturn(Optional.of(ana));
        when(addresses.findByIdAndCustomerId(CASA, ANA)).thenReturn(Optional.of(casa));
        when(products.findAllById(any())).thenReturn(List.of(product()));
        when(products.reserve(anyLong(), anyInt())).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity order = inv.getArgument(0);
            order.setId(99L);
            return order;
        });
    }

    @Test
    void pedidoCopiaOEnderecoESomaOFreteAoTotal() {
        when(shipping.quote("01310100", new BigDecimal("120.00")))
                .thenReturn(new ShippingCalculator.Quote("Centro expandido SP", new BigDecimal("7.90"), 1, false));

        service.createOrder(ANA, CASA, List.of(new OrderItemRequestDto(5L, 4)), OrderOrigin.WEB);

        ArgumentCaptor<OrderEntity> saved = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orders).save(saved.capture());
        OrderEntity order = saved.getValue();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("127.90");
        assertThat(order.getShippingFee()).isEqualByComparingTo("7.90");
        assertThat(order.getDeliveryDays()).isEqualTo(1);
        assertThat(order.getAddress()).isSameAs(casa);
        assertThat(order.getDeliveryAddress().getCep()).isEqualTo("01310100");
        assertThat(order.getDeliveryAddress().getStreet()).isEqualTo("Avenida Paulista");
        assertThat(order.getDeliveryAddress().getNumber()).isEqualTo("1000");
    }

    @Test
    void copiaNaoMudaQuandoOEnderecoEEditadoDepois() {
        when(shipping.quote(any(), any()))
                .thenReturn(new ShippingCalculator.Quote("Centro expandido SP", new BigDecimal("7.90"), 1, false));

        service.createOrder(ANA, CASA, List.of(new OrderItemRequestDto(5L, 4)), OrderOrigin.WEB);
        casa.setNumber("2000");

        ArgumentCaptor<OrderEntity> saved = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getDeliveryAddress().getNumber()).isEqualTo("1000");
    }

    @Test
    void enderecoDeOutroClienteRespondeNaoEncontrado() {
        when(addresses.findByIdAndCustomerId(CASA, 2L)).thenReturn(Optional.empty());
        UserEntity bruno = new UserEntity();
        bruno.setId(2L);
        bruno.setRole(Role.CUSTOMER);
        when(users.findById(2L)).thenReturn(Optional.of(bruno));

        assertThatThrownBy(() -> service.createOrder(2L, CASA, List.of(new OrderItemRequestDto(5L, 1)), OrderOrigin.API))
                .isInstanceOf(NotFoundException.class);
        verify(products, never()).reserve(anyLong(), anyInt());
        verify(orders, never()).save(any());
    }

    @Test
    void foraDeZonaNaoReservaEstoqueNemCriaPedido() {
        when(shipping.findZone("01310100")).thenThrow(new BusinessException("Não entregamos no CEP 01310-100."));

        assertThatThrownBy(() -> service.createOrder(ANA, CASA, List.of(new OrderItemRequestDto(5L, 1)), OrderOrigin.WEB))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não entregamos no CEP 01310-100.");
        verify(products, never()).reserve(anyLong(), anyInt());
        verify(orders, never()).save(any());
    }

    private static AddressEntity address() {
        AddressEntity a = new AddressEntity();
        a.setId(CASA);
        a.setCep("01310100");
        a.setStreet("Avenida Paulista");
        a.setNumber("1000");
        a.setDistrict("Bela Vista");
        a.setCity("São Paulo");
        a.setUf("SP");
        return a;
    }

    private static ProductEntity product() {
        ProductEntity p = new ProductEntity();
        p.setId(5L);
        p.setName("Banana prata (kg)");
        p.setCategory(Category.values()[0]);
        p.setCost(new BigDecimal("20.00"));
        p.setMarkupPercentage(new BigDecimal("50.00"));
        p.setPrice(new BigDecimal("30.00"));
        p.setDiscountPercentage(BigDecimal.ZERO);
        p.setStock(50);
        return p;
    }
}
