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
import org.springframework.security.web.AuthenticationEntryPoint;
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

    /**
     * Retorna 401 sem {@code WWW-Authenticate} para a SPA tratar a falha de
     * credencial no proprio formulario, sem acionar o dialogo nativo do navegador.
     */
    private static final AuthenticationEntryPoint SPA_AUTHENTICATION_ENTRY_POINT =
            (request, response, authenticationException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

    private static final String[] PUBLIC_REQUEST_MATCHERS = {
            "/h2-console/**",
            "/api/open/**",
            "/health-status",
            "/actuator/health"
    };

    private static final String[] OPENAPI_PUBLIC_REQUEST_MATCHERS = {
            "/app/swagger-ui/**",
            "/app/api-docs/**"
    };

    /**
     * Forca canal HTTPS quando o runtime externo exigir TLS ponta-a-ponta.
     *
     * <p>O default precisa ser falso para testes e execucoes locais sem
     * `application.properties` do modulo web.</p>
     */
    @Value("${enforce.https:false}")
    private Boolean enforceHttps;

    @Value("${opsfactor.openapi.enabled:false}")
    private Boolean openApiEnabled;

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
                        .requestMatchers(publicRequestMatchers()).permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                // O handler padrao de HTTP Basic inclui WWW-Authenticate e abre
                // um popup nativo quando a senha do formulario esta incorreta.
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(SPA_AUTHENTICATION_ENTRY_POINT))
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(SPA_AUTHENTICATION_ENTRY_POINT))
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
     * Mantem o conjunto publico minimo do Community e acrescenta a
     * documentacao somente no overlay Enterprise que a disponibiliza.
     */
    private String[] publicRequestMatchers() {

        if (!Boolean.TRUE.equals(openApiEnabled)) {
            return PUBLIC_REQUEST_MATCHERS;
        }

        String[] publicRequestMatchers = new String[
                PUBLIC_REQUEST_MATCHERS.length + OPENAPI_PUBLIC_REQUEST_MATCHERS.length];
        System.arraycopy(PUBLIC_REQUEST_MATCHERS, 0, publicRequestMatchers, 0, PUBLIC_REQUEST_MATCHERS.length);
        System.arraycopy(
                OPENAPI_PUBLIC_REQUEST_MATCHERS,
                0,
                publicRequestMatchers,
                PUBLIC_REQUEST_MATCHERS.length,
                OPENAPI_PUBLIC_REQUEST_MATCHERS.length);
        return publicRequestMatchers;

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
