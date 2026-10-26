package br.com.fiap.marketbuddies.api.entities;

import br.com.fiap.marketbuddies.api.enums.StatusEmail;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "TB_EMAILS")
public class EmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrderEntity order;

    @Column(nullable = false, length = 160)
    private String emailFrom;

    @Column(nullable = false, length = 160)
    private String emailTo;

    @Column(nullable = false, length = 200)
    private String subject;

    /** text no Postgres, varchar(max) no SQL Server. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String text;

    @Column(nullable = false)
    private LocalDateTime sendDateEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEmail statusEmail;

    @Column(length = 500)
    private String errorMessage;
}
