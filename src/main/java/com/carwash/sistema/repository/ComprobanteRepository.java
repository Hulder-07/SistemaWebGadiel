package com.carwash.sistema.repository;

import com.carwash.sistema.model.ComprobanteElectronico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComprobanteRepository extends JpaRepository<ComprobanteElectronico, Long> {

    List<ComprobanteElectronico> findTop20ByOrderByFechaEmisionDesc();

    List<ComprobanteElectronico> findByOrdenId(Long ordenId);

    /**
     * Máximo número aceptado por SUNAT para una serie (usado antes)
     */
    @Query("SELECT MAX(c.numero) FROM ComprobanteElectronico c " +
           "WHERE c.serie = :serie AND c.estado = 'ACEPTADO' AND c.numero IS NOT NULL")
    Integer findMaxNumeroBySerieAndEstadoAceptado(@Param("serie") String serie);

    /**
     * ✅ NUEVO: Máximo número de CUALQUIER estado para una serie
     * Incluye ERROR y ACEPTADO — así nunca repite un número que ya se intentó
     */
    @Query("SELECT MAX(c.numero) FROM ComprobanteElectronico c " +
           "WHERE c.serie = :serie AND c.numero IS NOT NULL")
    Integer findMaxNumeroBySerie(@Param("serie") String serie);
}