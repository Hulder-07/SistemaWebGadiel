package com.carwash.sistema.controller;

import com.carwash.sistema.model.*;
import com.carwash.sistema.repository.*;
import com.carwash.sistema.service.CajaDiariaService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SuppressWarnings("unused")
@Controller
@RequestMapping("/ordenes")
@RequiredArgsConstructor
@Slf4j
public class OrdenController {

    private final OrdenRepository ordenRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final CajaDiariaService cajaDiariaService;  // ⬅️ Para datos de caja

    // ── Colores corporativos Carwash ──
    private static final BaseColor ROJO_OSC   = new BaseColor(139, 26, 26);
    private static final BaseColor ROJO_MED   = new BaseColor(192, 57, 43);
    private static final BaseColor ROJO_CLARO = new BaseColor(253, 237, 236);
    private static final BaseColor ROJO_ALT   = new BaseColor(255, 245, 245);
    private static final BaseColor GRIS       = new BaseColor(120, 120, 120);
    private static final BaseColor GRIS_BORDE = new BaseColor(200, 200, 200);

    // ═══════════════════════════════════════════════════════════════════════
    // PÁGINA PRINCIPAL UNIFICADA (Órdenes + Caja Lavadero)
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping
    public String index(Model model, Authentication auth) {
        model.addAttribute("activePage", "ordenes");

        // ── DATOS DE CAJA ──
        boolean hayCajaAbierta = cajaDiariaService.hayCajaAbierta();
        model.addAttribute("hayCajaAbierta", hayCajaAbierta);

        if (hayCajaAbierta) {
            CajaDiaria cajaAbierta = cajaDiariaService.getCajaAbierta().orElse(null);
            List<RegistroLavado> registros = cajaDiariaService.getRegistrosCajaActual();

            model.addAttribute("cajaAbierta", cajaAbierta);
            model.addAttribute("registros", registros);
        }

        // ── DATOS DE ÓRDENES ──
        List<OrdenServicio> ordenes = ordenRepository.findTop15ByOrderByFechaInicioDesc();
        model.addAttribute("ordenes", ordenes);
        model.addAttribute("estadoFiltro", null);
        model.addAttribute("pendientes", ordenRepository.countByEstado("PENDIENTE"));
        model.addAttribute("enProceso", ordenRepository.countByEstado("EN_PROCESO"));
        model.addAttribute("completados", ordenRepository.countByEstado("COMPLETADO"));

        // ── HISTORIAL DE CAJAS CERRADAS ──
        List<CajaDiaria> historial = cajaDiariaService.getHistorialCajas(10);
        model.addAttribute("historial", historial);

        return "ordenes/index";
    }
    
    // ==============================================================
// LISTAR ÓRDENES CON FILTRO DE ESTADO (para la vista de caja)
// ==============================================================
@GetMapping("/caja-ordenes")
public String listarPorEstadoDesdeCaja(
        @RequestParam(required = false) String estado,
        Model model,
        Authentication auth) {
    
    // Necesitas inyectar CajaDiariaService si no lo tienes
    // @Autowired private CajaDiariaService cajaDiariaService;
    
    boolean hayCajaAbierta = cajaDiariaService.hayCajaAbierta();
    model.addAttribute("hayCajaAbierta", hayCajaAbierta);
    
    if (hayCajaAbierta) {
        CajaDiaria cajaAbierta = cajaDiariaService.getCajaAbierta().orElse(null);
        model.addAttribute("cajaAbierta", cajaAbierta);
        model.addAttribute("registros", cajaDiariaService.getRegistrosCajaActual());
    }
    
    List<OrdenServicio> ordenes;
    if (estado != null && !estado.isEmpty()) {
        ordenes = ordenRepository.findByEstado(estado);
        model.addAttribute("estadoFiltro", estado);
    } else {
        ordenes = ordenRepository.findTop15ByOrderByFechaInicioDesc();
        model.addAttribute("estadoFiltro", null);
    }
    
    model.addAttribute("ordenes", ordenes);
    model.addAttribute("activePage", "ordenes");
    model.addAttribute("pendientes", ordenRepository.countByEstado("PENDIENTE"));
    model.addAttribute("enProceso", ordenRepository.countByEstado("EN_PROCESO"));
    model.addAttribute("completados", ordenRepository.countByEstado("COMPLETADO"));
    
    List<CajaDiaria> historial = cajaDiariaService.getHistorialCajas(10);
    model.addAttribute("historial", historial);
    
    return "caja-lavadero/index";
}

    // ═══════════════════════════════════════════════════════════════════════
    // NUEVA ORDEN - FORMULARIO
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/nueva")
    public String formNueva(Model model) {
        model.addAttribute("activePage", "nueva-orden");
        model.addAttribute("vehiculos",  vehiculoRepository.findAll());
        model.addAttribute("servicios",  servicioRepository.findByActivoTrue());
        model.addAttribute("clientes",   clienteRepository.findByActivoTrue());
        return "ordenes/form";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VER DETALLE DE ORDEN
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/detalle")
    public String verDetalle(@PathVariable Long id, Model model) {
        OrdenServicio orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        model.addAttribute("activePage", "ordenes");
        model.addAttribute("orden", orden);
        return "ordenes/detalle";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GUARDAR ORDEN
    // ═══════════════════════════════════════════════════════════════════════
    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long vehiculoId,
                          @RequestParam(required = false) String placaInput,
                          @RequestParam(required = false) String marcaManual,
                          @RequestParam(required = false) String modeloManual,
                          @RequestParam(required = false) String clienteManual,
                          @RequestParam Long servicioId,
                          @RequestParam(required = false) String observaciones,
                          RedirectAttributes ra) {

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        Vehiculo vehiculo;

        if (vehiculoId != null) {
            vehiculo = vehiculoRepository.findById(vehiculoId)
                    .orElseThrow(() -> new RuntimeException("Vehículo no encontrado"));
        } else {
            String placa = placaInput != null ? placaInput.trim().toUpperCase() : "";
            if (placa.isEmpty()) {
                ra.addFlashAttribute("error", "Ingresa la placa del vehículo.");
                return "redirect:/ordenes/nueva";
            }

            vehiculo = vehiculoRepository.findFirstByPlaca(placa).orElse(null);
            if (vehiculo == null) {
                Vehiculo v = new Vehiculo();
                v.setPlaca(placa);
                v.setMarca(marcaManual != null && !marcaManual.isBlank() ? marcaManual.trim() : "Sin marca");
                v.setModelo(modeloManual != null && !modeloManual.isBlank() ? modeloManual.trim() : "Sin modelo");
                if (clienteManual != null && !clienteManual.isBlank()) {
                    Cliente c = new Cliente();
                    c.setNombres(clienteManual.trim());
                    c.setDni(String.valueOf(System.currentTimeMillis()).substring(5));
                    c.setActivo(true);
                    clienteRepository.save(c);
                    v.setCliente(c);
                }
                vehiculo = vehiculoRepository.save(v);
            } else {
                if (clienteManual != null && !clienteManual.isBlank() && vehiculo.getCliente() == null) {
                    Cliente c = new Cliente();
                    c.setNombres(clienteManual.trim());
                    c.setDni(String.valueOf(System.currentTimeMillis()).substring(5));
                    c.setActivo(true);
                    clienteRepository.save(c);
                    vehiculo.setCliente(c);
                    vehiculo = vehiculoRepository.save(vehiculo);
                }
            }
        }
        BigDecimal precioFinal = servicio.getPrecio();
        if (vehiculo.getTipo() != null && !"moto".equalsIgnoreCase(servicio.getCategoria())) {
            String t = vehiculo.getTipo().toUpperCase();
            if (t.contains("AUTO") || t.contains("SEDAN")) {
                if (servicio.getPrecioAuto() != null) precioFinal = servicio.getPrecioAuto();
            } else if (t.contains("CAMIONETA") || t.contains("SUV") || t.contains("PICKUP")) {
                if (servicio.getPrecioCamioneta() != null) precioFinal = servicio.getPrecioCamioneta();
            } else if (t.contains("VAN") || t.contains("BUS") || t.contains("CAMION")) {
                if (servicio.getPrecioVan() != null) precioFinal = servicio.getPrecioVan();
            } else {
                 if (servicio.getPrecioAuto() != null) precioFinal = servicio.getPrecioAuto();
            }
        } else if (!"moto".equalsIgnoreCase(servicio.getCategoria())) {
            if (servicio.getPrecioAuto() != null) precioFinal = servicio.getPrecioAuto();
        }

        OrdenServicio orden = new OrdenServicio();
        orden.setVehiculo(vehiculo);
        orden.setServicio(servicio);
        orden.setTotal(precioFinal != null ? precioFinal : BigDecimal.ZERO);
        orden.setObservaciones(observaciones);
        orden.setEstado("PENDIENTE");
        ordenRepository.save(orden);

        ra.addFlashAttribute("msg", "Orden creada: " + orden.getNumeroOrden());
        return "redirect:/ordenes/" + orden.getId() + "/detalle";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CAMBIAR ESTADO DE ORDEN
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/cambiar-estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam String estado,
                                RedirectAttributes ra) {
        ordenRepository.findById(id).ifPresent(o -> {
            o.setEstado(estado);
            if ("COMPLETADO".equals(estado) && o.getFechaFin() == null)
                o.setFechaFin(LocalDateTime.now());
            ordenRepository.save(o);
        });
        ra.addFlashAttribute("msg", "Estado actualizado.");
        return "redirect:/ordenes/" + id + "/detalle";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ELIMINAR ORDEN
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        ordenRepository.deleteById(id);
        ra.addFlashAttribute("msg", "Orden eliminada.");
        return "redirect:/ordenes";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PDF DE ORDEN
    // ═══════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/pdf")
    public void generarPdf(@PathVariable Long id, HttpServletResponse response)
            throws IOException, DocumentException {

        OrdenServicio orden = ordenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=orden_" + orden.getNumeroOrden() + ".pdf");

        com.itextpdf.text.Font fEmp  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD,   BaseColor.WHITE);
        com.itextpdf.text.Font fSub  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  8, com.itextpdf.text.Font.NORMAL, new BaseColor(255, 200, 200));
        com.itextpdf.text.Font fBox  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD,   ROJO_OSC);
        com.itextpdf.text.Font fBoxS = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.NORMAL, GRIS);
        com.itextpdf.text.Font fLbl  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.BOLD,   GRIS);
        com.itextpdf.text.Font fVal  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.Font fTot  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD,   ROJO_OSC);
        com.itextpdf.text.Font fPie  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  7, com.itextpdf.text.Font.ITALIC, GRIS);
        com.itextpdf.text.Font fDatosW = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        String fecha = orden.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora  = orden.getFechaInicio().format(DateTimeFormatter.ofPattern("HH:mm"));

        // CABECERA
        PdfPTable header = new PdfPTable(new float[]{2.5f, 1.5f});
        header.setWidthPercentage(100); header.setSpacingAfter(15);

        PdfPTable izq = new PdfPTable(1); izq.setWidthPercentage(100);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(BaseColor.BLACK); logoCell.setPadding(8); logoCell.setBorder(Rectangle.NO_BORDER); logoCell.setHorizontalAlignment(Element.ALIGN_CENTER); logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image img = Image.getInstance(new org.springframework.core.io.ClassPathResource("static/images/logo.jpeg").getURL());
            img.scaleToFit(100, 80);
            img.setAlignment(Element.ALIGN_CENTER);
            logoCell.addElement(img);
        } catch(Exception e) {
            Paragraph pEmp = new Paragraph("Lubricentro Car Detailing Gadiel", fEmp); pEmp.setAlignment(Element.ALIGN_CENTER); logoCell.addElement(pEmp);
            Paragraph pSubEmp = new Paragraph("Sistema de Gestión Carwash", fSub); pSubEmp.setAlignment(Element.ALIGN_CENTER); logoCell.addElement(pSubEmp);
        }
        izq.addCell(logoCell);

        PdfPCell datosCell = new PdfPCell(); datosCell.setBorder(Rectangle.NO_BORDER); datosCell.setPadding(10);
        datosCell.setBackgroundColor(BaseColor.BLACK);
        Paragraph pDatos = new Paragraph();
        pDatos.add(new Chunk("Lubricentro Car Detailing Gadiel\n", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, ROJO_OSC)));
        pDatos.add(new Chunk("Sistema de Lavado y Tienda\nGenerado: " + fecha + " " + hora + "\n", fDatosW));
        datosCell.addElement(pDatos); izq.addCell(datosCell);

        PdfPCell izqWrap = new PdfPCell(izq); izqWrap.setBorder(Rectangle.NO_BORDER); izqWrap.setPadding(0);
        header.addCell(izqWrap);

        PdfPCell derCell = new PdfPCell();
        derCell.setBorder(Rectangle.NO_BORDER); derCell.setPadding(12); derCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pTipo = new Paragraph("ORDEN DE SERVICIO", fBox); pTipo.setAlignment(Element.ALIGN_RIGHT); derCell.addElement(pTipo);
        Paragraph pNum = new Paragraph("\nN° " + orden.getNumeroOrden(), new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, ROJO_OSC));
        pNum.setAlignment(Element.ALIGN_RIGHT); derCell.addElement(pNum);
        Paragraph pFech = new Paragraph("Fecha: " + fecha, fBoxS); pFech.setAlignment(Element.ALIGN_RIGHT); pFech.setSpacingBefore(4); derCell.addElement(pFech);
        header.addCell(derCell);
        doc.add(header);

        // DATOS VEHÍCULO Y CLIENTE
        PdfPTable info = new PdfPTable(new float[]{1.5f, 4f});
        info.setWidthPercentage(100); info.setSpacingAfter(8);
        addInfoRow(info, "Placa",     orden.getVehiculo().getPlaca(), fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        addInfoRow(info, "Vehículo",  orden.getVehiculo().getMarca() + " " + orden.getVehiculo().getModelo(), fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        addInfoRow(info, "Cliente",   orden.getVehiculo().getCliente() != null ? orden.getVehiculo().getCliente().getNombres() : "—", fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        addInfoRow(info, "Fecha",     fecha + "  " + hora, fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        addInfoRow(info, "Estado",    orden.getEstado(), fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        doc.add(info);

        // TABLA SERVICIO
        com.itextpdf.text.Font fH = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        com.itextpdf.text.Font fR = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);

        PdfPTable tabla = new PdfPTable(new float[]{0.6f, 3f, 2f, 1.8f});
        tabla.setWidthPercentage(100); tabla.setSpacingBefore(4);
        for (String h : new String[]{"CANT.", "DESCRIPCIÓN", "CATEGORÍA", "IMPORTE (S/)"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(BaseColor.BLACK); c.setPadding(6); c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorderColor(BaseColor.BLACK);
            tabla.addCell(c);
        }
        addTd(tabla, "1",                                          fR, ROJO_ALT,      Element.ALIGN_CENTER, GRIS_BORDE);
        addTd(tabla, orden.getServicio().getNombre().toUpperCase() + (orden.getObservaciones() != null && !orden.getObservaciones().isBlank() ? "\n" + orden.getObservaciones() : ""),
              fR, ROJO_ALT, Element.ALIGN_LEFT, GRIS_BORDE);
        addTd(tabla, orden.getServicio().getCategoria() != null ? orden.getServicio().getCategoria().toUpperCase() : "—",
              fR, ROJO_ALT, Element.ALIGN_CENTER, GRIS_BORDE);
        addTd(tabla, String.format("%.2f", orden.getTotal().doubleValue()), fR, ROJO_ALT, Element.ALIGN_RIGHT, GRIS_BORDE);
        for (int i = 1; i < 8; i++) for (int j = 0; j < 4; j++) {
            PdfPCell c = new PdfPCell(new Phrase(" ")); c.setFixedHeight(18f); c.setBorderColor(GRIS_BORDE); tabla.addCell(c);
        }
        doc.add(tabla);

        // TOTALES
        double dTotal  = orden.getTotal().doubleValue();
        double gravada = dTotal / 1.18;
        double igv     = dTotal - gravada;

        PdfPTable pie = new PdfPTable(new float[]{3f, 1.5f});
        pie.setWidthPercentage(40); pie.setHorizontalAlignment(Element.ALIGN_RIGHT); pie.setSpacingBefore(8);
        addPieRow(pie, "OP. GRAVADA (S/):",   String.format("%.2f", gravada), fLbl, fVal);
        addPieRow(pie, "TOTAL IGV (S/):",     String.format("%.2f", igv),     fLbl, fVal);
        
        PdfPCell footL = new PdfPCell(new Phrase("IMPORTE TOTAL (S/):", fTot)); 
        footL.setBackgroundColor(BaseColor.BLACK); footL.setHorizontalAlignment(Element.ALIGN_RIGHT); footL.setPadding(6); pie.addCell(footL);
        PdfPCell footR = new PdfPCell(new Phrase(String.format("%.2f", dTotal), fTot)); 
        footR.setHorizontalAlignment(Element.ALIGN_RIGHT); footR.setPadding(6); footR.setBackgroundColor(BaseColor.WHITE); pie.addCell(footR);
        doc.add(pie);

        // PIE
        Paragraph pPie = new Paragraph("Lubricentro Car Detailing Gadiel — Orden " + orden.getNumeroOrden() + " — " + fecha + " " + hora, fPie);
        pPie.setAlignment(Element.ALIGN_CENTER); pPie.setSpacingBefore(20);
        doc.add(pPie);
        doc.close();
    }

    // ── MÉTODOS AUXILIARES ──
    private void addInfoRow(PdfPTable t, String label, String valor,
                             com.itextpdf.text.Font fl, com.itextpdf.text.Font fv,
                             BaseColor bg, BaseColor border) {
        PdfPCell lc = new PdfPCell(new Phrase(label + " :", fl)); lc.setBackgroundColor(bg); lc.setBorderColor(border); lc.setPadding(4);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fv)); vc.setBorderColor(border); vc.setPadding(4);
        t.addCell(lc); t.addCell(vc);
    }

    private void addTd(PdfPTable t, String txt, com.itextpdf.text.Font f, BaseColor bg, int align, BaseColor border) {
        PdfPCell c = new PdfPCell(new Phrase(txt, f));
        c.setBackgroundColor(bg); c.setPadding(6); c.setHorizontalAlignment(align); c.setVerticalAlignment(Element.ALIGN_MIDDLE); c.setBorderColor(border);
        t.addCell(c);
    }

    private void addPieRow(PdfPTable t, String label, String valor,
                            com.itextpdf.text.Font fl, com.itextpdf.text.Font fv) {
        PdfPCell lc = new PdfPCell(new Phrase(label, fl)); lc.setBorder(Rectangle.NO_BORDER); lc.setHorizontalAlignment(Element.ALIGN_RIGHT); lc.setPadding(3);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fv)); vc.setBorder(Rectangle.NO_BORDER); vc.setHorizontalAlignment(Element.ALIGN_RIGHT); vc.setPadding(3);
        t.addCell(lc); t.addCell(vc);
    }
}