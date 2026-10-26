package br.com.fiap.marketbuddies.api.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequestDto(
        @NotNull(message = "Informe o endereço de entrega")
        Long addressId,
        @NotEmpty(message = "Informe ao menos um item")
        List<@Valid OrderItemRequestDto> items) {
}
