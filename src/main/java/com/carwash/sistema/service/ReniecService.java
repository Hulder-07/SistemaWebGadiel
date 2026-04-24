package com.carwash.sistema.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ReniecService {

    @Value("${reniec.api.url:https://apiperu.dev/api/dni/}")
    private String reniecApiUrl;

    @Value("${reniec.api.token:}")
    private String apiToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consulta DNI en RENIEC vía apiperu.dev
     * 
     * @param dni Número de DNI (8 dígitos)
     * @return Datos de la persona o null si no se encuentra
     */
    public Map<String, String> consultarDni(String dni) {
        if (dni == null || dni.length() != 8 || !dni.matches("\\d+")) {
            log.warn("DNI inválido: {}", dni);
            return crearError("DNI inválido. Debe tener 8 dígitos numéricos.");
        }

        // Si no hay token configurado, retornar datos de ejemplo
        if (apiToken == null || apiToken.isEmpty() || apiToken.equals("TU_TOKEN_AQUI")) {
            log.warn("⚠️ Token de RENIEC no configurado. Usando datos de ejemplo.");
            return getDatosEjemplo(dni);
        }

        try {
            log.info("🔍 Consultando DNI en RENIEC (apiperu.dev): {}", dni);

            // API Perú usa el token en el header Authorization con Bearer
            String response = WebClient.builder()
                    .baseUrl(reniecApiUrl)
                    .defaultHeader("Authorization", "Bearer " + apiToken)
                    .defaultHeader("Accept", "application/json")
                    .build()
                    .get()
                    .uri(dni)  // El DNI va en la URL directamente
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return parsearRespuestaApiPeru(response);

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("❌ Token inválido o expirado");
            return crearError("Token de API inválido. Verifique su configuración.");
            
        } catch (WebClientResponseException.NotFound e) {
            log.warn("⚠️ DNI no encontrado en RENIEC: {}", dni);
            return crearError("DNI no encontrado en RENIEC");
            
        } catch (WebClientResponseException e) {
            log.error("❌ Error HTTP al consultar RENIEC: {} - {}", e.getStatusCode(), e.getMessage());
            return crearError("Error al consultar RENIEC: " + e.getStatusCode());
            
        } catch (Exception e) {
            log.error("❌ Error al consultar RENIEC para DNI: {}", dni, e);
            return crearError("Error de conexión con RENIEC");
        }
    }

    /**
     * Parsea la respuesta de apiperu.dev
     * 
     * Formato esperado de API Perú:
     * {
     *   "success": true,
     *   "data": {
     *     "numero": "12345678",
     *     "nombre_completo": "PEREZ GARCIA JUAN CARLOS",
     *     "nombres": "JUAN CARLOS",
     *     "apellido_paterno": "PEREZ",
     *     "apellido_materno": "GARCIA",
     *     "codigo_verificacion": "5",
     *     "fecha_nacimiento": "01/01/1990",
     *     "sexo": "M"
     *   }
     * }
     */
    private Map<String, String> parsearRespuestaApiPeru(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            
            // Verificar si la respuesta fue exitosa
            if (!json.has("success") || !json.get("success").asBoolean()) {
                String mensaje = json.has("message") ? json.get("message").asText() : "Error desconocido";
                log.error("❌ API Perú retornó error: {}", mensaje);
                return crearError(mensaje);
            }
            
            // Obtener el objeto data
            JsonNode data = json.get("data");
            if (data == null) {
                log.error("❌ Respuesta de API Perú sin campo 'data'");
                return crearError("Respuesta inválida de RENIEC");
            }
            
            Map<String, String> datos = new HashMap<>();
            datos.put("nombres", data.has("nombres") ? data.get("nombres").asText() : "");
            datos.put("apellidoPaterno", data.has("apellido_paterno") ? data.get("apellido_paterno").asText() : "");
            datos.put("apellidoMaterno", data.has("apellido_materno") ? data.get("apellido_materno").asText() : "");
            
            log.info("✅ Datos obtenidos de RENIEC: {} {} {}", 
                    datos.get("nombres"), 
                    datos.get("apellidoPaterno"), 
                    datos.get("apellidoMaterno"));
            
            return datos;
            
        } catch (Exception e) {
            log.error("❌ Error al parsear respuesta de RENIEC", e);
            return crearError("Error al procesar respuesta de RENIEC");
        }
    }

    /**
     * Crea un mapa con mensaje de error
     */
    private Map<String, String> crearError(String mensaje) {
        Map<String, String> error = new HashMap<>();
        error.put("error", mensaje);
        return error;
    }

    /**
     * Datos de ejemplo para desarrollo/pruebas
     * ELIMINA ESTE MÉTODO cuando tengas el token real configurado
     */
    private Map<String, String> getDatosEjemplo(String dni) {
        Map<String, String> datos = new HashMap<>();
        
        // Simular diferentes personas según el DNI
        switch (dni) {
            case "12345678":
                datos.put("nombres", "JUAN CARLOS");
                datos.put("apellidoPaterno", "PEREZ");
                datos.put("apellidoMaterno", "GARCIA");
                break;
            case "87654321":
                datos.put("nombres", "MARIA ELENA");
                datos.put("apellidoPaterno", "RODRIGUEZ");
                datos.put("apellidoMaterno", "LOPEZ");
                break;
            case "11111111":
                datos.put("nombres", "PEDRO JOSE");
                datos.put("apellidoPaterno", "GONZALES");
                datos.put("apellidoMaterno", "FLORES");
                break;
            case "44556677":
                datos.put("nombres", "ANA LUCIA");
                datos.put("apellidoPaterno", "MARTINEZ");
                datos.put("apellidoMaterno", "DIAZ");
                break;
            case "99887766":
                datos.put("nombres", "CARLOS ALBERTO");
                datos.put("apellidoPaterno", "RAMIREZ");
                datos.put("apellidoMaterno", "TORRES");
                break;
            default:
                // Generar datos basados en el DNI para cualquier otro número
                datos.put("nombres", "CLIENTE " + dni.substring(0, 2));
                datos.put("apellidoPaterno", "EJEMPLO");
                datos.put("apellidoMaterno", "RENIEC");
                break;
        }
        
        log.info("⚠️ MODO DESARROLLO: Retornando datos de ejemplo para DNI: {}", dni);
        return datos;
    }

    /**
     * Valida formato de DNI peruano
     */
    public boolean validarDni(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }

    /**
     * Formatea nombre completo
     */
    public String formatearNombreCompleto(Map<String, String> datos) {
        if (datos == null || datos.containsKey("error")) {
            return null;
        }
        
        return String.format("%s %s %s",
                datos.getOrDefault("nombres", ""),
                datos.getOrDefault("apellidoPaterno", ""),
                datos.getOrDefault("apellidoMaterno", "")
        ).trim();
    }
}