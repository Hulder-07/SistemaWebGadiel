package com.carwash.sistema.repository;

import com.carwash.sistema.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();
    List<Producto> findByActivoTrueAndCategoriaId(Long categoriaId);
    long countByActivoTrue();

    @Query("SELECT p FROM Producto p WHERE p.activo=true AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findBajoStock();
}