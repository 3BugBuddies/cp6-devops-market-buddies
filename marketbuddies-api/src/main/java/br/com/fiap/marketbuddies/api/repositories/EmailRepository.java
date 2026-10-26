package br.com.fiap.marketbuddies.api.repositories;

import br.com.fiap.marketbuddies.api.entities.EmailEntity;
import br.com.fiap.marketbuddies.api.enums.StatusEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmailRepository extends JpaRepository<EmailEntity, Long> {

    Optional<EmailEntity> findFirstByOrder_IdOrderByIdDesc(Long orderId);

    List<EmailEntity> findTop50ByOrderByIdDesc();

    @Modifying
    @Query("delete from EmailEntity e where e.order.id = :orderId")
    int deleteByOrderId(@Param("orderId") Long orderId);

    interface OrderEmailStatus {
        Long getOrderId();

        StatusEmail getStatusEmail();
    }

    @Query("""
            select e.order.id as orderId, e.statusEmail as statusEmail
              from EmailEntity e
             where e.order.id in :orderIds
               and e.id = (select max(e2.id) from EmailEntity e2 where e2.order = e.order)
            """)
    List<OrderEmailStatus> findLatestStatusByOrder(@Param("orderIds") Collection<Long> orderIds);
}
