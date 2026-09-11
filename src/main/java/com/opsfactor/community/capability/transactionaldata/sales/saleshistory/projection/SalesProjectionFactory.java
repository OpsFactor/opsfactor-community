package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOM;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDate;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDateImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMDateImpl;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByMaterialUOMImpl;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.calendar.IntervaloExtracaoCalendario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory Community de projections de vendas historicas.
 *
 * <p>A implementacao aberta materializa apenas sell-out. O nome `Sales` foi
 * adotado para reduzir a dependencia conceitual do nome fisico legado
 * `Sellout`. A edição Enterprise estende esta classe para acrescentar fontes
 * privadas de forma explícita, sem registro genérico de componentes.</p>
 */
@Slf4j
@Component
public class SalesProjectionFactory {

    /**
     * Fonte documental que pertence ao contrato Community.
     *
     * <p>Sell-in e pedidos fechados são declarados como campos individuais no
     * overlay Enterprise. A seleção explícita conserva a legibilidade do
     * legado e evita que a edição Community descubra capacidades privadas por
     * lista ou registry genérico.</p>
     */
    @Autowired
    private SelloutHistoricalSalesSource selloutHistoricalSalesSource;

    /**
     * Community publica apenas historico sell-out. Sell-in e sales orders ficam
     * como valores conhecidos do contrato para compatibilidade de configuracao,
     * mas a extracao dos dados pertence ao OpsFactor Enterprise.
     *
     * <p>Documento nulo e payload invalido, nao capability Enterprise. Essa
     * distincao e importante para callers de configuracao/execucao falharem
     * antes de consultar repositories e sem sugerir que uma versao Enterprise
     * resolveria um payload incompleto.</p>
     */
    protected HistoricalSalesSource getHistoricalSalesSource(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda) {

        return switch (tipoDocumentoVenda) {
            case SELLOUT -> selloutHistoricalSalesSource;
            case SELLIN, PEDIDO -> throw new RequiresEnterpriseVersionException(
                    "Historical Sales by " + tipoDocumentoVenda);
        };

    }

    /**
     * Popula o projection de sell-out para um conjunto de locations.
     *
     * @param calendario calendario operacional usado para bucketizar o historico extraido
     * @param locations locations internas consideradas na extracao de sell-out
     * @param produtos materiais considerados na extracao de sell-out
     * @param unidadeMedidaProjection projection de conversao usada para normalizar unidade quando necessario
     * @param unidadePadrao parametro opcional, pois a extracao da venda pode ser feita com unidade target qualquer
     * @return projection agregado por material/data para as locations informadas
     */
    public SalesProjectionMaterialData getSalesProjectionMaterialData(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            CalendarioSimples calendario, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadePadrao) {

        HistoricalSalesSource historicalSalesSource = getHistoricalSalesSource(tipoDocumentoVenda);

        SalesProjectionMaterialData salesProjection = SalesProjectionMaterialData.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .calendario(calendario)
                .locations(locations)
                .materiais(produtos)
                .unidadeMedidaPadraoParaNulos(clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal())
                .mapaVendasAgregadasPorPeriodo(new HashMap<>())
                .build();

        if (locations.isEmpty() || produtos.isEmpty()) return salesProjection;

        Set<String> locationIds = locations.stream().map(Location::getId).collect(Collectors.toSet());
        Set<String> materialIds = produtos.stream().map(Produto::getId).collect(Collectors.toSet());

        Collection<AggregatedByMaterialUOMDate> listaSelloutAgregado =
                historicalSalesSource.getAggregatedByMaterialUomDate(
                        calendario,
                        locationIds,
                        materialIds);

        for (AggregatedByMaterialUOMDate aggregatedByMaterialUOMDate : listaSelloutAgregado) {
            UnidadeMedida unidadeMedida = aggregatedByMaterialUOMDate.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadePadrao != null)
                        ? unidadePadrao
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            salesProjection.addSalesAgregado(AggregatedByMaterialUOMDateImpl.builder()
                    .material(aggregatedByMaterialUOMDate.getMaterial())
                    .uom(unidadeMedida)
                    // A query anual usa agregados mensais. Todos os meses do
                    // mesmo ano entram no mesmo fechamento; a projection soma
                    // suas contribuições (300 + 200 = 500), sem alterar o ano seguinte.
                    .referenceDate(calendario.getTamanhoBucket() == Constantes.TamanhoBucket.ANUAL
                            ? calendario.getUltimaDataPeriodo(calendario.getPosicaoPeriodo(
                                    aggregatedByMaterialUOMDate.getReferenceDate()))
                            : aggregatedByMaterialUOMDate.getReferenceDate())
                    .totalQuantity(aggregatedByMaterialUOMDate.getTotalQuantity())
                    .build());
        }

        return salesProjection;
    }

    public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialDataVazio(
            Calendario calendario, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {


        return SalesProjectionLocationMaterialData.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .calendario(calendario)
                .locations(locations)
                .materiais(produtos)
                .unidadeMedidaPadraoParaNulos(clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal())
                .mapaVendasAgregadasPorPeriodo(new HashMap<>())
                .build();

    }

    public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialData(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            Calendario calendario,
            @Nullable Set<Location> locations,
            @Nullable Set<Produto> produtos,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadePadrao) {

        HistoricalSalesSource historicalSalesSource = getHistoricalSalesSource(tipoDocumentoVenda);

        SalesProjectionLocationMaterialData salesProjection = SalesProjectionLocationMaterialData.builder()
                .conversaoUnidadeMedidaProjection(unidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .calendario(calendario)
                .locations(locations)
                .materiais(produtos)
                .mapaVendasAgregadasPorPeriodo(new HashMap<>())
                .build();

        Set<String> locationIds = (locations == null) ?
                null
                : locations.stream().map(Location::getId).collect(Collectors.toSet());
        Set<String> materialIds = (produtos == null) ?
                null
                : produtos.stream().map(Produto::getId).collect(Collectors.toSet());

        // Uma chamada por item configurado, sem extrair diariamente para
        // corrigir semanas técnicas. A fronteira exata deve chegar ao WHERE.
        for (IntervaloExtracaoCalendario intervalo : calendario.getIntervalosExtracao()) {
            Constantes.TamanhoBucket bucketConsulta = intervalo.tamanhoBucket() == Constantes.TamanhoBucket.ANUAL
                    ? Constantes.TamanhoBucket.MENSAL : intervalo.tamanhoBucket();
            IntervaloExtracaoCalendario intervaloConsulta = new IntervaloExtracaoCalendario(
                    intervalo.dataHorarioInicial(), intervalo.dataHorarioFinal(), bucketConsulta);
            Collection<AggregatedByLocationMaterialUOMDate> listaSelloutAgregado =
                    calendario instanceof CalendarioSimples simples
                            ? historicalSalesSource.getAggregatedByLocationMaterialUomDate(simples, locationIds, materialIds)
                            : historicalSalesSource.getAggregatedByLocationMaterialUomDate(
                                    intervaloConsulta, locationIds, materialIds);

            // Carrega os agregados de sales sell-out do Community no bucket solicitado.
            // Quando material/location sao informados, a consulta usa ids para evitar
            // problemas de identidade de entidade em execucao paralela.
            for (AggregatedByLocationMaterialUOMDate aggregatedByLocationMaterialUOMDate : listaSelloutAgregado) {
                UnidadeMedida unidadeMedida = aggregatedByLocationMaterialUOMDate.getUom();
                if (unidadeMedida == null) {
                    // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                    // precisa acontecer depois da leitura do agregado.
                    unidadeMedida = (unidadePadrao != null)
                            ? unidadePadrao
                            : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
                }

                AggregatedByLocationMaterialUOMDate aggregatedNormalizado = AggregatedByLocationMaterialUOMDateImpl.builder()
                        .material(aggregatedByLocationMaterialUOMDate.getMaterial())
                        .location(aggregatedByLocationMaterialUOMDate.getLocation())
                        .uom(unidadeMedida)
                        .referenceDate(calendario instanceof CalendarioSimples simples
                                && simples.getTamanhoBucket() != Constantes.TamanhoBucket.ANUAL
                                ? aggregatedByLocationMaterialUOMDate.getReferenceDate()
                                : normalizaDataReferenciaSalesAgregado(aggregatedByLocationMaterialUOMDate.getReferenceDate(),
                                        tipoDocumentoVenda, intervaloConsulta, calendario))
                        .totalQuantity(aggregatedByLocationMaterialUOMDate.getTotalQuantity())
                        .build();
                if (locationIds == null || locationIds.contains(aggregatedNormalizado.getLocation().getId())) {
                    if (materialIds == null || materialIds.contains(aggregatedNormalizado.getMaterial().getId())) {
                        salesProjection.addSalesAgregado(aggregatedNormalizado);
                    }
                }
            }

        }

        return salesProjection;
    }

    /**
     * Materializa uma projection de vendas preservando pares reais
     * location/material.
     *
     * <p>JPQL padrao recebe colecoes independentes de locations e materiais,
     * mas nao uma colecao heterogenea de pares. Por isso a leitura usa uma
     * unica consulta pelo envelope do escopo e este metodo elimina da
     * projection os pares cruzados que nao pertencem ao
     * {@link LocationMaterialCorrelationScope}. A alternativa de uma query por
     * location criaria N+1 e nao e aceita para snapshots de Demand Planning.</p>
     *
     * <p>A sobrecarga convencional seleciona a fonte documental disponível.
     * Assim, sell-in e pedidos fechados reutilizam a mesma filtragem
     * correlacionada sem duplicar a estratégia de consulta.</p>
     */
    public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialData(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            Calendario calendario,
            LocationMaterialCorrelationScope locationMaterialCorrelationScope,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadePadrao) {

        SalesProjectionLocationMaterialData envelopeSalesProjection =
                getSalesProjectionLocationMaterialData(
                        tipoDocumentoVenda,
                        calendario,
                        locationMaterialCorrelationScope.getLocations(),
                        locationMaterialCorrelationScope.getMateriais(),
                        unidadeMedidaProjection,
                        clusterEParametrosProjection,
                        unidadePadrao);

        SalesProjectionLocationMaterialData correlatedSalesProjection =
                SalesProjectionLocationMaterialData.builder()
                        .conversaoUnidadeMedidaProjection(
                                envelopeSalesProjection.getConversaoUnidadeMedidaProjection())
                        .clusterEParametrosProjection(
                                envelopeSalesProjection.getClusterEParametrosProjection())
                        .calendario(envelopeSalesProjection.getCalendario())
                        .locations(locationMaterialCorrelationScope.getLocations())
                        .materiais(locationMaterialCorrelationScope.getMateriais())
                        .unidadeMedidaPadraoParaNulos(
                                envelopeSalesProjection.getUnidadeMedidaPadraoParaNulos())
                        .mapaVendasAgregadasPorPeriodo(new HashMap<>())
                        .build();

        for (AggregatedByLocationMaterialUOMDate salesAggregate
                : envelopeSalesProjection.getSetSalesConsolidado()) {
            if (locationMaterialCorrelationScope.contains(
                    salesAggregate.getLocation(),
                    salesAggregate.getMaterial())) {
                correlatedSalesProjection.addSalesAgregado(salesAggregate);
            }
        }

        return correlatedSalesProjection;

    }

    public SalesProjectionLocationMaterialData getSalesProjectionLocationMaterialDataConsolidandoComModoPropagacaoDemanda(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            CalendarioSimples calendario,
            VersaoMalha versaoMalha,
            PerfilExecucaoSupplyPlan.ModoPropagacaoDemanda modoPropagacaoDemanda,
            LocationProjection locationProjection,
            MaterialProjection materialProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            SupplyNetworkProjection supplyNetworkProjection,
            UnidadeMedida unidadePadrao) {

        HistoricalSalesSource historicalSalesSource = getHistoricalSalesSource(tipoDocumentoVenda);

        Set<Location> locationsQueReceberaoConsolidcaoDemanda = locationProjection.getLocationsAtivasSetComTipoLocation(modoPropagacaoDemanda.getTipoLocationDestinoPropagacao());
        Set<Produto> materiaisNoProjection = materialProjection.getMateriaisAtivos();

        // traz o sellout diretamente amarrado às locations internas
        SalesProjectionLocationMaterialData salesProjection = getSalesProjectionLocationMaterialData(
                tipoDocumentoVenda,
                calendario,
                locationsQueReceberaoConsolidcaoDemanda,
                materiaisNoProjection,
                unidadeMedidaProjection,
                clusterEParametrosProjection,
                unidadePadrao);

        // extrai o sellout completo de todas as locations
        Collection<AggregatedByLocationMaterialUOMDate> listaSelloutAgregado =
                historicalSalesSource.getAggregatedByLocationMaterialUomDate(
                        calendario,
                        null,
                        null);

        // varre os dados de sellout. se material estiver na lista, location destino = cliente e location origem = interna (e na lista),
        // criar novo selloutAgregado (com location = location interna) e adicionar ao BI
        for (AggregatedByLocationMaterialUOMDate selloutAgregadoBruto : listaSelloutAgregado) {
            UnidadeMedida unidadeMedida = selloutAgregadoBruto.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadePadrao != null)
                        ? unidadePadrao
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            AggregatedByLocationMaterialUOMDate selloutAgregado = AggregatedByLocationMaterialUOMDateImpl.builder()
                    .material(selloutAgregadoBruto.getMaterial())
                    .location(selloutAgregadoBruto.getLocation())
                    .uom(unidadeMedida)
                    .referenceDate(selloutAgregadoBruto.getReferenceDate())
                    .totalQuantity(selloutAgregadoBruto.getTotalQuantity())
                    .build();
            Location location = selloutAgregado.getLocation();
            Produto material = selloutAgregado.getMaterial();
            if (modoPropagacaoDemanda.verificaSeRealizaPropagacao(location) && materiaisNoProjection.contains(material)) {
                Optional<Location> optionalLocationOrigem = supplyNetworkProjection.getLocationOrigemPrioritaria(
                        versaoMalha,
                        location,
                        material,
                        calendario.getDataHorarioInicialPresente(),
                        locationsQueReceberaoConsolidcaoDemanda);
                optionalLocationOrigem.ifPresent(locationOrigem -> {

                    if (locationOrigem.getTipoLocation().equals(LocationAbstract.TipoLocation.INTERNA)) {
                        AggregatedByLocationMaterialUOMDateImpl selloutAgregadoImpl = AggregatedByLocationMaterialUOMDateImpl.builder()
                                .material(material)
                                .location(locationOrigem)
                                .referenceDate(selloutAgregado.getReferenceDate())
                                .uom(selloutAgregado.getUom())
                                .totalQuantity(selloutAgregado.getTotalQuantity())
                                .build();

                        salesProjection.addSalesAgregado(selloutAgregadoImpl);
                    }

                });
            }
        }

        return salesProjection;

    }

    /**
     * Popula o projection de sell-out para uma unica location.
     *
     * @return projection agregado por material/data para a location informada
     */
    public SalesProjectionMaterialData getSalesProjectionMaterialData(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            CalendarioSimples calendario, Location location, Set<Produto> produtos,
            UnidadeMedidaProjection conversaoUnidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadePadrao) {

        getHistoricalSalesSource(tipoDocumentoVenda);


        return getSalesProjectionMaterialData(
                tipoDocumentoVenda,
                calendario,
                Sets.newHashSet(location), produtos,
                conversaoUnidadeMedidaProjection, clusterEParametrosProjection, unidadePadrao);

    }

    public SalesProjectionMaterial getSalesProjectionMaterial(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            CalendarioSimples calendario, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection conversaoUnidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadePadrao) {

        HistoricalSalesSource historicalSalesSource = getHistoricalSalesSource(tipoDocumentoVenda);

        SalesProjectionMaterial salesProjection = SalesProjectionMaterial.builder()
                .conversaoUnidadeMedidaProjection(conversaoUnidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .calendario(calendario)
                .locations(locations)
                .materiais(produtos)
                .unidadeMedidaPadraoParaNulos(unidadePadrao)
                .mapaVendasAgregadasPorMaterial(new HashMap<>())
                .build();

        if (locations.isEmpty() || produtos.isEmpty()) return salesProjection;

        Set<String> locationIds = locations.stream().map(Location::getId).collect(Collectors.toSet());
        Set<String> materialIds = produtos.stream().map(Produto::getId).collect(Collectors.toSet());
        Collection<AggregatedByMaterialUOM> selloutAgregadoPorMaterialUOMCollection =
                historicalSalesSource.getAggregatedByMaterialUom(
                        calendario,
                        locationIds,
                        materialIds);

        for (AggregatedByMaterialUOM aggregatedByMaterialUOM : selloutAgregadoPorMaterialUOMCollection) {
            UnidadeMedida unidadeMedida = aggregatedByMaterialUOM.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadePadrao != null)
                        ? unidadePadrao
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            salesProjection.addSalesAgregado(AggregatedByMaterialUOMImpl.builder()
                    .material(aggregatedByMaterialUOM.getMaterial())
                    .uom(unidadeMedida)
                    .totalQuantity(aggregatedByMaterialUOM.getTotalQuantity())
                    .build());
        }

        return salesProjection;
    }

    public SalesProjectionLocationMaterial getSalesProjectionMaterialLocation(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            CalendarioSimples calendario, Set<Location> locations, Set<Produto> produtos,
            UnidadeMedidaProjection conversaoUnidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadePadrao) {

        HistoricalSalesSource historicalSalesSource = getHistoricalSalesSource(tipoDocumentoVenda);

        SalesProjectionLocationMaterial salesProjection = SalesProjectionLocationMaterial.builder()
                .conversaoUnidadeMedidaProjection(conversaoUnidadeMedidaProjection)
                .clusterEParametrosProjection(clusterEParametrosProjection)
                .calendario(calendario)
                .locations(locations)
                .materiais(produtos)
                .unidadeMedidaPadraoParaNulos(unidadePadrao)
                .mapaVendasAgregadasPorMaterialLocation(new HashMap<>())
                .build();

        if (locations.isEmpty() || produtos.isEmpty()) return salesProjection;

        Set<String> locationIds = locations.stream().map(Location::getId).collect(Collectors.toSet());
        Set<String> materialIds = produtos.stream().map(Produto::getId).collect(Collectors.toSet());
        Collection<AggregatedByLocationMaterialUOM> selloutAgregadoPorLocationMaterialUOMCollection =
                historicalSalesSource.getAggregatedByLocationMaterialUom(
                        calendario,
                        locationIds,
                        materialIds);

        for (AggregatedByLocationMaterialUOM aggregatedByLocationMaterialUOM :
                selloutAgregadoPorLocationMaterialUOMCollection) {
            UnidadeMedida unidadeMedida = aggregatedByLocationMaterialUOM.getUom();
            if (unidadeMedida == null) {
                // Hibernate 6 não aceita mais coalesce de entidade; o fallback da UOM
                // precisa acontecer depois da leitura do agregado.
                unidadeMedida = (unidadePadrao != null)
                        ? unidadePadrao
                        : clusterEParametrosProjection.getSNPUnidadeMedidaPadraoGlobal();
            }

            salesProjection.addSalesAgregado(AggregatedByLocationMaterialUOMImpl.builder()
                    .material(aggregatedByLocationMaterialUOM.getMaterial())
                    .location(aggregatedByLocationMaterialUOM.getLocation())
                    .uom(unidadeMedida)
                    .totalQuantity(aggregatedByLocationMaterialUOM.getTotalQuantity())
                    .build());
        }

        return salesProjection;
    }

    /**
     * Materializa primeira e última venda da fonte documental selecionada.
     */
    public FirstLastSalesProjection getFirstLastSalesProjectionLocationMaterial(
            Constantes.TipoDocumentoVenda tipoDocumentoVenda,
            CalendarioSimples calendario) {

        HistoricalSalesSource historicalSalesSource = getHistoricalSalesSource(tipoDocumentoVenda);

        FirstLastSalesProjection firstLastSalesProjection = new FirstLastSalesProjection(calendario);

        /*
         * FirstLastSalesProjection e um indice mutavel em memoria. Embora use
         * ConcurrentHashMap internamente, a montagem deste snapshot deve ficar
         * sequencial para preservar ordem de falha, evitar disputa em nested maps
         * e manter o paralelismo apenas na rodada de clusters/projections.
         */
        historicalSalesSource.getFirstLastByMaterialLocation()
                .stream()
                .forEach(firstLastSalesProjection::addFirstLastByMaterialLocation);

        historicalSalesSource.getFirstLastByLocation()
                .stream()
                .forEach(firstLastSalesProjection::addFirstLastByLocation);

        historicalSalesSource.getFirstLastByMaterial()
                .stream()
                .forEach(firstLastSalesProjection::addFirstLastByMaterial);

        return firstLastSalesProjection;
    }

    /**
     * Valida calendario das projections de sell-out antes de acessar bucket,
     * datas ou repositories.
     */

    /**
     * Valida a projection de unidades usada para normalizar UOM historica.
     */
    /**
     * Valida a projection central usada para defaults de UOM e filtros de DFU.
     */
    /**
     * Valida a malha usada para consolidar vendas de clientes em locations
     * internas no modo de propagacao de demanda.
     */

    /**
     * Corrige somente a chave de tempo depois do filtro SQL, sem ratear valores.
     * Ex.: a query semanal limitada a 03/05 pode devolver 300 unidades com a
     * referência nominal domingo 05/05. Fazemos min(fim nominal, 03/05) e
     * indexamos em 03/05; as 300 unidades já excluem 04/05 e 05/05 pelo WHERE.
     * A primeira semana técnica passa pela mesma regra, com seu domingo dentro
     * da janela. Não há chamadas extras ou mapa auxiliar para suas duas pontas.
     *
     * <p>Pedidos preservam a assimetria legada: filtro por finalização e
     * agrupamento por data de pedido podem devolver bucket fora da faixa.
     * Nesse caso a referência original é mantida, nunca descartada.</p>
     */
    public static LocalDate normalizaDataReferenciaSalesAgregado(LocalDate referenciaOriginal,
                                                          Constantes.TipoDocumentoVenda documento,
                                                          IntervaloExtracaoCalendario intervalo,
                                                          Calendario calendario) {

        LocalDateTime inicioNominal = CalendarioSimples.getPrimeiraDataHorarioPeriodo(
                referenciaOriginal.atStartOfDay(), intervalo.tamanhoBucket());
        LocalDateTime fimNominal = CalendarioSimples.getUltimaDataHorarioPeriodo(
                referenciaOriginal.atStartOfDay(), intervalo.tamanhoBucket());
        if (fimNominal.isBefore(intervalo.dataHorarioInicial()) || inicioNominal.isAfter(intervalo.dataHorarioFinal())) {
            if (documento == Constantes.TipoDocumentoVenda.PEDIDO) return referenciaOriginal;
            throw new IllegalStateException("Sales aggregate bucket does not overlap queried interval: " + intervalo);
        }
        LocalDateTime referenciaRecortada = fimNominal.isBefore(intervalo.dataHorarioFinal())
                ? fimNominal : intervalo.dataHorarioFinal();
        return calendario.getUltimaDataPeriodo(calendario.getPosicaoPeriodo(referenciaRecortada));

    }

}
