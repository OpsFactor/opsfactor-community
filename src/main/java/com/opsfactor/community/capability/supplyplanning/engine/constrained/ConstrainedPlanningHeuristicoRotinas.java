package com.opsfactor.community.capability.supplyplanning.engine.constrained;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.configuration.projection.parametros.CadastroEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;

import java.util.*;
import java.util.stream.Collectors;

import static com.opsfactor.community.capability.supplyplanning.engine.ProductionPlanning.setPercentualQuantidadeOriginalOutputNoProductionPlanLinha;
import static com.opsfactor.community.capability.supplyplanning.engine.ProductionPlanning.setPercentualQuantidadeOriginalOutputProductionPlanLinha;
import static com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning.*;

/**
 * Rotinas puras do heuristico Community para aplicar restricoes quantitativas
 * sobre projections ja carregadas pelo service de Supply Planning.
 *
 * <p>Restricoes logisticas fisicas de armazenagem, inbound e outbound foram
 * removidas do Community. O heuristico mantido aqui cobre disponibilidade de
 * estoque, propagacao de demanda, insumos e capacidade produtiva.</p>
 */
public class ConstrainedPlanningHeuristicoRotinas {

    private static final double TOLERANCIA_QUANTIDADE = 0.000000001;

    /**
     * Restringe distribution plan outbound e demand plan em caso de indisponibilidade de estoque
     * @return true se alguma restrição foi aplicada
     */
    public static boolean restringeDistributionPlanOutboundEDemandPlan(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo) {

        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();
        LocationProjection locationProjectionLocationsOrigem = supplyPlanningProjection.getLocationProjectionLocationsOrigem();
        CadastroEParametrosProjection parametrosProjection = supplyPlanningProjection.getCadastroEParametrosProjection();
        
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado();
        
        Calendario calendario = supplyPlanningProjection.getCalendario();
        Location location = supplyPlanningProjection.getLocation();
        VersaoMalha versaoMalha = supplyPlanningProjection.getSupplyPlan().getVersaoMalha();
        
        boolean houveRestricaoPlano = false;
        
        for (Produto material : supplyPlanningProjection.getMaterialProjection().getMateriaisAtivos()) {

            // se for um fornecedor e não houver roteiro / linha transporte inbound, não restringir
            // demanda ou fluxo outbound
            if (location.getTipoLocation().equals(LocationAbstract.TipoLocation.FORNECEDOR) &&
                    supplyNetworkProjection.getVersoesProducaoViaveisOrdenadasPorPrioridade(
                            location,
                            material,
                            false,
                            materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                            .isEmpty() // fornecedor não produz o material
                    && !supplyNetworkProjection
                            .getLinhaTransporteViavelPrioritariaInbound(
                                    versaoMalha,
                                    location,
                                    material,
                                    calendario.getDataHorarioInicialPresente(),
                                    locationProjectionLocationsOrigem.getLocationsAtivasOuNuloSeLocationProjectionCompleto())
                            .isPresent()) { // fornecedor não recebe o material de nenhum outro local
                continue;
            }
            
            UnidadeMedida unidadeMedidaPadrao = parametrosProjection.getSNPUnidadeMedidaPadrao(material, location);
            
            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                    supplyPlanningProjection,
                    posicaoPeriodo, material,
                    Constantes.TipoPlano.PLANO_RESTRITO);
            atualizaEstoqueSeguranca(
                    supplyPlanningProjection,
                    material,
                    Constantes.TipoPlano.PLANO_RESTRITO);
            
            // como getValorInventoryPlanLinha pode limitar o estoque a 0 dependendo do perfil de execução, deve-se
            // re-projetar o estoque de forma a trazer um possível valor negativo
            double estoqueProjetadoTotal = SupplyPlanning.getEstoqueProjetado(
                    supplyPlanningProjection,
                    posicaoPeriodo-1, posicaoPeriodo,
                    material,
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    unidadeMedidaPadrao,
                    true, true, false, true);
            if (estoqueProjetadoTotal < -TOLERANCIA_QUANTIDADE) {
                houveRestricaoPlano = true;
                restringeDemandaPorDisponibilidade(
                        supplyPlanningProjection, posicaoPeriodo, material,
                        unidadeMedidaPadrao, -estoqueProjetadoTotal);

                if (perfilExecucaoSupplyPlan.getArredondaRequisicoesLoteMinimoEMultiplo()) {
                    arredondaTransferenciasInternas(
                            supplyPlanningProjection, posicaoPeriodo, material, unidadeMedidaPadrao);
                }
            }
            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                    supplyPlanningProjection, posicaoPeriodo, material, Constantes.TipoPlano.PLANO_RESTRITO);
        }
        return houveRestricaoPlano;
    }

    /**
     * Preserva a prioridade sobre formação de estoque. O plano de demanda é
     * cortado em um único rateio antes de qualquer fonte de demanda adicional.
     */
    static double restringeDemandaPorDisponibilidade(
            SupplyPlanningProjection projection,
            int periodo,
            Produto material,
            UnidadeMedida unidade,
            double deficit) {

        Collection<DistributionPlanItem> transferencias =
                projection.getDistributionPlanItemOutboundQueueParaLocationsInternas(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, periodo, material);
        Collection<DistributionPlanItem> expedicoes =
                projection.getDistributionPlanItemOutboundQueueParaClientes(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, periodo, material);
        UnidadeMedidaProjection conversoes = projection.getConversaoUnidadeMedidaProjection();
        var restricaoDemandaAdicional = projection.getRestricaoDemandaAdicional();
        var tiposTransferencia = restricaoDemandaAdicional == null
                ? List.of(Constantes.FirmePlanejado.PLANEJADO)
                : List.of(Constantes.FirmePlanejado.PLANEJADO, Constantes.FirmePlanejado.ORDEM);

        double restante = CorteProporcional.aplica(
                deficit, parcelasTransferencias(transferencias, tiposTransferencia, conversoes, unidade, false));
        if (restante > TOLERANCIA_QUANTIDADE) {
            restante = CorteProporcional.aplica(
                    restante, parcelasPlanoDemanda(projection, periodo, material, unidade, expedicoes));
        }

        if (restante > TOLERANCIA_QUANTIDADE && restricaoDemandaAdicional != null) {
            restante = CorteProporcional.aplica(restante,
                    restricaoDemandaAdicional.getParcelasDemandaAdicional(projection, periodo, material, unidade));
        }
        if (restante > TOLERANCIA_QUANTIDADE) {
            // Transferências dedicadas podem atender ambos os grupos e são reconciliadas pelo saldo físico.
            restante = CorteProporcional.aplica(
                    restante, parcelasTransferencias(transferencias, tiposTransferencia, conversoes, unidade, true));
        }
        limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(transferencias);
        limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(expedicoes);
        return restante;

    }

    private static List<CorteProporcional.Parcela> parcelasPlanoDemanda(
            SupplyPlanningProjection projection, int periodo, Produto material, UnidadeMedida unidade,
            Collection<DistributionPlanItem> expedicoes) {

        var tipoPlano = Constantes.TipoPlano.PLANO_RESTRITO;
        var tipoDemanda = DemandaDiretaConsideradaLinha.TipoDemandaDireta.PLANO_DEMANDA;
        UnidadeMedidaProjection conversoes = projection.getConversaoUnidadeMedidaProjection();
        List<CorteProporcional.Parcela> parcelas = new ArrayList<>();
        projection.getDemandaDiretaConsideradaProjection()
                .getDemandaDiretaConsideradaLinha(projection.getLocation(), material, periodo)
                .ifPresent(linha -> {
                    double quantidade = linha.getQuantidadeDemandaDiretaConsideradaSegregada(tipoDemanda, tipoPlano);
                    if (quantidade == 0) {
                        return;
                    }
                    double quantidadeConvertida = linha.getQuantidadeConsideradaSupplyPlan(
                            DemandaDiretaConsideradaLinha.UsoDemandaDireta.PROJECAO_ESTOQUE,
                            tipoDemanda, tipoPlano, unidade, conversoes);
                    parcelas.add(new CorteProporcional.Parcela(quantidadeConvertida, fracao -> {
                        linha.setQuantidadeDemandaDiretaConsideradaSegregada(
                                quantidade * (1 - fracao), tipoDemanda, tipoPlano);
                        var restricaoDemandaAdicional = projection.getRestricaoDemandaAdicional();
                        if (restricaoDemandaAdicional != null) {
                            restricaoDemandaAdicional.aplicaCorteValoresPlanoDemanda(linha, fracao);
                        }
                    }));
                });

        for (DistributionPlanItem expedicao : expedicoes) {
            double quantidade = expedicao.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, tipoPlano);
            if (quantidade != 0) {
                parcelas.add(parcelaDistribuicao(
                        expedicao, Constantes.FirmePlanejado.PLANEJADO, quantidade, quantidade, conversoes, unidade));
            }
        }
        return parcelas;

    }

    private static List<CorteProporcional.Parcela> parcelasTransferencias(
            Collection<DistributionPlanItem> transferencias, List<Constantes.FirmePlanejado> tiposTransferencia,
            UnidadeMedidaProjection conversoes,
            UnidadeMedida unidade, boolean paraAtendimentoDemanda) {

        var tipoPlano = Constantes.TipoPlano.PLANO_RESTRITO;
        List<CorteProporcional.Parcela> parcelas = new ArrayList<>();
        for (DistributionPlanItem transferencia : transferencias) {
            for (var tipo : tiposTransferencia) {
                double quantidade = transferencia.getQuantidade(tipo, tipoPlano);
                double atendimento = Math.min(quantidade,
                        transferencia.getParcelaParaAtendimentoIndiretoDemandaDireta(tipo, tipoPlano));
                double parcela = paraAtendimentoDemanda ? atendimento : quantidade - atendimento;
                if (parcela != 0) {
                    parcelas.add(parcelaDistribuicao(transferencia, tipo, quantidade, parcela, conversoes, unidade));
                }
            }
        }
        return parcelas;

    }

    private static CorteProporcional.Parcela parcelaDistribuicao(
            DistributionPlanItem linha, Constantes.FirmePlanejado tipo, double quantidade, double parcela,
            UnidadeMedidaProjection conversoes, UnidadeMedida unidade) {

        double fatorConversao = conversoes.getConversaoParaUnidadeDestino(
                linha.getProduto(), linha.getUnidadeMedida(conversoes.getParametrosGlobais()), unidade);
        double parcelaConvertida = parcela * fatorConversao;
        return new CorteProporcional.Parcela(parcelaConvertida, fracao -> linha.setQuantidade(
                quantidade - parcela * fracao, tipo, Constantes.TipoPlano.PLANO_RESTRITO));

    }

    private static void arredondaTransferenciasInternas(
            SupplyPlanningProjection projection, int periodo, Produto material, UnidadeMedida unidade) {

        var tipoPlano = Constantes.TipoPlano.PLANO_RESTRITO;
        var rede = projection.getSupplyNetworkProjection();
        var malha = projection.getSupplyPlan().getVersaoMalha();
        var referencia = projection.getCalendario().getDataHorarioInicialPresente();
        var conversoes = projection.getConversaoUnidadeMedidaProjection();
        var transferencias = projection.getDistributionPlanItemOutboundQueueParaLocationsInternas(
                Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, periodo, material);
        for (DistributionPlanItem linha : transferencias) {
            double planejado = linha.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, tipoPlano);
            double firme = linha.getQuantidade(Constantes.FirmePlanejado.ORDEM, tipoPlano);
            double total = linha.getQuantidadeNaUnidadeMedidaTarget(
                    Constantes.FirmePlanejado.TOTAL, tipoPlano, unidade, conversoes);
            if (total <= 0) {
                continue;
            }
            double minimo = rede.getLoteMinimoTransporteNaUnidadeTarget(
                    malha, linha.getLocationOrigem(), linha.getLocationDestino(), material, unidade, referencia)
                    .orElse(0);
            double multiplo = rede.getMultiploTransporteNaUnidadeTarget(
                    malha, linha.getLocationOrigem(), linha.getLocationDestino(), material, unidade, referencia)
                    .orElse(0);
            double arredondado = total;
            if (total < minimo) {
                arredondado = 0;
            } else if (multiplo > 0) {
                arredondado = Math.floor(total / multiplo) * multiplo;
            }
            double proporcao = arredondado / total;
            linha.setQuantidade(planejado * proporcao, Constantes.FirmePlanejado.PLANEJADO, tipoPlano);
            linha.setQuantidade(firme * proporcao, Constantes.FirmePlanejado.ORDEM, tipoPlano);
        }
        limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(transferencias);

    }

    /**
     * Limita as parcelas de atendimento direto de clientes ao volume restrito
     * da respectiva ordem de distribuicao.
     *
     * <p>Sem este ajuste, uma restricao de estoque/capacidade pode reduzir o
     * inbound do cliente sem reduzir a medida de fulfilled demand, produzindo
     * um indicador maior que a propria expedicao.</p>
     */
    static void limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(
            Collection<DistributionPlanItem> distributionPlanItemsClientesFinais) {

        for (DistributionPlanItem distributionPlanItem : distributionPlanItemsClientesFinais) {
            for (Constantes.FirmePlanejado firmePlanejado : List.of(
                    Constantes.FirmePlanejado.PLANEJADO,
                    Constantes.FirmePlanejado.ORDEM)) {
                double quantidadeRestrita = distributionPlanItem.getQuantidade(
                        firmePlanejado,
                        Constantes.TipoPlano.PLANO_RESTRITO);
                double parcelaAtendimentoRestrita =
                        distributionPlanItem.getParcelaParaAtendimentoIndiretoDemandaDireta(
                                firmePlanejado,
                                Constantes.TipoPlano.PLANO_RESTRITO);

                distributionPlanItem.setParcelaParaAtendimentoDemandaDireta(
                        Math.min(parcelaAtendimentoRestrita, quantidadeRestrita),
                        firmePlanejado,
                        Constantes.TipoPlano.PLANO_RESTRITO);
            }
        }

    }
    
    /**
     * Restringe ordens produção e plano produção com base na disponibilidade de insumos
     * @return true se alguma restrição foi aplicada
     */
    public static boolean restringeSugestoesEOrdensProducaoNoProjectionPorDisponibilidadeInsumos(
            SupplyPlanningProjection supplyPlanningProjectionLowLevelCodeAtual,
            SupplyPlanningProjection supplyPlanningProjectionInsumos,
            int posicaoPeriodo) {

        CadastroEParametrosProjection cadastroEParametrosProjection = supplyPlanningProjectionLowLevelCodeAtual.getCadastroEParametrosProjection();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjectionLowLevelCodeAtual.getSupplyNetworkProjection();
        Location location = supplyPlanningProjectionLowLevelCodeAtual.getLocation();
                
        boolean restricaoAplicada = false;
                
        Set<Produto> materiaisInput = supplyPlanningProjectionLowLevelCodeAtual.getProductionPlanLinhaOutput(posicaoPeriodo).stream()
                .map(x -> x.getMateriaisInput(supplyNetworkProjection))
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());
        
        // Material output -> Material input que o restringe
        Map<Produto, Produto> mapaMaterialInputQueRestringeMaterialOutput = new HashMap();
        Map<Produto, Double> mapaQuantidadeRestringidaPorMaterialOutput = new HashMap();
        
        // LOOP PRINCIPAL : PELA RESTRIÇÃO, MATERIAL INPUT
        for (Produto materialInput : materiaisInput) {
            
            UnidadeMedida unidadeMedidaPadraoMaterialInput = cadastroEParametrosProjection.getSNPUnidadeMedidaPadrao(materialInput, location);
            
            Map<Produto,Double> quantidadeInputNecessariaTotalPorMaterialOutput = supplyPlanningProjectionLowLevelCodeAtual.getQuantidadeConsumidaMaterialInputPorMaterialOutput(
                    posicaoPeriodo, materialInput, Constantes.FirmePlanejado.TOTAL, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadraoMaterialInput);
            
            double quantidadeInputNecessariaOrdensProducao = supplyPlanningProjectionLowLevelCodeAtual.getQuantidadeMaterialInputConsumidoNoProductionPlan(posicaoPeriodo, materialInput, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadraoMaterialInput);
            double quantidadeInputNecessariaSugestoesProducao = supplyPlanningProjectionLowLevelCodeAtual.getQuantidadeMaterialInputConsumidoNoProductionPlan(posicaoPeriodo, materialInput, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadraoMaterialInput);
            double quantidadeInputNecessariaTotal = quantidadeInputNecessariaOrdensProducao + quantidadeInputNecessariaSugestoesProducao;

            double quantidadeInputDisponivelSemConsumoProducao = Math.max(0,
                    SupplyPlanning.getEstoqueProjetado(
                            supplyPlanningProjectionInsumos,posicaoPeriodo-1, posicaoPeriodo, materialInput, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadraoMaterialInput,
                            true, true, false, true)
                    + quantidadeInputNecessariaTotal);

            if (quantidadeInputDisponivelSemConsumoProducao >= quantidadeInputNecessariaTotal) continue;
            
            Set<Produto> materiaisOutput = supplyPlanningProjectionLowLevelCodeAtual.getProductionPlanLinhaInput(posicaoPeriodo, materialInput).stream()
                    .map(x -> x.getMaterialOutput())
                    .collect(Collectors.toSet());
            
            // ATUALIZA MAPAS QUE GERARÃO RESTRIÇÕES RAW MATERIAL
            for (Produto materialOutput : materiaisOutput) {
                
                UnidadeMedida unidadeMedidaPadraoMaterialOutput = cadastroEParametrosProjection.getSNPUnidadeMedidaPadrao(materialOutput, location);
                
                double percentualDisponibilidadeInsumo = quantidadeInputDisponivelSemConsumoProducao / quantidadeInputNecessariaTotal;

                // indica que este output está sendo restringido por este input
                // a ser usado para fazer amarração restrição estoque input -> restrição raw material output
                mapaMaterialInputQueRestringeMaterialOutput.put(materialOutput, materialInput);
                double quantidadeOutputOrdensProducao = supplyPlanningProjectionLowLevelCodeAtual.getQuantidadeMaterialOutputProduzidoDependenteDeMaterialInputEmProductionPlanLinhaInput(posicaoPeriodo, materialInput, materialOutput, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadraoMaterialOutput);
                double quantidadeOutputSugestoesProducao = supplyPlanningProjectionLowLevelCodeAtual.getQuantidadeMaterialOutputProduzidoDependenteDeMaterialInputEmProductionPlanLinhaInput(posicaoPeriodo, materialInput, materialOutput, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadraoMaterialOutput);
                double quantidadeOutputTotalJaParcialmenteRestrita = quantidadeOutputOrdensProducao + quantidadeOutputSugestoesProducao;
                double gapMaterialOutputGeradoPelaRestricaoMaterialInput = (1-percentualDisponibilidadeInsumo) * quantidadeOutputTotalJaParcialmenteRestrita;
                // acumula gap para material output no mapa (junto das restrições derivadas de outros inputs)
                double novoGapQuantidadeOutputTotal = mapaQuantidadeRestringidaPorMaterialOutput.getOrDefault(materialOutput, 0.0) + gapMaterialOutputGeradoPelaRestricaoMaterialInput;
                mapaQuantidadeRestringidaPorMaterialOutput.put(materialOutput, novoGapQuantidadeOutputTotal);
                
            }
            
            // CASO 1 : MATERIAL INPUT INSUFICIENTE PARA ORDENS DE PRODUÇÃO
            // ZERA SUGESTÕES PRODUÇÃO E ATENDE PARCIALMENTE AS ORDENS
            if (quantidadeInputDisponivelSemConsumoProducao < quantidadeInputNecessariaOrdensProducao) {
                restricaoAplicada = true;
                double percentualDisponibilidade = quantidadeInputDisponivelSemConsumoProducao / quantidadeInputNecessariaOrdensProducao;
                
                for (Produto materialOutput : materiaisOutput) {
                    // ATUALIZA PRODUCTION PLAN - RESTRITO
                    // atende 0% das sugestões de produção
                    setPercentualQuantidadeOriginalOutputNoProductionPlanLinha(
                            0, supplyPlanningProjectionLowLevelCodeAtual, posicaoPeriodo,
                            materialOutput, Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO);
                    
                    setPercentualQuantidadeOriginalOutputNoProductionPlanLinha(
                            percentualDisponibilidade, supplyPlanningProjectionLowLevelCodeAtual, posicaoPeriodo, 
                            materialOutput, Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.ORDEM);
                    
                    SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                            supplyPlanningProjectionLowLevelCodeAtual, posicaoPeriodo, materialOutput, Constantes.TipoPlano.PLANO_RESTRITO);
                }
            // CASO 2 : MATERIAL INPUT SUFICIENTE PARA ORDENS DE PRODUÇÃO E INSUFICIENTE PARA SUGESTÕES
            // ATENDE PARCIALMENTE SUGESTÕES PRODUÇÃO E ATENDE 100% DAS ORDENS
            } else if (quantidadeInputDisponivelSemConsumoProducao < quantidadeInputNecessariaOrdensProducao + quantidadeInputNecessariaSugestoesProducao) {
                restricaoAplicada = true;
                double quantidadeInputDisponivelAposConsumoOrdens = quantidadeInputDisponivelSemConsumoProducao - quantidadeInputNecessariaOrdensProducao;
                double percentualDisponibilidade = quantidadeInputDisponivelAposConsumoOrdens / quantidadeInputNecessariaSugestoesProducao;

                // atende 100% das ordens de produção e x% das sugestões de produção
                for (Produto materialOutput : materiaisOutput) {      
                    // ATUALIZA PRODUCTION PLAN - RESTRITO
                    setPercentualQuantidadeOriginalOutputNoProductionPlanLinha(
                            percentualDisponibilidade, supplyPlanningProjectionLowLevelCodeAtual, posicaoPeriodo, 
                            materialOutput, Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO);
                    
                    SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                            supplyPlanningProjectionLowLevelCodeAtual, posicaoPeriodo, materialOutput, Constantes.TipoPlano.PLANO_RESTRITO);
                }
            // NENHUMA RESTRIÇÃO DE INSUMOS ENCONTRADA : IR PARA PRÓXIMO PRODUTO
            } else {
                continue;
            }

        } // ---- FIM LOOP MATERIAIS INPUT

        return restricaoAplicada; // true se ao menos 1 restrição foi encontrada. faz com que plano supply seja salvo novamente
    }
    
    /**
     * Restringe ordens de produção e plano produção com base na disponibilidade de horas atual
     * (após dedução das horas consumidas por outros projections) e no consumo de horas dos
     * recursos produtivos
     * Ao final do método atualiza mapa com o consumo atual de capacidade por recurso
     * @param consumoCapacidadeAcumuladoPorRecursoPeriodo atualizado após a execução, adicionando consumo do projection. na 1a rodada pode ser passado um hashmap vazio, que será posteriormente populado
     * @return true se alguma restrição foi aplicada
     */
    public static boolean restringeSugestoesEOrdensProducaoNoProjectionPorCapacidadeProdutiva(
            SupplyPlanningProjection supplyPlanningProjection,
            BIProjectionCapacidadeProdutiva biProjectionCapacidadeProdutiva,
            int posicaoPeriodo,
            Map<RecursoProdutivo, Map<Integer, Double>> consumoCapacidadeAcumuladoPorRecursoPeriodo) {
        
        boolean restricaoAplicada = false;
        
        CadastroEParametrosProjection cadastroEParametrosProjection = supplyPlanningProjection.getCadastroEParametrosProjection();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        
        Location location = supplyPlanningProjection.getLocation();
        PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado().getTipoCapacidadeProdutiva();

        Set<RecursoProdutivo> recursosProdutivos = supplyNetworkProjection.getRecursoProdutivoAtivoSet(location);

        // LOOP PRINCIPAL : PELA RESTRIÇÃO, RECURSO PRODUTIVO
        for (RecursoProdutivo recursoProdutivo : recursosProdutivos) {
            // valor das horas disponíveis NÃO será reduzido ao longo das iterações
            double capacidadeDisponivel = biProjectionCapacidadeProdutiva.getCapacidadeEmQuantidadeOuHorasEmPosicaoPeriodo(
                    posicaoPeriodo, recursoProdutivo, BIProjectionCapacidadeProdutiva.MasterOrPlanningData.MASTER_DATA); // = getCapacidadeRecursoHoras(calendario, recursoProdutivo, posicaoPeriodo);
            
            // consumo de horas acumulado de projections já processados anteriormente
            // acumulado do que já foi calculado para outras DFUs e low level codes
            double consumoCapacidadeAcumulado = consumoCapacidadeAcumuladoPorRecursoPeriodo
                    .computeIfAbsent(recursoProdutivo, x -> new HashMap<>())
                    .computeIfAbsent(posicaoPeriodo, x -> 0.0);

            // consumo total de capacidade pelas ordens firmes do projection
            // IMPORTANTE : apenas considera a lista de produtos DESTE low level code!
            double consumoCapacidadeRecursoPelasOrdensNoProjection = supplyPlanningProjection.getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(
                    posicaoPeriodo, recursoProdutivo, 
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    Constantes.FirmePlanejado.ORDEM,
                    tipoCapacidadeProdutiva);
            double consumoCapacidadeRecursoPelasSugestoesNoProjection = supplyPlanningProjection.getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(
                    posicaoPeriodo, recursoProdutivo, 
                    Constantes.TipoPlano.PLANO_RESTRITO,
                    Constantes.FirmePlanejado.PLANEJADO,
                    tipoCapacidadeProdutiva);

            Set<Produto> materiaisComProductionPlanLinha = supplyPlanningProjection.getProductionPlanLinhaOutput(posicaoPeriodo, recursoProdutivo).stream()
                    .map(x -> x.getMaterialOutput())
                    .collect(Collectors.toSet());
            
            // ATUALIZA RESTRICOES OBSERVADAS DE CAPACIDADE NO PROJECTION ----------------------------
            double capacidadeRestante = capacidadeDisponivel - consumoCapacidadeAcumulado;
            double consumoCapacidadeRecursoPelosMateriaisProjection = consumoCapacidadeRecursoPelasOrdensNoProjection + consumoCapacidadeRecursoPelasSugestoesNoProjection;
            if (capacidadeRestante < consumoCapacidadeRecursoPelosMateriaisProjection) {
                double percentualDemandaPeloRecursoAtendida = capacidadeRestante / consumoCapacidadeRecursoPelosMateriaisProjection;
                for (Produto material : materiaisComProductionPlanLinha) {
                    
                    UnidadeMedida unidadeMedidaPadrao = cadastroEParametrosProjection.getSNPUnidadeMedidaPadrao(
                            material, location);
                    double quantidadeOutputMaterialOrdens = supplyPlanningProjection.getQuantidadeProductionPlan(
                            posicaoPeriodo, material, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.ORDEM, unidadeMedidaPadrao);
                    double quantidadeOutputMaterialPlanejado = supplyPlanningProjection.getQuantidadeProductionPlan(
                            posicaoPeriodo, material, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO, unidadeMedidaPadrao);
                    double quantidadeProducaoMaterialNoRecursoProdutivo = quantidadeOutputMaterialOrdens + quantidadeOutputMaterialPlanejado;

                    double quantidadeRestringida = (1 - percentualDemandaPeloRecursoAtendida) * quantidadeProducaoMaterialNoRecursoProdutivo;
                    
                }
            }
            
            // HIPOTESE 1 : AO CHEGAR NESTE PROJECTION A CAPACIDADE JA HAVIA SIDO CONSUMIDA POR OUTROS PROJECTIONS ----------------------------
            // seta ordens firmes / planejadas como 0 no plano restrito
            if (consumoCapacidadeAcumulado > capacidadeDisponivel) {
                restricaoAplicada = true;
                for (Produto materialComProductionPlanLinha : materiaisComProductionPlanLinha) {
                    // 1.1 : ORDENS FIRMES NO PLANO RESTRITO = 0
                    setPercentualQuantidadeOriginalOutputProductionPlanLinha(
                            0, supplyPlanningProjection, posicaoPeriodo, 
                            materialComProductionPlanLinha, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.ORDEM);
                    // 1.2 : ORDENS PLANEJADAS NO PLANO RESTRITO = 0
                    setPercentualQuantidadeOriginalOutputProductionPlanLinha(
                            0, supplyPlanningProjection, posicaoPeriodo, 
                            materialComProductionPlanLinha, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO);
                    // 1.3 : ATUALIZA ESTOQUES PROJETADOS MATERIAL OUTPUT E MATERIAIS INPUT
                    SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                            supplyPlanningProjection, posicaoPeriodo, materialComProductionPlanLinha, Constantes.TipoPlano.PLANO_RESTRITO);
                    
                    // atualiza estoque projetado restrito dos insumos
                    Set<Produto> materiaisInput = supplyPlanningProjection.getProductionPlanLinhaOutput(posicaoPeriodo, materialComProductionPlanLinha).stream()
                            .map(x -> x.getMateriaisInput(supplyNetworkProjection))
                            .flatMap(x -> x.stream())
                            .collect(Collectors.toSet());
                    for (Produto materialInput : materiaisInput) {
                        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                                supplyPlanningProjection, posicaoPeriodo, materialInput, Constantes.TipoPlano.PLANO_RESTRITO);
                    }
                    
                }
            // HIPOTESE 2 : ATENDIMENTO PARCIAL DAS ORDENS FIRMES DE PRODUÇÃO ----------------------------
            // nesse caso se atende parcialmente as ordens firmes E não se atende nada das sugestões
            } else if (consumoCapacidadeAcumulado + consumoCapacidadeRecursoPelasOrdensNoProjection > capacidadeDisponivel) {
                restricaoAplicada = true;
                double percentualConsumoCapacidadeRecursoPelasOrdensNoProjectionAtendido = (capacidadeDisponivel - consumoCapacidadeAcumulado) / consumoCapacidadeRecursoPelasOrdensNoProjection;
                for (Produto materialComProductionPlanLinha : materiaisComProductionPlanLinha) {
                    // 2.1 : RESTRINGE ORDENS FIRMES : PRODUCAO = % HORAS DISPONIVEIS
                    setPercentualQuantidadeOriginalOutputProductionPlanLinha(
                            percentualConsumoCapacidadeRecursoPelasOrdensNoProjectionAtendido, supplyPlanningProjection, posicaoPeriodo, 
                            materialComProductionPlanLinha, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.ORDEM);
                    // 2.2 : ORDENS PLANEJADAS NO PLANO RESTRITO = 0
                    setPercentualQuantidadeOriginalOutputProductionPlanLinha(
                            0, supplyPlanningProjection, posicaoPeriodo, 
                            materialComProductionPlanLinha, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO);
                    // 2.3 : ATUALIZA ESTOQUES PROJETADOS MATERIAL OUTPUT E MATERIAIS INPUT
                    SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                            supplyPlanningProjection, posicaoPeriodo, materialComProductionPlanLinha, Constantes.TipoPlano.PLANO_RESTRITO);
                    
                    Set<Produto> materiaisInput = supplyPlanningProjection.getProductionPlanLinhaOutput(posicaoPeriodo, materialComProductionPlanLinha).stream()
                            .map(x -> x.getMateriaisInput(supplyNetworkProjection))
                            .flatMap(x -> x.stream())
                            .collect(Collectors.toSet());
                    for (Produto materialInput : materiaisInput) {
                        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                                supplyPlanningProjection, posicaoPeriodo, materialInput, Constantes.TipoPlano.PLANO_RESTRITO);
                    }
                }
                
            // HIPOTESE 3 : ATENDIMENTO PARCIAL DAS ORDENS PLANEJADAS DE PRODUÇÃO ----------------------------
            // nesse caso se atende parcialmente as ordens planejadas
            } else if (consumoCapacidadeAcumulado + consumoCapacidadeRecursoPelasOrdensNoProjection + consumoCapacidadeRecursoPelasSugestoesNoProjection> capacidadeDisponivel) {
                restricaoAplicada = true;
                double percentualConsumoCapacidadeRecursoPelasSugestoesNoProjectionAtendido = (capacidadeDisponivel - consumoCapacidadeAcumulado - consumoCapacidadeRecursoPelasOrdensNoProjection) / consumoCapacidadeRecursoPelasSugestoesNoProjection;
                for (Produto materialComProductionPlanLinha : materiaisComProductionPlanLinha) {
                    // 3.1 : RESTRINGE ORDENS PLANEJADAS : PRODUCAO = % HORAS DISPONIVEIS
                    setPercentualQuantidadeOriginalOutputProductionPlanLinha(
                            percentualConsumoCapacidadeRecursoPelasSugestoesNoProjectionAtendido, supplyPlanningProjection, posicaoPeriodo, 
                            materialComProductionPlanLinha, recursoProdutivo, 
                            Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO);
                    // 3.2 : ATUALIZA ESTOQUES PROJETADOS MATERIAL OUTPUT E MATERIAIS INPUT
                    SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                            supplyPlanningProjection, posicaoPeriodo, materialComProductionPlanLinha, Constantes.TipoPlano.PLANO_RESTRITO);
                    
                    Set<Produto> materiaisInput = supplyPlanningProjection.getProductionPlanLinhaOutput(posicaoPeriodo, materialComProductionPlanLinha).stream()
                            .map(x -> x.getMateriaisInput(supplyNetworkProjection))
                            .flatMap(x -> x.stream())
                            .collect(Collectors.toSet());
                    for (Produto materialInput : materiaisInput) {
                        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                                supplyPlanningProjection, posicaoPeriodo, materialInput, Constantes.TipoPlano.PLANO_RESTRITO);
                    }
                }
            }
            
            // atualiza consumo em horas ou quantidade do recurso produtivo até o momento
            consumoCapacidadeAcumuladoPorRecursoPeriodo.get(recursoProdutivo).replace(posicaoPeriodo, 
                    consumoCapacidadeAcumuladoPorRecursoPeriodo.get(recursoProdutivo).get(posicaoPeriodo)
                    + supplyPlanningProjection.getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(posicaoPeriodo, recursoProdutivo, Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.PLANEJADO, tipoCapacidadeProdutiva)
                    + supplyPlanningProjection.getConsumoCapacidadeEmQuantidadeOuHorasProductionPlan(posicaoPeriodo, recursoProdutivo, Constantes.TipoPlano.PLANO_RESTRITO, Constantes.FirmePlanejado.ORDEM, tipoCapacidadeProdutiva));
        }
        
        return restricaoAplicada; // true se ao menos 1 restrição foi encontrada. faz com que plano supply seja salvo novamente
    
    }    
    
}
