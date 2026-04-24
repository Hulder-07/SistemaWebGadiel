package com.carwash.sistema.repository;

import com.carwash.sistema.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    
    List<Vehiculo> findByClienteId(Long clienteId);
    
    List<Vehiculo> findByPlacaContainingIgnoreCase(String placa);
    
    // ⬇️⬇️⬇️ ESTE MÉTODO ES EL QUE FALTA ⬇️⬇️⬇️
    Optional<Vehiculo> findFirstByPlaca(String placa);
}