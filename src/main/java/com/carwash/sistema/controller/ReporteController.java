package com.carwash.sistema.controller;

import com.carwash.sistema.model.OrdenServicio;
import com.carwash.sistema.model.Producto;
import com.carwash.sistema.model.VentaProducto;
import com.carwash.sistema.repository.OrdenRepository;
import com.carwash.sistema.repository.ProductoRepository;
import com.carwash.sistema.repository.VentaProductoRepository;
import com.carwash.sistema.service.CajaDiariaService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final OrdenRepository ordenRepository;
    private final VentaProductoRepository ventaProductoRepository;
    private final ProductoRepository productoRepository;
    private final CajaDiariaService cajaDiariaService;
    private final com.carwash.sistema.repository.CajaDiariaRepository cajaDiariaRepository;
    private final com.carwash.sistema.repository.RegistroLavadoRepository registroLavadoRepository;

    private static final String EMPRESA      = "Lubricentro Car Detailing Gadiel";
    private static final BaseColor ROJO_OSC  = new BaseColor(139, 26, 26);
    private static final BaseColor ROJO_MED  = new BaseColor(192, 57, 43);
    private static final BaseColor ROJO_CLARO= new BaseColor(253, 237, 236);
    private static final BaseColor ROJO_ALT  = new BaseColor(255, 245, 245);
    private static final BaseColor ROJO_ALERT= new BaseColor(255, 235, 235);
    private static final BaseColor GRIS      = new BaseColor(120, 120, 120);
    private static final BaseColor GRIS_BORDE= new BaseColor(200, 200, 200);
    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Helper: nombre de categoría seguro ──────────────────
    private String catNombre(Producto p) {
        return p.getCategoria() != null ? p.getCategoria().getNombre() : "-";
    }

    @GetMapping
    public String verReportes(Model model) {
        model.addAttribute("activePage",     "reportes");
        model.addAttribute("totalOrdenes",   ordenRepository.count());
        model.addAttribute("totalProductos", productoRepository.countByActivoTrue());
        model.addAttribute("ordenes",        ordenRepository.findTop15ByOrderByFechaInicioDesc());
        model.addAttribute("ventas",         ventaProductoRepository.findTop15ByOrderByFechaDesc());
        model.addAttribute("productos",      productoRepository.findByActivoTrue());
        return "reportes/index";
    }

    // ── Helpers comunes ──────────────────────────────────────
    private List<OrdenServicio> getOrdenesPeriodo(String periodo) {
        LocalDateTime ahora  = LocalDateTime.now();
        LocalDateTime inicio = switch (periodo) {
            case "diario"  -> LocalDate.now().atStartOfDay();
            case "semanal" -> LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
            case "mensual" -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
            default        -> LocalDate.now().atStartOfDay();
        };
        return ordenRepository.findByFechaBetween(inicio, ahora.plusDays(1));
    }

    private List<VentaProducto> getVentasPeriodo(String periodo) {
        LocalDateTime ahora  = LocalDateTime.now();
        LocalDateTime inicio = switch (periodo) {
            case "diario"  -> LocalDate.now().atStartOfDay();
            case "semanal" -> LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
            case "mensual" -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
            default        -> LocalDate.now().atStartOfDay();
        };
        return ventaProductoRepository.findByFechaBetween(inicio, ahora.plusDays(1));
    }

    private String describePeriodo(String periodo) {
        java.util.Locale es = java.util.Locale.forLanguageTag("es");
        return switch (periodo) {
            case "diario"  -> "Hoy " + LocalDate.now().format(FMT);
            case "semanal" -> "Semana del " + LocalDate.now().with(java.time.DayOfWeek.MONDAY).format(FMT) + " al " + LocalDate.now().format(FMT);
            case "mensual" -> "Mes de " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", es));
            default        -> LocalDate.now().format(FMT);
        };
    }

    private Document crearDocumentoA4(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        return new Document(PageSize.A4, 36, 36, 36, 36);
    }

    private void agregarCabecera(Document doc, String tipo, String periodo) throws DocumentException {
        com.itextpdf.text.Font fEmp  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD,   BaseColor.WHITE);
        com.itextpdf.text.Font fSub  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  8, com.itextpdf.text.Font.NORMAL, new BaseColor(255,200,200));
        com.itextpdf.text.Font fBox  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD,   ROJO_OSC);
        com.itextpdf.text.Font fBoxS = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,  9, com.itextpdf.text.Font.NORMAL, GRIS);
        com.itextpdf.text.Font fDatosW = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL, BaseColor.WHITE);

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
            Paragraph logoTxt = new Paragraph(EMPRESA, fEmp); logoTxt.setAlignment(Element.ALIGN_CENTER); logoCell.addElement(logoTxt);
            Paragraph subTxt  = new Paragraph("Lavado & Tienda Automotriz", fSub); subTxt.setAlignment(Element.ALIGN_CENTER); logoCell.addElement(subTxt);
        }
        izq.addCell(logoCell);

        PdfPCell datosCell = new PdfPCell(); datosCell.setBorder(Rectangle.NO_BORDER); datosCell.setPadding(10);
        datosCell.setBackgroundColor(BaseColor.BLACK);
        Paragraph datos = new Paragraph();
        datos.add(new Chunk(EMPRESA + "\n", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, ROJO_OSC)));
        datos.add(new Chunk("Generado: " + LocalDate.now().format(FMT) + "\n", fDatosW));
        datosCell.addElement(datos); izq.addCell(datosCell);

        PdfPCell izqWrap = new PdfPCell(izq); izqWrap.setBorder(Rectangle.NO_BORDER); izqWrap.setPadding(0);
        header.addCell(izqWrap);

        PdfPCell derCell = new PdfPCell(); derCell.setBorder(Rectangle.NO_BORDER); derCell.setPadding(12); derCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph tipoP = new Paragraph(tipo, fBox); tipoP.setAlignment(Element.ALIGN_RIGHT); derCell.addElement(tipoP);
        Paragraph perP  = new Paragraph(periodo, fBoxS); perP.setAlignment(Element.ALIGN_RIGHT); derCell.addElement(perP);
        Paragraph fechP = new Paragraph("Fecha: " + LocalDate.now().format(FMT),
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, BaseColor.BLACK));
        fechP.setAlignment(Element.ALIGN_RIGHT); fechP.setSpacingBefore(6); derCell.addElement(fechP);
        header.addCell(derCell);
        doc.add(header);
    }

    private void agregarPie(Document doc) throws DocumentException {
        com.itextpdf.text.Font f = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 7.5f, com.itextpdf.text.Font.ITALIC, GRIS);
        Paragraph p = new Paragraph("Reporte generado por " + EMPRESA + " - " + LocalDateTime.now().format(FMT_FULL), f);
        p.setAlignment(Element.ALIGN_CENTER); p.setSpacingBefore(16);
        doc.add(p);
    }

    private void addInfoRow(PdfPTable t, String label, String valor,
                             com.itextpdf.text.Font fl, com.itextpdf.text.Font fv) {
        PdfPCell lc = new PdfPCell(new Phrase(label + " :", fl)); lc.setBackgroundColor(ROJO_CLARO); lc.setBorderColor(GRIS_BORDE); lc.setPadding(4);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fv)); vc.setBorderColor(GRIS_BORDE); vc.setPadding(4);
        t.addCell(lc); t.addCell(vc);
    }

    private void addTd(PdfPTable t, String txt, com.itextpdf.text.Font f, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(txt != null ? txt : "", f));
        c.setBackgroundColor(bg); c.setPadding(5); c.setHorizontalAlignment(align); c.setBorderColor(GRIS_BORDE);
        t.addCell(c);
    }

    private void addPieRow(PdfPTable t, String label, String valor,
                            com.itextpdf.text.Font fl, com.itextpdf.text.Font fv) {
        PdfPCell lc = new PdfPCell(new Phrase(label, fl)); lc.setBorder(Rectangle.NO_BORDER); lc.setHorizontalAlignment(Element.ALIGN_RIGHT); lc.setPadding(3);
        PdfPCell vc = new PdfPCell(new Phrase(valor, fv)); vc.setBorder(Rectangle.NO_BORDER); vc.setHorizontalAlignment(Element.ALIGN_RIGHT); vc.setPadding(3);
        t.addCell(lc); t.addCell(vc);
    }

    // ── PDF ORDENES ──────────────────────────────────────────
    @GetMapping("/pdf/ordenes")
    public void pdfOrdenes(@RequestParam(defaultValue = "mensual") String periodo,
                           HttpServletResponse response) throws IOException, DocumentException {
        List<OrdenServicio> ordenes = getOrdenesPeriodo(periodo);
        String desc  = describePeriodo(periodo);
        double total = ordenes.stream().filter(o -> "COMPLETADO".equals(o.getEstado()))
                .mapToDouble(o -> o.getTotal().doubleValue()).sum();

        Document doc = crearDocumentoA4(response, "ordenes_" + periodo + ".pdf");
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        agregarCabecera(doc, "REPORTE DE SERVICIOS", desc);

        com.itextpdf.text.Font fLbl = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, GRIS);
        com.itextpdf.text.Font fVal = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
        PdfPTable info = new PdfPTable(new float[]{1.5f, 4f}); info.setWidthPercentage(100); info.setSpacingAfter(8);
        addInfoRow(info, "Periodo",    desc,                            fLbl, fVal);
        addInfoRow(info, "N Ordenes",  String.valueOf(ordenes.size()),  fLbl, fVal);
        addInfoRow(info, "Total",      "S/ " + String.format("%.2f", total), fLbl, fVal);
        doc.add(info);

        com.itextpdf.text.Font fH = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        com.itextpdf.text.Font fR = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);
        PdfPTable t = new PdfPTable(new float[]{1.8f, 1.4f, 2.5f, 1.5f, 1.5f, 1.5f});
        t.setWidthPercentage(100); t.setSpacingBefore(6);
        for (String h : new String[]{"N ORDEN", "PLACA", "SERVICIO", "ESTADO", "FECHA", "TOTAL (S/)"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(BaseColor.BLACK); c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorderColor(BaseColor.BLACK); t.addCell(c);
        }
        boolean alt = false;
        for (OrdenServicio o : ordenes) {
            BaseColor bg  = alt ? ROJO_ALT : BaseColor.WHITE;
            String fecha  = o.getFechaInicio() != null ? o.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "-";
            addTd(t, o.getNumeroOrden(),          fR, bg, Element.ALIGN_CENTER);
            addTd(t, o.getVehiculo().getPlaca(),  fR, bg, Element.ALIGN_CENTER);
            addTd(t, o.getServicio().getNombre(), fR, bg, Element.ALIGN_LEFT);
            addTd(t, o.getEstado(),               fR, bg, Element.ALIGN_CENTER);
            addTd(t, fecha,                       fR, bg, Element.ALIGN_CENTER);
            addTd(t, String.format("%.2f", o.getTotal().doubleValue()), fR, bg, Element.ALIGN_RIGHT);
            alt = !alt;
        }
        for (int i = ordenes.size(); i < 10; i++) for (int j = 0; j < 6; j++) {
            PdfPCell c = new PdfPCell(new Phrase(" ")); c.setFixedHeight(16f); c.setBorderColor(GRIS_BORDE); t.addCell(c);
        }
        doc.add(t);

        com.itextpdf.text.Font fTot = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD, ROJO_OSC);
        PdfPTable pie = new PdfPTable(new float[]{3f, 1.5f});
        pie.setWidthPercentage(45); pie.setHorizontalAlignment(Element.ALIGN_RIGHT); pie.setSpacingBefore(8);
        addPieRow(pie, "OP. GRAVADA (S/):",    String.format("%.2f", total / 1.18),        fLbl, fVal);
        addPieRow(pie, "TOTAL IGV (S/):",      String.format("%.2f", total - total / 1.18), fLbl, fVal);
        
        PdfPCell footL = new PdfPCell(new Phrase("IMPORTE TOTAL (S/):", fTot)); 
        footL.setBackgroundColor(BaseColor.BLACK); footL.setHorizontalAlignment(Element.ALIGN_RIGHT); footL.setPadding(6); pie.addCell(footL);
        PdfPCell footR = new PdfPCell(new Phrase(String.format("%.2f", total), fTot)); 
        footR.setHorizontalAlignment(Element.ALIGN_RIGHT); footR.setPadding(6); footR.setBackgroundColor(BaseColor.WHITE); pie.addCell(footR);
        doc.add(pie);
        agregarPie(doc);
        doc.close();
    }

    // ── PDF VENTAS TIENDA ────────────────────────────────────
    @GetMapping("/pdf/ventas")
    public void pdfVentas(@RequestParam(defaultValue = "mensual") String periodo,
                          HttpServletResponse response) throws IOException, DocumentException {
        List<VentaProducto> ventas = getVentasPeriodo(periodo);
        String desc  = describePeriodo(periodo);
        double total = ventas.stream().mapToDouble(v -> v.getTotal().doubleValue()).sum();

        Document doc = crearDocumentoA4(response, "ventas_" + periodo + ".pdf");
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        agregarCabecera(doc, "REPORTE DE VENTAS TIENDA", desc);

        com.itextpdf.text.Font fLbl = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, GRIS);
        com.itextpdf.text.Font fVal = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
        com.itextpdf.text.Font fTot = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD, ROJO_OSC);
        com.itextpdf.text.Font fH   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        com.itextpdf.text.Font fR   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);

        PdfPTable info = new PdfPTable(new float[]{1.5f, 4f}); info.setWidthPercentage(100); info.setSpacingAfter(8);
        addInfoRow(info, "Periodo",   desc,                          fLbl, fVal);
        addInfoRow(info, "N Ventas",  String.valueOf(ventas.size()), fLbl, fVal);
        addInfoRow(info, "Total",     "S/ " + String.format("%.2f", total), fLbl, fVal);
        doc.add(info);

        PdfPTable t = new PdfPTable(new float[]{0.6f, 2.5f, 2f, 1f, 1.5f, 1.5f});
        t.setWidthPercentage(100); t.setSpacingBefore(6);
        for (String h : new String[]{"CANT.", "PRODUCTO", "CLIENTE", "FECHA", "P.UNIT.", "TOTAL"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(BaseColor.BLACK); c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorderColor(BaseColor.BLACK); t.addCell(c);
        }
        boolean alt = false;
        for (VentaProducto v : ventas) {
            BaseColor bg = alt ? ROJO_ALT : BaseColor.WHITE;
            String fecha = v.getFecha() != null ? v.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "-";
            addTd(t, String.valueOf(v.getCantidad()),                              fR, bg, Element.ALIGN_CENTER);
            addTd(t, v.getProducto().getNombre(),                                  fR, bg, Element.ALIGN_LEFT);
            addTd(t, v.getCliente() != null ? v.getCliente() : "General",         fR, bg, Element.ALIGN_LEFT);
            addTd(t, fecha,                                                        fR, bg, Element.ALIGN_CENTER);
            addTd(t, String.format("%.2f", v.getPrecioUnitario().doubleValue()),   fR, bg, Element.ALIGN_RIGHT);
            addTd(t, String.format("%.2f", v.getTotal().doubleValue()),            fR, bg, Element.ALIGN_RIGHT);
            alt = !alt;
        }
        for (int i = ventas.size(); i < 10; i++) for (int j = 0; j < 6; j++) {
            PdfPCell c = new PdfPCell(new Phrase(" ")); c.setFixedHeight(16f); c.setBorderColor(GRIS_BORDE); t.addCell(c);
        }
        doc.add(t);

        PdfPTable pie = new PdfPTable(new float[]{3f, 1.5f});
        pie.setWidthPercentage(45); pie.setHorizontalAlignment(Element.ALIGN_RIGHT); pie.setSpacingBefore(8);
        addPieRow(pie, "OP. GRAVADA (S/):",   String.format("%.2f", total / 1.18),         fLbl, fVal);
        addPieRow(pie, "TOTAL IGV (S/):",     String.format("%.2f", total - total / 1.18), fLbl, fVal);

        PdfPCell footL = new PdfPCell(new Phrase("IMPORTE TOTAL (S/):", fTot)); 
        footL.setBackgroundColor(BaseColor.BLACK); footL.setHorizontalAlignment(Element.ALIGN_RIGHT); footL.setPadding(6); pie.addCell(footL);
        PdfPCell footR = new PdfPCell(new Phrase(String.format("%.2f", total), fTot)); 
        footR.setHorizontalAlignment(Element.ALIGN_RIGHT); footR.setPadding(6); footR.setBackgroundColor(BaseColor.WHITE); pie.addCell(footR);
        doc.add(pie);
        agregarPie(doc);
        doc.close();
    }

    // ── PDF INVENTARIO ───────────────────────────────────────
    @GetMapping("/pdf/inventario")
    public void pdfInventario(HttpServletResponse response) throws IOException, DocumentException {
        List<Producto> productos = productoRepository.findByActivoTrue();
        Document doc = crearDocumentoA4(response, "inventario.pdf");
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();
        agregarCabecera(doc, "REPORTE DE INVENTARIO", "Estado actual - " + LocalDate.now().format(FMT));

        com.itextpdf.text.Font fLbl = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, GRIS);
        com.itextpdf.text.Font fVal = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
        com.itextpdf.text.Font fH   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        com.itextpdf.text.Font fR   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);
        com.itextpdf.text.Font fA   = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, new BaseColor(180, 0, 0));
        com.itextpdf.text.Font fOK  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, new BaseColor(0, 120, 0));

        long sinStock  = productos.stream().filter(p -> p.getStock() == 0).count();
        long bajoStock = productos.stream().filter(p -> p.getStock() > 0 && p.getStock() <= p.getStockMinimo()).count();
        PdfPTable info = new PdfPTable(new float[]{1.5f, 4f}); info.setWidthPercentage(100); info.setSpacingAfter(8);
        addInfoRow(info, "Total productos", String.valueOf(productos.size()), fLbl, fVal);
        addInfoRow(info, "Sin stock",       String.valueOf(sinStock),         fLbl, fVal);
        addInfoRow(info, "Stock bajo",      String.valueOf(bajoStock),        fLbl, fVal);
        doc.add(info);

        PdfPTable t = new PdfPTable(new float[]{3f, 2f, 1.2f, 1.2f, 1.8f, 1.8f});
        t.setWidthPercentage(100); t.setSpacingBefore(6);
        for (String h : new String[]{"PRODUCTO", "CATEGORIA", "STOCK", "MIN.", "PRECIO", "ESTADO"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(BaseColor.BLACK); c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorderColor(BaseColor.BLACK); t.addCell(c);
        }
        boolean alt = false;
        for (Producto p : productos) {
            boolean sin  = p.getStock() == 0;
            boolean bajo = p.getStock() > 0 && p.getStock() <= p.getStockMinimo();
            BaseColor bg = sin ? ROJO_ALERT : (alt ? ROJO_ALT : BaseColor.WHITE);
            String estado = sin ? "Sin stock" : bajo ? "Stock bajo" : "Normal";
            com.itextpdf.text.Font fE = (sin || bajo) ? fA : fOK;
            addTd(t, p.getNombre(),                                               fR,  bg, Element.ALIGN_LEFT);
            addTd(t, catNombre(p),                                                fR,  bg, Element.ALIGN_LEFT);
            addTd(t, String.valueOf(p.getStock()),                                fR,  bg, Element.ALIGN_CENTER);
            addTd(t, String.valueOf(p.getStockMinimo()),                          fR,  bg, Element.ALIGN_CENTER);
            addTd(t, "S/ " + String.format("%.2f", p.getPrecio().doubleValue()), fR,  bg, Element.ALIGN_RIGHT);
            addTd(t, estado,                                                      fE,  bg, Element.ALIGN_CENTER);
            alt = !alt;
        }
        doc.add(t);
        agregarPie(doc);
        doc.close();
    }

    private void addImageToExcel(Workbook wb, Sheet sheet) {
        try {
            java.io.InputStream is = new org.springframework.core.io.ClassPathResource("static/images/logo.jpeg").getInputStream();
            byte[] bytes = org.apache.poi.util.IOUtils.toByteArray(is);
            int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);
            is.close();
            CreationHelper helper = wb.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(0); // logo at left
            anchor.setRow1(0);
            Picture pict = drawing.createPicture(anchor, pictureIdx);
            pict.resize(0.8);
        } catch (Exception e) {
            // Logo no encontrado
        }
    }

    // ── EXCEL ORDENES ────────────────────────────────────────
    @GetMapping("/excel/ordenes")
    public void excelOrdenes(@RequestParam(defaultValue = "mensual") String periodo,
                             HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=ordenes_" + periodo + ".xlsx");
        List<OrdenServicio> ordenes = getOrdenesPeriodo(periodo);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet  = wb.createSheet("Ordenes de Servicio");
        
        addImageToExcel(wb, sheet);
        
        CellStyle hStyle = wb.createCellStyle();
        Font hFont = wb.createFont(); hFont.setBold(true); hFont.setColor(IndexedColors.WHITE.getIndex());
        hStyle.setFont(hFont); hStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(229, 9, 20), null));
        hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); hStyle.setAlignment(HorizontalAlignment.CENTER);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(40); // make space for logo
        Cell tc = title.createCell(2);
        tc.setCellValue(EMPRESA + " - Reporte de Servicios (" + describePeriodo(periodo) + ")");
        CellStyle ts = wb.createCellStyle(); Font tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short)13);
        ts.setFont(tf); tc.setCellStyle(ts);

        String[] cols = {"N Orden","Placa","Vehiculo","Servicio","Estado","Fecha","Total"};
        Row hr = sheet.createRow(2);
        for (int i = 0; i < cols.length; i++) { Cell c = hr.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hStyle); }

        int rn = 3;
        for (OrdenServicio o : ordenes) {
            Row r = sheet.createRow(rn++);
            r.createCell(0).setCellValue(o.getNumeroOrden());
            r.createCell(1).setCellValue(o.getVehiculo().getPlaca());
            r.createCell(2).setCellValue(o.getVehiculo().getMarca() + " " + o.getVehiculo().getModelo());
            r.createCell(3).setCellValue(o.getServicio().getNombre());
            r.createCell(4).setCellValue(o.getEstado());
            r.createCell(5).setCellValue(o.getFechaInicio().toLocalDate().toString());
            r.createCell(6).setCellValue(o.getTotal().doubleValue());
        }
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
        wb.write(response.getOutputStream()); wb.close();
    }

    // ── EXCEL VENTAS ─────────────────────────────────────────
    @GetMapping("/excel/ventas")
    public void excelVentas(@RequestParam(defaultValue = "mensual") String periodo,
                            HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=ventas_" + periodo + ".xlsx");
        List<VentaProducto> ventas = getVentasPeriodo(periodo);

        Workbook wb = new XSSFWorkbook();
        Sheet sheet  = wb.createSheet("Ventas Tienda");
        
        addImageToExcel(wb, sheet);
        
        CellStyle hStyle = wb.createCellStyle();
        Font hFont = wb.createFont(); hFont.setBold(true); hFont.setColor(IndexedColors.WHITE.getIndex());
        hStyle.setFont(hFont); hStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(229, 9, 20), null));
        hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(40); // make space for logo
        Cell tc = title.createCell(2);
        tc.setCellValue(EMPRESA + " - Ventas Tienda (" + describePeriodo(periodo) + ")");
        CellStyle ts = wb.createCellStyle(); Font tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short)13);
        ts.setFont(tf); tc.setCellStyle(ts);

        String[] cols = {"Boleta","Producto","Cliente","Cantidad","Precio Unit.","Total","Fecha"};
        Row hr = sheet.createRow(2);
        for (int i = 0; i < cols.length; i++) { Cell c = hr.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hStyle); }

        int rn = 3;
        for (VentaProducto v : ventas) {
            Row r = sheet.createRow(rn++);
            r.createCell(0).setCellValue(v.getNumeroBoleta());
            r.createCell(1).setCellValue(v.getProducto().getNombre());
            r.createCell(2).setCellValue(v.getCliente() != null ? v.getCliente() : "General");
            r.createCell(3).setCellValue(v.getCantidad());
            r.createCell(4).setCellValue(v.getPrecioUnitario().doubleValue());
            r.createCell(5).setCellValue(v.getTotal().doubleValue());
            r.createCell(6).setCellValue(v.getFecha().toLocalDate().toString());
        }
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
        wb.write(response.getOutputStream()); wb.close();
    }

    // ── EXCEL INVENTARIO ─────────────────────────────────────
    @GetMapping("/excel/inventario")
    public void excelInventario(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=inventario.xlsx");
        List<Producto> productos = productoRepository.findByActivoTrue();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet  = wb.createSheet("Inventario");
        
        addImageToExcel(wb, sheet);
        
        CellStyle hStyle = wb.createCellStyle();
        Font hFont = wb.createFont(); hFont.setBold(true); hFont.setColor(IndexedColors.WHITE.getIndex());
        hStyle.setFont(hFont); hStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(229, 9, 20), null));
        hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row title = sheet.createRow(0);
        title.setHeightInPoints(40); // make space for logo
        Cell tc = title.createCell(2);
        tc.setCellValue(EMPRESA + " - Inventario Tienda");
        CellStyle ts = wb.createCellStyle(); Font tf = wb.createFont(); tf.setBold(true); tf.setFontHeightInPoints((short)13);
        ts.setFont(tf); tc.setCellStyle(ts);

        String[] cols = {"Producto","Categoria","Stock","Stock Minimo","Precio","Unidad","Estado"};
        Row hr = sheet.createRow(2);
        for (int i = 0; i < cols.length; i++) { Cell c = hr.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hStyle); }

        int rn = 3;
        for (Producto p : productos) {
            Row r = sheet.createRow(rn++);
            r.createCell(0).setCellValue(p.getNombre());
            r.createCell(1).setCellValue(catNombre(p));
            r.createCell(2).setCellValue(p.getStock());
            r.createCell(3).setCellValue(p.getStockMinimo());
            r.createCell(4).setCellValue(p.getPrecio().doubleValue());
            r.createCell(5).setCellValue(p.getUnidad() != null ? p.getUnidad() : "-");
            r.createCell(6).setCellValue(p.getStock() == 0 ? "Sin stock" : p.getStock() <= p.getStockMinimo() ? "Stock bajo" : "Normal");
        }
        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
        wb.write(response.getOutputStream()); wb.close();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LavadoDTO {
        private String source; // "ORDEN" o "CAJA"
        private String numeroOrden;
        private LocalDateTime fecha;
        private String vehiculo;
        private String servicio;
        private String estado;
        private double total;
    }

    // ── REPORTES LAVADOS HTML ────────────────────────────────
    @GetMapping("/lavados")
    public String reportesLavados(@RequestParam(required = false) String fechaInicio,
                                   @RequestParam(required = false) String fechaFin,
                                   Model model) {
        LocalDateTime inicio = (fechaInicio != null && !fechaInicio.isEmpty())
                ? LocalDate.parse(fechaInicio).atStartOfDay()
                : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fin = (fechaFin != null && !fechaFin.isEmpty())
                ? LocalDate.parse(fechaFin).plusDays(1).atStartOfDay()
                : LocalDate.now().plusDays(1).atStartOfDay();

        List<OrdenServicio> ordenes = ordenRepository.findByFechaBetween(inicio, fin);
        List<com.carwash.sistema.model.RegistroLavado> registros = registroLavadoRepository.findByFechaRegistroBetween(inicio, fin);

        List<LavadoDTO> listaMixta = new java.util.ArrayList<>();
        
        for (OrdenServicio o : ordenes) {
            String veh = o.getVehiculo() != null ? o.getVehiculo().getPlaca() : "-";
            String srv = o.getServicio() != null ? o.getServicio().getNombre() : "-";
            listaMixta.add(new LavadoDTO("ORDEN", o.getNumeroOrden(), o.getFechaInicio(), veh, srv, o.getEstado(), o.getTotal().doubleValue()));
        }

        for (com.carwash.sistema.model.RegistroLavado r : registros) {
            String nr = "CAJ-" + r.getNumeroOrden();
            String veh = r.getPlaca() != null && !r.getPlaca().isEmpty() ? r.getPlaca() : r.getTipoVehiculo();
            listaMixta.add(new LavadoDTO("CAJA", nr, r.getFechaRegistro(), veh, r.getServicioCompleto(), r.getEstado(), r.getMonto().doubleValue()));
        }

        listaMixta.sort((a, b) -> {
            if (a.getFecha() == null) return 1;
            if (b.getFecha() == null) return -1;
            return b.getFecha().compareTo(a.getFecha());
        });

        double ingresosTotales = listaMixta.stream()
                .filter(l -> "COMPLETADO".equals(l.getEstado()) || "PAGADO".equals(l.getEstado()))
                .mapToDouble(LavadoDTO::getTotal).sum();
        long pendientes = listaMixta.stream().filter(l -> "PENDIENTE".equals(l.getEstado())).count();
        long completadas = listaMixta.stream().filter(l -> "COMPLETADO".equals(l.getEstado()) || "PAGADO".equals(l.getEstado())).count();
        long enProceso = listaMixta.stream().filter(l -> "EN_PROCESO".equals(l.getEstado())).count();

        model.addAttribute("activePage",       "reportes-lavados");
        model.addAttribute("ordenes",          listaMixta);
        model.addAttribute("totalOrdenes",     listaMixta.size());
        model.addAttribute("ingresosTotales",  ingresosTotales);
        model.addAttribute("pendientes",       pendientes);
        model.addAttribute("completadas",      completadas);
        model.addAttribute("enProceso",        enProceso);
        model.addAttribute("fechaInicio",      inicio.toLocalDate().toString());
        model.addAttribute("fechaFin",         fin.minusDays(1).toLocalDate().toString());
        model.addAttribute("historial",        cajaDiariaService.getHistorialCajas(20));
        return "reportes/lavados";
    }

    // ── ESTADISTICAS HTML ────────────────────────────────────
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        LocalDateTime inicioMes           = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes              = inicioMes.plusMonths(1);
        LocalDateTime inicioSemana        = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime finSemana           = inicioSemana.plusDays(7);
        LocalDateTime inicioSemanaAnterior= inicioSemana.minusWeeks(1);
        LocalDateTime finSemanaAnterior   = finSemana.minusWeeks(1);

        double ingresosOrdenesMes = ordenRepository.findByFechaBetween(inicioMes, finMes).stream()
                .filter(o -> "COMPLETADO".equals(o.getEstado()))
                .mapToDouble(o -> o.getTotal().doubleValue()).sum();
        Double cajaMes = cajaDiariaRepository.sumTotalDiaBetween(inicioMes, finMes);
        if (cajaMes == null) cajaMes = 0.0;
        
        double ingresosLavadosMes = ingresosOrdenesMes + cajaMes;

        double ingresosTiendaMes  = ventaProductoRepository.findByFechaBetween(inicioMes, finMes).stream()
                .mapToDouble(v -> v.getTotal().doubleValue()).sum();

        double ingresosOrdenesSemana = ordenRepository.findByFechaBetween(inicioSemana, finSemana).stream()
                .filter(o -> "COMPLETADO".equals(o.getEstado()))
                .mapToDouble(o -> o.getTotal().doubleValue()).sum();
        Double cajaSemana = cajaDiariaRepository.sumTotalDiaBetween(inicioSemana, finSemana);
        if (cajaSemana == null) cajaSemana = 0.0;

        double ingresosTiendaSemana = ventaProductoRepository.findByFechaBetween(inicioSemana, finSemana).stream()
                .mapToDouble(v -> v.getTotal().doubleValue()).sum();

        double ingresosSemanaActual = ingresosOrdenesSemana + cajaSemana + ingresosTiendaSemana;

        double ingresosOrdenesSemanaAnt = ordenRepository.findByFechaBetween(inicioSemanaAnterior, finSemanaAnterior).stream()
                .filter(o -> "COMPLETADO".equals(o.getEstado()))
                .mapToDouble(o -> o.getTotal().doubleValue()).sum();
        Double cajaSemanaAnt = cajaDiariaRepository.sumTotalDiaBetween(inicioSemanaAnterior, finSemanaAnterior);
        if (cajaSemanaAnt == null) cajaSemanaAnt = 0.0;

        double ingresosTiendaSemanaAnt = ventaProductoRepository.findByFechaBetween(inicioSemanaAnterior, finSemanaAnterior).stream()
                .mapToDouble(v -> v.getTotal().doubleValue()).sum();

        double ingresosSemanaAnterior = ingresosOrdenesSemanaAnt + cajaSemanaAnt + ingresosTiendaSemanaAnt;

        double variacionSemanal = ingresosSemanaAnterior > 0
                ? ((ingresosSemanaActual - ingresosSemanaAnterior) / ingresosSemanaAnterior) * 100 : 0;

        java.util.List<String> labelsFechas = new java.util.ArrayList<>();
        java.util.List<Double> lavadosData = new java.util.ArrayList<>();
        java.util.List<Double> tiendaData = new java.util.ArrayList<>();

        java.time.LocalDate hoy = java.time.LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate fecha = hoy.minusDays(i);
            labelsFechas.add(fecha.format(DateTimeFormatter.ofPattern("dd/MM")));

            LocalDateTime inicioDia = fecha.atStartOfDay();
            LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

            double lv = ordenRepository.findByFechaBetween(inicioDia, finDia).stream()
                    .filter(o -> "COMPLETADO".equals(o.getEstado()))
                    .mapToDouble(o -> o.getTotal().doubleValue()).sum();
            Double cajaDia = cajaDiariaRepository.sumTotalDiaBetween(inicioDia, finDia);
            if (cajaDia == null) cajaDia = 0.0;
            
            lavadosData.add(lv + cajaDia);

            double tv = ventaProductoRepository.findByFechaBetween(inicioDia, finDia).stream()
                    .mapToDouble(v -> v.getTotal().doubleValue()).sum();
            tiendaData.add(tv);
        }

        model.addAttribute("activePage",             "reportes-estadisticas");
        model.addAttribute("ingresosLavadosMes",     ingresosLavadosMes);
        model.addAttribute("ingresosTiendaMes",      ingresosTiendaMes);
        model.addAttribute("ingresosTotalesMes",     ingresosLavadosMes + ingresosTiendaMes);
        model.addAttribute("ingresosSemanaActual",   ingresosSemanaActual);
        model.addAttribute("ingresosSemanaAnterior", ingresosSemanaAnterior);
        model.addAttribute("variacionSemanal",       variacionSemanal);
        
        model.addAttribute("labelsFechas", labelsFechas);
        model.addAttribute("lavadosData", lavadosData);
        model.addAttribute("tiendaData", tiendaData);

        return "reportes/estadisticas";
    }
}