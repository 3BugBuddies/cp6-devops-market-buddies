package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.AddressDto;
import br.com.fiap.marketbuddies.api.dtos.AddressFormDto;
import br.com.fiap.marketbuddies.api.security.AuthUser;
import br.com.fiap.marketbuddies.api.services.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressApiController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressDto> list(@AuthenticationPrincipal AuthUser user) {
        return addressService.list(user.getId());
    }

    @GetMapping("/{id}")
    public AddressDto get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return addressService.get(id, user.getId());
    }

    @GetMapping("/cep/{cep}")
    public AddressDto lookupCep(@PathVariable String cep) {
        return addressService.lookupCep(cep);
    }

    @PostMapping
    public ResponseEntity<AddressDto> create(@Valid @RequestBody AddressFormDto request,
                                             @AuthenticationPrincipal AuthUser user) {
        AddressDto address = addressService.create(user.getId(), request);
        return ResponseEntity.created(URI.create("/api/addresses/" + address.id())).body(address);
    }

    @PutMapping("/{id}")
    public AddressDto update(@PathVariable Long id, @Valid @RequestBody AddressFormDto request,
                             @AuthenticationPrincipal AuthUser user) {
        return addressService.update(id, user.getId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        addressService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
