package com.opsfactor.community.capability.supplyplanning.supplyplan.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.lowlevelcode.facade.LowLevelCodeFacade;
import com.opsfactor.community.capability.planningbook.facade.dto.CellDetailsDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardSupplyPlanning;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeEdgeDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeNodeDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.mapper.SupplyPlanAutoMapper;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanPeriodDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.SupplyPlanSelectDTO;
import com.opsfactor.community.capability.supplyplanning.supplyplan.facade.dto.VersaoSupplyPlanDTO;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;

/**
 * Valida contratos Community do Supply Planning Book sem subir Spring.
 *
 * <p>A edicao Community permite ajustes no Planning Book de Supply apenas para
 * key figures operacionais do plano heuristico. Payloads manuais ainda podem
 * chegar ao endpoint com uma lista de celulas, por isso a service precisa
 * validar a lista inteira antes de montar projections e antes de salvar.</p>
 */
class SupplyPlanFacadeCommunityContractTest {

    @Test
    void supplyPlanFrontServiceSourceShouldNotKeepLegacyPlanningBookImplementationBlocksCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path supplyPlanFrontServiceSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/supplyplan/facade/SupplyPlanFacade.java");
        String supplyPlanFrontServiceSource = Files.readString(
                supplyPlanFrontServiceSourcePath,
                StandardCharsets.UTF_8);

        /*
         * O Planning Book Community usa `locationId` direto no DTO selecionado
         * e parametros completos vindos do cache. Blocos antigos comentados por
         * description columns ou populacao manual de parametros deixam o fluxo
         * de abertura/detalhe ambiguo e sugerem caminhos que nao fazem parte do
         * recorte Community atual.
         */
        Assertions.assertFalse(
                supplyPlanFrontServiceSource.contains("//        String locationId = selectedPlanningBookCellDTO.locationDescriptionCols.get(\"locationId\");")
                        || supplyPlanFrontServiceSource.contains("populaParametrosProjectionComParametrosDP")
                        || supplyPlanFrontServiceSource.contains("populaParametrosProjectionComParametrosSNP")
                        || supplyPlanFrontServiceSource.contains("//        configuredViewProjectionFactory.setValorFiltradoAConfiguredViewProjection"),
                "SupplyPlanFrontService nao deve manter blocos legados de Planning Book comentados.");

    }

    @Test
    void lowLevelCodeDtosShouldExposeOnlyCommunityGraphFields() {

        /*
         * Low level code e uma visualizacao tecnica Community de malha/BOM,
         * nao Supply Network Flows Enterprise. O grafo deve continuar pequeno:
         * nodes, edges, rotulos e levels, sem custos/frete/solver.
         */
        assertDeclaredFieldNames(
                LowLevelCodeDTO.class,
                List.of(
                        "nodeDTOSet",
                        "edgeDTOSet"));
        assertDeclaredFieldNames(
                LowLevelCodeNodeDTO.class,
                List.of(
                        "tipo",
                        "id",
                        "label",
                        "level"));
        assertDeclaredFieldNames(
                LowLevelCodeEdgeDTO.class,
                List.of(
                        "from",
                        "to",
                        "label"));

    }

    @Test
    void supplyPlanningDtosShouldExposeOnlyCommunityFields() {

        /*
         * Estes DTOs sao a superficie publica de selecao/listagem do Supply
         * Planning Book Community. Campos de otimizador, process chain,
         * Constraint Tracker, custos, P&L, line scheduling ou outputs
         * agregados precisam entrar em DTOs Enterprise, nao aqui.
         */
        assertDeclaredFieldNames(
                SupplyPlanDTO.class,
                List.of(
                        "supplyPlanId",
                        "supplyNetworkVersionId",
                        "executionProfileId",
                        "description",
                        "bucketSize",
                        "timeOfExecution",
                        "beginsOn",
                        "generatedBy",
                        "demandPlanDTO"));
        assertDeclaredFieldNames(
                SupplyPlanPeriodDTO.class,
                List.of(
                        "periodIndex",
                        "label",
                        "bucketSize",
                        "referenceDate",
                        "startDateTime",
                        "endDateTime"));
        assertDeclaredFieldNames(
                SupplyPlanSelectDTO.class,
                List.of(
                        "locationDTOList",
                        "supplyPlanDTOList"));
        /*
         * O identificador do grupo de preset constraints e a unica ponte
         * escalar compartilhada neste DTO: ele preserva a selecao persistida
         * do Supply Plan, sem expor as regras ou colecoes privadas que o
         * Enterprise resolve pelo seu SPI.
         */
        assertDeclaredFieldNames(
                VersaoSupplyPlanDTO.class,
                List.of(
                        "supplyPlanId",
                        "supplyPlanIdForStartingStockProjection",
                        "executionProfileId",
                        "demandPlanId",
                        "supplyNetworkVersionId",
                        "presetConstraintGroupId",
                        "descricaoSupplyPlan",
                        "descricaoDemandPlan",
                        "tamanhoBucket",
                        "horarioGeracao",
                        "periodoReferencia"));

    }

    @Test
    void supplyPlanningBookServiceMethodsShouldNotExposeLegacyJavassistNotFoundException() throws Exception {

        Method getPlanningBookDTOMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getPlanningBookDTO",
                ConfiguredViewSelectionDTO.class,
                String.class);
        Method modificaSupplyPlanMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "modificaSupplyPlan",
                List.class,
                String.class);
        Method getDetalhesSupplyPlanningBookMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getDetalhesSupplyPlanningBook",
                SelectedPlanningBookCellDTO.class,
                String.class);
        Method getDetalhesDemandaIndiretaSupplyPlanningBookMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getDetalhesDemandaIndiretaSupplyPlanningBook",
                SelectedPlanningBookCellDTO.class,
                String.class);
        Method getDetalhesCelulaSupplyPlanningBookMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getDetalhesCelulaSupplyPlanningBook",
                SelectedPlanningBookCellDTO.class,
                String.class);

        /*
         * Location/plano ausentes sao erros funcionais de SupplyPlanException.
         * A excecao checked do Javassist nao faz parte do contrato Community
         * dos endpoints de Planning Book.
         */
        assertNoLegacyJavassistNotFoundException(
                getPlanningBookDTOMethod,
                modificaSupplyPlanMethod,
                getDetalhesSupplyPlanningBookMethod,
                getDetalhesDemandaIndiretaSupplyPlanningBookMethod,
                getDetalhesCelulaSupplyPlanningBookMethod);

    }

    @Test
    void lowLevelCodeFrontServiceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                LowLevelCodeFacade.class,
                "supplyNetworkProjectionFactory",
                "clusterEParametrosProjectionFactory",
                "versaoMalhaRepository",
                "produtoRepository",
                "dfuAutoMapper");

    }

    @Test
    void lowLevelCodeFrontServiceShouldNotExposeLegacyJavassistNotFoundException() throws Exception {

        Method getLowLevelCodeDTOMethod = LowLevelCodeFacade.class.getDeclaredMethod(
                "getLowLevelCodeDTO",
                String.class,
                String.class);

        /*
         * A API de LLC pode falhar por validacao, repository/projection ou erro
         * de calculo, mas nao deve expor a excecao checked do Javassist usada
         * historicamente como marcador generico de "nao encontrado".
         */
        assertNoLegacyJavassistNotFoundException(getLowLevelCodeDTOMethod);

    }

    @Test
    void lowLevelCodeFrontServiceShouldRejectMissingDfuMapParametersBeforeRepositories() {

        LowLevelCodeFacade lowLevelCodeFrontService = new LowLevelCodeFacade();

        IllegalArgumentException missingVersionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodePorDFU(
                        " ",
                        LocalDateTime.of(2026, 1, 1, 0, 0)));
        Assertions.assertEquals(
                "Supply Network Version is null or empty",
                missingVersionException.getMessage());

        IllegalArgumentException missingReferenceDateException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodePorDFU(
                        "MALHA-1",
                        null));
        Assertions.assertEquals(
                "Low Level Code reference date is required",
                missingReferenceDateException.getMessage());

    }

    @Test
    void lowLevelCodeFrontServiceShouldRejectMissingCircularNetworkParametersBeforeRepositories() {

        LowLevelCodeFacade lowLevelCodeFrontService = new LowLevelCodeFacade();

        IllegalArgumentException missingVersionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getDFUMalhaCircularDTOSet(
                        " ",
                        LocalDateTime.of(2026, 1, 1, 0, 0)));
        Assertions.assertEquals(
                "Supply Network Version is null or empty",
                missingVersionException.getMessage());

        IllegalArgumentException missingReferenceDateException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getDFUMalhaCircularDTOSet(
                        "MALHA-1",
                        null));
        Assertions.assertEquals(
                "Low Level Code circular-network reference date is required",
                missingReferenceDateException.getMessage());

    }

    @Test
    void lowLevelCodeFrontServiceShouldRejectMissingMaterialPathParametersBeforeRepositories() {

        LowLevelCodeFacade lowLevelCodeFrontService = new LowLevelCodeFacade();

        IllegalArgumentException missingVersionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodeDTO(
                        "",
                        "MAT-1"));
        Assertions.assertEquals(
                "Supply Network Version is null or empty",
                missingVersionException.getMessage());

        IllegalArgumentException missingMaterialException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> lowLevelCodeFrontService.getLowLevelCodeDTO(
                        "MALHA-1",
                        " "));
        Assertions.assertEquals(
                "Material Id is null or empty",
                missingMaterialException.getMessage());

    }

    @Test
    void lowLevelCodeFrontServiceShouldRejectMaterialWithoutActiveLocation() {

        LowLevelCodeFacade lowLevelCodeFrontService = new LowLevelCodeFacade();
        Produto material = new Produto("MAT-1");

        /*
         * O teste chama a validacao isolada para nao montar malha/projections.
         * Se o calculo de LLC nao encontrou nenhum codigo, o Community nao deve
         * criar grafo parcial nem depender de recurso Enterprise de rede/mapa
         * para explicar o material.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaMaterialAtivoEmAlgumaLocationParaLowLevelCodeCommunity(
                        lowLevelCodeFrontService,
                        material,
                        Set.of()));

        Assertions.assertInstanceOf(
                SupplyPlanException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Selected material MAT-1 is not active in any location",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void serviceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                "planningBookService",
                "configuredViewService",
                "supplyPlanService",
                "supplyPlanningModificacoesService",
                "parametrosGlobaisService",
                "supplyNetworkProjectionFactory",
                "configuredViewProjectionFactory",
                "conversaoUnidadeMedidaProjectionFactory",
                "demandPlanProjectionFactory",
                "supplyPlanProjectionFactory",
                "clusterEParametrosProjectionFactory",
                "splitTemporalProjectionFactory",
                "politicaEstoquesProjectionFactory",
                "keyFigureProjectionFactory",
                "supplyPlanRepository",
                "locationRepository",
                "supplyPlanAutoMapper");

    }

    @Test
    void supplyPlanSelectorRepositoryShouldFetchItsDisplayedRelationshipsInBatch() throws Exception {

        Method method = SupplyPlanRepository.class.getDeclaredMethod("customFindAllForSelector");
        Query query = method.getAnnotation(Query.class);

        Assertions.assertNotNull(
                query,
                "Supply Plan selector deve declarar query explicita com fetch joins.");
        Assertions.assertTrue(
                query.value().contains("LEFT JOIN FETCH sp.demandPlan demandPlan"),
                "Supply Plan selector deve carregar o Demand Plan de cada plano em lote.");
        Assertions.assertTrue(
                query.value().contains("LEFT JOIN FETCH sp.versaoMalha versaoMalha"),
                "Supply Plan selector deve carregar a versao de malha de cada plano em lote.");
        Assertions.assertTrue(
                query.value().contains(
                        "LEFT JOIN FETCH sp.perfilExecucaoSupplyPlan perfilExecucaoSupplyPlan"),
                "Supply Plan selector deve carregar o perfil de execucao de cada plano em lote.");

    }

    @Test
    void getSupplyPlanningSelectDTOShouldRejectBrokenLocationSnapshotBeforeDto() throws Exception {

        SupplyPlanFacade serviceComListaNula = new SupplyPlanFacade();
        setPrivateField(
                serviceComListaNula,
                "locationRepository",
                getLocationRepositoryComCustomFindAll(null));

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Location repository returned null list for Supply Planning selector.",
                listaNulaException.getMessage());

        SupplyPlanFacade serviceComItemNulo = new SupplyPlanFacade();
        setPrivateField(
                serviceComItemNulo,
                "locationRepository",
                getLocationRepositoryComCustomFindAll(Arrays.asList((Location) null)));

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Location repository returned null item at index 0 for Supply Planning selector.",
                itemNuloException.getMessage());

        SupplyPlanFacade serviceComLocationSemId = new SupplyPlanFacade();
        setPrivateField(
                serviceComLocationSemId,
                "locationRepository",
                getLocationRepositoryComCustomFindAll(List.of(new Location())));

        IllegalStateException locationSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComLocationSemId::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Location repository returned item without id at index 0 for Supply Planning selector.",
                locationSemIdException.getMessage());

    }

    @Test
    void getSupplyPlanningSelectDTOShouldRejectBrokenSupplyPlanSnapshotBeforeDto() throws Exception {

        SupplyPlanFacade serviceComListaNula = criaSupplyPlanFrontServiceComLocationsValidas();
        setPrivateField(
                serviceComListaNula,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindAllForSelector(null));

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComListaNula::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Supply Plan repository returned null list for Supply Planning selector.",
                listaNulaException.getMessage());

        SupplyPlanFacade serviceComItemNulo = criaSupplyPlanFrontServiceComLocationsValidas();
        setPrivateField(
                serviceComItemNulo,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindAllForSelector(Arrays.asList((SupplyPlan) null)));

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Supply Plan repository returned null item at index 0 for Supply Planning selector.",
                itemNuloException.getMessage());

        SupplyPlanFacade serviceComPlanoSemId = criaSupplyPlanFrontServiceComLocationsValidas();
        setPrivateField(
                serviceComPlanoSemId,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindAllForSelector(List.of(criaSupplyPlanSeletor(
                        null,
                        10L,
                        "SUPPLY-PROFILE",
                        "MALHA-1"))));

        IllegalStateException planoSemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComPlanoSemId::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Supply Plan repository returned item without id at index 0 for Supply Planning selector.",
                planoSemIdException.getMessage());

        SupplyPlanFacade serviceComDemandPlanAusente = criaSupplyPlanFrontServiceComLocationsValidas();
        setPrivateField(
                serviceComDemandPlanAusente,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindAllForSelector(List.of(criaSupplyPlanSeletor(
                        1L,
                        null,
                        "SUPPLY-PROFILE",
                        "MALHA-1"))));

        IllegalStateException demandPlanAusenteException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDemandPlanAusente::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Supply Plan repository returned item without Demand Plan id at index 0 for Supply Planning selector.",
                demandPlanAusenteException.getMessage());

        SupplyPlanFacade serviceComPerfilAusente = criaSupplyPlanFrontServiceComLocationsValidas();
        setPrivateField(
                serviceComPerfilAusente,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindAllForSelector(List.of(criaSupplyPlanSeletor(
                        1L,
                        10L,
                        null,
                        "MALHA-1"))));

        IllegalStateException perfilAusenteException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComPerfilAusente::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Supply Plan repository returned item without execution profile id at index 0 for Supply Planning selector.",
                perfilAusenteException.getMessage());

        SupplyPlanFacade serviceComMalhaAusente = criaSupplyPlanFrontServiceComLocationsValidas();
        setPrivateField(
                serviceComMalhaAusente,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComCustomFindAllForSelector(List.of(criaSupplyPlanSeletor(
                        1L,
                        10L,
                        "SUPPLY-PROFILE",
                        null))));

        IllegalStateException malhaAusenteException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComMalhaAusente::getSupplyPlanningSelectDTO);

        Assertions.assertEquals(
                "Supply Plan repository returned item without supply network version id at index 0 for Supply Planning selector.",
                malhaAusenteException.getMessage());

    }

    @Test
    void getSupplyPlanDTOListShouldRejectBrokenMapperSnapshotBeforeReturning() throws Exception {

        SupplyPlanFacade serviceComMapperNulo =
                criaSupplyPlanFrontServiceParaListagemDTO(null);

        IllegalStateException nullListException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComMapperNulo::getSupplyPlanDTOList);
        Assertions.assertEquals(
                "Supply Plan mapper returned null DTO list for Supply Planning DTO listing.",
                nullListException.getMessage());

        SupplyPlanFacade serviceComItemNulo =
                criaSupplyPlanFrontServiceParaListagemDTO(Arrays.asList((SupplyPlanDTO) null));

        IllegalStateException nullItemException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComItemNulo::getSupplyPlanDTOList);
        Assertions.assertEquals(
                "Supply Plan mapper returned null DTO at index 0 for Supply Planning DTO listing.",
                nullItemException.getMessage());

        SupplyPlanFacade serviceComDtoSemId =
                criaSupplyPlanFrontServiceParaListagemDTO(List.of(new SupplyPlanDTO()));

        IllegalStateException missingIdException = Assertions.assertThrows(
                IllegalStateException.class,
                serviceComDtoSemId::getSupplyPlanDTOList);
        Assertions.assertEquals(
                "Supply Plan mapper returned DTO without supplyPlanId at index 0 for Supply Planning DTO listing.",
                missingIdException.getMessage());

        SupplyPlanFacade serviceComDtoValido =
                criaSupplyPlanFrontServiceParaListagemDTO(List.of(criaSupplyPlanDTOListagem(1L)));

        Assertions.assertEquals(
                1L,
                serviceComDtoValido.getSupplyPlanDTOList().get(0).supplyPlanId);

    }

    @Test
    void getSupplyPlanPeriodDTOListShouldExposeCalendarFieldsConsumedByNewFront() throws Exception {

        SupplyPlan supplyPlan = criaSupplyPlanSeletor(1L, null, null, null);
        supplyPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        supplyPlan.setDataInicioPlano(LocalDateTime.of(2030, 3, 1, 0, 0));
        supplyPlan.setDataFimPlano(LocalDateTime.of(2030, 4, 15, 23, 59));

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        setPrivateField(
                supplyPlanFrontService,
                "supplyPlanRepository",
                getSupplyPlanRepositoryComFindRetornando(Optional.of(supplyPlan)));
        setPrivateField(
                supplyPlanFrontService,
                "parametrosGlobaisService",
                new TestParametrosGlobaisService(new ParametrosGlobais()));

        /*
         * O seletor do novo front usa referenceDate como value e usa label,
         * bucketSize, startDateTime e endDateTime apenas para montar o texto
         * exibido. A API deve devolver esses campos a partir do calendario do
         * proprio plano, sem projection paralela nem consulta de linhas.
         */
        List<SupplyPlanPeriodDTO> supplyPlanPeriodDTOList =
                supplyPlanFrontService.getSupplyPlanPeriodDTOList(1L);

        Assertions.assertEquals(2, supplyPlanPeriodDTOList.size());
        SupplyPlanPeriodDTO firstPeriodDTO = supplyPlanPeriodDTOList.get(0);
        SupplyPlanPeriodDTO secondPeriodDTO = supplyPlanPeriodDTOList.get(1);
        Assertions.assertEquals(0, firstPeriodDTO.periodIndex);
        Assertions.assertEquals(firstPeriodDTO.periodIndex + 1, secondPeriodDTO.periodIndex);
        Assertions.assertEquals(Constantes.TamanhoBucket.MENSAL, firstPeriodDTO.bucketSize);
        Assertions.assertEquals(LocalDateTime.of(2030, 3, 1, 0, 0), firstPeriodDTO.referenceDate);
        Assertions.assertEquals(firstPeriodDTO.referenceDate, firstPeriodDTO.startDateTime);
        Assertions.assertNotNull(firstPeriodDTO.label);
        Assertions.assertFalse(firstPeriodDTO.label.isBlank());
        Assertions.assertNotNull(firstPeriodDTO.endDateTime);
        Assertions.assertNotNull(secondPeriodDTO.label);
        Assertions.assertEquals(Constantes.TamanhoBucket.MENSAL, secondPeriodDTO.bucketSize);
        Assertions.assertNotNull(secondPeriodDTO.referenceDate);
        Assertions.assertEquals(secondPeriodDTO.referenceDate, secondPeriodDTO.startDateTime);
        Assertions.assertNotNull(secondPeriodDTO.endDateTime);
        Assertions.assertTrue(firstPeriodDTO.endDateTime.isBefore(secondPeriodDTO.startDateTime));

        String serializedPeriod = new ObjectMapper().findAndRegisterModules().writeValueAsString(firstPeriodDTO);
        Assertions.assertTrue(serializedPeriod.contains("\"periodIndex\""));
        Assertions.assertTrue(serializedPeriod.contains("\"label\""));
        Assertions.assertTrue(serializedPeriod.contains("\"bucketSize\""));
        Assertions.assertTrue(serializedPeriod.contains("\"referenceDate\""));
        Assertions.assertTrue(serializedPeriod.contains("\"startDateTime\""));
        Assertions.assertTrue(serializedPeriod.contains("\"endDateTime\""));

    }

    @Test
    void getPlanningBookDTOShouldRejectMissingSelectionBeforeLoadingPlanOrView() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        IllegalArgumentException missingSelectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getPlanningBookDTO(null, "admin"));

        Assertions.assertEquals(
                "Supply Planning Book view selection is required",
                missingSelectionException.getMessage());

        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.viewName = "Supply Planning Book";
        configuredViewSelectionDTO.locationId = "LOC01";

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Supply Planning Book plan id is required",
                illegalArgumentException.getMessage());

        configuredViewSelectionDTO.planId = "1";
        configuredViewSelectionDTO.viewName = null;

        IllegalArgumentException missingViewNameException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Supply Planning Book view name is required",
                missingViewNameException.getMessage());

    }

    @Test
    void getPlanningBookDTOShouldRejectMissingLocationBeforeLoadingPlanOrView() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.planId = "1";
        configuredViewSelectionDTO.viewName = "Supply Planning Book";

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Supply Planning Book location id is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void getPlanningBookDTOShouldRejectInvalidPlanIdBeforeLoadingPlanOrView() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.planId = "abc";
        configuredViewSelectionDTO.viewName = "Supply Planning Book";
        configuredViewSelectionDTO.locationId = "LOC01";

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getPlanningBookDTO(
                        configuredViewSelectionDTO,
                        "admin"));

        Assertions.assertEquals(
                "Supply Planning Book plan id must be numeric: abc",
                illegalArgumentException.getMessage());

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldAcceptHomogeneousCommunityCells() throws Exception {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        SelectedPlanningBookCellDTO primeiraCelula = getSelectedPlanningBookCellDTO(
                KeyFigureStandardEnum.ESTOQUE,
                10.0);
        SelectedPlanningBookCellDTO segundaCelula = getSelectedPlanningBookCellDTO(
                KeyFigureStandardEnum.ESTOQUE,
                10.0);
        segundaCelula.materialDescriptionCols = Map.of("materialId", "MAT02");

        Object keyFigureStandardSupplyPlanning = invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                supplyPlanFrontService,
                List.of(
                        primeiraCelula,
                        segundaCelula));

        Assertions.assertInstanceOf(
                KeyFigureStandardSupplyPlanning.class,
                keyFigureStandardSupplyPlanning);
        Assertions.assertEquals(
                KeyFigureStandardEnum.ESTOQUE,
                ((KeyFigureStandardSupplyPlanning) keyFigureStandardSupplyPlanning).getKeyFigureStandardEnum());
        Assertions.assertEquals(
                "LOC01",
                primeiraCelula.locationDescriptionCols.get("locationId"));
        Assertions.assertEquals(
                "LOC01",
                segundaCelula.locationDescriptionCols.get("locationId"));

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectNullCellBeforeKeyFigureParsing() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        Arrays.asList(
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 10.0),
                                null)));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Supply Planning Book update cannot contain null selected cell at index 1",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectReferencePlanInAnyCell() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        SelectedPlanningBookCellDTO segundaCelula = getSelectedPlanningBookCellDTO(
                KeyFigureStandardEnum.ESTOQUE,
                10.0);
        segundaCelula.referencePlanId = "reference-plan";

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 10.0),
                                segundaCelula)));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectAggregatedCellBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = getSelectedPlanningBookCellDTO(
                KeyFigureStandardEnum.ESTOQUE,
                10.0);
        selectedPlanningBookCellDTO.materialDescriptionCols = null;

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(selectedPlanningBookCellDTO)));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains("Planning Book aggregated adjustments"));

    }

    @Test
    void normalizaLocationDescriptionColsSupplyPlanningBookCommunityShouldCreateMapWhenPayloadOmitsIt() throws Exception {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = getSelectedPlanningBookCellDTO(
                KeyFigureStandardEnum.ESTOQUE,
                10.0);

        selectedPlanningBookCellDTO.locationDescriptionCols = null;

        invokeNormalizaLocationDescriptionColsSupplyPlanningBookCommunity(
                supplyPlanFrontService,
                selectedPlanningBookCellDTO);

        Assertions.assertEquals(
                Map.of("locationId", "LOC01"),
                selectedPlanningBookCellDTO.locationDescriptionCols);

    }

    @Test
    void normalizaLocationDescriptionColsSupplyPlanningBookCommunityShouldPreserveExistingLocationDimensions() throws Exception {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        SelectedPlanningBookCellDTO selectedPlanningBookCellDTO = getSelectedPlanningBookCellDTO(
                KeyFigureStandardEnum.ESTOQUE,
                10.0);

        selectedPlanningBookCellDTO.locationDescriptionCols = new HashMap<>();
        selectedPlanningBookCellDTO.locationDescriptionCols.put("region", "South");

        invokeNormalizaLocationDescriptionColsSupplyPlanningBookCommunity(
                supplyPlanFrontService,
                selectedPlanningBookCellDTO);

        Assertions.assertEquals("South", selectedPlanningBookCellDTO.locationDescriptionCols.get("region"));
        Assertions.assertEquals("LOC01", selectedPlanningBookCellDTO.locationDescriptionCols.get("locationId"));

    }

    @Test
    void validaConfiguredViewProjectionSupplyPlanningBookCommunityShouldRejectEmptyMaterialScope() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        Location location = new Location("LOC-1");
        ConfiguredViewProjection configuredViewProjection = getConfiguredViewProjection(
                Set.of(location),
                Set.of());
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.locationId = location.getId();
        configuredViewSelectionDTO.viewName = "Supply Planning Book";

        /*
         * Uma view sem materiais nao e uma grade valida no Community. O service
         * precisa falhar antes de buscar Supply Plan, KeyFigureProjection ou
         * PlanningBookService, mantendo explicito que o problema esta no escopo
         * material/location configurado para o usuario.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        configuredViewProjection,
                        location,
                        configuredViewSelectionDTO));

        Assertions.assertInstanceOf(
                SupplyPlanException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "No materials conform to the filters. Please review the filters in the Admin -> User Data View menu",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaConfiguredViewProjectionSupplyPlanningBookCommunityShouldRejectBrokenProjectionSnapshot() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        Location location = new Location("LOC-1");
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.locationId = location.getId();
        configuredViewSelectionDTO.viewName = "Supply Planning Book";

        /*
         * A factory de User View deve entregar uma projection completa com DFUs
         * filtrados. Se ela vier nula ou sem FiltroDFUProjection, o Community
         * precisa falhar como snapshot quebrado antes de acessar getters que
         * delegam para a projection interna.
         */
        InvocationTargetException missingProjectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        null,
                        location,
                        configuredViewSelectionDTO));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                missingProjectionException.getCause());
        Assertions.assertEquals(
                "Supply Planning Book opening requires configured view projection",
                missingProjectionException.getCause().getMessage());

        InvocationTargetException missingDfuProjectionException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        new ConfiguredViewProjection(),
                        location,
                        configuredViewSelectionDTO));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                missingDfuProjectionException.getCause());
        Assertions.assertEquals(
                "Supply Planning Book opening requires DFU projection in configured view projection",
                missingDfuProjectionException.getCause().getMessage());

    }

    @Test
    void validaConfiguredViewProjectionSupplyPlanningBookCommunityShouldUseSelectionViewNameWhenProjectionHasNoConfiguredView() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        Location selectedLocation = new Location("LOC-1");
        ConfiguredViewProjection configuredViewProjection = getConfiguredViewProjection(
                Set.of(new Location("OTHER-LOC")),
                Set.of(new Produto("MAT-1")));
        ConfiguredViewSelectionDTO configuredViewSelectionDTO = new ConfiguredViewSelectionDTO();
        configuredViewSelectionDTO.locationId = selectedLocation.getId();
        configuredViewSelectionDTO.viewName = "Supply Planning Book";

        /*
         * Snapshots de teste ou payloads stale podem chegar sem entidade
         * ConfiguredView. A validacao de acesso por location ainda deve devolver
         * mensagem funcional usando o nome solicitado no DTO, sem NPE.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaConfiguredViewProjectionSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        configuredViewProjection,
                        selectedLocation,
                        configuredViewSelectionDTO));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                "Location LOC-1 not accessible for view Supply Planning Book",
                invocationTargetException.getCause().getMessage());

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectEnterpriseKeyFigureInAnyCell() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 10.0),
                                getSelectedPlanningBookCellDTOComKeyFigureId(
                                        KeyFigureStandardEnum.PRODUCAO_FIRME.name(),
                                        10.0))));

        Assertions.assertInstanceOf(
                RequiresEnterpriseVersionException.class,
                invocationTargetException.getCause());

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectReadOnlyCommunityKeyFigure() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        /*
         * Total Demand e visivel no Planning Book, mas calculado. O endpoint de
         * modificacao precisa bloquear o payload antes de buscar plano, view ou
         * projection; caso contrario o backend aceitaria uma edicao que o
         * RuntimeInfo nunca publicou como editavel.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(getSelectedPlanningBookCellDTO(
                                KeyFigureStandardEnum.DEMANDA_TOTAL,
                                10.0))));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains("not editable in Community"));

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectNonWorkingPlanKeyFigure() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        /*
         * Stock e editavel somente no Working Plan. Planos restrito/irrestrito
         * podem ser representados pela projection para leitura tecnica, mas nao
         * fazem parte do contrato de ajuste manual Community.
         */
        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(getSelectedPlanningBookCellDTOComKeyFigureId(
                                new KeyFigureStandardSupplyPlanning(
                                        KeyFigureStandardEnum.ESTOQUE,
                                        Constantes.TipoPlano.PLANO_RESTRITO).getId(),
                                10.0))));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        Assertions.assertTrue(
                invocationTargetException.getCause().getMessage().contains("not editable in Community"));

    }

    @Test
    void getDetalhesSupplyPlanningBookShouldRejectEnterpriseKeyFigureBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanFrontService.getDetalhesSupplyPlanningBook(
                        getSelectedPlanningBookCellDTOComKeyFigureId(
                                KeyFigureStandardEnum.PRODUCAO_FIRME.name(),
                                10.0),
                        "admin"));

        Assertions.assertTrue(
                requiresEnterpriseVersionException.getMessage().contains("Supply Planning production orders key figure"));

    }

    @Test
    void getDetalhesSupplyPlanningBookShouldKeepSalesOrdersEnterpriseOnlyBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanFrontService.getDetalhesSupplyPlanningBook(
                        getSelectedPlanningBookCellDTOComKeyFigureId(
                                KeyFigureStandardEnum.DEMANDA_DIRETA_CARTEIRA_SNP.name(),
                                10.0),
                        "admin"));

        Assertions.assertTrue(
                requiresEnterpriseVersionException.getMessage().contains("Supply Planning customer orders key figure"));

    }

    @Test
    void getDetalhesSupplyPlanningBookShouldRejectMissingSelectionBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        IllegalArgumentException missingSelectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getDetalhesSupplyPlanningBook(null, "admin"));

        Assertions.assertEquals(
                "Supply Planning Book cell details selection is required",
                missingSelectionException.getMessage());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getDetalhesSupplyPlanningBook(
                        SelectedPlanningBookCellDTO.builder()
                                .planId(1L)
                                .viewName("Supply Planning Book")
                                .locationId("LOC01")
                                .period(LocalDate.of(2026, 1, 31))
                                .build(),
                        "admin"));

        Assertions.assertEquals(
                "Supply Planning Book cell details key figure is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void modificaDetalhesSupplyPlanShouldRejectEnterpriseKeyFigureBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        CellDetailsDTO cellDetailsDTO = new CellDetailsDTO();
        cellDetailsDTO.keyFigure = KeyFigureStandardEnum.PRODUCAO_FIRME;

        /*
         * Nenhum collaborator Spring e injetado. A validacao de KF Enterprise
         * precisa acontecer antes de buscar location, view ou supply network.
         */
        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> supplyPlanFrontService.modificaDetalhesSupplyPlan(cellDetailsDTO, "admin"));

    }

    @Test
    void modificaDetalhesSupplyPlanShouldRejectMissingPayloadBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        IllegalArgumentException missingPayloadException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.modificaDetalhesSupplyPlan(null, "admin"));

        Assertions.assertEquals(
                "Supply Planning Book cell detail update payload is required",
                missingPayloadException.getMessage());

        CellDetailsDTO cellDetailsDTO = new CellDetailsDTO();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.modificaDetalhesSupplyPlan(cellDetailsDTO, "admin"));

        Assertions.assertEquals(
                "Supply Planning Book cell detail update key figure is required",
                illegalArgumentException.getMessage());

    }

    @Test
    void getDetalhesDemandaIndiretaShouldRejectNonIndirectDemandKeyFigureBeforeProjection() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        IllegalArgumentException missingSelectionException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getDetalhesDemandaIndiretaSupplyPlanningBook(
                        null,
                        "admin"));

        Assertions.assertEquals(
                "Supply Planning Book cell details selection is required",
                missingSelectionException.getMessage());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanFrontService.getDetalhesDemandaIndiretaSupplyPlanningBook(
                        getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 10.0),
                        "admin"));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(
                        "SupplyPlanFrontService can load indirect demand details only for DEMANDA_INDIRETA_TOTAL"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("received ESTOQUE"));
        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains("standard cell-details flow"));

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectMixedCommunityKeyFigures() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 10.0),
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.INBOUND_PLANEJADO, 10.0))));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());

    }

    @Test
    void validaSelecaoModificacaoSupplyPlanningBookCommunityShouldRejectMixedValues() {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
                        supplyPlanFrontService,
                        List.of(
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 10.0),
                                getSelectedPlanningBookCellDTO(KeyFigureStandardEnum.ESTOQUE, 12.0))));

        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());

    }

    private static void assertDeclaredFieldNames(
            Class<?> dtoClass,
            List<String> expectedFieldNameList) {

        List<String> fieldNameList = Arrays.stream(dtoClass.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(Field::getName)
                .toList();

        Assertions.assertEquals(
                expectedFieldNameList,
                fieldNameList,
                dtoClass.getSimpleName() + " possui campo fora do contrato Community aprovado.");

    }

    private static void assertAutowiredFields(String... fieldNames) throws Exception {

        assertAutowiredFields(
                SupplyPlanFacade.class,
                fieldNames);

    }

    private static void assertAutowiredFields(Class<?> serviceClass, String... fieldNames) throws Exception {

        for (String fieldName : fieldNames) {
            Field field = serviceClass.getDeclaredField(fieldName);
            Autowired autowired = field.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    serviceClass.getSimpleName() + "." + fieldName + " deve usar @Autowired explicito");
            Assertions.assertTrue(
                    autowired.required(),
                    serviceClass.getSimpleName() + "." + fieldName + " deve ser bean obrigatorio");
        }

    }

    private static SelectedPlanningBookCellDTO getSelectedPlanningBookCellDTO(
            KeyFigureStandardEnum keyFigureStandardEnum,
            Double newValue) {

        return getSelectedPlanningBookCellDTOComKeyFigureId(
                new KeyFigureStandardSupplyPlanning(
                        keyFigureStandardEnum,
                        Constantes.TipoPlano.PLANO_TRABALHO).getId(),
                newValue);

    }

    private static SelectedPlanningBookCellDTO getSelectedPlanningBookCellDTOComKeyFigureId(
            String keyFigureId,
            Double newValue) {

        return SelectedPlanningBookCellDTO.builder()
                .planId(1L)
                .viewName("Supply Planning Book")
                .locationId("LOC01")
                .materialDescriptionCols(Map.of("materialId", "MAT01"))
                .keyFigure(keyFigureId)
                .period(LocalDate.of(2026, 1, 31))
                .oldValue(5.0)
                .newValue(newValue)
                .build();

    }

    private static Object invokeValidaSelecaoModificacaoSupplyPlanningBookCommunity(
            SupplyPlanFacade supplyPlanFrontService,
            List<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOs) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "validaSelecaoModificacaoSupplyPlanningBookCommunity",
                List.class);
        validationMethod.setAccessible(true);
        return validationMethod.invoke(
                supplyPlanFrontService,
                selectedPlanningBookCellDTOs);

    }

    private static void invokeNormalizaLocationDescriptionColsSupplyPlanningBookCommunity(
            SupplyPlanFacade supplyPlanFrontService,
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "normalizaLocationDescriptionColsSupplyPlanningBookCommunity",
                SelectedPlanningBookCellDTO.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                supplyPlanFrontService,
                selectedPlanningBookCellDTO);

    }

    private static SupplyPlan invokeGetSupplyPlanObrigatorio(
            SupplyPlanFacade supplyPlanFrontService,
            Long supplyPlanId,
            String contextoOperacional) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getSupplyPlanObrigatorio",
                Long.class,
                String.class);
        validationMethod.setAccessible(true);
        return (SupplyPlan) validationMethod.invoke(
                supplyPlanFrontService,
                supplyPlanId,
                contextoOperacional);

    }

    private static Location invokeGetLocationObrigatoria(
            SupplyPlanFacade supplyPlanFrontService,
            String locationId,
            String contextoOperacional) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getLocationObrigatoria",
                String.class,
                String.class);
        validationMethod.setAccessible(true);
        return (Location) validationMethod.invoke(
                supplyPlanFrontService,
                locationId,
                contextoOperacional);

    }

    private static Roteiro invokeGetRoteiroViavelDetalheProducaoObrigatorio(
            SupplyPlanFacade supplyPlanFrontService,
            SupplyNetworkProjection supplyNetworkProjection,
            Location location,
            Produto material,
            String roteiroId) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getRoteiroViavelDetalheProducaoObrigatorio",
                SupplyNetworkProjection.class,
                Location.class,
                Produto.class,
                String.class);
        validationMethod.setAccessible(true);
        return (Roteiro) validationMethod.invoke(
                supplyPlanFrontService,
                supplyNetworkProjection,
                location,
                material,
                roteiroId);

    }

    private static Location invokeGetLocationOrigemDetalheInboundObrigatoria(
            SupplyPlanFacade supplyPlanFrontService,
            String locationOrigemId) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getLocationOrigemDetalheInboundObrigatoria",
                String.class);
        validationMethod.setAccessible(true);
        return (Location) validationMethod.invoke(
                supplyPlanFrontService,
                locationOrigemId);

    }

    private static Integer invokeGetLeadTimeDiasInboundDetalheObrigatorio(
            SupplyPlanFacade supplyPlanFrontService,
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha,
            LinhaTransporte linhaTransporteInbound,
            Produto material,
            SupplyPlan supplyPlan) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getLeadTimeDiasInboundDetalheObrigatorio",
                SupplyNetworkProjection.class,
                VersaoMalha.class,
                LinhaTransporte.class,
                Produto.class,
                SupplyPlan.class);
        validationMethod.setAccessible(true);
        return (Integer) validationMethod.invoke(
                supplyPlanFrontService,
                supplyNetworkProjection,
                versaoMalha,
                linhaTransporteInbound,
                material,
                supplyPlan);

    }

    private static UnidadeMedidaProjection invokeGetUnidadeMedidaProjectionObrigatoria(
            SupplyPlanFacade supplyPlanFrontService,
            UnidadeMedidaProjection unidadeMedidaProjection,
            String contextoOperacional) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getUnidadeMedidaProjectionObrigatoria",
                UnidadeMedidaProjection.class,
                String.class);
        validationMethod.setAccessible(true);
        return (UnidadeMedidaProjection) validationMethod.invoke(
                supplyPlanFrontService,
                unidadeMedidaProjection,
                contextoOperacional);

    }

    private static SupplyNetworkProjection invokeGetSupplyNetworkProjectionObrigatoria(
            SupplyPlanFacade supplyPlanFrontService,
            SupplyNetworkProjection supplyNetworkProjection,
            String contextoOperacional) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getSupplyNetworkProjectionObrigatoria",
                SupplyNetworkProjection.class,
                String.class);
        validationMethod.setAccessible(true);
        return (SupplyNetworkProjection) validationMethod.invoke(
                supplyPlanFrontService,
                supplyNetworkProjection,
                contextoOperacional);

    }

    private static PoliticaEstoquesProjection invokeGetPoliticaEstoquesProjectionObrigatoria(
            SupplyPlanFacade supplyPlanFrontService,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            String contextoOperacional) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "getPoliticaEstoquesProjectionObrigatoria",
                PoliticaEstoquesProjection.class,
                String.class);
        validationMethod.setAccessible(true);
        return (PoliticaEstoquesProjection) validationMethod.invoke(
                supplyPlanFrontService,
                politicaEstoquesProjection,
                contextoOperacional);

    }

    private static void invokeValidaConfiguredViewProjectionSupplyPlanningBookCommunity(
            SupplyPlanFacade supplyPlanFrontService,
            ConfiguredViewProjection configuredViewProjection,
            Location location,
            ConfiguredViewSelectionDTO configuredViewSelectionDTO) throws Exception {

        Method validationMethod = SupplyPlanFacade.class.getDeclaredMethod(
                "validaConfiguredViewProjectionSupplyPlanningBookCommunity",
                ConfiguredViewProjection.class,
                Location.class,
                ConfiguredViewSelectionDTO.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                supplyPlanFrontService,
                configuredViewProjection,
                location,
                configuredViewSelectionDTO);

    }

    private static void invokeValidaMaterialAtivoEmAlgumaLocationParaLowLevelCodeCommunity(
            LowLevelCodeFacade lowLevelCodeFrontService,
            Produto material,
            Set<Integer> lowLevelCodes) throws Exception {

        Method validationMethod = LowLevelCodeFacade.class.getDeclaredMethod(
                "validaMaterialAtivoEmAlgumaLocationParaLowLevelCodeCommunity",
                Produto.class,
                Set.class);
        validationMethod.setAccessible(true);
        validationMethod.invoke(
                lowLevelCodeFrontService,
                material,
                lowLevelCodes);

    }

    private static Integer invokeGetLeadTimeEmDiasOrigemPrioritariaLowLevelCodeObrigatorio(
            LowLevelCodeFacade lowLevelCodeFrontService,
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha,
            Location locationDestino,
            Produto material,
            LocalDateTime dataHorarioReferenciaStatusMateriais) throws Exception {

        Method validationMethod = LowLevelCodeFacade.class.getDeclaredMethod(
                "getLeadTimeEmDiasOrigemPrioritariaLowLevelCodeObrigatorio",
                SupplyNetworkProjection.class,
                VersaoMalha.class,
                Location.class,
                Produto.class,
                LocalDateTime.class);
        validationMethod.setAccessible(true);
        return (Integer) validationMethod.invoke(
                lowLevelCodeFrontService,
                supplyNetworkProjection,
                versaoMalha,
                locationDestino,
                material,
                dataHorarioReferenciaStatusMateriais);

    }

    private static ConfiguredViewProjection getConfiguredViewProjection(
            Set<Location> locations,
            Set<Produto> materiais) {

        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setDfuProjectionFiltrado(
                new FiltroDFUProjection(
                        locations,
                        materiais,
                        null));

        return configuredViewProjection;

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryVazio() {

        return getSupplyPlanRepositoryComFindRetornando(Optional.empty());

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryComFindRetornando(
            Optional<SupplyPlan> optionalSupplyPlan) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalSupplyPlan;
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository find controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (SupplyPlanRepository) Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler);

    }

    private static SupplyPlanRepository getSupplyPlanRepositoryComCustomFindAllForSelector(
            List<SupplyPlan> supplyPlanList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAllForSelector".equals(method.getName())) {
                return supplyPlanList;
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanRepository customFindAllForSelector controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (SupplyPlanRepository) Proxy.newProxyInstance(
                SupplyPlanRepository.class.getClassLoader(),
                new Class<?>[]{SupplyPlanRepository.class},
                invocationHandler);

    }

    private static LocationRepository getLocationRepositoryVazio() {

        return getLocationRepositoryComFindRetornando(Optional.empty());

    }

    private static LocationRepository getLocationRepositoryComFindRetornando(
            Optional<Location> optionalLocation) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return optionalLocation;
            }
            if ("toString".equals(method.getName())) {
                return "LocationRepository find controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (LocationRepository) Proxy.newProxyInstance(
                LocationRepository.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                invocationHandler);

    }

    private static LocationRepository getLocationRepositoryComCustomFindAll(
            List<Location> locationList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("customFindAllWithParametros".equals(method.getName())) {
                return locationList;
            }
            if ("toString".equals(method.getName())) {
                return "LocationRepository customFindAll controlado para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (LocationRepository) Proxy.newProxyInstance(
                LocationRepository.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                invocationHandler);

    }

    private static SupplyPlanFacade criaSupplyPlanFrontServiceComLocationsValidas() throws Exception {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        setPrivateField(
                supplyPlanFrontService,
                "locationRepository",
                getLocationRepositoryComCustomFindAll(List.of()));

        return supplyPlanFrontService;

    }

    private static SupplyPlanFacade criaSupplyPlanFrontServiceParaListagemDTO(
            List<SupplyPlanDTO> supplyPlanDTOList) throws Exception {

        SupplyPlanFacade supplyPlanFrontService = new SupplyPlanFacade();
        setPrivateField(
                supplyPlanFrontService,
                "supplyPlanService",
                new TestSupplyPlanService(List.of(new SupplyPlan())));
        setPrivateField(
                supplyPlanFrontService,
                "supplyPlanAutoMapper",
                getSupplyPlanAutoMapperComLista(supplyPlanDTOList));

        return supplyPlanFrontService;

    }

    private static SupplyPlanAutoMapper getSupplyPlanAutoMapperComLista(
            List<SupplyPlanDTO> supplyPlanDTOList) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("converteLista".equals(method.getName())) {
                return supplyPlanDTOList;
            }
            if ("toString".equals(method.getName())) {
                return "SupplyPlanAutoMapper com lista para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (SupplyPlanAutoMapper) Proxy.newProxyInstance(
                SupplyPlanAutoMapper.class.getClassLoader(),
                new Class<?>[]{SupplyPlanAutoMapper.class},
                invocationHandler);

    }

    private static SupplyPlanDTO criaSupplyPlanDTOListagem(Long supplyPlanId) {

        SupplyPlanDTO supplyPlanDTO = new SupplyPlanDTO();
        supplyPlanDTO.supplyPlanId = supplyPlanId;

        return supplyPlanDTO;

    }

    private static SupplyPlan criaSupplyPlanSeletor(
            Long supplyPlanId,
            Long demandPlanId,
            String perfilExecucaoSupplyPlanId,
            String versaoMalhaId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(supplyPlanId);
        if (demandPlanId != null) {
            DemandPlan demandPlan = new DemandPlan();
            demandPlan.setId(demandPlanId);
            supplyPlan.setDemandPlan(demandPlan);
        }
        if (perfilExecucaoSupplyPlanId != null) {
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
            perfilExecucaoSupplyPlan.setId(perfilExecucaoSupplyPlanId);
            supplyPlan.setPerfilExecucaoSupplyPlan(perfilExecucaoSupplyPlan);
        }
        if (versaoMalhaId != null) {
            supplyPlan.setVersaoMalha(new VersaoMalha(versaoMalhaId));
        }

        return supplyPlan;

    }

    private static class TestSupplyPlanService extends SupplyPlanService {

        private final List<SupplyPlan> supplyPlanList;

        private TestSupplyPlanService(List<SupplyPlan> supplyPlanList) {

            this.supplyPlanList = supplyPlanList;

        }

        @Override
        public List<SupplyPlan> getSupplyPlanList() {

            return supplyPlanList;

        }

    }

    private static class TestParametrosGlobaisService extends ParametrosGlobaisService {

        private final ParametrosGlobais parametrosGlobais;

        private TestParametrosGlobaisService(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

    private static VersaoMalhaRepository getVersaoMalhaRepositoryComVersao(VersaoMalha versaoMalha) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(versaoMalha);
            }
            if ("toString".equals(method.getName())) {
                return "VersaoMalhaRepository com versao para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (VersaoMalhaRepository) Proxy.newProxyInstance(
                VersaoMalhaRepository.class.getClassLoader(),
                new Class<?>[]{VersaoMalhaRepository.class},
                invocationHandler);

    }

    private static ProdutoRepository getProdutoRepositoryComMaterial(Produto material) {

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(material);
            }
            if ("toString".equals(method.getName())) {
                return "ProdutoRepository com material para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());
        };

        return (ProdutoRepository) Proxy.newProxyInstance(
                ProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ProdutoRepository.class},
                invocationHandler);

    }

    private static ClusterEParametrosProjection getClusterEParametrosProjectionComParametrosGlobais() {

        return new TestClusterEParametrosProjection(new ParametrosGlobais());

    }

    private static UnidadeMedidaProjection getUnidadeMedidaProjectionComParametrosGlobais() {

        return new TestUnidadeMedidaProjection(new ParametrosGlobais());

    }

    private static SupplyNetworkProjection getSupplyNetworkProjectionComClusterEParametrosProjection(
            ClusterEParametrosProjection clusterEParametrosProjection) {

        return new TestSupplyNetworkProjection(
                clusterEParametrosProjection,
                null);

    }

    private static SupplyNetworkProjection getSupplyNetworkProjectionComClusterEParametrosProjection(
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        return new TestSupplyNetworkProjection(
                clusterEParametrosProjection,
                unidadeMedidaProjection);

    }

    private static SupplyNetworkProjection getSupplyNetworkProjectionSemDadosProdutivos() {

        return new SupplyNetworkProjection() {

            @Override
            public Set<Roteiro> getRoteirosViaveis(
                    Location location,
                    Produto material) {

                return Set.of();

            }

            @Override
            public Set<ListaTecnica> getListasTecnicasViaveis(
                    Location location,
                    Produto material,
                    Collection<Produto> possiveisMateriaisInput) {

                return Set.of();

            }

            @Override
            public List<VersaoProducao> getVersoesProducaoViaveisOrdenadasPorPrioridade(
                    Location location,
                    Produto material,
                    boolean consideraVersoesProducaoParalelas,
                    Collection<Produto> possiveisMateriaisInput) {

                return List.of();

            }

            @Override
            public Optional<Integer> getLeadTimeDiasEntreOrigemDestinoParaMaterial(
                    VersaoMalha versaoMalha,
                    Location locationOrigem,
                    Location locationDestino,
                    Produto material,
                    LocalDateTime dataReferenciaParaStatusProduto) {

                return Optional.empty();

            }

            @Override
            public Optional<Integer> getLeadTimeEmDiasDeOrigemPrioritaria(
                    VersaoMalha versaoMalha,
                    Location locationDestino,
                    Produto material,
                    LocalDateTime dataReferenciaParaStatusProduto,
                    Collection<Location> possiveisLocationsOrigem) {

                return Optional.empty();

            }

        };

    }

    private static class TestClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        private final ClusterEParametrosProjection clusterEParametrosProjection;

        private TestClusterEParametrosProjectionFactory(ClusterEParametrosProjection clusterEParametrosProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;

        }

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return clusterEParametrosProjection;

        }

    }

    private static class TestClusterEParametrosProjection extends ClusterEParametrosProjection {

        private final ParametrosGlobais parametrosGlobais;

        private TestClusterEParametrosProjection(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

    }

    private static class TestSupplyNetworkProjectionFactory extends SupplyNetworkProjectionFactory {

        private final SupplyNetworkProjection supplyNetworkProjection;

        private TestSupplyNetworkProjectionFactory(SupplyNetworkProjection supplyNetworkProjection) {

            this.supplyNetworkProjection = supplyNetworkProjection;

        }

        @Override
        public SupplyNetworkProjection getSupplyNetworkProjectionCompletoDeCache() {

            return supplyNetworkProjection;

        }

    }

    private static class TestUnidadeMedidaProjection extends UnidadeMedidaProjection {

        private TestUnidadeMedidaProjection(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

    }

    private static class TestSupplyNetworkProjection extends SupplyNetworkProjection {

        private TestSupplyNetworkProjection(
                ClusterEParametrosProjection clusterEParametrosProjection,
                UnidadeMedidaProjection unidadeMedidaProjection) {

            this.clusterEParametrosProjection = clusterEParametrosProjection;
            this.conversaoUnidadeMedidaProjection = unidadeMedidaProjection;

        }

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static void assertNoLegacyJavassistNotFoundException(Method... methods) {

        for (Method method : methods) {
            Assertions.assertTrue(
                    Arrays.stream(method.getExceptionTypes())
                            .noneMatch(exceptionType -> "javassist.NotFoundException".equals(exceptionType.getName())),
                    method.getName() + " nao deve expor javassist.NotFoundException");
        }

    }

    private static Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

}
