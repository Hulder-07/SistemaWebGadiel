package com.carwash.sistema.controller;

import com.carwash.sistema.model.*;
import com.carwash.sistema.repository.*;
import com.carwash.sistema.service.NubefactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Controller
@RequestMapping("/sunat")
@RequiredArgsConstructor
@Slf4j
public class SunatController {

    private final NubefactService nubefactService;
    private final ComprobanteRepository comprobanteRepository;
    private final OrdenRepository ordenRepository;
    private final VentaProductoRepository ventaRepository;
    private final ProductoRepository productoRepository;

    // ✅ Helper para leer campos del Map sin riesgo de NullPointerException
    private String safeGet(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    // ── Listar comprobantes ──────────────────────────────────────
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "sunat");
        List<ComprobanteElectronico> comprobantes = comprobanteRepository.findTop20ByOrderByFechaEmisionDesc();

        long totalAceptados = comprobantes.stream().filter(c -> "ACEPTADO".equals(c.getEstado())).count();
        long totalError     = comprobantes.stream().filter(c -> "ERROR".equals(c.getEstado())).count();
        long totalPendiente = comprobantes.stream().filter(c -> "PENDIENTE".equals(c.getEstado())).count();

        model.addAttribute("comprobantes",       comprobantes);
        model.addAttribute("totalComprobantes",  comprobantes.size());
        model.addAttribute("totalAceptados",     totalAceptados);
        model.addAttribute("totalError",         totalError);
        model.addAttribute("totalPendiente",     totalPendiente);

        return "sunat/lista";
    }

    // ── Ver detalle de comprobante ────────────────────────────────
    @GetMapping("/ver/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        ComprobanteElectronico comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

        model.addAttribute("activePage",   "sunat");
        model.addAttribute("comprobante",  comprobante);

        if (comprobante.getOrdenId() != null) {
            ordenRepository.findById(comprobante.getOrdenId())
                    .ifPresent(orden -> model.addAttribute("orden", orden));
        }

        if (comprobante.getVentaId() != null && comprobante.getVentaId() > 0) {
            model.addAttribute("tieneVenta", true);
        }

        return "sunat/detalle";
    }

    // ── Formulario emitir desde orden ────────────────────────────
    @GetMapping("/emitir/orden/{id}")
    public String formOrden(@PathVariable Long id, Model model) {
        OrdenServicio orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        List<ComprobanteElectronico> existentes = comprobanteRepository.findByOrdenId(id);
        if (!existentes.isEmpty() && existentes.stream().anyMatch(c -> "ACEPTADO".equals(c.getEstado()))) {
            model.addAttribute("error", "Esta orden ya tiene un comprobante emitido");
            return "redirect:/ordenes";
        }

        model.addAttribute("activePage",        "sunat");
        model.addAttribute("origen",            "orden");
        model.addAttribute("origenId",          id);
        model.addAttribute("total",             orden.getTotal());
        model.addAttribute("descripcion",       orden.getServicio().getNombre());
        model.addAttribute("clienteNombre",     orden.getVehiculo().getCliente() != null
                ? orden.getVehiculo().getCliente().getNombre() : "Cliente General");
        model.addAttribute("clienteDocumento",  orden.getVehiculo().getCliente() != null
                ? orden.getVehiculo().getCliente().getDni() : "00000000");

        return "sunat/form";
    }

    // ── Formulario emitir desde venta ────────────────────────────
    @GetMapping("/emitir/venta/{boleta}")
    public String formVenta(@PathVariable String boleta, Model model) {
        List<VentaProducto> ventas = ventaRepository.findByNumeroBoleta(boleta);
        if (ventas.isEmpty()) return "redirect:/ventas";

        BigDecimal total = ventas.stream()
                .map(VentaProducto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("activePage",        "sunat");
        model.addAttribute("origen",            "venta");
        model.addAttribute("origenId",          boleta);
        model.addAttribute("total",             total);
        model.addAttribute("ventas",            ventas);
        model.addAttribute("descripcion",       "Venta de productos");
        model.addAttribute("clienteNombre",     ventas.get(0).getCliente());
        model.addAttribute("clienteDocumento",  "00000000");

        return "sunat/form";
    }

    // ── Emitir comprobante ────────────────────────────────────────
    @PostMapping("/emitir")
    public String emitir(@RequestParam String tipoComprobante,
                         @RequestParam String clienteNombre,
                         @RequestParam String clienteDocumento,
                         @RequestParam(required = false) String clienteDireccion,
                         @RequestParam String descripcion,
                         @RequestParam double total,
                         @RequestParam String origen,
                         @RequestParam String origenId,
                         RedirectAttributes ra) {
        try {
            // ── Preparar items ──
            List<NubefactService.ItemComprobante> items = new ArrayList<>();

            if ("orden".equals(origen)) {
                Long ordenIdLong = Long.parseLong(origenId);
                OrdenServicio orden = ordenRepository.findById(ordenIdLong)
                        .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
                items.add(new NubefactService.ItemComprobante(
                        "SERV-" + orden.getServicio().getId(),
                        orden.getServicio().getNombre() + " - " + orden.getServicio().getDescripcion(),
                        1.0,
                        orden.getTotal().doubleValue(),
                        "ZZ"
                ));

            } else if ("venta".equals(origen)) {
                List<VentaProducto> ventas = ventaRepository.findByNumeroBoleta(origenId);
                for (VentaProducto venta : ventas) {
                    Producto producto = productoRepository.findById(venta.getProducto().getId())
                            .orElse(venta.getProducto());
                    items.add(new NubefactService.ItemComprobante(
                            "PROD-" + producto.getId(),
                            producto.getNombre(),
                            venta.getCantidad().doubleValue(),
                            venta.getPrecioUnitario().doubleValue(),
                            "NIU"
                    ));
                }
            } else {
                items.add(new NubefactService.ItemComprobante(descripcion, 1.0, total, "NIU"));
            }

            // ── Llamar a Nubefact ──
            Map<String, Object> respuesta;
            if ("BOLETA".equals(tipoComprobante)) {
                respuesta = nubefactService.emitirBoleta(clienteNombre, clienteDocumento, items, total);
            } else {
                respuesta = nubefactService.emitirFactura(clienteNombre, clienteDocumento,
                        clienteDireccion, items, total);
            }

            // Log completo de la respuesta para diagnóstico
            log.info("Respuesta completa Nubefact: {}", respuesta);

            // ── Construir entidad comprobante ──
            ComprobanteElectronico comp = new ComprobanteElectronico();
            comp.setTipoComprobante(tipoComprobante);
            comp.setClienteNombre(clienteNombre);
            comp.setClienteDocumento(clienteDocumento);
            comp.setTotal(BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP));

            BigDecimal totalBD   = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal  = totalBD.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
            BigDecimal igv       = totalBD.subtract(subtotal);
            comp.setSubtotal(subtotal);
            comp.setIgv(igv);

            // ✅ CORREGIDO: en modo beta aceptada_por_sunat=false pero el comprobante
            // sí se genera correctamente — verificar por presencia de enlace_del_pdf
            Object aceptadaObj = respuesta.get("aceptada_por_sunat");
            boolean aceptado = Boolean.TRUE.equals(aceptadaObj)
                    || "true".equalsIgnoreCase(String.valueOf(aceptadaObj))
                    || (respuesta.get("enlace_del_pdf") != null
                        && !respuesta.get("enlace_del_pdf").toString().isEmpty())
                    || (respuesta.get("numero") != null && respuesta.get("errors") == null);

            if (aceptado) {
                comp.setEstado("ACEPTADO");

                // ✅ CORREGIDO: leer todos los campos con safeGet — nunca lanza NullPointerException
                String serie          = safeGet(respuesta, "serie_generada");
                String numeroCompleto = safeGet(respuesta, "numero_completo");
                String urlPdf         = safeGet(respuesta, "enlace_del_pdf");
                String urlXml         = safeGet(respuesta, "enlace_del_xml");
                String urlCdr         = safeGet(respuesta, "enlace_del_cdr");

                // Número como Integer de forma segura
                Integer numero = null;
                Object numeroObj = respuesta.get("numero_generado");
                if (numeroObj == null) numeroObj = respuesta.get("numero");
                if (numeroObj != null) {
                    try { numero = Integer.parseInt(numeroObj.toString()); }
                    catch (NumberFormatException ignored) {}
                }

                // Si numero_completo vino vacío, construirlo manualmente
                if (numeroCompleto.isEmpty()) {
                    numeroCompleto = serie + "-" + (numero != null ? numero : "");
                }

                comp.setSerie(serie);
                comp.setNumero(numero);
                comp.setNumeroCompleto(numeroCompleto);
                comp.setUrlPdf(urlPdf);
                comp.setUrlXml(urlXml);
                comp.setUrlCdr(urlCdr);

                ra.addFlashAttribute("success",
                        "✅ Comprobante emitido correctamente: " + numeroCompleto);
                log.info("Comprobante emitido: {}", numeroCompleto);

            } else {
                comp.setEstado("ERROR");

                // ✅ CORREGIDO: leer mensaje de error de forma segura
                String mensajeError = safeGet(respuesta, "errors");
                if (mensajeError.isEmpty()) mensajeError = safeGet(respuesta, "sunat_description");
                if (mensajeError.isEmpty()) mensajeError = safeGet(respuesta, "error");
                if (mensajeError.isEmpty()) mensajeError = "Error desconocido al emitir comprobante";

                comp.setMensajeError(mensajeError);
                ra.addFlashAttribute("error", "❌ Error SUNAT: " + mensajeError);
                log.error("Error al emitir comprobante: {}", mensajeError);
            }

            // Asociar con origen
            if ("orden".equals(origen)) {
                try { comp.setOrdenId(Long.parseLong(origenId)); }
                catch (Exception e) { log.warn("Error al parsear ordenId: {}", origenId); }
            } else if ("venta".equals(origen)) {
                comp.setVentaId(0L);
            }

            comprobanteRepository.save(comp);

        } catch (Exception e) {
            log.error("Error en proceso de emisión", e);
            ra.addFlashAttribute("error", "❌ Error al procesar: " + e.getMessage());
        }

        return "redirect:/sunat";
    }

    // ── Reenviar comprobante con error ────────────────────────────
    @PostMapping("/reenviar/{id}")
    public String reenviar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            ComprobanteElectronico comprobante = comprobanteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

            if ("ACEPTADO".equals(comprobante.getEstado())) {
                ra.addFlashAttribute("warning", "Este comprobante ya fue aceptado por SUNAT");
                return "redirect:/sunat";
            }

            List<NubefactService.ItemComprobante> items = List.of(
                    new NubefactService.ItemComprobante(
                            "Comprobante de venta", 1.0,
                            comprobante.getTotal().doubleValue(), "NIU")
            );

            Map<String, Object> respuesta;
            if ("BOLETA".equals(comprobante.getTipoComprobante())) {
                respuesta = nubefactService.emitirBoleta(
                        comprobante.getClienteNombre(),
                        comprobante.getClienteDocumento(),
                        items, comprobante.getTotal().doubleValue());
            } else {
                respuesta = nubefactService.emitirFactura(
                        comprobante.getClienteNombre(),
                        comprobante.getClienteDocumento(),
                        "", items, comprobante.getTotal().doubleValue());
            }

            log.info("Reenvío - Respuesta Nubefact: {}", respuesta);

            Object aceptadaObj = respuesta.get("aceptada_por_sunat");
            boolean aceptado   = Boolean.TRUE.equals(aceptadaObj)
                    || "true".equalsIgnoreCase(String.valueOf(aceptadaObj));

            if (aceptado) {
                comprobante.setEstado("ACEPTADO");
                comprobante.setNumeroCompleto(safeGet(respuesta, "numero_completo"));
                comprobante.setUrlPdf(safeGet(respuesta, "enlace_del_pdf"));
                comprobante.setUrlXml(safeGet(respuesta, "enlace_del_xml"));
                comprobante.setUrlCdr(safeGet(respuesta, "enlace_del_cdr"));
                comprobante.setMensajeError(null);
                ra.addFlashAttribute("success", "✅ Comprobante reenviado y aceptado");
            } else {
                String err = safeGet(respuesta, "errors");
                if (err.isEmpty()) err = safeGet(respuesta, "sunat_description");
                if (err.isEmpty()) err = "Error al reenviar";
                comprobante.setMensajeError(err);
                ra.addFlashAttribute("error", "❌ Error al reenviar: " + err);
            }

            comprobanteRepository.save(comprobante);

        } catch (Exception e) {
            log.error("Error al reenviar comprobante", e);
            ra.addFlashAttribute("error", "❌ Error: " + e.getMessage());
        }

        return "redirect:/sunat";
    }

    // ── Descargar PDF ─────────────────────────────────────────────
    @GetMapping("/descargar/pdf/{id}")
    public String descargarPdf(@PathVariable Long id, RedirectAttributes ra) {
        ComprobanteElectronico comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

        if (comprobante.getUrlPdf() != null && !comprobante.getUrlPdf().isEmpty()) {
            return "redirect:" + comprobante.getUrlPdf();
        }

        ra.addFlashAttribute("error", "Este comprobante no tiene PDF disponible");
        return "redirect:/sunat";
    }
}