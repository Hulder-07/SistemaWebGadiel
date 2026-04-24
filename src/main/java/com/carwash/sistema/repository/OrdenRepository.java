package com.carwash.sistema.repository;

import com.carwash.sistema.model.OrdenServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrdenRepository extends JpaRepository<OrdenServicio, Long> {

    List<OrdenServicio> findTop15ByOrderByFechaInicioDesc();
    List<OrdenServicio> findByEstado(String estado);
    List<OrdenServicio> findByNumeroOrden(String numeroOrden);

    @Query("SELECT COALESCE(SUM(o.total),0) FROM OrdenServicio o WHERE o.estado='COMPLETADO' AND o.fechaInicio >= :ini AND o.fechaInicio < :fin")
    BigDecimal sumTotalBetween(@Param("ini") LocalDateTime ini, @Param("fin") LocalDateTime fin);

    @Query("SELECT o FROM OrdenServicio o WHERE o.fechaInicio >= :ini AND o.fechaInicio < :fin ORDER BY o.fechaInicio DESC")
    List<OrdenServicio> findByFechaBetween(@Param("ini") LocalDateTime ini, @Param("fin") LocalDateTime fin);

    @Query("SELECT o.servicio.nombre, SUM(o.total) FROM OrdenServicio o WHERE o.estado='COMPLETADO' AND o.fechaInicio >= :ini AND o.fechaInicio < :fin GROUP BY o.servicio.nombre ORDER BY SUM(o.total) DESC")
    List<Object[]> topServiciosBetween(@Param("ini") LocalDateTime ini, @Param("fin") LocalDateTime fin);

    long countByEstado(String estado);
}
