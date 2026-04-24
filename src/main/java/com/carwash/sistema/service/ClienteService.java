package com.carwash.sistema.service;

import com.carwash.sistema.model.Cliente;
import com.carwash.sistema.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clienteRepository;
    public List<Cliente> listarActivos() { return clienteRepository.findByActivoTrue(); }
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }
    public Cliente guardar(Cliente c) { return clienteRepository.save(c); }
    public void desactivar(Long id) {
        clienteRepository.findById(id).ifPresent(c -> { c.setActivo(false); clienteRepository.save(c); });
    }
    public long contar() { return clienteRepository.countByActivoTrue(); }
}
