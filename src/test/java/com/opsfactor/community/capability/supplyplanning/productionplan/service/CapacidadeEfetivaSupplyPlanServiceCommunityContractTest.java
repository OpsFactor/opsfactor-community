package com.opsfactor.community.capability.supplyplanning.productionplan.service;

import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.productionplan.repository.CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contratos Community da persistencia do snapshot de capacidade efetiva do
 * Supply Plan.
 *
 * <p>Capacidade efetiva produtiva e fluxo padrao do heuristico Community, nao
 * pos-processamento Enterprise. Por isso a borda de persistencia precisa ser
 * explicita: snapshot estrutural quebrado deve falhar antes de apagar a
 * fotografia anterior e colecoes retornadas por repository precisam ser
 * validadas antes da montagem do mapa em memoria.</p>
 */
class CapacidadeEfetivaSupplyPlanServiceCommunityContractTest {

    @Test
    void salvaCapacidadesEfetivasShouldRejectBrokenInputBeforeDeletingPreviousSnapshot() throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        CapacidadeEfetivaRepositoryInvocationHandler capacidadeEfetivaRepositoryInvocationHandler =
                new CapacidadeEfetivaRepositoryInvocationHandler(List.of());
        setField(
                capacidadeEfetivaSupplyPlanService,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                capacidadeEfetivaRepositoryInvocationHandler.getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> capacidadeEfetivaSupplyPlanService.salvaCapacidadesEfetivasSupplyPlan(
                        getSupplyPlanComId(10L),
                        null,
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Calendar is required to persist Community effective production capacity snapshot.",
                illegalArgumentException.getMessage());
        Assertions.assertFalse(
                capacidadeEfetivaRepositoryInvocationHandler.removeBySupplyPlanIdCalled,
                "Snapshot antigo nao deve ser apagado quando a entrada minima da rodada esta quebrada.");
        Assertions.assertFalse(
                capacidadeEfetivaRepositoryInvocationHandler.saveAllCalled,
                "Snapshot novo nao deve ser persistido quando a entrada minima da rodada esta quebrada.");

    }

    @Test
    void salvaCapacidadesEfetivasShouldRejectMissingSupplyPlanIdBeforeDeletingPreviousSnapshot() throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        CapacidadeEfetivaRepositoryInvocationHandler capacidadeEfetivaRepositoryInvocationHandler =
                new CapacidadeEfetivaRepositoryInvocationHandler(List.of());
        setField(
                capacidadeEfetivaSupplyPlanService,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                capacidadeEfetivaRepositoryInvocationHandler.getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> capacidadeEfetivaSupplyPlanService.salvaCapacidadesEfetivasSupplyPlan(
                        new SupplyPlan(),
                        getCalendarioTeste(),
                        null,
                        null,
                        null));

        Assertions.assertEquals(
                "Supply Plan id is required for Community effective production capacity snapshot.",
                illegalArgumentException.getMessage());
        Assertions.assertFalse(
                capacidadeEfetivaRepositoryInvocationHandler.removeBySupplyPlanIdCalled,
                "Snapshot antigo nao deve ser apagado quando o Supply Plan ainda nao tem id.");

    }

    @Test
    void removeBySupplyPlanIdShouldRejectMissingIdBeforeRepository() throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        CapacidadeEfetivaRepositoryInvocationHandler capacidadeEfetivaRepositoryInvocationHandler =
                new CapacidadeEfetivaRepositoryInvocationHandler(List.of());
        setField(
                capacidadeEfetivaSupplyPlanService,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                capacidadeEfetivaRepositoryInvocationHandler.getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> capacidadeEfetivaSupplyPlanService.removeBySupplyPlanId(null));

        Assertions.assertEquals(
                "Supply Plan id is required to remove Community effective production capacity snapshot.",
                illegalArgumentException.getMessage());
        Assertions.assertFalse(
                capacidadeEfetivaRepositoryInvocationHandler.removeBySupplyPlanIdCalled,
                "Repository nao deve receber remocao destrutiva sem Supply Plan id.");

    }

    @Test
    void getMapaCapacidadeShouldRejectBrokenRepositorySnapshotBeforeMapping() throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas =
                new ArrayList<>();
        capacidadesProdutivas.add(null);
        setField(
                capacidadeEfetivaSupplyPlanService,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                new CapacidadeEfetivaRepositoryInvocationHandler(capacidadesProdutivas).getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> capacidadeEfetivaSupplyPlanService.getMapaCapacidadeProdutivaPorRecursoPeriodo(
                        getSupplyPlanComId(20L),
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Loaded effective production capacity snapshot item at index 0 is required for Community Supply Planning.",
                illegalArgumentException.getMessage());

    }

    @Test
    void getMapaCapacidadeShouldRejectSnapshotItemWithoutCompositeKeyBeforeMapping() throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        setField(
                capacidadeEfetivaSupplyPlanService,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                new CapacidadeEfetivaRepositoryInvocationHandler(List.of(
                        new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan())).getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> capacidadeEfetivaSupplyPlanService.getMapaCapacidadeProdutivaPorRecursoPeriodo(
                        getSupplyPlanComId(25L),
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Loaded effective production capacity snapshot item at index 0 must have a composite key.",
                illegalArgumentException.getMessage());

    }

    @Test
    void getMapaCapacidadeShouldRejectNullRepositoryCollectionBeforeMapping() throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        setField(
                capacidadeEfetivaSupplyPlanService,
                "capacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository",
                new CapacidadeEfetivaRepositoryInvocationHandler(null).getProxy());

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> capacidadeEfetivaSupplyPlanService.getMapaCapacidadeProdutivaPorRecursoPeriodo(
                        getSupplyPlanComId(30L),
                        getCalendarioTeste()));

        Assertions.assertEquals(
                "Loaded effective production capacity snapshot collection is required for Community Supply Planning.",
                illegalArgumentException.getMessage());

    }

    @Test
    void savedCapacitySnapshotShouldRejectNullRepositoryCollection() throws Exception {

        IllegalArgumentException illegalArgumentException =
                getSavedCapacityValidationFailure(null);

        Assertions.assertEquals(
                "Saved effective production capacity snapshot collection is required for Community Supply Planning.",
                illegalArgumentException.getMessage());

    }

    @Test
    void savedCapacitySnapshotShouldRejectPartialRepositoryCollection() throws Exception {

        IllegalArgumentException illegalArgumentException =
                getSavedCapacityValidationFailure(
                        List.of(criaCapacidadeProdutivaEfetivaParaTeste("RESOURCE-PARTIAL")),
                        2);

        Assertions.assertEquals(
                "Saved effective production capacity snapshot size 1 differs from expected Community effective production capacity snapshot size 2.",
                illegalArgumentException.getMessage());

    }

    @Test
    void savedCapacitySnapshotShouldRejectNullRepositoryItem() throws Exception {

        IllegalArgumentException illegalArgumentException =
                getSavedCapacityValidationFailure(Arrays.asList((CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan) null));

        Assertions.assertEquals(
                "Saved effective production capacity snapshot item at index 0 is required for Community Supply Planning.",
                illegalArgumentException.getMessage());

    }

    @Test
    void savedCapacitySnapshotShouldRejectItemWithoutCompositeKey() throws Exception {

        IllegalArgumentException illegalArgumentException =
                getSavedCapacityValidationFailure(List.of(
                        new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan()));

        Assertions.assertEquals(
                "Saved effective production capacity snapshot item at index 0 must have a composite key.",
                illegalArgumentException.getMessage());

    }

    private static SupplyPlan getSupplyPlanComId(Long supplyPlanId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(supplyPlanId);
        return supplyPlan;

    }

    private static Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static IllegalArgumentException getSavedCapacityValidationFailure(
            List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas) throws Exception {

        return getSavedCapacityValidationFailure(
                capacidadesProdutivas,
                capacidadesProdutivas == null ? 0 : capacidadesProdutivas.size());

    }

    private static IllegalArgumentException getSavedCapacityValidationFailure(
            List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas,
            int quantidadeCapacidadesProdutivasEsperada) throws Exception {

        CapacidadeEfetivaSupplyPlanService capacidadeEfetivaSupplyPlanService =
                new CapacidadeEfetivaSupplyPlanService();
        Method method = CapacidadeEfetivaSupplyPlanService.class.getDeclaredMethod(
                "validaCapacidadesProdutivasSalvasCommunity",
                List.class,
                int.class);
        method.setAccessible(true);

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> method.invoke(
                        capacidadeEfetivaSupplyPlanService,
                        capacidadesProdutivas,
                        quantidadeCapacidadesProdutivasEsperada));
        Assertions.assertInstanceOf(
                IllegalArgumentException.class,
                invocationTargetException.getCause());
        return (IllegalArgumentException) invocationTargetException.getCause();

    }

    private static CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan criaCapacidadeProdutivaEfetivaParaTeste(
            String recursoProdutivoId) {

        RecursoProdutivo recursoProdutivo = new RecursoProdutivo();
        recursoProdutivo.setId(recursoProdutivoId);
        return new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan(
                new CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan
                        .CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanCompositeKey(
                        getSupplyPlanComId(100L),
                        recursoProdutivo,
                        LocalDateTime.of(2026, 1, 31, 23, 59)));

    }

    private static class CapacidadeEfetivaRepositoryInvocationHandler implements InvocationHandler {

        private final List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas;

        private boolean removeBySupplyPlanIdCalled;

        private boolean saveAllCalled;

        private CapacidadeEfetivaRepositoryInvocationHandler(
                List<CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlan> capacidadesProdutivas) {

            this.capacidadesProdutivas = capacidadesProdutivas;

        }

        private CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository getProxy() {

            return (CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository) Proxy.newProxyInstance(
                    CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.class.getClassLoader(),
                    new Class<?>[]{CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository.class},
                    this);

        }

        @Override
        public Object invoke(
                Object proxy,
                java.lang.reflect.Method method,
                Object[] args) {

            if ("removeBySupplyPlanId".equals(method.getName())) {
                removeBySupplyPlanIdCalled = true;
                return null;
            }
            if ("saveAll".equals(method.getName())) {
                saveAllCalled = true;
                return args[0];
            }
            if ("customFindBySupplyPlan".equals(method.getName())) {
                return capacidadesProdutivas;
            }
            if ("toString".equals(method.getName())) {
                return "CapacidadeProdutivaEfetivaRecursoProdutivoSupplyPlanRepository em memoria para teste Community";
            }
            throw new UnsupportedOperationException(
                    "Metodo nao esperado no proxy de teste: " + method.getName());

        }

    }

}
