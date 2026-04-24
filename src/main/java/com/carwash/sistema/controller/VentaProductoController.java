package com.carwash.sistema.controller;

import com.carwash.sistema.model.*;
import com.carwash.sistema.repository.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaProductoController {

    private final VentaProductoRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;  // ⬅️ AGREGADO

    private static final BaseColor ROJO_OSC   = new BaseColor(139, 26, 26);
    private static final BaseColor ROJO_MED   = new BaseColor(192, 57, 43);
    private static final BaseColor ROJO_CLARO = new BaseColor(253, 237, 236);
    private static final BaseColor ROJO_ALT   = new BaseColor(255, 245, 245);
    private static final BaseColor GRIS       = new BaseColor(120, 120, 120);
    private static final BaseColor GRIS_BORDE = new BaseColor(200, 200, 200);

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "ventas");
        model.addAttribute("ventas", ventaRepository.findTop15ByOrderByFechaDesc());
        return "ventas/lista";
    }

    @GetMapping("/nueva")
    public String nuevaVenta(Model model) {
        model.addAttribute("productos",       productoRepository.findByActivoTrue());
        model.addAttribute("categorias",      categoriaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("categoriaActual", null);
        return "ventas/form";
    }

    @GetMapping("/nueva/categoria/{id}")
    public String nuevaVentaCategoria(@PathVariable Long id, Model model) {
        model.addAttribute("productos",       productoRepository.findByActivoTrueAndCategoriaId(id));
        model.addAttribute("categorias",      categoriaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("categoriaActual", id);
        return "ventas/form";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Long productoId,
                          @RequestParam Integer cantidad,
                          @RequestParam(required = false) String cliente,
                          RedirectAttributes ra) {

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getStock() < cantidad) {
            ra.addFlashAttribute("error", "Stock insuficiente. Disponible: " + producto.getStock());
            return "redirect:/ventas/nueva";
        }

        String boleta = "BOL-" + System.currentTimeMillis();

        VentaProducto venta = new VentaProducto();
        venta.setProducto(producto);
        venta.setCantidad(cantidad);
        venta.setPrecioUnitario(producto.getPrecio());
        venta.setTotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        venta.setCliente(cliente != null && !cliente.isBlank() ? cliente : "Cliente general");
        venta.setFecha(LocalDateTime.now());
        venta.setNumeroBoleta(boleta);
        ventaRepository.save(venta);

        producto.setStock(producto.getStock() - cantidad);
        productoRepository.save(producto);

        return "redirect:/ventas/boleta/" + boleta;
    }

    @GetMapping("/boleta/{numeroBoleta}")
    public String verBoleta(@PathVariable String numeroBoleta, Model model) {
        List<VentaProducto> ventas = ventaRepository.findByNumeroBoleta(numeroBoleta);
        if (ventas.isEmpty()) return "redirect:/ventas";
        BigDecimal total = ventas.stream().map(VentaProducto::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("activePage", "ventas");
        model.addAttribute("ventas",       ventas);
        model.addAttribute("numeroBoleta", numeroBoleta);
        model.addAttribute("total",        total);
        model.addAttribute("cliente",      ventas.get(0).getCliente());
        model.addAttribute("fecha",        ventas.get(0).getFecha());
        return "ventas/boleta";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        ventaRepository.deleteById(id);
        ra.addFlashAttribute("msg", "Venta eliminada.");
        return "redirect:/ventas";
    }

    // ── PDF Boleta de Venta ──
    @GetMapping("/boleta/{numeroBoleta}/pdf")
    public void generarBoletaPDF(@PathVariable String numeroBoleta, HttpServletResponse response)
            throws IOException, DocumentException {

        List<VentaProducto> ventas = ventaRepository.findByNumeroBoleta(numeroBoleta);
        if (ventas.isEmpty()) return;

        BigDecimal total = ventas.stream().map(VentaProducto::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String cliente   = ventas.get(0).getCliente() != null ? ventas.get(0).getCliente() : "Cliente general";
        String fecha     = ventas.get(0).getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora      = ventas.get(0).getFecha().format(DateTimeFormatter.ofPattern("HH:mm"));

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=boleta_" + numeroBoleta + ".pdf");

        com.itextpdf.text.Font fEmp  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD,   BaseColor.WHITE);
        com.itextpdf.text.Font fSub  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  8, com.itextpdf.text.Font.NORMAL, new BaseColor(255, 200, 200));
        com.itextpdf.text.Font fBox  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD,   ROJO_OSC);
        com.itextpdf.text.Font fBoxS = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.NORMAL, GRIS);
        com.itextpdf.text.Font fLbl  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.BOLD,   GRIS);
        com.itextpdf.text.Font fVal  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.Font fH    = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  8, com.itextpdf.text.Font.BOLD,   BaseColor.WHITE);
        com.itextpdf.text.Font fRow  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  8);
        com.itextpdf.text.Font fTot  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD,   ROJO_OSC);
        com.itextpdf.text.Font fPie  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  7, com.itextpdf.text.Font.ITALIC, GRIS);
        com.itextpdf.text.Font fDatosW = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

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
            Paragraph pS = new Paragraph("Lavado & Tienda Automotriz", fSub); pS.setAlignment(Element.ALIGN_CENTER); logoCell.addElement(pS);
        }
        izq.addCell(logoCell);

        PdfPCell datosCell = new PdfPCell(); datosCell.setBorder(Rectangle.NO_BORDER); datosCell.setPadding(10);
        datosCell.setBackgroundColor(BaseColor.BLACK);
        Paragraph pDatos = new Paragraph();
        pDatos.add(new Chunk("Lubricentro Car Detailing Gadiel\n", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, ROJO_OSC)));
        pDatos.add(new Chunk("Generado: " + fecha + " " + hora + "\n", fDatosW));
        datosCell.addElement(pDatos); izq.addCell(datosCell);

        PdfPCell izqWrap = new PdfPCell(izq); izqWrap.setBorder(Rectangle.NO_BORDER); izqWrap.setBorderColor(GRIS_BORDE); izqWrap.setPadding(0);
        header.addCell(izqWrap);

        PdfPCell derCell = new PdfPCell(); derCell.setBorder(Rectangle.NO_BORDER); derCell.setBorderColor(GRIS_BORDE); derCell.setPadding(12); derCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pTipo = new Paragraph("BOLETA DE VENTA", fBox); pTipo.setAlignment(Element.ALIGN_RIGHT); derCell.addElement(pTipo);
        Paragraph pNum = new Paragraph("\nN° " + numeroBoleta, new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, ROJO_OSC));
        pNum.setAlignment(Element.ALIGN_RIGHT); derCell.addElement(pNum);
        Paragraph pFechaBox = new Paragraph("Fecha: " + fecha, fBoxS); pFechaBox.setAlignment(Element.ALIGN_RIGHT); pFechaBox.setSpacingBefore(4); derCell.addElement(pFechaBox);
        header.addCell(derCell);
        doc.add(header);

        // DATOS CLIENTE
        PdfPTable infoCliente = new PdfPTable(new float[]{1.2f, 4f});
        infoCliente.setWidthPercentage(100); infoCliente.setSpacingAfter(8);
        addInfoRow(infoCliente, "Cliente", cliente,       fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        addInfoRow(infoCliente, "Fecha",   fecha + "  " + hora, fLbl, fVal, ROJO_CLARO, GRIS_BORDE);
        doc.add(infoCliente);

        // TABLA PRODUCTOS
        PdfPTable tabla = new PdfPTable(new float[]{0.8f, 1f, 3f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100); tabla.setSpacingBefore(4);
        for (String h : new String[]{"CANTIDAD", "UNID.", "DESCRIPCIÓN", "PRECIO UNIT.", "IMPORTE"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(BaseColor.BLACK); c.setPadding(6); c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorderColor(BaseColor.BLACK);
            tabla.addCell(c);
        }
        boolean alt = false;
        for (VentaProducto v : ventas) {
            BaseColor bg = alt ? ROJO_ALT : BaseColor.WHITE;
            String unidad = v.getProducto().getUnidad() != null ? v.getProducto().getUnidad().toUpperCase() : "UND";
            addTd(tabla, String.valueOf(v.getCantidad()),  fRow, bg, Element.ALIGN_CENTER, GRIS_BORDE);
            addTd(tabla, unidad, fRow, bg, Element.ALIGN_CENTER, GRIS_BORDE);
            addTd(tabla, v.getProducto().getNombre().toUpperCase(), fRow, bg, Element.ALIGN_LEFT, GRIS_BORDE);
            addTd(tabla, String.format("%.2f", v.getPrecioUnitario().doubleValue()), fRow, bg, Element.ALIGN_RIGHT, GRIS_BORDE);
            addTd(tabla, String.format("%.2f", v.getTotal().doubleValue()), fRow, bg, Element.ALIGN_RIGHT, GRIS_BORDE);
            alt = !alt;
        }
        for (int i = ventas.size(); i < 8; i++) for (int j = 0; j < 5; j++) {
            PdfPCell c = new PdfPCell(new Phrase(" ")); c.setFixedHeight(18f); c.setBorderColor(GRIS_BORDE); tabla.addCell(c);
        }
        doc.add(tabla);

        // TOTALES
        double dTotal  = total.doubleValue();
        double gravada = dTotal / 1.18;
        double igv     = dTotal - gravada;
        PdfPTable pie = new PdfPTable(new float[]{3f, 1.5f});
        pie.setWidthPercentage(45); pie.setHorizontalAlignment(Element.ALIGN_RIGHT); pie.setSpacingBefore(8);
        addPieRow(pie, "OP. GRAVADA (S/):",   String.format("%.2f", gravada), fLbl, fVal);
        addPieRow(pie, "TOTAL IGV (S/):",     String.format("%.2f", igv),     fLbl, fVal);
        PdfPCell footL = new PdfPCell(new Phrase("IMPORTE TOTAL (S/):", fTot)); 
        footL.setBackgroundColor(BaseColor.BLACK); footL.setHorizontalAlignment(Element.ALIGN_RIGHT); footL.setPadding(6); pie.addCell(footL);
        PdfPCell footR = new PdfPCell(new Phrase(String.format("%.2f", dTotal), fTot)); 
        footR.setHorizontalAlignment(Element.ALIGN_RIGHT); footR.setPadding(6); footR.setBackgroundColor(BaseColor.WHITE); pie.addCell(footR);
        doc.add(pie);

        Paragraph pPie = new Paragraph("Lubricentro Car Detailing Gadiel — Boleta " + numeroBoleta + " — " + fecha + " " + hora, fPie);
        pPie.setAlignment(Element.ALIGN_CENTER); pPie.setSpacingBefore(20);
        doc.add(pPie);
        doc.close();
    }

    private void addInfoRow(PdfPTable t, String label, String valor,
                             com.itextpdf.text.Font fl, com.itextpdf.text.Font fv,
                             BaseColor bg, BaseColor border) {
        PdfPCell lc = new PdfPCell(new Phrase(label + " :", fl)); lc.setBackgroundColor(bg); lc.setBorderColor(border); lc.setPadding(4);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fv)); vc.setBorderColor(border); vc.setPadding(4);
        t.addCell(lc); t.addCell(vc);
    }

    private void addTd(PdfPTable t, String txt, com.itextpdf.text.Font f, BaseColor bg, int align, BaseColor border) {
        PdfPCell c = new PdfPCell(new Phrase(txt, f));
        c.setBackgroundColor(bg); c.setPadding(5); c.setHorizontalAlignment(align); c.setVerticalAlignment(Element.ALIGN_MIDDLE); c.setBorderColor(border);
        t.addCell(c);
    }

    private void addPieRow(PdfPTable t, String label, String valor,
                            com.itextpdf.text.Font fl, com.itextpdf.text.Font fv) {
        PdfPCell lc = new PdfPCell(new Phrase(label, fl)); lc.setBorder(Rectangle.NO_BORDER); lc.setHorizontalAlignment(Element.ALIGN_RIGHT); lc.setPadding(3);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fv)); vc.setBorder(Rectangle.NO_BORDER); vc.setHorizontalAlignment(Element.ALIGN_RIGHT); vc.setPadding(3);
        t.addCell(lc); t.addCell(vc);
    }
}