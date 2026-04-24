package com.carwash.sistema.controller;

import com.carwash.sistema.model.Servicio;
import com.carwash.sistema.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioRepository servicioRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "servicios");
        model.addAttribute("servicios", servicioRepository.findAll());
        return "servicios/lista";
    }

    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        model.addAttribute("activePage", "servicios");
        model.addAttribute("servicio", new Servicio());
        return "servicios/form";
    }

    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Long id, Model model) {
        Servicio s = servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        model.addAttribute("activePage", "servicios");
        model.addAttribute("servicio", s);
        return "servicios/form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Servicio servicio, RedirectAttributes ra) {
        servicioRepository.save(servicio);
        ra.addFlashAttribute("msg", "Servicio guardado correctamente.");
        return "redirect:/servicios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        servicioRepository.findById(id).ifPresent(s -> {
            s.setActivo(false);
            servicioRepository.save(s);
        });
        ra.addFlashAttribute("msg", "Servicio desactivado.");
        return "redirect:/servicios";
    }
}
