package com.carwash.sistema.controller;

import com.carwash.sistema.model.ConfiguracionSistema;
import com.carwash.sistema.service.ConfiguracionSistemaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/configuracion")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionController {

    private final ConfiguracionSistemaService configuracionService;

    // ══════════════════════════════════════════════════════════
    // PANTALLA PRINCIPAL
    // ══════════════════════════════════════════════════════════

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/index";
    }

    // ══════════════════════════════════════════════════════════
    // SISTEMA - DATOS DEL NEGOCIO
    // ══════════════════════════════════════════════════════════

    @GetMapping("/sistema")
    public String sistema(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/sistema";
    }

    @PostMapping("/sistema/guardar")
    public String guardarSistema(@RequestParam String nombreNegocio,
                                  @RequestParam String ruc,
                                  @RequestParam(required = false) String direccion,
                                  @RequestParam(required = false) String telefono,
                                  @RequestParam(required = false) String email,
                                  RedirectAttributes ra) {
        try {
            configuracionService.actualizarDatosNegocio(nombreNegocio, ruc, direccion, telefono, email);
            ra.addFlashAttribute("success", "✅ Datos del negocio actualizados correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al guardar datos del negocio", e);
        }
        return "redirect:/configuracion/sistema";
    }

    // ══════════════════════════════════════════════════════════
    // PERSONALIZACIÓN
    // ══════════════════════════════════════════════════════════

    @GetMapping("/personalizacion")
    public String personalizacion(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/personalizacion";
    }

    @PostMapping("/personalizacion/guardar")
public String guardarPersonalizacion(@RequestParam String colorPrimario,
                                      @RequestParam String colorSecundario,
                                      @RequestParam String tema,
                                      @RequestParam String tamanioTexto,
                                      RedirectAttributes ra,
                                      HttpServletResponse response) {
    try {
        configuracionService.actualizarPersonalizacion(colorPrimario, colorSecundario, tema, tamanioTexto);
        
        // Guardar en cookies
        addCookie(response, "tema", tema);
        addCookie(response, "colorPrimario", colorPrimario.replace("#", ""));
        addCookie(response, "colorSecundario", colorSecundario.replace("#", ""));
        addCookie(response, "tamanioTexto", tamanioTexto);
        
        ra.addFlashAttribute("success", "✅ Personalización guardada. Recarga la página para ver los cambios.");
    } catch (Exception e) {
        ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
        log.error("Error al guardar personalización", e);
    }
    return "redirect:/configuracion/personalizacion";
}

private void addCookie(HttpServletResponse response, String name, String value) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setMaxAge(365 * 24 * 60 * 60); // 1 año
    response.addCookie(cookie);
}

    // ══════════════════════════════════════════════════════════
    // FACTURACIÓN
    // ══════════════════════════════════════════════════════════

    @GetMapping("/facturacion")
    public String facturacion(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/facturacion";
    }

    @PostMapping("/facturacion/guardar")
    public String guardarFacturacion(@RequestParam String nubefactToken,
                                      @RequestParam String nubefactModo,
                                      @RequestParam String serieBoleta,
                                      @RequestParam String serieFactura,
                                      RedirectAttributes ra) {
        try {
            configuracionService.actualizarFacturacion(nubefactToken, nubefactModo, serieBoleta, serieFactura);
            ra.addFlashAttribute("success", "✅ Configuración de facturación actualizada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al guardar facturación", e);
        }
        return "redirect:/configuracion/facturacion";
    }

    // ══════════════════════════════════════════════════════════
    // HORA E IDIOMA
    // ══════════════════════════════════════════════════════════

    @GetMapping("/hora-idioma")
    public String horaIdioma(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/hora-idioma";
    }

    @PostMapping("/hora-idioma/guardar")
    public String guardarHoraIdioma(@RequestParam String zonaHoraria,
                                     @RequestParam String formatoFecha,
                                     @RequestParam String formatoHora,
                                     @RequestParam String idioma,
                                     @RequestParam String moneda,
                                     RedirectAttributes ra) {
        try {
            ConfiguracionSistema config = configuracionService.getConfiguracion();
            config.setZonaHoraria(zonaHoraria);
            config.setFormatoFecha(formatoFecha);
            config.setFormatoHora(formatoHora);
            config.setIdioma(idioma);
            config.setMoneda(moneda);
            configuracionService.guardarConfiguracion(config);
            
            ra.addFlashAttribute("success", "✅ Configuración de hora e idioma actualizada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al guardar hora e idioma", e);
        }
        return "redirect:/configuracion/hora-idioma";
    }

    // ══════════════════════════════════════════════════════════
    // COPIAS DE SEGURIDAD
    // ══════════════════════════════════════════════════════════

    @GetMapping("/copias-seguridad")
    public String copiasSeguridad(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/copias-seguridad";
    }

    @PostMapping("/copias-seguridad/descargar")
    public void descargarBackup(HttpServletResponse response) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "backup_carwash_" + timestamp + ".sql";
            
            response.setContentType("application/sql");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\MySQL\\MySQL Server 9.4\\bin\\mysqldump.exe",
                "-u", "root",
                "-padmin123",
                "carwash"
            );
            
            Process process = pb.start();
            InputStream is = process.getInputStream();
            OutputStream os = response.getOutputStream();
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            
            os.flush();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                configuracionService.registrarBackup();
            } else {
                log.error("El proceso mysqldump falló con código " + exitCode);
            }
        } catch (Exception e) {
            log.error("Error al descargar backup", e);
        }
    }

    @PostMapping("/copias-seguridad/restaurar")
    public String restaurarBackup(@RequestParam("backupFile") MultipartFile backupFile, RedirectAttributes ra) {
        if (backupFile.isEmpty()) {
            ra.addFlashAttribute("error", "❌ Por favor selecciona un archivo.");
            return "redirect:/configuracion/copias-seguridad";
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\MySQL\\MySQL Server 9.4\\bin\\mysql.exe",
                "-u", "root",
                "-padmin123",
                "carwash"
            );
            
            Process process = pb.start();
            OutputStream os = process.getOutputStream();
            InputStream is = backupFile.getInputStream();
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close(); 
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                ra.addFlashAttribute("success", "✅ Base de datos restaurada correctamente desde " + backupFile.getOriginalFilename());
            } else {
                ra.addFlashAttribute("error", "❌ Error al restaurar: El proceso mysql devolvió código " + exitCode);
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error de sistema al restaurar: " + e.getMessage());
            log.error("Error al restaurar backup", e);
        }
        
        return "redirect:/configuracion/copias-seguridad";
    }

    // ══════════════════════════════════════════════════════════
    // DISPOSITIVOS
    // ══════════════════════════════════════════════════════════

    @GetMapping("/dispositivos")
    public String dispositivos(Model model) {
        model.addAttribute("activePage", "configuracion");
        ConfiguracionSistema config = configuracionService.getConfiguracion();
        model.addAttribute("config", config);
        return "configuracion/dispositivos";
    }

    @PostMapping("/dispositivos/guardar")
    public String guardarDispositivos(@RequestParam(required = false) String impresora,
                                       @RequestParam(required = false) Boolean impresoraConectada,
                                       @RequestParam(required = false) String tablet,
                                       @RequestParam(required = false) Boolean tabletConectada,
                                       RedirectAttributes ra) {
        try {
            configuracionService.actualizarDispositivos(
                    impresora, 
                    impresoraConectada != null && impresoraConectada,
                    tablet,
                    tabletConectada != null && tabletConectada
            );
            ra.addFlashAttribute("success", "✅ Dispositivos actualizados correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al guardar dispositivos", e);
        }
        return "redirect:/configuracion/dispositivos";
    }
    @GetMapping("/accesibilidad")
public String accesibilidad(Model model) {
    model.addAttribute("activePage", "configuracion");
    ConfiguracionSistema config = configuracionService.getConfiguracion();
    model.addAttribute("config", config);
    return "configuracion/accesibilidad";
}

@GetMapping("/actualizaciones")
public String actualizaciones(Model model) {
    model.addAttribute("activePage", "configuracion");
    ConfiguracionSistema config = configuracionService.getConfiguracion();
    model.addAttribute("config", config);
    return "configuracion/actualizaciones";
}
}