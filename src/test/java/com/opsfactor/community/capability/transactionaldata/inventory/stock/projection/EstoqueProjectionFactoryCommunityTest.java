package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedDataInterface;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Guardas de contrato do factory Community de projection de estoque.
 */
class EstoqueProjectionFactoryCommunityTest {

    @Test
    void locationMaterialDateProjectionForPeriodEndsShouldQueryOnlyHistoricalPeriodEndDates() throws Exception {

        EstoqueProjectionFactory estoqueProjectionFactory = criaEstoqueProjectionFactory();
        EstoqueRepository estoqueRepository = Mockito.mock(EstoqueRepository.class);
        Mockito.when(estoqueRepository.consolidatedStockQuantityByMaterialAndLocationAndReferenceDates(
                        Mockito.anyCollection(),
                        Mockito.anyCollection(),
                        Mockito.anyCollection()))
                .thenReturn(List.of());
        setPrivateField(estoqueProjectionFactory, "estoqueRepository", estoqueRepository);

        Calendario calendario = Mockito.mock(Calendario.class);
        Mockito.when(calendario.getPosicaoPeriodoPresente()).thenReturn(3);
        Mockito.when(calendario.getUltimaDataPeriodo(0)).thenReturn(LocalDate.of(2026, 1, 7));
        Mockito.when(calendario.getUltimaDataPeriodo(1)).thenReturn(LocalDate.of(2026, 1, 14));
        Mockito.when(calendario.getUltimaDataPeriodo(2)).thenReturn(LocalDate.of(2026, 1, 21));

        estoqueProjectionFactory.getEstoqueProjectionLocationProdutoUltimosDiasPeriodosPassadosCalendario(
                calendario,
                Set.of(new Location("LOC")),
                Set.of(new Produto("MAT")),
                Mockito.mock(UnidadeMedidaProjection.class),
                Mockito.mock(ClusterEParametrosProjection.class),
                null);

        ArgumentCaptor<Collection<LocalDate>> datasReferenciaCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(estoqueRepository)
                .consolidatedStockQuantityByMaterialAndLocationAndReferenceDates(
                        datasReferenciaCaptor.capture(),
                        Mockito.anyCollection(),
                        Mockito.anyCollection());
        Assertions.assertEquals(
                Set.of(
                        LocalDate.of(2026, 1, 7),
                        LocalDate.of(2026, 1, 14),
                        LocalDate.of(2026, 1, 21)),
                Set.copyOf(datasReferenciaCaptor.getValue()));
        Mockito.verifyNoMoreInteractions(estoqueRepository);

    }

    @Test
    void nullableUomFallbackContractShouldBeDeclaredExplicitly() throws Exception {

        Method getUnidadeMedidaIdAgregadaMethod =
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getUnidadeMedidaIdAgregada",
                        AggregatedDataInterface.class,
                        String.class,
                        int.class);

        Assertions.assertTrue(
                getUnidadeMedidaIdAgregadaMethod.isAnnotationPresent(Nullable.class),
                "getUnidadeMedidaIdAgregada deve declarar retorno @Nullable para UOM agregada ausente.");
        assertParameterNullable(
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getEstoqueProjection",
                        LocalDateTime.class,
                        Location.class,
                        Set.class,
                        UnidadeMedidaProjection.class,
                        ClusterEParametrosProjection.class,
                        UnidadeMedida.class),
                5,
                "getEstoqueProjection deve declarar fallback de UOM @Nullable.");
        assertParameterNullable(
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getEstoqueProjectionLocationProduto",
                        LocalDateTime.class,
                        Set.class,
                        Set.class,
                        UnidadeMedidaProjection.class,
                        ClusterEParametrosProjection.class,
                        UnidadeMedida.class),
                5,
                "getEstoqueProjectionLocationProduto filtrado deve declarar fallback de UOM @Nullable.");
        assertParameterNullable(
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getEstoqueProjectionLocationProdutoPeriodosPassadosCalendario",
                        Calendario.class,
                        Set.class,
                        Set.class,
                        UnidadeMedidaProjection.class,
                        ClusterEParametrosProjection.class,
                        UnidadeMedida.class),
                5,
                "getEstoqueProjectionLocationProdutoPeriodosPassadosCalendario deve declarar fallback de UOM @Nullable.");
        assertParameterNullable(
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getEstoqueProjectionLocationProdutoUltimosDiasPeriodosPassadosCalendario",
                        Calendario.class,
                        Set.class,
                        Set.class,
                        UnidadeMedidaProjection.class,
                        ClusterEParametrosProjection.class,
                        UnidadeMedida.class),
                5,
                "getEstoqueProjectionLocationProdutoUltimosDiasPeriodosPassadosCalendario deve declarar fallback de UOM @Nullable.");
        assertParameterNullable(
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getEstoqueProjectionLocationProduto",
                        LocalDateTime.class,
                        UnidadeMedidaProjection.class,
                        ClusterEParametrosProjection.class,
                        UnidadeMedida.class),
                3,
                "getEstoqueProjectionLocationProduto completo deve declarar fallback de UOM @Nullable.");
        assertParameterNullable(
                EstoqueProjectionFactory.class.getDeclaredMethod(
                        "getEstoqueProjectionProduto",
                        LocalDateTime.class,
                        Location.class,
                        Set.class,
                        UnidadeMedidaProjection.class,
                        ClusterEParametrosProjection.class,
                        UnidadeMedida.class),
                5,
                "getEstoqueProjectionProduto deve declarar fallback de UOM @Nullable.");

    }

    @Test
    void estoqueProjectionFactoryShouldNotKeepLegacyCalendarImplementationCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path estoqueProjectionFactorySourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/transactionaldata/inventory/stock/projection/EstoqueProjectionFactory.java");
        String estoqueProjectionFactorySource = Files.readString(
                estoqueProjectionFactorySourcePath,
                StandardCharsets.UTF_8);

        /*
         * Projections de estoque de snapshot nao carregam calendario artificial.
         * Somente o metodo por periodos passados recebe e persiste o calendario
         * real informado pelo caller. Alternativas comentadas de calendario
         * antigo deixam essa diferenca ambigua para o recorte Community.
         */
        Assertions.assertFalse(
                estoqueProjectionFactorySource.contains("//        Calendario calendario = Calendario.criaCalendario(")
                        || estoqueProjectionFactorySource.contains("//                .calendario(calendario)"),
                "EstoqueProjectionFactory nao deve manter calendario legado comentado.");

    }

    @Test
    void estoqueRepositoryShouldExposeReferenceDateInLocationMaterialDateAggregate() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path estoqueRepositorySourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/transactionaldata/inventory/stock/repository/EstoqueRepository.java");
        String estoqueRepositorySource = Files.readString(
                estoqueRepositorySourcePath,
                StandardCharsets.UTF_8);

        /*
         * A projection por data e usada por fluxos que precisam recuperar o
         * estoque historico na mesma granularidade diaria do calendario. O
         * alias referenceDate deve continuar explicito e normalizado para
         * LocalDate; agrupar pela data sem seleciona-la faz a projection falhar
         * apenas em runtime.
         */
        Assertions.assertTrue(
                estoqueRepositorySource.contains(
                        "DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia) AS referenceDate"),
                "EstoqueRepository deve selecionar referenceDate no agregado por data.");
        Assertions.assertTrue(
                estoqueRepositorySource.contains(
                        "GROUP BY est.estoqueCompositeKey.produto, est.estoqueCompositeKey.location, DATA_SEM_HORARIO(est.estoqueCompositeKey.dataReferencia), est.unidadeMedida"),
                "EstoqueRepository deve agrupar pela mesma data normalizada entregue como referenceDate.");

    }

    private static void assertParameterNullable(
            Executable executable,
            int parameterIndex,
            String errorMessage) {

        boolean parameterIsNullable = Arrays.stream(executable.getParameterAnnotations()[parameterIndex])
                .anyMatch(annotation -> annotation.annotationType().equals(Nullable.class));

        Assertions.assertTrue(parameterIsNullable, errorMessage);

    }

    private EstoqueProjectionFactory criaEstoqueProjectionFactory() {

        return new EstoqueProjectionFactory();

    }

    private Calendario getCalendarioTeste() {

        return Calendario.criaCalendarioPeriodosFuturosDeDatas(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 3, 0, 0));

    }

    private void setPrivateField(
            Object target,
            String fieldName,
            Object value) throws Exception {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

}
