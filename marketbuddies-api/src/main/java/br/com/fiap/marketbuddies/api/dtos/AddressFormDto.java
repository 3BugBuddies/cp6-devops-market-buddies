package br.com.fiap.marketbuddies.api.dtos;

import br.com.fiap.marketbuddies.api.entities.AddressEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressFormDto(
        @Size(max = 40, message = "O apelido pode ter até 40 caracteres.")
        String label,

        @NotBlank(message = "Informe o CEP.")
        String cep,

        @NotBlank(message = "Informe a rua.")
        @Size(max = 120, message = "A rua pode ter até 120 caracteres.")
        String street,

        @NotBlank(message = "Informe o número.")
        @Size(max = 10, message = "O número pode ter até 10 caracteres.")
        String number,

        @Size(max = 60, message = "O complemento pode ter até 60 caracteres.")
        String complement,

        @NotBlank(message = "Informe o bairro.")
        @Size(max = 60, message = "O bairro pode ter até 60 caracteres.")
        String district,

        @NotBlank(message = "Informe a cidade.")
        @Size(max = 60, message = "A cidade pode ter até 60 caracteres.")
        String city,

        @NotBlank(message = "Informe a UF.")
        @Pattern(regexp = "[A-Z]{2}", message = "Informe a UF com 2 letras maiúsculas, como SP.")
        String uf) {

    public static AddressFormDto empty() {
        return new AddressFormDto(null, null, null, null, null, null, null, null);
    }

    public static AddressFormDto from(AddressEntity address) {
        return new AddressFormDto(
                address.getLabel(),
                address.getCep(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getCity(),
                address.getUf());
    }
}
