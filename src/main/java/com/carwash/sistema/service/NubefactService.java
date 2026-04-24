package com.carwash.sistema.service;

import com.carwash.sistema.repository.ComprobanteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NubefactService {

    // ✅ URL base limpia, sin token ni path extra
    @Value("${nubefact.url}")
    private String nubefactUrl;

    @Value("${nubefact.token}")
    private String token;

    @Value("${nubefact.ruc}")
    private String rucEmisor;

    @Value("${empresa.nombre}")
    private String empresaNombre;

    @Value("${empresa.direccion}")
    private String empresaDireccion;

    @Value("${sunat.serie.boleta}")
    private String serieBoleta;

    @Value("${sunat.serie.factura}")
    private String serieFactura;

    @Value("${nubefact.modo}")
    private String modo;

    private final ComprobanteRepository comprobanteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Emite una BOLETA DE VENTA via Nubefact
     */
    public Map<String, Object> emitirBoleta(String clienteNombre, String clienteDni,
                                             List<ItemComprobante> items, double total) {
        String doc = clienteDni != null ? clienteDni.trim() : "";

        // ✅ CORREGIDO: tipo de documento como entero, no String
        int tipoDoc;
        if (doc.isEmpty() || doc.equals("00000000")) {
            doc = "00000000";
            tipoDoc = 0; // sin documento
        } else if (doc.length() == 11) {
            tipoDoc = 6; // RUC
        } else if (doc.length() == 8) {
            tipoDoc = 1; // DNI
        } else {
            doc = "00000000";
            tipoDoc = 0;
        }
        return emitirComprobante("03", serieBoleta, clienteNombre, doc, tipoDoc, items, total, null);
    }

    /**
     * Emite una FACTURA via Nubefact
     */
    public Map<String, Object> emitirFactura(String clienteRazonSocial, String clienteRuc,
                                              String clienteDireccion,
                                              List<ItemComprobante> items, double total) {
        String doc = clienteRuc != null ? clienteRuc.trim() : "";
        return emitirComprobante("01", serieFactura, clienteRazonSocial,
                doc, 6, items, total, clienteDireccion);
    }

    /**
     * Obtiene el siguiente número correlativo.
     * Compara el máximo local con el máximo conocido de Nubefact (guardado en BD)
     * para siempre avanzar, nunca repetir.
     */
    private Integer obtenerSiguienteNumero(String serie) {
        Integer localMax = comprobanteRepository.findMaxNumeroBySerie(serie);
        return (localMax != null ? localMax : 0) + 1;
    }

    // ✅ CORREGIDO: tipoDocCliente ahora es int
    private Map<String, Object> emitirComprobante(String tipoComprobante, String serie,
                                                    String clienteNombre, String clienteDoc,
                                                    int tipoDocCliente,
                                                    List<ItemComprobante> items, double total,
                                                    String clienteDireccion) {
        try {
            // Validaciones
            if (items == null || items.isEmpty()) {
                return crearRespuestaError("No se han proporcionado items para el comprobante");
            }

            if (clienteNombre == null || clienteNombre.trim().isEmpty()) {
                return crearRespuestaError("El nombre del cliente es obligatorio");
            }

            if (clienteDoc == null || clienteDoc.trim().isEmpty()) {
                clienteDoc = "00000000";
            }

            // Obtener siguiente número correlativo
            Integer siguienteNumero = obtenerSiguienteNumero(serie);

            // Calcular montos con precisión
            BigDecimal totalBD = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gravadas = totalBD.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
            BigDecimal igv = totalBD.subtract(gravadas);

            // Preparar items para JSON
            List<Map<String, Object>> itemsJson = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                ItemComprobante item = items.get(i);

                BigDecimal precioUnitarioBD = BigDecimal.valueOf(item.precioUnitario()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal cantidadBD = BigDecimal.valueOf(item.cantidad()).setScale(2, RoundingMode.HALF_UP);

                BigDecimal valorUnitario = precioUnitarioBD.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
                BigDecimal subtotal = valorUnitario.multiply(cantidadBD).setScale(2, RoundingMode.HALF_UP);
                BigDecimal igvItem = precioUnitarioBD.subtract(valorUnitario).multiply(cantidadBD).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalItem = precioUnitarioBD.multiply(cantidadBD).setScale(2, RoundingMode.HALF_UP);

                Map<String, Object> itemJson = new LinkedHashMap<>();
                itemJson.put("unidad_de_medida", item.unidad() != null ? item.unidad() : "NIU");
                itemJson.put("codigo", item.codigo() != null ? item.codigo() : "ITEM" + (i + 1));
                itemJson.put("descripcion", item.descripcion());
                itemJson.put("cantidad", cantidadBD.doubleValue());
                itemJson.put("valor_unitario", valorUnitario.doubleValue());
                itemJson.put("precio_unitario", precioUnitarioBD.doubleValue());
                itemJson.put("subtotal", subtotal.doubleValue());
                itemJson.put("tipo_de_igv", 1);
                itemJson.put("igv", igvItem.doubleValue());
                itemJson.put("total", totalItem.doubleValue());
                itemJson.put("anticipo_regularizacion", false);
                itemJson.put("anticipo_documento_serie", "");
                itemJson.put("anticipo_documento_numero", "");

                itemsJson.add(itemJson);
            }

            // ✅ Construir request para NubeFact con todos los campos correctos
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("operacion", "generar_comprobante");
            body.put("tipo_de_comprobante", tipoComprobante.equals("01") ? 1 : 2);
            body.put("serie", serie);
            // ✅ CORREGIDO: enviar número correlativo para que Nubefact lo registre
            body.put("numero", siguienteNumero);
            body.put("sunat_transaction", 1);
            // ✅ CORREGIDO: tipo de documento como entero
            body.put("cliente_tipo_de_documento", tipoDocCliente);
            body.put("cliente_numero_de_documento", clienteDoc);
            body.put("cliente_denominacion", clienteNombre);
            body.put("cliente_direccion", clienteDireccion != null ? clienteDireccion : "");
            body.put("cliente_email", "");
            body.put("cliente_email_1", "");
            body.put("cliente_email_2", "");
            body.put("fecha_de_emision", java.time.LocalDate.now().toString());
            body.put("fecha_de_vencimiento", "");
            body.put("moneda", 1);
            body.put("tipo_de_cambio", "");
            body.put("porcentaje_de_igv", 18.0);
            body.put("descuento_global", 0.0);
            body.put("total_descuento", 0.0);
            body.put("total_anticipo", 0.0);
            body.put("total_gravada", gravadas.doubleValue());
            body.put("total_inafecta", 0.0);
            body.put("total_exonerada", 0.0);
            body.put("total_igv", igv.doubleValue());
            body.put("total_gratuita", 0.0);
            body.put("total_otros_cargos", 0.0);
            body.put("total", totalBD.doubleValue());
            body.put("percepcion_tipo", "");
            body.put("percepcion_base_imponible", 0.0);
            body.put("total_percepcion", 0.0);
            body.put("total_incluido_percepcion", 0.0);
            body.put("detraccion", false);
            body.put("observaciones", "");
            body.put("documento_que_se_modifica_tipo", "");
            body.put("documento_que_se_modifica_serie", "");
            body.put("documento_que_se_modifica_numero", "");
            body.put("tipo_de_nota_de_credito", "");
            body.put("tipo_de_nota_de_debito", "");
            body.put("enviar_automaticamente_a_la_sunat", true);
            body.put("enviar_automaticamente_al_cliente", false);
            body.put("codigo_unico", "");
            body.put("condiciones_de_pago", "");
            body.put("medio_de_pago", "");
            body.put("placa_vehiculo", "");
            body.put("orden_compra_servicio", "");
            body.put("tabla_personalizada_codigo", "");
            body.put("formato_de_pdf", "");
            body.put("items", itemsJson);

            // ── Llamar a NubeFact con reintento automático ──
            // Si Nubefact dice "ya existe", incrementa el número y reintenta hasta 20 veces
            String response = null;
            Map<String, Object> result = null;
            int intentos = 0;
            int maxIntentos = 20;

            while (intentos < maxIntentos) {
                try {
                    response = WebClient.builder()
                            .baseUrl(nubefactUrl)
                            .defaultHeader("Authorization", "Token " + token)
                            .defaultHeader("Content-Type", "application/json")
                            .build()
                            .post()
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(30))
                            .block();

                    result = objectMapper.readValue(response, Map.class);

                    // Verificar si el error es "ya existe"
                    String errMsg = result.containsKey("errors") ? result.get("errors").toString() : "";
                    if (errMsg.toLowerCase().contains("ya existe") || errMsg.toLowerCase().contains("already exists")) {
                        // Incrementar número y reintentar
                        siguienteNumero++;
                        body.put("numero", siguienteNumero);
                        log.warn("Número {} ya existe en Nubefact, reintentando con {}...", siguienteNumero - 1, siguienteNumero);
                        intentos++;
                        continue;
                    }

                    // Si no es error de "ya existe", salir del loop
                    break;

                } catch (WebClientResponseException e) {
                    String responseBody = e.getResponseBodyAsString();
                    log.error("Error HTTP NubeFact: {} - Body: {}", e.getStatusCode(), responseBody);
                    String errorMessage = "Error de conexión con NubeFact: " + e.getStatusCode();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);
                        if (errorResponse != null && errorResponse.containsKey("errors")) {
                            errorMessage = "Error NubeFact: " + errorResponse.get("errors").toString();
                        }
                    } catch (Exception parseEx) {
                        log.error("No se pudo parsear el error de NubeFact", parseEx);
                    }
                    return crearRespuestaError(errorMessage);
                }
            }

            if (result == null) {
                return crearRespuestaError("No se pudo emitir el comprobante después de " + maxIntentos + " intentos");
            }

            result.put("serie_generada", serie);
            Object autogenerado = result.get("numero");
            result.put("numero_generado", autogenerado != null ? autogenerado : siguienteNumero);
            result.put("numero_completo", serie + "-" + (autogenerado != null ? autogenerado : siguienteNumero));

            log.info("Respuesta de NubeFact: aceptada={}", result.get("aceptada_por_sunat"));

            return result;

        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Error HTTP NubeFact: {} - Body: {}", e.getStatusCode(), responseBody);

            String errorMessage = "Error de conexión con NubeFact: " + e.getStatusCode();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);
                if (errorResponse != null && errorResponse.containsKey("errors")) {
                    errorMessage = "Error NubeFact: " + errorResponse.get("errors").toString();
                }
            } catch (Exception parseEx) {
                log.error("No se pudo parsear el error de NubeFact", parseEx);
            }

            return crearRespuestaError(errorMessage);
        } catch (Exception e) {
            log.error("Error al emitir comprobante", e);
            return crearRespuestaError("Error al conectar con SUNAT: " + e.getMessage());
        }
    }

    private Map<String, Object> crearRespuestaError(String mensaje) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("errors", mensaje);
        error.put("aceptada_por_sunat", false);
        error.put("sunat_description", mensaje);
        error.put("error", true);
        return error;
    }

    public record ItemComprobante(
            String codigo,
            String descripcion,
            double cantidad,
            double precioUnitario,
            String unidad
    ) {
        public ItemComprobante(String descripcion, double cantidad, double precioUnitario, String unidad) {
            this(null, descripcion, cantidad, precioUnitario, unidad);
        }
    }
}