package com.opsfactor.community.capability.masterdata.product.material.facade;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Contratos defensivos da borda DTO Community de materiais.
 *
 * <p>O service alimenta seletores e planning books do front Community. Por
 * isso snapshots nulos ou com itens quebrados devem falhar como erro
 * estrutural de repository/projection, antes de mappers transformarem a falha
 * em lista vazia, `NullPointerException` ou status calculado sobre material
 * sem chave.</p>
 */
class MaterialDtoServiceCommunityTest {

    @Test
    void getMaterialDTOListShouldRejectNullActiveMaterialSnapshotBeforeMapper() {

        ProdutoRepositoryStub produtoRepositoryStub = new ProdutoRepositoryStub();
        produtoRepositoryStub.customFindProdutosAtivosReturn = null;
        MaterialDtoService materialDtoService =
                getMaterialDtoServiceComProdutoRepository(produtoRepositoryStub);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                materialDtoService::getMaterialDTOList);

        Assertions.assertEquals(
                "Active material repository snapshot is required.",
                illegalStateException.getMessage());

    }

    @Test
    void getMaterialDTOListShouldRejectBrokenActiveMaterialSnapshotBeforeMapper() {

        ProdutoRepositoryStub produtoRepositoryStub = new ProdutoRepositoryStub();
        produtoRepositoryStub.customFindProdutosAtivosReturn =
                new ArrayList<>(List.of(new Produto("MAT_01")));
        produtoRepositoryStub.customFindProdutosAtivosReturn.add(null);
        MaterialDtoService materialDtoService =
                getMaterialDtoServiceComProdutoRepository(produtoRepositoryStub);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                materialDtoService::getMaterialDTOList);

        Assertions.assertTrue(
                illegalStateException.getMessage().startsWith(
                        "Active material repository snapshot item at index "));
        Assertions.assertTrue(
                illegalStateException.getMessage().endsWith(" is required."));

    }

    @Test
    void getMaterialDTOListShouldRejectDuplicatedActiveMaterialIdBeforeMapper() {

        ProdutoRepositoryStub produtoRepositoryStub = new ProdutoRepositoryStub();
        produtoRepositoryStub.customFindProdutosAtivosReturn =
                List.of(
                        new Produto("MAT_01"),
                        new Produto("MAT_01"));
        MaterialDtoService materialDtoService =
                getMaterialDtoServiceComProdutoRepository(produtoRepositoryStub);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                materialDtoService::getMaterialDTOList);

        Assertions.assertEquals(
                "Active material repository snapshot has duplicated material id MAT_01.",
                illegalStateException.getMessage());

    }

    @Test
    void getMaterialClusterDTOListShouldRejectBrokenClusterSnapshotBeforeMapper() {

        ClusterProdutos clusterMateriaisSemId = new ClusterProdutos();
        clusterMateriaisSemId.setDescricao("Cluster sem id");

        ClusterProdutosRepositoryStub clusterProdutosRepositoryStub =
                new ClusterProdutosRepositoryStub();
        clusterProdutosRepositoryStub.findAllReturn =
                new ArrayList<>(List.of(clusterMateriaisSemId));
        MaterialDtoService materialDtoService =
                getMaterialDtoServiceComClusterRepository(clusterProdutosRepositoryStub);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                materialDtoService::getMaterialClusterDTOList);

        Assertions.assertEquals(
                "Material cluster repository snapshot item at index 0 requires id.",
                illegalStateException.getMessage());

    }

    @Test
    void getMaterialDTOListFromMaterialClusterIdShouldRejectMissingInputsBeforeProjectionUse() {

        MaterialDtoService materialDtoService = new MaterialDtoService();

        IllegalArgumentException missingIdException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> materialDtoService.getMaterialDTOListFromMaterialClusterId(null));

        Assertions.assertEquals(
                "Material cluster id is required to list Community material DTOs.",
                missingIdException.getMessage());

        setPrivateField(
                materialDtoService,
                "clusterEParametrosProjectionFactory",
                new NullClusterEParametrosProjectionFactory());

        IllegalStateException missingProjectionException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> materialDtoService.getMaterialDTOListFromMaterialClusterId(10L));

        Assertions.assertEquals(
                "Cluster and parameters projection snapshot is required to list Community material DTOs.",
                missingProjectionException.getMessage());

    }

    private static MaterialDtoService getMaterialDtoServiceComProdutoRepository(
            ProdutoRepositoryStub produtoRepositoryStub) {

        MaterialDtoService materialDtoService = new MaterialDtoService();
        setPrivateField(
                materialDtoService,
                "produtoRepository",
                produtoRepositoryStub.getRepository());
        return materialDtoService;

    }

    private static MaterialDtoService getMaterialDtoServiceComClusterRepository(
            ClusterProdutosRepositoryStub clusterProdutosRepositoryStub) {

        MaterialDtoService materialDtoService = new MaterialDtoService();
        setPrivateField(
                materialDtoService,
                "clusterProdutosRepository",
                clusterProdutosRepositoryStub.getRepository());
        return materialDtoService;

    }

    private static void setPrivateField(
            MaterialDtoService materialDtoService,
            String fieldName,
            Object value) {

        try {
            Field field = MaterialDtoService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(materialDtoService, value);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(
                    "Unable to configure MaterialDtoService test field " + fieldName,
                    reflectiveOperationException);
        }

    }

    /**
     * Stub minimo do repository de materiais.
     */
    private static class ProdutoRepositoryStub {

        private List<Produto> customFindProdutosAtivosReturn;

        private ProdutoRepository getRepository() {

            return (ProdutoRepository) Proxy.newProxyInstance(
                    ProdutoRepository.class.getClassLoader(),
                    new Class[]{ProdutoRepository.class},
                    this::invoke);

        }

        private Object invoke(Object proxy, Method method, Object[] args) {

            return switch (method.getName()) {
                case "customFindProdutosAtivos" -> customFindProdutosAtivosReturn;
                case "toString" -> "ProdutoRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(
                        "Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

    /**
     * Stub minimo do repository de clusters de material.
     */
    private static class ClusterProdutosRepositoryStub {

        private List<ClusterProdutos> findAllReturn;

        private ClusterProdutosRepository getRepository() {

            return (ClusterProdutosRepository) Proxy.newProxyInstance(
                    ClusterProdutosRepository.class.getClassLoader(),
                    new Class[]{ClusterProdutosRepository.class},
                    this::invoke);

        }

        private Object invoke(Object proxy, Method method, Object[] args) {

            return switch (method.getName()) {
                case "findAll" -> findAllReturn;
                case "toString" -> "ClusterProdutosRepositoryStub";
                case "hashCode" -> System.identityHashCode(this);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(
                        "Metodo nao suportado no stub: " + method.getName());
            };

        }

    }

    /**
     * Factory de teste que representa cache/projection estrutural ausente.
     */
    private static class NullClusterEParametrosProjectionFactory extends ClusterEParametrosProjectionFactory {

        @Override
        public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

            return null;

        }

    }

}
