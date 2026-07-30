package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesMaterialLocationRepository;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade.dto.PoliticaEstoquesDTO;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Valida o contrato Community da API de politica de estoques.
 *
 * <p>Safety stock operacional continua disponivel no Community, mas parametros
 * ligados a otimizacao de politica de estoques devem falhar antes de qualquer
 * acesso a repository.</p>
 */
public class PoliticaEstoquesFacadeCommunityTest {

    @Test
    public void serviceShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredRequired("politicaEstoquesRepository");
        assertAutowiredRequired("politicaEstoquesMaterialLocationRepository");
        assertAutowiredRequired("produtoRepository");
        assertAutowiredRequired("locationRepository");

    }

    @Test
    public void inventoryPolicyLookupRepositoryShouldUseDistinctFetchJoin() throws Exception {

        Method method = PoliticaEstoquesRepository.class.getDeclaredMethod(
                "customFindById",
                String.class);
        Query query = method.getAnnotation(Query.class);

        Assertions.assertNotNull(
                query,
                "Inventory policy lookup deve declarar query explicita com fetch join.");
        Assertions.assertTrue(
                query.value().contains("SELECT DISTINCT pe FROM PoliticaEstoques pe"),
                "Lookup de Inventory Policy deve usar DISTINCT para nao duplicar a raiz no fetch join.");
        Assertions.assertTrue(
                query.value().contains("LEFT JOIN FETCH pe.politicaEstoquesMaterialLocationList"),
                "Lookup de Inventory Policy deve carregar linhas material/location em lote.");
        Assertions.assertTrue(
                query.value().contains(
                        "LEFT JOIN FETCH pemll.politicaEstoquesMaterialLocationCompositeKey.material"),
                "Lookup de Inventory Policy deve carregar materiais das linhas em lote.");
        Assertions.assertTrue(
                query.value().contains(
                        "LEFT JOIN FETCH pemll.politicaEstoquesMaterialLocationCompositeKey.location"),
                "Lookup de Inventory Policy deve carregar locations das linhas em lote.");

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectNullMaterialLocationListBeforeRepository() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");
        politicaEstoquesDTO.setMaterialLocationList(null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

        Assertions.assertEquals(
                "Inventory policy material/location list must be provided",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectNullPayloadBeforeRepository() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(null));

        Assertions.assertEquals(
                "Inventory policy payload must be provided",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectNullMaterialLocationItemBeforeRepository() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");
        politicaEstoquesDTO.getMaterialLocationList().add(null);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

        Assertions.assertEquals(
                "Inventory policy material/location list cannot contain null values",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectReplenishmentFrequencyCommunity() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");

        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        politicaEstoquesMaterialLocationDTO.setMaterialId("MAT_01");
        politicaEstoquesMaterialLocationDTO.setLocationId("LOC_01");
        politicaEstoquesMaterialLocationDTO.setFrequenciaReabastecimentoDias(7.0d);
        politicaEstoquesDTO.getMaterialLocationList().add(politicaEstoquesMaterialLocationDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectMissingMaterialLocationKeysBeforeRepository() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");

        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        politicaEstoquesMaterialLocationDTO.setLocationId("LOC_01");
        politicaEstoquesDTO.getMaterialLocationList().add(politicaEstoquesMaterialLocationDTO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

        Assertions.assertEquals(
                "Inventory policy material id must be provided",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectEnterpriseFrequencyBeforeMissingKeys() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");

        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        politicaEstoquesMaterialLocationDTO.setFrequenciaReabastecimentoDias(7.0d);
        politicaEstoquesDTO.getMaterialLocationList().add(politicaEstoquesMaterialLocationDTO);

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

    }

    @Test
    public void getAndDeletePoliticaEstoquesShouldRejectMissingIdBeforeRepository() {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();

        IllegalArgumentException getException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.getPoliticaEstoquesDTO(" "));
        Assertions.assertEquals(
                "Inventory policy id must be provided",
                getException.getMessage());

        IllegalArgumentException deleteException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.deletePoliticaEstoques(null));
        Assertions.assertEquals(
                "Inventory policy id must be provided",
                deleteException.getMessage());

    }

    @Test
    public void getPoliticaEstoquesDTOShouldRejectNullRepositoryOptionalBeforeDtoConversion()
            throws Exception {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        setField(
                politicaEstoquesFrontService,
                "politicaEstoquesRepository",
                getPoliticaEstoquesRepositoryParaFindByIdRetornando(null));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> politicaEstoquesFrontService.getPoliticaEstoquesDTO("INV_POLICY_01"));

        Assertions.assertEquals(
                "Inventory policy repository returned null Optional for lookup id INV_POLICY_01.",
                illegalStateException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectNullRepositoryOptionalsBeforeMaterialLocationSave()
            throws Exception {

        PoliticaEstoquesDTO politicaEstoquesDTO = criaPoliticaEstoquesDTOParaSaveMaterialLocation();

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComPolicyQuebrada =
                new PoliticaEstoquesFacade();
        setField(
                politicaEstoquesFrontServiceComPolicyQuebrada,
                "politicaEstoquesRepository",
                getPoliticaEstoquesRepositoryParaSaveComFindByIdRetornando(null));

        /*
         * Optional.empty() cria nova politica. Optional nulo deve falhar antes
         * de montar header, salvar ou substituir linhas material/location.
         */
        IllegalStateException policyException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> politicaEstoquesFrontServiceComPolicyQuebrada.savePoliticaEstoquesDTO(politicaEstoquesDTO));
        Assertions.assertEquals(
                "Inventory policy repository returned null Optional for save id INV_POLICY_01.",
                policyException.getMessage());

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComMaterialQuebrado =
                new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllRetornandoComMaterialELocation(
                politicaEstoquesFrontServiceComMaterialQuebrado,
                List.of(),
                null,
                Optional.of(new Location("LOC_01")));

        IllegalStateException materialException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> politicaEstoquesFrontServiceComMaterialQuebrado.savePoliticaEstoquesDTO(politicaEstoquesDTO));
        Assertions.assertEquals(
                "Material repository returned null Optional for inventory policy material id MAT_01.",
                materialException.getMessage());

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComLocationQuebrada =
                new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllRetornandoComMaterialELocation(
                politicaEstoquesFrontServiceComLocationQuebrada,
                List.of(),
                Optional.of(new Produto("MAT_01")),
                null);

        IllegalStateException locationException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> politicaEstoquesFrontServiceComLocationQuebrada.savePoliticaEstoquesDTO(politicaEstoquesDTO));
        Assertions.assertEquals(
                "Location repository returned null Optional for inventory policy location id LOC_01.",
                locationException.getMessage());

    }

    @Test
    public void getPoliticaEstoquesDTOListShouldHideReplenishmentFrequencyAndKeepSafetyStockFields()
            throws Exception {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        PoliticaEstoques politicaEstoques = criaPoliticaEstoquesComFrequenciaLegada();
        injetaPoliticaEstoquesRepositorySomenteLeitura(politicaEstoquesFrontService, politicaEstoques);

        List<PoliticaEstoquesDTO> politicaEstoquesDTOList =
                politicaEstoquesFrontService.getPoliticaEstoquesDTOList();

        Assertions.assertEquals(1, politicaEstoquesDTOList.size());
        PoliticaEstoquesDTO politicaEstoquesDTO = politicaEstoquesDTOList.getFirst();
        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                politicaEstoquesDTO.getMaterialLocationList().getFirst();

        /*
         * A API Community precisa continuar publicando o cadastro operacional
         * de safety stock, mas nao pode vazar frequencia de reabastecimento de
         * bases legadas porque esse campo agora pertence ao IPO Enterprise.
         */
        Assertions.assertEquals("INV_POLICY_01", politicaEstoquesDTO.getId());
        Assertions.assertEquals(10, politicaEstoquesDTO.getPrioridade());
        Assertions.assertEquals("MAT_01", politicaEstoquesMaterialLocationDTO.getMaterialId());
        Assertions.assertEquals("LOC_01", politicaEstoquesMaterialLocationDTO.getLocationId());
        Assertions.assertEquals(
                Constantes.SNPModeloReabastecimento.DRP,
                politicaEstoquesMaterialLocationDTO.getModeloReabastecimento());
        Assertions.assertEquals(
                Constantes.SNPModeloOperacional.MTS,
                politicaEstoquesMaterialLocationDTO.getModeloOperacional());
        Assertions.assertEquals(
                Constantes.SNPCalculoSafetyStock.QUANTITY,
                politicaEstoquesMaterialLocationDTO.getCalculoSafetyStock());
        Assertions.assertEquals(
                25.0d,
                politicaEstoquesMaterialLocationDTO.getEstoqueSegurancaDrpOuTargetKanban());
        Assertions.assertEquals(80.0d, politicaEstoquesMaterialLocationDTO.getEstoqueMaximoDrp());
        Assertions.assertNull(politicaEstoquesMaterialLocationDTO.getFrequenciaReabastecimentoDias());

    }

    @Test
    public void getPoliticaEstoquesDTOListShouldRejectBrokenRepositorySnapshotBeforeSorting()
            throws Exception {

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComListaNula =
                new PoliticaEstoquesFacade();
        injetaPoliticaEstoquesRepositorySomenteLeituraLista(
                politicaEstoquesFrontServiceComListaNula,
                null);

        IllegalStateException listaNulaException = Assertions.assertThrows(
                IllegalStateException.class,
                politicaEstoquesFrontServiceComListaNula::getPoliticaEstoquesDTOList);

        Assertions.assertEquals(
                "Inventory policy repository returned null list for Community listing.",
                listaNulaException.getMessage());

        List<PoliticaEstoques> politicaEstoquesListComItemNulo = new ArrayList<>();
        politicaEstoquesListComItemNulo.add(null);
        PoliticaEstoquesFacade politicaEstoquesFrontServiceComItemNulo =
                new PoliticaEstoquesFacade();
        injetaPoliticaEstoquesRepositorySomenteLeituraLista(
                politicaEstoquesFrontServiceComItemNulo,
                politicaEstoquesListComItemNulo);

        IllegalStateException itemNuloException = Assertions.assertThrows(
                IllegalStateException.class,
                politicaEstoquesFrontServiceComItemNulo::getPoliticaEstoquesDTOList);

        Assertions.assertEquals(
                "Inventory policy repository returned null item at index 0 for Community listing.",
                itemNuloException.getMessage());

        PoliticaEstoques politicaEstoquesSemId = criaPoliticaEstoquesComFrequenciaLegada();
        politicaEstoquesSemId.setId(null);
        PoliticaEstoquesFacade politicaEstoquesFrontServiceComPolicySemId =
                new PoliticaEstoquesFacade();
        injetaPoliticaEstoquesRepositorySomenteLeituraLista(
                politicaEstoquesFrontServiceComPolicySemId,
                List.of(politicaEstoquesSemId));

        IllegalStateException policySemIdException = Assertions.assertThrows(
                IllegalStateException.class,
                politicaEstoquesFrontServiceComPolicySemId::getPoliticaEstoquesDTOList);

        Assertions.assertEquals(
                "Inventory policy repository returned item without id at index 0 for Community listing.",
                policySemIdException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectNullSavedMaterialLocationSnapshot() throws Exception {

        PoliticaEstoquesFacade politicaEstoquesFrontService = new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllNulo(politicaEstoquesFrontService);

        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");
        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        politicaEstoquesMaterialLocationDTO.setMaterialId("MAT_01");
        politicaEstoquesMaterialLocationDTO.setLocationId("LOC_01");
        politicaEstoquesDTO.getMaterialLocationList().add(politicaEstoquesMaterialLocationDTO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

        Assertions.assertEquals(
                "Saved inventory policy material/location collection is required",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectPartialSavedMaterialLocationSnapshot()
            throws Exception {

        PoliticaEstoquesFacade politicaEstoquesFrontService =
                new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllRetornando(
                politicaEstoquesFrontService,
                List.of(criaPoliticaEstoquesMaterialLocationSalva(
                        "INV_POLICY_01",
                        "MAT_01",
                        "LOC_01")));

        PoliticaEstoquesDTO politicaEstoquesDTO =
                criaPoliticaEstoquesDTOParaSaveMaterialLocation();
        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO outraPoliticaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        outraPoliticaEstoquesMaterialLocationDTO.setMaterialId("MAT_02");
        outraPoliticaEstoquesMaterialLocationDTO.setLocationId("LOC_02");
        politicaEstoquesDTO.getMaterialLocationList().add(
                outraPoliticaEstoquesMaterialLocationDTO);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontService.savePoliticaEstoquesDTO(politicaEstoquesDTO));

        Assertions.assertEquals(
                "Saved inventory policy material/location collection size 1 differs from expected size 2",
                illegalArgumentException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectBrokenSavedMaterialLocationKeys()
            throws Exception {

        PoliticaEstoquesDTO politicaEstoquesDTO = criaPoliticaEstoquesDTOParaSaveMaterialLocation();

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComPolicySemId =
                new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllRetornando(
                politicaEstoquesFrontServiceComPolicySemId,
                List.of(criaPoliticaEstoquesMaterialLocationSalva(" ", "MAT_01", "LOC_01")));

        IllegalArgumentException policySemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontServiceComPolicySemId.savePoliticaEstoquesDTO(
                        politicaEstoquesDTO));
        Assertions.assertEquals(
                "Saved inventory policy material/location item at index 0 must have an inventory policy id",
                policySemIdException.getMessage());

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComMaterialSemId =
                new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllRetornando(
                politicaEstoquesFrontServiceComMaterialSemId,
                List.of(criaPoliticaEstoquesMaterialLocationSalva("INV_POLICY_01", " ", "LOC_01")));

        IllegalArgumentException materialSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontServiceComMaterialSemId.savePoliticaEstoquesDTO(
                        politicaEstoquesDTO));
        Assertions.assertEquals(
                "Saved inventory policy material/location item at index 0 must have a material id",
                materialSemIdException.getMessage());

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComLocationSemId =
                new PoliticaEstoquesFacade();
        injetaRepositoriesParaSaveAllRetornando(
                politicaEstoquesFrontServiceComLocationSemId,
                List.of(criaPoliticaEstoquesMaterialLocationSalva("INV_POLICY_01", "MAT_01", " ")));

        IllegalArgumentException locationSemIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontServiceComLocationSemId.savePoliticaEstoquesDTO(
                        politicaEstoquesDTO));
        Assertions.assertEquals(
                "Saved inventory policy material/location item at index 0 must have a location id",
                locationSemIdException.getMessage());

    }

    @Test
    public void savePoliticaEstoquesDTOShouldRejectBrokenSavedPolicySnapshotBeforeReplacingLines()
            throws Exception {

        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");

        PoliticaEstoquesFacade politicaEstoquesFrontServiceComSnapshotNulo =
                new PoliticaEstoquesFacade();
        injetaPoliticaEstoquesRepositoryParaSaveRetornando(
                politicaEstoquesFrontServiceComSnapshotNulo,
                null);

        IllegalArgumentException nullSnapshotException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontServiceComSnapshotNulo.savePoliticaEstoquesDTO(
                        politicaEstoquesDTO));

        Assertions.assertEquals(
                "Saved inventory policy snapshot is required",
                nullSnapshotException.getMessage());

        PoliticaEstoques politicaEstoquesSalvaSemId = new PoliticaEstoques();
        politicaEstoquesSalvaSemId.setId(" ");
        PoliticaEstoquesFacade politicaEstoquesFrontServiceComSnapshotSemId =
                new PoliticaEstoquesFacade();
        injetaPoliticaEstoquesRepositoryParaSaveRetornando(
                politicaEstoquesFrontServiceComSnapshotSemId,
                politicaEstoquesSalvaSemId);

        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> politicaEstoquesFrontServiceComSnapshotSemId.savePoliticaEstoquesDTO(
                        politicaEstoquesDTO));

        Assertions.assertEquals(
                "Saved inventory policy id is required",
                missingIdException.getMessage());

    }

    private static void assertAutowiredRequired(String fieldName) throws Exception {

        Field field = PoliticaEstoquesFacade.class.getDeclaredField(fieldName);
        Autowired autowired = field.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                "PoliticaEstoquesFrontService." + fieldName + " deve usar @Autowired explicito");
        Assertions.assertTrue(
                autowired.required(),
                "PoliticaEstoquesFrontService." + fieldName + " deve ser bean obrigatorio");

    }

    private static PoliticaEstoques criaPoliticaEstoquesComFrequenciaLegada() {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId("INV_POLICY_01");
        politicaEstoques.setPrioridade(10);
        politicaEstoques.setDataHorarioInicio(LocalDateTime.of(2026, 1, 1, 0, 0));
        politicaEstoques.setDataHorarioFim(LocalDateTime.of(2026, 12, 31, 23, 59));

        Produto material = new Produto("MAT_01");
        Location location = new Location("LOC_01");
        PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation =
                new PoliticaEstoquesMaterialLocation(
                        new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                                politicaEstoques,
                                material,
                                location));
        politicaEstoquesMaterialLocation.setModeloReabastecimento(Constantes.SNPModeloReabastecimento.DRP);
        politicaEstoquesMaterialLocation.setModeloOperacional(Constantes.SNPModeloOperacional.MTS);
        politicaEstoquesMaterialLocation.setCalculoSafetyStock(Constantes.SNPCalculoSafetyStock.QUANTITY);
        politicaEstoquesMaterialLocation.setEstoqueSegurancaDrpOuTargetKanban(25.0d);
        politicaEstoquesMaterialLocation.setEstoqueMaximoDrp(80.0d);
        politicaEstoquesMaterialLocation.setFrequenciaReabastecimentoDias(14.0d);
        politicaEstoques.getPoliticaEstoquesMaterialLocationList().add(politicaEstoquesMaterialLocation);
        return politicaEstoques;

    }

    private static void injetaPoliticaEstoquesRepositorySomenteLeitura(
            PoliticaEstoquesFacade politicaEstoquesFrontService,
            PoliticaEstoques politicaEstoques) throws Exception {

        injetaPoliticaEstoquesRepositorySomenteLeituraLista(
                politicaEstoquesFrontService,
                List.of(politicaEstoques));

    }

    private static void injetaPoliticaEstoquesRepositorySomenteLeituraLista(
            PoliticaEstoquesFacade politicaEstoquesFrontService,
            List<PoliticaEstoques> politicaEstoquesList) throws Exception {

        PoliticaEstoquesRepository politicaEstoquesRepository =
                (PoliticaEstoquesRepository) Proxy.newProxyInstance(
                        PoliticaEstoquesRepository.class.getClassLoader(),
                        new Class<?>[]{PoliticaEstoquesRepository.class},
                        (proxy, method, args) -> {

                            if ("customFindAllWithMaterialLocation".equals(method.getName())) {
                                return politicaEstoquesList;
                            }
                            if ("toString".equals(method.getName())) {
                                return "PoliticaEstoquesRepository test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Repository method should not be called by read-only DTO list test: "
                                            + method.getName());

                        });

        setField(
                politicaEstoquesFrontService,
                "politicaEstoquesRepository",
                politicaEstoquesRepository);

    }

    private static void injetaRepositoriesParaSaveAllNulo(
            PoliticaEstoquesFacade politicaEstoquesFrontService) throws Exception {

        injetaRepositoriesParaSaveAllRetornando(politicaEstoquesFrontService, null);

    }

    private static void injetaRepositoriesParaSaveAllRetornando(
            PoliticaEstoquesFacade politicaEstoquesFrontService,
            List<PoliticaEstoquesMaterialLocation> politicaEstoquesMaterialLocationListSalva) throws Exception {

        injetaRepositoriesParaSaveAllRetornandoComMaterialELocation(
                politicaEstoquesFrontService,
                politicaEstoquesMaterialLocationListSalva,
                Optional.of(new Produto("MAT_01")),
                Optional.of(new Location("LOC_01")));

    }

    private static void injetaRepositoriesParaSaveAllRetornandoComMaterialELocation(
            PoliticaEstoquesFacade politicaEstoquesFrontService,
            List<PoliticaEstoquesMaterialLocation> politicaEstoquesMaterialLocationListSalva,
            Optional<Produto> materialOptional,
            Optional<Location> locationOptional) throws Exception {

        PoliticaEstoquesRepository politicaEstoquesRepository =
                (PoliticaEstoquesRepository) Proxy.newProxyInstance(
                        PoliticaEstoquesRepository.class.getClassLoader(),
                        new Class<?>[]{PoliticaEstoquesRepository.class},
                        (proxy, method, args) -> {
                            if ("findById".equals(method.getName())) {
                                return Optional.empty();
                            }
                            if ("save".equals(method.getName())) {
                                return args[0];
                            }
                            if ("toString".equals(method.getName())) {
                                return "PoliticaEstoquesRepository save test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Policy repository method should not be called by saved snapshot test: "
                                            + method.getName());
                        });
        PoliticaEstoquesMaterialLocationRepository politicaEstoquesMaterialLocationRepository =
                (PoliticaEstoquesMaterialLocationRepository) Proxy.newProxyInstance(
                        PoliticaEstoquesMaterialLocationRepository.class.getClassLoader(),
                        new Class<?>[]{PoliticaEstoquesMaterialLocationRepository.class},
                        (proxy, method, args) -> {
                            if ("removeByPoliticaEstoquesMaterialLocationCompositeKeyPoliticaEstoquesId".equals(method.getName())) {
                                return null;
                            }
                            if ("saveAll".equals(method.getName())) {
                                return politicaEstoquesMaterialLocationListSalva;
                            }
                            if ("toString".equals(method.getName())) {
                                return "PoliticaEstoquesMaterialLocationRepository save test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Policy material/location repository method should not be called by saved snapshot test: "
                                            + method.getName());
                        });
        ProdutoRepository produtoRepository =
                (ProdutoRepository) Proxy.newProxyInstance(
                        ProdutoRepository.class.getClassLoader(),
                        new Class<?>[]{ProdutoRepository.class},
                        (proxy, method, args) -> {
                            if ("findById".equals(method.getName())) {
                                return materialOptional;
                            }
                            if ("toString".equals(method.getName())) {
                                return "ProdutoRepository save test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Material repository method should not be called by saved snapshot test: "
                                            + method.getName());
                        });
        LocationRepository locationRepository =
                (LocationRepository) Proxy.newProxyInstance(
                        LocationRepository.class.getClassLoader(),
                        new Class<?>[]{LocationRepository.class},
                        (proxy, method, args) -> {
                            if ("findById".equals(method.getName())) {
                                return locationOptional;
                            }
                            if ("toString".equals(method.getName())) {
                                return "LocationRepository save test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Location repository method should not be called by saved snapshot test: "
                                            + method.getName());
                        });

        setField(
                politicaEstoquesFrontService,
                "politicaEstoquesRepository",
                politicaEstoquesRepository);
        setField(
                politicaEstoquesFrontService,
                "politicaEstoquesMaterialLocationRepository",
                politicaEstoquesMaterialLocationRepository);
        setField(
                politicaEstoquesFrontService,
                "produtoRepository",
                produtoRepository);
        setField(
                politicaEstoquesFrontService,
                "locationRepository",
                locationRepository);

    }

    private static PoliticaEstoquesDTO criaPoliticaEstoquesDTOParaSaveMaterialLocation() {

        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId("INV_POLICY_01");
        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        politicaEstoquesMaterialLocationDTO.setMaterialId("MAT_01");
        politicaEstoquesMaterialLocationDTO.setLocationId("LOC_01");
        politicaEstoquesDTO.getMaterialLocationList().add(politicaEstoquesMaterialLocationDTO);
        return politicaEstoquesDTO;

    }

    private static PoliticaEstoquesMaterialLocation criaPoliticaEstoquesMaterialLocationSalva(
            String politicaEstoquesId,
            String materialId,
            String locationId) {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId(politicaEstoquesId);
        Produto material = new Produto(materialId);
        Location location = new Location(locationId);
        return new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                        politicaEstoques,
                        material,
                        location));

    }

    private static void injetaPoliticaEstoquesRepositoryParaSaveRetornando(
            PoliticaEstoquesFacade politicaEstoquesFrontService,
            PoliticaEstoques politicaEstoquesSalva) throws Exception {

        PoliticaEstoquesRepository politicaEstoquesRepository =
                (PoliticaEstoquesRepository) Proxy.newProxyInstance(
                        PoliticaEstoquesRepository.class.getClassLoader(),
                        new Class<?>[]{PoliticaEstoquesRepository.class},
                        (proxy, method, args) -> {
                            if ("findById".equals(method.getName())) {
                                return Optional.empty();
                            }
                            if ("save".equals(method.getName())) {
                                return politicaEstoquesSalva;
                            }
                            if ("toString".equals(method.getName())) {
                                return "PoliticaEstoquesRepository policy save test double";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new AssertionError(
                                    "Policy repository method should not be called by saved policy snapshot test: "
                                            + method.getName());
                        });

        setField(
                politicaEstoquesFrontService,
                "politicaEstoquesRepository",
                politicaEstoquesRepository);

    }

    private static PoliticaEstoquesRepository getPoliticaEstoquesRepositoryParaFindByIdRetornando(
            Optional<PoliticaEstoques> politicaEstoquesOptional) {

        return (PoliticaEstoquesRepository) Proxy.newProxyInstance(
                PoliticaEstoquesRepository.class.getClassLoader(),
                new Class<?>[]{PoliticaEstoquesRepository.class},
                (proxy, method, args) -> {
                    if ("customFindById".equals(method.getName())) {
                        return politicaEstoquesOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "PoliticaEstoquesRepository lookup test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Policy repository method should not be called by lookup Optional test: "
                                    + method.getName());
                });

    }

    private static PoliticaEstoquesRepository getPoliticaEstoquesRepositoryParaSaveComFindByIdRetornando(
            Optional<PoliticaEstoques> politicaEstoquesOptional) {

        return (PoliticaEstoquesRepository) Proxy.newProxyInstance(
                PoliticaEstoquesRepository.class.getClassLoader(),
                new Class<?>[]{PoliticaEstoquesRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return politicaEstoquesOptional;
                    }
                    if ("toString".equals(method.getName())) {
                        return "PoliticaEstoquesRepository save Optional test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Policy repository method should not be called after null Optional: "
                                    + method.getName());
                });

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
