package com.carwash.sistema.service;

import com.carwash.sistema.model.ConfiguracionSistema;
import com.carwash.sistema.repository.ConfiguracionSistemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionSistemaService {

    private final ConfiguracionSistemaRepository configuracionRepository;

    /**
     * Obtiene la configuración del sistema (crea una si no existe)
     */
    public ConfiguracionSistema getConfiguracion() {
        return configuracionRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::crearConfiguracionPorDefecto);
    }

    /**
     * Crea configuración por defecto
     */
    @Transactional
    public ConfiguracionSistema crearConfiguracionPorDefecto() {
        log.info("Creando configuración por defecto del sistema");
        
        ConfiguracionSistema config = new ConfiguracionSistema();
        config.setNombreNegocio("Car Detailing Gadiel");
        config.setRuc("20615184421");
        config.setVersion("1.0.0");
        
        return configuracionRepository.save(config);
    }

    /**
     * Guarda cambios en la configuración
     */
    @Transactional
    public ConfiguracionSistema guardarConfiguracion(ConfiguracionSistema configuracion) {
        log.info("Guardando configuración del sistema");
        return configuracionRepository.save(configuracion);
    }

    /**
     * Actualiza solo datos del negocio
     */
    @Transactional
    public ConfiguracionSistema actualizarDatosNegocio(String nombre, String ruc, 
                                                        String direccion, String telefono, String email) {
        ConfiguracionSistema config = getConfiguracion();
        config.setNombreNegocio(nombre);
        config.setRuc(ruc);
        config.setDireccion(direccion);
        config.setTelefono(telefono);
        config.setEmail(email);
        
        return configuracionRepository.save(config);
    }

    /**
     * Actualiza configuración de facturación
     */
    @Transactional
    public ConfiguracionSistema actualizarFacturacion(String token, String modo, 
                                                       String serieBoleta, String serieFactura) {
        ConfiguracionSistema config = getConfiguracion();
        config.setNubefactToken(token);
        config.setNubefactModo(modo);
        config.setSerieBoleta(serieBoleta);
        config.setSerieFactura(serieFactura);
        config.setFacturacionActiva(token != null && !token.isEmpty());
        
        return configuracionRepository.save(config);
    }

    /**
     * Actualiza tema y personalización
     */
    @Transactional
    public ConfiguracionSistema actualizarPersonalizacion(String colorPrimario, String colorSecundario, 
                                                           String tema, String tamanioTexto) {
        ConfiguracionSistema config = getConfiguracion();
        config.setColorPrimario(colorPrimario);
        config.setColorSecundario(colorSecundario);
        config.setTema(tema);
        config.setTamanioTexto(tamanioTexto);
        
        return configuracionRepository.save(config);
    }

    /**
     * Registra último backup
     */
    @Transactional
    public void registrarBackup() {
        ConfiguracionSistema config = getConfiguracion();
        config.setUltimoBackup(LocalDateTime.now());
        configuracionRepository.save(config);
        
        log.info("Backup registrado: {}", config.getUltimoBackup());
    }

    /**
     * Actualiza estado de dispositivos
     */
    @Transactional
    public ConfiguracionSistema actualizarDispositivos(String impresora, Boolean impresoraConectada,
                                                        String tablet, Boolean tabletConectada) {
        ConfiguracionSistema config = getConfiguracion();
        config.setImpresoraTermica(impresora);
        config.setImpresoraConectada(impresoraConectada);
        config.setTabletCajero(tablet);
        config.setTabletConectada(tabletConectada);
        
        return configuracionRepository.save(config);
    }
}