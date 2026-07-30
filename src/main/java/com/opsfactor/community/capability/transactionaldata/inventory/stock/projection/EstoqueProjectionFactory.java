package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedDataInterface;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDateImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMImpl;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.platform.calendar.Calendario;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory de projections de estoque transacional usadas no Community como
 * snapshot operacional por material/location/UOM.
 */
@Slf4j
@Component
public class EstoqueProjectionFactory {

    /**
     * Repository de estoque transacional. As queries retornam agregados
     * quantitativos por material/location/UOM, sem dimensoes Enterprise de lote,
     * validade ou aging.
     */
    @Autowired
    private EstoqueRepository estoqueRepository;


    public EstoqueProjectionLocationProduto getEstoqueProjection(
            LocalDateTime dataReferencia, Location location, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable UnidadeMedida unidadeMedidaPadraoParaNulos) {

        return getEstoqueProjectionLocationProduto(dataReferencia, Sets.newHashSet(location), produtos,
                unidadeMedidaProjection, clusterEParametrosProjection, unidadeMedidaPadraoParaNulos);

    }
    /**
     * Popula o projection de estoques para um conjunto de materiais e locations
     *
     * @param unidadeMedidaPadraoParaNulos parametro opcional, pois a extracao do estoque pode ser feita com unidade target qualquer
     * @return projection de estoque consolidado para as locations informadas
     */
    public EstoqueProjectionLocationProduto getEstoqueProjectionLocationProduto(
            LocalDateTime dataReferencia, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable UnidadeMedida unidadeMedidaPadraoParaNulos) {

        EstoqueProjectionLocationProduto estoqueProjection = EstoqueProjectionLocationProduto.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .locations(locations)
                .materiais(produtos)
                .mapaEstoquePorMaterialLocationUnidadeMedida(new HashMap<>())
                .unidadeMedidaPadraoParaNulos(unidadeMedidaPadraoParaNulos)
                .build();

        if (locations.isEmpty() || produtos.isEmpty()) return estoqueProjection;

        Collection<AggregatedByLocationMaterialUOM> estoqueAgregadoCollection =
                estoqueRepository.consolidatedStockQuantityByMaterialAndLocation(
                                dataReferencia,
                                locations,
                                produtos);

        for (AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM : estoqueAgregadoCollection) {
            UnidadeMedida unidadeMedida = aggregatedByLocationMaterialUOM.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadeMedidaPadraoParaNulos != null)
                        ? unidadeMedidaPadraoParaNulos
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            estoqueProjection.addEstoque(AggregatedByLocationMaterialUOMImpl.builder()
                    .material(aggregatedByLocationMaterialUOM.getMaterial())
                    .location(aggregatedByLocationMaterialUOM.getLocation())
                    .uom(unidadeMedida)
                    .totalQuantity(aggregatedByLocationMaterialUOM.getTotalQuantity())
                    .build());
        }

        return estoqueProjection;
    }

    /**
     * Popula o projection de estoques para um conjunto de materiais e locations para todos os períodos passados do calendário
     *
     * @param calendario calendario com os periodos passados considerados
     * @param locations locations consideradas na extracao
     * @param produtos materiais considerados na extracao
     * @param unidadeMedidaProjection projection de conversoes de UOM
     * @param unidadeMedidaPadraoParaNulos fallback para linhas sem UOM persistida
     * @return projection de estoque por data para as locations informadas
     */
    public EstoqueProjectionLocationProdutoData getEstoqueProjectionLocationProdutoPeriodosPassadosCalendario(
            Calendario calendario, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable UnidadeMedida unidadeMedidaPadraoParaNulos) {

        EstoqueProjectionLocationProdutoData estoqueProjection = EstoqueProjectionLocationProdutoData.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .locations(locations)
                .materiais(produtos)
                .calendario(calendario)
                .mapaEstoquePorMaterialLocationUnidadeMedidaPorData(new HashMap<>())
                .unidadeMedidaPadraoParaNulos(unidadeMedidaPadraoParaNulos)
                .build();

        if (locations.isEmpty() || produtos.isEmpty()) return estoqueProjection;

        Collection<AggregatedByLocationMaterialUOMDate> estoqueAgregadoCollection =
                estoqueRepository.consolidatedStockQuantityByMaterialAndLocationAndReferenceDate(
                                calendario.getDataHorarioInicial(),
                                calendario.getPrimeiraDataHorarioPeriodo(calendario.getPosicaoPeriodoPresente()),
                                locations,
                                produtos);

        for (AggregatedByLocationMaterialUOMDate aggregatedByLocationMaterialUOMDate : estoqueAgregadoCollection) {
            UnidadeMedida unidadeMedida = aggregatedByLocationMaterialUOMDate.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadeMedidaPadraoParaNulos != null)
                        ? unidadeMedidaPadraoParaNulos
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            estoqueProjection.addEstoque(AggregatedByLocationMaterialUOMDateImpl.builder()
                    .material(aggregatedByLocationMaterialUOMDate.getMaterial())
                    .location(aggregatedByLocationMaterialUOMDate.getLocation())
                    .uom(unidadeMedida)
                    .referenceDate(aggregatedByLocationMaterialUOMDate.getReferenceDate())
                    .totalQuantity(aggregatedByLocationMaterialUOMDate.getTotalQuantity())
                    .build());
        }

        return estoqueProjection;
    }

    /**
     * Popula o projection de estoque somente nos últimos dias dos períodos
     * históricos do calendário.
     *
     * <p>É o contrato apropriado para cálculos de cobertura por período, como
     * stockout de Demand Planning. Uma única query recebe o conjunto de datas
     * finais; não há leitura da faixa diária completa nem consulta por folha.</p>
     */
    public EstoqueProjectionLocationProdutoData getEstoqueProjectionLocationProdutoUltimosDiasPeriodosPassadosCalendario(
            Calendario calendario, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable UnidadeMedida unidadeMedidaPadraoParaNulos) {

        EstoqueProjectionLocationProdutoData estoqueProjection = EstoqueProjectionLocationProdutoData.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .locations(locations)
                .materiais(produtos)
                .calendario(calendario)
                .mapaEstoquePorMaterialLocationUnidadeMedidaPorData(new HashMap<>())
                .unidadeMedidaPadraoParaNulos(unidadeMedidaPadraoParaNulos)
                .build();

        if (locations.isEmpty() || produtos.isEmpty()) {
            return estoqueProjection;
        }

        Set<LocalDate> datasFinaisPeriodosHistoricos = new HashSet<>();
        for (int periodo = 0; periodo < calendario.getPosicaoPeriodoPresente(); periodo++) {
            datasFinaisPeriodosHistoricos.add(calendario.getUltimaDataPeriodo(periodo));
        }
        if (datasFinaisPeriodosHistoricos.isEmpty()) {
            return estoqueProjection;
        }

        Collection<AggregatedByLocationMaterialUOMDate> estoqueAgregadoCollection =
                estoqueRepository.consolidatedStockQuantityByMaterialAndLocationAndReferenceDates(
                                datasFinaisPeriodosHistoricos,
                                locations,
                                produtos);

        for (AggregatedByLocationMaterialUOMDate aggregatedByLocationMaterialUOMDate : estoqueAgregadoCollection) {
            UnidadeMedida unidadeMedida = aggregatedByLocationMaterialUOMDate.getUom();
            if (unidadeMedida == null) {
                unidadeMedida = (unidadeMedidaPadraoParaNulos != null)
                        ? unidadeMedidaPadraoParaNulos
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            estoqueProjection.addEstoque(AggregatedByLocationMaterialUOMDateImpl.builder()
                    .material(aggregatedByLocationMaterialUOMDate.getMaterial())
                    .location(aggregatedByLocationMaterialUOMDate.getLocation())
                    .uom(unidadeMedida)
                    .referenceDate(aggregatedByLocationMaterialUOMDate.getReferenceDate())
                    .totalQuantity(aggregatedByLocationMaterialUOMDate.getTotalQuantity())
                    .build());
        }

        return estoqueProjection;

    }

    /**
     * Popula o projection de estoques para um conjunto de locations
     *
     * @param dataReferencia data do snapshot de estoque
     * @param unidadeMedidaProjection projection de conversoes de UOM
     * @param clusterEParametrosProjection parametros globais do cluster
     * @param unidadeMedidaPadraoParaNulos fallback para linhas sem UOM persistida
     * @return projection de estoque consolidado para as locations do snapshot
     */
    public EstoqueProjectionLocationProduto getEstoqueProjectionLocationProduto(
            LocalDateTime dataReferencia,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable UnidadeMedida unidadeMedidaPadraoParaNulos) {

        Collection<AggregatedByLocationMaterialUOM> estoqueAgregadoCollection =
                estoqueRepository.consolidatedStockQuantityByMaterialAndLocation(
                                dataReferencia);

        Set<Produto> materiais = estoqueAgregadoCollection.stream().map(x -> x.getMaterial()).collect(Collectors.toSet());
        Set<Location> locations = estoqueAgregadoCollection.stream().map(x -> x.getLocation()).collect(Collectors.toSet());

        EstoqueProjectionLocationProduto estoqueProjection = EstoqueProjectionLocationProduto.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .locations(locations)
                .materiais(materiais)
                .mapaEstoquePorMaterialLocationUnidadeMedida(new HashMap<>())
                .unidadeMedidaPadraoParaNulos(unidadeMedidaPadraoParaNulos)
                .build();

        for (AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM : estoqueAgregadoCollection) {
            UnidadeMedida unidadeMedida = aggregatedByLocationMaterialUOM.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadeMedidaPadraoParaNulos != null)
                        ? unidadeMedidaPadraoParaNulos
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            estoqueProjection.addEstoque(AggregatedByLocationMaterialUOMImpl.builder()
                    .material(aggregatedByLocationMaterialUOM.getMaterial())
                    .location(aggregatedByLocationMaterialUOM.getLocation())
                    .uom(unidadeMedida)
                    .totalQuantity(aggregatedByLocationMaterialUOM.getTotalQuantity())
                    .build());
        }

        return estoqueProjection;
    }

    public EstoqueProjectionProduto getEstoqueProjectionProduto(
            LocalDateTime dataReferencia, Location location, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable UnidadeMedida unidadeMedidaPadraoParaNulos) {

        EstoqueProjectionProduto estoqueProjection = EstoqueProjectionProduto.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .locations(Sets.newHashSet(location))
                .materiais(produtos)
                .mapaEstoquePorMaterialUnidadeMedida(new HashMap<>())
                .unidadeMedidaPadraoParaNulos(unidadeMedidaPadraoParaNulos)
                .build();

        if (produtos.isEmpty()) return estoqueProjection;

        Collection<AggregatedByMaterialUOM> estoqueAgregadoCollection =
                estoqueRepository.consolidatedStockQuantityByMaterial(
                                dataReferencia,
                                location,
                                produtos);

        for (AggregatedByMaterialUOM aggregatedByMaterialUOM : estoqueAgregadoCollection) {
            UnidadeMedida unidadeMedida = aggregatedByMaterialUOM.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadeMedidaPadraoParaNulos != null)
                        ? unidadeMedidaPadraoParaNulos
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            estoqueProjection.addEstoque(AggregatedByMaterialUOMImpl.builder()
                    .material(aggregatedByMaterialUOM.getMaterial())
                    .uom(unidadeMedida)
                    .totalQuantity(aggregatedByMaterialUOM.getTotalQuantity())
                    .build());
        }

        return estoqueProjection;
    }

    /**
     * Valida agregados por location/material antes de popular o snapshot.
     */
    

    /**
     * Valida agregados por location/material/data antes de popular o snapshot.
     */
    

    /**
     * Valida agregados por material antes de popular o snapshot.
     */
    

    private void validaAgregadoLocationMaterialUom(
            AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM,
            String projectionDescription,
            int indice) {

        if (aggregatedByLocationMaterialUOM == null) {
            throw new IllegalStateException(
                    projectionDescription
                            + " repository returned null aggregate at index "
                            + indice
                            + ".");
        }
        validaMaterial(
                aggregatedByLocationMaterialUOM.getMaterial(),
                projectionDescription,
                indice);
        validaLocation(
                aggregatedByLocationMaterialUOM.getLocation(),
                projectionDescription,
                indice);
        validaQuantidadeAgregada(
                aggregatedByLocationMaterialUOM,
                projectionDescription,
                indice);

    }

    private void validaAgregadoLocationMaterialUomDate(
            AggregatedByLocationMaterialUOMDate aggregatedByLocationMaterialUOMDate,
            String projectionDescription,
            int indice) {

        if (aggregatedByLocationMaterialUOMDate == null) {
            throw new IllegalStateException(
                    projectionDescription
                            + " repository returned null aggregate at index "
                            + indice
                            + ".");
        }
        validaMaterial(
                aggregatedByLocationMaterialUOMDate.getMaterial(),
                projectionDescription,
                indice);
        validaLocation(
                aggregatedByLocationMaterialUOMDate.getLocation(),
                projectionDescription,
                indice);
        validaQuantidadeAgregada(
                aggregatedByLocationMaterialUOMDate,
                projectionDescription,
                indice);

    }

    private void validaMaterial(
            Produto material,
            String projectionDescription,
            int indice) {

        if (material == null || material.getId() == null || material.getId().isBlank()) {
            throw new IllegalStateException(
                    projectionDescription
                            + " repository returned aggregate without material id at index "
                            + indice
                            + ".");
        }

    }

    private void validaLocation(
            Location location,
            String projectionDescription,
            int indice) {

        if (location == null || location.getId() == null || location.getId().isBlank()) {
            throw new IllegalStateException(
                    projectionDescription
                            + " repository returned aggregate without location id at index "
                            + indice
                            + ".");
        }

    }

    /**
     * Valida a quantidade agregada sem exigir UOM.
     *
     * <p>UOM nula e fallback operacional conhecido da factory; quantidade nula
     * ou nao finita representa snapshot transacional quebrado e nao pode entrar
     * no estoque inicial Community.</p>
     */
    private void validaQuantidadeAgregada(
            AggregatedDataInterface aggregatedDataInterface,
            String projectionDescription,
            int indice) {

        if (aggregatedDataInterface.getTotalQuantity() == null
                || !Double.isFinite(aggregatedDataInterface.getTotalQuantity())) {
            throw new IllegalStateException(
                    projectionDescription
                            + " repository returned invalid aggregate quantity at index "
                            + indice
                            + ": "
                            + aggregatedDataInterface.getTotalQuantity()
                            + ".");
        }

    }

    /**
     * Extrai a UOM da chave agregada preservando nulo operacional.
     */
    @Nullable
    private String getUnidadeMedidaIdAgregada(
            AggregatedDataInterface aggregatedDataInterface,
            String projectionDescription,
            int indice) {

        UnidadeMedida unidadeMedida = aggregatedDataInterface.getUom();
        if (unidadeMedida == null) {
            return null;
        }
        if (unidadeMedida.getId() == null || unidadeMedida.getId().isBlank()) {
            throw new IllegalStateException(
                    projectionDescription
                            + " repository returned aggregate with uom without id at index "
                            + indice
                            + ".");
        }

        return unidadeMedida.getId();

    }

}
