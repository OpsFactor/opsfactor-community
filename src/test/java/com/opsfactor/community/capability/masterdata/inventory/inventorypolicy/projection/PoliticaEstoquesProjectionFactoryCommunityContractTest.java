package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoPoliticaEstoques;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoPoliticaEstoques.PerfilExecucaoPoliticaEstoquesCompositeKey;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.supplyplanning.configuration.repository.PerfilExecucaoPoliticaEstoquesRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Contrato Community da factory de politicas operacionais de estoque.
 *
 * <p>Inventory Policy Optimization pertence ao Enterprise, mas o Community
 * usa politicas material/location para safety stock operacional. Por isso a
 * factory deve aceitar lista vazia como ausencia de override e falhar cedo
 * quando o snapshot do repository vier estruturalmente quebrado.</p>
 */
class PoliticaEstoquesProjectionFactoryCommunityContractTest {

    @Test
    void factoryShouldAcceptEmptyRepositoryResultAsEmptyProjection() throws Exception {

        ProjectionFactoryFixture projectionFactoryFixture = criaProjectionFactoryFixture(Collections.emptyList());

        PoliticaEstoquesProjection politicaEstoquesProjection =
                projectionFactoryFixture.politicaEstoquesProjectionFactory()
                        .getPoliticaEstoquesProjection(
                                getCalendarioTeste(),
                                Mockito.mock(ClusterEParametrosProjection.class),
                                projectionFactoryFixture.perfilExecucaoSupplyPlan());

        Assertions.assertNotNull(politicaEstoquesProjection);
        Assertions.assertFalse(politicaEstoquesProjection.verificaSeHaPoliticaEstoquesMaterialLocationCadastrada());

    }

    private static ProjectionFactoryFixture criaProjectionFactoryFixture(
            List<PerfilExecucaoPoliticaEstoques> perfilExecucaoPoliticaEstoquesList) throws Exception {

        return criaProjectionFactoryFixture(criaPerfilExecucaoSupplyPlan(), perfilExecucaoPoliticaEstoquesList);

    }

    private static ProjectionFactoryFixture criaProjectionFactoryFixture(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            List<PerfilExecucaoPoliticaEstoques> perfilExecucaoPoliticaEstoquesList) throws Exception {

        PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory =
                new PoliticaEstoquesProjectionFactory();
        PerfilExecucaoPoliticaEstoquesRepository perfilExecucaoPoliticaEstoquesRepository =
                Mockito.mock(PerfilExecucaoPoliticaEstoquesRepository.class);
        Mockito.when(perfilExecucaoPoliticaEstoquesRepository.customFindByPerfilExecucaoSupplyPlan(
                        perfilExecucaoSupplyPlan.getId()))
                .thenReturn(perfilExecucaoPoliticaEstoquesList);
        setPrivateField(
                politicaEstoquesProjectionFactory,
                "perfilExecucaoPoliticaEstoquesRepository",
                perfilExecucaoPoliticaEstoquesRepository);

        return new ProjectionFactoryFixture(
                politicaEstoquesProjectionFactory,
                perfilExecucaoSupplyPlan);

    }

    private static PerfilExecucaoSupplyPlan criaPerfilExecucaoSupplyPlan() {

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = new PerfilExecucaoSupplyPlan();
        perfilExecucaoSupplyPlan.setId("PROFILE");

        return perfilExecucaoSupplyPlan;

    }

    private static PoliticaEstoques criaPoliticaEstoques(String id) {

        PoliticaEstoques politicaEstoques = new PoliticaEstoques();
        politicaEstoques.setId(id);

        return politicaEstoques;

    }

    private static PerfilExecucaoPoliticaEstoques criaPerfilExecucaoPoliticaEstoques(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            PoliticaEstoques politicaEstoques) {

        return new PerfilExecucaoPoliticaEstoques(
                new PerfilExecucaoPoliticaEstoquesCompositeKey(
                        perfilExecucaoSupplyPlan,
                        politicaEstoques));

    }

    private static PoliticaEstoquesMaterialLocation criaPoliticaEstoquesMaterialLocation(
            PoliticaEstoques politicaEstoques) {

        Produto material = new Produto();
        material.setId("MAT");
        Location location = new Location();
        location.setId("LOC");

        return new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocationCompositeKey(
                        politicaEstoques,
                        material,
                        location));

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private record ProjectionFactoryFixture(
            PoliticaEstoquesProjectionFactory politicaEstoquesProjectionFactory,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {
    }

}
