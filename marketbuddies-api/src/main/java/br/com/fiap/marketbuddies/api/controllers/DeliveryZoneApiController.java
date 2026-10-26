package br.com.fiap.marketbuddies.api.controllers;

import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneDto;
import br.com.fiap.marketbuddies.api.dtos.DeliveryZoneFormDto;
import br.com.fiap.marketbuddies.api.services.DeliveryZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/delivery-zones")
@RequiredArgsConstructor
public class DeliveryZoneApiController {

    private final DeliveryZoneService deliveryZoneService;

    @GetMapping
    public List<DeliveryZoneDto> list() {
        return deliveryZoneService.list();
    }

    @GetMapping("/{id}")
    public DeliveryZoneDto get(@PathVariable Long id) {
        return deliveryZoneService.find(id);
    }

    @PostMapping
    public ResponseEntity<DeliveryZoneDto> create(@Valid @RequestBody DeliveryZoneFormDto request) {
        DeliveryZoneDto zone = deliveryZoneService.create(request);
        return ResponseEntity.created(URI.create("/api/delivery-zones/" + zone.id())).body(zone);
    }

    @PutMapping("/{id}")
    public DeliveryZoneDto update(@PathVariable Long id, @Valid @RequestBody DeliveryZoneFormDto request) {
        return deliveryZoneService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deliveryZoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
