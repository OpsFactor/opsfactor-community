package com.opsfactor.community.platform.security.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contratos estruturais da seguranca HTTP Community.
 */
class CustomHttpSecurityConfigTest {

    @Test
    void customHttpSecurityConfigShouldDeclareCommunitySecurityAnnotations() {

        Assertions.assertTrue(CustomHttpSecurityConfig.class.isAnnotationPresent(Configuration.class));
        Assertions.assertTrue(CustomHttpSecurityConfig.class.isAnnotationPresent(EnableWebSecurity.class));

        EnableMethodSecurity enableMethodSecurity =
                CustomHttpSecurityConfig.class.getAnnotation(EnableMethodSecurity.class);

        Assertions.assertNotNull(enableMethodSecurity);
        Assertions.assertTrue(enableMethodSecurity.securedEnabled());

    }

    @Test
    void passwordEncoderShouldUseBCrypt() {

        CustomHttpSecurityConfig customHttpSecurityConfig = new CustomHttpSecurityConfig();

        PasswordEncoder passwordEncoder = customHttpSecurityConfig.passwordEncoder();

        Assertions.assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);

    }

    @Test
    void publicRequestMatchersShouldExposeOnlyOpenAndHealthSurfaces() throws Exception {

        Field publicRequestMatchersField =
                CustomHttpSecurityConfig.class.getDeclaredField("PUBLIC_REQUEST_MATCHERS");
        publicRequestMatchersField.setAccessible(true);

        String[] publicRequestMatchers = (String[]) publicRequestMatchersField.get(null);
        Set<String> publicRequestMatcherSet = Arrays.stream(publicRequestMatchers).collect(Collectors.toSet());

        Assertions.assertEquals(
                Set.of("/h2-console/**", "/api/open/**", "/health-status", "/actuator/health"),
                publicRequestMatcherSet);

    }

    @Test
    void communitySecurityModuleShouldNotDeclareAdvancedIdentityDependencies() throws IOException {

        Path communitySecurityPomPath = resolveCommunitySecurityModuleDirectory().resolve("pom.xml");
        String communitySecurityPom = Files.readString(communitySecurityPomPath, StandardCharsets.UTF_8);
        List<String> forbiddenDependencyTokens = List.of(
                "oauth2",
                "oidc",
                "saml",
                "jwt",
                "resource-server");

        for (String forbiddenDependencyToken : forbiddenDependencyTokens) {
            /*
             * A seguranca Community deve continuar pequena e auditavel. Se algum
             * desses tokens aparecer no POM, provavelmente estamos reintroduzindo
             * SSO/Bearer/OIDC no repositorio aberto sem passar pelo overlay
             * Enterprise.
             */
            Assertions.assertFalse(
                    communitySecurityPom.toLowerCase().contains(forbiddenDependencyToken),
                    "Community security POM must not declare advanced identity dependency token: "
                            + forbiddenDependencyToken);
        }

    }

    @Test
    void communitySecurityConfigurationShouldRemainHttpBasicOnly() throws IOException {

        Path customHttpSecurityConfigPath = resolveCommunitySecurityModuleDirectory().resolve(
                "src/main/java/com/opsfactor/community/platform/security/config/CustomHttpSecurityConfig.java");
        String customHttpSecurityConfigSource = Files.readString(customHttpSecurityConfigPath, StandardCharsets.UTF_8);

        /*
         * O arquivo pode mencionar conceitualmente OAuth/SSO em Javadoc para
         * explicar o recorte, mas nao deve chamar configuradores Spring dessas
         * familias. A validacao fica nos tokens de API, nao em texto livre.
         */
        List<String> forbiddenSecurityConfigurerTokens = List.of(
                ".oauth2Login(",
                ".oauth2ResourceServer(",
                ".saml2Login(",
                ".openidLogin(",
                ".jwt(");

        for (String forbiddenSecurityConfigurerToken : forbiddenSecurityConfigurerTokens) {
            Assertions.assertFalse(
                    customHttpSecurityConfigSource.contains(forbiddenSecurityConfigurerToken),
                    "Community security configuration must stay HTTP Basic only and not call "
                            + forbiddenSecurityConfigurerToken);
        }

    }

    @Test
    void corsConfigurationShouldSupportSeparatedSpaWithoutCookieCredentials() {

        CustomHttpSecurityConfig customHttpSecurityConfig = new CustomHttpSecurityConfig();
        CorsConfigurationSource corsConfigurationSource = customHttpSecurityConfig.corsConfigurationSource();

        CorsConfiguration apiCorsConfiguration = corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/secured/user"));
        CorsConfiguration logoutCorsConfiguration = corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("POST", "/logout"));
        CorsConfiguration internalCorsConfiguration = corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/internal/admin"));

        /*
         * O front Community roda em servidor separado e usa HTTP Basic no
         * header Authorization. A configuracao deve habilitar esse header, mas
         * sem cookies/sessao cross-origin e sem abrir caminhos internos que nao
         * sejam API, logout ou health.
         */
        Assertions.assertNotNull(apiCorsConfiguration);
        Assertions.assertNotNull(logoutCorsConfiguration);
        Assertions.assertNull(internalCorsConfiguration);
        Assertions.assertEquals(List.of("*"), apiCorsConfiguration.getAllowedOriginPatterns());
        Assertions.assertEquals(
                List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"),
                apiCorsConfiguration.getAllowedHeaders());
        Assertions.assertEquals(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"),
                apiCorsConfiguration.getAllowedMethods());
        Assertions.assertEquals(Boolean.FALSE, apiCorsConfiguration.getAllowCredentials());

    }

    private Path resolveCommunitySecurityModuleDirectory() {

        Path currentWorkingDirectory = Path.of("").toAbsolutePath();

        /*
         * Apos o achatamento Maven, a trava de seguranca deve resolver sempre
         * o workspace Community raiz. O teste fica independente do diretorio
         * corrente escolhido por Maven, IntelliJ ou execucao focada.
         */
        while (currentWorkingDirectory != null
                && !"opsfactor-community".equals(currentWorkingDirectory.getFileName().toString())) {
            currentWorkingDirectory = currentWorkingDirectory.getParent();
        }
        if (currentWorkingDirectory == null) {
            throw new IllegalStateException("Could not resolve opsfactor-community workspace directory.");
        }
        return currentWorkingDirectory;

    }

}
