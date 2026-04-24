package com.carwash.sistema.service;

import com.carwash.sistema.model.OrdenServicio;
import com.carwash.sistema.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrdenService {
    private final OrdenRepository ordenRepository;

    public List<OrdenServicio> listarTodas() { return ordenRepository.findTop15ByOrderByFechaInicioDesc(); }
    public List<OrdenServicio> listarPorEstado(String estado) { return ordenRepository.findByEstado(estado); }
    public OrdenServicio buscarPorId(Long id) {
        return ordenRepository.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada: " + id));
    }
    public OrdenServicio guardar(OrdenServicio orden) { return ordenRepository.save(orden); }
    public OrdenServicio cambiarEstado(Long id, String estado) {
        OrdenServicio o = buscarPorId(id);
        o.setEstado(estado);
        if ("COMPLETADO".equals(estado) && o.getFechaFin() == null) o.setFechaFin(LocalDateTime.now());
        return ordenRepository.save(o);
    }
    public void eliminar(Long id) { ordenRepository.deleteById(id); }
    public long contarPorEstado(String estado) { return ordenRepository.countByEstado(estado); }
}
