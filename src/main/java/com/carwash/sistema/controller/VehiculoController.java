package com.carwash.sistema.controller;

import com.carwash.sistema.model.Vehiculo;
import com.carwash.sistema.repository.ClienteRepository;
import com.carwash.sistema.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "vehiculos");
        model.addAttribute("vehiculos", vehiculoRepository.findAll());
        return "vehiculos/lista";
    }

    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        model.addAttribute("activePage", "vehiculos");
        model.addAttribute("vehiculo", new Vehiculo());
        model.addAttribute("clientes", clienteRepository.findByActivoTrue());
        return "vehiculos/form";
    }

    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Long id, Model model) {
        Vehiculo v = vehiculoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        model.addAttribute("activePage", "vehiculos");
        model.addAttribute("vehiculo", v);
        model.addAttribute("clientes", clienteRepository.findByActivoTrue());
        return "vehiculos/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Vehiculo vehiculo,
                          @RequestParam(required = false) Long clienteId,
                          RedirectAttributes ra) {
        if (clienteId != null) {
            clienteRepository.findById(clienteId).ifPresent(vehiculo::setCliente);
        }
        vehiculoRepository.save(vehiculo);
        ra.addFlashAttribute("msg", "Vehículo guardado correctamente.");
        return "redirect:/vehiculos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        vehiculoRepository.deleteById(id);
        ra.addFlashAttribute("msg", "Vehículo eliminado.");
        return "redirect:/vehiculos";
    }
}
