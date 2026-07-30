package com.opsfactor.community.capability.masterdata.production.productionversion.service;

import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoInexistente;
import com.opsfactor.community.capability.masterdata.production.productionversion.repository.VersaoProducaoInexistenteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Contratos Community do service da sentinela de versao de producao inexistente.
 *
 * <p>A sentinela e um cadastro tecnico unico usado por projections e pelo
 * Supply Planning heuristico quando roteiro/BOM podem operar sem versao simples
 * de producao. Este teste garante que o service falha cedo para snapshots
 * quebrados e nao deixa repository inconsistente parecer ausencia operacional
 * valida.</p>
 */
public class VersaoProducaoServiceCommunityContractTest {

    @Test
    public void getOuPersisteShouldReturnExistingCanonicalSentinel() throws Exception {

        VersaoProducaoInexistente versaoProducaoInexistente = new VersaoProducaoInexistente();
        RepositoryState repositoryState = new RepositoryState(List.of(versaoProducaoInexistente), null);
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        VersaoProducaoInexistente versaoProducaoInexistenteRetornada =
                versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        Assertions.assertSame(versaoProducaoInexistente, versaoProducaoInexistenteRetornada);
        Assertions.assertFalse(repositoryState.saveCalled);
        Assertions.assertFalse(repositoryState.deleteAllCalled);

    }

    @Test
    public void getOuPersisteShouldCreateAndValidateMissingCanonicalSentinel() throws Exception {

        VersaoProducaoInexistente versaoProducaoInexistenteSalva = new VersaoProducaoInexistente();
        RepositoryState repositoryState = new RepositoryState(List.of(), versaoProducaoInexistenteSalva);
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        VersaoProducaoInexistente versaoProducaoInexistenteRetornada =
                versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        Assertions.assertSame(versaoProducaoInexistenteSalva, versaoProducaoInexistenteRetornada);
        Assertions.assertTrue(repositoryState.saveCalled);

    }

    @Test
    public void getOuPersisteShouldRejectNullSnapshotBeforeSave() throws Exception {

        RepositoryState repositoryState = new RepositoryState(null, new VersaoProducaoInexistente());
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                versaoProducaoService::getOuPersisteVersaoProducaoInexistente);

        Assertions.assertEquals(
                "Production version sentinel snapshot is required.",
                illegalArgumentException.getMessage());
        Assertions.assertFalse(repositoryState.saveCalled);

    }

    @Test
    public void getOuPersisteShouldRejectNullSnapshotItemBeforeSave() throws Exception {

        RepositoryState repositoryState = new RepositoryState(asMutableList((VersaoProducaoInexistente) null), null);
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                versaoProducaoService::getOuPersisteVersaoProducaoInexistente);

        Assertions.assertEquals(
                "Production version sentinel snapshot item at index 0 is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getOuPersisteShouldRejectBrokenSavedSnapshot() throws Exception {

        RepositoryState repositoryState = new RepositoryState(List.of(), null);
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                versaoProducaoService::getOuPersisteVersaoProducaoInexistente);

        Assertions.assertEquals(
                "Saved production version sentinel is required.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getOuPersisteShouldRejectSavedSnapshotWithInvalidId() throws Exception {

        VersaoProducaoInexistente versaoProducaoInexistenteSalva =
                getVersaoProducaoInexistenteComIdForcado("BROKEN_SENTINEL");
        RepositoryState repositoryState = new RepositoryState(List.of(), versaoProducaoInexistenteSalva);
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                versaoProducaoService::getOuPersisteVersaoProducaoInexistente);

        Assertions.assertEquals(
                "Saved production version sentinel must have id DEFAULT_PRODUCTION_VERSION.",
                illegalArgumentException.getMessage());

    }

    @Test
    public void getOuPersisteShouldDeleteIncompatibleSnapshotRows() throws Exception {

        VersaoProducaoInexistente versaoProducaoInexistente = new VersaoProducaoInexistente();
        VersaoProducaoInexistente versaoProducaoInexistenteIncompativel =
                getVersaoProducaoInexistenteComIdForcado("BROKEN_SENTINEL");
        RepositoryState repositoryState =
                new RepositoryState(List.of(versaoProducaoInexistente, versaoProducaoInexistenteIncompativel), null);
        VersaoProducaoService versaoProducaoService = getVersaoProducaoService(repositoryState);

        VersaoProducaoInexistente versaoProducaoInexistenteRetornada =
                versaoProducaoService.getOuPersisteVersaoProducaoInexistente();

        Assertions.assertSame(versaoProducaoInexistente, versaoProducaoInexistenteRetornada);
        Assertions.assertTrue(repositoryState.deleteAllCalled);
        Assertions.assertEquals(List.of(versaoProducaoInexistenteIncompativel), repositoryState.deletedEntities);

    }

    private static VersaoProducaoService getVersaoProducaoService(
            RepositoryState repositoryState) throws Exception {

        VersaoProducaoService versaoProducaoService = new VersaoProducaoService();
        setField(
                versaoProducaoService,
                "versaoProducaoInexistenteRepository",
                getVersaoProducaoInexistenteRepository(repositoryState));
        return versaoProducaoService;

    }

    private static VersaoProducaoInexistenteRepository getVersaoProducaoInexistenteRepository(
            RepositoryState repositoryState) {

        return (VersaoProducaoInexistenteRepository) Proxy.newProxyInstance(
                VersaoProducaoInexistenteRepository.class.getClassLoader(),
                new Class<?>[]{VersaoProducaoInexistenteRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return repositoryState.findAllResult;
                    }
                    if ("save".equals(method.getName())) {
                        repositoryState.saveCalled = true;
                        return repositoryState.saveResult;
                    }
                    if ("deleteAll".equals(method.getName())) {
                        repositoryState.deleteAllCalled = true;
                        repositoryState.deletedEntities = getListFromIterable(args[0]);
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VersaoProducaoInexistenteRepository test double";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new AssertionError(
                            "Repository method should not be called by VersaoProducaoService test: "
                                    + method.getName());
                });

    }

    private static VersaoProducaoInexistente getVersaoProducaoInexistenteComIdForcado(
            String id) throws Exception {

        VersaoProducaoInexistente versaoProducaoInexistente = new VersaoProducaoInexistente();
        Field idField = VersaoProducao.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(versaoProducaoInexistente, id);
        return versaoProducaoInexistente;

    }

    private static List<VersaoProducaoInexistente> getListFromIterable(Object iterableObject) {

        List<VersaoProducaoInexistente> versoesProducaoInexistentes = new ArrayList<>();
        Iterable<?> iterable = (Iterable<?>) iterableObject;
        for (Object item : iterable) {
            versoesProducaoInexistentes.add((VersaoProducaoInexistente) item);
        }
        return versoesProducaoInexistentes;

    }

    @SafeVarargs
    private static List<VersaoProducaoInexistente> asMutableList(
            VersaoProducaoInexistente... versoesProducaoInexistentes) {

        List<VersaoProducaoInexistente> versoesProducaoInexistentesList = new ArrayList<>();
        for (VersaoProducaoInexistente versaoProducaoInexistente : versoesProducaoInexistentes) {
            versoesProducaoInexistentesList.add(versaoProducaoInexistente);
        }
        return versoesProducaoInexistentesList;

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

        private final List<VersaoProducaoInexistente> findAllResult;

        private final VersaoProducaoInexistente saveResult;

        private boolean saveCalled;

        private boolean deleteAllCalled;

        private List<VersaoProducaoInexistente> deletedEntities = List.of();

        private RepositoryState(
                List<VersaoProducaoInexistente> findAllResult,
                VersaoProducaoInexistente saveResult) {

            this.findAllResult = findAllResult;
            this.saveResult = saveResult;

        }

    }

}
