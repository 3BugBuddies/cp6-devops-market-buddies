package br.com.fiap.marketbuddies.api.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "TB_DELIVERY_ZONES")
public class DeliveryZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(nullable = false, length = 8)
    private String cepStart;

    @Column(nullable = false, length = 8)
    private String cepEnd;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false)
    private Integer deliveryDays;
}
