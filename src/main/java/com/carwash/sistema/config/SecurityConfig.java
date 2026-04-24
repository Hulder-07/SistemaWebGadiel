package com.carwash.sistema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**",
                                 "/imagenes/**", "/webjars/**").permitAll()

                // Solo ADMIN puede gestionar usuarios y reportes
                .requestMatchers("/usuarios/**").hasAuthority("ADMIN")
                .requestMatchers("/reportes/**").hasAnyAuthority("ADMIN", "CAJERO")

                // VISUALIZADOR solo puede ver — bloquear rutas de modificación
                .requestMatchers(
                    "/ordenes/guardar", "/ordenes/*/eliminar", "/ordenes/*/cambiar-estado",
                    "/ventas/guardar", "/ventas/*/eliminar",
                    "/productos/guardar", "/productos/*/eliminar",
                    "/clientes/guardar", "/clientes/*/eliminar",
                    "/vehiculos/guardar", "/vehiculos/*/eliminar",
                    "/servicios/guardar", "/servicios/*/eliminar",
                    "/categorias/guardar", "/categorias/*/eliminar",
                    "/configuracion/**", "/sunat/emitir"
                ).hasAnyAuthority("ADMIN", "CAJERO", "LAVADOR")

                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(-1)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}