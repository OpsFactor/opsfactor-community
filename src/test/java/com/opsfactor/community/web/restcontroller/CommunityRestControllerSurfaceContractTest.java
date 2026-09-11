package com.opsfactor.community.web.restcontroller;

import com.opsfactor.community.web.restcontroller.admin.AdminRestController;
import com.opsfactor.community.web.restcontroller.configuration.ParametroMaterialLocationController;
import com.opsfactor.community.web.restcontroller.configuration.ParametrosGlobaisController;
import com.opsfactor.community.web.restcontroller.configuration.PerfilExecucaoDemandPlanController;
import com.opsfactor.community.web.restcontroller.configuration.PerfilExecucaoSupplyPlanController;
import com.opsfactor.community.web.restcontroller.configuration.PoliticaEstoquesController;
import com.opsfactor.community.web.restcontroller.configuration.UserConfigurationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.ConversaoUnidadeIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.ConversaoUnidadeProdutoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.LocationIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.MaterialIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.ParametrosMaterialLocationIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.UnidadeMedidaIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.DemandPlanDetailedExportController;
import com.opsfactor.community.web.restcontroller.dataupload.DistributionPlanIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.LocationCharacteristicIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.LocationCharacteristicValueIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.MaterialCharacteristicIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.MaterialCharacteristicValueIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.ProductionPlanOccupationIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.ProductionPlanVolumeIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.inventorypolicy.InventoryPolicyDetailIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.inventorypolicy.InventoryPolicyIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha.LinhaTransporteIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha.LinhaTransporteMaterialIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.malha.VersaoMalhaIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.DisponibilidadeRecursoProdutivoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.ListaTecnicaComponenteIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.ListaTecnicaIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.OperacaoRoteiroIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.RecursoProdutivoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.RoteiroIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.masterdata.production.VersaoProducaoIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.planning.supply.FulfilledDemandIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.planning.supply.InventoryPlanIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.EstoqueIntegrationController;
import com.opsfactor.community.web.restcontroller.dataupload.transactionaldata.SelloutIntegrationController;
import com.opsfactor.community.web.restcontroller.masterdata.ClusteringRestController;
import com.opsfactor.community.web.restcontroller.masterdata.CharacteristicCatalogRestController;
import com.opsfactor.community.web.restcontroller.masterdata.LinhaTransporteController;
import com.opsfactor.community.web.restcontroller.masterdata.LocationRestController;
import com.opsfactor.community.web.restcontroller.masterdata.MaterialRestController;
import com.opsfactor.community.web.restcontroller.masterdata.ProductionRestController;
import com.opsfactor.community.web.restcontroller.masterdata.UnidadeMedidaRestController;
import com.opsfactor.community.web.restcontroller.bi.CommunityDemandSalesOverviewController;
import com.opsfactor.community.web.restcontroller.bi.CommunityInventoryOverviewController;
import com.opsfactor.community.web.restcontroller.bi.CommunityMaterialFlowsController;
import com.opsfactor.community.web.restcontroller.bi.CommunityProductionOverviewController;
import com.opsfactor.community.web.restcontroller.bi.CommunityProductionOverviewResourceDetailController;
import com.opsfactor.community.web.restcontroller.bi.CommunitySupplyNetworkDependenciesController;
import com.opsfactor.community.web.restcontroller.planning.ConstrainedPlanController;
import com.opsfactor.community.web.restcontroller.planning.DemandAnalysisRestController;
import com.opsfactor.community.web.restcontroller.planning.DemandPlanningRestController;
import com.opsfactor.community.web.restcontroller.planning.DeploymentOperationalController;
import com.opsfactor.community.web.restcontroller.planning.ProductionPlanningBookController;
import com.opsfactor.community.web.restcontroller.planning.SupplyPlanningController;
import com.opsfactor.community.web.restcontroller.runtime.RuntimeInfoController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

/**
 * Contrato global da superficie de controllers REST publicada pelo Community.
 *
 * <p>Os testes especificos de admin, configuration, data upload, master data,
 * planning e runtime congelam os endpoints por controller. Este teste cobre a
 * fresta complementar: uma nova classe anotada com {@link RestController} nao
 * pode entrar no scan Community sem ser classificada explicitamente como parte
 * do recorte aberto.</p>
 */
public class CommunityRestControllerSurfaceContractTest {

    @Test
    public void communityRestControllerSurfaceShouldStayExplicitlyApproved() {

        Assertions.assertEquals(
                getApprovedRestControllerClassNames(),
                getScannedRestControllerClassNames(),
                "Todo @RestController Community precisa estar na allowlist explicita do recorte publicado.");

    }

    private static List<String> getApprovedRestControllerClassNames() {

        return Stream.of(
                        AdminRestController.class,
                        ParametroMaterialLocationController.class,
                        ParametrosGlobaisController.class,
                        PerfilExecucaoDemandPlanController.class,
                        PerfilExecucaoSupplyPlanController.class,
                        PoliticaEstoquesController.class,
                        ProcessStatusController.class,
                        RuntimeInfoController.class,
                        UserConfigurationController.class,
                        DemandPlanDetailedExportController.class,
                        DistributionPlanIntegrationController.class,
                        LocationCharacteristicIntegrationController.class,
                        LocationCharacteristicValueIntegrationController.class,
                        MaterialCharacteristicIntegrationController.class,
                        MaterialCharacteristicValueIntegrationController.class,
                        ProductionPlanOccupationIntegrationController.class,
                        ProductionPlanVolumeIntegrationController.class,
                        ConversaoUnidadeIntegrationController.class,
                        ConversaoUnidadeProdutoIntegrationController.class,
                        LocationIntegrationController.class,
                        MaterialIntegrationController.class,
                        ParametrosMaterialLocationIntegrationController.class,
                        UnidadeMedidaIntegrationController.class,
                        InventoryPolicyDetailIntegrationController.class,
                        InventoryPolicyIntegrationController.class,
                        VersaoMalhaIntegrationController.class,
                        LinhaTransporteIntegrationController.class,
                        LinhaTransporteMaterialIntegrationController.class,
                        DisponibilidadeRecursoProdutivoIntegrationController.class,
                        ListaTecnicaComponenteIntegrationController.class,
                        ListaTecnicaIntegrationController.class,
                        OperacaoRoteiroIntegrationController.class,
                        RecursoProdutivoIntegrationController.class,
                        RoteiroIntegrationController.class,
                        VersaoProducaoIntegrationController.class,
                        FulfilledDemandIntegrationController.class,
                        InventoryPlanIntegrationController.class,
                        EstoqueIntegrationController.class,
                        SelloutIntegrationController.class,
                        CharacteristicCatalogRestController.class,
                        ClusteringRestController.class,
                        LinhaTransporteController.class,
                        LocationRestController.class,
                        MaterialRestController.class,
                        ProductionRestController.class,
                        UnidadeMedidaRestController.class,
                        ConstrainedPlanController.class,
                        CommunityDemandSalesOverviewController.class,
                        CommunityInventoryOverviewController.class,
                        CommunityMaterialFlowsController.class,
                        CommunityProductionOverviewController.class,
                        CommunityProductionOverviewResourceDetailController.class,
                        CommunitySupplyNetworkDependenciesController.class,
                        DemandAnalysisRestController.class,
                        DemandPlanningRestController.class,
                        DeploymentOperationalController.class,
                        ProductionPlanningBookController.class,
                        SupplyPlanningController.class)
                .map(Class::getName)
                .sorted()
                .toList();

    }

    private static List<String> getScannedRestControllerClassNames() {

        ClassPathScanningCandidateComponentProvider classPathScanningCandidateComponentProvider =
                new ClassPathScanningCandidateComponentProvider(false);
        classPathScanningCandidateComponentProvider.addIncludeFilter(
                new AnnotationTypeFilter(RestController.class));

        /*
         * A varredura usa o mesmo pacote raiz escaneado pelo WebApplication.
         * Qualquer controller Enterprise copiado por engano para Community
         * aparece aqui antes de ser publicado em runtime.
         */
        return classPathScanningCandidateComponentProvider
                .findCandidateComponents("com.opsfactor.community.web.restcontroller")
                .stream()
                .map(beanDefinition -> loadClassName(beanDefinition.getBeanClassName()))
                .sorted()
                .toList();

    }

    private static String loadClassName(String className) {

        try {
            return Class.forName(className).getName();
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalStateException(
                    "Nao foi possivel carregar @RestController Community " + className,
                    classNotFoundException);
        }

    }

}
