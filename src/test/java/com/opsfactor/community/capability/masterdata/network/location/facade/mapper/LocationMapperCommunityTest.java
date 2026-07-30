package com.opsfactor.community.capability.masterdata.network.location.facade.mapper;

import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Testes de contrato do mapper Community de locations.
 *
 * <p>O Community publica locations sem caracteristicas dinamicas, sem
 * visualizacao de mapa e sem cluster de location embutido no DTO. Esses
 * conceitos podem existir em outros fluxos, mas nao devem reaparecer aqui como
 * codigo comentado ou como payload preenchido por engano.</p>
 */
class LocationMapperCommunityTest {

    @Test
    void convertSemCaracteristicasLocationShouldKeepCommunityOnlyFields() {

        Location location = new Location("LOC-1");
        location.setDescricao("Location 1");
        location.setPais("BR");
        location.setEstado("SP");
        location.setCidade("Sao Paulo");
        location.setPlanejaSupply(true);
        location.setPlanejaProducao(false);
        location.setConsideraRestricaoLinhaInbound(false);
        location.setIncluiDemandaIndiretaNoSafetyStock(false);
        location.setConsideraRestricaoProducao(true);

        LocationDTO locationDTO =
                LocationMapper.convertSemCaracteristicasLocation(location);

        Assertions.assertEquals("LOC-1", locationDTO.getId());
        Assertions.assertEquals("Location 1", locationDTO.getDescription());
        Assertions.assertEquals("BR", locationDTO.getCountry());
        Assertions.assertEquals("SP", locationDTO.getState());
        Assertions.assertEquals("Sao Paulo", locationDTO.getCity());
        Assertions.assertNull(locationDTO.getLatitude());
        Assertions.assertNull(locationDTO.getLongitude());
        Assertions.assertTrue(locationDTO.getShowInSupplyPlanningBook());
        Assertions.assertFalse(locationDTO.getShowInProductionPlanningBook());
        Assertions.assertFalse(locationDTO.getApplyInboundConstraints());
        Assertions.assertFalse(locationDTO.getSafetyStockConsiderIndirectDemand());
        Assertions.assertTrue(locationDTO.getApplyProductionConstraints());

    }

    @Test
    void convertSemCaracteristicasLocationShouldExposeInboundConstraintsDomainDefault() {

        Location location = new Location("LOC-DEFAULT");

        LocationDTO locationDTO =
                LocationMapper.convertSemCaracteristicasLocation(location);

        /*
         * O campo persistido e nullable para preservar cadastros legados. O
         * contrato DTO, no entanto, deve publicar o mesmo default efetivo
         * usado pelo heuristico de Supply: respeitar o lead time inbound.
         */
        Assertions.assertTrue(locationDTO.getApplyInboundConstraints());

    }

    @Test
    void convertShouldMapInboundConstraintToLocation() {

        LocationMapper locationMapper = new LocationMapper() {
        };
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId("LOC-CONVERT");
        locationDTO.setApplyInboundConstraints(false);

        Location location = locationMapper.convert(locationDTO);

        Assertions.assertFalse(location.getConsideraRestricaoLinhaInbound());

    }

    @Test
    void convertSemCaracteristicasLocationShouldExposeOnlyConfiguredSafetyStockOverride() {

        Location location = new Location("LOC-SAFETY-STOCK");
        location.setIncluiDemandaIndiretaNoSafetyStock(null);

        LocationDTO locationDTO =
                LocationMapper.convertSemCaracteristicasLocation(location);

        /*
         * O DTO administrativo nao calcula o valor efetivo pois o fallback e
         * global. Assim, null permite a tela limpar o override sem gravar uma
         * copia do parametro global na Location.
         */
        Assertions.assertNull(locationDTO.getSafetyStockConsiderIndirectDemand());

    }

    @Test
    void convertShouldMapSafetyStockOverrideToLocation() {

        LocationMapper locationMapper = new LocationMapper() {
        };
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setId("LOC-SAFETY-STOCK-CONVERT");
        locationDTO.setSafetyStockConsiderIndirectDemand(false);

        Location location = locationMapper.convert(locationDTO);

        Assertions.assertEquals(
                Boolean.FALSE,
                location.getIncluiDemandaIndiretaNoSafetyStockCadastrado());

    }

    @Test
    void locationMapperSourceShouldNotKeepLegacyClusterDtoImplementationCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path locationMapperSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/network/location/facade/mapper/LocationMapper.java");
        List<String> sourceLines = Files.readAllLines(
                locationMapperSourcePath,
                StandardCharsets.UTF_8);
        List<String> violations = new ArrayList<>();

        /*
         * `LocationDTO` compartilhado nao possui mais locationCluster. Se esse
         * contrato for reaberto, deve nascer em DTO/mapper ativo e testado, nao
         * como bloco antigo comentado no mapper Community.
         */
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);
            if (sourceLine.trim().startsWith("//")
                    && (sourceLine.contains("ClusterLocationsDTO")
                    || sourceLine.contains("setLocationCluster"))) {
                violations.add(
                        communityWorkspaceDirectory.relativize(locationMapperSourcePath)
                                + ":"
                                + (lineIndex + 1)
                                + ": "
                                + sourceLine.trim());
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "LocationMapper Community nao deve manter DTO de cluster de location comentado:\n"
                        + String.join("\n", violations));

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
