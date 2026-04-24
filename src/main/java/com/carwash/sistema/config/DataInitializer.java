package com.carwash.sistema.config;

import com.carwash.sistema.model.*;
import com.carwash.sistema.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // ── Roles ──────────────────────────────────────────────
        Rol admin   = crearRol("ADMIN");
        Rol cajero  = crearRol("CAJERO");
        Rol lavador = crearRol("LAVADOR");

        // ── Usuarios ───────────────────────────────────────────
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Usuario u = new Usuario();
            u.setUsername("admin");
            u.setPassword(passwordEncoder.encode("admin123"));
            u.setNombre("Administrador");
            u.setRoles(Set.of(admin));
            usuarioRepository.save(u);
        }
        if (usuarioRepository.findByUsername("cajero").isEmpty()) {
            Usuario u = new Usuario();
            u.setUsername("cajero");
            u.setPassword(passwordEncoder.encode("cajero123"));
            u.setNombre("Cajero Principal");
            u.setRoles(Set.of(cajero));
            usuarioRepository.save(u);
        }
        if (usuarioRepository.findByUsername("lavador").isEmpty()) {
            Usuario u = new Usuario();
            u.setUsername("lavador");
            u.setPassword(passwordEncoder.encode("lavador123"));
            u.setNombre("Lavador");
            u.setRoles(Set.of(lavador));
            usuarioRepository.save(u);
        }

        // ── Categorías base ────────────────────────────────────
        if (categoriaRepository.count() == 0) {
            crearCategoria("shampoo",     "Shampoo para Vehículos",  "fa-solid fa-soap",         "#3ab0e8");
            crearCategoria("cera",        "Ceras y Pulidores",        "fa-solid fa-star",         "#f39c12");
            crearCategoria("ambientador", "Ambientadores",            "fa-solid fa-wind",         "#27ae60");
            crearCategoria("limpieza",    "Limpieza General",         "fa-solid fa-broom",        "#9b59b6");
            crearCategoria("cuidado",     "Cuidado y Siliconas",      "fa-solid fa-shield-heart", "#c0392b");
            crearCategoria("motor",       "Motor y Desengrasante",    "fa-solid fa-gears",        "#16a085");
            crearCategoria("accesorios",  "Accesorios",               "fa-solid fa-toolbox",      "#2c3e50");
        }

        // ── Servicios (Comentado o Limpiado, ahora usa DataSeeder) ──
        if (servicioRepository.count() == 0) {
            crearServicio("Lavado Premium",     "Completo + encerado + aromatizado",                  new BigDecimal("50.00"),  "premium",   60);
            crearServicio("Lavado de Moto",     "Lavado completo para motocicletas",                  new BigDecimal("12.00"),  "moto",      15);
        }

        // ── Productos ──────────────────────────────────────────
        if (productoRepository.count() == 0) {
            crearProducto("Shampoo para auto 500ml", "Shampoo neutro pH balanceado",    25, 5, new BigDecimal("18.00"), "und", "shampoo");
            crearProducto("Cera líquida 300ml",      "Protección y brillo duradero",    15, 3, new BigDecimal("35.00"), "und", "cera");
            crearProducto("Ambientador Fresh",        "Aroma cítrico para interior",    40, 8, new BigDecimal("8.00"),  "und", "ambientador");
            crearProducto("Microfibra 40x40",         "Paño microfibra multiusos",      30, 5, new BigDecimal("12.00"), "und", "accesorios");
            crearProducto("Limpiavidrios 500ml",      "Sin rayas, cristal limpio",      20, 4, new BigDecimal("14.00"), "und", "limpieza");
            crearProducto("Silicona para tablero",    "Protege y abrilla plásticos",    18, 4, new BigDecimal("16.00"), "und", "cuidado");
            crearProducto("Desengrasante 1L",         "Limpieza profunda de motor",     10, 2, new BigDecimal("25.00"), "und", "motor");
        }

        // ── Clientes demo ──────────────────────────────────────
if (clienteRepository.count() == 0) {
    Cliente c1 = new Cliente();
    c1.setDni("00000000");
    c1.setNombres("CLIENTE");
    c1.setApellidoPaterno("GENERAL");
    c1.setApellidoMaterno("SISTEMA");
    c1.setTelefono("000000000");
    c1.setActivo(true);
    clienteRepository.save(c1);
    
    Cliente c2 = new Cliente();
    c2.setDni("12345678");
    c2.setNombres("JUAN CARLOS");
    c2.setApellidoPaterno("PEREZ");
    c2.setApellidoMaterno("GARCIA");
    c2.setTelefono("999123456");
    c2.setEmail("juan.perez@ejemplo.com");
    c2.setDireccion("Av. Principal 123");
    c2.setActivo(true);
    clienteRepository.save(c2);
}
        
        // ── LIMPIEZA DE SERVICIOS ANTIGUOS ──
        java.util.List<String> viejos = java.util.Arrays.asList(
            "Lavado Básico", "Lavado Completo", "Detailing Exterior", 
            "Detailing Interior", "Lavado de Motor", "Cambio de aceite"
        );
        servicioRepository.findAll().forEach(s -> {
            if (viejos.contains(s.getNombre()) && s.getPrecioAuto() == null && s.getPrecioCamioneta() == null) {
                try {
                    servicioRepository.delete(s);
                    System.out.println("Eliminado servicio antiguo de BD: " + s.getNombre());
                } catch (Exception e) {
                    System.out.println("No se pudo eliminar el servicio (quizás tiene ventas): " + s.getNombre());
                }
            }
        });
    }

    private Rol crearRol(String nombre) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol r = new Rol();
            r.setNombre(nombre);
            return rolRepository.save(r);
        });
    }

    private void crearCategoria(String nombre, String descripcion, String icono, String color) {
        if (categoriaRepository.findByNombre(nombre).isEmpty()) {
            Categoria c = new Categoria();
            c.setNombre(nombre);
            c.setDescripcion(descripcion);
            c.setIcono(icono);
            c.setColor(color);
            categoriaRepository.save(c);
        }
    }

    private void crearServicio(String nombre, String desc, BigDecimal precio, String cat, int duracion) {
        Servicio s = new Servicio();
        s.setNombre(nombre);
        s.setDescripcion(desc);
        s.setPrecio(precio);
        s.setCategoria(cat);
        s.setDuracionMinutos(duracion);
        servicioRepository.save(s);
    }

    private void crearProducto(String nombre, String desc, int stock, int min,
                                BigDecimal precio, String unidad, String categoriaNombre) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(desc);
        p.setStock(stock);
        p.setStockMinimo(min);
        p.setPrecio(precio);
        p.setUnidad(unidad);
        // Buscar la categoría por nombre y asignarla como objeto
        categoriaRepository.findByNombre(categoriaNombre).ifPresent(p::setCategoria);
        productoRepository.save(p);
    }
}
