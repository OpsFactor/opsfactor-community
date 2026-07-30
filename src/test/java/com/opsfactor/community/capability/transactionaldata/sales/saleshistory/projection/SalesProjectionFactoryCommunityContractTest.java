package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Contrato Community da factory de historico de vendas.
 *
 * <p>O Community extrai apenas sell-out. Sell-in e sales orders permanecem no
 * enum compartilhado para compatibilidade de payload/front, mas qualquer caller
 * que tente materializar essas fontes deve falhar antes de acessar repository
 * ou montar projection real. Por isso o teste chama os metodos com os demais
 * parametros nulos: se a validacao deixar passar, o teste quebra por NPE em vez
 * de esconder a brecha.</p>
 */
public class SalesProjectionFactoryCommunityContractTest {

    @Test
    public void allSalesProjectionFactoryMethodsShouldRejectEnterpriseSalesDocumentsBeforeRepositoryAccess() {

        SalesProjectionFactory salesProjectionFactory = new SalesProjectionFactory();

        for (Constantes.TipoDocumentoVenda tipoDocumentoVendaEnterprise : List.of(
                Constantes.TipoDocumentoVenda.SELLIN,
                Constantes.TipoDocumentoVenda.PEDIDO)) {

            for (Executable salesProjectionCall : getSalesProjectionCalls(
                    salesProjectionFactory,
                    tipoDocumentoVendaEnterprise)) {

                Assertions.assertThrows(
                        RequiresEnterpriseVersionException.class,
                        salesProjectionCall,
                        "Documento historico Enterprise deveria falhar antes de repository: " + tipoDocumentoVendaEnterprise);

            }
        }

    }

    @Test
    public void materialDataShouldAcceptEmptyRequiredFiltersAsOperationalNoopBeforeRepositoryAccess() throws Exception {

        SalesProjectionFactory salesProjectionFactory = new SalesProjectionFactory();

        Assertions.assertNotNull(
                salesProjectionFactory.getSalesProjectionMaterialData(
                        Constantes.TipoDocumentoVenda.SELLOUT,
                        criaCalendarioDiario(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        criaUnidadeMedidaProjectionComParametrosGlobais(),
                        criaClusterEParametrosProjectionComParametrosGlobais(),
                        null));

    }

    @Test
    public void firstLastSalesProjectionShouldPopulateMutableIndexSequentially() throws Exception {

        SalesProjectionFactory salesProjectionFactory = new SalesProjectionFactory();
        FirstLastRow firstLastRow = new FirstLastRow(
                new Produto("MAT"),
                new Location("LOC"),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));
        setField(
                salesProjectionFactory,
                "selloutRepository",
                criaSelloutRepositoryComFirstLastSequencial(firstLastRow));

        /*
         * O Set retornado pelo repository falha se alguem chamar parallelStream().
         * A projection first/last e mutavel e deve ser montada sequencialmente;
         * o paralelismo fica na rodada de clusters/projections, nao nesta borda
         * de snapshot compartilhado.
         */
        FirstLastSalesProjection firstLastSalesProjection =
                salesProjectionFactory.getFirstLastSalesProjectionLocationMaterial(
                        Constantes.TipoDocumentoVenda.SELLOUT,
                        criaCalendarioDiario());

        Assertions.assertEquals(
                firstLastRow.getFirstDateTime(),
                firstLastSalesProjection
                        .getFirstLastByMaterialLocation(
                                firstLastRow.getLocation(),
                                firstLastRow.getMaterial())
                        .orElseThrow()
                        .getFirstDateTime());
        Assertions.assertEquals(
                firstLastRow.getLastDateTime(),
                firstLastSalesProjection
                        .getFirstLastByLocation(firstLastRow.getLocation())
                        .orElseThrow()
                        .getLastDateTime());
        Assertions.assertEquals(
                firstLastRow.getLastDateTime(),
                firstLastSalesProjection
                        .getFirstLastByMaterial(firstLastRow.getMaterial())
                        .orElseThrow()
                        .getLastDateTime());

    }

    private static SelloutRepository criaSelloutRepositoryRetornandoNulo() {

        return criaSelloutRepositoryRetornando(
                "consolidatedSelloutByLocationMaterialUOMDayForMaterialLocationIds",
                null);

    }

    private static SelloutRepository criaSelloutRepositoryRetornandoItemNulo() {

        return criaSelloutRepositoryRetornando(
                "consolidatedSelloutByLocationMaterialUOMDayForMaterialLocationIds",
                Collections.singletonList(null));

    }

    private static SelloutRepository criaSelloutRepositoryRetornando(
            String methodName,
            Object methodReturnValue) {

        return (SelloutRepository) Proxy.newProxyInstance(
                SelloutRepository.class.getClassLoader(),
                new Class<?>[]{SelloutRepository.class},
                (proxy, method, args) -> {

                    if (method.getName().equals(methodName)) {
                        return methodReturnValue;
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no teste: " + method.getName());

                });

    }

    private static SelloutRepository criaSelloutRepositoryComFirstLastSequencial(
            FirstLastRow firstLastRow) {

        return (SelloutRepository) Proxy.newProxyInstance(
                SelloutRepository.class.getClassLoader(),
                new Class<?>[]{SelloutRepository.class},
                (proxy, method, args) -> {

                    return switch (method.getName()) {
                        case "findFirstLastSelloutPorMaterialLocation" ->
                                new SequentialOnlyList<FirstLastByMaterialLocation>(List.of(firstLastRow));
                        case "findFirstLastSelloutPorLocation" ->
                                new SequentialOnlyList<FirstLastByLocation>(List.of(firstLastRow));
                        case "findFirstLastSelloutPorMaterial" ->
                                new SequentialOnlyList<FirstLastByMaterial>(List.of(firstLastRow));
                        default -> throw new UnsupportedOperationException(
                                "Metodo nao esperado no teste: " + method.getName());
                    };

                });

    }

    private static SelloutRepository criaSelloutRepositoryComFirstLastMaterialLocationDuplicado(
            FirstLastByMaterialLocation primeiraLinhaFirstLast,
            FirstLastByMaterialLocation segundaLinhaFirstLast) {

        return (SelloutRepository) Proxy.newProxyInstance(
                SelloutRepository.class.getClassLoader(),
                new Class<?>[]{SelloutRepository.class},
                (proxy, method, args) -> {

                    if (method.getName().equals("findFirstLastSelloutPorMaterialLocation")) {
                        return List.of(
                                primeiraLinhaFirstLast,
                                segundaLinhaFirstLast);
                    }
                    throw new UnsupportedOperationException(
                            "Metodo nao esperado no teste: " + method.getName());

                });

    }

    private static ClusterEParametrosProjection criaClusterEParametrosProjectionComParametrosGlobais()
            throws Exception {

        ClusterEParametrosProjection clusterEParametrosProjection =
                new ClusterEParametrosProjection();
        setField(
                clusterEParametrosProjection,
                "parametrosGlobais",
                new ParametrosGlobais());

        return clusterEParametrosProjection;

    }

    private static UnidadeMedidaProjection criaUnidadeMedidaProjectionComParametrosGlobais()
            throws Exception {

        UnidadeMedidaProjection unidadeMedidaProjection = new UnidadeMedidaProjection();
        setField(
                unidadeMedidaProjection,
                "parametrosGlobais",
                new ParametrosGlobais());

        return unidadeMedidaProjection;

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        if (target instanceof SalesProjectionFactory
                && "selloutRepository".equals(fieldName)) {
            SelloutHistoricalSalesSource selloutHistoricalSalesSource =
                    new SelloutHistoricalSalesSource();
            Field selloutRepositoryField = SelloutHistoricalSalesSource.class
                    .getDeclaredField("selloutRepository");
            selloutRepositoryField.setAccessible(true);
            selloutRepositoryField.set(selloutHistoricalSalesSource, value);

            Field selloutHistoricalSalesSourceField = SalesProjectionFactory.class
                    .getDeclaredField("selloutHistoricalSalesSource");
            selloutHistoricalSalesSourceField.setAccessible(true);
            selloutHistoricalSalesSourceField.set(target, selloutHistoricalSalesSource);
            return;
        }

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(
                target,
                value);

    }

    @SuppressWarnings("DataFlowIssue")
    private static List<Executable> getSalesProjectionCalls(
            SalesProjectionFactory salesProjectionFactory,
            Constantes.TipoDocumentoVenda tipoDocumentoVendaEnterprise) {

        return List.of(
                () -> salesProjectionFactory.getSalesProjectionMaterialData(
                        tipoDocumentoVendaEnterprise,
                        null,
                        (Set<Location>) null,
                        null,
                        null,
                        null,
                        null),
                () -> salesProjectionFactory.getSalesProjectionLocationMaterialData(
                        tipoDocumentoVendaEnterprise,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                () -> salesProjectionFactory.getSalesProjectionLocationMaterialDataConsolidandoComModoPropagacaoDemanda(
                        tipoDocumentoVendaEnterprise,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                () -> salesProjectionFactory.getSalesProjectionMaterialData(
                        tipoDocumentoVendaEnterprise,
                        null,
                        (Location) null,
                        null,
                        null,
                        null,
                        null),
                () -> salesProjectionFactory.getSalesProjectionMaterial(
                        tipoDocumentoVendaEnterprise,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                () -> salesProjectionFactory.getSalesProjectionMaterialLocation(
                        tipoDocumentoVendaEnterprise,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                () -> salesProjectionFactory.getFirstLastSalesProjectionLocationMaterial(
                        tipoDocumentoVendaEnterprise,
                        null));

    }

    private static Calendario criaCalendarioAnual() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.ANUAL,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59));

    }

    private static Calendario criaCalendarioDiario() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                0,
                0,
                1,
                0);

    }

    private static class SequentialOnlyList<T> extends AbstractList<T> {

        private final List<T> delegate;

        private SequentialOnlyList(List<T> delegate) {

            this.delegate = delegate;

        }

        /**
         * Permite o fluxo sequencial normal da factory.
         */
        @Override
        public Iterator<T> iterator() {

            return delegate.iterator();

        }

        @Override
        public T get(int index) {

            return delegate.get(index);

        }

        @Override
        public int size() {

            return delegate.size();

        }

        /**
         * Trava regressao para parallelStream na montagem de snapshot mutavel.
         */
        @Override
        public Stream<T> parallelStream() {

            throw new AssertionError("First/last sales projection must be populated sequentially.");

        }

    }

    private static class FirstLastRow implements
            FirstLastByMaterialLocation,
            FirstLastByLocation,
            FirstLastByMaterial {

        private final Produto material;

        private final Location location;

        private final LocalDateTime firstDateTime;

        private final LocalDateTime lastDateTime;

        private FirstLastRow(
                Produto material,
                Location location,
                LocalDateTime firstDateTime,
                LocalDateTime lastDateTime) {

            this.material = material;
            this.location = location;
            this.firstDateTime = firstDateTime;
            this.lastDateTime = lastDateTime;

        }

        @Override
        public Produto getMaterial() {

            return material;

        }

        @Override
        public Location getLocation() {

            return location;

        }

        @Override
        public LocalDateTime getFirstDateTime() {

            return firstDateTime;

        }

        @Override
        public LocalDateTime getLastDateTime() {

            return lastDateTime;

        }

        @Override
        public Double getTotalQuantity() {

            return 0.0d;

        }

        @Override
        public UnidadeMedida getUom() {

            return null;

        }

    }

}
