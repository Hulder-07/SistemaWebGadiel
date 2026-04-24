package com.carwash.sistema.repository;

import com.carwash.sistema.model.CajaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {

    /**
     * ✅ CORREGIDO: retorna List para evitar excepción si hubiera más de un resultado.
     * Usar getCajaAbierta() en el Service que toma el primero de la lista.
     */
    @Query("SELECT c FROM CajaDiaria c WHERE c.estado = :estado ORDER BY c.fechaApertura DESC")
    List<CajaDiaria> findAllByEstado(@Param("estado") String estado);

    /**
     * Verifica si existe una caja abierta
     */
    boolean existsByEstado(String estado);

    /**
     * Encuentra cajas por rango de fechas
     */
    @Query("SELECT c FROM CajaDiaria c WHERE c.fechaApertura BETWEEN :inicio AND :fin ORDER BY c.fechaApertura DESC")
    List<CajaDiaria> findByFechaAperturaBetween(@Param("inicio") LocalDateTime inicio,
                                                @Param("fin") LocalDateTime fin);

    /**
     * Encuentra cajas cerradas por rango de fechas
     */
    @Query("SELECT c FROM CajaDiaria c WHERE c.estado = 'CERRADA' AND c.fechaCierre BETWEEN :inicio AND :fin ORDER BY c.fechaCierre DESC")
    List<CajaDiaria> findCajasCerradasBetween(@Param("inicio") LocalDateTime inicio,
                                              @Param("fin") LocalDateTime fin);

    /**
     * Encuentra cajas por responsable
     */
    @Query("SELECT c FROM CajaDiaria c WHERE c.responsable.id = :responsableId ORDER BY c.fechaApertura DESC")
    List<CajaDiaria> findByResponsableId(@Param("responsableId") Long responsableId);

    /**
     * Obtiene el total recaudado en un rango de fechas
     */
    @Query("SELECT COALESCE(SUM(c.totalDia), 0) FROM CajaDiaria c WHERE c.estado = 'CERRADA' AND c.fechaCierre BETWEEN :inicio AND :fin")
    Double sumTotalDiaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Cuenta cajas cerradas en un rango
     */
    @Query("SELECT COUNT(c) FROM CajaDiaria c WHERE c.estado = 'CERRADA' AND c.fechaCierre BETWEEN :inicio AND :fin")
    Long countCajasCerradasBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Encuentra las últimas 10 cajas cerradas
     */
    List<CajaDiaria> findTop10ByEstadoOrderByFechaCierreDesc(String estado);

    /**
     * Encuentra cajas con diferencia (arqueo con faltante o sobrante)
     */
    @Query("SELECT c FROM CajaDiaria c WHERE c.estado = 'CERRADA' AND c.diferencia <> 0 ORDER BY c.fechaCierre DESC")
    List<CajaDiaria> findCajasConDiferencia();
}