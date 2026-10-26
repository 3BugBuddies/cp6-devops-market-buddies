package br.com.fiap.marketbuddies.api.repositories;

import br.com.fiap.marketbuddies.api.entities.DeliveryZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZoneEntity, Long> {

    List<DeliveryZoneEntity> findAllByOrderByCepStartAsc();

    Optional<DeliveryZoneEntity> findByNameIgnoreCase(String name);

    /** CEP já normalizado (8 dígitos); as faixas não se sobrepõem, então há no máximo uma zona. */
    @Query("select z from DeliveryZoneEntity z where z.cepStart <= :cep and z.cepEnd >= :cep")
    Optional<DeliveryZoneEntity> findCovering(@Param("cep") String cep);

    /** Zonas cuja faixa cruza [start, end], fora a de id {@code excludeId} (na criação, um id que não existe). */
    @Query("""
            select z from DeliveryZoneEntity z
             where z.id <> :excludeId and z.cepStart <= :end and z.cepEnd >= :start
             order by z.cepStart
            """)
    List<DeliveryZoneEntity> findOverlapping(@Param("start") String start, @Param("end") String end,
                                             @Param("excludeId") Long excludeId);
}
