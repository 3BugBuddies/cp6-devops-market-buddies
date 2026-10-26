package br.com.fiap.marketbuddies.api.dtos;

import br.com.fiap.marketbuddies.api.entities.AddressEntity;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A busca de CEP devolve o mesmo DTO só com cep, rua, bairro, cidade e UF. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddressDto(
        Long id,
        String label,
        String cep,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String uf) {

    public static AddressDto from(AddressEntity address) {
        return new AddressDto(
                address.getId(),
                address.getLabel(),
                address.getCep(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getCity(),
                address.getUf());
    }

    public static AddressDto lookup(String cep, String street, String district, String city, String uf) {
        return new AddressDto(null, null, cep, street, null, null, district, city, uf);
    }

    /** CEP com hífen, para exibir. */
    public String formattedCep() {
        return cep != null && cep.length() == 8 ? cep.substring(0, 5) + "-" + cep.substring(5) : cep;
    }
}
