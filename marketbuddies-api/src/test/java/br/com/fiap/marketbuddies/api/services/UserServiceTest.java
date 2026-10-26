package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.configs.JwtProperties;
import br.com.fiap.marketbuddies.api.dtos.UserFormDto;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.enums.Role;
import br.com.fiap.marketbuddies.api.exceptions.ConflictException;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.repositories.AddressRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final Long ANA = 1L;
    private static final Long ADMIN = 9L;

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final UserService service = new UserService(users, encoder, orders, addresses);

    private UserEntity ana;

    @BeforeEach
    void setUp() {
        ana = new UserEntity();
        ana.setId(ANA);
        ana.setFullName("Ana Souza");
        ana.setEmail("cliente@marketbuddies.com");
        ana.setPassword("hash-antigo");
        ana.setRole(Role.CUSTOMER);
        when(users.findById(ANA)).thenReturn(Optional.of(ana));
        when(encoder.matches("cliente123", "hash-antigo")).thenReturn(true);
        when(encoder.encode("novaSenha1")).thenReturn("hash-novo");
        when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void senhaAtualErradaRecusaNoCampo() {
        UserFormDto form = new UserFormDto("Ana Souza", "ana@exemplo.com", "errada", null, null);

        assertThatThrownBy(() -> service.updateAccount(ANA, form))
                .isInstanceOfSatisfying(FieldValidationException.class, ex -> {
                    assertThat(ex.getField()).isEqualTo("currentPassword");
                    assertThat(ex.getMessage()).isEqualTo(UserService.WRONG_PASSWORD);
                });
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void emailEmUsoPorOutroUsuarioRecusaComMensagemDeDuplicado() {
        when(users.existsByEmailAndIdNot("bruno@exemplo.com", ANA)).thenReturn(true);
        UserFormDto form = new UserFormDto("Ana Souza", " Bruno@Exemplo.com ", "cliente123", null, null);

        assertThatThrownBy(() -> service.updateAccount(ANA, form))
                .isInstanceOfSatisfying(FieldValidationException.class, ex -> {
                    assertThat(ex.getField()).isEqualTo("email");
                    assertThat(ex.getMessage()).isEqualTo(UserService.DUPLICATE_EMAIL);
                });
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void senhaNovaComConfirmacaoDiferenteRecusa() {
        UserFormDto form = new UserFormDto("Ana Souza", "cliente@marketbuddies.com", "cliente123", "novaSenha1", "outra");

        assertThatThrownBy(() -> service.updateAccount(ANA, form))
                .isInstanceOfSatisfying(FieldValidationException.class,
                        ex -> assertThat(ex.getField()).isEqualTo("confirmPassword"));
    }

    @Test
    void trocaEmailESenhaEGeraTokenComOEmailNovo() {
        UserFormDto form = new UserFormDto("Ana S.", "Ana.Nova@Exemplo.com", "cliente123", "novaSenha1", "novaSenha1");

        UserEntity updated = service.updateAccount(ANA, form);

        assertThat(updated.getEmail()).isEqualTo("ana.nova@exemplo.com");
        assertThat(updated.getFullName()).isEqualTo("Ana S.");
        assertThat(updated.getPassword()).isEqualTo("hash-novo");
        JwtUtil jwt = new JwtUtil(new JwtProperties("segredo-de-teste-com-pelo-menos-32-bytes-0123456789", 60_000));
        String token = jwt.generateToken(new AuthUser(updated));
        assertThat(jwt.extractUsername(token)).isEqualTo("ana.nova@exemplo.com");
    }

    @Test
    void senhaNovaEmBrancoMantemASenha() {
        UserFormDto form = new UserFormDto("Ana Souza", "cliente@marketbuddies.com", "cliente123", "", "");

        UserEntity updated = service.updateAccount(ANA, form);

        assertThat(updated.getPassword()).isEqualTo("hash-antigo");
        verify(users, never()).existsByEmailAndIdNot(any(), anyLong());
    }

    @Test
    void exclusaoBloqueadaQuandoOClienteTemPedidos() {
        when(orders.existsByCustomer_Id(ANA)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteUser(ANA, ADMIN))
                .isInstanceOf(ConflictException.class)
                .hasMessage(UserService.HAS_ORDERS);
        verify(addresses, never()).deleteByCustomerId(anyLong());
        verify(users, never()).delete(any());
    }

    @Test
    void adminNaoExcluiAPropriaConta() {
        UserEntity admin = new UserEntity();
        admin.setId(ADMIN);
        admin.setRole(Role.ADMIN);
        when(users.findById(ADMIN)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.deleteUser(ADMIN, ADMIN))
                .isInstanceOf(ConflictException.class)
                .hasMessage(UserService.SELF_DELETE);
        verify(users, never()).delete(any());
    }

    @Test
    void exclusaoApagaOsEnderecosAntesDoUsuario() {
        when(orders.existsByCustomer_Id(ANA)).thenReturn(false);

        service.deleteUser(ANA, ADMIN);

        InOrder order = inOrder(addresses, users);
        order.verify(addresses).deleteByCustomerId(ANA);
        order.verify(users).delete(ana);
    }
}
