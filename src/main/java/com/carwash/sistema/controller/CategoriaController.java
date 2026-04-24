package com.carwash.sistema.controller;

import com.carwash.sistema.model.Categoria;
import com.carwash.sistema.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "categorias");
        model.addAttribute("categorias", categoriaService.listarActivas());
        return "categorias/lista";
    }

    @GetMapping("/nueva")
    public String formNueva(Model model) {
        model.addAttribute("activePage", "categorias");
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("iconos", ICONOS);
        model.addAttribute("colores", COLORES);
        return "categorias/form";
    }

    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Long id, Model model) {
        model.addAttribute("activePage", "categorias");
        model.addAttribute("categoria", categoriaService.buscarPorId(id));
        model.addAttribute("iconos", ICONOS);
        model.addAttribute("colores", COLORES);
        return "categorias/form";
    }

    @PostMapping("/guardar")
public String guardar(@ModelAttribute Categoria categoria, RedirectAttributes ra) {
    
    // Buscar por nombre (incluye inactivos)
    Categoria existente = categoriaService.buscarPorNombre(categoria.getNombre());
    
    // Si existe y está ACTIVA, y NO es la misma categoría (en edición)
    if (existente != null && existente.getActivo() && !existente.getId().equals(categoria.getId())) {
        ra.addFlashAttribute("error", "Ya existe una categoría activa con el nombre '" + categoria.getNombre() + "'");
        return "redirect:/categorias/nueva";
    }
    
    // Si existe pero está INACTIVA, reactivarla en lugar de crear nueva
    if (existente != null && !existente.getActivo()) {
        existente.setActivo(true);
        existente.setDescripcion(categoria.getDescripcion());
        categoriaService.guardar(existente);
        ra.addFlashAttribute("msg", "Categoría reactivada correctamente.");
        return "redirect:/categorias";
    }
    
    // No existe, crear nueva
    categoriaService.guardar(categoria);
    ra.addFlashAttribute("msg", "Categoría guardada correctamente.");
    return "redirect:/categorias";
}

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        categoriaService.eliminar(id);
        ra.addFlashAttribute("msg", "Categoría eliminada.");
        return "redirect:/categorias";
    }

    // Íconos disponibles de Font Awesome
    private static final String[][] ICONOS = {
        {"fa-solid fa-soap",          "Jabón/Shampoo"},
        {"fa-solid fa-star",          "Estrella/Cera"},
        {"fa-solid fa-wind",          "Viento/Ambientador"},
        {"fa-solid fa-broom",         "Escoba/Limpieza"},
        {"fa-solid fa-shield-heart",  "Escudo/Cuidado"},
        {"fa-solid fa-gears",         "Engranajes/Motor"},
        {"fa-solid fa-toolbox",       "Caja/Accesorios"},
        {"fa-solid fa-spray-can",     "Spray"},
        {"fa-solid fa-droplet",       "Gota/Líquido"},
        {"fa-solid fa-fire",          "Fuego"},
        {"fa-solid fa-leaf",          "Hoja/Natural"},
        {"fa-solid fa-car",           "Auto"},
        {"fa-solid fa-wrench",        "Llave/Herramienta"},
        {"fa-solid fa-box",           "Caja"},
        {"fa-solid fa-circle",        "Círculo"},
        {"fa-solid fa-bolt",          "Rayo/Eléctrico"},
        {"fa-solid fa-cubes",         "Cubos/Stock"},
        {"fa-solid fa-paint-roller",  "Pintura"},
        {"fa-solid fa-microscope",    "Detailing"},
        {"fa-solid fa-tshirt",        "Ropa/Textil"}
    };

    private static final String[][] COLORES = {
        {"#c0392b", "Rojo"},
        {"#3ab0e8", "Azul claro"},
        {"#f39c12", "Naranja"},
        {"#27ae60", "Verde"},
        {"#9b59b6", "Morado"},
        {"#e74c3c", "Rojo claro"},
        {"#16a085", "Verde azulado"},
        {"#2c3e50", "Gris oscuro"},
        {"#1abc9c", "Turquesa"},
        {"#e67e22", "Naranja oscuro"},
        {"#2980b9", "Azul"},
        {"#8e44ad", "Violeta"},
        {"#d35400", "Marrón"},
        {"#27ae60", "Verde oscuro"}
    };
}