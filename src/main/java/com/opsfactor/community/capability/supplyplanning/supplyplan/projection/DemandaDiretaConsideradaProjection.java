package com.opsfactor.community.capability.supplyplanning.supplyplan.projection;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.projection.inmemorybi.BIEmMemoria;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;
import org.javatuples.Pair;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

/**
 * Projection em memoria da demanda direta considerada pelo Supply Planning.
 *
 * <p>No Community, a serie efetiva e derivada do Demand Plan e permanece
 * separada entre plano irrestrito, restrito e trabalho. Campos de carteira,
 * sales orders, sell-in, pedidos firmes e reconciliacao transacional ficam
 * zerados ate que o overlay Enterprise alimente essas origens.</p>
 */
public class DemandaDiretaConsideradaProjection {

    /** Supply Plan dono das linhas indexadas. */
    @Getter
    SupplyPlan supplyPlan;

    /** Calendario usado para mapear data de referencia para periodo. */
    @Getter
    Calendario calendario;

    /** Projection de unidades de medida para conversao segura de quantidades. */
    @Getter
    UnidadeMedidaProjection unidadeMedidaProjection;

    /**
     * BI de demanda atendida e, quando configurado no perfil, propagada para
     * locations internas. A chave funcional Community e material/location/periodo.
     */
    BIEmMemoria<DemandaDiretaConsideradaLinha> biEmMemoriaDemandPlanRestritoEIrrestritoLinha = new BIEmMemoria<>(DemandaDiretaConsideradaLinha.class);

    /**
     * Cria a projection vazia e registra os indices usados pelas rotinas de
     * Supply Planning.
     */
    public DemandaDiretaConsideradaProjection(SupplyPlan supplyPlan, Calendario calendario, UnidadeMedidaProjection unidadeMedidaProjection) {

        if (supplyPlan == null || calendario == null) {
            throw new IllegalArgumentException(
                    "Direct demand considered projection requires supply plan and calendar.");
        }

        this.supplyPlan = supplyPlan;
        this.calendario = calendario;
        this.unidadeMedidaProjection = unidadeMedidaProjection;

        biEmMemoriaDemandPlanRestritoEIrrestritoLinha.addObjectAttribute(
                "material",
                Produto.class,
                DemandaDiretaConsideradaLinha::getMaterial,
                true);

        biEmMemoriaDemandPlanRestritoEIrrestritoLinha.addObjectAttribute(
                "location",
                Location.class,
                DemandaDiretaConsideradaLinha::getLocation,
                true);

        // cria apenas 1 atributo para período
        biEmMemoriaDemandPlanRestritoEIrrestritoLinha.addIntegerAttribute(
                "periodo",
                aggregatedByLocationMaterialUOMDate -> calendario.getPosicaoPeriodo(aggregatedByLocationMaterialUOMDate.getDataReferencia()),
                true);

    }

    /**
     * Adiciona uma linha da demanda direta considerada, mantendo apenas uma
     * linha por material/location/periodo.
     */
    public void addDemandPlanRestritoEIrrestritoLinha(DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) {

        validaDemandaDiretaConsideradaLinhaParaIndexacao(demandaDiretaConsideradaLinha);

        /*
         * A chave funcional da demanda considerada no Supply Plan e mensal. Se
         * o banco ainda contiver execucoes antigas com horarios diferentes no
         * mesmo mes, mantemos somente a linha carregada primeiro pela factory
         * para nao somar a mesma demanda mais de uma vez no modelo.
         */
        int periodo = calendario.getPosicaoPeriodo(demandaDiretaConsideradaLinha.getDataReferencia());
        if (getDemandaDiretaConsideradaLinha(
                demandaDiretaConsideradaLinha.getLocation(),
                demandaDiretaConsideradaLinha.getMaterial(),
                periodo).isPresent()) {
            return;
        }

        biEmMemoriaDemandPlanRestritoEIrrestritoLinha.addElementoNoBI(demandaDiretaConsideradaLinha);

    }

    /**
     * Valida uma linha antes de indexa-la no BI em memoria.
     *
     * <p>A factory ja protege os snapshots vindos do repository, mas esta
     * projection tambem pode ser alimentada diretamente por rotinas de Supply
     * Planning, testes e overlays Enterprise. Chaves incompletas devem falhar
     * aqui, antes de entrar no indice por material/location/periodo.</p>
     */
    private void validaDemandaDiretaConsideradaLinhaParaIndexacao(
            DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) {

        if (demandaDiretaConsideradaLinha == null) {
            throw new IllegalArgumentException(
                    "Direct demand considered projection cannot index null line.");
        }

        DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey compositeKey =
                demandaDiretaConsideradaLinha.getDemandaDiretaConsideradaLinhaCompositeKey();
        if (compositeKey == null
                || compositeKey.getSupplyPlan() == null
                || compositeKey.getLocation() == null
                || compositeKey.getMaterial() == null
                || compositeKey.getDataReferencia() == null) {
            throw new IllegalArgumentException(
                    "Direct demand considered projection requires line with supply plan, location, material and reference date before indexing.");
        }

        if (compositeKey.getLocation().getId() == null
                || compositeKey.getLocation().getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Direct demand considered projection requires line location with id before indexing.");
        }

        if (compositeKey.getMaterial().getId() == null
                || compositeKey.getMaterial().getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Direct demand considered projection requires line material with id before indexing.");
        }

    }

    /**
     * Busca a linha unica de material/location/periodo.
     */
    public Optional<DemandaDiretaConsideradaLinha> getDemandaDiretaConsideradaLinha(
            Location location,
            Produto material,
            int periodo) {

        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getWhereEquals(
                Pair.with("location", location),
                Pair.with("material", material),
                Pair.with("periodo", periodo))
                .stream().findAny();

    }

    /**
     * Retorna todas as linhas de uma DFU material/location.
     */
    public List<DemandaDiretaConsideradaLinha> getDemandaDiretaConsideradaLinha(
            Location location,
            Produto material) {

        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getWhereEquals(
                Pair.with("location", location),
                Pair.with("material", material))
                .stream()
                .collect(Collectors.toList());

    }

    public List<DemandaDiretaConsideradaLinha> getDemandaDiretaConsideradaLinha(Location location) {

        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getWhereEquals(
                Pair.with("location", location))
                .stream()
                .collect(Collectors.toList());

    }

    public List<DemandaDiretaConsideradaLinha> getDemandaDiretaConsideradaLinha(int periodo) {

        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getWhereEquals(
                        Pair.with("periodo", periodo))
                .stream()
                .collect(Collectors.toList());

    }

    public DemandaDiretaConsideradaLinha getOrAddDemandaDiretaConsideradaLinha(
            Location location,
            Produto material,
            int periodo) {

        return getDemandaDiretaConsideradaLinha(location, material, periodo)
                .orElseGet(() -> {
                    DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha = new DemandaDiretaConsideradaLinha(
                            new DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey(
                                    supplyPlan,
                                    location,
                                    material,
                                    calendario.getUltimaDataHorarioPeriodo(periodo)));
                    addDemandPlanRestritoEIrrestritoLinha(demandaDiretaConsideradaLinha);
                    return demandaDiretaConsideradaLinha;
                });

    }


    public double getQuantidadeConsideradaSupplyPlan(
            Location location,
            Produto material,
            int periodo,
            DemandaDiretaConsideradaLinha.UsoDemandaDireta usoDemandaDireta,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {

        return getDemandaDiretaConsideradaLinha(location, material, periodo)
                .stream()
                .mapToDouble(demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getQuantidadeConsideradaSupplyPlan(
                        usoDemandaDireta, tipoPlano, unidadeMedidaTarget, unidadeMedidaProjection))
                .sum();

    }

    public double getQuantidadeOriginal(
            Location location,
            Produto material,
            int periodo,
            DemandaDiretaConsideradaLinha.TipoDemandaDireta tipoDemandaDireta,
            DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta propagacaoDemandaDireta,
            UnidadeMedida unidadeMedidaTarget) {

        return getDemandaDiretaConsideradaLinha(location, material, periodo)
                .stream()
                .mapToDouble(demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getQuantidadeOriginal(
                        tipoDemandaDireta, propagacaoDemandaDireta, unidadeMedidaTarget, unidadeMedidaProjection))
                .sum();

    }

    public double getQuantidadeOriginal(
            Location location,
            Produto material,
            DemandaDiretaConsideradaLinha.TipoDemandaDireta tipoDemandaDireta,
            DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta propagacaoDemandaDireta,
            UnidadeMedida unidadeMedidaTarget) {

        return getDemandaDiretaConsideradaLinha(location, material)
                .stream()
                .mapToDouble(demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getQuantidadeOriginal(
                        tipoDemandaDireta, propagacaoDemandaDireta, unidadeMedidaTarget, unidadeMedidaProjection))
                .sum();

    }

    public double getQuantidadeConsideradaSupplyPlan(
            Location location,
            Produto material,
            DemandaDiretaConsideradaLinha.UsoDemandaDireta usoDemandaDireta,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget) {

        return getDemandaDiretaConsideradaLinha(location, material)
                .stream()
                .mapToDouble(demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getQuantidadeConsideradaSupplyPlan(
                        usoDemandaDireta, tipoPlano, unidadeMedidaTarget, unidadeMedidaProjection))
                .sum();

    }

    /**
     * Retorna valor economico considerado no Supply Plan para uma DFU/periodo.
     *
     * <p>O Community nao usa valores, custos ou margens no heuristico, mas as
     * colunas permanecem na linha compartilhada para compatibilidade com o
     * Enterprise. A projection consolida os valores ja materializados na linha
     * sem buscar tabelas de precos/custos que sao exclusivas da edicao privada.</p>
     */
    public double getValorConsideradoSupplyPlan(
            Location location,
            Produto material,
            int periodo,
            DemandaDiretaConsideradaLinha.ValorDemandaDireta valorDemandaDireta,
            Constantes.TipoPlano tipoPlano) {

        return getDemandaDiretaConsideradaLinha(location, material, periodo)
                .stream()
                .mapToDouble(demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getValorConsideradoSupplyPlan(
                        valorDemandaDireta,
                        tipoPlano))
                .sum();

    }

    /**
     * Atualiza uma das quantidades consideradas supply plan de um único DemandaDiretaConsideradaLinha
     * Se não houver um DemandaDiretaConsideradaLinha cria um novo e o indexa no BI
     */
    public void updateQuantidadeConsideradaSupplyPlan(
            DoubleUnaryOperator funcaoAtualizacaoQuantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            Location location,
            Produto material,
            int periodo,
            DemandaDiretaConsideradaLinha.UsoDemandaDireta usoDemandaDireta,
            Constantes.TipoPlano tipoPlano) {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha = getOrAddDemandaDiretaConsideradaLinha(location, material, periodo);

        demandaDiretaConsideradaLinha.updateQuantidadeConsideradaSupplyPlan(
                funcaoAtualizacaoQuantidade, unidadeMedidaQuantidade, usoDemandaDireta, tipoPlano, unidadeMedidaProjection);

    }

    public void updateQuantidadeDemandaDiretaConsideradaSegregada(
            DoubleUnaryOperator funcaoAtualizacaoQuantidade,
            UnidadeMedida unidadeMedidaQuantidade,
            Location location,
            Produto material,
            int periodo,
            DemandaDiretaConsideradaLinha.TipoDemandaDireta tipoDemandaDireta,
            Constantes.TipoPlano tipoPlano) {

        /*
         * Mantem a atualizacao separada por origem da demanda direta. A rotina
         * Community usa PLANO_DEMANDA; o segmento CARTEIRA permanece zerado
         * salvo quando uma extensao Enterprise alimentar esse canal.
         */
        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha = getOrAddDemandaDiretaConsideradaLinha(
                location,
                material,
                periodo);

        demandaDiretaConsideradaLinha.updateQuantidadeDemandaDiretaConsideradaSegregada(
                funcaoAtualizacaoQuantidade,
                unidadeMedidaQuantidade,
                tipoDemandaDireta,
                tipoPlano,
                unidadeMedidaProjection);

    }

    public void atualizaPlanoIrrestritoCommunityComDemandPlan(
            PoliticaEstoquesProjection politicaEstoquesProjection,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        ClusterEParametrosProjection clusterEParametrosProjection = politicaEstoquesProjection.getClusterEParametrosProjection();

        biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getAllRecords()
                .stream()
                .forEach(demandaDiretaConsideradaLinha -> {
                    Location location = demandaDiretaConsideradaLinha.getLocation();

                    // só atualiza locations cobertas pelo perfil de execução
                    if (!perfilExecucaoSupplyPlan.contemLocation(clusterEParametrosProjection, location)) return;

                    // Community usa apenas Demand Plan como fonte futura.
                    // Conciliacao forecast/carteira, MTO sem forecast e
                    // horizonte de carteira sao recursos Enterprise.
                    demandaDiretaConsideradaLinha.atualizaPlanoIrrestritoCommunityComPlanoDemanda();
                });

    }

    public List<DemandaDiretaConsideradaLinha> getAllDemandaDiretaConsideradaLinha() {
        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getAllRecords()
                .stream()
                .collect(Collectors.toList());
    }

    public Set<Produto> getMateriaisComDemandaDiretaConsiderada() {
        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getAllRecords()
                .stream()
                .map(DemandaDiretaConsideradaLinha::getMaterial)
                .collect(Collectors.toSet());
    }

    public Set<DFU> getDFUsComDemandaDiretaConsiderada() {
        return biEmMemoriaDemandPlanRestritoEIrrestritoLinha.getAllRecords()
                .stream()
                .map(demandaDiretaConsideradaLinha -> new DFU(
                        demandaDiretaConsideradaLinha.getMaterial(),
                        demandaDiretaConsideradaLinha.getLocation()))
                .collect(Collectors.toSet());
    }

    public void eliminaDemandaDiretaAbaixoDeValorMinimo(UnidadeMedida unidadeMedidaReferencia, double valorMinimoPermitido) {
        biEmMemoriaDemandPlanRestritoEIrrestritoLinha.removeElementosDoBISeCondicao(
                demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getQuantidadeOriginal(
                        DemandaDiretaConsideradaLinha.TipoDemandaDireta.TOTAL,
                        DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta.TOTAL,
                        unidadeMedidaReferencia,
                        unidadeMedidaProjection) < valorMinimoPermitido);
    }

}
