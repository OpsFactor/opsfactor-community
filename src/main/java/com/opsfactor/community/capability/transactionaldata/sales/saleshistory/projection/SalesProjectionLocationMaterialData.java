package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedByLocationMaterialUOMDate;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.FuncoesMap;
import com.pivovarit.function.ThrowingFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Projection material/location/data para consolidacao rapida de vendas
 * historicas observadas.
 *
 * <p>No Community a venda historica funcional e sell-out quantitativo, mas a
 * projection nao sabe qual documento alimentou os agregados. Essa neutralidade
 * permite que o Enterprise complemente sell-in ou sales orders na factory
 * privada sem trocar consumidores de forecast, Demand Analysis ou Planning
 * Book.</p>
 */
@SuperBuilder
@Getter
public class SalesProjectionLocationMaterialData extends SalesProjectionAbstract {
    
    /**
     * Indice material -> location -> data -> agregados sales. O desenho permite
     * consultar rapidamente por DFU, material, location ou periodo sem voltar
     * ao banco durante forecast e Planning Book.
     *
     * <p>O `@Builder.Default` e importante porque varias bordas de teste e
     * simulacao criam snapshots vazios via builder. Snapshot vazio e valido;
     * mapa nulo indica projection quebrada e nao deve aparecer como NPE tardio
     * em rotinas de forecast.</p>
     */
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Map<Produto,Map<Location,Map<LocalDate,Set<AggregatedByLocationMaterialUOMDate>>>> mapaVendasAgregadasPorPeriodo = new HashMap<>();

    public Set<DFU> getDFUsComSales() {
        Set<DFU> dfus = new HashSet<>();
        for (Produto produto : mapaVendasAgregadasPorPeriodo.keySet()) {
            for (Location location : mapaVendasAgregadasPorPeriodo.get(produto).keySet()) {
                dfus.add(new DFU(produto, location));
            }
        }
        return dfus;
    }
    
    public Optional<Integer> getUltimoPeriodoComSales() {
        
        Optional<LocalDate> optionalUltimaDataSales = mapaVendasAgregadasPorPeriodo.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.keySet().stream())
                .max(Comparator.comparing(LocalDate::toEpochDay));
        
        return optionalUltimaDataSales
                .map(dataSales -> getCalendario().getPosicaoPeriodo(dataSales));
        
    }
    
    public Optional<Integer> getPrimeiroPeriodoComSales() {
        
        Optional<LocalDate> optionalPrimeiraDataSales = mapaVendasAgregadasPorPeriodo.values().stream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.keySet().stream())
                .min(Comparator.comparing(LocalDate::toEpochDay));
        
        return optionalPrimeiraDataSales
                .map(dataSales -> getCalendario().getPosicaoPeriodo(dataSales));
        
    }
    
    public Set<Produto> getMateriaisComSales() {
        return mapaVendasAgregadasPorPeriodo.keySet();
    }

    public Set<Produto> getMateriaisComSalesNaLocation(Location location) {
        return mapaVendasAgregadasPorPeriodo
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().keySet().contains(location))
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }

    public Set<Location> getLocationsComSales() {
        return mapaVendasAgregadasPorPeriodo.values().stream()
                .flatMap(x -> x.keySet().stream())
                .collect(Collectors.toSet());
    }
    
    public Set<Location> getLocationsComSales(Produto material) {
        return mapaVendasAgregadasPorPeriodo.getOrDefault(material, new HashMap<>()).keySet();
    }
    
    public void addSalesAgregado(AggregatedByLocationMaterialUOMDate aggregatedByLocationMaterialUOMDate) {

        validaSalesAgregadoObrigatorio(
                aggregatedByLocationMaterialUOMDate,
                "location-material-date sales projection");
        validaSalesAgregadoMaterialObrigatorio(
                aggregatedByLocationMaterialUOMDate.getMaterial(),
                "location-material-date sales projection");
        validaSalesAgregadoLocationObrigatoria(
                aggregatedByLocationMaterialUOMDate.getLocation(),
                "location-material-date sales projection");
        validaSalesAgregadoReferenceDateObrigatoria(
                aggregatedByLocationMaterialUOMDate.getReferenceDate(),
                "location-material-date sales projection");

        mapaVendasAgregadasPorPeriodo
                .computeIfAbsent(aggregatedByLocationMaterialUOMDate.getMaterial(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByLocationMaterialUOMDate.getLocation(), x -> new HashMap<>())
                .computeIfAbsent(aggregatedByLocationMaterialUOMDate.getReferenceDate(), x -> new HashSet<>())
                .add(aggregatedByLocationMaterialUOMDate);

    }
    
    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado() {
        
        return mapaVendasAgregadasPorPeriodo
                .values().parallelStream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }

    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado(Produto material) {

        return mapaVendasAgregadasPorPeriodo
                .getOrDefault(material, new HashMap<>())
                .values().parallelStream()
                .flatMap(x -> x.values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());

    }

    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado(Produto produto, LocalDate data) {
        
        if (!mapaVendasAgregadasPorPeriodo.containsKey(produto)) return new HashSet<>();
        
        return mapaVendasAgregadasPorPeriodo
                .get(produto)
                .values().parallelStream()
                .map(x -> x.getOrDefault(data, new HashSet<>()))
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
    }
    
    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado(Produto produto, Location location) {
        
        return mapaVendasAgregadasPorPeriodo
                .getOrDefault(produto, new HashMap<>())
                .getOrDefault(location, new HashMap<>())
                .values().parallelStream()
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toSet());
        
    }

    public Map<Integer, Double> getQuantidadeTotalSalesPorPeriodo(MaterialProjection materialProjection, LocationProjection locationProjection, UnidadeMedida unidadeMedida) {

        return mapaVendasAgregadasPorPeriodo
                .entrySet()
                .parallelStream()
                .filter(entry -> materialProjection.getMaterialSet().contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().entrySet().parallelStream())
                .filter(entry -> locationProjection.getLocationSet().contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().values().stream())
                .flatMap(x -> x.stream())
                .collect(Collectors.groupingBy(
                        x -> calendario.getPosicaoPeriodo(x.getReferenceDate()),
                        Collectors.summingDouble(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida))
                ));

    }

    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado(Location location, Produto produto, LocalDate data) {
        
        return mapaVendasAgregadasPorPeriodo
                .getOrDefault(produto, new HashMap<>())
                .getOrDefault(location, new HashMap<>())
                .getOrDefault(data, new HashSet<>());

    }

    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado(Location location, LocalDate data) {
        return mapaVendasAgregadasPorPeriodo
                .values()
                .stream()
                .flatMap(subMapa -> subMapa
                        .getOrDefault(location, new HashMap<>())
                        .getOrDefault(data, new HashSet<>()).stream())
                .collect(Collectors.toSet());
    }

    public Set<AggregatedByLocationMaterialUOMDate> getSetSalesConsolidado(Location location) {
        return mapaVendasAgregadasPorPeriodo
                .values()
                .stream()
                .flatMap(subMapa -> subMapa
                        .getOrDefault(location, new HashMap<>())
                        .values()
                        .stream())
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    // EXTRAÇÃO QUANTIDADE SALES MATERIAL/PERIODO
    public double getQuantidadeSales(Produto produto, LocalDate data, UnidadeMedida unidadeMedida) {
        
        return getSetSalesConsolidado(produto, data).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
        
    }

    public double getQuantidadeSales(Set<DFU> dfus,  UnidadeMedida unidadeMedida) {
        return dfus
                .parallelStream()
                .mapToDouble(dfu -> getQuantidadeSales(dfu.getProduto(), dfu.getLocation(), unidadeMedida))
                .sum();
    }
    
    // EXTRAÇÃO QUANTIDADE SALES MATERIAL/LOCATION/PERIODO
    public double getQuantidadeSales(Produto produto, Location location, Calendario calendario, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        if (calendario == null
                || this.calendario == null
                || !Objects.equals(calendario.getTamanhoBucket(), this.calendario.getTamanhoBucket())) {
            throw getIncompatibleSalesProjectionCalendarException(calendario);
        }
        return getQuantidadeSales(produto, location, calendario.getUltimaDataPeriodo(posicaoPeriodo), unidadeMedida);
    }
    public double getQuantidadeSales(Produto produto, Location location, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        return getQuantidadeSales(produto, location, calendario.getUltimaDataPeriodo(posicaoPeriodo), unidadeMedida);
    }
    public double getQuantidadeSales(Location location, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        return getQuantidadeSales(location, calendario.getUltimaDataPeriodo(posicaoPeriodo), unidadeMedida);
    }
    public double getQuantidadeSales(Produto produto, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        return getQuantidadeSales(produto, calendario.getUltimaDataPeriodo(posicaoPeriodo), unidadeMedida);
    }
    public double getQuantidadeSales(Collection<Produto> produtos, Location location, int posicaoPeriodo, UnidadeMedida unidadeMedida) {
        LocalDate dataReferencia = calendario.getUltimaDataPeriodo(posicaoPeriodo);

        return mapaVendasAgregadasPorPeriodo.entrySet()
                .stream()
                .filter(entry -> produtos.contains(entry.getKey()))
                .mapToDouble(entry -> entry.getValue()
                            .getOrDefault(location, new HashMap<>())
                            .getOrDefault(dataReferencia, new HashSet<>())
                            .stream()
                            .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(entry.getKey(), x.getUom(), unidadeMedida)))
                            .mapToDouble(x -> x)
                            .sum())
                .sum();
    }
    public double getQuantidadeSales(Produto produto, Location location, LocalDate data, UnidadeMedida unidadeMedida) {
        return getSetSalesConsolidado(location, produto, data).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }
    public double getQuantidadeSales(Location location, LocalDate data, UnidadeMedida unidadeMedida) {
        return getSetSalesConsolidado(location, data).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

    private IllegalArgumentException getIncompatibleSalesProjectionCalendarException(Calendario calendarioConsulta) {

        return new IllegalArgumentException(
                "SalesProjectionLocationMaterialData requires the query calendar bucket to match the projection calendar bucket; query bucket="
                        + getTamanhoBucket(calendarioConsulta)
                        + ", projection bucket="
                        + getTamanhoBucket(this.calendario)
                        + ". Build or query the sales projection with a calendar using the same bucket.");

    }

    private static String getTamanhoBucket(Calendario calendario) {

        return calendario == null ? "null" : String.valueOf(calendario.getTamanhoBucket());

    }
    public double getQuantidadeSales(Location location, UnidadeMedida unidadeMedida) {
        return getSetSalesConsolidado(location).stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }
    public double getQuantidadeSales(Produto produto, UnidadeMedida unidadeMedida) {
        return FuncoesMap.flattenMapToStream(
                mapaVendasAgregadasPorPeriodo.getOrDefault(produto, new HashMap<>()),
                AggregatedByLocationMaterialUOMDate.class)
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

    public double getQuantidadeSales(Produto produto, Location location, UnidadeMedida unidadeMedida) {
        return mapaVendasAgregadasPorPeriodo
                .getOrDefault(produto, new HashMap<>())
                .getOrDefault(location, new HashMap<>())
                .values()
                .stream()
                .flatMap(set -> set.stream())
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

    public double getQuantidadeSales(UnidadeMedida unidadeMedida) {
        return getSetSalesConsolidado().stream()
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(x.getMaterial(), x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }
    public double getQuantidadeSales(Produto produto, Location location, LocalDate dataInicial, LocalDate dataFinal, UnidadeMedida unidadeMedida) {
        return getSetSalesConsolidado(produto, location).stream()
                .filter(x ->
                        (x.getReferenceDate().isAfter(dataInicial) || x.getReferenceDate().isEqual(dataInicial))
                        && (x.getReferenceDate().isBefore(dataFinal) || x.getReferenceDate().isEqual(dataFinal)))
                .map(ThrowingFunction.unchecked(x -> x.getTotalQuantity() * conversaoUnidadeMedidaProjection.getConversaoParaUnidadeDestino(produto, x.getUom(), unidadeMedida)))
                .mapToDouble(x -> x)
                .sum();
    }

}
