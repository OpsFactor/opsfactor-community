package com.opsfactor.community.mcp;

import com.opsfactor.community.mcp.catalog.CommunityMcpCatalog;
import com.opsfactor.community.mcp.execution.CommunityMcpCapabilityRegistry;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerObjectMapperAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.ToolCallbackConverterAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerSpecificationFactoryAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.function.RouterFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Prova que o starter WebMVC cria o transporte Streamable HTTP e consegue
 * escanear a classe real das quatro tools, sem subir banco ou servidor TCP.
 */
class CommunityMcpServerAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    McpServerObjectMapperAutoConfiguration.class,
                    ToolCallbackConverterAutoConfiguration.class,
                    McpServerAutoConfiguration.class,
                    McpServerSpecificationFactoryAutoConfiguration.class,
                    McpServerAnnotationScannerAutoConfiguration.class,
                    McpServerStreamableHttpWebMvcAutoConfiguration.class))
            .withPropertyValues(
                    "spring.ai.mcp.server.enabled=true",
                    "spring.ai.mcp.server.name=opsfactor-community-test",
                    "spring.ai.mcp.server.version=0.1.0-test",
                    "spring.ai.mcp.server.protocol=STREAMABLE",
                    "spring.ai.mcp.server.annotation-scanner.enabled=true")
            .withBean(CommunityMcpCatalog.class)
            .withBean(CommunityMcpCapabilityRegistry.class, () -> mock(CommunityMcpCapabilityRegistry.class))
            .withBean(CommunityMcpTools.class);

    @Test
    void shouldCreateStreamableHttpTransportAndRouter() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebMvcStreamableServerTransportProvider.class);
            assertThat(context).hasSingleBean(RouterFunction.class);
            assertThat(context.getBean(McpServerStreamableHttpProperties.class).getMcpEndpoint())
                    .isEqualTo("/mcp");
        });

    }

}
