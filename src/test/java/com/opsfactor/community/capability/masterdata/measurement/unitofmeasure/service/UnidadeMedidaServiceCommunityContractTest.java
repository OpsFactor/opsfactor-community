package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;

/**
 * Contratos Community do bootstrap de unidade de medida padrao.
 *
 * <p>A unidade `UN` fica no Community porque é base fisica para demanda,
 * estoque, producao e transporte. O service deve falhar cedo se o repository
 * devolver lookup ou snapshot salvo quebrado, evitando que projections de
 * quantidade rodem com unidade padrao ausente ou incoerente.</p>
 */
public class UnidadeMedidaServiceCommunityContractTest {

    @Test
    public void criaUnidadeMedidaUNShouldNotSaveWhenDefaultUomExists() throws Exception {

        RepositoryState repositoryState =
                new RepositoryState(Optional.of(new UnidadeMedida("UN")), null);
        UnidadeMedidaService unidadeMedidaService = getUnidadeMedidaService(repositoryState);

        executaCriaUnidadeMedidaUN(unidadeMedidaService);

        Assertions.assertFalse(repositoryState.saveCalled);

    }

    @Test
    public void criaUnidadeMedidaUNShouldCreateAndValidateMissingDefaultUom() throws Exception {

        UnidadeMedida unidadeMedidaSalva = new UnidadeMedida("UN");
        RepositoryState repositoryState = new RepositoryState(Optional.empty(), unidadeMedidaSalva);
        UnidadeMedidaService unidadeMedidaService = getUnidadeMedidaService(repositoryState);

        executaCriaUnidadeMedidaUN(unidadeMedidaService);

        Assertions.assertTrue(repositoryState.saveCalled);
        Assertions.assertEquals("UN", repositoryState.savedEntity.getId());
        Assertions.assertEquals("Units", repositoryState.savedEntity.getDescricao());

    }

    @Test
    public void criaUnidadeMedidaUNShouldRejectNullLookupOptionalBeforeSave() throws Exception {

        RepositoryState repositoryState = new RepositoryState(null, new UnidadeMedida("UN"));
        UnidadeMedidaService unidadeMedidaService = getUnidadeMedidaService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> executaCriaUnidadeMedidaUN(unidadeMedidaService));

        Assertions.assertEquals(
                "Default UOM lookup result is required.",
                illegalArgumentException.getMessage());
        Assertions.assertFalse(repositoryState.saveCalled);

    }

    @Test
    public void criaUnidadeMedidaUNShouldRejectExistingDefaultUomWithWrongId() throws Exception {

        RepositoryState repositoryState =
                new RepositoryState(Optional.of(new UnidadeMedida("KG")), null);
        UnidadeMedidaService unidadeMedidaService = getUnidadeMedidaService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> executaCriaUnidadeMedidaUN(unidadeMedidaService));

        Assertions.assertEquals(
                "Default UOM must have id UN.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void criaUnidadeMedidaUNShouldRejectNullSavedDefaultUom() throws Exception {

        RepositoryState repositoryState = new RepositoryState(Optional.empty(), null);
        UnidadeMedidaService unidadeMedidaService = getUnidadeMedidaService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> executaCriaUnidadeMedidaUN(unidadeMedidaService));

        Assertions.assertEquals(
                "Default UOM UN is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void criaUnidadeMedidaUNShouldRejectSavedDefaultUomWithWrongId() throws Exception {

        RepositoryState repositoryState = new RepositoryState(Optional.empty(), new UnidadeMedida("KG"));
        UnidadeMedidaService unidadeMedidaService = getUnidadeMedidaService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> executaCriaUnidadeMedidaUN(unidadeMedidaService));

        Assertions.assertEquals(
                "Default UOM must have id UN.",
                illegalArgumentException.getMessage());

    }

    private static UnidadeMedidaService getUnidadeMedidaService(
            RepositoryState repositoryState) throws Exception {

        UnidadeMedidaService unidadeMedidaService = new UnidadeMedidaService();
        setField(
                unidadeMedidaService,
                "unidadeMedidaRepository",
                getUnidadeMedidaRepository(repositoryState));
        return unidadeMedidaService;

    }

    private static UnidadeMedidaRepository getUnidadeMedidaRepository(
            RepositoryState repositoryState) {

        return (UnidadeMedidaRepository) Proxy.newProxyInstance(
                UnidadeMedidaRepository.class.getClassLoader(),
                new Class<?>[]{UnidadeMedidaRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return repositoryState.findByIdResult;
                    }
                    if ("save".equals(method.getName())) {
                        repositoryState.saveCalled = true;
                        repositoryState.savedEntity = (UnidadeMedida) args[0];
                        return repositoryState.saveResult;
                    }
                    if ("toString".equals(method.getName())) {
                        return "UnidadeMedidaRepository test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Repository method should not be called by UnidadeMedidaService test: "
                                    + method.getName());
                });

    }

    private static void executaCriaUnidadeMedidaUN(
            UnidadeMedidaService unidadeMedidaService) throws Exception {

        Method method = UnidadeMedidaService.class.getDeclaredMethod("criaUnidadeMedidaUN");
        method.setAccessible(true);
        try {
            method.invoke(unidadeMedidaService);
        } catch (InvocationTargetException invocationTargetException) {
            Throwable causa = invocationTargetException.getCause();
            if (causa instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (causa instanceof Error error) {
                throw error;
            }
            throw new Exception(causa);
        }

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private static class RepositoryState {

        private final Optional<UnidadeMedida> findByIdResult;

        private final UnidadeMedida saveResult;

        private boolean saveCalled;

        private UnidadeMedida savedEntity;

        private RepositoryState(
                Optional<UnidadeMedida> findByIdResult,
                UnidadeMedida saveResult) {

            this.findByIdResult = findByIdResult;
            this.saveResult = saveResult;

        }

    }

}
