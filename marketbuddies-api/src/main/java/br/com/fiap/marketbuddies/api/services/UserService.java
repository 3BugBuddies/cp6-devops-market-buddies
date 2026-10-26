package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.dtos.SignUpDto;
import br.com.fiap.marketbuddies.api.dtos.UserDto;
import br.com.fiap.marketbuddies.api.dtos.UserFormDto;
import br.com.fiap.marketbuddies.api.entities.UserEntity;
import br.com.fiap.marketbuddies.api.enums.Role;
import br.com.fiap.marketbuddies.api.exceptions.ConflictException;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.AddressRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    public static final String DUPLICATE_EMAIL = "Já existe uma conta com esse e-mail";
    public static final String WRONG_PASSWORD = "Senha atual incorreta.";
    public static final String HAS_ORDERS = "Cliente com pedidos não pode ser excluído.";
    public static final String SELF_DELETE = "Você não pode excluir a própria conta.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;

    /** Sem {@code @Transactional} de propósito: a violação do unique chega aqui e vira erro no campo. */
    public UserEntity register(SignUpDto dto) {
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "As senhas não conferem");
        }
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new FieldValidationException("email", DUPLICATE_EMAIL);
        }

        UserEntity user = new UserEntity();
        user.setFullName(dto.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(Role.CUSTOMER);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new FieldValidationException("email", DUPLICATE_EMAIL);
        }
    }

    @Transactional(readOnly = true)
    public UserDto findProfile(Long id) {
        return userRepository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("Usuário " + id + " não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(UserDto::from).toList();
    }

    /**
     * A própria conta: exige a senha atual; senha nova é opcional. Devolve a entidade para o controller gerar o
     * token novo (o JWT usa o e-mail como subject). Sem {@code @Transactional}, como {@link #register}.
     */
    public UserEntity updateAccount(Long userId, UserFormDto form) {
        UserEntity user = find(userId);
        if (form.currentPassword() == null || !passwordEncoder.matches(form.currentPassword(), user.getPassword())) {
            throw new FieldValidationException("currentPassword", WRONG_PASSWORD);
        }
        boolean changePassword = form.newPassword() != null && !form.newPassword().isEmpty();
        if (changePassword && !form.newPassword().equals(form.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "As senhas não conferem");
        }
        apply(user, form);
        if (changePassword) {
            user.setPassword(passwordEncoder.encode(form.newPassword()));
        }
        UserEntity saved = save(user);
        log.info("Conta {} atualizada{}", userId, changePassword ? " (senha trocada)" : "");
        return saved;
    }

    /** Admin edita só nome e e-mail. */
    public UserDto updateByAdmin(Long userId, UserFormDto form) {
        UserEntity user = find(userId);
        apply(user, form);
        UserEntity saved = save(user);
        log.info("Usuário {} atualizado pelo admin", userId);
        return UserDto.from(saved);
    }

    /** Bloqueia a própria conta e cliente com pedidos; senão apaga os endereços e o usuário. */
    @Transactional
    public void deleteUser(Long userId, Long adminId) {
        UserEntity user = find(userId);
        if (userId.equals(adminId)) {
            throw new ConflictException(SELF_DELETE);
        }
        if (orderRepository.existsByCustomer_Id(userId)) {
            throw new ConflictException(HAS_ORDERS);
        }
        int addresses = addressRepository.deleteByCustomerId(userId);
        userRepository.delete(user);
        log.info("Usuário {} excluído com {} endereço(s)", userId, addresses);
    }

    private UserEntity find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário " + id + " não encontrado"));
    }

    private void apply(UserEntity user, UserFormDto form) {
        String email = form.email().trim().toLowerCase(Locale.ROOT);
        if (!email.equals(user.getEmail()) && userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new FieldValidationException("email", DUPLICATE_EMAIL);
        }
        user.setFullName(form.fullName().trim());
        user.setEmail(email);
    }

    private UserEntity save(UserEntity user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new FieldValidationException("email", DUPLICATE_EMAIL);
        }
    }
}
