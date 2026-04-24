package com.carwash.sistema.service;

import com.carwash.sistema.model.CajaDiaria;
import com.carwash.sistema.model.RegistroLavado;
import com.carwash.sistema.model.Usuario;
import com.carwash.sistema.repository.CajaDiariaRepository;
import com.carwash.sistema.repository.RegistroLavadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CajaDiariaService {

    private final CajaDiariaRepository cajaDiariaRepository;
    private final RegistroLavadoRepository registroLavadoRepository;

    /**
     * Abre una nueva caja para el día
     */
    @Transactional
    public CajaDiaria abrirCaja(Usuario responsable, BigDecimal montoInicial) {
        if (cajaDiariaRepository.existsByEstado("ABIERTA")) {
            throw new RuntimeException("Ya existe una caja abierta. Debe cerrarla primero.");
        }

        CajaDiaria caja = new CajaDiaria();
        caja.setFechaApertura(LocalDateTime.now());
        caja.setMontoInicial(montoInicial != null ? montoInicial : BigDecimal.ZERO);
        caja.setResponsable(responsable);
        caja.setEstado("ABIERTA");

        log.info("Abriendo caja - Responsable: {}, Monto inicial: {}",
                responsable.getNombre(), montoInicial);

        return cajaDiariaRepository.save(caja);
    }

    /**
     * Obtiene la caja abierta actual
     * ✅ CORREGIDO: usa findAllByEstado para evitar excepción con duplicados
     */
    public Optional<CajaDiaria> getCajaAbierta() {
        List<CajaDiaria> cajas = cajaDiariaRepository.findAllByEstado("ABIERTA");
        return cajas.isEmpty() ? Optional.empty() : Optional.of(cajas.get(0));
    }

    /**
     * Verifica si hay caja abierta
     */
    public boolean hayCajaAbierta() {
        return cajaDiariaRepository.existsByEstado("ABIERTA");
    }

    /**
     * Registra un nuevo vehículo/lavado
     */
    @Transactional
    public RegistroLavado registrarVehiculo(RegistroLavado registro) {
        CajaDiaria cajaAbierta = getCajaAbierta()
                .orElseThrow(() -> new RuntimeException("No hay caja abierta. Debe abrir caja primero."));

        // Asignar número de orden correlativo
        Integer ultimoNumero = registroLavadoRepository.findMaxNumeroOrdenByCajaDiariaId(cajaAbierta.getId());
        registro.setNumeroOrden(ultimoNumero != null ? ultimoNumero + 1 : 1);

        // Asociar con la caja actual
        registro.setCajaDiaria(cajaAbierta);

        // Guardar registro
        RegistroLavado guardado = registroLavadoRepository.save(registro);

        // ✅ CORREGIDO: actualizar totales cargando registros desde el repositorio
        actualizarTotalesCaja(cajaAbierta.getId());

        log.info("Vehículo registrado - N°: {}, Tipo: {}, Monto: {}",
                guardado.getNumeroOrden(), guardado.getTipoVehiculo(), guardado.getMonto());

        return guardado;
    }

    /**
     * Actualiza los totales de la caja
     * ✅ CORREGIDO: carga los registros explícitamente desde el repositorio
     *    en lugar de usar caja.getRegistros() que es LAZY y puede estar vacío
     */
    @Transactional
    public void actualizarTotalesCaja(Long cajaDiariaId) {
        CajaDiaria caja = cajaDiariaRepository.findById(cajaDiariaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        // Cargar registros directamente desde el repositorio (evita problema LAZY)
        List<RegistroLavado> registros = registroLavadoRepository
                .findByCajaDiariaIdOrderByNumeroOrdenDesc(cajaDiariaId);

        // Calcular totales sobre la lista cargada
        BigDecimal totalEfectivo = registros.stream()
                .filter(r -> "EFECTIVO".equals(r.getMetodoPago()) && "PAGADO".equals(r.getEstado()))
                .map(RegistroLavado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalYape = registros.stream()
                .filter(r -> "YAPE".equals(r.getMetodoPago()) && "PAGADO".equals(r.getEstado()))
                .map(RegistroLavado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTarjeta = registros.stream()
                .filter(r -> "TARJETA".equals(r.getMetodoPago()) && "PAGADO".equals(r.getEstado()))
                .map(RegistroLavado::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cantidadVehiculos = (int) registros.stream()
                .filter(r -> "PAGADO".equals(r.getEstado()))
                .count();

        // Asignar totales calculados a la caja
        caja.setTotalEfectivo(totalEfectivo);
        caja.setTotalYape(totalYape);
        caja.setTotalTarjeta(totalTarjeta);
        caja.setTotalDia(totalEfectivo.add(totalYape).add(totalTarjeta));
        caja.setCantidadVehiculos(cantidadVehiculos);

        cajaDiariaRepository.save(caja);
    }

    /**
     * Obtiene todos los registros de la caja actual
     */
    public List<RegistroLavado> getRegistrosCajaActual() {
        CajaDiaria cajaAbierta = getCajaAbierta()
                .orElseThrow(() -> new RuntimeException("No hay caja abierta"));

        return registroLavadoRepository.findByCajaDiariaIdOrderByNumeroOrdenDesc(cajaAbierta.getId());
    }

    /**
     * Cierra la caja del día
     */
    @Transactional
    public CajaDiaria cerrarCaja(Long cajaId, BigDecimal montoFinalContado, String observaciones) {
        CajaDiaria caja = cajaDiariaRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        if (caja.estaCerrada()) {
            throw new RuntimeException("Esta caja ya está cerrada");
        }

        // Recalcular totales antes de cerrar (usando el método corregido)
        actualizarTotalesCaja(cajaId);

        // Recargar la caja con los totales actualizados
        caja = cajaDiariaRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        // Cerrar caja
        caja.cerrarCaja(montoFinalContado);
        caja.setObservaciones(observaciones);

        log.info("Cerrando caja - ID: {}, Total día: {}, Diferencia: {}",
                caja.getId(), caja.getTotalDia(), caja.getDiferencia());

        return cajaDiariaRepository.save(caja);
    }

    /**
     * Elimina un registro (solo si la caja está abierta)
     */
    @Transactional
    public void eliminarRegistro(Long registroId) {
        RegistroLavado registro = registroLavadoRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado"));

        if (registro.getCajaDiaria().estaCerrada()) {
            throw new RuntimeException("No se puede eliminar registros de una caja cerrada");
        }

        Long cajaDiariaId = registro.getCajaDiaria().getId();

        // Primero eliminar el registro
        registroLavadoRepository.deleteById(registroId);
        registroLavadoRepository.flush();

        // Después actualizar totales
        actualizarTotalesCaja(cajaDiariaId);

        log.info("Registro eliminado - ID: {}", registroId);
    }

    /**
     * Obtiene un registro por ID
     */
    public RegistroLavado getRegistroById(Long id) {
        return registroLavadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado"));
    }

    /**
     * Actualiza un registro
     */
    @Transactional
    public RegistroLavado actualizarRegistro(RegistroLavado registro) {
        RegistroLavado existente = registroLavadoRepository.findById(registro.getId())
                .orElseThrow(() -> new RuntimeException("Registro no encontrado"));

        if (existente.getCajaDiaria().estaCerrada()) {
            throw new RuntimeException("No se puede editar registros de una caja cerrada");
        }

        RegistroLavado actualizado = registroLavadoRepository.save(registro);

        // Actualizar totales
        actualizarTotalesCaja(existente.getCajaDiaria().getId());

        return actualizado;
    }

    /**
     * Obtiene historial de cajas cerradas
     */
    public List<CajaDiaria> getHistorialCajas(int limit) {
        return cajaDiariaRepository.findTop10ByEstadoOrderByFechaCierreDesc("CERRADA");
    }

    /**
     * Busca cajas por rango de fechas
     */
    public List<CajaDiaria> getCajasPorRango(LocalDate inicio, LocalDate fin) {
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.plusDays(1).atStartOfDay();
        return cajaDiariaRepository.findByFechaAperturaBetween(inicioDateTime, finDateTime);
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

    /**
     * Genera archivo Excel con el cierre de caja
     */
    public byte[] generarExcelCierre(Long cajaId) throws IOException {
        CajaDiaria caja = cajaDiariaRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

        List<RegistroLavado> registros = registroLavadoRepository.findByCajaDiariaIdOrderByNumeroOrdenDesc(cajaId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Cierre de Caja");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            
            // XSSFColor is specific to XSSFWorkbook
            headerStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(229, 9, 20), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 18);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("S/ #,##0.00"));

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(40); // make space for logo
            Cell titleCell = titleRow.createCell(2);
            titleCell.setCellValue("LUBRICENTRO CAR DETAILING GADIEL - CIERRE DE CAJA");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 2, 5));
            
            addImageToExcel(workbook, sheet);

            int rowNum = 2;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            Row infoRow1 = sheet.createRow(rowNum++);
            infoRow1.createCell(0).setCellValue("Fecha:");
            infoRow1.createCell(1).setCellValue(caja.getFechaApertura().format(formatter));

            Row infoRow2 = sheet.createRow(rowNum++);
            infoRow2.createCell(0).setCellValue("Responsable:");
            infoRow2.createCell(1).setCellValue(caja.getResponsable().getNombre());

            rowNum++;

            Row resumenTitleRow = sheet.createRow(rowNum++);
            Cell resumenCell = resumenTitleRow.createCell(0);
            resumenCell.setCellValue("RESUMEN DE INGRESOS");
            resumenCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

            Row efectivoRow = sheet.createRow(rowNum++);
            efectivoRow.createCell(0).setCellValue("💵 Efectivo:");
            Cell efectivoCell = efectivoRow.createCell(1);
            efectivoCell.setCellValue(caja.getTotalEfectivo().doubleValue());
            efectivoCell.setCellStyle(moneyStyle);

            Row yapeRow = sheet.createRow(rowNum++);
            yapeRow.createCell(0).setCellValue("📱 Yape:");
            Cell yapeCell = yapeRow.createCell(1);
            yapeCell.setCellValue(caja.getTotalYape().doubleValue());
            yapeCell.setCellStyle(moneyStyle);

            Row tarjetaRow = sheet.createRow(rowNum++);
            tarjetaRow.createCell(0).setCellValue("💳 Tarjeta:");
            Cell tarjetaCell = tarjetaRow.createCell(1);
            tarjetaCell.setCellValue(caja.getTotalTarjeta().doubleValue());
            tarjetaCell.setCellStyle(moneyStyle);

            Row totalRow = sheet.createRow(rowNum++);
            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("💰 TOTAL DEL DÍA:");
            totalLabelCell.setCellStyle(headerStyle);
            Cell totalCell = totalRow.createCell(1);
            totalCell.setCellValue(caja.getTotalDia().doubleValue());
            totalCell.setCellStyle(moneyStyle);

            rowNum++;

            Row detalleTitle = sheet.createRow(rowNum++);
            Cell detalleCell = detalleTitle.createCell(0);
            detalleCell.setCellValue("DETALLE DE VEHÍCULOS ATENDIDOS");
            detalleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"N°", "Tipo", "Placa", "Servicio", "Monto", "Pago"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (RegistroLavado registro : registros) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(registro.getNumeroOrden());
                row.createCell(1).setCellValue(registro.getTipoVehiculo());
                row.createCell(2).setCellValue(registro.getPlaca() != null ? registro.getPlaca() : "-");
                row.createCell(3).setCellValue(registro.getServicioCompleto());
                Cell montoCell = row.createCell(4);
                montoCell.setCellValue(registro.getMonto().doubleValue());
                montoCell.setCellStyle(moneyStyle);
                row.createCell(5).setCellValue(registro.getMetodoPago());
            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}