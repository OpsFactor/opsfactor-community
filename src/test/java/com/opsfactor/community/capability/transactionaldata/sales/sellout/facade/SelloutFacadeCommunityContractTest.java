package com.opsfactor.community.capability.transactionaldata.sales.sellout.facade;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto.SelloutReportParametrosDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.FiltroMaterialLocationDeCombinacaoCaracteristicasDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SelloutFacadeCommunityContractTest {

    @Test
    public void getSelloutParaExportacaoAgGridShouldRejectCharacteristicFilterBeforeProjections() {

        SelloutFacade selloutFrontService = new SelloutFacade();
        SelloutReportParametrosDTO selloutReportParametrosDTO = new SelloutReportParametrosDTO();
        selloutReportParametrosDTO.materialLocationFilterDTO = new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO();
        selloutReportParametrosDTO.materialLocationFilterDTO.valuesByMaterialCharacteristicId =
                Map.of("BRAND", List.of("B1"));

        /*
         * Nao injetamos projection factories nem repository: filtros por
         * caracteristica precisam falhar na borda Community antes de qualquer
         * extracao de dados.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> selloutFrontService.getSelloutParaExportacaoAgGrid(selloutReportParametrosDTO));

    }

    @Test
    public void getSelloutParaExportacaoAgGridShouldRejectLocationCharacteristicFilterBeforeProjections() {

        SelloutFacade selloutFrontService = new SelloutFacade();
        SelloutReportParametrosDTO selloutReportParametrosDTO = new SelloutReportParametrosDTO();
        selloutReportParametrosDTO.materialLocationFilterDTO = new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO();
        selloutReportParametrosDTO.materialLocationFilterDTO.valuesByLocationCharacteristicId =
                Map.of("REGION", List.of("SOUTH"));

        /*
         * A rota publica precisa ter o mesmo comportamento para filtros de
         * location e material: a feature de caracteristicas e Enterprise e deve
         * falhar antes de validar periodo ou carregar qualquer projection.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> selloutFrontService.getSelloutParaExportacaoAgGrid(selloutReportParametrosDTO));

    }

    @Test
    public void getSelloutParaExportacaoAgGridShouldRejectMissingPayloadBeforeProjection() {

        SelloutFacade selloutFrontService = new SelloutFacade();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> selloutFrontService.getSelloutParaExportacaoAgGrid(null));

        Assertions.assertEquals(
                "Sell-out report parameters are required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getSelloutParaExportacaoAgGridShouldRejectMissingStartDateBeforeProjection() {

        SelloutFacade selloutFrontService = new SelloutFacade();
        SelloutReportParametrosDTO selloutReportParametrosDTO = createSelloutReportParametrosDTO(
                null,
                LocalDate.of(2026, 1, 31));

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> selloutFrontService.getSelloutParaExportacaoAgGrid(selloutReportParametrosDTO));

        Assertions.assertEquals(
                "Sell-out report start date is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void validaFiltroCaracteristicasEnterpriseCommunityShouldRejectMaterialCharacteristicFilter() throws Exception {

        FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filtroMaterialLocationDeCombinacaoCaracteristicasDTO =
                new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO();
        filtroMaterialLocationDeCombinacaoCaracteristicasDTO.valuesByMaterialCharacteristicId =
                Map.of("BRAND", List.of("B1"));

        assertRequiresEnterpriseVersionException(filtroMaterialLocationDeCombinacaoCaracteristicasDTO);

    }

    @Test
    public void validaFiltroCaracteristicasEnterpriseCommunityShouldRejectLocationCharacteristicFilter() throws Exception {

        FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filtroMaterialLocationDeCombinacaoCaracteristicasDTO =
                new FiltroMaterialLocationDeCombinacaoCaracteristicasDTO();
        filtroMaterialLocationDeCombinacaoCaracteristicasDTO.valuesByLocationCharacteristicId =
                Map.of("REGION", List.of("SOUTH"));

        assertRequiresEnterpriseVersionException(filtroMaterialLocationDeCombinacaoCaracteristicasDTO);

    }

    @Test
    public void validaEntidadesSelloutRelatorioCommunityShouldRejectNullItemWithIndex() throws Exception {

        SelloutFacade selloutFrontService = new SelloutFacade();
        Method validationMethod = SelloutFacade.class.getDeclaredMethod(
                "validaEntidadesSelloutRelatorioCommunity",
                java.util.Collection.class);
        validationMethod.setAccessible(true);

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> validationMethod.invoke(
                        selloutFrontService,
                        Collections.singletonList((Sellout) null)));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Sell-out report rows cannot contain null item at index 0",
                invocationTargetException.getCause().getMessage());

    }

    private void assertRequiresEnterpriseVersionException(
            FiltroMaterialLocationDeCombinacaoCaracteristicasDTO filtroMaterialLocationDeCombinacaoCaracteristicasDTO)
            throws Exception {

        SelloutFacade selloutFrontService = new SelloutFacade();
        Method validationMethod = SelloutFacade.class.getDeclaredMethod(
                "validaFiltroCaracteristicasEnterpriseCommunity",
                FiltroMaterialLocationDeCombinacaoCaracteristicasDTO.class);
        validationMethod.setAccessible(true);

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> validationMethod.invoke(
                        selloutFrontService,
                        filtroMaterialLocationDeCombinacaoCaracteristicasDTO));
        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    private SelloutReportParametrosDTO createSelloutReportParametrosDTO(
            LocalDate startDate,
            LocalDate endDate) {

        SelloutReportParametrosDTO selloutReportParametrosDTO = new SelloutReportParametrosDTO();
        selloutReportParametrosDTO.startDate = startDate;
        selloutReportParametrosDTO.endDate = endDate;
        return selloutReportParametrosDTO;

    }

    private SelloutFacade createSelloutFrontService(
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        SelloutFacade selloutFrontService = new SelloutFacade();
        ReflectionTestUtils.setField(
                selloutFrontService,
                "clusterEParametrosProjectionFactory",
                new FakeClusterEParametrosProjectionFactory(clusterEParametrosProjection));
        ReflectionTestUtils.setField(
                selloutFrontService,
                "unidadeMedidaProjectionFactory",
                new FakeUnidadeMedidaProjectionFactory(unidadeMedidaProjection));
        return selloutFrontService;

    }

    private static ClusterEParametrosProjection createClusterEParametrosProjectionComParametrosGlobais() {

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();
        ReflectionTestUtils.setField(
                clusterEParametrosProjection,
                "parametrosGlobais",
                new ParametrosGlobais());
        return clusterEParametrosProjection;

    }

    private static UnidadeMedidaProjection createUnidadeMedidaProjectionComParametrosGlobais() {

        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();
        ReflectionTestUtils.setField(
                unidadeMedidaProjection,
                "parametrosGlobais",
                new ParametrosGlobais());
        return unidadeMedidaProjection;

    }

    private static class FakeClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private FakeClusterEParametrosProjectionFactory(
                ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class FakeUnidadeMedidaProjectionFactory extends UnidadeMedidaProjectionFactory {

        private final UnidadeMedidaProjection unidadeMedidaProjection;

        private FakeUnidadeMedidaProjectionFactory(
                UnidadeMedidaProjection unidadeMedidaProjection) {

            this.unidadeMedidaProjection = unidadeMedidaProjection;

        }

        @Override
        public UnidadeMedidaProjection getUnidadeMedidaProjectionCompletoDeCache() {

            return unidadeMedidaProjection;

        }

    }

}
