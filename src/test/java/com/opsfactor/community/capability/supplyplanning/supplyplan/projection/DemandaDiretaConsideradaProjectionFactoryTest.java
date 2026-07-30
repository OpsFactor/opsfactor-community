package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Testes da factory Community da demanda direta considerada.
 *
 * <p>A factory e uma fronteira importante entre Demand Planning e Supply
 * Planning heuristico: ela materializa a demanda direta ja persistida e a
 * entrega ordenada para a projection em memoria. Estes testes nao sobem JPA;
 * eles protegem o contrato de input e de snapshot retornado pelos repositories
 * antes que a projection tente indexar linhas incompletas.</p>
 */
public class DemandaDiretaConsideradaProjectionFactoryTest {

    @Test
    public void completeProjectionShouldRejectMissingSupplyPlanBeforeRepositoryAccess() {

        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                new DemandaDiretaConsideradaProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionCompleto(
                        null,
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Direct demand considered projection requires supply plan and calendar.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void completeProjectionShouldRejectNullRepositoryResult() throws Exception {

        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                createDemandaDiretaConsideradaProjectionFactory(
                        null,
                        List.of());

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionCompleto(
                        new SupplyPlan(),
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Direct demand considered complete snapshot repository returned null collection.",
                illegalStateException.getMessage());

    }

    @Test
    public void locationProjectionShouldRejectNullLineReturnedByRepository() throws Exception {

        Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhaCollection = new ArrayList<>();
        demandaDiretaConsideradaLinhaCollection.add(null);
        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                createDemandaDiretaConsideradaProjectionFactory(
                        List.of(),
                        demandaDiretaConsideradaLinhaCollection);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionParaLocation(
                        new SupplyPlan(),
                        getCalendarioTeste(),
                        new Location("LOC")));

        Assertions.assertEquals(
                "Direct demand considered location snapshot repository returned null line.",
                illegalStateException.getMessage());

    }

    @Test
    public void locationProjectionShouldRejectIncompleteLineReturnedByRepository() throws Exception {

        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                createDemandaDiretaConsideradaProjectionFactory(
                        List.of(),
                        List.of(new DemandaDiretaConsideradaLinha()));

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionParaLocation(
                        new SupplyPlan(),
                        getCalendarioTeste(),
                        new Location("LOC")));

        Assertions.assertEquals(
                "Direct demand considered location snapshot repository returned line without supply plan, location, material or reference date.",
                illegalStateException.getMessage());

    }

    @Test
    public void locationProjectionDerivedFromCompleteProjectionShouldRejectMissingSourceProjection() {

        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                new DemandaDiretaConsideradaProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionParaLocation(
                        new SupplyPlan(),
                        new Location("LOC"),
                        getCalendarioTeste(),
                        null));

        Assertions.assertEquals(
                "Direct demand considered projection derived for location requires loaded complete projection.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void completeProjectionShouldAcceptEmptyRepositoryResultAsValidSnapshot() throws Exception {

        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                createDemandaDiretaConsideradaProjectionFactory(
                        List.of(),
                        List.of());

        DemandaDiretaConsideradaProjection demandaDiretaConsideradaProjection =
                demandaDiretaConsideradaProjectionFactory.getDemandaDiretaConsideradaProjectionCompleto(
                        new SupplyPlan(),
                        getCalendarioTeste());

        Assertions.assertTrue(demandaDiretaConsideradaProjection.getAllDemandaDiretaConsideradaLinha().isEmpty());

    }

    private DemandaDiretaConsideradaProjectionFactory createDemandaDiretaConsideradaProjectionFactory(
            Collection<DemandaDiretaConsideradaLinha> completeProjectionRepositoryResult,
            Collection<DemandaDiretaConsideradaLinha> locationProjectionRepositoryResult) throws Exception {

        DemandaDiretaConsideradaProjectionFactory demandaDiretaConsideradaProjectionFactory =
                new DemandaDiretaConsideradaProjectionFactory();

        setField(
                demandaDiretaConsideradaProjectionFactory,
                "demandaDiretaConsideradaLinhaRepository",
                createDemandaDiretaConsideradaLinhaRepository(
                        completeProjectionRepositoryResult,
                        locationProjectionRepositoryResult));
        setField(
                demandaDiretaConsideradaProjectionFactory,
                "unidadeMedidaProjectionFactory",
                createUnidadeMedidaProjectionFactory());

        return demandaDiretaConsideradaProjectionFactory;

    }

    private DemandaDiretaConsideradaLinhaRepository createDemandaDiretaConsideradaLinhaRepository(
            Collection<DemandaDiretaConsideradaLinha> completeProjectionRepositoryResult,
            Collection<DemandaDiretaConsideradaLinha> locationProjectionRepositoryResult) {

        return (DemandaDiretaConsideradaLinhaRepository) Proxy.newProxyInstance(
                DemandaDiretaConsideradaLinhaRepository.class.getClassLoader(),
                new Class<?>[]{DemandaDiretaConsideradaLinhaRepository.class},
                (proxy, method, args) -> {

                    if (method.getName().equals("customFindAllBySupplyPlan")) {
                        return completeProjectionRepositoryResult;
                    }
                    if (method.getName().equals("customFindAllBySupplyPlanAndLocation")) {
                        return locationProjectionRepositoryResult;
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no teste: " + method.getName());

                });

    }

    private UnidadeMedidaProjectionFactory createUnidadeMedidaProjectionFactory() {

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                Mockito.mock(UnidadeMedidaProjectionFactory.class);
        Mockito.when(unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionCompletoDeCache())
                .thenReturn(Mockito.mock(UnidadeMedidaProjection.class));
        return unidadeMedidaProjectionFactory;

    }

    private void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(
                target,
                value);

    }

    private Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

    }

}
