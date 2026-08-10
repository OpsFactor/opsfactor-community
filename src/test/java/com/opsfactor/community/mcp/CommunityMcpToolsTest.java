package com.opsfactor.community.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.mcp.catalog.CommunityMcpCatalog;
import com.opsfactor.community.mcp.catalog.McpCapabilityOperation;
import com.opsfactor.community.mcp.catalog.McpCatalogNode;
import com.opsfactor.community.mcp.catalog.McpCatalogNodeType;
import com.opsfactor.community.mcp.dto.McpCapabilityRequest;
import com.opsfactor.community.mcp.execution.CommunityMcpCapabilityRegistry;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Valida a borda das quatro tools MCP sem inicializar banco ou servidor HTTP.
 */
class CommunityMcpToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CommunityMcpCatalog communityMcpCatalog = mock(CommunityMcpCatalog.class);
    private final CommunityMcpCapabilityRegistry communityMcpCapabilityRegistry =
            mock(CommunityMcpCapabilityRegistry.class);
    private final CommunityMcpTools communityMcpTools = new CommunityMcpTools(
            communityMcpCatalog,
            communityMcpCapabilityRegistry);

    @Test
    void shouldPublishOnlyFourStableMcpTools() {

        List<String> toolNames = Arrays.stream(CommunityMcpTools.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(McpTool.class))
                .filter(annotation -> annotation != null)
                .map(McpTool::name)
                .sorted()
                .toList();

        assertThat(toolNames).containsExactly(
                "opsfactor_catalog",
                "opsfactor_query",
                "opsfactor_run",
                "opsfactor_update");

    }

    @Test
    void shouldForwardQueryPayloadToRegisteredCapability() {

        JsonNode payload = objectMapper.createObjectNode().put("supplyPlanId", 10L);
        JsonNode result = objectMapper.createObjectNode().put("result", "ok");
        McpCapabilityRequest request = new McpCapabilityRequest(
                "report.supply.material-flows",
                payload,
                null);
        when(communityMcpCatalog.getCapability(request.capabilityId()))
                .thenReturn(topic(request.capabilityId(), McpCapabilityOperation.QUERY));
        when(communityMcpCapabilityRegistry.query(request.capabilityId(), payload)).thenReturn(result);

        assertThat(communityMcpTools.query(request).data()).isSameAs(result);
        verify(communityMcpCapabilityRegistry).query(request.capabilityId(), payload);

    }

    @Test
    void shouldRequireExplicitConfirmationBeforeUpdate() {

        JsonNode payload = objectMapper.createArrayNode();
        McpCapabilityRequest request = new McpCapabilityRequest(
                "data.master-data.material",
                payload,
                false);
        when(communityMcpCatalog.getCapability(request.capabilityId()))
                .thenReturn(topic(request.capabilityId(), McpCapabilityOperation.UPDATE));

        assertThatThrownBy(() -> communityMcpTools.update(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmed=true");
        verify(communityMcpCapabilityRegistry, never()).update(request.capabilityId(), payload);

    }

    @Test
    void shouldRejectOperationNotDeclaredByTopic() {

        McpCapabilityRequest request = new McpCapabilityRequest(
                "data.planning.inventory-plan",
                objectMapper.createObjectNode().put("supplyPlanId", 1L),
                true);
        when(communityMcpCatalog.getCapability(request.capabilityId()))
                .thenReturn(topic(request.capabilityId(), McpCapabilityOperation.QUERY));

        assertThatThrownBy(() -> communityMcpTools.update(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not support UPDATE");

    }

    private McpCatalogNode topic(
            String capabilityId,
            McpCapabilityOperation... operations) {

        return new McpCatalogNode(
                "topic",
                "Topic",
                "Test topic",
                McpCatalogNodeType.TOPIC,
                "theme/group/section/topic",
                List.of(),
                capabilityId,
                List.of(operations),
                "input",
                "output",
                true);

    }

}
