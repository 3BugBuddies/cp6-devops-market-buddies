package br.com.fiap.marketbuddies.api.repositories;

import br.com.fiap.marketbuddies.api.entities.OrderEntity;
import br.com.fiap.marketbuddies.api.enums.DeliveryStatus;
import br.com.fiap.marketbuddies.api.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /** Update condicional: devolve 0 se o pedido já foi respondido. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update OrderEntity o
               set o.status = :newStatus,
                   o.authorizationCode = :code,
                   o.refusalReason = :reason,
                   o.respondedAt = :now
             where o.id = :id
               and o.status = br.com.fiap.marketbuddies.api.enums.OrderStatus.AWAITING_PAYMENT
            """)
    int respond(@Param("id") Long id,
                @Param("newStatus") OrderStatus newStatus,
                @Param("code") String code,
                @Param("reason") String reason,
                @Param("now") LocalDateTime now);

    /** Abre a entrega do pedido aprovado; devolve 0 se ela já começou. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update OrderEntity o
               set o.deliveryStatus = br.com.fiap.marketbuddies.api.enums.DeliveryStatus.EM_SEPARACAO,
                   o.deliveryUpdatedAt = :now
             where o.id = :id
               and o.status = br.com.fiap.marketbuddies.api.enums.OrderStatus.APPROVED
               and o.deliveryStatus is null
            """)
    int startDelivery(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** Avança uma etapa os pedidos aprovados que estão em {@code from} desde {@code limit} ou antes. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update OrderEntity o
               set o.deliveryStatus = :to,
                   o.deliveryUpdatedAt = :now
             where o.status = br.com.fiap.marketbuddies.api.enums.OrderStatus.APPROVED
               and o.deliveryStatus = :from
               and o.deliveryUpdatedAt <= :limit
            """)
    int advanceDelivery(@Param("from") DeliveryStatus from,
                        @Param("to") DeliveryStatus to,
                        @Param("limit") LocalDateTime limit,
                        @Param("now") LocalDateTime now);

    List<OrderEntity> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime limit);

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<OrderEntity> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"customer", "items"})
    List<OrderEntity> findTop50ByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"customer", "items"})
    List<OrderEntity> findTop50ByCustomer_IdOrderByCreatedAtDescIdDesc(Long customerId);

    interface StatusCount {
        OrderStatus getStatus();

        Long getTotal();
    }

    interface RecentOrderRow {
        Long getId();

        String getCustomerName();

        LocalDateTime getCreatedAt();

        BigDecimal getTotalAmount();

        OrderStatus getStatus();
    }

    @Query("""
            select o.status as status, count(o) as total
              from OrderEntity o
             where o.createdAt >= :from and o.createdAt < :to
             group by o.status
            """)
    List<StatusCount> countByStatusBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select o.id as id, c.fullName as customerName, o.createdAt as createdAt,
                   o.totalAmount as totalAmount, o.status as status
              from OrderEntity o join o.customer c
             order by o.createdAt desc, o.id desc
            """)
    List<RecentOrderRow> findRecent(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("update OrderEntity o set o.address = null where o.address.id = :addressId")
    int detachAddress(@Param("addressId") Long addressId);
}
