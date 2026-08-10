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
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.production.productionresource.projection.BIProjectionCapacidadeProdutiva;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.MetodosUtilidade;

import java.util.*;
import java.util.function.ToDoubleFunction;
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
    
    /**
     * Restringe distribution plan outbound e demand plan em caso de indisponibilidade de estoque
     * @return true se alguma restrição foi aplicada
     */
    public static boolean restringeDistributionPlanOutboundEDemandPlan(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo) {

        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();
        LocationProjection locationProjectionLocationsOrigem = supplyPlanningProjection.getLocationProjectionLocationsOrigem();
        UnidadeMedidaProjection unidadeMedidaProjection = supplyPlanningProjection.getConversaoUnidadeMedidaProjection();
        ClusterEParametrosProjection parametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();
        
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
            if (estoqueProjetadoTotal < 0) {
                houveRestricaoPlano = true;
                
                double demandaNaoAtendidaRestante = Math.abs(estoqueProjetadoTotal);
                                
                // ATUALIZA RESTRICOES OBSERVADAS RELATIVAS A REQS E PEDIDOS OUTBOUND (DEMANDA INDIRETA)
                // IMPACTARÃO EXECUÇÃO DOS PRÓXIMOS LOW LEVEL CODES
                double demandaIndireta = getDemandaIndireta(supplyPlanningProjection, posicaoPeriodo, material, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                if (demandaIndireta > 0) {
                    Set<Location> locationsOutboundMaterialNoPlano = supplyPlanningProjection.getLocationsOutboundDeDistributionPlansOutbound(Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material);
                    for (Location locationDestino : locationsOutboundMaterialNoPlano) {
                        
                        // clientes não fazem parte da demanda indireta! entram mais adiante como componente da demanda direta
                        if (locationDestino.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL)
                                || locationDestino.getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL)) continue;
                        
                        double quantidadePedidosOutboundJaRestritosParaLocationDestino = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaLocationsInternas(Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, locationDestino, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                        double quantidadeRequisicoesOutboundJaRestritasParaLocationDestino = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaLocationsInternas(Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, locationDestino, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                        double demandaIndiretaParaLocationDestino = quantidadePedidosOutboundJaRestritosParaLocationDestino + quantidadeRequisicoesOutboundJaRestritasParaLocationDestino;

                        double gapObservadoParaMaterialLocationDestino = demandaIndiretaParaLocationDestino / demandaIndireta * Math.min(demandaIndireta, demandaNaoAtendidaRestante);
                                                
                    }
                }
                
                // RESTRINGE OUTBOUNDS E PARCELAS DE ATENDIMENTO A DEMANDA DIRETA ----------------
                
                Collection<DistributionPlanItem> distributionPlanItemsOutboundMalhaInterna = supplyPlanningProjection.getDistributionPlanItemOutboundQueueParaLocationsInternas(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material);
                Collection<DistributionPlanItem> distributionPlanItemsOutboundClientesFinais = supplyPlanningProjection.getDistributionPlanItemOutboundQueueParaClientes(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material);

                // Fração dos outbounds internos dedicada ao atendimento indireto da demanda direta.
                // No Community, os componentes de carteira/pedidos ficam zerados; o caminho existe
                // para preservar o mesmo algoritmo heuristico usado sobre a tabela compartilhada.
                double parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaAtendimentoDemandaDireta(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                double parcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaAtendimentoDemandaDireta(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                // Canal transacional Enterprise. No Community, esta parcela deve estar zerada.
                double quantidadeCarteiraTransicional =
                        supplyPlanningProjection.getDemandaDiretaConsideradaProjection().getQuantidadeOriginal(
                                location, material, posicaoPeriodo,
                                DemandaDiretaConsideradaLinha.TipoDemandaDireta.CARTEIRA,
                                DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta.TOTAL,
                                unidadeMedidaPadrao);
                double quantidadeOrdensFirmesOutboundAtendimentoClientes = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaClientes(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);

                double quantidadeOutboundAtendimentoDemandaDireta =
                        + parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta
                        + parcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta
                        + quantidadeCarteiraTransicional
                        + quantidadeOrdensFirmesOutboundAtendimentoClientes;
                
                double quantidadeOutboundRequisicoesTotalMalhaInterna = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaLocationsInternas(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                double quantidadeOutboundPedidosTotalMalhaInterna = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaLocationsInternas(
                        Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                double quantidadeOutboundRequisicoesEPedidosTotalMalhaInterna = quantidadeOutboundRequisicoesTotalMalhaInterna + quantidadeOutboundPedidosTotalMalhaInterna;
                                
                // demanda direta: propagação do DP em clientes finais
                double quantidadeOrdensPlanejadasOutboundAtendimentoClientes = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaClientes(
                            Constantes.ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO, unidadeMedidaPadrao);
                
                // 1o volume a ser restringido
                double parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoFormacaoEstoque = Math.max(0, quantidadeOutboundRequisicoesTotalMalhaInterna - parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta);
                double parcelaOrdensFirmesTransferenciaOutboundAtendimentoFormacaoEstoque = Math.max(0, quantidadeOutboundPedidosTotalMalhaInterna - parcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta);
                double quantidadeOutboundFormacaoEstoque = parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoFormacaoEstoque + parcelaOrdensFirmesTransferenciaOutboundAtendimentoFormacaoEstoque;
                // 2o volume a ser restringido: outbounds planejados para clientes finais.
                // 3o volume a ser restringido: atendimento indireto da demanda direta.
                // 1) RESTRINGE PARCELA DE REQUISIÇÕES + PEDIDOS OUTBOUND PARA FORMAÇÃO DE ESTOQUE NOS DESTINOS (MALHA INTERNA) ---------------------------
                if (demandaNaoAtendidaRestante > 0 && quantidadeOutboundFormacaoEstoque > 0) {
                    
                    double modificacaoRequisicoesEPedidos = Math.min(quantidadeOutboundFormacaoEstoque, demandaNaoAtendidaRestante);
                    
                    double modificacaoRequisicoes = modificacaoRequisicoesEPedidos * parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoFormacaoEstoque / quantidadeOutboundFormacaoEstoque;
                    double modificacaoPedidos = modificacaoRequisicoesEPedidos * parcelaOrdensFirmesTransferenciaOutboundAtendimentoFormacaoEstoque / quantidadeOutboundFormacaoEstoque;
                                        
                    // As modificacoes sao aplicadas sobre a parcela ainda nao dedicada
                    // ao atendimento indireto da demanda direta.
                    ToDoubleFunction<DistributionPlanItem> funcaoReferenciaRequisicoes = x -> x.getQuantidadeNaUnidadeMedidaTarget(
                            y -> y.getQuantidade(
                                    Constantes.FirmePlanejado.PLANEJADO,
                                    Constantes.TipoPlano.PLANO_RESTRITO)
                                    - y.getParcelaParaAtendimentoIndiretoDemandaDireta(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO),
                            unidadeMedidaProjection, unidadeMedidaPadrao);
                    ToDoubleFunction<DistributionPlanItem> funcaoReferenciaPedidos = x -> x.getQuantidadeNaUnidadeMedidaTarget(
                            y -> y.getQuantidade(
                                    Constantes.FirmePlanejado.ORDEM,
                                    Constantes.TipoPlano.PLANO_RESTRITO)
                                    - y.getParcelaParaAtendimentoIndiretoDemandaDireta(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO),
                            unidadeMedidaProjection, unidadeMedidaPadrao);

                    // A modificacao segue a proporcao indicada nas funcoes de referencia;
                    // o BiFunction final grava a nova quantidade na linha correspondente.
                    MetodosUtilidade.modificaValorProporcional(
                            -modificacaoRequisicoes, distributionPlanItemsOutboundMalhaInterna, 
                            funcaoReferenciaRequisicoes,
                            x -> x.getQuantidade(
                                    Constantes.FirmePlanejado.PLANEJADO,
                                    Constantes.TipoPlano.PLANO_RESTRITO),
                            (x, valorAtualizacao) -> x.setQuantidade(
                                    valorAtualizacao,
                                    Constantes.FirmePlanejado.PLANEJADO,
                                    Constantes.TipoPlano.PLANO_RESTRITO));
                    MetodosUtilidade.modificaValorProporcional(
                            -modificacaoPedidos, distributionPlanItemsOutboundMalhaInterna, 
                            funcaoReferenciaPedidos,
                            x -> x.getQuantidade(
                                    Constantes.FirmePlanejado.ORDEM,
                                    Constantes.TipoPlano.PLANO_RESTRITO),
                            (x, valorAtualizacao) -> x.setQuantidade(
                                    valorAtualizacao,
                                    Constantes.FirmePlanejado.ORDEM,
                                    Constantes.TipoPlano.PLANO_RESTRITO));
                    
                    // atualiza variáveis
                    quantidadeOutboundRequisicoesTotalMalhaInterna -= modificacaoRequisicoes;
                    quantidadeOutboundPedidosTotalMalhaInterna -= modificacaoPedidos;
                    quantidadeOutboundRequisicoesEPedidosTotalMalhaInterna -= modificacaoRequisicoesEPedidos;
                    demandaNaoAtendidaRestante -= modificacaoRequisicoesEPedidos;
                    
                }

                // 2) RESTRINGE PARCELA DE OUTBOUNDS PLANEJADOS PARA ATENDIMENTO DO PLANO DE DEMANDA EM CLIENTES FINAIS ---------------------------
                
                // A parcela planejada para clientes finais e restringida diretamente,
                // sempre proporcional ao volume planejado por linha.
                ToDoubleFunction<DistributionPlanItem> funcaoReferenciaOrdensPlanejadasClientesFinais = x -> x.getQuantidadeNaUnidadeMedidaTarget(
                        y -> y.getQuantidade(
                                Constantes.FirmePlanejado.PLANEJADO,
                                Constantes.TipoPlano.PLANO_RESTRITO),
                        unidadeMedidaProjection, unidadeMedidaPadrao);

                if (demandaNaoAtendidaRestante > 0 && quantidadeOrdensPlanejadasOutboundAtendimentoClientes > 0) {
                    double modificacaoOrdensPlanejadasOutboundAtendimentoClientes = Math.min(quantidadeOrdensPlanejadasOutboundAtendimentoClientes, demandaNaoAtendidaRestante);

                    // A modificacao segue a proporcao indicada nas funcoes de referencia;
                    // o BiFunction final grava a nova quantidade na linha correspondente.
                    MetodosUtilidade.modificaValorProporcional(
                            -modificacaoOrdensPlanejadasOutboundAtendimentoClientes, distributionPlanItemsOutboundClientesFinais,
                            funcaoReferenciaOrdensPlanejadasClientesFinais,
                            x -> x.getQuantidade(
                                    Constantes.FirmePlanejado.PLANEJADO,
                                    Constantes.TipoPlano.PLANO_RESTRITO),
                            (x, valorAtualizacao) -> x.setQuantidade(
                                    valorAtualizacao,
                                    Constantes.FirmePlanejado.PLANEJADO,
                                    Constantes.TipoPlano.PLANO_RESTRITO));

                    // atualiza variáveis
                    quantidadeOrdensPlanejadasOutboundAtendimentoClientes -= modificacaoOrdensPlanejadasOutboundAtendimentoClientes;
                    demandaNaoAtendidaRestante -= modificacaoOrdensPlanejadasOutboundAtendimentoClientes;
                }
                    
                // 3) RESTRINGE OUTBOUNDS FIRMES PARA CLIENTES E PARCELAS INTERNAS DEDICADAS A DEMANDA DIRETA
                double modificacaoTotalAtendimentoDemandaDireta = Math.min(quantidadeOutboundAtendimentoDemandaDireta, demandaNaoAtendidaRestante);

                double modificacaoParcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta = modificacaoTotalAtendimentoDemandaDireta * parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta / quantidadeOutboundAtendimentoDemandaDireta;
                double modificacaoParcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta = modificacaoTotalAtendimentoDemandaDireta * parcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta / quantidadeOutboundAtendimentoDemandaDireta;
                double modificacaoOrdensFirmesOutboundAtendimentoClientes = modificacaoTotalAtendimentoDemandaDireta * quantidadeOrdensFirmesOutboundAtendimentoClientes / quantidadeOutboundAtendimentoDemandaDireta;
                double modificacaoCarteiraTransicional = modificacaoTotalAtendimentoDemandaDireta * quantidadeCarteiraTransicional / quantidadeOutboundAtendimentoDemandaDireta;

                if (modificacaoTotalAtendimentoDemandaDireta > 0) {

                    // Esta etapa reduz as parcelas internas e firmes dedicadas ao atendimento
                    // direto. No Community, a parcela de carteira transicional fica zerada.
                    ToDoubleFunction<DistributionPlanItem> funcaoReferenciaRequisicoesInternas = x -> x.getQuantidadeNaUnidadeMedidaTarget(
                            y -> y.getParcelaParaAtendimentoIndiretoDemandaDireta(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO),
                            unidadeMedidaProjection, unidadeMedidaPadrao);
                    ToDoubleFunction<DistributionPlanItem> funcaoReferenciaPedidosInternos = x -> x.getQuantidadeNaUnidadeMedidaTarget(
                            y -> y.getParcelaParaAtendimentoIndiretoDemandaDireta(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO),
                            unidadeMedidaProjection, unidadeMedidaPadrao);
                    ToDoubleFunction<DistributionPlanItem> funcaoReferenciaOrdensFirmesClientesFinais = x -> x.getQuantidadeNaUnidadeMedidaTarget(
                            y -> y.getQuantidade(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO),
                            unidadeMedidaProjection, unidadeMedidaPadrao);

                    // A modificacao segue a proporcao indicada nas funcoes de referencia;
                    // o BiFunction final grava a nova quantidade na linha correspondente.
                    MetodosUtilidade.modificaValorProporcional(
                            -modificacaoParcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta,
                            distributionPlanItemsOutboundMalhaInterna, // aplica a modificação em múltiplos distribution plan linha
                            funcaoReferenciaRequisicoesInternas,
                            x -> x.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO),
                            (x, valorAtualizacao) -> x.setQuantidade(valorAtualizacao, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO));
                    MetodosUtilidade.modificaValorProporcional(
                            -modificacaoParcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta,
                            distributionPlanItemsOutboundMalhaInterna, // aplica a modificação em múltiplos distribution plan linha
                            funcaoReferenciaPedidosInternos,
                            x -> x.getQuantidade(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO),
                            (x, valorAtualizacao) -> x.setQuantidade(valorAtualizacao, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO));
                    MetodosUtilidade.modificaValorProporcional(
                            -modificacaoOrdensFirmesOutboundAtendimentoClientes,
                            distributionPlanItemsOutboundClientesFinais, // aplica a modificação em múltiplos distribution plan linha
                            funcaoReferenciaOrdensFirmesClientesFinais,
                            x -> x.getQuantidade(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO),
                            (x, valorAtualizacao) -> x.setQuantidade(valorAtualizacao, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO));

                    if (quantidadeCarteiraTransicional > 0) {
                        double percentualDemandaAtendidaRestante =
                                (quantidadeCarteiraTransicional - modificacaoCarteiraTransicional)
                                        / quantidadeCarteiraTransicional;

                        /*
                         * A reducao aqui pertence ao canal transacional de
                         * carteira/client orders. Community mantem esse canal
                         * zerado, mas a rotina compartilhada preserva o update
                         * segmentado para nao reabrir escrita em demanda total
                         * agregada.
                         */
                        supplyPlanningProjection.getDemandaDiretaConsideradaProjection()
                                .updateQuantidadeDemandaDiretaConsideradaSegregada(
                                        quantidadeOriginal -> quantidadeOriginal * percentualDemandaAtendidaRestante,
                                        unidadeMedidaPadrao,
                                        location,
                                        material,
                                        posicaoPeriodo,
                                        DemandaDiretaConsideradaLinha.TipoDemandaDireta.CARTEIRA,
                                        Constantes.TipoPlano.PLANO_RESTRITO);
                    }

                    
                    // Atualiza a parcela das requisições/pedidos internos usada para atendimento
                    // indireto da demanda direta.
                    for (DistributionPlanItem distributionPlanItem : distributionPlanItemsOutboundMalhaInterna) {
                        // requisições
                        distributionPlanItem.setParcelaParaAtendimentoDemandaDireta(
                                Math.min(
                                        distributionPlanItem.getParcelaParaAtendimentoIndiretoDemandaDireta(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO),
                                        distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO)),
                                Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO);
                        // pedidos
                        distributionPlanItem.setParcelaParaAtendimentoDemandaDireta(
                                Math.min(
                                        distributionPlanItem.getParcelaParaAtendimentoIndiretoDemandaDireta(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO),
                                        distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO)),
                                Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO);
                    }
                    
                    // atualiza variáveis
                    parcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta -= modificacaoParcelaOrdensPlanejadasTransferenciaOutboundAtendimentoDemandaDireta;
                    parcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta -= modificacaoParcelaOrdensFirmesTransferenciaOutboundAtendimentoDemandaDireta;
                    quantidadeCarteiraTransicional -= modificacaoCarteiraTransicional;
                    quantidadeOutboundAtendimentoDemandaDireta -= modificacaoTotalAtendimentoDemandaDireta;
                    demandaNaoAtendidaRestante -= modificacaoTotalAtendimentoDemandaDireta;

                }

                /*
                 * Os outbounds para clientes podem ter sido reduzidos tanto na
                 * etapa planejada quanto na etapa firme. A parcela de
                 * atendimento e uma decomposicao da propria ordem e nunca pode
                 * permanecer acima do volume restrito efetivamente expedido.
                 */
                limitaParcelasAtendimentoDemandaDiretaAoVolumeRestrito(
                        distributionPlanItemsOutboundClientesFinais);

                // ARREDONDA PARA LOTE MINIMO E MULTIPLO
                double modificacaoLoteMinimo = 0;
                double modificacaoMultiplo = 0;
                if (houveRestricaoPlano) {
                    if (perfilExecucaoSupplyPlan.getArredondaRequisicoesLoteMinimoEMultiplo()) {

                        for (DistributionPlanItem distributionPlanItem : distributionPlanItemsOutboundMalhaInterna) {
                            OptionalDouble optionalLoteMinimo = supplyNetworkProjection.getLoteMinimoTransporteNaUnidadeTarget(
                                    versaoMalha, distributionPlanItem.getLocationOrigem(), distributionPlanItem.getLocationDestino(), material, 
                                    unidadeMedidaPadrao, calendario.getDataHorarioInicialPresente());
                            OptionalDouble optionalMultiplo = supplyNetworkProjection.getMultiploTransporteNaUnidadeTarget(
                                    versaoMalha, distributionPlanItem.getLocationOrigem(), distributionPlanItem.getLocationDestino(), material, 
                                    unidadeMedidaPadrao, calendario.getDataHorarioInicialPresente());

                            if (optionalLoteMinimo.isPresent()) {

                                double valorRequisicoes = distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO);
                                double valorPedidos = distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO);

                                if (valorPedidos + valorRequisicoes < optionalLoteMinimo.getAsDouble()) {
                                    distributionPlanItem.setQuantidade(0, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO);
                                    distributionPlanItem.setQuantidade(0, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO);

                                    modificacaoLoteMinimo = -(valorRequisicoes + valorPedidos);
                                }

                            }

                            if (optionalMultiplo.orElse(0) > 0) {

                                double valorOrdensTransferenciaPlanejadas = distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO);
                                double valorOrdensTransferenciaFirmes = distributionPlanItem.getQuantidade(Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO);

                                if (valorOrdensTransferenciaPlanejadas + valorOrdensTransferenciaFirmes <= 0) {
                                    
                                    distributionPlanItem.setQuantidade(0, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO);
                                    distributionPlanItem.setQuantidade(0, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO);
                                
                                } else {
                                    
                                    double multiploInferior = Math.floor((valorOrdensTransferenciaPlanejadas + valorOrdensTransferenciaFirmes) / optionalMultiplo.getAsDouble()) * optionalMultiplo.getAsDouble();

                                    double requisicoesComArredondamentoMultiplo = valorOrdensTransferenciaPlanejadas / (valorOrdensTransferenciaPlanejadas + valorOrdensTransferenciaFirmes) * multiploInferior;
                                    double pedidosComArredondamentoMultiplo = valorOrdensTransferenciaFirmes / (valorOrdensTransferenciaPlanejadas + valorOrdensTransferenciaFirmes) * multiploInferior;

                                    distributionPlanItem.setQuantidade(requisicoesComArredondamentoMultiplo, Constantes.FirmePlanejado.PLANEJADO, Constantes.TipoPlano.PLANO_RESTRITO);
                                    distributionPlanItem.setQuantidade(pedidosComArredondamentoMultiplo, Constantes.FirmePlanejado.ORDEM, Constantes.TipoPlano.PLANO_RESTRITO);

                                    modificacaoMultiplo = -(valorOrdensTransferenciaPlanejadas + valorOrdensTransferenciaFirmes);
                                
                                }

                            }

                        }
                    }
                }
            }
            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                    supplyPlanningProjection, posicaoPeriodo, material, Constantes.TipoPlano.PLANO_RESTRITO);
        }
        return houveRestricaoPlano;
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

        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningProjectionLowLevelCodeAtual.getClusterEParametrosProjection();
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
            
            UnidadeMedida unidadeMedidaPadraoMaterialInput = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(materialInput, location);
            
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
                
                UnidadeMedida unidadeMedidaPadraoMaterialOutput = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(materialOutput, location);
                
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
        
        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();
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
                    
                    UnidadeMedida unidadeMedidaPadrao = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(
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
