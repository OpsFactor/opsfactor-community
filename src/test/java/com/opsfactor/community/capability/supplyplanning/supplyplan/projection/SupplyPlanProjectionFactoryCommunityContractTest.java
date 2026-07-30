package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Contrato Community da factory de projections de Supply Planning.
 *
 * <p>A factory pode depender apenas dos repositories/projections operacionais
 * usados pelo motor heuristico. Custos, precos, pedidos transacionais,
 * otimizador, P&L e diagnostics Enterprise devem entrar por factories
 * Enterprise separadas.</p>
 */
class SupplyPlanProjectionFactoryCommunityContractTest {

    @Test
    void supplyPlanningProjectionShouldNotDeclareEnterpriseEconomicOrOptimizerState() {

        /*
         * A projection Community pode guardar estado operacional de estoque,
         * producao, distribuicao e demanda direta considerada. Estado economico,
         * otimizador, solver, frotas, impostos e diagnosticos de restricao
         * devem aparecer em projections Enterprise separadas.
         */
        List<String> forbiddenFieldFragmentList = List.of(
                "cost",
                "custo",
                "price",
                "preco",
                "optimizer",
                "otimiz",
                "solver",
                "pnl",
                "margin",
                "margem",
                "tax",
                "imposto",
                "fleet",
                "vehicle",
                "constraint");

        List<String> forbiddenFieldNameList = Arrays.stream(SupplyPlanningProjection.class.getDeclaredFields())
                .map(Field::getName)
                .filter(fieldName -> {
                    String normalizedFieldName = fieldName.toLowerCase(Locale.ROOT);
                    return forbiddenFieldFragmentList.stream().anyMatch(normalizedFieldName::contains);
                })
                .toList();

        Assertions.assertTrue(
                forbiddenFieldNameList.isEmpty(),
                "SupplyPlanningProjection Community nao deve declarar estado Enterprise: " + forbiddenFieldNameList);

    }

    @Test
    void factoryShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                SupplyPlanProjectionFactory.class,
                List.of(
                        "distributionPlanItemRepository",
                        "inventoryPlanLinhaRepository",
                        "productionPlanLinhaRepository",
                        "supplyPlanPersistedBaselinePreflight",
                        "demandaDiretaConsideradaProjectionFactory"));

    }

    @Test
    void directDemandProjectionFactoryShouldUseExplicitAutowiredBeanFields() throws Exception {

        assertAutowiredFields(
                DemandaDiretaConsideradaProjectionFactory.class,
                List.of(
                        "demandaDiretaConsideradaLinhaRepository",
                        "unidadeMedidaProjectionFactory"));

    }

    @Test
    void factoryShouldRejectProjectionCalendarWithDifferentBucketBeforeProjectionCreation() {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        Calendario calendarioSemanal = Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.SEMANAL,
                LocalDateTime.of(2026, 6, 24, 0, 0),
                0,
                0,
                1,
                0);
        SupplyPlanProjectionFactory supplyPlanProjectionFactory = new SupplyPlanProjectionFactory();

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> supplyPlanProjectionFactory.getSupplyPlanningProjectionVazio(
                        supplyPlan,
                        calendarioSemanal,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        Assertions.assertTrue(illegalArgumentException.getMessage().contains(
                "SupplyPlanProjectionFactory requires the projection calendar bucket to match the Supply Plan bucket"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("projection bucket=SEMANAL"));
        Assertions.assertTrue(illegalArgumentException.getMessage().contains("supply plan bucket=MENSAL"));

    }

    @Test
    void repositoryLineValidatorsShouldRejectBrokenSnapshotsBeforeParallelPopulation() throws Exception {

        SupplyPlanProjectionFactory supplyPlanProjectionFactory = new SupplyPlanProjectionFactory();

        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaInventoryPlanLinhaRepositoryResult",
                null,
                "Supply Plan projection population repository returned null Inventory Plan line collection.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaInventoryPlanLinhaRepositoryResult",
                Arrays.asList((InventoryPlanLinha) null),
                "Supply Plan projection population repository returned null Inventory Plan line at index 0.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaInventoryPlanLinhaRepositoryResult",
                List.of(new InventoryPlanLinha()),
                "Supply Plan projection population repository returned Inventory Plan line without supply plan, location, material or reference date at index 0.");

        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaDistributionPlanItemRepositoryResult",
                null,
                "Supply Plan projection population repository returned null Distribution Plan line collection.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaDistributionPlanItemRepositoryResult",
                Arrays.asList((DistributionPlanItem) null),
                "Supply Plan projection population repository returned null Distribution Plan line at index 0.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaDistributionPlanItemRepositoryResult",
                List.of(new DistributionPlanItem()),
                "Supply Plan projection population repository returned Distribution Plan line without supply plan, origin, destination, material, shipping date or receiving date at index 0.");

        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaProductionPlanLinhaRepositoryResult",
                null,
                "Supply Plan projection population repository returned null Production Plan line collection.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaProductionPlanLinhaRepositoryResult",
                Arrays.asList((ProductionPlanLinha) null),
                "Supply Plan projection population repository returned null Production Plan line at index 0.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaProductionPlanLinhaRepositoryResult",
                List.of(new ProductionPlanLinha()),
                "Supply Plan projection population repository returned Production Plan line without supply plan, location, output material, production version, routing, bill of materials or reference date at index 0.");
        assertRepositoryValidationFailure(
                supplyPlanProjectionFactory,
                "validaProductionPlanLinhaRepositoryResult",
                getProductionPlanLinhaDuplicadaListParaTeste(),
                "Supply Plan projection population repository returned duplicated Production Plan line for supply plan 1, location LOC, production version PV, routing ROUTING, bill of materials BOM and reference date 2026-01-10T00:00.");

        /*
         * Uma consulta sem linhas e snapshot valido: significa que ainda nao ha
         * dados materializados para o plano/recorte, nao erro de repository.
         */
        Assertions.assertDoesNotThrow(
                () -> invokeRepositoryValidation(
                        supplyPlanProjectionFactory,
                        "validaInventoryPlanLinhaRepositoryResult",
                        List.of()));
        Assertions.assertDoesNotThrow(
                () -> invokeRepositoryValidation(
                        supplyPlanProjectionFactory,
                        "validaDistributionPlanItemRepositoryResult",
                        List.of()));
        Assertions.assertDoesNotThrow(
                () -> invokeRepositoryValidation(
                        supplyPlanProjectionFactory,
                        "validaProductionPlanLinhaRepositoryResult",
                        List.of()));

    }

    private static void assertIllegalArgumentMessage(
            ProjectionFactoryCall projectionFactoryCall,
            String expectedMessage) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                projectionFactoryCall::execute);
        Assertions.assertEquals(
                expectedMessage,
                illegalArgumentException.getMessage());

    }

    private static void assertAutowiredFields(
            Class<?> factoryClass,
            List<String> beanFieldNameList) throws Exception {

        for (String beanFieldName : beanFieldNameList) {
            Field beanField = factoryClass.getDeclaredField(beanFieldName);
            Autowired autowired = beanField.getAnnotation(Autowired.class);

            Assertions.assertNotNull(
                    autowired,
                    factoryClass.getSimpleName() + "." + beanFieldName + " deve usar @Autowired explicito");
            Assertions.assertTrue(
                    autowired.required(),
                    factoryClass.getSimpleName() + "." + beanFieldName + " deve ser bean obrigatorio");
        }

    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static void assertRepositoryValidationFailure(
            SupplyPlanProjectionFactory supplyPlanProjectionFactory,
            String methodName,
            Collection<?> linhaCollection,
            String expectedMessage) throws Exception {

        InvocationTargetException invocationTargetException = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeRepositoryValidation(
                        supplyPlanProjectionFactory,
                        methodName,
                        linhaCollection));

        Assertions.assertInstanceOf(
                IllegalStateException.class,
                invocationTargetException.getCause());
        Assertions.assertEquals(
                expectedMessage,
                invocationTargetException.getCause().getMessage());

    }

    private static Object invokeRepositoryValidation(
            SupplyPlanProjectionFactory supplyPlanProjectionFactory,
            String methodName,
            Collection<?> linhaCollection) throws Exception {

        Method method = SupplyPlanProjectionFactory.class.getDeclaredMethod(
                methodName,
                Collection.class,
                String.class);
        method.setAccessible(true);
        return method.invoke(
                supplyPlanProjectionFactory,
                linhaCollection,
                "Supply Plan projection population");

    }

    private static List<ProductionPlanLinha> getProductionPlanLinhaDuplicadaListParaTeste() {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(1L);

        Location location = new Location("LOC");
        Produto materialOutput = new Produto("MAT");

        Roteiro roteiro = new Roteiro();
        roteiro.setId("ROUTING");
        roteiro.setLocation(location);
        roteiro.setMaterialOutput(materialOutput);

        ListaTecnica listaTecnica = new ListaTecnica();
        listaTecnica.setId("BOM");
        listaTecnica.setLocation(location);
        listaTecnica.setMaterialOutput(materialOutput);

        VersaoProducaoSimples versaoProducao = new VersaoProducaoSimples(
                "PV",
                location,
                1,
                materialOutput,
                roteiro,
                listaTecnica);

        ProductionPlanLinha primeiraProductionPlanLinha = new ProductionPlanLinha(
                new ProductionPlanLinha.ProductionPlanLinhaCompositeKey(
                        supplyPlan,
                        location,
                        versaoProducao,
                        roteiro,
                        listaTecnica,
                        LocalDateTime.of(2026, 1, 10, 0, 0)),
                materialOutput);

        ProductionPlanLinha segundaProductionPlanLinha = new ProductionPlanLinha(
                new ProductionPlanLinha.ProductionPlanLinhaCompositeKey(
                        supplyPlan,
                        location,
                        versaoProducao,
                        roteiro,
                        listaTecnica,
                        LocalDateTime.of(2026, 1, 10, 0, 0)),
                materialOutput);

        return List.of(primeiraProductionPlanLinha, segundaProductionPlanLinha);

    }

    @FunctionalInterface
    private interface ProjectionFactoryCall {

        void execute();

    }

}
