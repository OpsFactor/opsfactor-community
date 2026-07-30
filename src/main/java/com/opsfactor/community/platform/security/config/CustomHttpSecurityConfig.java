package com.opsfactor.community.platform.security.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracao de seguranca simples da edicao Community.
 *
 * <p>O backend expõe HTTP Basic para a SPA separada e mantem apenas endpoints
 * abertos estritamente necessarios para bootstrap, health check e runtime info.
 * Mecanismos avancados de identidade ficam no overlay Enterprise.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true) // permite o uso de @Secured, @RolesAllowed
public class CustomHttpSecurityConfig {

    private static final String[] PUBLIC_REQUEST_MATCHERS = {
            "/h2-console/**",
            "/api/open/**",
            "/health-status",
            "/actuator/health"
    };

    /**
     * Forca canal HTTPS quando o runtime externo exigir TLS ponta-a-ponta.
     *
     * <p>O default precisa ser falso para testes e execucoes locais sem
     * `application.properties` do modulo web.</p>
     */
    @Value("${enforce.https:false}")
    private Boolean enforceHttps;

    /**
     * Encoder padrao da edicao Community para credenciais locais.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

    /**
     * Configuracao Community para backend orientado a API.
     * O Community nao serve mais paginas Thymeleaf de login/front-end legado: a UI roda em um servidor separado
     * e autentica por HTTP Basic contra os endpoints do backend.
     *
     * OAuth, SSO, JWT/Bearer tokens, lockout e demais politicas avancadas pertencem ao Enterprise. Mantemos esta
     * classe propositalmente pequena para que o repo aberto exponha apenas o mecanismo basico de usuario/senha.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        if (enforceHttps) {
            httpSecurity.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        httpSecurity
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_REQUEST_MATCHERS).permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .logout(logout -> logout
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)));

        return httpSecurity.build();
    }

    /**
     * Fonte CORS aberta para a SPA desacoplada consumir os endpoints Community.
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
