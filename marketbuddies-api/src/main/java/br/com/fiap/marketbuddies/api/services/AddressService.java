package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.clients.ViaCepClient;
import br.com.fiap.marketbuddies.api.dtos.AddressDto;
import br.com.fiap.marketbuddies.api.dtos.AddressFormDto;
import br.com.fiap.marketbuddies.api.entities.AddressEntity;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.AddressRepository;
import br.com.fiap.marketbuddies.api.repositories.OrderRepository;
import br.com.fiap.marketbuddies.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    public static final String INVALID_CEP = "Informe um CEP com 8 dígitos.";
    public static final String CEP_NOT_FOUND = "CEP não encontrado";

    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ViaCepClient viaCepClient;

    @Transactional(readOnly = true)
    public List<AddressDto> list(Long customerId) {
        return addressRepository.findByCustomerIdOrderByIdDesc(customerId).stream().map(AddressDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AddressDto get(Long id, Long customerId) {
        return AddressDto.from(find(id, customerId));
    }

    @Transactional(readOnly = true)
    public AddressFormDto findForm(Long id, Long customerId) {
        return AddressFormDto.from(find(id, customerId));
    }

    @Transactional
    public AddressDto create(Long customerId, AddressFormDto form) {
        AddressEntity address = new AddressEntity();
        apply(form, address);
        address.setCustomer(userRepository.getReferenceById(customerId));
        AddressEntity saved = addressRepository.save(address);
        log.info("Endereço {} criado para o cliente {}", saved.getId(), customerId);
        return AddressDto.from(saved);
    }

    @Transactional
    public AddressDto update(Long id, Long customerId, AddressFormDto form) {
        AddressEntity address = find(id, customerId);
        apply(form, address);
        addressRepository.save(address);
        log.info("Endereço {} atualizado", id);
        return AddressDto.from(address);
    }

    @Transactional
    public void delete(Long id, Long customerId) {
        AddressEntity address = find(id, customerId);
        int detached = orderRepository.detachAddress(id);
        addressRepository.delete(address);
        log.info("Endereço {} excluído; {} pedido(s) mantêm a cópia", id, detached);
    }

    public AddressDto lookupCep(String cep) {
        String normalized = normalizeCep(cep);
        return viaCepClient.find(normalized).orElseThrow(() -> new NotFoundException(CEP_NOT_FOUND));
    }

    /** Só os dígitos; diferente de 8 → 400 no campo {@code cep}. */
    public static String normalizeCep(String cep) {
        String digits = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new FieldValidationException("cep", INVALID_CEP);
        }
        return digits;
    }

    /** Endereço de outro cliente responde como inexistente. */
    private AddressEntity find(Long id, Long customerId) {
        return addressRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("Endereço " + id + " não encontrado"));
    }

    private static void apply(AddressFormDto form, AddressEntity address) {
        address.setCep(normalizeCep(form.cep()));
        address.setLabel(blankToNull(form.label()));
        address.setStreet(form.street().trim());
        address.setNumber(form.number().trim());
        address.setComplement(blankToNull(form.complement()));
        address.setDistrict(form.district().trim());
        address.setCity(form.city().trim());
        address.setUf(form.uf().trim());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
