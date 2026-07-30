package com.opsfactor.community.capability.planningbook.facade;

import com.opsfactor.community.capability.planningbook.facade.dto.GroupDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure.KeyFigureDTOPadrao;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Guardas de contrato do Planning Book compartilhado pela edicao Community.
 */
class PlanningBookServiceCommunityContractTest {

    @Test
    void planningBookServiceSourceShouldNotKeepLegacyFrozenHorizonClassPopulationCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path planningBookServiceSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/planningbook/facade/PlanningBookService.java");
        String planningBookServiceSource = Files.readString(
                planningBookServiceSourcePath,
                StandardCharsets.UTF_8);

        /*
         * O Planning Book Community deve deixar explicito quais classes e grupos
         * sao calculados no fluxo ativo. Caminhos comentados de horizonte
         * congelado sugerem uma decoracao visual que nao esta sendo aplicada no
         * contrato atual da tela.
         */
        Assertions.assertFalse(
                planningBookServiceSource.contains("//        populaGroupDTOComAdditionalClassHorizonteCongelado("),
                "PlanningBookService nao deve manter populacao antiga de horizonte congelado comentada.");

    }

    @Test
    void planningBookDtoShouldAlwaysMaterializeDistinctMaterialLocationLeavesEvenWithLegacyFlagsDisabled() {

        PlanningBookService planningBookService = new PlanningBookService();
        Calendario calendario = getCalendario();

        ConfiguredView configuredView = new ConfiguredView(
                new ConfiguredView.ConfiguredViewCompositeKey(
                        "admin",
                        "Legacy aggregated view",
                        ConfiguredView.TipoView.DEMANDPLANNINGBOOK));
        configuredView.setExibeMateriais(false);
        configuredView.setExibeLocations(false);
        configuredView.setPermiteAlteracaoHorizonteCongelado(true);
        configuredView.setExibeVendaMediaHistorica(false);

        ParametrosGlobais parametrosGlobais = Mockito.mock(ParametrosGlobais.class);
        ClusterEParametrosProjection clusterEParametrosProjection =
                Mockito.mock(ClusterEParametrosProjection.class);
        Mockito.when(clusterEParametrosProjection.getParametrosGlobais())
                .thenReturn(parametrosGlobais);

        UnidadeMedida unidadeMedida = new UnidadeMedida("UN");
        ConfiguredViewProjection configuredViewProjection =
                Mockito.mock(ConfiguredViewProjection.class);
        Mockito.when(configuredViewProjection.getConfiguredView())
                .thenReturn(configuredView);
        Mockito.when(configuredViewProjection.getClusterEParametrosProjection())
                .thenReturn(clusterEParametrosProjection);
        Mockito.when(configuredViewProjection.getUnidadeMedidaView(parametrosGlobais))
                .thenReturn(unidadeMedida);
        Mockito.when(configuredViewProjection.getDetalhesSelecaoAAtualizar())
                .thenReturn(null);
        Mockito.when(configuredViewProjection.getErroAtualizacaoPorDetalheSelecao())
                .thenReturn(null);

        Produto materialA = getMaterial("MAT-A", "Material A");
        Produto materialB = getMaterial("MAT-B", "Material B");
        Location locationA = getLocation("LOC-A", "Location A");
        Location locationB = getLocation("LOC-B", "Location B");
        DFU dfuA = new DFU(materialA, locationA);
        DFU dfuB = new DFU(materialB, locationB);

        FiltroDFUProjection filtroDFUProjection = Mockito.mock(FiltroDFUProjection.class);
        FiltroDFUProjection primeiroEscopoProjection = Mockito.mock(FiltroDFUProjection.class);
        FiltroDFUProjection segundoEscopoProjection = Mockito.mock(FiltroDFUProjection.class);
        Mockito.when(primeiroEscopoProjection.getDFUs()).thenReturn(List.of(dfuB, dfuA));
        Mockito.when(segundoEscopoProjection.getDFUs()).thenReturn(List.of(dfuA));
        Mockito.when(configuredViewProjection.getDfuProjectionFiltrado())
                .thenReturn(filtroDFUProjection);

        PlanningBookDfuScope primeiroEscopo = Mockito.mock(PlanningBookDfuScope.class);
        PlanningBookDfuScope segundoEscopo = Mockito.mock(PlanningBookDfuScope.class);
        Mockito.when(primeiroEscopo.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                        filtroDFUProjection,
                        clusterEParametrosProjection))
                .thenReturn(primeiroEscopoProjection);
        Mockito.when(segundoEscopo.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                        filtroDFUProjection,
                        clusterEParametrosProjection))
                .thenReturn(segundoEscopoProjection);
        Mockito.when(configuredViewProjection.getPlanningBookDfuScopes())
                .thenReturn(new LinkedHashSet<>(List.of(primeiroEscopo, segundoEscopo)));

        KeyFigureProjection keyFigureProjection = Mockito.mock(KeyFigureProjection.class);
        Mockito.when(keyFigureProjection.getCalendario()).thenReturn(calendario);
        Mockito.when(keyFigureProjection.getConfiguredViewProjection())
                .thenReturn(configuredViewProjection);
        Mockito.when(keyFigureProjection.getKeyFiguresApresentadosEOrdenados())
                .thenReturn(List.of());

        PlanningBookDTO planningBookDTO =
                planningBookService.getPlanningBookDTO(keyFigureProjection);

        Assertions.assertEquals(2, planningBookDTO.groups.size());
        Assertions.assertEquals(
                List.of("LOC-A", "LOC-B"),
                planningBookDTO.groups.stream()
                        .map(groupDTO -> groupDTO.locationDescriptionCols.get("locationId"))
                        .toList());
        Assertions.assertEquals(
                List.of("MAT-A", "MAT-B"),
                planningBookDTO.groups.stream()
                        .map(groupDTO -> groupDTO.materialDescriptionCols.get("materialId"))
                        .toList());
        Assertions.assertTrue(planningBookDTO.groups.stream()
                .allMatch(groupDTO -> groupDTO.subGroups == null));
        Assertions.assertTrue(planningBookDTO.columnDefs.stream()
                .anyMatch(columnDefDTO -> "locationId".equals(columnDefDTO.field)));
        Assertions.assertTrue(planningBookDTO.columnDefs.stream()
                .anyMatch(columnDefDTO -> "materialId".equals(columnDefDTO.field)));
        Mockito.verify(configuredViewProjection, Mockito.never()).getExibeLocations();
        Mockito.verify(configuredViewProjection, Mockito.never()).getExibeMateriais();

    }

    @Test
    void zeroFillShouldNotReplaceUnavailableReasonWithArtificialNumericValue() throws Exception {

        Calendario calendario = getCalendario();
        String unavailablePeriod = calendario.getUltimaDataHorarioPeriodo(0).toString();
        KeyFigureDTOPadrao keyFigureDTO = new KeyFigureDTOPadrao(
                "Any derived value",
                EditMode.NOEDIT);
        keyFigureDTO.unavailableReasons = Map.of(
                unavailablePeriod,
                "MISSING_SOURCE_VALUE");
        GroupDTO groupDTO = new GroupDTO();
        groupDTO.keyFigures.add(keyFigureDTO);
        PlanningBookDTO planningBookDTO = PlanningBookDTO.builder()
                .groups(List.of(groupDTO))
                .build();
        PlanningBookService planningBookService = new PlanningBookService();

        Method zeroFillMethod = PlanningBookService.class.getDeclaredMethod(
                "atualizaPeriodosSemInformacaoComValorZero",
                PlanningBookDTO.class,
                Calendario.class);
        zeroFillMethod.setAccessible(true);
        zeroFillMethod.invoke(planningBookService, planningBookDTO, calendario);

        Assertions.assertFalse(keyFigureDTO.values.containsKey(unavailablePeriod));
        Assertions.assertEquals(
                "MISSING_SOURCE_VALUE",
                keyFigureDTO.unavailableReasons.get(unavailablePeriod));

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

    private static Calendario getCalendario() {

        return Calendario.criaCalendarioDeOffsetsPeriodos(
                Constantes.TamanhoBucket.MENSAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                0,
                0,
                2,
                0);

    }

    private static Produto getMaterial(String id, String descricao) {

        Produto material = new Produto();
        material.setId(id);
        material.setDescricao(descricao);
        return material;

    }

    private static Location getLocation(String id, String descricao) {

        Location location = new Location();
        location.setId(id);
        location.setDescricao(descricao);
        return location;

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

}
