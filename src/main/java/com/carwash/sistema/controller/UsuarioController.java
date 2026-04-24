package com.carwash.sistema.controller;

import com.carwash.sistema.model.Rol;
import com.carwash.sistema.model.Usuario;
import com.carwash.sistema.repository.RolRepository;
import com.carwash.sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "usuarios");
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        model.addAttribute("activePage", "usuarios");
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolRepository.findAll());
        return "usuarios/form";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String nombre,
                          @RequestParam List<Long> rolesIds,
                          RedirectAttributes ra) {

        if (usuarioRepository.findByUsername(username).isPresent()) {
            ra.addFlashAttribute("error", "El usuario '" + username + "' ya existe.");
            return "redirect:/usuarios/nuevo";
        }

        Set<Rol> roles = new HashSet<>(rolRepository.findAllById(rolesIds));
        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNombre(nombre);
        user.setRoles(roles);
        usuarioRepository.save(user);

        ra.addFlashAttribute("msg", "Usuario '" + username + "' creado correctamente.");
        return "redirect:/usuarios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        usuarioRepository.findById(id).ifPresent(u -> {
            if (!u.getUsername().equals("admin")) {
                usuarioRepository.delete(u);
                ra.addFlashAttribute("msg", "Usuario eliminado.");
            } else {
                ra.addFlashAttribute("error", "No puedes eliminar al usuario admin.");
            }
        });
        return "redirect:/usuarios";
    }
}
