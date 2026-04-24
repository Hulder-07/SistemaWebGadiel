package com.carwash.sistema.controller;

import com.carwash.sistema.model.Cliente;
import com.carwash.sistema.repository.ClienteRepository;
import com.carwash.sistema.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import com.carwash.sistema.service.ReniecService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Slf4j 
public class ClienteController {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "clientes");
        model.addAttribute("clientes", clienteRepository.findByActivoTrue());
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        model.addAttribute("activePage", "clientes");
        model.addAttribute("cliente", new Cliente());
        return "clientes/form";
    }

    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Long id, Model model) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        model.addAttribute("activePage", "clientes");
        model.addAttribute("cliente", c);
        return "clientes/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente, RedirectAttributes ra) {
        clienteRepository.save(cliente);
        ra.addFlashAttribute("msg", "Cliente guardado correctamente.");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}/detalle")
    public String detalle(@PathVariable Long id, Model model) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        model.addAttribute("activePage", "clientes");
        model.addAttribute("cliente", c);
        model.addAttribute("vehiculos", vehiculoRepository.findByClienteId(id));
        return "clientes/detalle";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        clienteRepository.findById(id).ifPresent(c -> {
            c.setActivo(false);
            clienteRepository.save(c);
        });
        ra.addFlashAttribute("msg", "Cliente eliminado.");
        return "redirect:/clientes";
    }

// ... dentro de la clase ClienteController:

@Autowired
private ReniecService reniecService;

@GetMapping("/api/consultar-dni/{dni}")
@ResponseBody
public ResponseEntity<Map<String, String>> consultarDni(@PathVariable String dni) {
    log.info("Consulta DNI recibida: {}", dni);
    
    Map<String, String> datos = reniecService.consultarDni(dni);
    
    if (datos != null && !datos.containsKey("error")) {
        log.info("DNI encontrado: {}", dni);
        return ResponseEntity.ok(datos);
    } else {
        String mensajeError = datos != null ? datos.get("error") : "DNI no encontrado";
        log.warn("Error consultando DNI {}: {}", dni, mensajeError);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", mensajeError));
    }
}
}
