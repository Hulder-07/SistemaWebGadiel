package com.carwash.sistema.controller;

import com.carwash.sistema.model.OrdenServicio;
import com.carwash.sistema.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final OrdenRepository ordenRepository;
    private final VentaProductoRepository ventaProductoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicioRepository servicioRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {

        LocalDateTime inicioDia  = LocalDate.now().atStartOfDay();
        LocalDateTime finDia     = inicioDia.plusDays(1);
        LocalDateTime inicioMes  = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes     = inicioMes.plusMonths(1);
        LocalDateTime inicioSemana = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime finSemana    = inicioSemana.plusDays(7);

        // Ingresos
        BigDecimal lavadosHoy    = ordenRepository.sumTotalBetween(inicioDia, finDia);
        BigDecimal lavadosSemana = ordenRepository.sumTotalBetween(inicioSemana, finSemana);
        BigDecimal lavadosMes    = ordenRepository.sumTotalBetween(inicioMes, finMes);
        BigDecimal tiendaHoy     = ventaProductoRepository.sumTotalBetween(inicioDia, finDia);
        BigDecimal tiendaMes     = ventaProductoRepository.sumTotalBetween(inicioMes, finMes);
        BigDecimal totalHoy      = lavadosHoy.add(tiendaHoy);
        BigDecimal totalMes      = lavadosMes.add(tiendaMes);

        // ── DATOS GRÁFICA 7 DÍAS ─────────────────────────────────
        List<String> labels7 = new ArrayList<>();
        List<Double> lavados7 = new ArrayList<>();
        List<Double> tienda7  = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate dia      = LocalDate.now().minusDays(i);
            LocalDateTime ini  = dia.atStartOfDay();
            LocalDateTime fin  = ini.plusDays(1);

            String label = dia.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, new Locale("es", "PE"));
            labels7.add(label);

            BigDecimal lv = ordenRepository.sumTotalBetween(ini, fin);
            BigDecimal tv = ventaProductoRepository.sumTotalBetween(ini, fin);
            lavados7.add(lv.doubleValue());
            tienda7.add(tv.doubleValue());
        }

        // ── DATOS GRÁFICA 6 MESES ────────────────────────────────
        List<String> labels6m = new ArrayList<>();
        List<Double> data6m = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime ini = LocalDate.now().minusMonths(i).withDayOfMonth(1).atStartOfDay();
            LocalDateTime fin = ini.plusMonths(1);
            String label = ini.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "PE")).toUpperCase();
            labels6m.add(label);
            BigDecimal lvm = ordenRepository.sumTotalBetween(ini, fin);
            BigDecimal tvm = ventaProductoRepository.sumTotalBetween(ini, fin);
            data6m.add(lvm.add(tvm).doubleValue());
        }

        // ── ÓRDENES RECIENTES ────────────────────────────────────
        List<OrdenServicio> ordenes = ordenRepository.findTop15ByOrderByFechaInicioDesc();
        List<Map<String, Object>> ordenesDto = ordenes.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            o.getId());
            m.put("numeroOrden",   o.getNumeroOrden());
            m.put("vehiculoPLaca", o.getVehiculo().getPlaca());
            m.put("servicioNombre",o.getServicio().getNombre());
            m.put("estado",        o.getEstado());
            m.put("total",         o.getTotal());
            m.put("fecha",         o.getFechaInicio());
            return m;
        }).collect(Collectors.toList());

        // Contadores estado
        long pendientes  = ordenRepository.countByEstado("PENDIENTE");
        long enProceso   = ordenRepository.countByEstado("EN_PROCESO");
        long completados = ordenRepository.countByEstado("COMPLETADO");

        // Stock bajo
        var bajoStock = productoRepository.findBajoStock();

        // Top servicios del mes
        List<Object[]> topRaw = ordenRepository.topServiciosBetween(inicioMes, finMes);
        List<Map<String, Object>> topServicios = new ArrayList<>();
        double totalTop = topRaw.stream().mapToDouble(r -> ((Number) r[1]).doubleValue()).sum();
        for (int i = 0; i < Math.min(topRaw.size(), 5); i++) {
            Object[] row = topRaw.get(i);
            double ing = ((Number) row[1]).doubleValue();
            int pct = totalTop > 0 ? (int)(ing * 100 / totalTop) : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", row[0]); m.put("ingresos", ing); m.put("porcentaje", pct);
            topServicios.add(m);
        }

        // ── MODEL ────────────────────────────────────────────────
        model.addAttribute("activePage",       "dashboard");
        model.addAttribute("lavadosHoy",       lavadosHoy);
        model.addAttribute("lavadosSemana",    lavadosSemana);
        model.addAttribute("lavadosMes",       lavadosMes);
        model.addAttribute("tiendaHoy",        tiendaHoy);
        model.addAttribute("tiendaMes",        tiendaMes);
        model.addAttribute("totalHoy",         totalHoy);
        model.addAttribute("totalMes",         totalMes);
        model.addAttribute("pendientes",       pendientes);
        model.addAttribute("enProceso",        enProceso);
        model.addAttribute("completados",      completados);
        model.addAttribute("ordenesRecientes", ordenesDto);
        model.addAttribute("bajoStock",        bajoStock);
        model.addAttribute("totalClientes",    clienteRepository.countByActivoTrue());
        model.addAttribute("totalServicios",   servicioRepository.countByActivoTrue());
        model.addAttribute("topServicios",     topServicios);
        // Gráficas
        model.addAttribute("chart7Labels",  labels7);
        model.addAttribute("chart7Lavados", lavados7);
        model.addAttribute("chart7Tienda",  tienda7);
        model.addAttribute("chartDonaLavados", lavadosMes.doubleValue());
        model.addAttribute("chartDonaTienda",  tiendaMes.doubleValue());
        model.addAttribute("chart6MLabels", labels6m);
        model.addAttribute("chart6MData",   data6m);

        return "dashboard";
    }
}