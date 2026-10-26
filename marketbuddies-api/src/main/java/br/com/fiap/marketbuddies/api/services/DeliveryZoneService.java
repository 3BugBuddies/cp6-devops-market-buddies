package br.com.fiap.marketbuddies.api.services;

import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneDto;
import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneFormDto;
import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;
import br.com.fiap.marketbuddies.api.exceptions.ConflictException;
import br.com.fiap.marketbuddies.api.exceptions.FieldValidationException;
import br.com.fiap.marketbuddies.api.exceptions.NotFoundException;
import br.com.fiap.marketbuddies.api.repositories.DeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryZoneService {

    public static final String DUPLICATE_NAME = "Já existe uma zona com esse nome.";
    static final String INVALID_CEP = "Informe um CEP com 8 dígitos.";

    /** Na criação ainda não há id; nenhum registro tem id negativo. */
    private static final Long NO_ZONE = -1L;

    private final DeliveryZoneRepository deliveryZoneRepository;

    @Transactional(readOnly = true)
    public List<DeliveryZoneDto> list() {
        return deliveryZoneRepository.findAllByOrderByCepStartAsc().stream().map(DeliveryZoneDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryZoneDto find(Long id) {
        return DeliveryZoneDto.from(findEntity(id));
    }

    @Transactional
    public DeliveryZoneDto create(DeliveryZoneFormDto form) {
        DeliveryZoneEntity zone = new DeliveryZoneEntity();
        apply(validate(null, form), zone);
        DeliveryZoneEntity saved = save(zone);
        log.info("Zona {} criada: {} ({}–{})", saved.getId(), saved.getName(), saved.getCepStart(), saved.getCepEnd());
        return DeliveryZoneDto.from(saved);
    }

    @Transactional
    public DeliveryZoneDto update(Long id, DeliveryZoneFormDto form) {
        DeliveryZoneEntity zone = findEntity(id);
        apply(validate(id, form), zone);
        save(zone);
        log.info("Zona {} atualizada", id);
        return DeliveryZoneDto.from(zone);
    }

    /** Pedidos guardam a cópia do frete, então excluir a zona não os afeta. Devolve o nome da zona. */
    @Transactional
    public String delete(Long id) {
        DeliveryZoneEntity zone = findEntity(id);
        deliveryZoneRepository.delete(zone);
        log.info("Zona {} excluída", id);
        return zone.getName();
    }

    /** Remove tudo que não é dígito; o resultado precisa ter 8 dígitos. */
    static String normalizeCep(String field, String cep) {
        String digits = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new FieldValidationException(field, INVALID_CEP);
        }
        return digits;
    }

    private DeliveryZoneFormDto validate(Long id, DeliveryZoneFormDto form) {
        String start = normalizeCep("cepStart", form.cepStart());
        String end = normalizeCep("cepEnd", form.cepEnd());
        if (start.compareTo(end) > 0) {
            throw new FieldValidationException("cepEnd", "O CEP final deve ser maior ou igual ao inicial.");
        }
        String name = form.name().trim();
        deliveryZoneRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("name", DUPLICATE_NAME);
                });
        deliveryZoneRepository.findOverlapping(start, end, id == null ? NO_ZONE : id).stream().findFirst()
                .ifPresent(other -> {
                    throw new FieldValidationException("cepStart", "A faixa sobrepõe a zona " + other.getName() + ".");
                });
        return new DeliveryZoneFormDto(id, name, start, end, form.fee(), form.deliveryDays());
    }

    /** O unique do nome pode estourar numa corrida entre duas gravações; vira o mesmo erro no campo. */
    private DeliveryZoneEntity save(DeliveryZoneEntity zone) {
        try {
            return deliveryZoneRepository.saveAndFlush(zone);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("name", DUPLICATE_NAME);
        }
    }

    private DeliveryZoneEntity findEntity(Long id) {
        return deliveryZoneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Zona de entrega " + id + " não encontrada"));
    }

    private static void apply(DeliveryZoneFormDto form, DeliveryZoneEntity zone) {
        zone.setName(form.name());
        zone.setCepStart(form.cepStart());
        zone.setCepEnd(form.cepEnd());
        zone.setFee(form.fee());
        zone.setDeliveryDays(form.deliveryDays());
    }
}
