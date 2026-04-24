package com.carwash.sistema.repository;

import com.carwash.sistema.model.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Long> {
    
    /**
     * Obtiene la primera (y única) configuración del sistema
     */
    Optional<ConfiguracionSistema> findFirstByOrderByIdAsc();
}