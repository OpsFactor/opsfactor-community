package com.opsfactor.community.capability.supplyplanning.distributionplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Getter
public class DistributionPlanItemBiProjection {

    Calendario calendario;
    UnidadeMedidaProjection unidadeMedidaProjection;
    BIEmMemoria<DistributionPlanItem> biEmMemoria = new BIEmMemoria<>(DistributionPlanItem.class);

    public DistributionPlanItemBiProjection(Calendario calendario, UnidadeMedidaProjection unidadeMedidaProjection) {

        this(calendario, unidadeMedidaProjection, true);

    }

    /**
     * A fotografia central pode abranger locations com calendários distintos.
     * Nesse caso, a conversão data/período pertence à view local e os índices
     * globais de período precisam permanecer desligados.
     */
    public DistributionPlanItemBiProjection(
            Calendario calendario,
            UnidadeMedidaProjection unidadeMedidaProjection,
            boolean indexaPeriodos) {

        this.calendario = calendario;
        this.unidadeMedidaProjection = unidadeMedidaProjection;

        // Plano de supply indexado para separar linhas de diferentes execuções.
        biEmMemoria.addObjectAttribute(
                "supplyPlan",
                SupplyPlan.class,
                DistributionPlanItem::getSupplyPlan,
                true);
        biEmMemoria.addObjectAttribute(
                "locationOrigem",
                Location.class,
                DistributionPlanItem::getLocationOrigem,
                true);
        biEmMemoria.addObjectAttribute(
                "locationDestino",
                Location.class,
                DistributionPlanItem::getLocationDestino,
                true);
        biEmMemoria.addObjectAttribute(
                "material",
                Produto.class,
                DistributionPlanItem::getProduto,
                true);
        if (indexaPeriodos) {
            biEmMemoria.addIntegerAttribute(
                    "periodoExpedicao",
                    distributionPlanItem -> calendario.getPosicaoPeriodo(distributionPlanItem.getDataExpedicao()),
                    true);
            biEmMemoria.addIntegerAttribute(
                    "periodoRecebimento",
                    distributionPlanItem -> calendario.getPosicaoPeriodo(distributionPlanItem.getDataRecebimento()),
                    true);
        }
        biEmMemoria.addLocalDateTimeAttribute(
                "dataExpedicao",
                DistributionPlanItem::getDataExpedicao,
                true);
        biEmMemoria.addLocalDateTimeAttribute(
                "dataRecebimento",
                DistributionPlanItem::getDataRecebimento,
                true);

    }

    public DistributionPlanItemBiProjection(Calendario calendario, UnidadeMedidaProjection unidadeMedidaProjection, Collection<DistributionPlanItem> distributionPlanItemCollection) {
        this(calendario, unidadeMedidaProjection);
        distributionPlanItemCollection.stream().forEach(distributionPlanItem -> addDadoAoBI(distributionPlanItem));
    }

    public void addDadoAoBI(DistributionPlanItem distributionPlanItem) {

        if (!biEmMemoria.contains(distributionPlanItem)) {
            biEmMemoria.addElementoNoBI(distributionPlanItem);
        }
    }

    public Stream<DistributionPlanItem> getStreamTodosDistributionPlanItems() {
        return biEmMemoria.getAllRecords().stream();
    }

    /** Retorna linhas canônicas vistas como demanda indireta da origem. */
    public Set<DistributionPlanItem> getDistributionPlanItemsPorOrigem(
            SupplyPlan supplyPlan,
            Location locationOrigem,
            Collection<Produto> materiais) {

        Set<Produto> materiaisSet = new LinkedHashSet<>(materiais);
        return biEmMemoria.getWhereEquals(
                        Pair.with("supplyPlan", supplyPlan),
                        Pair.with("locationOrigem", locationOrigem))
                .stream()
                .filter(distributionPlanItem -> materiaisSet.contains(distributionPlanItem.getProduto()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

    /** Retorna as mesmas linhas canônicas vistas como inbound no destino. */
    public Set<DistributionPlanItem> getDistributionPlanItemsPorDestino(
            SupplyPlan supplyPlan,
            Location locationDestino,
            Collection<Produto> materiais) {

        Set<Produto> materiaisSet = new LinkedHashSet<>(materiais);
        return biEmMemoria.getWhereEquals(
                        Pair.with("supplyPlan", supplyPlan),
                        Pair.with("locationDestino", locationDestino))
                .stream()
                .filter(distributionPlanItem -> materiaisSet.contains(distributionPlanItem.getProduto()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    }

    public Optional<DistributionPlanItem> getDistributionPlanItem(
            SupplyPlan supplyPlan,
            Location locationOrigem,
            Location locationDestino,
            Produto material,
            Integer periodoExpedicao,
            Integer periodoRecebimento) {

        return biEmMemoria.getWhereEquals(
                Pair.with("supplyPlan", supplyPlan),
                Pair.with("locationOrigem", locationOrigem),
                Pair.with("locationDestino", locationDestino),
                Pair.with("material", material),
                Pair.with("periodoExpedicao", periodoExpedicao),
                Pair.with("periodoRecebimento", periodoRecebimento))
                .stream()
                .findAny();

    }

    public Stream<DistributionPlanItem> getDistributionPlanItemStream(
            SupplyPlan supplyPlan,
            Location locationOrigem,
            Location locationDestino,
            Integer periodoExpedicao) {

        return biEmMemoria.getWhereEquals(
                Pair.with("supplyPlan", supplyPlan),
                Pair.with("locationOrigem", locationOrigem),
                Pair.with("locationDestino", locationDestino),
                Pair.with("periodoExpedicao", periodoExpedicao))
                .stream();

    }

    public Stream<DistributionPlanItem> getDistributionPlanItemStream(
            SupplyPlan supplyPlan,
            Location locationOrigem,
            Location locationDestino,
            Produto material,
            Integer periodoExpedicao) {

        return biEmMemoria.getWhereEquals(
                Pair.with("supplyPlan", supplyPlan),
                Pair.with("locationOrigem", locationOrigem),
                Pair.with("locationDestino", locationDestino),
                Pair.with("periodoExpedicao", periodoExpedicao),
                Pair.with("material", material))
                .stream();

    }

    public double getQuantidadeNaUnidadeMedidaTarget(
            Constantes.FirmePlanejado firmePlanejado,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            SupplyPlan supplyPlan,
            Location locationOrigem,
            Location locationDestino,
            Integer periodoExpedicao) {

        return getDistributionPlanItemStream(supplyPlan, locationOrigem, locationDestino, periodoExpedicao)
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado, tipoPlano, unidadeMedidaTarget, unidadeMedidaProjection))
                .sum();

    }

    public double getQuantidadeNaUnidadeMedidaTarget(
            Constantes.FirmePlanejado firmePlanejado,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            SupplyPlan supplyPlan,
            Location locationOrigem,
            Location locationDestino,
            Produto material,
            Integer periodoExpedicao) {

        return getDistributionPlanItemStream(supplyPlan, locationOrigem, locationDestino, material, periodoExpedicao)
                .mapToDouble(distributionPlanItem -> distributionPlanItem.getQuantidadeNaUnidadeMedidaTarget(
                        firmePlanejado, tipoPlano, unidadeMedidaTarget, unidadeMedidaProjection))
                .sum();

    }

}
