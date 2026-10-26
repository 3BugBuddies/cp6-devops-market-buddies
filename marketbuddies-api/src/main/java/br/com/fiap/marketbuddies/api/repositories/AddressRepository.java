package br.com.fiap.marketbuddies.api.repositories;

import br.com.fiap.marketbuddies.api.entities.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {

    /** Mais recente primeiro. */
    List<AddressEntity> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<AddressEntity> findByIdAndCustomerId(Long id, Long customerId);

    @Modifying
    @Query("delete from AddressEntity a where a.customer.id = :customerId")
    int deleteByCustomerId(@Param("customerId") Long customerId);
}
