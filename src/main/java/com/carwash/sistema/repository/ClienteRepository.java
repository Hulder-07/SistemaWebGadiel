package com.carwash.sistema.repository;

import com.carwash.sistema.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    /**
     * Encuentra todos los clientes activos
     */
    List<Cliente> findByActivoTrue();
    
    /**
     * Cuenta cuántos clientes activos hay
     */
    Long countByActivoTrue();
    
    /**
     * Busca un cliente por DNI
     */
    Optional<Cliente> findByDni(String dni);
    
    /**
     * Verifica si existe un cliente con ese DNI
     */
    boolean existsByDni(String dni);
    
    /**
     * Busca clientes por nombre, apellidos o DNI (búsqueda parcial)
     */
    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(c.nombres) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "c.dni LIKE CONCAT('%', :texto, '%')")
    List<Cliente> buscarPorTexto(String texto);
}