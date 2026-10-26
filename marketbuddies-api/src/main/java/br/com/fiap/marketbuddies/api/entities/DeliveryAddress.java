package br.com.fiap.marketbuddies.api.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

/** Cópia do endereço no momento da compra: editar ou excluir o endereço depois não muda o pedido. */
@Data
@Embeddable
public class DeliveryAddress {

    @Column(name = "delivery_cep", length = 8)
    private String cep;

    @Column(name = "delivery_street", length = 120)
    private String street;

    @Column(name = "delivery_number", length = 10)
    private String number;

    @Column(name = "delivery_complement", length = 60)
    private String complement;

    @Column(name = "delivery_district", length = 60)
    private String district;

    @Column(name = "delivery_city", length = 60)
    private String city;

    @Column(name = "delivery_uf", length = 2)
    private String uf;

    public static DeliveryAddress from(AddressEntity address) {
        DeliveryAddress copy = new DeliveryAddress();
        copy.setCep(address.getCep());
        copy.setStreet(address.getStreet());
        copy.setNumber(address.getNumber());
        copy.setComplement(address.getComplement());
        copy.setDistrict(address.getDistrict());
        copy.setCity(address.getCity());
        copy.setUf(address.getUf());
        return copy;
    }
}
