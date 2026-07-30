package com.opsfactor.community.capability.supplyplanning.service.heuristic;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducaoSimples;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem.DistributionPlanItemKey;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha.ProductionPlanLinhaCompositeKey;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva.MasterOrPlanningData;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningBiProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Redistribui a produção planejada do plano irrestrito do heurístico antes da
 * rodada restrita, preservando volume, prioridades e capacidade física.
 */
@Slf4j
@Service
public class NivelamentoCapacidadePlanoIrrestritoHeuristicoService {

    private static final double EPSILON = 0.00001;

    @Transactional
    public boolean aplica(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            SupplyPlanningBiProjection supplyPlanningBiProjection) {

        if (!perfilExecucaoSupplyPlan.getHeuristicUnconstrainedPlanCapacityLeveling()) {
            return false;
        }
        if (!perfilExecucaoSupplyPlan.getModoExecucao().equals(PerfilExecucaoSupplyPlan.ModoExecucao.HEURISTICO)) {
            return false;
        }
        Set<ProductionPlanLinha> productionPlanLinhas = supplyPlanningBiProjection.getTodosProductionPlanLinhas();
        if (productionPlanLinhas.isEmpty()) {
            return false;
        }

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        UnidadeMedidaProjection unidadeMedidaProjection = supplyNetworkProjection.getConversaoUnidadeMedidaProjection();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        List<NecessidadeProducao> necessidades = criaNecessidadesPlanejadas(
                productionPlanLinhas, calendario, supplyNetworkProjection, clusterEParametrosProjection,
                unidadeMedidaProjection, perfilExecucaoSupplyPlan);
        if (necessidades.isEmpty()) {
            return false;
        }

        Map<RecursoProdutivo, Map<Integer, Double>> capacidadeResidualPorRecursoEPeriodo = new HashMap<>();
        reservaCapacidadeParaLinhasFixas(
                productionPlanLinhas,
                necessidades.stream().map(NecessidadeProducao::linhaOriginal).collect(Collectors.toSet()),
                capacidadeResidualPorRecursoEPeriodo, calendario, supplyNetworkProjection,
                biProjectionCapacidadeProdutiva, perfilExecucaoSupplyPlan);
        List<AlocacaoProducao> alocacoes = new ArrayList<>();
        necessidades.sort(Comparator.comparingInt(NecessidadeProducao::posicaoPeriodoNecessidade)
                .thenComparing(necessidade -> necessidade.linhaOriginal().getLocation().getId())
                .thenComparing(necessidade -> necessidade.linhaOriginal().getMaterialOutput().getId()));

        Map<Integer, List<NecessidadeProducao>> necessidadesPorPeriodo = necessidades.stream().collect(Collectors.groupingBy(
                NecessidadeProducao::posicaoPeriodoNecessidade, LinkedHashMap::new, Collectors.toList()));
        for (List<NecessidadeProducao> necessidadesMesmoPeriodo : necessidadesPorPeriodo.values()) {
            tentaAlocarNecessidadesDoPeriodo(necessidadesMesmoPeriodo, calendario, supplyNetworkProjection,
                    biProjectionCapacidadeProdutiva, perfilExecucaoSupplyPlan,
                    capacidadeResidualPorRecursoEPeriodo, alocacoes);
        }
        tentaAlocarEmOrigensAlternativas(necessidades, calendario, supplyNetworkProjection,
                biProjectionCapacidadeProdutiva, perfilExecucaoSupplyPlan,
                capacidadeResidualPorRecursoEPeriodo,
                alocacoes, supplyPlanningBiProjection);
        materializaAlocacoesNoPlanoIrrestrito(productionPlanLinhas, necessidades, alocacoes, calendario,
                clusterEParametrosProjection, unidadeMedidaProjection, parametrosGlobais, supplyPlanningBiProjection);
        atualizaEstoquesIrrestritos(perfilExecucaoSupplyPlan, supplyPlanningBiProjection);

        log.info("Capacity leveling changed {} planned production needs in unconstrained plan {}",
                necessidades.size(), supplyPlan.getId());
        return true;

    }

    private List<NecessidadeProducao> criaNecessidadesPlanejadas(
            Collection<ProductionPlanLinha> productionPlanLinhas,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        List<NecessidadeProducao> necessidades = new ArrayList<>();
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhas) {
            if (!perfilExecucaoSupplyPlan.getConsideraRestricaoProducao(productionPlanLinha.getLocation())) {
                continue;
            }
            if (!(productionPlanLinha.getVersaoProducao() instanceof VersaoProducaoSimples versaoProducaoOriginal)) {
                continue;
            }
            UnidadeMedida unidadeMedidaPadrao = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(
                    productionPlanLinha.getMaterialOutput(), productionPlanLinha.getLocation());
            double quantidadePlanejada = productionPlanLinha.getQuantidade(
                    Constantes.TipoPlano.PLANO_IRRESTRITO, Constantes.FirmePlanejado.PLANEJADO,
                    unidadeMedidaPadrao, unidadeMedidaProjection);
            if (quantidadePlanejada <= EPSILON) {
                continue;
            }
            int posicaoPeriodoNecessidade = calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia());
            if (posicaoPeriodoNecessidade < calendario.getPosicaoPeriodoPresente()
                    || posicaoPeriodoNecessidade > calendario.getPosicaoPeriodoFinalFuturo()) {
                continue;
            }
            List<VersaoProducaoSimples> alternativasOrdenadas = getAlternativasMesmoBomOrdenadas(
                    versaoProducaoOriginal, supplyNetworkProjection);
            necessidades.add(new NecessidadeProducao(productionPlanLinha, unidadeMedidaPadrao,
                    posicaoPeriodoNecessidade, quantidadePlanejada, alternativasOrdenadas));
        }
        return necessidades;

    }

    private List<VersaoProducaoSimples> getAlternativasMesmoBomOrdenadas(
            VersaoProducaoSimples versaoProducaoOriginal,
            SupplyNetworkProjection supplyNetworkProjection) {

        List<VersaoProducaoSimples> alternativas = supplyNetworkProjection
                .getVersoesProducaoViaveisOrdenadasPorPrioridade(
                        versaoProducaoOriginal.getLocation(), versaoProducaoOriginal.getMaterialOutput(), false, null)
                .stream().filter(VersaoProducaoSimples.class::isInstance).map(VersaoProducaoSimples.class::cast)
                .filter(versaoProducao -> versaoProducao.getListaTecnica().equals(versaoProducaoOriginal.getListaTecnica()))
                .collect(Collectors.toCollection(ArrayList::new));
        alternativas.remove(versaoProducaoOriginal);
        alternativas.addFirst(versaoProducaoOriginal);
        return alternativas;

    }

    private void reservaCapacidadeParaLinhasFixas(
            Collection<ProductionPlanLinha> productionPlanLinhas,
            Set<ProductionPlanLinha> linhasNivelaveis,
            Map<RecursoProdutivo, Map<Integer, Double>> capacidadeResidualPorRecursoEPeriodo,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan) {

        Map<RecursoProdutivo, Map<Integer, Double>> consumoFixoPorRecursoEPeriodo = new HashMap<>();
        for (ProductionPlanLinha productionPlanLinha : productionPlanLinhas) {
            int posicaoPeriodo = calendario.getPosicaoPeriodo(productionPlanLinha.getDataReferencia());
            if (posicaoPeriodo < calendario.getPosicaoPeriodoPresente()
                    || posicaoPeriodo > calendario.getPosicaoPeriodoFinalFuturo()) {
                continue;
            }
            productionPlanLinha.getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                            Constantes.TipoPlano.PLANO_IRRESTRITO, Constantes.FirmePlanejado.ORDEM,
                            perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva(), supplyNetworkProjection)
                    .forEach((recursoProdutivo, consumo) -> mergeQuantidadePorRecursoEPeriodo(
                            consumoFixoPorRecursoEPeriodo,
                            recursoProdutivo,
                            posicaoPeriodo,
                            consumo));
            if (!linhasNivelaveis.contains(productionPlanLinha)) {
                productionPlanLinha.getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
                                Constantes.TipoPlano.PLANO_IRRESTRITO, Constantes.FirmePlanejado.PLANEJADO,
                                perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva(), supplyNetworkProjection)
                        .forEach((recursoProdutivo, consumo) -> mergeQuantidadePorRecursoEPeriodo(
                                consumoFixoPorRecursoEPeriodo,
                                recursoProdutivo,
                                posicaoPeriodo,
                                consumo));
            }
        }
        consumoFixoPorRecursoEPeriodo.forEach((recursoProdutivo, consumoFixoPorPeriodo) ->
                consumoFixoPorPeriodo.forEach((posicaoPeriodo, consumoFixo) -> {
                    double capacidade = getCapacidadeInicial(
                            recursoProdutivo,
                            posicaoPeriodo,
                            capacidadeResidualPorRecursoEPeriodo,
                            biProjectionCapacidadeProdutiva);
                    putQuantidadePorRecursoEPeriodo(
                            capacidadeResidualPorRecursoEPeriodo,
                            recursoProdutivo,
                            posicaoPeriodo,
                            Math.max(0, capacidade - consumoFixo));
                }));

    }

    private void tentaAlocarNecessidadesDoPeriodo(
            List<NecessidadeProducao> necessidadesMesmoPeriodo,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Map<RecursoProdutivo, Map<Integer, Double>> capacidadeResidualPorRecursoEPeriodo,
            List<AlocacaoProducao> alocacoes) {

        int posicaoPeriodoNecessidade = necessidadesMesmoPeriodo.getFirst().posicaoPeriodoNecessidade();
        for (int posicaoPeriodoProducao = posicaoPeriodoNecessidade;
             posicaoPeriodoProducao >= calendario.getPosicaoPeriodoPresente()
                     && necessidadesMesmoPeriodo.stream().anyMatch(necessidade -> necessidade.quantidadeResidual() > EPSILON);
             posicaoPeriodoProducao--) {
            int numeroMaximoAlternativas = necessidadesMesmoPeriodo.stream()
                    .mapToInt(necessidade -> necessidade.alternativasOrdenadas().size()).max().orElse(0);
            for (int indiceAlternativa = 0; indiceAlternativa < numeroMaximoAlternativas; indiceAlternativa++) {
                List<CandidatoAlocacao> candidatos = new ArrayList<>();
                for (NecessidadeProducao necessidade : necessidadesMesmoPeriodo) {
                    if (necessidade.quantidadeResidual() <= EPSILON
                            || indiceAlternativa >= necessidade.alternativasOrdenadas().size()) {
                        continue;
                    }
                    VersaoProducaoSimples alternativa = necessidade.alternativasOrdenadas().get(indiceAlternativa);
                    Map<RecursoProdutivo, Double> consumoPorUnidade = supplyNetworkProjection
                            .getConsumoCapacidadePorRecursoProdutivoEmHorasOuQuantidade(
                                    alternativa.getRoteiro(), 1, necessidade.unidadeMedidaPadrao(),
                                    perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva());
                    candidatos.add(new CandidatoAlocacao(necessidade, alternativa, posicaoPeriodoProducao,
                            null, consumoPorUnidade));
                }
                aplicaFairShare(
                        candidatos,
                        capacidadeResidualPorRecursoEPeriodo,
                        biProjectionCapacidadeProdutiva,
                        alocacoes);
            }
        }

    }

    private void tentaAlocarEmOrigensAlternativas(
            List<NecessidadeProducao> necessidades,
            Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            Map<RecursoProdutivo, Map<Integer, Double>> capacidadeResidualPorRecursoEPeriodo,
            List<AlocacaoProducao> alocacoes,
            SupplyPlanningBiProjection supplyPlanningBiProjection) {

        Map<Integer, List<NecessidadeProducao>> necessidadesPorPeriodo = necessidades.stream()
                .filter(necessidade -> necessidade.quantidadeResidual() > EPSILON)
                .collect(Collectors.groupingBy(NecessidadeProducao::posicaoPeriodoNecessidade,
                        LinkedHashMap::new, Collectors.toList()));
        for (List<NecessidadeProducao> necessidadesMesmoPeriodo : necessidadesPorPeriodo.values()) {
            Map<NecessidadeProducao, List<AlternativaOrigem>> alternativasPorNecessidade = new LinkedHashMap<>();
            TreeSet<Integer> prioridadesOrigem = new TreeSet<>();
            for (NecessidadeProducao necessidade : necessidadesMesmoPeriodo) {
                List<AlternativaOrigem> alternativasOrigem = criaAlternativasOrigem(
                        necessidade, calendario, supplyNetworkProjection, supplyPlanningBiProjection);
                alternativasPorNecessidade.put(necessidade, alternativasOrigem);
                alternativasOrigem.stream().map(AlternativaOrigem::prioridade).forEach(prioridadesOrigem::add);
            }
            for (Integer prioridadeOrigem : prioridadesOrigem) {
                if (necessidadesMesmoPeriodo.stream().noneMatch(necessidade -> necessidade.quantidadeResidual() > EPSILON)) {
                    break;
                }
                int antecipacaoMaxima = alternativasPorNecessidade.values().stream().flatMap(Collection::stream)
                        .filter(alternativaOrigem -> alternativaOrigem.prioridade() == prioridadeOrigem)
                        .mapToInt(alternativaOrigem -> alternativaOrigem.posicaoPeriodoExpedicao()
                                - calendario.getPosicaoPeriodoPresente()).max().orElse(-1);
                for (int antecipacao = 0; antecipacao <= antecipacaoMaxima; antecipacao++) {
                    int antecipacaoAtual = antecipacao;
                    int numeroMaximoAlternativasProdutivas = alternativasPorNecessidade.values().stream()
                            .flatMap(Collection::stream)
                            .filter(alternativaOrigem -> alternativaOrigem.prioridade() == prioridadeOrigem)
                            .filter(alternativaOrigem -> alternativaOrigem.posicaoPeriodoExpedicao()
                                    - antecipacaoAtual >= calendario.getPosicaoPeriodoPresente())
                            .mapToInt(alternativaOrigem -> alternativaOrigem.versoesProducaoOrdenadas().size()).max().orElse(0);
                    for (int indiceVersao = 0; indiceVersao < numeroMaximoAlternativasProdutivas; indiceVersao++) {
                        List<CandidatoAlocacao> candidatos = new ArrayList<>();
                        for (NecessidadeProducao necessidade : necessidadesMesmoPeriodo) {
                            if (necessidade.quantidadeResidual() <= EPSILON) continue;
                            for (AlternativaOrigem alternativaOrigem : alternativasPorNecessidade.getOrDefault(necessidade, List.of())) {
                                if (alternativaOrigem.prioridade() != prioridadeOrigem
                                        || indiceVersao >= alternativaOrigem.versoesProducaoOrdenadas().size()) continue;
                                int posicaoPeriodoProducao = alternativaOrigem.posicaoPeriodoExpedicao() - antecipacao;
                                if (posicaoPeriodoProducao < calendario.getPosicaoPeriodoPresente()) continue;
                                VersaoProducaoSimples versaoProducao = alternativaOrigem.versoesProducaoOrdenadas().get(indiceVersao);
                                Map<RecursoProdutivo, Double> consumoPorUnidade = perfilExecucaoSupplyPlan
                                        .getConsideraRestricaoProducao(alternativaOrigem.locationOrigem())
                                        ? supplyNetworkProjection.getConsumoCapacidadePorRecursoProdutivoEmHorasOuQuantidade(
                                                versaoProducao.getRoteiro(), 1, necessidade.unidadeMedidaPadrao(),
                                                perfilExecucaoSupplyPlan.getTipoCapacidadeProdutiva())
                                        : Map.of();
                                candidatos.add(new CandidatoAlocacao(necessidade, versaoProducao,
                                        posicaoPeriodoProducao, alternativaOrigem, consumoPorUnidade));
                            }
                        }
                        aplicaFairShare(candidatos, capacidadeResidualPorRecursoEPeriodo,
                                biProjectionCapacidadeProdutiva, alocacoes);
                    }
                }
            }
        }

    }

    private List<AlternativaOrigem> criaAlternativasOrigem(NecessidadeProducao necessidade, Calendario calendario,
            SupplyNetworkProjection supplyNetworkProjection, SupplyPlanningBiProjection supplyPlanningBiProjection) {

        ProductionPlanLinha linhaOriginal = necessidade.linhaOriginal();
        Location locationDestino = linhaOriginal.getLocation();
        Produto material = linhaOriginal.getMaterialOutput();
        Set<Location> locationsNoEscopo = supplyPlanningBiProjection.getLocationProjection().getLocationsAtivas();
        List<AlternativaOrigem> alternativasOrigem = new ArrayList<>();
        for (LinhaTransporte linhaTransporte : supplyNetworkProjection.getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(
                linhaOriginal.getSupplyPlan().getVersaoMalha(), locationDestino, material,
                linhaOriginal.getDataReferencia(), locationsNoEscopo)) {
            Location locationOrigem = linhaTransporte.getLocationOrigem();
            if (locationOrigem.equals(locationDestino)) continue;
            List<VersaoProducaoSimples> versoesProducao = supplyNetworkProjection
                    .getVersoesProducaoViaveisOrdenadasPorPrioridade(locationOrigem, material, false, null).stream()
                    .filter(VersaoProducaoSimples.class::isInstance).map(VersaoProducaoSimples.class::cast)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (versoesProducao.isEmpty()) continue;
            Pair<Integer, Integer> periodos = DistributionPlanItem.getPosicaoPeriodosExpedicaoERecebimentoDeReferencia(
                    Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, calendario,
                    necessidade.posicaoPeriodoNecessidade(), linhaOriginal.getSupplyPlan().getVersaoMalha(), material,
                    locationOrigem, locationDestino, supplyNetworkProjection);
            if (periodos.getValue0() < calendario.getPosicaoPeriodoPresente()) continue;
            int prioridade = supplyNetworkProjection.getParametrosLinhaTransporte(
                    linhaTransporte, material, linhaOriginal.getDataReferencia()).orElseThrow(() ->
                    new IllegalStateException("Missing transportation parameters for viable capacity-leveling lane."))
                    .getPrioridade();
            alternativasOrigem.add(new AlternativaOrigem(locationOrigem, locationDestino, prioridade,
                    periodos.getValue0(), periodos.getValue1(), versoesProducao));
        }
        return alternativasOrigem;

    }

    private void aplicaFairShare(List<CandidatoAlocacao> candidatos,
            Map<RecursoProdutivo, Map<Integer, Double>> capacidadeResidualPorRecursoEPeriodo,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            List<AlocacaoProducao> alocacoes) {

        List<CandidatoAlocacao> candidatosAtivos = new ArrayList<>(candidatos);
        while (!candidatosAtivos.isEmpty()) {
            Map<NecessidadeProducao, Long> quantidadeCandidatosPorNecessidade = candidatosAtivos.stream()
                    .collect(Collectors.groupingBy(CandidatoAlocacao::necessidade, LinkedHashMap::new, Collectors.counting()));
            Map<CandidatoAlocacao, Double> quantidadeSolicitada = new LinkedHashMap<>();
            candidatosAtivos.forEach(candidato -> quantidadeSolicitada.put(candidato,
                    candidato.necessidade().quantidadeResidual() / quantidadeCandidatosPorNecessidade.get(candidato.necessidade())));
            Map<RecursoProdutivo, Map<Integer, Double>> cargaSolicitadaPorRecursoEPeriodo = new HashMap<>();
            for (CandidatoAlocacao candidato : candidatosAtivos) {
                candidato.consumoPorUnidade().forEach((recurso, consumo) -> {
                    if (consumo > EPSILON) {
                        mergeQuantidadePorRecursoEPeriodo(
                                cargaSolicitadaPorRecursoEPeriodo,
                                recurso,
                                candidato.posicaoPeriodoProducao(),
                                quantidadeSolicitada.get(candidato) * consumo);
                    }
                });
            }
            Map<RecursoProdutivo, Map<Integer, Double>> fatorAtendimentoPorRecursoEPeriodo = new HashMap<>();
            cargaSolicitadaPorRecursoEPeriodo.forEach((recursoProdutivo, cargaSolicitadaPorPeriodo) ->
                    cargaSolicitadaPorPeriodo.forEach((posicaoPeriodo, cargaSolicitada) ->
                            putQuantidadePorRecursoEPeriodo(
                                    fatorAtendimentoPorRecursoEPeriodo,
                                    recursoProdutivo,
                                    posicaoPeriodo,
                                    Math.min(
                                            1,
                                            getCapacidadeInicial(
                                                    recursoProdutivo,
                                                    posicaoPeriodo,
                                                    capacidadeResidualPorRecursoEPeriodo,
                                                    biProjectionCapacidadeProdutiva)
                                                    / cargaSolicitada))));
            Map<CandidatoAlocacao, Double> proposta = new LinkedHashMap<>();
            for (CandidatoAlocacao candidato : candidatosAtivos) {
                double fator = candidato.consumoPorUnidade().entrySet().stream().filter(consumo -> consumo.getValue() > EPSILON)
                        .mapToDouble(consumo -> getQuantidadePorRecursoEPeriodo(
                                fatorAtendimentoPorRecursoEPeriodo,
                                consumo.getKey(),
                                candidato.posicaoPeriodoProducao()))
                        .min()
                        .orElse(1);
                proposta.put(candidato, quantidadeSolicitada.get(candidato) * fator);
            }
            if (proposta.values().stream().noneMatch(quantidade -> quantidade > EPSILON)) return;
            proposta.forEach((candidato, quantidade) -> {
                if (quantidade <= EPSILON) return;
                candidato.consumoPorUnidade().forEach((recurso, consumo) -> {
                    putQuantidadePorRecursoEPeriodo(
                            capacidadeResidualPorRecursoEPeriodo,
                            recurso,
                            candidato.posicaoPeriodoProducao(),
                            Math.max(
                                    0,
                                    getCapacidadeInicial(
                                            recurso,
                                            candidato.posicaoPeriodoProducao(),
                                            capacidadeResidualPorRecursoEPeriodo,
                                            biProjectionCapacidadeProdutiva)
                                            - quantidade * consumo));
                });
                candidato.necessidade().reduzQuantidadeResidual(quantidade);
                alocacoes.add(new AlocacaoProducao(candidato.necessidade(), candidato.versaoProducao(),
                        candidato.posicaoPeriodoProducao(), candidato.alternativaOrigem(), quantidade));
            });
            candidatosAtivos.removeIf(candidato -> candidato.necessidade().quantidadeResidual() <= EPSILON
                    || candidato.consumoPorUnidade().entrySet().stream().anyMatch(consumo -> consumo.getValue() > EPSILON
                    && getCapacidadeInicial(
                            consumo.getKey(),
                            candidato.posicaoPeriodoProducao(),
                            capacidadeResidualPorRecursoEPeriodo,
                            biProjectionCapacidadeProdutiva) <= EPSILON));
        }

    }

    private void materializaAlocacoesNoPlanoIrrestrito(Collection<ProductionPlanLinha> productionPlanLinhas,
            Collection<NecessidadeProducao> necessidades, Collection<AlocacaoProducao> alocacoes,
            Calendario calendario, ClusterEParametrosProjection parametros, UnidadeMedidaProjection uomProjection,
            ParametrosGlobais parametrosGlobais, SupplyPlanningBiProjection snapshot) {

        Map<ProductionPlanLinhaCompositeKey, ProductionPlanLinha> linhasPorChave = new LinkedHashMap<>();
        productionPlanLinhas.forEach(linha -> linhasPorChave.put(linha.getProductionPlanLinhaCompositeKey(), linha));
        Map<DistributionPlanItemKey, DistributionPlanItem> distribuicoesPorChave = snapshot
                .getTodosDistributionPlanItems().stream().collect(Collectors.toMap(
                        DistributionPlanItem::getKey, linha -> linha,
                        (primeira, segunda) -> primeira, LinkedHashMap::new));
        necessidades.stream().map(NecessidadeProducao::linhaOriginal).collect(Collectors.toSet()).forEach(linha ->
                linha.setQuantidade(0, Constantes.TipoPlano.PLANO_IRRESTRITO, Constantes.FirmePlanejado.PLANEJADO,
                        linha.getUnidadeMedida(parametrosGlobais), uomProjection));
        for (AlocacaoProducao alocacao : alocacoes) {
            Location locationProducao = Optional.ofNullable(alocacao.alternativaOrigem()).map(AlternativaOrigem::locationOrigem)
                    .orElse(alocacao.necessidade().linhaOriginal().getLocation());
            adicionaQuantidadePlanejada(linhasPorChave, alocacao.necessidade().linhaOriginal().getSupplyPlan(),
                    locationProducao, alocacao.necessidade().linhaOriginal().getMaterialOutput(), alocacao.versaoProducao(),
                    alocacao.posicaoPeriodoProducao(), alocacao.quantidade(), alocacao.necessidade().unidadeMedidaPadrao(),
                    calendario, parametros, uomProjection, snapshot);
            if (alocacao.alternativaOrigem() != null) adicionaTransferenciaPlanejada(distribuicoesPorChave,
                    alocacao.necessidade(), alocacao.alternativaOrigem(), alocacao.quantidade(), calendario,
                    parametros, uomProjection, snapshot);
        }
        for (NecessidadeProducao necessidade : necessidades) {
            if (necessidade.quantidadeResidual() > EPSILON) adicionaQuantidadePlanejada(linhasPorChave,
                    necessidade.linhaOriginal().getSupplyPlan(), necessidade.linhaOriginal().getLocation(),
                    necessidade.linhaOriginal().getMaterialOutput(), (VersaoProducaoSimples) necessidade.linhaOriginal().getVersaoProducao(),
                    necessidade.posicaoPeriodoNecessidade(), necessidade.quantidadeResidual(), necessidade.unidadeMedidaPadrao(),
                    calendario, parametros, uomProjection, snapshot);
        }

    }

    private void adicionaQuantidadePlanejada(Map<ProductionPlanLinhaCompositeKey, ProductionPlanLinha> linhasPorChave,
            SupplyPlan supplyPlan, Location location, Produto materialOutput, VersaoProducaoSimples versaoProducao,
            int posicaoPeriodo, double quantidade, UnidadeMedida unidadeMedidaQuantidade, Calendario calendario,
            ClusterEParametrosProjection parametros, UnidadeMedidaProjection uomProjection,
            SupplyPlanningBiProjection snapshot) {

        ProductionPlanLinhaCompositeKey chave = new ProductionPlanLinhaCompositeKey(supplyPlan, location, versaoProducao,
                versaoProducao.getRoteiro(), versaoProducao.getListaTecnica(), calendario.getUltimoSegundoPeriodo(posicaoPeriodo));
        ProductionPlanLinha linha = linhasPorChave.get(chave);
        UnidadeMedida uomPadrao = parametros.getSNPUnidadeMedidaPadrao(materialOutput, location);
        if (linha == null) {
            linha = new ProductionPlanLinha(chave, materialOutput);
            linha.setUnidadeMedida(uomPadrao);
            linhasPorChave.put(chave, linha);
            snapshot.addProductionPlanLinha(linha);
        }
        double atual = linha.getQuantidade(Constantes.TipoPlano.PLANO_IRRESTRITO, Constantes.FirmePlanejado.PLANEJADO,
                uomPadrao, uomProjection);
        double quantidadePadrao = quantidade * uomProjection.getConversaoParaUnidadeDestino(
                materialOutput, unidadeMedidaQuantidade, uomPadrao);
        linha.setQuantidade(atual + quantidadePadrao, Constantes.TipoPlano.PLANO_IRRESTRITO,
                Constantes.FirmePlanejado.PLANEJADO, uomPadrao, uomProjection);

    }

    private void adicionaTransferenciaPlanejada(Map<DistributionPlanItemKey, DistributionPlanItem> distribuicoesPorChave,
            NecessidadeProducao necessidade, AlternativaOrigem origem, double quantidade, Calendario calendario,
            ClusterEParametrosProjection parametros, UnidadeMedidaProjection uomProjection,
            SupplyPlanningBiProjection snapshot) {

        ProductionPlanLinha linhaOriginal = necessidade.linhaOriginal();
        DistributionPlanItemKey chave = new DistributionPlanItemKey(linhaOriginal.getSupplyPlan(),
                origem.locationDestino(), origem.locationOrigem(), linhaOriginal.getMaterialOutput(),
                calendario.getPrimeiraDataHorarioPeriodo(origem.posicaoPeriodoExpedicao()),
                calendario.getUltimoSegundoPeriodo(origem.posicaoPeriodoRecebimento()));
        DistributionPlanItem linha = distribuicoesPorChave.get(chave);
        UnidadeMedida uomDestino = parametros.getSNPUnidadeMedidaPadrao(linhaOriginal.getMaterialOutput(), origem.locationDestino());
        if (linha == null) {
            linha = new DistributionPlanItem(chave);
            linha.setUnidadeMedida(uomDestino);
            distribuicoesPorChave.put(chave, linha);
            snapshot.addDistributionPlanItem(linha);
        }
        double atual = linha.getQuantidadeNaUnidadeMedidaTarget(Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_IRRESTRITO, uomDestino, uomProjection);
        double quantidadeDestino = quantidade * uomProjection.getConversaoParaUnidadeDestino(
                linhaOriginal.getMaterialOutput(), necessidade.unidadeMedidaPadrao(), uomDestino);
        linha.setQuantidadeEmUnidadeMedida(atual + quantidadeDestino, uomDestino, Constantes.FirmePlanejado.PLANEJADO,
                Constantes.TipoPlano.PLANO_IRRESTRITO, uomProjection);

    }

    private void atualizaEstoquesIrrestritos(PerfilExecucaoSupplyPlan perfil, SupplyPlanningBiProjection snapshot) {

        if (!perfil.getSalvaInventoryPlan()) return;
        for (Location location : snapshot.getLocationProjection().getLocationsAtivas()) {
            SupplyPlanningProjection projection = snapshot.getSupplyPlanningProjection(location, snapshot.getMaterialProjection());
            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(projection, Constantes.TipoPlano.PLANO_IRRESTRITO);
            snapshot.sincroniza(projection);
        }

    }

    private double getCapacidadeInicial(
            RecursoProdutivo recursoProdutivo,
            int posicaoPeriodo,
            Map<RecursoProdutivo, Map<Integer, Double>> capacidadeResidualPorRecursoEPeriodo,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva) {

        return capacidadeResidualPorRecursoEPeriodo
                .computeIfAbsent(recursoProdutivo, ignoredResource -> new HashMap<>())
                .computeIfAbsent(
                        posicaoPeriodo,
                        ignoredPeriod -> Math.max(
                                0,
                                biProjectionCapacidadeProdutiva.getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                                        posicaoPeriodo,
                                        recursoProdutivo,
                                        MasterOrPlanningData.MASTER_DATA)));

    }

    private void mergeQuantidadePorRecursoEPeriodo(
            Map<RecursoProdutivo, Map<Integer, Double>> quantidadePorRecursoEPeriodo,
            RecursoProdutivo recursoProdutivo,
            int posicaoPeriodo,
            double quantidade) {

        quantidadePorRecursoEPeriodo
                .computeIfAbsent(recursoProdutivo, ignoredResource -> new HashMap<>())
                .merge(posicaoPeriodo, quantidade, Double::sum);

    }

    private void putQuantidadePorRecursoEPeriodo(
            Map<RecursoProdutivo, Map<Integer, Double>> quantidadePorRecursoEPeriodo,
            RecursoProdutivo recursoProdutivo,
            int posicaoPeriodo,
            double quantidade) {

        quantidadePorRecursoEPeriodo
                .computeIfAbsent(recursoProdutivo, ignoredResource -> new HashMap<>())
                .put(posicaoPeriodo, quantidade);

    }

    private double getQuantidadePorRecursoEPeriodo(
            Map<RecursoProdutivo, Map<Integer, Double>> quantidadePorRecursoEPeriodo,
            RecursoProdutivo recursoProdutivo,
            int posicaoPeriodo) {

        return quantidadePorRecursoEPeriodo.get(recursoProdutivo).get(posicaoPeriodo);

    }

    private static final class NecessidadeProducao {
        private final ProductionPlanLinha linhaOriginal;
        private final UnidadeMedida unidadeMedidaPadrao;
        private final int posicaoPeriodoNecessidade;
        private double quantidadeResidual;
        private final List<VersaoProducaoSimples> alternativasOrdenadas;
        private NecessidadeProducao(ProductionPlanLinha linhaOriginal, UnidadeMedida unidadeMedidaPadrao,
                int posicaoPeriodoNecessidade, double quantidadeResidual, List<VersaoProducaoSimples> alternativasOrdenadas) {
            this.linhaOriginal = linhaOriginal;
            this.unidadeMedidaPadrao = unidadeMedidaPadrao;
            this.posicaoPeriodoNecessidade = posicaoPeriodoNecessidade;
            this.quantidadeResidual = quantidadeResidual;
            this.alternativasOrdenadas = alternativasOrdenadas;
        }
        private ProductionPlanLinha linhaOriginal() { return linhaOriginal; }
        private UnidadeMedida unidadeMedidaPadrao() { return unidadeMedidaPadrao; }
        private int posicaoPeriodoNecessidade() { return posicaoPeriodoNecessidade; }
        private double quantidadeResidual() { return quantidadeResidual; }
        private List<VersaoProducaoSimples> alternativasOrdenadas() { return alternativasOrdenadas; }
        private void reduzQuantidadeResidual(double quantidadeAlocada) { quantidadeResidual = Math.max(0, quantidadeResidual - quantidadeAlocada); }
    }
    private record AlocacaoProducao(NecessidadeProducao necessidade, VersaoProducaoSimples versaoProducao,
            int posicaoPeriodoProducao, AlternativaOrigem alternativaOrigem, double quantidade) { }
    private record AlternativaOrigem(Location locationOrigem, Location locationDestino, int prioridade,
            int posicaoPeriodoExpedicao, int posicaoPeriodoRecebimento,
            List<VersaoProducaoSimples> versoesProducaoOrdenadas) { }
    private record CandidatoAlocacao(NecessidadeProducao necessidade, VersaoProducaoSimples versaoProducao,
            int posicaoPeriodoProducao, AlternativaOrigem alternativaOrigem,
            Map<RecursoProdutivo, Double> consumoPorUnidade) { }

}
