package com.carwash.sistema.repository;

import com.carwash.sistema.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByActivoTrueOrderByNombreAsc();
    Optional<Categoria> findByNombre(String nombre);
}