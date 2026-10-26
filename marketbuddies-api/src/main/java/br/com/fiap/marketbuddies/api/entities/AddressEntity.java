package br.com.fiap.marketbuddies.api.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "TB_ADDRESSES")
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity customer;

    @Column(length = 40)
    private String label;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 120)
    private String street;

    @Column(nullable = false, length = 10)
    private String number;

    @Column(length = 60)
    private String complement;

    @Column(nullable = false, length = 60)
    private String district;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
