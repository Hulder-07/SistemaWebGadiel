package com.carwash.sistema.service;

import com.carwash.sistema.model.Producto;
import com.carwash.sistema.model.VentaProducto;
import com.carwash.sistema.repository.ProductoRepository;
import com.carwash.sistema.repository.VentaProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VentaService {
    private final VentaProductoRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public List<VentaProducto> listarUltimas15() { return ventaRepository.findTop15ByOrderByFechaDesc(); }
    public List<VentaProducto> buscarPorBoleta(String boleta) { return ventaRepository.findByNumeroBoleta(boleta); }
    public VentaProducto buscarPorId(Long id) {
        return ventaRepository.findById(id).orElseThrow(() -> new RuntimeException("Venta no encontrada"));
    }

    public VentaProducto registrarVenta(Long productoId, Integer cantidad, String cliente) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        if (producto.getStock() < cantidad)
            throw new RuntimeException("Stock insuficiente. Disponible: " + producto.getStock());

        String boleta = "BOL-" + System.currentTimeMillis();
        VentaProducto venta = new VentaProducto();
        venta.setProducto(producto);
        venta.setCantidad(cantidad);
        venta.setPrecioUnitario(producto.getPrecio());
        venta.setTotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        venta.setCliente(cliente != null && !cliente.isBlank() ? cliente : "Cliente general");
        venta.setFecha(LocalDateTime.now());
        venta.setNumeroBoleta(boleta);
        ventaRepository.save(venta);

        producto.setStock(producto.getStock() - cantidad);
        productoRepository.save(producto);
        return venta;
    }

    public void eliminar(Long id) { ventaRepository.deleteById(id); }
}
