package br.com.fiap.marketbuddies.api.entities;

import br.com.fiap.marketbuddies.api.enums.DeliveryStatus;
import br.com.fiap.marketbuddies.api.enums.OrderOrigin;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "TB_ORDERS")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity customer;

    /** De onde veio a cópia; vira nulo quando o cliente exclui o endereço (a cópia fica). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AddressEntity address;

    @Embedded
    private DeliveryAddress deliveryAddress;

    @Column(precision = 15, scale = 2)
    private BigDecimal shippingFee;

    private Integer deliveryDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    @Column(length = 40)
    private String authorizationCode;

    @Column(length = 200)
    private String refusalReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeliveryStatus deliveryStatus;

    private LocalDateTime deliveryUpdatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItemEntity> items = new ArrayList<>();
}
