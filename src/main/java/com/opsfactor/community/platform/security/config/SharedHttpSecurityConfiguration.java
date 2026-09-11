package com.opsfactor.community.platform.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Infraestrutura HTTP compartilhada, independente da chain da distribuição.
 *
 * <p>O bootstrap privado pode excluir a chain Basic Community sem perder o
 * encoder das credenciais locais, CORS ou a segurança de métodos. Não há uma
 * segunda chain catch-all nesta configuração: cada executável seleciona a sua
 * configuração de autenticação por composição de classes, não por flags.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SharedHttpSecurityConfiguration {

    /**
     * Encoder padrão para as credenciais locais de ambas as distribuições.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

    /**
     * Preserva a política CORS já usada pelas APIs e pelo login Basic da SPA.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/logout", configuration);
        source.registerCorsConfiguration("/health-status", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        return source;

    }

}
