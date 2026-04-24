package com.carwash.sistema.controller;

import com.carwash.sistema.model.*;
import com.carwash.sistema.repository.*;
import com.carwash.sistema.service.CajaDiariaService;
import com.carwash.sistema.service.OrdenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/caja-lavadero")
@RequiredArgsConstructor
@Slf4j
public class CajaDiariaController {

    private final CajaDiariaService cajaDiariaService;
    private final OrdenService ordenService;  
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final OrdenRepository ordenRepository;

    @GetMapping
    public String redirectToOrdenes() {
        return "redirect:/ordenes";
    }

    @GetMapping("/abrir")
    public String formAbrirCaja(Model model) {
        if (cajaDiariaService.hayCajaAbierta()) {
            return "redirect:/caja-lavadero?error=Ya hay una caja abierta";
        }
        model.addAttribute("activePage", "caja-lavadero");
        return "caja-lavadero/abrir-caja";
    }

    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam(required = false) BigDecimal montoInicial,
                            Authentication auth,
                            RedirectAttributes ra) {
        try {
            String username = auth.getName();
            Usuario responsable = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            CajaDiaria caja = cajaDiariaService.abrirCaja(responsable, montoInicial);

            ra.addFlashAttribute("success", "✅ Caja abierta exitosamente. ID: " + caja.getId());
            log.info("Caja abierta por: {}", username);

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al abrir caja", e);
        }

        return "redirect:/caja-lavadero";
    }

    @GetMapping("/registrar")
    public String formRegistrar(Model model) {
        if (!cajaDiariaService.hayCajaAbierta()) {
            return "redirect:/caja-lavadero?error=Debe abrir caja primero";
        }

        model.addAttribute("activePage", "caja-lavadero");
        model.addAttribute("servicios", servicioRepository.findAll());
        model.addAttribute("clientes", clienteRepository.findAll());

        return "caja-lavadero/registrar-vehiculo";
    }

    @PostMapping("/registrar")
    public String registrarVehiculo(
            @RequestParam String tipoVehiculo,
            @RequestParam(required = false) String placa,
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) String servicioDescripcion,
            @RequestParam BigDecimal monto,
            @RequestParam String metodoPago,
            @RequestParam(required = false) String observaciones,
            RedirectAttributes ra,
            Authentication authentication) {

        try {
            if (!cajaDiariaService.hayCajaAbierta()) {
                ra.addFlashAttribute("error", "❌ No hay caja abierta. Debe abrir caja primero.");
                return "redirect:/caja-lavadero";
            }
            
            CajaDiaria cajaAbierta = cajaDiariaService.getCajaAbierta().get();
            
            if (cajaAbierta.estaCerrada()) {
                ra.addFlashAttribute("error", "❌ La caja ya está cerrada.");
                return "redirect:/caja-lavadero";
            }

            RegistroLavado registro = new RegistroLavado();
            registro.setTipoVehiculo(tipoVehiculo);
            registro.setPlaca(placa != null ? placa.toUpperCase() : null);
            registro.setMonto(monto);
            registro.setMetodoPago(metodoPago);
            registro.setObservaciones(observaciones);
            registro.setEstado("PAGADO");

            if (servicioId != null && servicioId > 0) {
                Servicio servicio = servicioRepository.findById(servicioId).orElse(null);
                registro.setServicio(servicio);
            } else if (servicioDescripcion != null && !servicioDescripcion.isEmpty()) {
                registro.setServicioDescripcion(servicioDescripcion);
            }

            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
                registro.setUsuarioRegistro(usuario);
            }

            RegistroLavado guardado = cajaDiariaService.registrarVehiculo(registro);

            ra.addFlashAttribute("success", "✅ Vehículo registrado - N° " + guardado.getNumeroOrden());
            log.info("Vehículo registrado: N° {}, Tipo: {}, Monto: {}", 
                    guardado.getNumeroOrden(), guardado.getTipoVehiculo(), guardado.getMonto());

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al registrar vehículo", e);
        }

        return "redirect:/caja-lavadero";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ÓRDENES POR ESTADO (desde la vista de caja) - RUTA CORREGIDA
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/caja-ordenes")
    public String listarOrdenesPorEstado(
            @RequestParam(required = false) String estado,
            Model model,
            Authentication auth) {
        
        boolean hayCajaAbierta = cajaDiariaService.hayCajaAbierta();
        model.addAttribute("hayCajaAbierta", hayCajaAbierta);
        
        if (hayCajaAbierta) {
            CajaDiaria cajaAbierta = cajaDiariaService.getCajaAbierta().orElse(null);
            model.addAttribute("cajaAbierta", cajaAbierta);
            model.addAttribute("registros", cajaDiariaService.getRegistrosCajaActual());
        }
        
        List<OrdenServicio> ordenes;
        if (estado != null && !estado.isEmpty()) {
            ordenes = ordenService.listarPorEstado(estado);
            model.addAttribute("estadoFiltro", estado);
        } else {
            ordenes = ordenService.listarTodas();
            model.addAttribute("estadoFiltro", null);
        }
        
        model.addAttribute("ordenes", ordenes);
        model.addAttribute("activePage", "ordenes");
        model.addAttribute("pendientes", ordenService.contarPorEstado("PENDIENTE"));
        model.addAttribute("enProceso", ordenService.contarPorEstado("EN_PROCESO"));
        model.addAttribute("completados", ordenService.contarPorEstado("COMPLETADO"));
        model.addAttribute("cancelados", ordenService.contarPorEstado("CANCELADO"));
        
        List<CajaDiaria> historial = cajaDiariaService.getHistorialCajas(10);
        model.addAttribute("historial", historial);
        
        return "caja-lavadero/index";
    }

    @DeleteMapping("/registro/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarRegistro(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            cajaDiariaService.eliminarRegistro(id);
            response.put("success", true);
            response.put("message", "Registro eliminado exitosamente");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cerrar")
    public String formCerrarCaja(Model model) {
        if (!cajaDiariaService.hayCajaAbierta()) {
            return "redirect:/caja-lavadero?error=No hay caja abierta";
        }

        CajaDiaria cajaAbierta = cajaDiariaService.getCajaAbierta()
                .orElseThrow(() -> new RuntimeException("No hay caja abierta"));

        model.addAttribute("activePage", "caja-lavadero");
        model.addAttribute("caja", cajaAbierta);
        model.addAttribute("registros", cajaDiariaService.getRegistrosCajaActual());

        return "caja-lavadero/cerrar-caja";
    }

    @PostMapping("/cerrar")
    public String cerrarCaja(@RequestParam Long cajaId,
                             @RequestParam BigDecimal montoFinalContado,
                             @RequestParam(required = false) String observaciones,
                             RedirectAttributes ra) {
        try {
            CajaDiaria cajaCerrada = cajaDiariaService.cerrarCaja(cajaId, montoFinalContado, observaciones);

            ra.addFlashAttribute("success", "✅ Caja cerrada exitosamente. Total: S/ " + cajaCerrada.getTotalDia());
            
            if (cajaCerrada.getDiferencia().compareTo(BigDecimal.ZERO) != 0) {
                String diferenciaTipo = cajaCerrada.getDiferencia().compareTo(BigDecimal.ZERO) > 0 ? "Sobrante" : "Faltante";
                ra.addFlashAttribute("warning", "⚠️ " + diferenciaTipo + ": S/ " + cajaCerrada.getDiferencia().abs());
            }

            log.info("Caja cerrada - ID: {}, Total: {}", cajaCerrada.getId(), cajaCerrada.getTotalDia());

        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
            log.error("Error al cerrar caja", e);
        }

        return "redirect:/caja-lavadero";
    }

    @GetMapping("/exportar/{cajaId}")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable Long cajaId) {
        try {
            byte[] excelBytes = cajaDiariaService.generarExcelCierre(cajaId);

            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = "Cierre_Caja_" + fecha + "_ID" + cajaId + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("Excel generado - Caja ID: {}, Tamaño: {} bytes", cajaId, excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Error al generar Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reportes")
    public String reportes(Model model,
                          @RequestParam(required = false) String fechaInicio,
                          @RequestParam(required = false) String fechaFin) {
        
        model.addAttribute("activePage", "caja-lavadero");

        if (fechaInicio != null && fechaFin != null) {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            
            List<CajaDiaria> cajas = cajaDiariaService.getCajasPorRango(inicio, fin);
            model.addAttribute("cajas", cajas);

            BigDecimal totalPeriodo = cajas.stream()
                    .map(CajaDiaria::getTotalDia)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("totalPeriodo", totalPeriodo);
            model.addAttribute("fechaInicio", fechaInicio);
            model.addAttribute("fechaFin", fechaFin);
        } else {
            List<CajaDiaria> cajas = cajaDiariaService.getHistorialCajas(30);
            model.addAttribute("cajas", cajas);

            BigDecimal totalPeriodo = cajas.stream()
                    .map(CajaDiaria::getTotalDia)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("totalPeriodo", totalPeriodo);
        }

        return "caja-lavadero/reportes";
    }

    @GetMapping("/api/caja-actual")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCajaActual() {
        Map<String, Object> response = new HashMap<>();

        if (!cajaDiariaService.hayCajaAbierta()) {
            response.put("cajaAbierta", false);
            return ResponseEntity.ok(response);
        }

        CajaDiaria caja = cajaDiariaService.getCajaAbierta().orElse(null);
        List<RegistroLavado> registros = cajaDiariaService.getRegistrosCajaActual();

        response.put("cajaAbierta", true);
        response.put("caja", Map.of(
                "id", caja.getId(),
                "totalEfectivo", caja.getTotalEfectivo(),
                "totalYape", caja.getTotalYape(),
                "totalTarjeta", caja.getTotalTarjeta(),
                "totalDia", caja.getTotalDia(),
                "cantidadVehiculos", caja.getCantidadVehiculos()
        ));
        response.put("registros", registros);

        return ResponseEntity.ok(response);
    }
}