package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.ConversaoUnidadeRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contrato Community da factory de projection de unidades de medida.
 *
 * <p>A projection de UOM e compartilhada por Demand Planning, Supply Planning
 * e Planning Book. Este teste garante que snapshots quebrados de repositories
 * falham antes de popular os mapas de conversao, enquanto listas vazias seguem
 * ausencia operacional valida.</p>
 */
public class UnidadeMedidaProjectionFactoryTest {

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldLoadGlobalConversionAndInverse() throws Exception {

        UnidadeMedida unidadeMedidaKg = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaUn = new UnidadeMedida("UN");
        ConversaoUnidade conversaoUnidade =
                getConversaoUnidade(unidadeMedidaKg, unidadeMedidaUn, 2.0d);

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(unidadeMedidaKg, unidadeMedidaUn),
                        List.of(conversaoUnidade),
                        List.of(),
                        new ParametrosGlobais());

        UnidadeMedidaProjection unidadeMedidaProjection =
                unidadeMedidaProjectionFactory.getUnidadeMedidaProjectionComConversoes();

        Assertions.assertEquals(
                2.0d,
                unidadeMedidaProjection.getOptionalConversaoPadraoParaUnidadeDestino(
                                unidadeMedidaKg,
                                unidadeMedidaUn)
                        .orElseThrow(),
                0.0001d);
        Assertions.assertEquals(
                0.5d,
                unidadeMedidaProjection.getOptionalConversaoPadraoParaUnidadeDestino(
                                unidadeMedidaUn,
                                unidadeMedidaKg)
                        .orElseThrow(),
                0.0001d);

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectNullUnitCatalogBeforeConversionLoops() throws Exception {

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        null,
                        List.of(),
                        List.of(),
                        new ParametrosGlobais());

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Unit of Measure repository returned null collection.",
                illegalStateException.getMessage());

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectNullGlobalConversionItemBeforeConversionLoops() throws Exception {

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(new UnidadeMedida("KG"), new UnidadeMedida("UN")),
                        listWithNullConversaoUnidade(),
                        List.of(),
                        new ParametrosGlobais());

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Global UOM conversion repository returned null item at index 0.",
                illegalStateException.getMessage());

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectMaterialConversionWithUnknownUomBeforeProjectionLookup() throws Exception {

        UnidadeMedida unidadeMedidaKg = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaUn = new UnidadeMedida("UN");
        UnidadeMedida unidadeMedidaCx = new UnidadeMedida("CX");
        ConversaoUnidadeProduto conversaoUnidadeProduto =
                getConversaoUnidadeProduto(
                        new Produto("MAT_01"),
                        unidadeMedidaKg,
                        unidadeMedidaCx,
                        12.0d);

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(unidadeMedidaKg, unidadeMedidaUn),
                        List.of(),
                        List.of(conversaoUnidadeProduto),
                        new ParametrosGlobais());

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Material-level UOM conversion target UOM CX is not present in Unit of Measure catalog.",
                illegalStateException.getMessage());

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectNullGlobalParametersBeforeProjectionUse() throws Exception {

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(new UnidadeMedida("KG")),
                        List.of(),
                        List.of(),
                        null);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Global parameters service returned null for Unit of Measure projection.",
                illegalStateException.getMessage());

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectNonFiniteGlobalConversionBeforePublishingProjection()
            throws Exception {

        UnidadeMedida unidadeMedidaKg = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaUn = new UnidadeMedida("UN");
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(unidadeMedidaKg, unidadeMedidaUn),
                        List.of(getConversaoUnidade(unidadeMedidaKg, unidadeMedidaUn, Double.NaN)),
                        List.of(),
                        new ParametrosGlobais());

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Global UOM conversion must be finite and positive from KG to UN",
                unitOfMeasureConversionException.getMessage());

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectNonFiniteMaterialConversionBeforePublishingProjection()
            throws Exception {

        UnidadeMedida unidadeMedidaKg = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaUn = new UnidadeMedida("UN");
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(unidadeMedidaKg, unidadeMedidaUn),
                        List.of(),
                        List.of(getConversaoUnidadeProduto(
                                new Produto("MAT_01"),
                                unidadeMedidaKg,
                                unidadeMedidaUn,
                                Double.POSITIVE_INFINITY)),
                        new ParametrosGlobais());

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Material-level UOM conversion must be finite and positive from KG to UN for material MAT_01",
                unitOfMeasureConversionException.getMessage());

    }

    @Test
    public void getUnidadeMedidaProjectionComConversoesShouldRejectConflictingLegacyAndCanonicalGlobalRatioBeforePublishingProjection()
            throws Exception {

        UnidadeMedida unidadeMedidaKg = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaUn = new UnidadeMedida("UN");
        ConversaoUnidade conversaoUnidade = getConversaoUnidade(
                unidadeMedidaKg,
                unidadeMedidaUn,
                2.0);
        conversaoUnidade.setQuantidadeUnidadeOrigem(1.0);
        conversaoUnidade.setQuantidadeUnidadeDestino(3.0);
        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory =
                getUnidadeMedidaProjectionFactory(
                        List.of(unidadeMedidaKg, unidadeMedidaUn),
                        List.of(conversaoUnidade),
                        List.of(),
                        new ParametrosGlobais());

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                unidadeMedidaProjectionFactory::getUnidadeMedidaProjectionComConversoes);

        Assertions.assertEquals(
                "Global UOM conversion has conflicting deprecated and canonical quantity ratios from KG to UN",
                unitOfMeasureConversionException.getMessage());

    }

    @Test
    public void unidadeMedidaProjectionShouldRejectNonFiniteDirectConversionBeforeReturningOptional() {

        UnidadeMedida unidadeMedidaKg = new UnidadeMedida("KG");
        UnidadeMedida unidadeMedidaUn = new UnidadeMedida("UN");
        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();

        /*
         * A factory produtiva rejeita conversoes nao positivas. Este teste
         * protege chamadas diretas ou fixtures que montem a projection em
         * memoria e tentem retornar NaN/infinito como fator valido.
         */
        unidadeMedidaProjection.mapaConversoesPadrao
                .computeIfAbsent(unidadeMedidaKg, unidadeMedida -> new ConcurrentHashMap<>())
                .put(unidadeMedidaUn, Double.NaN);

        UnitOfMeasureConversionException unitOfMeasureConversionException = Assertions.assertThrows(
                UnitOfMeasureConversionException.class,
                () -> unidadeMedidaProjection.getOptionalConversaoPadraoParaUnidadeDestino(
                        unidadeMedidaKg,
                        unidadeMedidaUn));

        Assertions.assertEquals(
                "Invalid UOM conversion from KG to UN",
                unitOfMeasureConversionException.getMessage());

    }

    private static UnidadeMedidaProjectionFactory getUnidadeMedidaProjectionFactory(
            List<UnidadeMedida> unidadeMedidaList,
            List<ConversaoUnidade> conversoesUnidadePadrao,
            List<ConversaoUnidadeProduto> conversoesUnidadePorProduto,
            ParametrosGlobais parametrosGlobais) throws Exception {

        UnidadeMedidaProjectionFactory unidadeMedidaProjectionFactory = new UnidadeMedidaProjectionFactory();
        setPrivateField(
                unidadeMedidaProjectionFactory,
                "unidadeMedidaRepository",
                getUnidadeMedidaRepositoryProxy(unidadeMedidaList));
        setPrivateField(
                unidadeMedidaProjectionFactory,
                "conversaoUnidadeRepository",
                getConversaoUnidadeRepositoryProxy(conversoesUnidadePadrao));
        setPrivateField(
                unidadeMedidaProjectionFactory,
                "conversaoUnidadeProdutoRepository",
                getConversaoUnidadeProdutoRepositoryProxy(conversoesUnidadePorProduto));
        setPrivateField(
                unidadeMedidaProjectionFactory,
                "parametrosGlobaisService",
                new TestParametrosGlobaisService(parametrosGlobais));

        return unidadeMedidaProjectionFactory;

    }

    private static UnidadeMedidaRepository getUnidadeMedidaRepositoryProxy(
            List<UnidadeMedida> unidadeMedidaList) {

        return (UnidadeMedidaRepository) Proxy.newProxyInstance(
                UnidadeMedidaRepository.class.getClassLoader(),
                new Class<?>[]{UnidadeMedidaRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return getObjectMethodResult(proxy, method.getName(), args);
                    }
                    if ("findAll".equals(method.getName()) && method.getParameterCount() == 0) {
                        return unidadeMedidaList;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static ConversaoUnidadeRepository getConversaoUnidadeRepositoryProxy(
            List<ConversaoUnidade> conversoesUnidadePadrao) {

        return (ConversaoUnidadeRepository) Proxy.newProxyInstance(
                ConversaoUnidadeRepository.class.getClassLoader(),
                new Class<?>[]{ConversaoUnidadeRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return getObjectMethodResult(proxy, method.getName(), args);
                    }
                    if ("customFindAllJoinUnidades".equals(method.getName())) {
                        return conversoesUnidadePadrao;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static ConversaoUnidadeProdutoRepository getConversaoUnidadeProdutoRepositoryProxy(
            List<ConversaoUnidadeProduto> conversoesUnidadePorProduto) {

        return (ConversaoUnidadeProdutoRepository) Proxy.newProxyInstance(
                ConversaoUnidadeProdutoRepository.class.getClassLoader(),
                new Class<?>[]{ConversaoUnidadeProdutoRepository.class},
                (proxy, method, args) -> {

                    if (method.getDeclaringClass().equals(Object.class)) {
                        return getObjectMethodResult(proxy, method.getName(), args);
                    }
                    if ("customFindAllJoinProdutoEUnidades".equals(method.getName())) {
                        return conversoesUnidadePorProduto;
                    }

                    throw new AssertionError("Metodo inesperado no teste: " + method.getName());

                });

    }

    private static Object getObjectMethodResult(
            Object proxy,
            String methodName,
            Object[] args) {

        return switch (methodName) {
            case "toString" -> "RepositoryProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new AssertionError("Metodo Object inesperado: " + methodName);
        };

    }

    private static ConversaoUnidade getConversaoUnidade(
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaDestino,
            Double conversao) {

        ConversaoUnidade conversaoUnidade = new ConversaoUnidade(
                new ConversaoUnidade.ConversaoUnidadeCompositeKey(
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));
        conversaoUnidade.setQuantidadeUnidadeDestinoPorUnidadeOrigem(conversao);
        return conversaoUnidade;

    }

    private static ConversaoUnidadeProduto getConversaoUnidadeProduto(
            Produto produto,
            UnidadeMedida unidadeMedidaOrigem,
            UnidadeMedida unidadeMedidaDestino,
            Double conversao) {

        ConversaoUnidadeProduto conversaoUnidadeProduto = new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        produto,
                        unidadeMedidaOrigem,
                        unidadeMedidaDestino));
        conversaoUnidadeProduto.setQuantidadeUnidadeDestinoPorUnidadeOrigem(conversao);
        return conversaoUnidadeProduto;

    }

    private static List<ConversaoUnidade> listWithNullConversaoUnidade() {

        return java.util.Collections.singletonList(null);

    }

    private static void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

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

}
