package com.carwash.sistema.repository;

import com.carwash.sistema.model.RegistroLavado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistroLavadoRepository extends JpaRepository<RegistroLavado, Long> {

    /**
     * Encuentra todos los registros de una caja específica
     */
    List<RegistroLavado> findByCajaDiariaIdOrderByNumeroOrdenDesc(Long cajaDiariaId);

    /**
     * Encuentra registros por tipo de vehículo
     */
    List<RegistroLavado> findByTipoVehiculo(String tipoVehiculo);

    /**
     * Encuentra registros por método de pago
     */
    List<RegistroLavado> findByMetodoPago(String metodoPago);

    /**
     * Busca por placa
     */
    List<RegistroLavado> findByPlacaContainingIgnoreCase(String placa);

    /**
     * Obtiene el último número de orden de una caja
     */
    @Query("SELECT MAX(r.numeroOrden) FROM RegistroLavado r WHERE r.cajaDiaria.id = :cajaDiariaId")
    Integer findMaxNumeroOrdenByCajaDiariaId(@Param("cajaDiariaId") Long cajaDiariaId);

    /**
     * Cuenta registros por tipo de vehículo en un rango de fechas
     */
    @Query("SELECT r.tipoVehiculo, COUNT(r) FROM RegistroLavado r WHERE r.fechaRegistro BETWEEN :inicio AND :fin GROUP BY r.tipoVehiculo")
    List<Object[]> countByTipoVehiculoBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Obtiene estadísticas por método de pago
     */
    @Query("SELECT r.metodoPago, COALESCE(SUM(r.monto), 0), COUNT(r) FROM RegistroLavado r WHERE r.cajaDiaria.id = :cajaDiariaId AND r.estado = 'PAGADO' GROUP BY r.metodoPago")
    List<Object[]> getEstadisticasByMetodoPago(@Param("cajaDiariaId") Long cajaDiariaId);

    /**
     * Encuentra registros pendientes de pago
     */
    List<RegistroLavado> findByEstadoOrderByFechaRegistroDesc(String estado);

    /**
     * Servicios más vendidos en un rango
     */
    @Query("SELECT r.servicioDescripcion, COUNT(r), SUM(r.monto) FROM RegistroLavado r WHERE r.fechaRegistro BETWEEN :inicio AND :fin AND r.estado = 'PAGADO' GROUP BY r.servicioDescripcion ORDER BY COUNT(r) DESC")
    List<Object[]> findServiciosMasVendidos(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Encuentra registros por fecha base
     */
    List<RegistroLavado> findByFechaRegistroBetween(LocalDateTime inicio, LocalDateTime fin);
}