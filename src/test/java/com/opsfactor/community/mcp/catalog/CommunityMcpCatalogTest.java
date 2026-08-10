package com.opsfactor.community.mcp.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prova a navegacao progressiva e a disponibilidade dos temas Community.
 */
class CommunityMcpCatalogTest {

    private final CommunityMcpCatalog communityMcpCatalog = new CommunityMcpCatalog();

    @Test
    void shouldExposeOnlyImmediateRootThemes() {

        McpCatalogNode rootNode = communityMcpCatalog.navigate(null, null);

        assertThat(rootNode.children())
                .extracting(McpCatalogNode::id)
                .containsExactly(
                        "master-data",
                        "transactional-data",
                        "configuration",
                        "planning-data",
                        "report",
                        "planning-book",
                        "process-execution",
                        "admin");
        assertThat(rootNode.children())
                .allSatisfy(themeNode -> assertThat(themeNode.children()).isEmpty());

    }

    @Test
    void shouldNavigateThemeGroupSectionAndTopicWithoutDumpingDescendants() {

        McpCatalogNode themeNode = communityMcpCatalog.navigate("master-data", null);
        McpCatalogNode groupNode = communityMcpCatalog.navigate("master-data/material-location", null);
        McpCatalogNode sectionNode = communityMcpCatalog.navigate("master-data/material-location/records", null);
        McpCatalogNode topicNode = communityMcpCatalog.navigate(
                "master-data/material-location/records/material",
                null);

        assertThat(themeNode.children()).extracting(McpCatalogNode::type)
                .containsOnly(McpCatalogNodeType.GROUP);
        assertThat(groupNode.children()).extracting(McpCatalogNode::type)
                .containsOnly(McpCatalogNodeType.SECTION);
        assertThat(sectionNode.children()).extracting(McpCatalogNode::type)
                .containsOnly(McpCatalogNodeType.TOPIC);
        assertThat(topicNode.capabilityId()).isEqualTo("data.master-data.material");
        assertThat(topicNode.operations())
                .containsExactly(McpCapabilityOperation.QUERY, McpCapabilityOperation.UPDATE);

    }

    @Test
    void shouldIncludeConfigurationOutsideDataOperations() {

        McpCatalogNode demandConfiguration = communityMcpCatalog.navigate(
                "configuration/demand-planning/forecast",
                null);
        McpCatalogNode supplyConfiguration = communityMcpCatalog.navigate(
                "configuration/supply-planning/execution",
                null);

        assertThat(demandConfiguration.children())
                .extracting(McpCatalogNode::capabilityId)
                .containsExactly(
                        "configuration.demand.execution-profile",
                        "configuration.demand.cluster-level");
        assertThat(supplyConfiguration.children())
                .extracting(McpCatalogNode::capabilityId)
                .containsExactly("configuration.supply.execution-profile");

    }

    @Test
    void shouldSearchLeafCapabilitiesAcrossThemes() {

        McpCatalogNode searchResult = communityMcpCatalog.navigate(null, "material flows");

        assertThat(searchResult.children())
                .extracting(McpCatalogNode::capabilityId)
                .containsExactly("report.supply.material-flows");

    }

    @Test
    void shouldRejectUnknownPathAndCapability() {

        assertThatThrownBy(() -> communityMcpCatalog.navigate("unknown", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown MCP catalog path");
        assertThatThrownBy(() -> communityMcpCatalog.getCapability("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown MCP capability");

    }

}
