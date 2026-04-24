package com.carwash.sistema.repository;

import com.carwash.sistema.model.VentaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, Long> {

    List<VentaProducto> findTop15ByOrderByFechaDesc();
    List<VentaProducto> findByNumeroBoleta(String numeroBoleta);

    @Query("SELECT COALESCE(SUM(v.total),0) FROM VentaProducto v WHERE v.fecha >= :ini AND v.fecha < :fin")
    BigDecimal sumTotalBetween(@Param("ini") LocalDateTime ini, @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM VentaProducto v WHERE v.fecha >= :ini AND v.fecha < :fin ORDER BY v.fecha DESC")
    List<VentaProducto> findByFechaBetween(@Param("ini") LocalDateTime ini, @Param("fin") LocalDateTime fin);
}
