package com.opsfactor.community.capability.masterdata.production.routing.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.routing.integration.dto.RoteiroIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Protege a fronteira Community do identificador escalar de cluster de
 * roteiros.
 *
 * <p>O agregado Community pode preservar o identificador para permitir que o
 * overlay Enterprise reconcilie o relacionamento em batch. Ele nao pode,
 * contudo, publicar a coluna, resolve-la localmente nem depender da entidade
 * Enterprise que possui o cluster.</p>
 */
class RoteiroIntegrationMapperCommunityTest {

    @Test
    void shouldRejectRoutingClusterIdAtCommunityIntegrationBoundary() {

        RoteiroIntegrationMapper mapper = new RoteiroIntegrationMapper();
        RoteiroIntegrationDataDto dto = RoteiroIntegrationDataDto.builder()
                .routingClusterId("ROUTING_CLUSTER_01")
                .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> mapper.updateEntityNonPrimaryFieldsFromDTO(
                        new Roteiro(),
                        dto,
                        new RoteiroIntegrationSupportData(),
                        null));

    }

    @Test
    void shouldKeepRoutingClusterAsScalarAndHideItFromCommunityExport() throws Exception {

        Field routingClusterIdField = Roteiro.class.getDeclaredField("routingClusterId");
        Assertions.assertEquals(String.class, routingClusterIdField.getType());
        Assertions.assertNull(routingClusterIdField.getAnnotation(ManyToOne.class));
        Assertions.assertNull(routingClusterIdField.getAnnotation(OneToMany.class));

        RoteiroIntegrationMapper mapper = new RoteiroIntegrationMapper();
        Roteiro roteiro = new Roteiro();
        roteiro.setId("ROUTING_01");
        roteiro.setLocation(new Location("LOCATION_01"));
        roteiro.setMaterialOutput(new Produto("MATERIAL_01"));
        roteiro.setRoutingClusterId("ROUTING_CLUSTER_01");

        RoteiroIntegrationDataDto exportedDto = mapper.getDtoWithoutPrimaryKeyFromEntity(roteiro);
        ProcessedFileRow processedFileRow = mapper.convertEntityToProcessedFileRow(
                roteiro,
                new RoteiroIntegrationSupportData());

        Assertions.assertNull(exportedDto.routingClusterId);
        Assertions.assertEquals(7, processedFileRow.getRowSize());
        Assertions.assertFalse(mapper.getProcessedFileHeaders().stream()
                .anyMatch(header -> header.contains("Routing Cluster")));

    }

    @Test
    void shouldNotImportEnterpriseTypesIntoCommunityRoutingClasses() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        List<Path> routingSources = List.of(
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/capability/masterdata/production/routing/domain/Roteiro.java"),
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/capability/masterdata/production/routing/integration/mapper/RoteiroIntegrationMapper.java"),
                communityWorkspaceDirectory.resolve(
                        "src/main/java/com/opsfactor/community/capability/masterdata/production/routing/integration/mapper/RoteiroIntegrationSupportData.java"));

        for (Path routingSource : routingSources) {
            String source = Files.readString(routingSource, StandardCharsets.UTF_8);

            Assertions.assertFalse(
                    source.contains("import com.opsfactor.enterprise."),
                    () -> "Community routing source must not import Enterprise: " + routingSource);
        }

    }

    private static Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        while (currentDirectory != null
                && !"opsfactor-community".equals(currentDirectory.getFileName().toString())) {
            currentDirectory = currentDirectory.getParent();
        }
        if (currentDirectory == null) {
            throw new IllegalStateException("Could not resolve opsfactor-community workspace directory.");
        }
        return currentDirectory;

    }

}
