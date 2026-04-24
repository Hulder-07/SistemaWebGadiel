package com.carwash.sistema.controller;

import com.carwash.sistema.model.Producto;
import com.carwash.sistema.repository.CategoriaRepository;
import com.carwash.sistema.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import com.carwash.sistema.service.CloudinaryService;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CloudinaryService cloudinaryService;

    // ── Listar todos ──────────────────────────────────────────
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "productos");
        model.addAttribute("productos",  productoRepository.findByActivoTrue());
        model.addAttribute("bajoStock",  productoRepository.findBajoStock());
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("categoriaActual", null);
        return "productos/lista";
    }

    // ── Filtrar por categoría ─────────────────────────────────
    @GetMapping("/categoria/{id}")
    public String listarPorCategoria(@PathVariable Long id, Model model) {
        List<Producto> filtrados = productoRepository.findByActivoTrueAndCategoriaId(id);
        model.addAttribute("activePage", "productos");
        model.addAttribute("productos",  filtrados);
        model.addAttribute("bajoStock",  productoRepository.findBajoStock());
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("categoriaActual", id);
        return "productos/lista";
    }

    // ── Nuevo ─────────────────────────────────────────────────
    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        model.addAttribute("activePage", "productos");
        model.addAttribute("producto",   new Producto());
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombreAsc());
        return "productos/form";
    }

    // ── Editar ────────────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String formEditar(@PathVariable Long id, Model model) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        model.addAttribute("activePage", "productos");
        model.addAttribute("producto",   p);
        model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombreAsc());
        return "productos/form";
    }

    // ── Guardar ───────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto,
                          @RequestParam(value = "categoriaId", required = false) Long categoriaId,
                          @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
                          RedirectAttributes ra) throws IOException {

        // Asignar categoría
        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(producto::setCategoria);
        } else {
            producto.setCategoria(null);
        }

        // Guardar imagen en Cloudinary
        if (imagenFile != null && !imagenFile.isEmpty()) {
            String ct = imagenFile.getContentType();
            if (ct != null && ct.startsWith("image/")) {
                String imageUrl = cloudinaryService.uploadFile(imagenFile, "productos");
                producto.setImagen(imageUrl);
            }
        }

        producto.setActivo(true);
        productoRepository.save(producto);
        ra.addFlashAttribute("msg", "Producto guardado correctamente.");
        return "redirect:/productos";
    }


    // ── Eliminar ──────────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        productoRepository.findById(id).ifPresent(p -> {
            p.setActivo(false);
            productoRepository.save(p);
        });
        ra.addFlashAttribute("msg", "Producto eliminado.");
        return "redirect:/productos";
    }
    @GetMapping("/stock-bajo")
public String stockBajo(Model model) {
    model.addAttribute("activePage", "stock");
    model.addAttribute("productos",  productoRepository.findBajoStock());
    model.addAttribute("bajoStock",  productoRepository.findBajoStock());
    model.addAttribute("categorias", categoriaRepository.findByActivoTrueOrderByNombreAsc());
    model.addAttribute("categoriaActual", null);
    return "productos/lista";
}
}