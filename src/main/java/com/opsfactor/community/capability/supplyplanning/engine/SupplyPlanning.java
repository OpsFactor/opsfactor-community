package com.opsfactor.community.capability.supplyplanning.engine;

import com.google.common.collect.Sets;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan.ModeloEstoqueTarget;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.projection.EstoqueProjectionProduto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.IncompatibleCalendarException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.*;
import com.pivovarit.function.ThrowingFunction;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rotinas puras do plano heuristico de Supply Planning Community.
 *
 * <p>A classe manipula projections ja carregadas pelos services e nao acessa
 * banco ou beans Spring diretamente. Otimizacao, custos, P&L, frotas,
 * process chains e analises privadas devem entrar pelos overlays Enterprise.</p>
 */
public class SupplyPlanning {

    /**
     * Ajusta o valor das requisicoes outbound no production plan de acordo com percentual especificado
     * Em caso de percentual menor que 1, a reducao e aplicada proporcionalmente
     * nas linhas existentes.
     * O ajuste negativo nao e concentrado em Ajuste Supply,
     * portanto o método não deve ser usado para processar inputs de usuários
     * Uso principal : aplicar redução da qtde atendida no plano restrito
     * @param percentualQuantidadeOriginal ex. 90% do valor original
     */
    public static void setPercentualQuantidadeOriginalInboundNoDistributionPlan(
            double percentualQuantidadeOriginal,
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo,
            Produto material,
            Constantes.TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado) {
        
        UnidadeMedida unidadeMedidaPadrao = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                material, supplyPlanningProjection.getLocation());
        
        double requisicoes = supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodo, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedidaPadrao);
        if (requisicoes == 0) return; // não há o que fazer com percentuais

        double quantidadeAReduzir = (1 - percentualQuantidadeOriginal) * requisicoes;
        double ajuste = Math.min(quantidadeAReduzir, Math.max(0, requisicoes));

        double percentualRequisicoes = (requisicoes - ajuste) / requisicoes;

        // Aplica o mesmo percentual a cada linha inbound planejada. No Community,
        // componentes Enterprise como uplift/materiais novos chegam zerados, mas as
        // colunas fisicas podem permanecer no schema transicional.
        Collection<DistributionPlanItem> distributionPlanItemsInbound = supplyPlanningProjection.getDistributionPlanInboundQueue(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodo, material);

        for (DistributionPlanItem distributionPlanItemInbound : distributionPlanItemsInbound) {
            double quantidadeAtual = distributionPlanItemInbound.getQuantidade(firmePlanejado, tipoPlano);
            distributionPlanItemInbound.setQuantidade(percentualRequisicoes * quantidadeAtual, firmePlanejado, tipoPlano);
        }

    }
    
    /**
     * Cascateia mudança no estoque total feita em nível agregado para cada um dos SKUs
     * Considera a cobertura de estoque calculada com base na demanda direta+idireta por período
     * Busca equilibrar a cobertura de estoque a partir do estoque de segurança (estoque de ciclo)
     *
     * <p>No Community, pedidos, sell-in e ordens firmes sao Enterprise e chegam
     * zerados pelos fluxos de service/persistencia. As referencias a firme x
     * planejado neste metodo existem por compatibilidade do schema e para manter
     * a rotina capaz de operar sobre os campos fisicos compartilhados sem
     * publicar firm orders como capacidade Community.</p>
     *
     * @return valor efetivo da modificação
     */
    public static double modificaEstoqueTotalAgregado(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo,
            Set<Produto> materiais, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaModificacao,
            boolean modificaProducaoPlanejada,
            boolean modificaRequisicoesInboundPlanejadas) {
    
        return modificaEstoqueTotalAgregado(
                modificacaoValorTotal,
                supplyPlanningProjection,
                ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                posicaoPeriodo, materiais, tipoPlano, 
                unidadeMedidaModificacao, modificaProducaoPlanejada, 
                modificaRequisicoesInboundPlanejadas, null);
        
    }
    
    /**
     * Conta lead time períodos a partir de posicaoPeriodoConsumoCapacidade para chegar ao primeiro período de reabastecimento do material
     * Usa o lead time padrão (rota inbound prioritária) no caso de requisições e 0 no caso de produção
     * @param material
     * @param location
     * @param posicaoPeriodoConsumoCapacidade
     * @param calendario
     * @param versaoMalha
     * @param consideraVersoesProducaoParalelas
     * @param locationProjection
     * @param materialProjection
     * @param locationsOrigemPossiveis
     * @param materiaisInputPossiveis
     * @param supplyNetworkProjection
     * @return 
     */
    public static int getPeriodoPadraoParaPrimeiraDisponibilizacaoMaterial(
            Produto material, Location location, int posicaoPeriodoConsumoCapacidade, 
            Calendario calendario,
            VersaoMalha versaoMalha,
            boolean consideraVersoesProducaoParalelas,
            LocationProjection locationProjection, // todas as locations consideradas no plano
            MaterialProjection materialProjection,
            @Nullable Set<Location> locationsOrigemPossiveis, // alternativas de inbound
            @Nullable Set<Produto> materiaisInputPossiveis,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        // se nulo, nenhum filtro será aplicado
        Set<Location> filtroLocationsOrigemConsiderado = locationsOrigemPossiveis != null ?
                locationsOrigemPossiveis
                : locationProjection.getLocationsAtivasOuNuloSeLocationProjectionCompleto();
        Set<Produto> filtroMateriaisInputConsiderado = materiaisInputPossiveis != null ?
                materiaisInputPossiveis
                : materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto();
        
        SNPOrigemReabastecimento tipoReabastecimento = supplyNetworkProjection.getTipoRessuprimento(
                versaoMalha, 
                location, 
                material, 
                calendario.getDataHorarioInicialPresente(), 
                consideraVersoesProducaoParalelas,
                filtroLocationsOrigemConsiderado,
                filtroMateriaisInputConsiderado);
        switch (tipoReabastecimento) {
            case REQUISICAO:
                Optional<Location> optionalLocationOrigemReferencia = supplyNetworkProjection.getLocationOrigemPrioritaria(
                        versaoMalha, 
                        location, 
                        material, 
                        calendario.getDataHorarioInicialPresente(),
                        filtroLocationsOrigemConsiderado);
                if (optionalLocationOrigemReferencia.isPresent()) {
                    Location locationOrigemReferencia = optionalLocationOrigemReferencia
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Location origem prioritaria deveria estar presente depois da checagem de Optional.isPresent()."));
                    int leadTimeDiasOrigemDestino = supplyNetworkProjection
                            .getLeadTimeDiasEntreOrigemDestinoParaMaterial(
                                    versaoMalha,
                                    locationOrigemReferencia,
                                    location,
                                    material,
                                    calendario.getDataHorarioInicialPresente())
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Linha de transporte prioritaria sem lead time em dias para material="
                                            + getMaterialId(material)
                                            + ", locationOrigem="
                                            + getLocationId(locationOrigemReferencia)
                                            + ", locationDestino="
                                            + getLocationId(location)
                                            + ". O offset de consumo de capacidade precisa de lead time materializado."));

                    return calendario.getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(
                            posicaoPeriodoConsumoCapacidade, 
                            leadTimeDiasOrigemDestino,
                            TamanhoBucket.DIARIO)
                            - posicaoPeriodoConsumoCapacidade;
                } else {
                    return posicaoPeriodoConsumoCapacidade;
                }
            case PRODUCAO:
                return posicaoPeriodoConsumoCapacidade;
            case SEM_RESSUPRIMENTO:
                return posicaoPeriodoConsumoCapacidade;
        }
        
        return posicaoPeriodoConsumoCapacidade;
        
    }
    
    /**
     * Cascateia mudança no estoque total feita em nível agregado para cada um dos SKUs
     * Considera a cobertura de estoque calculada com base na demanda direta+idireta por período
     * Busca equilibrar a cobertura de estoque a partir do estoque de segurança (estoque de ciclo)
     *
     * <p>No Community, pedidos, sell-in e ordens firmes sao Enterprise e chegam
     * zerados pelos fluxos de service/persistencia. As referencias a firme x
     * planejado neste metodo existem por compatibilidade do schema e para manter
     * a rotina capaz de operar sobre os campos fisicos compartilhados sem
     * publicar firm orders como capacidade Community.</p>
     *
     * @param referenciaPeriodo : no caso de requisições, se CONSUMO_CAPACIDADE a modificação das requisições
     * é feita com base na data de saída do material da origem. se DISPONIBILIZACAO_MATERIAL, a data
     * considerada é a entrada do material no destino
     * no caso da produção a regra é similar. se CONSUMO_CAPACIDADE a referência é feita ao período
     * onde o material será produzido e se DISPONIBILIZACAO_MATERIAL a referência é ao período de liberação
     * do material (ex. após tempo de cura/quarentena)
     * @param locationsOrigemConsideradasParaRequisicoes OPCIONAL. no caso de se modificarem requisições, determina quais origens são passíveis de alteração
     * @return valor efetivo da modificação
     */
    public static double modificaEstoqueTotalAgregado(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection,
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo, 
            Set<Produto> materiais,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaModificacao,
            boolean modificaProductionPlan,
            boolean modificaDistributionPlanInbound,
            Set<Location> locationsOrigemConsideradasParaRequisicoes) {
        
        Location location = supplyPlanningProjection.getLocation();
        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();
        LocationProjection locationProjectionLocationsOrigem = supplyPlanningProjection.getLocationProjectionLocationsOrigem();
        VersaoMalha versaoMalha = supplyPlanningProjection.getSupplyPlan().getVersaoMalha();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        
        // se nulo, nenhum filtro será aplicado
        Set<Location> filtroLocationsOrigemConsiderado = locationsOrigemConsideradasParaRequisicoes != null ?
                locationsOrigemConsideradasParaRequisicoes
                : locationProjectionLocationsOrigem.getLocationsAtivasOuNuloSeLocationProjectionCompleto();
        
        boolean permiteEstoqueNegativo = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado().getPermiteBacklogDemanda();
        // Community nao executa otimizador, process chain nem roteiros paralelos.
        // Mesmo que uma configuracao antiga chegue ate este ponto, o fluxo
        // heuristico deve avaliar somente a versao prioritaria de producao.
        boolean consideraVersoesProducaoParalelas = false;
        
        // prepara mapa de offsets para a posição de estoque em caso da referência ser o consumo da capacidade
        // se tipo ressuprimento = requisicao, usa estoque em periodo + lead time em periodos (pois periodo = quando material sai da origem e
        // queremos avaliar gap estoque no momento em que material chegar no destino)
        Map<Produto,Integer> offsetPeriodoEstoquePorMaterial = new HashMap<>();
        if (referenciaPeriodo.equals(ReferenciaPeriodo.CONSUMO_CAPACIDADE)) {
            for (Produto material : materiais) {
                offsetPeriodoEstoquePorMaterial.put(material, 
                        getPeriodoPadraoParaPrimeiraDisponibilizacaoMaterial(
                                material, 
                                location, 
                                posicaoPeriodo, 
                                calendario, 
                                versaoMalha, 
                                consideraVersoesProducaoParalelas, 
                                locationProjectionLocationsOrigem,
                                materialProjection,
                                filtroLocationsOrigemConsiderado, 
                                null,
                                supplyNetworkProjection));
            }
        // se referencia periodo = disponibilizacao material, usa a própria posicaoPeriodo
        } else {
            for (Produto material : materiais) {
                offsetPeriodoEstoquePorMaterial
                                .put(material, 0);
            }
        }
        
        // não há como se realizar mudanças no estoque caso as entradas não possam ser ajustadas
        if (!modificaProductionPlan && !modificaDistributionPlanInbound) return 0;
                
        // estoque de segurança em qtde por material. fora do loop pois não muda
        Map<Produto,Double> estoqueSegurancaPorMaterial = materiais.stream()
                .collect(Collectors.toMap(Function.identity(), 
                        x -> supplyPlanningProjection.getQuantidadeEstoqueSeguranca(
                                posicaoPeriodo + offsetPeriodoEstoquePorMaterial.get(x),
                                x, tipoPlano, unidadeMedidaModificacao)));
        
        // se não houve variação entre valor ajustado e valor original, encerra método
        if (modificacaoValorTotal == 0) return 0;
        
        double quantidadeAjusteRestante = modificacaoValorTotal;
        int posicaoPeriodoAtual = posicaoPeriodo;
        
        /*
         * Ajuste positivo: primeiro restringe o conjunto aos materiais com
         * ressuprimento viavel no heuristico Community. Em seguida caminha
         * pelos periodos procurando gaps contra a politica de estoque. Quando
         * encontra gaps, distribui o ajuste remanescente proporcionalmente ao
         * gap de cada material; quando nao encontra, avanca o periodo. Se o
         * horizonte acabar, distribui o saldo igualmente no periodo original do
         * input para garantir que a alteracao manual seja persistida sem entrar
         * em loop.
         */
        if (modificacaoValorTotal > 0) {
            /*
             * Somente materiais com producao viavel ou inbound planejado viavel
             * podem receber aumento de estoque no Planning Book Community. Esse
             * filtro tambem protege contra materiais sem alternativa de
             * abastecimento que jamais absorveriam o ajuste.
             */
            Set<Produto> materiaisComReposicaoViavel = materiais.stream()
                    .filter(x -> 
                            (modificaProductionPlan && supplyNetworkProjection.getTipoRessuprimento(
                                    versaoMalha,
                                    location,
                                    x,
                                    calendario.getDataHorarioInicialPresente(),
                                    consideraVersoesProducaoParalelas,
                                    filtroLocationsOrigemConsiderado,
                                    materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                    .equals(Constantes.SNPOrigemReabastecimento.PRODUCAO))
                            || (
                                    modificaDistributionPlanInbound && supplyNetworkProjection.getTipoRessuprimento(
                                            versaoMalha,
                                            location,
                                            x,
                                            calendario.getDataHorarioInicialPresente(),
                                            consideraVersoesProducaoParalelas,
                                            filtroLocationsOrigemConsiderado,
                                            materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                            .equals(Constantes.SNPOrigemReabastecimento.REQUISICAO)
                                    
                                    // caso haja filtro de origens possíveis realiza o filtro. caso contrário retorna true para não aplicar o filtro
                                    && supplyNetworkProjection.getLocationOrigemPrioritaria(
                                            versaoMalha,
                                            location,
                                            x,
                                            calendario.getDataHorarioInicialPresente(),
                                            filtroLocationsOrigemConsiderado).isPresent()
                            )
                    )
                    .collect(Collectors.toSet());
            
            // Sem reposicao viavel, nao ha linha planejada onde persistir aumento de estoque.
            if (materiaisComReposicaoViavel.isEmpty()) return 0;
            
            // re-projetando estoques para evitar erro de estoque projetado 
            // salvo de maneira incorreta que torna o loop infinito
            atualizaEstoqueProjetadoSemLimitarAZero(
                    supplyPlanningProjection,
                    materiaisComReposicaoViavel,
                    tipoPlano);
            
            Double ultimoGapTotalPeriodo = null; // usado para tratar problemas de arredondamento
            while (Math.abs(quantidadeAjusteRestante) > 0.00001) {
                
                /*
                 * Quando nao ha mais horizonte para procurar gaps, o saldo e
                 * gravado no periodo original do input. Esse fallback e local
                 * ao fluxo de ajuste manual e evita perder uma alteracao que a
                 * UI ja aceitou.
                 */
                if (posicaoPeriodoAtual > calendario.getNumeroPeriodosTotais() - 1) {
                    for (Produto material : materiaisComReposicaoViavel) {
                        modificaEstoqueTotalMaterial(
                                quantidadeAjusteRestante / materiaisComReposicaoViavel.size(), 
                                supplyPlanningProjection,
                                referenciaPeriodo,
                                posicaoPeriodo,
                                material,
                                tipoPlano,
                                unidadeMedidaModificacao,
                                modificaProductionPlan,
                                modificaDistributionPlanInbound,
                                locationsOrigemConsideradasParaRequisicoes);
                    }
                    // força saída do while loop
                    quantidadeAjusteRestante = 0;
                    continue;
                }
                
                // Mantem a posicao como variavel efetivamente final para uso no stream.
                final int copiaLambdaPosicaoPeriodoAtual = posicaoPeriodoAtual;
            
                // Extrai o estoque projetado atualizado para medir gap contra a politica.
                Map<Produto,Double> estoquePorMaterial = materiaisComReposicaoViavel.stream()
                        .collect(Collectors.toMap(Function.identity(), 
                                x ->
                                        // a princípio
                                        // não considera nenhuma entradas planejada no estoque projetado base
                                        // para o período que está sendo iterado (entradas planejadas
                                        // entre posicaoPeriodo -> posicaoPeriodoAtual)
                                        // a posição de partida = fim de posicaoPeriodo, pois assim se consideram
                                        // somente as entradas neste período
                                        SupplyPlanning.getEstoqueProjetado(
                                                supplyPlanningProjection,
                                                posicaoPeriodo + offsetPeriodoEstoquePorMaterial.get(x),
                                                copiaLambdaPosicaoPeriodoAtual + offsetPeriodoEstoquePorMaterial.get(x), 
                                                x,
                                                tipoPlano,
                                                unidadeMedidaModificacao, false, true, false, permiteEstoqueNegativo)
                                ));
                                                
                // Calcula o gap de cada material contra a politica de estoque no periodo avaliado.
                Map<Produto,Double> mapaGapSafetyStockPorMaterial = materiaisComReposicaoViavel.stream()
                        // filtra somente materiais onde há gap de estoque segurança
                        .filter(x -> estoqueSegurancaPorMaterial.get(x) > estoquePorMaterial.get(x) + 0.00001)
                        .collect(Collectors.toMap(Function.identity(), 
                            ThrowingFunction.unchecked(x -> estoqueSegurancaPorMaterial.get(x) - estoquePorMaterial.get(x))));
            
                double gapTotalPeriodo = mapaGapSafetyStockPorMaterial.values().stream()
                        .mapToDouble(x -> x)
                        .sum();

                // primeiro se aumenta o estoque dos materiais com maior gap de safety stock
                // se não houver materiais com gap pula p/ a próxima etapa, de tentativa de manutenção
                // da cobertura de estoque
                if (mapaGapSafetyStockPorMaterial.isEmpty() 
                        // componente para lidar com problemas de arredondamento
                        || (ultimoGapTotalPeriodo != null && Math.abs(ultimoGapTotalPeriodo - gapTotalPeriodo) < 0.00001)) {
                    ultimoGapTotalPeriodo = null;
                    posicaoPeriodoAtual++;
                    continue;
                }
                ultimoGapTotalPeriodo = gapTotalPeriodo;

                double ajusteTotalADistribuirPeriodo = Math.min(quantidadeAjusteRestante, gapTotalPeriodo);
                
                // modifica entradas no período do ajuste e atualiza estoque de fechamento no período do ajuste
                for (Produto materialComGapEstoqueSeguranca : mapaGapSafetyStockPorMaterial.keySet()) {
                    double modificacao = modificaEstoqueTotalMaterial(
                            // ajuste = ajuste total * (gap material / gap total) 
                            ajusteTotalADistribuirPeriodo * (mapaGapSafetyStockPorMaterial.get(materialComGapEstoqueSeguranca) / gapTotalPeriodo), 
                            supplyPlanningProjection,
                            referenciaPeriodo, posicaoPeriodo, 
                            materialComGapEstoqueSeguranca, tipoPlano, 
                            unidadeMedidaModificacao,
                            modificaProductionPlan,
                            modificaDistributionPlanInbound,
                            locationsOrigemConsideradasParaRequisicoes);
                }
                SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                        supplyPlanningProjection,
                        mapaGapSafetyStockPorMaterial.keySet(), tipoPlano);

                quantidadeAjusteRestante -= ajusteTotalADistribuirPeriodo;
                
            }            
            
        }
                
        /*
         * Ajuste negativo: a rotina so pode reduzir entradas planejadas já
         * existentes no periodo do input. Primeiro calcula a quantidade
         * planejada por material, remove materiais sem entrada ajustavel e
         * depois reduz a cobertura dos materiais mais folgados, preservando a
         * politica de estoque sempre que houver volume suficiente.
         */
        if (modificacaoValorTotal < 0) {
            
            Map<Produto,Integer> mapaPeriodoReferencia = materiais.stream()
                    .collect(Collectors.toMap(
                            Function.identity(), 
                            x -> posicaoPeriodo + offsetPeriodoEstoquePorMaterial.get(x)));
            // se restar ajuste a ser feito e todos os estoques estiverem abaixo da segurança, 
            // se farão os ajustes considerando todo o estoque disponível
            boolean abateSegurancaDoEstoqueProjetado = true;
            
            Double ultimaMaiorCobertura = null; // usado para limitar número de iterações : while pode ficar preso por problemas de arredondamento
            Double ultimoEstoqueAcimaSegurancaTotal = null; // usado para tratar problemas de arredondamento
            while (Math.abs(quantidadeAjusteRestante) > 0.00001) {
                                
                boolean abateSegurancaDoEstoqueProjetadoLambda = abateSegurancaDoEstoqueProjetado;
                
                // Soma producao planejada e inbound planejado que podem absorver reducao manual.
                Map<Produto,Double> entradasPlanejadasPorMaterial = materiais.stream()
                        .collect(Collectors.toMap(Function.identity(), 
                                x ->
                                        ((modificaDistributionPlanInbound) ?
                                                supplyPlanningProjection.getQuantidadeDistributionPlanItemInboundDeLocationsOrigem(
                                                        referenciaPeriodo, posicaoPeriodo, x, FirmePlanejado.PLANEJADO, tipoPlano,
                                                        unidadeMedidaModificacao, locationsOrigemConsideradasParaRequisicoes)
                                                : 0)
                                           // Community agrega a producao planejada por material/periodo neste ajuste.
                                           // Referencia de periodo e recurso produtivo especifico pertencem ao detalhe editavel.
                                        + ((modificaProductionPlan) ?
                                                supplyPlanningProjection.getQuantidadeProductionPlan(
                                                        posicaoPeriodo, x, tipoPlano, Constantes.FirmePlanejado.PLANEJADO, unidadeMedidaModificacao)
                                                : 0)));

                Set<Produto> materiaisComEntradasPlanejadas = entradasPlanejadasPorMaterial.keySet().stream()
                        .filter(x -> entradasPlanejadasPorMaterial.get(x) > 0)
                        .collect(Collectors.toSet());
                
                // se não houver nenhum material com entradas planejadas em posicaoPeriodo, retornar 
                // (não se pode reduzir estoque, só entradas)
                if (materiaisComEntradasPlanejadas.isEmpty()) return modificacaoValorTotal - quantidadeAjusteRestante;
                                
                // calcula o último período onde há cobertura acima da segurança
                // considera entradas planejadas + firmes
                // abate o estoque de segurança do cálculo da cobertura
                Map<Produto,Double> coberturaEstoqueAcimaSegurancaEmPeriodosPorMaterial = materiaisComEntradasPlanejadas.stream()
                        .collect(Collectors.toMap(Function.identity(),
                                x ->
                                        getCoberturaEstoqueEmDias(
                                                supplyPlanningProjection,
                                                tipoPlano,
                                                x,
                                                posicaoPeriodo + offsetPeriodoEstoquePorMaterial.get(x),
                                                unidadeMedidaModificacao,
                                                false, true, abateSegurancaDoEstoqueProjetadoLambda)));
                
                Map.Entry<Produto,Double> mapEntryMaiorCobertura = coberturaEstoqueAcimaSegurancaEmPeriodosPorMaterial.entrySet().stream()
                        .sorted(Comparator.comparingDouble((x -> ((Map.Entry<Produto,Double>) x).getValue().doubleValue())).reversed())
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException(
                                "Nao foi possivel selecionar o material com maior cobertura porque o mapa de coberturas esta vazio."));
                Produto materialMaiorCobertura = mapEntryMaiorCobertura.getKey();
                double maiorCobertura = mapEntryMaiorCobertura.getValue();
                
                // não há nenhum estoque disponível acima da segurança 
                // para atender nem o primeiro período
                // neste caso, roda tudo novamente sem abater o estoque de segurança
                if ((abateSegurancaDoEstoqueProjetado == true && maiorCobertura < 0.00001)
                        // componente para lidar com problemas de arredondamento
                        || (ultimaMaiorCobertura != null && Math.abs(ultimaMaiorCobertura - maiorCobertura) < 0.00001)) {
                    ultimaMaiorCobertura = null;
                    abateSegurancaDoEstoqueProjetado = false; // recomeça todos os cálculos, agora considerando 100% estoque disponível
                    continue;
                }
                // não há nenhum estoque disponível
                // para atender nem parcialmente os períodos após o primeiro     
                // ex : estoque periodo ajustado = 0, porém ainda resta um ajuste de -1000 na produção
                if ((abateSegurancaDoEstoqueProjetado == false && maiorCobertura < 0.00001)
                        // componente para lidar com problemas de arredondamento
                        || (ultimaMaiorCobertura != null && Math.abs(ultimaMaiorCobertura - maiorCobertura) < 0.00001)) {
                    // Quando todos os materiais ja estao sem cobertura disponivel, reduz as entradas
                    // planejadas proporcionalmente. A tentativa anterior no loop ja tentou preservar o SS.
                    double totalEntradasPlanejadas = entradasPlanejadasPorMaterial.values().stream()
                            .mapToDouble(x -> x)
                            .sum();
                    
                    double reducaoTargetPercentualPeriodo = Math.min(totalEntradasPlanejadas, -quantidadeAjusteRestante) / totalEntradasPlanejadas;
                    for (Produto material : materiaisComEntradasPlanejadas) {
                        quantidadeAjusteRestante -= modificaEstoqueTotalMaterial(
                                -reducaoTargetPercentualPeriodo * entradasPlanejadasPorMaterial.get(material),
                                supplyPlanningProjection,
                                referenciaPeriodo, posicaoPeriodo, material, 
                                tipoPlano, unidadeMedidaModificacao,
                                modificaProductionPlan,
                                modificaDistributionPlanInbound,
                                locationsOrigemConsideradasParaRequisicoes);
                    }
                    return modificacaoValorTotal - quantidadeAjusteRestante;
                }
                ultimaMaiorCobertura = maiorCobertura;
                                
                int periodoFinalMaiorCobertura = mapaPeriodoReferencia.get(materialMaiorCobertura) + (int) maiorCobertura;
               
                // calcula estoque projetado por material no período indicado acima
                Map<Produto,Double> estoqueProjetadoAcimaSegurancaNoPeriodoFinalPorMaterial = materiaisComEntradasPlanejadas.stream()
                        .collect(Collectors.toMap(Function.identity(),
                                x ->
                                        // a princípio
                                        // não considera nenhuma entradas planejada no estoque projetado base
                                        // para o período que está sendo iterado (entradas planejadas
                                        // entre posicaoPeriodo -> posicaoPeriodoAtual)
                                        // a posição de partida = fim de posicaoPeriodo, pois assim se consideram
                                        // somente as entradas neste período
                                        getEstoqueProjetado(
                                                supplyPlanningProjection,
                                                posicaoPeriodo + offsetPeriodoEstoquePorMaterial.get(x),
                                                periodoFinalMaiorCobertura, 
                                                x,
                                                tipoPlano,
                                                unidadeMedidaModificacao,
                                                false, true, abateSegurancaDoEstoqueProjetadoLambda, permiteEstoqueNegativo)));
                
                // soma dos estoques disponíveis (inicialmente estoques acima da segurança, mas dependendo de 
                // abateSegurancaDoEstoqueProjetado poderá ser a soma dos estoques totais
                double estoqueAcimaSegurancaTotal = estoqueProjetadoAcimaSegurancaNoPeriodoFinalPorMaterial.values().stream()
                        .filter(x -> x > 0)
                        .mapToDouble(x -> x)
                        .sum();

                // caso especial onde todos os materiais terminam exatamente no estoque de segurança (0 de diferença)
                // precisa ser tratado à parte para evitar divisão por zero : redução proporcional às entradas planejadas de cada material
                if (estoqueAcimaSegurancaTotal == 0 
                        // componente para lidar com problemas de arredondamento
                        || (ultimoEstoqueAcimaSegurancaTotal != null && Math.abs(ultimoEstoqueAcimaSegurancaTotal - estoqueAcimaSegurancaTotal) < 0.00001)) {
                    double entradasPlanejadasTotais = entradasPlanejadasPorMaterial.values().stream()
                            .mapToDouble(x -> x)
                            .sum();
                    
                    for (Produto material : materiaisComEntradasPlanejadas) {
                        double entradasPlanejadasMaterial = entradasPlanejadasPorMaterial.get(material);
                        quantidadeAjusteRestante -= modificaEstoqueTotalMaterial(
                                quantidadeAjusteRestante * (entradasPlanejadasMaterial / entradasPlanejadasTotais),
                                supplyPlanningProjection,
                                referenciaPeriodo, posicaoPeriodo, material, 
                                tipoPlano, unidadeMedidaModificacao,
                                modificaProductionPlan,
                                modificaDistributionPlanInbound,
                                locationsOrigemConsideradasParaRequisicoes);
                    }
                } else if (estoqueAcimaSegurancaTotal > 0) {
                    double reducaoTargetPercentualPeriodo = Math.min(estoqueAcimaSegurancaTotal, -quantidadeAjusteRestante) / estoqueAcimaSegurancaTotal;
                    // a redução percentual efetiva do estoque para período final coberturas
                    // deve se limitar às entradas planejadas disponíveis
                    // se red
                    double reducaoEfetivaPercentualPeriodoFinal = materiaisComEntradasPlanejadas.stream()
                            .filter(x -> estoqueProjetadoAcimaSegurancaNoPeriodoFinalPorMaterial.get(x) > 0)
                            .mapToDouble(x -> Math.min(
                                    reducaoTargetPercentualPeriodo, 
                                    entradasPlanejadasPorMaterial.get(x)/estoqueProjetadoAcimaSegurancaNoPeriodoFinalPorMaterial.get(x)))
                            .sorted()
                            .findFirst()
                            .getAsDouble();

                    // aplica a máxima redução percentual possível
                    for (Produto material : materiaisComEntradasPlanejadas) {
                        double estoquePeriodoFimCoberturas = estoqueProjetadoAcimaSegurancaNoPeriodoFinalPorMaterial.get(material);
                        if (estoquePeriodoFimCoberturas > 0) {
                            quantidadeAjusteRestante -= modificaEstoqueTotalMaterial(
                                    -reducaoEfetivaPercentualPeriodoFinal * estoquePeriodoFimCoberturas,
                                    supplyPlanningProjection,
                                    referenciaPeriodo, posicaoPeriodo, material, 
                                    tipoPlano, unidadeMedidaModificacao,
                                    modificaProductionPlan,
                                    modificaDistributionPlanInbound,
                                    locationsOrigemConsideradasParaRequisicoes);
                        }
                    }
                // nenhum estoque acima da segurança no fechamento do período : reduz proporcionalmente à 
                // disponibilidade no início do período.
                // estoque acima segurança no início período atende a demanda apenas parcialmente 
                // (estoque projetado fim de período negativo mas saldo de abertura atende parte da demanda)
                } else {
                    // calcula estoque projetado por material no período indicado acima
                    Map<Produto,Double> demandaAtendidaPeriodoFimCoberturasPorMaterial = materiaisComEntradasPlanejadas.stream()
                            .collect(Collectors.toMap(Function.identity(),
                                    x ->
                                            // demanda parcialmente atendida c/ estoque acima segurança
                                            // segundo valor somado é negativo, por isso vem somado
                                            getDemandaDiretaConsideradaEIndiretaParaProjecaoEstoque(
                                                    supplyPlanningProjection,
                                                    periodoFinalMaiorCobertura,
                                                    x,
                                                    tipoPlano,
                                                    unidadeMedidaModificacao)
                                            + estoqueProjetadoAcimaSegurancaNoPeriodoFinalPorMaterial.get(x)));
                    
                    double demandaAtendidaPeriodoFimCoberturasTotal = demandaAtendidaPeriodoFimCoberturasPorMaterial.values().stream()
                            .mapToDouble(x -> x)
                            .sum();
                    
                    double reducaoTargetPercentualPeriodo = Math.min(demandaAtendidaPeriodoFimCoberturasTotal, -quantidadeAjusteRestante) / demandaAtendidaPeriodoFimCoberturasTotal;
                    
                    // a redução percentual efetiva do estoque para período final coberturas
                    // deve se limitar às entradas planejadas disponíveis
                    double reducaoEfetivaPercentualPeriodoFinal = materiaisComEntradasPlanejadas.stream()
                            .filter(x -> demandaAtendidaPeriodoFimCoberturasPorMaterial.get(x) > 0)
                            .mapToDouble(x -> Math.min(
                                    reducaoTargetPercentualPeriodo, 
                                    entradasPlanejadasPorMaterial.get(x)/demandaAtendidaPeriodoFimCoberturasPorMaterial.get(x)))
                            .sorted()
                            .findFirst()
                            .getAsDouble();
                    
                    // aplica a máxima redução percentual possível
                    for (Produto material : materiaisComEntradasPlanejadas) {
                        double demandaAtendidaPeriodoFimCoberturas = demandaAtendidaPeriodoFimCoberturasPorMaterial.get(material);
                        if (demandaAtendidaPeriodoFimCoberturas > 0) {
                            quantidadeAjusteRestante -= modificaEstoqueTotalMaterial(
                                    -reducaoEfetivaPercentualPeriodoFinal * demandaAtendidaPeriodoFimCoberturas,
                                    supplyPlanningProjection,
                                    referenciaPeriodo, posicaoPeriodo, material, 
                                    tipoPlano, unidadeMedidaModificacao,
                                    modificaProductionPlan,
                                    modificaDistributionPlanInbound,
                                    locationsOrigemConsideradasParaRequisicoes);
                        }
                    }
                }
                ultimoEstoqueAcimaSegurancaTotal = estoqueAcimaSegurancaTotal;
                
                // atualiza os estoques projetados para próxima rodada
                atualizaEstoqueProjetadoSemLimitarAZero(
                        supplyPlanningProjection,
                        tipoPlano);
            }
        }   
        /*
         * O caller persiste production/distribution plans alterados e recalcula
         * estoques das DFUs dependentes. Este metodo devolve apenas o valor
         * efetivamente absorvido pela projection em memoria.
         */
        return modificacaoValorTotal - quantidadeAjusteRestante;
    }

    /**
     * Retorna combinações material-location para
     * 1) Todos os componentes de listas técnicas viáveis onde as DFUs principais são output
     * 2) Todas as locations-materiais onde há linha de transporte com origem nas DFUs principais
     * Usado para se gerar lista de DFUs para as quais se deve recalcular o estoque
     * @param dfusPrincipais
     * @param dataReferenciaStatusMateriais
     * @param supplyNetworkProjection
     * @return
     */
    public static Set<DFU> getDfusDependentes(
            Set<DFU> dfusPrincipais,
            VersaoMalha versaoMalha,
            LocalDateTime dataReferenciaStatusMateriais,
            SupplyNetworkProjection supplyNetworkProjection,
            boolean consideraComoDependenciaInsumosEmListasTecnicas,
            boolean consideraComoDependenciaLocationsOrigemPossiveis) {

        Set<DFU> dfusDependentes = new HashSet<>();
        
        for (DFU dfu : dfusPrincipais) {
            Location location = dfu.getLocation();
            Produto material = dfu.getProduto();
            
            if (consideraComoDependenciaInsumosEmListasTecnicas) {
                dfusDependentes.addAll(supplyNetworkProjection.getListasTecnicasViaveis(location, material, null).stream()
                        .map(supplyNetworkProjection::getMateriaisInput)
                        .flatMap(x -> x.stream())
                        .distinct()
                        .map(x -> new DFU(x, location))
                        .collect(Collectors.toSet()));
            }
            
            if (consideraComoDependenciaLocationsOrigemPossiveis) {
                dfusDependentes.addAll(supplyNetworkProjection.getLinhaTransporteInboundViavelSetParaLocationMaterial(
                        versaoMalha, 
                        location, 
                        material, 
                        dataReferenciaStatusMateriais, 
                        null)
                        .stream()
                        .map(x -> x.getLocationOrigem())
                        .distinct()
                        .map(x -> new DFU(material, x))
                        .collect(Collectors.toSet()));
            }
        }
        
        return dfusDependentes;

    }
    
    /**
     * Incremento : 100% feito em Ajuste Supply na receita/operação de maior prioridade
     * Redução : aplicada a todas as receitas, proporcionalmente
     * @return
     */
    public static double modificaProducaoTotalMaterial(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection, int posicaoPeriodo,
            Produto material,
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaModificacao) {
        
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        Location location = supplyPlanningProjection.getLocation();
        
        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();
                
        // AJUSTE POSITIVO : SEMPRE NO ROTEIRO/OPERAÇÃO PRIORITÁRIO,
        // MODIFICANDO SEMPRE A LINHA AJUSTE_SUPPLY
        if (modificacaoValorTotal > 0) {
            
            Optional<VersaoProducao> optionalVersaoProducaoPrioritaria = supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                    location, 
                    material, 
                    materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto());
            
            VersaoProducao versaoProducao = optionalVersaoProducaoPrioritaria
                    .orElseThrow(() -> getMissingProductionVersionForPositiveProductionAdjustmentException(
                            material,
                            location));

            double valorProducaoAtualParaRoteiroListaTecnica = supplyPlanningProjection.getQuantidadeProductionPlan(
                    posicaoPeriodo, material, versaoProducao, tipoPlano, firmePlanejado, unidadeMedidaModificacao);

            double novoValorProducao = valorProducaoAtualParaRoteiroListaTecnica + modificacaoValorTotal;
            supplyPlanningProjection.setQuantidadeProductionPlan(
                    posicaoPeriodo,
                    material,
                    versaoProducao,
                    novoValorProducao, tipoPlano, firmePlanejado, unidadeMedidaModificacao);

            // 100% da alteração solicitada foi efetivada
            return modificacaoValorTotal;
        
        // AJUSTE NEGATIVO : DISTRIBUÍDO PROPORCIONALMENTE ENTRE TODAS AS ORDENS PLANEJADAS,
        // MODIFICANDO SEMPRE A LINHA AJUSTE_SUPPLY            
        } else if (modificacaoValorTotal < 0) {
            
            double producaoAtual = supplyPlanningProjection.getQuantidadeProductionPlan(
                    posicaoPeriodo, material, tipoPlano, firmePlanejado, unidadeMedidaModificacao);
                        
            if (producaoAtual > 0) {
                
                // em caso de redução talvez não seja possível realizar 100% da redução solicitada
                // pois ela só pode ser aplicadda às sugestões produção existentes
                double modificacaoProducaoPlanejada = -Math.min(-modificacaoValorTotal, producaoAtual);
                double novoValorProducaoPlanejada = producaoAtual + modificacaoProducaoPlanejada;
                
                // circula todos os production plan linhas e ajusta todos usando o mesmo
                // percentual de redução
                for (ProductionPlanLinha productionPlanLinha : supplyPlanningProjection.getProductionPlanLinhaOutput(posicaoPeriodo, material)) {
                    
                    VersaoProducao versaoProducaoTratada = productionPlanLinha.getVersaoProducaoAlocadaOuTemporariaSeInexistente(supplyNetworkProjection);

                    double valorAtual = supplyPlanningProjection.getQuantidadeProductionPlan(
                            posicaoPeriodo, material, versaoProducaoTratada,
                            tipoPlano, firmePlanejado, unidadeMedidaModificacao);

                    double novoValor = (novoValorProducaoPlanejada / producaoAtual) * valorAtual;
                    
                    supplyPlanningProjection.setQuantidadeProductionPlan(
                            posicaoPeriodo, material, versaoProducaoTratada, novoValor, 
                            tipoPlano, firmePlanejado,
                            unidadeMedidaModificacao);
                }   
                // retorna a parcela da redução que pôde efetivamente ser feita
                return modificacaoProducaoPlanejada;
            }
        }
        return 0;
    }

    private static IllegalStateException getMissingProductionVersionForPositiveProductionAdjustmentException(
            Produto material,
            Location location) {

        return new IllegalStateException(
                "SupplyPlanning positive production adjustment requires a viable simple production version "
                        + "before writing planned production; material="
                        + getMaterialId(material)
                        + ", location="
                        + getLocationId(location)
                        + ". Community production adjustments cannot infer routing/BOM from material/location only.");

    }

    private static NoSuchElementException getMissingProductionVersionForHeuristicReplenishmentException(
            Produto material,
            Location location) {

        return new NoSuchElementException(
                "SupplyPlanning heuristic identified production as replenishment source, "
                        + "but no viable production version was available; material="
                        + getMaterialId(material)
                        + ", location="
                        + getLocationId(location)
                        + ". The production source decision and the production-version projection must remain consistent.");

    }

    private static NoSuchElementException getMissingInboundLineForHeuristicReplenishmentException(
            Produto material,
            Location locationDestino) {

        return new NoSuchElementException(
                "SupplyPlanning heuristic is writing an inbound planned requisition, "
                        + "but no priority inbound transport line was available; material="
                        + getMaterialId(material)
                        + ", locationDestino="
                        + getLocationId(locationDestino)
                        + ". The inbound branch must only run after the supply network projection materializes a viable line.");

    }

    private static String getMaterialId(Produto material) {

        if (material == null) {
            return "null";
        }

        return material.getId();

    }

    private static String getLocationId(Location location) {

        if (location == null) {
            return "null";
        }

        return location.getId();

    }
    
    /**
     * Incremento : 100% feito em Ajuste Supply na linha transporte inbound de maior prioridade
     * Redução : aplicada a todas as requisições inbound de todas as linhas transporte inbound, proporcionalmente
     * @return
     */
    public static double modificaInboundPlanejadoTotalMaterial(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection,
            Constantes.ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo, 
            Produto material,
            Constantes.TipoPlano tipoPlano,
            FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaModificacao,
            Set<Location> locationsOrigemConsideradasParaRequisicoes) {
                
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();
        LocationProjection locationProjectionLocationsOrigem = supplyPlanningProjection.getLocationProjectionLocationsOrigem();
        
        // AJUSTE POSITIVO : SEMPRE NA LINHA TRANSPORTE PRIORITÁRIA,
        // MODIFICANDO SEMPRE A LINHA AJUSTE_SUPPLY
        if (modificacaoValorTotal > 0) {
            
            Location location = supplyPlanningProjection.getLocation();
            Calendario calendario = supplyPlanningProjection.getCalendario();
            VersaoMalha versaoMalha = supplyPlanningProjection.getSupplyPlan().getVersaoMalha();
            ParametrosGlobais parametrosGlobais = supplyPlanningProjection.getClusterEParametrosProjection().getParametrosGlobais();
            LocalDate dataReferencia = calendario.getPrimeiraDataPeriodo(calendario.getPosicaoPeriodoPresente());
            
            Set<Location> locationsOrigemFiltradas = (locationsOrigemConsideradasParaRequisicoes != null) ?
                    locationsOrigemConsideradasParaRequisicoes
                    : locationProjectionLocationsOrigem.getLocationsAtivasOuNuloSeLocationProjectionCompleto();
            
            // aloca modificação na linha transporte inbound prioritária, aplicando filtro de locations origem
            // caso seja necessário
            Optional<Location> optionalLocationOrigemPrioritaria = supplyNetworkProjection.getLocationOrigemPrioritaria(
                    versaoMalha, 
                    location, 
                    material, 
                    calendario.getDataHorarioInicialPresente(),
                    locationsOrigemFiltradas);
            
            if (optionalLocationOrigemPrioritaria.isPresent()) {
                Location locationOrigemPrioritaria = optionalLocationOrigemPrioritaria
                        .orElseThrow(() -> new NoSuchElementException(
                                "Location origem prioritaria deveria estar presente depois da checagem de Optional.isPresent()."));
                double inboundDeOrigemPrioritariaAtual = supplyPlanningProjection.getQuantidadeDistributionPlanItemInboundDeLocationsOrigem(
                        referenciaPeriodo, posicaoPeriodo, material, firmePlanejado, tipoPlano, unidadeMedidaModificacao, Sets.newHashSet(locationOrigemPrioritaria));
                double novoValorInbound = inboundDeOrigemPrioritariaAtual + modificacaoValorTotal;
                supplyPlanningProjection.setQuantidadeDistributionPlanInbound(
                        referenciaPeriodo, posicaoPeriodo, material, locationOrigemPrioritaria, 
                        novoValorInbound, unidadeMedidaModificacao, firmePlanejado, tipoPlano);
                
                // 100% da alteração solicitada foi efetivada
                return modificacaoValorTotal;
            }
            
        // AJUSTE NEGATIVO : DISTRIBUÍDO PROPORCIONALMENTE ENTRE TODAS AS REQUISIÇÕES INBOUND,
        // MODIFICANDO SEMPRE A LINHA AJUSTE_SUPPLY            
        } else if (modificacaoValorTotal < 0) {
            
            // se há filtro de locations origem, extrai apenas inbound planejado associado a essas locations
            // caso contrário, extrai todo o inbound
            double inboundAtual = supplyPlanningProjection.getQuantidadeDistributionPlanItemInboundDeLocationsOrigem(
                    referenciaPeriodo, posicaoPeriodo, material, firmePlanejado, tipoPlano,
                    unidadeMedidaModificacao, locationsOrigemConsideradasParaRequisicoes);
            
            if (inboundAtual > 0) {

                // em caso de redução talvez não seja possível realizar 100% da redução solicitada
                // pois ela só pode ser aplicadda às requisições inbound existentes                
                double modificacaoInbound = -Math.min(-modificacaoValorTotal, inboundAtual);
                double novoValorInbound = inboundAtual + modificacaoInbound;
                // todas as locations origem que tenham distribution plan
                Collection<DistributionPlanItem> distributionPlanItemList = 
                        supplyPlanningProjection.getDistributionPlanItemInboundListDeLocationsOrigemEReferenciaPeriodo(
                                        referenciaPeriodo, posicaoPeriodo, material,
                                        locationsOrigemConsideradasParaRequisicoes);
                
                // circula todos os distribution plan linha filtrados e 
                // aplica os ajustes
                for (DistributionPlanItem distributionPlanItem : distributionPlanItemList) {
                    double novoValor = (novoValorInbound / inboundAtual) * distributionPlanItem.getQuantidade(firmePlanejado, tipoPlano);
                    distributionPlanItem.setQuantidade(novoValor, firmePlanejado, tipoPlano);
                }
                
                // retorna a parcela da redução que pôde efetivamente ser feita
                return modificacaoInbound;
            }
        }
        
        return 0;
    }
    
    /**
     * Aplica modificação solicitada por usuário em primeiro lugar à producao planejada 
     * e em segundo às requisições inbound
     * Não altera ordens ou pedidos
     * Retorna a modificação total efetivamente realizada, considerando a disponibilidade do
     * plano no caso de redução da quantidade
     * @return modificação total efetivamente realizada, considerando a disponibilidade do plano no caso de redução da quantidade
     * @throws IncompatibleCalendarException
     * @throws UnitOfMeasureConversionException
     */
    public static double modificaEstoqueTotalMaterial(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection,
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodo,
            Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaModificacao,
            boolean modificaProducaoPlanejada,
            boolean modificaRequisicoesInboundPlanejadas,
            Set<Location> locationsOrigemConsideradasParaRequisicoes) {
        
        // se não houver ajuste ou não for possível mudar nenhuma das entradas, retornar 0
        if (modificacaoValorTotal == 0 || 
                (!modificaProducaoPlanejada && !modificaRequisicoesInboundPlanejadas)) return 0;
        
        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        VersaoMalha versaoMalha = supplyPlanningProjection.getSupplyPlan().getVersaoMalha();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        
        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();
        LocationProjection locationProjectionLocationsOrigem = supplyPlanningProjection.getLocationProjectionLocationsOrigem();
        
        Set<Location> locationsOrigemFiltradas = (locationsOrigemConsideradasParaRequisicoes != null) ?
                locationsOrigemConsideradasParaRequisicoes
                : locationProjectionLocationsOrigem.getLocationsAtivasOuNuloSeLocationProjectionCompleto();
        
        // Community mantem sempre a versao prioritaria: parallel routing e
        // line scheduling pertencem aos overlays Enterprise.
        boolean consideraVersoesProducaoParalelas = false;
        
        SNPOrigemReabastecimento tipoReabastecimento = supplyNetworkProjection.getTipoRessuprimento(
                versaoMalha, 
                location, 
                material, 
                calendario.getDataHorarioInicialPresente(), 
                consideraVersoesProducaoParalelas,
                locationsOrigemFiltradas,
                materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto());
        
        // se tenta sempre alocar a modificação na linha de produção planejada
        double modificacaoRealizadaNaProducao = (modificaProducaoPlanejada && tipoReabastecimento.equals(SNPOrigemReabastecimento.PRODUCAO)) ?
                modificaProducaoTotalMaterial(
                        modificacaoValorTotal,
                        supplyPlanningProjection,
                        posicaoPeriodo, material,
                        tipoPlano,
                        FirmePlanejado.PLANEJADO, // sempre realiza ajustes no estoque através do campo Planejado
                        unidadeMedidaModificacao)
                : 0;
        
        // caso não seja possível alocar toda a diferença na linha de produção planejada,
        // se modificam as requisições inbound
        if (Math.abs(modificacaoValorTotal - modificacaoRealizadaNaProducao) > 0) {
            double modificacaoRealizadaNoInbound = (modificaRequisicoesInboundPlanejadas && !tipoReabastecimento.equals(SNPOrigemReabastecimento.SEM_RESSUPRIMENTO)) ?
                    modificaInboundPlanejadoTotalMaterial(
                            (modificacaoValorTotal - modificacaoRealizadaNaProducao), // só processa o saldo restante
                            supplyPlanningProjection,
                            referenciaPeriodo, posicaoPeriodo,
                            material,
                            tipoPlano,
                            FirmePlanejado.PLANEJADO, // sempre realiza ajustes no estoque através do campo Planejado
                            unidadeMedidaModificacao,
                            locationsOrigemConsideradasParaRequisicoes)
                    : 0;
            
            return modificacaoRealizadaNaProducao + modificacaoRealizadaNoInbound;
        } else {
            return modificacaoRealizadaNaProducao;
        }
        
    }
    
    public static void geraRequisicoesESugestoesProducao(
            SupplyPlanningProjection supplyPlanningProjection,
            MaterialProjection materialProjectionPerfilExecucaoSupplyPlan,
            LocationProjection locationProjectionPerfilExecucaoSupplyPlan,
            TipoPlano tipoPlano) {
        
        Calendario calendario = supplyPlanningProjection.getCalendario();        
        VersaoMalha versaoMalha = supplyPlanningProjection.getSupplyPlan().getVersaoMalha();
        
        ClusterEParametrosProjection clusterEparametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();
        PoliticaEstoquesProjection politicaEstoquesProjection = supplyPlanningProjection.getPoliticaEstoquesProjection();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        UnidadeMedidaProjection unidadeMedidaProjection = supplyPlanningProjection.getConversaoUnidadeMedidaProjection();
        
        MaterialProjection materialProjectionLowLevelCode = supplyPlanningProjection.getMaterialProjection();

        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado();
        // Community executa apenas o motor heuristico simples; portanto a
        // geracao de sugestoes usa sempre a versao de producao prioritaria.
        boolean consideraVersoesProducaoParalelas = false;
        
        boolean permiteEstoqueNegativo = perfilExecucaoSupplyPlan.getPermiteBacklogDemanda();
        
        // atualiza somente para produtos / locations DRP
        atualizaEstoqueSeguranca(
                supplyPlanningProjection,
                tipoPlano);
        
        Location location = supplyPlanningProjection.getLocation();
        
        materialProjectionLowLevelCode.getMateriaisAtivosEmLocation(location).parallelStream().forEach(material -> {
            
            UnidadeMedida unidadeMedidaPadrao = clusterEparametrosProjection.getSNPUnidadeMedidaPadrao(material, location);
            
            Optional<LinhaTransporte> optionalLinhaTransporteInboundPrioritaria = supplyNetworkProjection.getLinhaTransporteViavelPrioritariaInbound(
                    versaoMalha, 
                    location, 
                    material, 
                    calendario.getDataHorarioInicialPresente(),
                    locationProjectionPerfilExecucaoSupplyPlan.getLocationsAtivasOuNuloSeLocationProjectionCompleto());
            
            Integer leadTime = null;
            if (optionalLinhaTransporteInboundPrioritaria.isPresent()) {
                leadTime = supplyNetworkProjection
                        .getLeadTimeEmPeriodosDeOrigemPrioritaria(
                                versaoMalha, location, material, calendario, 
                                calendario.getDataHorarioInicialPresente(),
                                locationProjectionPerfilExecucaoSupplyPlan.getLocationsAtivasOuNuloSeLocationProjectionCompleto())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Linha de transporte prioritaria inbound sem lead time em periodos para material="
                                        + getMaterialId(material)
                                        + ", locationDestino="
                                        + getLocationId(location)
                                        + ". O heuristico Community precisa do lead time para posicionar requisicoes."));
            } else if (!location.getConsideraRestricaoLinhaInbound()) {
                leadTime = 0;
            }
            for (int i=supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente(); i<supplyPlanningProjection.getCalendario().getNumeroPeriodosTotais(); i++) {

            double estoqueSeguranca = supplyPlanningProjection.getQuantidadeEstoqueSeguranca(i, material, tipoPlano, unidadeMedidaPadrao);
            double estoqueMaximo = supplyPlanningProjection.getQuantidadeEstoqueMaximo(i, material, tipoPlano, unidadeMedidaPadrao);

            double estoqueProjetado = getEstoqueProjetado(supplyPlanningProjection, i-1, i, material, tipoPlano, unidadeMedidaPadrao, true, true, false, permiteEstoqueNegativo);

                double gapEstoqueSeguranca = -Math.min(0,
                        estoqueProjetado - estoqueSeguranca);

                // Estoque faltante total considerando estoque projetado contra
                // estoque de seguranca. No Community, safety stock operacional
                // nao depende de Uplift/New Materials.
                double estoqueFaltanteTotal = gapEstoqueSeguranca;

                // ao fazer o split do pedido a ser gerado, acumula quanto do estoqueFaltanteTotal já foi atendido
                double acumulado = 0;
                
                // se modelo reabastecimento diferente DRP e perfil execução não obrigar o plano a ser rodado como DRP,
                // rodar kanban ou ponto de ressuprimento (olha apenas o estoque atual)
                // PROCESSA MODELO KANBAN
                if (!supplyPlanningProjection.isTreatPolicyAsDrp() &&
                        !politicaEstoquesProjection.getSNPModeloReabastecimento(i, material, location).equals(Constantes.SNPModeloReabastecimento.DRP)) {
                    
                    //   se período = 0, emitir requisição com base no Kanban
                    if (i == supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente()) {
                        // data emissão = período 0, porém data de entrega determinada pelo lead time
                        double valorReposicaoTotal = 0;
                        switch (politicaEstoquesProjection.getSNPModeloReabastecimento(i, material, location)) {
                            case KANBAN:
                                valorReposicaoTotal = -Math.min(0, 
                                    supplyPlanningProjection.getQuantidadeEstoqueProjetado(-1, material, tipoPlano, unidadeMedidaPadrao)
                                    - supplyPlanningProjection.getQuantidadeEstoqueSeguranca(-1, material, tipoPlano, unidadeMedidaPadrao));
                                break;
                            default:
                                throw new IllegalStateException(
                                        "Modelo de reabastecimento nao-DRP sem rotina heuristica Community: "
                                                + politicaEstoquesProjection.getSNPModeloReabastecimento(i, material, location));
                        }
                        
                    double quantidadePedidosInbound = supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material, FirmePlanejado.ORDEM, tipoPlano, unidadeMedidaPadrao);
                    double quantidadeRequisicoesInbound = supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedidaPadrao);
                    double quantidadeOrdensProducao = supplyPlanningProjection.getQuantidadeProductionPlan(i, material, tipoPlano, Constantes.FirmePlanejado.ORDEM, unidadeMedidaPadrao);
                           
                        // seta produção, considerando o que já há de pedidos inbound/requisicoes inbound/ordens para reposição do kanban/PR
                        if (supplyPlanningProjection.isGeneratePlannedProductionOrder()
                            && supplyNetworkProjection
                                    .getTipoRessuprimento(
                                            versaoMalha, 
                                            location, 
                                            material, 
                                            calendario.getDataHorarioInicialPresente(), 
                                            consideraVersoesProducaoParalelas,
                                            locationProjectionPerfilExecucaoSupplyPlan.getLocationsAtivasOuNuloSeLocationProjectionCompleto(),
                                            materialProjectionPerfilExecucaoSupplyPlan.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                    .equals(Constantes.SNPOrigemReabastecimento.PRODUCAO)) {
                            
                            VersaoProducao versaoProducao = supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                                    location, 
                                    material, 
                                    consideraVersoesProducaoParalelas,
                                    materialProjectionPerfilExecucaoSupplyPlan.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                    .orElseThrow(() -> getMissingProductionVersionForHeuristicReplenishmentException(
                                            material,
                                            location));
                            
                            // arredonda necessidade líquida para lote mínimo (máximo entre necessidade e lote mínimo)
                            if (perfilExecucaoSupplyPlan.getArredondaProducaoLoteMinimoEMultiplo() && valorReposicaoTotal > 0) {
                                double loteMinimo = clusterEparametrosProjection.getSNPLoteMinimoProducao(material, location, unidadeMedidaPadrao, unidadeMedidaProjection).orElse(0);
                                valorReposicaoTotal = Math.max(valorReposicaoTotal, loteMinimo);
                            }
                            
                            // arredonda para múltiplo de produção
                            if (perfilExecucaoSupplyPlan.getArredondaProducaoLoteMinimoEMultiplo() && valorReposicaoTotal > 0) {
                            OptionalDouble multiploRequisicoes = clusterEparametrosProjection.getSNPMultiploProducao(material, location, unidadeMedidaPadrao, unidadeMedidaProjection);
                                if (multiploRequisicoes.isPresent()) valorReposicaoTotal = 
                                        Math.ceil(valorReposicaoTotal / multiploRequisicoes.getAsDouble()) * multiploRequisicoes.getAsDouble();
                            }
                            
                            // aloca tudo em baseline
                            if (quantidadePedidosInbound + quantidadeRequisicoesInbound + quantidadeOrdensProducao < valorReposicaoTotal) {
                                supplyPlanningProjection.modificaProductionPlan(
                                        i, material, versaoProducao,
                                        valorReposicaoTotal - quantidadePedidosInbound - quantidadeRequisicoesInbound - quantidadeOrdensProducao,
                                    tipoPlano, Constantes.FirmePlanejado.PLANEJADO,
                                        unidadeMedidaPadrao);
                            }
                        // seta inbound, considerando o que já há de pedidos inbound/ordens para reposição do kanban/PR
                        } else if (supplyPlanningProjection.isGenerateInbound() && (
                            optionalLinhaTransporteInboundPrioritaria.isPresent())) {
                            
                            // não gerar requisição inbound se:
                            // 1) perfil execução não permitir geração de ordens planejadas produção
                            // 2) material pode ser produzido na location
                            // 3) perfil execução não permitir substituir uma produção por uma requisição inbound 
                            if (!supplyNetworkProjection
                                    .getTipoRessuprimento(
                                            versaoMalha, 
                                            location, 
                                            material, 
                                            calendario.getDataHorarioInicialPresente(), 
                                            consideraVersoesProducaoParalelas,
                                            locationProjectionPerfilExecucaoSupplyPlan.getLocationsAtivasOuNuloSeLocationProjectionCompleto(),
                                            materialProjectionPerfilExecucaoSupplyPlan.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                    .equals(Constantes.SNPOrigemReabastecimento.PRODUCAO)
                                    || (perfilExecucaoSupplyPlan.getGeraRequisicoesInboundParaMateriaisComProducaoViavel()
                                            && !supplyPlanningProjection.isGeneratePlannedProductionOrder())) {
                            
                                // O proprio bloco exige linha inbound prioritaria; por isso a origem
                                // deve estar materializada antes de gravarmos a requisicao planejada.
                                Location locationOrigem = optionalLinhaTransporteInboundPrioritaria
                                        .orElseThrow(() -> getMissingInboundLineForHeuristicReplenishmentException(
                                                material,
                                                location))
                                        .getLocationOrigem();

                                // arredonda necessidade líquida para lote mínimo (máximo entre necessidade e lote mínimo)
                                if (perfilExecucaoSupplyPlan.getArredondaRequisicoesLoteMinimoEMultiplo() && valorReposicaoTotal > 0) {
                                    OptionalDouble loteMinimo = supplyNetworkProjection.getLoteMinimoTransporteNaUnidadeTarget(
                                            versaoMalha, locationOrigem, location, material, unidadeMedidaPadrao, 
                                            calendario.getDataHorarioInicialPresente());
                                    if (loteMinimo.isPresent()) {
                                        valorReposicaoTotal = Math.max(valorReposicaoTotal, loteMinimo.getAsDouble());
                                    }
                                }

                                if (quantidadePedidosInbound + quantidadeOrdensProducao < valorReposicaoTotal && valorReposicaoTotal > 0) {

                                    // arredonda para múltiplo de requisições
                                    if (perfilExecucaoSupplyPlan.getArredondaRequisicoesLoteMinimoEMultiplo()) {
                                        OptionalDouble multiploRequisicoes = supplyNetworkProjection.getMultiploTransporteNaUnidadeTarget(
                                                versaoMalha, locationOrigem, location, material, unidadeMedidaPadrao,
                                                calendario.getDataHorarioInicialPresente());
                                            if (multiploRequisicoes.isPresent()) valorReposicaoTotal = 
                                                    Math.ceil(valorReposicaoTotal / multiploRequisicoes.getAsDouble()) * multiploRequisicoes.getAsDouble();
                                    }

                                    // Aloca a necessidade liquida no plano irrestrito. O bloco DRP abaixo segue
                                    // a mesma semantica para replenishment externo.
                                    supplyPlanningProjection.setQuantidadeDistributionPlanInbound(
                                            ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                                            i + leadTime, material, locationOrigem,
                                            valorReposicaoTotal - quantidadePedidosInbound - quantidadeOrdensProducao, 
                                            unidadeMedidaPadrao,
                                            FirmePlanejado.PLANEJADO,
                                        tipoPlano);
                                    
                                }
                            }
                            
                        }
                        
                    }
                // PROCESSA MODELO DRP
                // gera a sugestão de requisição. são geradas sugestões para cada key figure, mas sempre buscando atender ao gap consolidado (menor q gaps individuais)
                // é feito um split do gap total em função gaps individuais    
                } else if (estoqueFaltanteTotal > 0) {

                    /*
                     * O plano restrito considera somente lanes capazes de
                     * atender a data. O irrestrito conserva a origem primária,
                     * mesmo quando uma secundária tem lead time menor.
                     */
                    Optional<LinhaTransporte> optionalLinhaTransporteInboundConsiderada =
                            tipoPlano.equals(TipoPlano.PLANO_RESTRITO)
                                    ? getLinhaTransporteInboundViavelParaDataNecessidade(
                                            supplyPlanningProjection,
                                            material,
                                            i,
                                            locationProjectionPerfilExecucaoSupplyPlan)
                                    : optionalLinhaTransporteInboundPrioritaria;
                    Integer leadTimeConsiderado = optionalLinhaTransporteInboundConsiderada
                            .map(linhaTransporte -> supplyNetworkProjection
                                    .getLeadTimePeriodosEntreOrigemDestinoParaMaterial(
                                            versaoMalha,
                                            linhaTransporte.getLocationOrigem(),
                                            location,
                                            material,
                                            calendario,
                                            calendario.getDataHorarioInicialPresente())
                                    .orElseThrow(() -> new IllegalStateException(
                                            "Missing lead time for viable transportation lane "
                                                    + linhaTransporte.getLocationOrigem().getId()
                                                    + " -> " + location.getId()
                                                    + " and material " + material.getId())))
                            .orElse(null);

                    // adiciona à necessidade de reabastecimento o volume relativo à frequência de ressuprimento,
                    // reduzindo a frequência de reabatecimento futura por gerar estoque de ciclo acima do estoque de segurança
                    // só adiciona esse estoque de ciclo caso o estoque esteja abaixo da segurança (estoqueFaltanteTotal > 0)
                    if (perfilExecucaoSupplyPlan.getModeloEstoqueTarget().equals(ModeloEstoqueTarget.MIN_MAX)) {
                        estoqueFaltanteTotal -= estoqueSeguranca;
                        estoqueFaltanteTotal += estoqueMaximo;
                    }

                    // SE HA ROTEIRO + LISTA TECNICA, ATUALIZA SUGESTAO DE PRODUCAO
                    if (supplyPlanningProjection.isGeneratePlannedProductionOrder()
                        && supplyNetworkProjection
                                .getTipoRessuprimento(
                                        versaoMalha, 
                                        location, 
                                        material, 
                                        calendario.getDataHorarioInicialPresente(), 
                                        consideraVersoesProducaoParalelas,
                                        locationProjectionPerfilExecucaoSupplyPlan.getLocationsAtivasOuNuloSeLocationProjectionCompleto(),
                                        materialProjectionPerfilExecucaoSupplyPlan.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                .equals(Constantes.SNPOrigemReabastecimento.PRODUCAO)) {
                        

                        // arredondamento para minimo e multiplo producao
                        if (perfilExecucaoSupplyPlan.getArredondaProducaoLoteMinimoEMultiplo()) {
                            // usa o lote mínimo, se for maior que o gap de necessidade de estoque
                            double loteMinimoProducao = clusterEparametrosProjection.getSNPLoteMinimoProducao(material, location, unidadeMedidaPadrao, unidadeMedidaProjection).orElse(0);
                            estoqueFaltanteTotal = Math.max(estoqueFaltanteTotal, loteMinimoProducao);
                            // arredonda para múltiplo de requisições
                            OptionalDouble multiploProducao = clusterEparametrosProjection.getSNPMultiploProducao(material, location, unidadeMedidaPadrao, unidadeMedidaProjection);
                            if (multiploProducao.isPresent()) estoqueFaltanteTotal = 
                                    Math.ceil(estoqueFaltanteTotal / multiploProducao.getAsDouble()) * multiploProducao.getAsDouble();
                        }
                        
                        VersaoProducao versaoProducao = supplyNetworkProjection
                                .getVersaoProducaoViavelPrioritaria(
                                        location, 
                                        material, 
                                        consideraVersoesProducaoParalelas,
                                        materialProjectionPerfilExecucaoSupplyPlan.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                .orElseThrow(() -> getMissingProductionVersionForHeuristicReplenishmentException(
                                        material,
                                        location));
                        
                        // Aloca o estoque faltante nas ordens planejadas. No
                        // Community, a demanda considerada vem de Demand Plan e
                        // ajustes Community; Uplift/New Materials permanecem
                        // zerados por contrato.
                        double novaOrdemPlanejadaProducao = estoqueFaltanteTotal - acumulado; //Math.min(estoqueFaltanteTotal - acumulado, gapEstoqueSegurancaBaseline);
                        supplyPlanningProjection.modificaProductionPlan(
                                i, material, versaoProducao, novaOrdemPlanejadaProducao,
                            tipoPlano,
                                Constantes.FirmePlanejado.PLANEJADO,
                                unidadeMedidaPadrao);

                    // SE NAO HA ROTEIRO PRODUCAO + LISTA TECNICA, ATUALIZA REQUISICOES - SOMENTE SE FORA DO LEAD TIME
                    // se não se consideram restrições inbound, geram-se requisições mesmo que
                    // lead time seja desrespeitado
                    // no caso de se tratar de fornecedor sem alternativa produção/inbound não se gerarão
                    // requisições porém o plano será limitado a zero no método executaSupplyPlanHeuristico do SupplyPlanningService
                    } else if (supplyPlanningProjection.isGenerateInbound() && 
                            optionalLinhaTransporteInboundConsiderada.isPresent() && (
                            !location.getConsideraRestricaoLinhaInbound() ||
                            i >= supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente()
                                    + leadTimeConsiderado)) {
                        
                        // não gerar requisição inbound se:
                        // 1) perfil execução não permitir geração de ordens planejadas produção
                        // 2) material pode ser produzido na location
                        // 3) perfil execução não permitir substituir uma produção por uma requisição inbound 
                        if (!supplyNetworkProjection
                                .getTipoRessuprimento(
                                        versaoMalha, 
                                        location, 
                                        material, 
                                        calendario.getDataHorarioInicialPresente(), 
                                        consideraVersoesProducaoParalelas,
                                        locationProjectionPerfilExecucaoSupplyPlan.getLocationsAtivasOuNuloSeLocationProjectionCompleto(),
                                        materialProjectionPerfilExecucaoSupplyPlan.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                                .equals(Constantes.SNPOrigemReabastecimento.PRODUCAO) 
                                || (perfilExecucaoSupplyPlan.getGeraRequisicoesInboundParaMateriaisComProducaoViavel()
                                        && !supplyPlanningProjection.isGeneratePlannedProductionOrder())) {
                        
                            // se não consideramos restrições inbound e não há linha transporte inbound,
                            // location origem = location destino para a requisição (auto-ressuprimento)
                            // exemplo : fornecedores
                            // O proprio bloco exige linha inbound prioritaria; por isso a origem
                            // deve estar materializada antes de gravarmos a requisicao planejada.
                            Location locationOrigem = optionalLinhaTransporteInboundConsiderada
                                    .orElseThrow(() -> getMissingInboundLineForHeuristicReplenishmentException(
                                            material,
                                            location))
                                    .getLocationOrigem();

                            // ARREDONDAMENTO DO ESTOQUE FALTANTE PARA LOTE MINIMO E MULTIPLO
                            if (perfilExecucaoSupplyPlan.getArredondaRequisicoesLoteMinimoEMultiplo()) {
                                // usa o lote mínimo, se for maior que o gap de necessidade de estoque
                                double loteMinimoTransporte = supplyNetworkProjection
                                        .getLoteMinimoTransporteNaUnidadeTarget(
                                                versaoMalha, locationOrigem, location, material, unidadeMedidaPadrao,
                                                calendario.getDataHorarioInicialPresente())
                                        .getAsDouble();
                                estoqueFaltanteTotal = Math.max(estoqueFaltanteTotal, loteMinimoTransporte);
                                // arredonda para múltiplo de requisições
                                OptionalDouble multiploRequisicoes = supplyNetworkProjection.getMultiploTransporteNaUnidadeTarget(
                                        versaoMalha, locationOrigem, location, material, unidadeMedidaPadrao,
                                        calendario.getDataHorarioInicialPresente());
                                if (multiploRequisicoes.isPresent() && multiploRequisicoes.getAsDouble() > 0) estoqueFaltanteTotal =
                                        Math.ceil(estoqueFaltanteTotal / multiploRequisicoes.getAsDouble()) * multiploRequisicoes.getAsDouble();
                            }

                            // Aloca o estoque faltante em requisicoes inbound
                            // planejadas. Componentes Enterprise de demanda
                            // permanecem zerados no Community.
                            double novaRequisicaoBaseline = estoqueFaltanteTotal - acumulado; //Math.min(estoqueFaltanteTotal - acumulado, gapEstoqueSegurancaBaseline);
                        supplyPlanningProjection.setQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material, locationOrigem, novaRequisicaoBaseline, unidadeMedidaPadrao, FirmePlanejado.PLANEJADO, tipoPlano);
                        }
                    }
                }
                // atualiza os Inventory Plan Linhas com a projeção dadas as novas sugestões de produção e requisições
            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(supplyPlanningProjection, i, material, tipoPlano);
            if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) limitaEstoqueNegativoAZero(tipoPlano, supplyPlanningProjection, material, i);
            }
        });
    }

    /**
     * Seleciona por prioridade entre as lanes cujo lead time ainda permite
     * expedir no período presente ou depois dele.
     */
    static Optional<LinhaTransporte> getLinhaTransporteInboundViavelParaDataNecessidade(
            SupplyPlanningProjection supplyPlanningProjection,
            Produto material,
            int posicaoPeriodoNecessidade,
            LocationProjection locationProjectionPerfilExecucaoSupplyPlan) {

        SupplyNetworkProjection supplyNetworkProjection =
                supplyPlanningProjection.getSupplyNetworkProjection();
        SupplyPlan supplyPlan = supplyPlanningProjection.getSupplyPlan();
        Location locationDestino = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        LocalDateTime dataReferencia = calendario.getDataHorarioInicialPresente();
        Collection<Location> locationsOrigem = locationProjectionPerfilExecucaoSupplyPlan
                .getLocationsAtivasOuNuloSeLocationProjectionCompleto();

        return supplyNetworkProjection
                .getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(
                        supplyPlan.getVersaoMalha(),
                        locationDestino,
                        material,
                        dataReferencia,
                        locationsOrigem)
                .stream()
                .filter(linhaTransporte -> {
                    int leadTimePeriodos = supplyNetworkProjection
                            .getLeadTimePeriodosEntreOrigemDestinoParaMaterial(
                                    supplyPlan.getVersaoMalha(),
                                    linhaTransporte.getLocationOrigem(),
                                    locationDestino,
                                    material,
                                    calendario,
                                    dataReferencia)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Missing lead time for viable transportation lane "
                                            + linhaTransporte.getLocationOrigem().getId()
                                            + " -> " + locationDestino.getId()
                                            + " and material " + material.getId()));
                    return posicaoPeriodoNecessidade - leadTimePeriodos
                            >= calendario.getPosicaoPeriodoPresente();
                })
                .findFirst();

    }

    /**
     * USADO PARA HEURÍSTICO ------------------------------------------------------------------------------
     * Atualiza colunas dos distribution plans que indicam que parte das requisições e pedidos serão usados
     * para atender carteira própria / de destinos
     */
    public static void atualizaDistributionPlanItemComParcelaAtendimentoDemandaDireta(
            SupplyPlanningProjection supplyPlanningProjection,
            TipoPlano tipoPlano) {

        ClusterEParametrosProjection clusterEparametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();
        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();

        Location location = supplyPlanningProjection.getLocation();

        for (Produto material : materialProjection.getMateriaisAtivosEmLocation(location)) {

            UnidadeMedida unidadeMedidaPadrao = clusterEparametrosProjection.getSNPUnidadeMedidaPadrao(material, location);

            // fluxos planejados com destino em clientes finais
            double ordensTransferenciaPlanejadasEFirmesParaClientes =
                    supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaClientes(
                            material, FirmePlanejado.TOTAL, tipoPlano, unidadeMedidaPadrao);
            // atendimento de demanda direta considerada no Supply Plan (sem destino especificado)
            double demandaDiretaSemClienteEspecificado = supplyPlanningProjection.getDemandaDiretaConsideradaProjection().getQuantidadeConsideradaSupplyPlan(
                    location,
                    material,
                    DemandaDiretaConsideradaLinha.UsoDemandaDireta.PROJECAO_ESTOQUE,
                    tipoPlano,
                    unidadeMedidaPadrao);

            double estoqueDisponivelInicial = supplyPlanningProjection.getQuantidadeEstoqueProjetado(
                    supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente() - 1,
                    material, tipoPlano, unidadeMedidaPadrao);

            // demanda direta considerada nesta location + parcela planejada em requisições outbound - estoque disponível inicial.
            // Desta forma, se estoque disponível inicial < 0, o valor devido é tratado com a mesma prioridade da demanda
            // direta/indireta. Saldos positivos de estoque são automaticamente alocados à demanda.
            double valorRestanteParaAtendimentoDemandaDiretaOuIndireta = ordensTransferenciaPlanejadasEFirmesParaClientes + demandaDiretaSemClienteEspecificado - estoqueDisponivelInicial;

            if (valorRestanteParaAtendimentoDemandaDiretaOuIndireta < 0.00001) continue;

            for (int i=supplyPlanningProjection.getCalendario().getPosicaoPeriodoPresente(); i<supplyPlanningProjection.getCalendario().getNumeroPeriodosTotais(); i++) {

                if (valorRestanteParaAtendimentoDemandaDiretaOuIndireta <= 0) break;

                double quantidadeProducaoPlanejada = supplyPlanningProjection.getQuantidadeProductionPlan(i, material, tipoPlano, Constantes.FirmePlanejado.PLANEJADO, unidadeMedidaPadrao);
                double quantidadeProducaoFirme = supplyPlanningProjection.getQuantidadeProductionPlan(i, material, tipoPlano, Constantes.FirmePlanejado.ORDEM, unidadeMedidaPadrao);
                double quantidadeDistributionPlanPlanejada = supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedidaPadrao);
                double quantidadeDistributionPlanFirme = supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material, FirmePlanejado.ORDEM, tipoPlano, unidadeMedidaPadrao);
                double quantidadeEmTransito = supplyPlanningProjection.getQuantidadeEstoqueTransito(i, material, unidadeMedidaPadrao);

                // parcela do estoque em trânsito alocado para atendimento direto/indireto da demanda
                double volumeEstoqueTransitoParaAtendimentoDemandaDiretaOuIndireta = Math.min(
                        quantidadeEmTransito,
                        valorRestanteParaAtendimentoDemandaDiretaOuIndireta);

                valorRestanteParaAtendimentoDemandaDiretaOuIndireta -= volumeEstoqueTransitoParaAtendimentoDemandaDiretaOuIndireta;

                // parcela das ordens de produção alocadas para atendimento direto/indireto da demanda
                double volumeProducaoParaAtendimentoDemandaDiretaOuIndireta = Math.min(
                        quantidadeProducaoPlanejada + quantidadeProducaoFirme,
                        valorRestanteParaAtendimentoDemandaDiretaOuIndireta);

                valorRestanteParaAtendimentoDemandaDiretaOuIndireta -= volumeProducaoParaAtendimentoDemandaDiretaOuIndireta;

                // parcela dos firmes inbound alocados para atendimento direto/indireto da demanda
                double volumePedidosInboundParaAtendimentoDemandaDiretaOuIndireta = Math.min(
                        quantidadeDistributionPlanFirme,
                        valorRestanteParaAtendimentoDemandaDiretaOuIndireta);

                valorRestanteParaAtendimentoDemandaDiretaOuIndireta -= volumePedidosInboundParaAtendimentoDemandaDiretaOuIndireta;

                // parcela das requisições inbound alocadas para atendimento direto/indireto da demanda
                double volumeRequisicoesInboundParaAtendimentoDemandaDiretaOuIndireta = Math.min(
                        quantidadeDistributionPlanPlanejada,
                        valorRestanteParaAtendimentoDemandaDiretaOuIndireta);

                valorRestanteParaAtendimentoDemandaDiretaOuIndireta -= volumeRequisicoesInboundParaAtendimentoDemandaDiretaOuIndireta;

                // Seta a parcela das ordens firmes e planejadas para atendimento
                // indireto da demanda direta, proporcionalmente ao valor total
                // de cada DistributionPlanItem.
                supplyPlanningProjection.setQuantidadeDistributionPlanInboundParaAtendimentoDemandaDiretaEmUnidadeValor(
                        ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material,
                        volumePedidosInboundParaAtendimentoDemandaDiretaOuIndireta,
                        unidadeMedidaPadrao, tipoPlano, Constantes.FirmePlanejado.ORDEM);

                supplyPlanningProjection.setQuantidadeDistributionPlanInboundParaAtendimentoDemandaDiretaEmUnidadeValor(
                        ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, i, material,
                        volumeRequisicoesInboundParaAtendimentoDemandaDiretaOuIndireta,
                        unidadeMedidaPadrao, tipoPlano, Constantes.FirmePlanejado.PLANEJADO);

            }
        }
    }

    public static void limitaEstoqueNegativoAZero(TipoPlano tipoPlano, SupplyPlanningProjection supplyPlanningProjection, Produto material, int posicaoPeriodo) {
        
        Optional<InventoryPlanLinha> inventoryPlanLinhaOptional = supplyPlanningProjection.getInventoryPlanLinha(posicaoPeriodo, material);

        inventoryPlanLinhaOptional.ifPresent(inventoryPlanLinha -> limitaEstoqueNegativoAZero(tipoPlano, inventoryPlanLinha));
        
    }
    
    public static void limitaEstoqueNegativoAZero(TipoPlano tipoPlano, InventoryPlanLinha inventoryPlanLinha) {

        double quantidadeEstoqueProjetado = inventoryPlanLinha.getQuantidadeEstoqueProjetado(tipoPlano);
        if (quantidadeEstoqueProjetado < 0) inventoryPlanLinha.setQuantidadeEstoqueProjetado(0, tipoPlano);

    }

    public static void limitaEstoquesNegativosAZero(TipoPlano tipoPlano, SupplyPlanningProjection supplyPlanningProjection) {

        for(InventoryPlanLinha inventoryPlanLinha : supplyPlanningProjection.getTodosInventoryPlanLinhas()) {
            limitaEstoqueNegativoAZero(tipoPlano, inventoryPlanLinha);
        }

    }

    /**
     * Atualiza os inventory plan linhas para o periodo/material considerando o estoque anterior,
     * entradas e saídas firmes e planejadas
     * Não rebalanceia os estoques
     * Permite que estoques projetados assumam valores negativos
     * @param supplyPlanningProjection
     * @param posicaoPeriodo
     * @param material
     */
    public static void atualizaEstoqueProjetadoSemLimitarAZero(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo,
            Produto material,
            Constantes.TipoPlano tipoPlano) {

        UnidadeMedida unidadeMedidaPadrao = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                material, supplyPlanningProjection.getLocation());
        
        double estoqueProjetado = getEstoqueProjetado(
                supplyPlanningProjection,
                posicaoPeriodo-1,
                posicaoPeriodo,
                material,
                tipoPlano,
                unidadeMedidaPadrao,
                true, true, false, true);

        supplyPlanningProjection.setQuantidadeEstoqueProjetado(posicaoPeriodo, material, estoqueProjetado, unidadeMedidaPadrao, tipoPlano);

        boolean permiteEstoqueNegativo = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado().getPermiteBacklogDemanda();
        if (!permiteEstoqueNegativo) {
            limitaEstoqueNegativoAZero(tipoPlano, supplyPlanningProjection, material, posicaoPeriodo);
        }

    }
    
    public static void atualizaEstoqueProjetadoSemLimitarAZero(
            SupplyPlanningProjection supplyPlanningProjection,
            Set<Produto> materiais,
            Constantes.TipoPlano tipoPlano) {
        
        Calendario calendario = supplyPlanningProjection.getCalendario();
        
        materiais.stream().forEach(material -> {
            for (int i=calendario.getPosicaoPeriodoPresente(); i<calendario.getNumeroPeriodosTotais(); i++) {
                SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                        supplyPlanningProjection,
                        i,
                        material,
                        tipoPlano);
            }
        });
    }
    
    public static void atualizaEstoqueProjetadoSemLimitarAZero(
            SupplyPlanningProjection supplyPlanningProjection,
            Constantes.TipoPlano tipoPlano) {
        
        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                supplyPlanningProjection,
                supplyPlanningProjection.getMaterialProjection().getMateriaisAtivos(),
                tipoPlano);
        
    }
    
    public static void atualizaEstoqueSeguranca(
            SupplyPlanningProjection supplyPlanningProjection,
            Constantes.TipoPlano tipoPlano) {
        
        supplyPlanningProjection.getMaterialProjection().getMateriaisAtivos().stream().forEach(material -> {
            atualizaEstoqueSeguranca(
                    supplyPlanningProjection,
                    material, tipoPlano);
        });
            
    }
    
    public static void atualizaEstoqueSeguranca(
            SupplyPlanningProjection supplyPlanningProjection,
            Produto material,
            Constantes.TipoPlano tipoPlano) {
       
        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        ClusterEParametrosProjection parametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();
        PoliticaEstoquesProjection politicaEstoquesProjection = supplyPlanningProjection.getPoliticaEstoquesProjection();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado();

        UnidadeMedida unidadeMedidaPadrao = parametrosProjection.getSNPUnidadeMedidaPadrao(
                material, location);

        for (int i=calendario.getPosicaoPeriodoPresente() - 1; i<calendario.getPosicaoPeriodoFinalFuturo(); i++) {

            SNPModeloReabastecimento modeloReabastecimento = politicaEstoquesProjection.getSNPModeloReabastecimento(i, material, location);
            boolean modeloDrp = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado().getTrataPoliticaEstoqueComoDrp(location)
                    || !modeloReabastecimento.equals(Constantes.SNPModeloReabastecimento.DRP);

            // parâmetro pode querer dizer quantidade ou dias, a depender da configuração politicaEstoquesProjection.getSNPModeloCalculoSafetyStock
            double valorEstoqueSegurancaParametro = politicaEstoquesProjection.getSNPEstoqueSegurancaDrpOuTargetKanban(i, material, location);
            double valorEstoqueMaximoParametro = politicaEstoquesProjection.getSNPEstoqueMaximoDrp(i, material, location);

            // zera estoque de segurança/máximo antes de atualizá-los
            supplyPlanningProjection.setQuantidadeEstoqueSeguranca(i, material, 0, unidadeMedidaPadrao, tipoPlano);
            supplyPlanningProjection.setQuantidadeEstoqueMaximo(i, material, 0, unidadeMedidaPadrao, tipoPlano);

            if (valorEstoqueSegurancaParametro <= 0 && valorEstoqueMaximoParametro <= 0) continue;

            double estoqueSegurancaParametroConsiderado = 0;
            double estoqueMaximoParametroConsiderado = 0;

            // trata Min/Max ou Estoque Médio
            switch (perfilExecucaoSupplyPlan.getModeloEstoqueTarget()) {
                case MIN_MAX:
                    estoqueSegurancaParametroConsiderado = valorEstoqueSegurancaParametro;
                    estoqueMaximoParametroConsiderado = valorEstoqueMaximoParametro;
                    break;
                case ESTOQUE_MEDIO:
                    estoqueSegurancaParametroConsiderado = (valorEstoqueSegurancaParametro + valorEstoqueMaximoParametro) / 2;
                    estoqueMaximoParametroConsiderado = estoqueSegurancaParametroConsiderado;
                    break;
                default:
                    throw new IllegalStateException(
                            "Modelo de target stock sem rotina Community de estoque de seguranca: "
                                    + perfilExecucaoSupplyPlan.getModeloEstoqueTarget());
            }

            Constantes.SNPCalculoSafetyStock snpCalculoSafetyStock =
                    politicaEstoquesProjection.getSNPModeloCalculoSafetyStock(i, material, location);

            if (snpCalculoSafetyStock == null) {
                throw getUnsupportedSafetyStockCalculationForHeuristicException(
                        i,
                        material,
                        location,
                        null);
            }

            switch (snpCalculoSafetyStock) {
                case QUANTITY:
                    // No Community, estoque de seguranca em quantidade e
                    // alocado no componente operacional padrao; tratamento
                    // especifico de materiais novos e Enterprise.
                    supplyPlanningProjection.setQuantidadeEstoqueSeguranca(i, material,
                            estoqueSegurancaParametroConsiderado,
                            unidadeMedidaPadrao, tipoPlano);
                    supplyPlanningProjection.setQuantidadeEstoqueMaximo(i, material,
                            (modeloDrp) ? estoqueMaximoParametroConsiderado : estoqueSegurancaParametroConsiderado, // se modelo não for DRP (e.g. Kanban), estoque max = estoque segurança
                            unidadeMedidaPadrao, tipoPlano);
                    break;
                case DAYS:
                    // atualiza o estoque de segurança
                    double estoqueSegurancaBaseline = getQuantidadeRelativaACoberturaEstoqueEmDias(
                            i,
                            estoqueSegurancaParametroConsiderado, // em dias
                            location, material, 
                            tipoPlano, unidadeMedidaPadrao,
                            supplyPlanningProjection);
                    double estoqueMaximoBaseline = (modeloDrp && (estoqueMaximoParametroConsiderado > estoqueSegurancaParametroConsiderado)) ? // se modelo não for DRP (e.g. Kanban), estoque max = estoque segurança. se estoque seguranca em dias for igual ao de segurança, simplesmente igualar os dois sem recalcular o equivalente em dias do estoque
                            getQuantidadeRelativaACoberturaEstoqueEmDias(
                                    i,
                                    estoqueMaximoParametroConsiderado, // em dias
                                    location, material,
                                    tipoPlano, unidadeMedidaPadrao,
                                    supplyPlanningProjection)
                            : estoqueSegurancaBaseline;
                    supplyPlanningProjection.setQuantidadeEstoqueSeguranca(
                            i, material,
                            estoqueSegurancaBaseline,
                            unidadeMedidaPadrao,
                            tipoPlano);
                    supplyPlanningProjection.setQuantidadeEstoqueMaximo(
                            i, material,
                            estoqueMaximoBaseline,
                            unidadeMedidaPadrao,
                            tipoPlano);
                    break;
                default:
                    throw getUnsupportedSafetyStockCalculationForHeuristicException(
                            i,
                            material,
                            location,
                            snpCalculoSafetyStock);
            }

        }
    }

    private static IllegalStateException getUnsupportedSafetyStockCalculationForHeuristicException(
            int posicaoPeriodo,
            Produto material,
            Location location,
            Constantes.SNPCalculoSafetyStock snpCalculoSafetyStock) {

        return new IllegalStateException(
                "SupplyPlanning heuristic safety stock calculation requires QUANTITY or DAYS before updating "
                        + "projected safety stock; calculation model="
                        + snpCalculoSafetyStock
                        + ", material="
                        + getMaterialId(material)
                        + ", location="
                        + getLocationId(location)
                        + ", period="
                        + posicaoPeriodo
                        + ". Inventory policy optimization remains an Enterprise capability.");

    }
    
    public static double getQuantidadeRelativaACoberturaEstoqueEmDias(
            int posicaoPeriodo, double coberturaEstoqueEmDias,
            Location location, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida,
            SupplyPlanningProjection supplyPlanningProjection) {
        
        // para simplificar leitura
        int i = posicaoPeriodo;
        
        Calendario calendario = supplyPlanningProjection.getCalendario();
        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningProjection.getClusterEParametrosProjection();
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        double coberturaSegurancaPeriodos = calendario.converteDiasParaPeriodosCalendario(coberturaEstoqueEmDias);

        double estoqueSeguranca = 0;
        
        // acumula o estoque de segurança
        int primeiroPeriodoCobertura = i+1;//ultimo periodo Safety Stock
        int ultimoPeriodoCobertura = (int) Math.ceil(i + coberturaSegurancaPeriodos);
        double percentualDemandaConsideradaUltimoPeriodo =
                (coberturaSegurancaPeriodos > 0 && coberturaSegurancaPeriodos % 1 == 0) ?
                1 // caso cobertura seja exatamente um múltiplo de períodos. para não desconsiderar o último período
                : coberturaSegurancaPeriodos - Math.floor(coberturaSegurancaPeriodos);
        
        for (int periodoCobertura = primeiroPeriodoCobertura; periodoCobertura <= ultimoPeriodoCobertura && periodoCobertura <= calendario.getPosicaoPeriodoFinalFuturo(); periodoCobertura++) {
            // Community considera apenas fluxos planejados ja materializados no plano. Pedidos,
            // sell-in e ordens firmes sao Enterprise e nao entram na cobertura de safety stock.
            double requisicoesTipoDemandaOutboundDemandaIndireta = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaLocationsInternas(ReferenciaPeriodo.CONSUMO_CAPACIDADE, periodoCobertura, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida);
            double demandaTotalTipoDemanda =
                    getDemandaDiretaConsideradaParaEstoqueSeguranca(
                            supplyPlanningProjection,
                            periodoCobertura, material, tipoPlano, unidadeMedida)
                    + ((location.getIncluiDemandaIndiretaNoSafetyStock(parametrosGlobais)) ? 
                            requisicoesTipoDemandaOutboundDemandaIndireta
                            : 0)
                    + supplyPlanningProjection.getQuantidadeMaterialInputConsumidoNoProductionPlan(periodoCobertura, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida);

            // adiciona forecast completo para o safety stock
            if (periodoCobertura != ultimoPeriodoCobertura) {
                estoqueSeguranca += demandaTotalTipoDemanda;
            // último período : se por ex. cobertura seguranca = 4.5 semanas, na 5a semana se usa 50% do forecast
            } else { // pegar o decimal somente do coberturaSeg Periodos... esse sera o % demanda
                estoqueSeguranca += percentualDemandaConsideradaUltimoPeriodo * demandaTotalTipoDemanda;
            }
        }
        
        return estoqueSeguranca;

    }
    
    public static boolean contemDemandaDiretaParaCalculoEstoqueProjetado(
            SupplyPlanningProjection supplyPlanningProjection,
            Produto material,
            Constantes.TipoPlano tipoPlano) {
        
        Location location = supplyPlanningProjection.getLocation();
        Calendario calendario = supplyPlanningProjection.getCalendario();
        UnidadeMedida unidadeMedidaPadrao = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                material, location);
        
        double demandaAcumulada = 0;
        for (int i=calendario.getPosicaoPeriodoPresente(); i<=calendario.getPosicaoPeriodoFinalFuturo(); i++) {
            demandaAcumulada += getDemandaDiretaConsideradaParaEstoqueProjetado(
                    supplyPlanningProjection,
                    i, material, tipoPlano, unidadeMedidaPadrao);
        }
        
        return demandaAcumulada > 0.000001;
        
    }

    /**
     * Retorna a demanda direta considerada para a projecao de estoque no
     * Community.
     *
     * <p>Esta edicao usa o Demand Plan como unica fonte transacional futura.
     * Carteira, sell-in, sales orders e ordens firmes sao recursos Enterprise.
     * A parcela planejada de outbound para cliente final permanece somada
     * porque representa fluxo planejado pelo proprio supply plan, nao documento
     * transacional externo.</p>
     *
     * @return
     */
    public static double getDemandaDiretaConsideradaParaEstoqueProjetado(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) {

        return getDemandaDiretaPlanoDemandaConsideradaParaEstoqueProjetado(
                supplyPlanningProjection,
                posicaoPeriodo,
                material,
                tipoPlano,
                unidadeMedida);

    }

    /**
     * Retorna a parcela Demand Plan da demanda direta considerada para projecao
     * de estoque.
     *
     * <p>No Community esta e tambem a demanda direta total usada pelo supply
     * plan, pois nao ha carteira ou pedidos firmes. A quantidade planejada de
     * outbound para cliente final e mantida como complemento operacional do
     * plano.</p>
     */
    public static double getDemandaDiretaPlanoDemandaConsideradaParaEstoqueProjetado(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo,
            Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) {

        Location location = supplyPlanningProjection.getLocation();

        double demandaDiretaPlanoDemandaConsiderada = supplyPlanningProjection.getDemandaDiretaConsideradaProjection()
                .getDemandaDiretaConsideradaLinha(location, material, posicaoPeriodo)
                .stream()
                .mapToDouble(demandaDiretaConsideradaLinha -> demandaDiretaConsideradaLinha.getQuantidadeConsideradaSupplyPlan(
                        DemandaDiretaConsideradaLinha.UsoDemandaDireta.PROJECAO_ESTOQUE,
                        DemandaDiretaConsideradaLinha.TipoDemandaDireta.PLANO_DEMANDA,
                        tipoPlano,
                        unidadeMedida,
                        supplyPlanningProjection.getDemandaDiretaConsideradaProjection().getUnidadeMedidaProjection()))
                .sum();

        double ordensPlanejadasOutboundDemandaDireta = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaClientes(
                ReferenciaPeriodo.CONSUMO_CAPACIDADE,
                posicaoPeriodo,
                material,
                FirmePlanejado.PLANEJADO,
                tipoPlano,
                unidadeMedida);

        return demandaDiretaPlanoDemandaConsiderada + ordensPlanejadasOutboundDemandaDireta;
    }

    /**
     * Retorna a demanda direta considerada para estoque de seguranca no
     * Community.
     *
     * <p>Safety stock Community tambem parte apenas do Demand Plan. O dado de
     * safety stock e persistido como total no projection de demanda direta
     * considerada, portanto nao ha recomposicao entre Demand Plan e carteira.</p>
     *
     * @return
     */
    public static double getDemandaDiretaConsideradaParaEstoqueSeguranca(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) {

        Location location = supplyPlanningProjection.getLocation();
        double demandaDiretaSafetyStock = supplyPlanningProjection.getDemandaDiretaConsideradaProjection().getQuantidadeOriginal(
                location,
                material,
                posicaoPeriodo,
                DemandaDiretaConsideradaLinha.TipoDemandaDireta.TOTAL,
                DemandaDiretaConsideradaLinha.PropagacaoDemandaDireta.TOTAL,
                unidadeMedida);

        double ordensPlanejadasOutboundDemandaDireta = supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaClientes(
                ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida);

        return demandaDiretaSafetyStock + ordensPlanejadasOutboundDemandaDireta;
    }

    /**
     * Retorna o estoque projetado entre dois períodos
     * Os cálculos intermediários não carregam backlog, mas o
     * resultado final pode ser negativo
     * @param abateEstoqueSeguranca se verdadeiro, abate o estoque de segurança de posicaoPeriodo do estoque inicial considerado
     * @param herdaEstoqueNegativoPeriodoAnterior determina se nas etapas intermediárias do cálculo se 'carregam' os estoques negativos como backlog
     * @return o estoque projetado para o fim do período target (PODE SER NEGATIVO!)
     */
    public static double getEstoqueProjetado(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodoInicial,
            int posicaoPeriodoAProjetar,
            Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida,
            boolean consideraEntradasPlanejadas,
            boolean consideraEntradasFirmes,
            boolean abateEstoqueSeguranca,
            boolean herdaEstoqueNegativoPeriodoAnterior) {
        
        if (posicaoPeriodoAProjetar < posicaoPeriodoInicial) throw new IllegalArgumentException("Periodo target " + posicaoPeriodoAProjetar + " <= periodo inicial " + posicaoPeriodoInicial + " na projeção de estoque");
        
        // valor inicial estoque = inventory plan do período inicial
        double estoqueAtual = supplyPlanningProjection.getQuantidadeEstoqueProjetado(posicaoPeriodoInicial, material, tipoPlano, unidadeMedida);
        if (abateEstoqueSeguranca) estoqueAtual -= supplyPlanningProjection.getQuantidadeEstoqueSeguranca(posicaoPeriodoInicial, material, tipoPlano, unidadeMedida);
        for (int i = posicaoPeriodoInicial + 1; i <= posicaoPeriodoAProjetar; i++) {
            
            // não herda estoque negativo do período anterior
            if (!herdaEstoqueNegativoPeriodoAnterior) estoqueAtual = Math.max(estoqueAtual, 0);
            double saldoEntradasSaidas = getSaldoEntradasSaidas(
                    supplyPlanningProjection,
                    i,
                    material,
                    tipoPlano,
                    unidadeMedida,
                    consideraEntradasPlanejadas, consideraEntradasFirmes);
            estoqueAtual += saldoEntradasSaidas;
            
            }

        // permite que estoque projetado final seja negativo
        return estoqueAtual;
    }
    
    /**
     * Usado para se gerar um sub supply projection (ex. plano supply M5) com granularidade diferente do original com o propósito de
     * se projetar um estoque inicial futuro a ser consumido em outro supply projection (ex. plano supply M6)
     * @return
     */
    public static double getEstoqueProjetadoDeSupplyPlanningProjectionCalendarioOrigemParaPeriodoCalendarioTarget(
            SupplyPlanningProjection supplyPlanningProjectionCalendarioOrigem,
            EstoqueProjectionProduto estoqueProjectionProdutoPeriodoInicial,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget,
            int posicaoPeriodoInicialCalendarioTarget,
            int posicaoPeriodoAProjetarCalendarioTarget,
            Produto material,
            Constantes.TipoDemanda tipoDemanda, Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida,
            boolean consideraEntradasPlanejadas,
            boolean consideraEntradasFirmes,
            boolean abateEstoqueSeguranca,
            boolean herdaEstoqueNegativoPeriodoAnterior) {
        
        if (posicaoPeriodoAProjetarCalendarioTarget < posicaoPeriodoInicialCalendarioTarget) throw new IllegalArgumentException("Periodo target " + posicaoPeriodoAProjetarCalendarioTarget + " <= periodo inicial " + posicaoPeriodoInicialCalendarioTarget + " na projeção de estoque");
        
        // valor inicial estoque = inventory plan do período inicial
        double estoqueAtual = estoqueProjectionProdutoPeriodoInicial.getQuantidadeEstoque(material, unidadeMedida);
        if (abateEstoqueSeguranca) estoqueAtual -= supplyPlanningProjectionCalendarioOrigem.getQuantidadeEstoqueSeguranca(posicaoPeriodoInicialCalendarioTarget, material, tipoPlano, unidadeMedida);
        for (int i = posicaoPeriodoInicialCalendarioTarget; i <= posicaoPeriodoAProjetarCalendarioTarget; i++) { // ex : hora 0 -> hora 47
            
            // não herda estoque negativo do período anterior
            if (!herdaEstoqueNegativoPeriodoAnterior) estoqueAtual = Math.max(estoqueAtual, 0);
            double saldoEntradasSaidas = getSaldoEntradasSaidasConvertendoDeCalendarioOrigemParaCalendarioTarget(
                    supplyPlanningProjectionCalendarioOrigem, // calendario com bucket size = supply plan antigo usado para projeção estoque inicial. ex : dias
                    splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget, // ex : split dia -> hora (calendario mais granular que o do supply plan a ser executado)
                    i, material,
                    tipoPlano,
                    unidadeMedida,
                    consideraEntradasPlanejadas, consideraEntradasFirmes);
            estoqueAtual += saldoEntradasSaidas;
            
            }

        // permite que estoque projetado final seja negativo
        return estoqueAtual;
    }
    
    public static double getDemandaDiretaConsideradaEIndiretaParaProjecaoEstoque(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) {

        return getDemandaDiretaConsideradaParaEstoqueProjetado(
                supplyPlanningProjection,
                posicaoPeriodo,
                material,
                tipoPlano,
                unidadeMedida)
                + getDemandaIndireta(supplyPlanningProjection, posicaoPeriodo, material, tipoPlano, unidadeMedida);

    }

    /**
     * Transferencias outbound planejadas (exceto destino = CLIENTE_FINAL) e
     * consumo planejado de material por lista tecnica.
     *
     * <p>Pedidos, sell-in e ordens firmes sao Enterprise. Community calcula a
     * demanda indireta apenas a partir dos fluxos planejados pelo heuristico.</p>
     *
     * @return
     */
    public static double getDemandaIndireta(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida) {

        return supplyPlanningProjection.getQuantidadeDistributionPlanOutboundParaLocationsInternas(ReferenciaPeriodo.CONSUMO_CAPACIDADE, posicaoPeriodo, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida)
                + supplyPlanningProjection.getQuantidadeMaterialInputConsumidoNoProductionPlan(posicaoPeriodo, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida);

    }

    /**
     * Determina se no período indicado o material poderá ser abastecido na location destino 
     * dentro do lead time (linha transporte prioritária)
     * Considera-se o ponto de partida como o período presente do calendario 
     * ( calendario.getPosicaoPeriodoPresente() )
     * Se retorno = true, o material não poderá ser recebido no período em questão por se encontrar dentro do lead time
     * Se retorno = false, o material poderá ser recebido caso o pedido seja emitido no período presente
     * @param material
     * @param locationDestino
     * @param calendario
     * @param posicaoPeriodo
     * @param parametrosGlobais
     * @return Optional<Boolean> : caso optional esteja vazio não há linha de transporte inbound para este material
     */
    public static Optional<Boolean> verificaSeDentroDoLeadTime(Produto material, Location locationDestino, 
            Calendario calendario, int posicaoPeriodo, ParametrosGlobais parametrosGlobais) {
        
        Optional<LinhaTransporteProduto> optionalLinhaTransporteProduto = locationDestino.getLinhaTransporteProdutoPrioritariaOndeDestino(
                material, calendario.getPrimeiraDataHorarioPeriodo(posicaoPeriodo), parametrosGlobais);
        
        return optionalLinhaTransporteProduto
                .map(linhaTransporteProduto -> {
                    int leadTimePeriodos = linhaTransporteProduto.getLeadTimePeriodos(calendario);
                    return posicaoPeriodo < calendario.getPosicaoPeriodoPresente() + leadTimePeriodos;
                });
    }
    
    /**
     * 
     * @param dataReferencia pode ser uma data no meío do período. rotina já corrige para 1a data do período
     * @param tamanhoBucket
     * @param perfilExecucaoSupplyPlan
     * @param parametrosGlobais
     * @return 
     */
    public static Calendario getCalendarioDeDataReferencia(LocalDateTime dataReferencia, Constantes.TamanhoBucket tamanhoBucket, 
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan, ParametrosGlobais parametrosGlobais) {
        
        LocalDateTime dataInicial = Calendario.getPrimeiraDataHorarioPeriodo(dataReferencia, tamanhoBucket);
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = dataInicial.plusDays(perfilExecucaoSupplyPlan.getHorizontePlanoDiasMaximo(parametrosGlobais) - 1);
        
        Calendario calendario = Calendario.criaCalendarioPeriodosFuturosDeDatas(tamanhoBucket, dataInicial, dataFinal);
        
        return calendario;
        
    }
      
    /**
     * Traz a cobertura de estoques em dias dividindo o estoque total pela demanda (direta+indireta) indicada
     * Tipicamente se usa TipoDemanda = TOTAL para o cálculo
     * Considera estoque a partir do fechamento do período indicado
     * @param consideraEntradasPlanejadas se falso, não considera ordens planejadas produção e requisições inbound
     * @param consideraEntradasFirmes se falso, não considera colunas firmes transicionais. No Community esses valores
     * chegam zerados; pedidos inbound e ordens firmes sao capacidades Enterprise.
     * @param abateEstoqueSegurança se verdadeiro, abate o estoque de segurança de posicaoPeriodo do estoque inicial considerado
     * @return
     * @throws UnitOfMeasureConversionException
     * @throws IncompatibleCalendarException 
     */
    public static double getCoberturaEstoqueEmDias(
            SupplyPlanningProjection supplyPlanningProjection,
            Constantes.TipoPlano tipoPlano,
            Produto material,
            int posicaoPeriodo,
            UnidadeMedida unidadeMedida,
            boolean consideraEntradasPlanejadas,
            boolean consideraEntradasFirmes,
            boolean abateEstoqueSegurança) {
        
        Calendario calendario = supplyPlanningProjection.getCalendario();

        double quantidadeEstoqueAtual = supplyPlanningProjection.getQuantidadeEstoqueProjetado(posicaoPeriodo, material, tipoPlano, unidadeMedida);
        if (abateEstoqueSegurança) quantidadeEstoqueAtual -= supplyPlanningProjection.getQuantidadeEstoqueSeguranca(posicaoPeriodo, material, tipoPlano, unidadeMedida);

        // se já estivermos no último período, cobertura = 0
        if (quantidadeEstoqueAtual <= 0 || posicaoPeriodo == calendario.getNumeroPeriodosTotais()) return 0;
        
        return getCoberturaEstoqueEmDias(
                supplyPlanningProjection,
                tipoPlano,
                material, 
                posicaoPeriodo, 
                quantidadeEstoqueAtual,
                unidadeMedida, 
                consideraEntradasPlanejadas, 
                consideraEntradasFirmes);
        
    }
    
    /**
     * Traz a cobertura de estoques em dias dividindo o estoque total pela demanda (direta+indireta) indicada
     * Tipicamente se usa TipoDemanda = TOTAL para o cálculo
     * Considera estoque a partir do fechamento do período indicado
     * @param consideraEntradasPlanejadas se falso, não considera ordens planejadas produção e requisições inbound
     * @param consideraEntradasFirmes se falso, não considera ordens produção e pedidos inbound
     */
    public static double getCoberturaEstoqueEmDias(
            SupplyPlanningProjection supplyPlanningProjection,
            Constantes.TipoPlano tipoPlano,
            Produto material,
            int posicaoPeriodo,
            double quantidadeEstoque,
            UnidadeMedida unidadeMedida,
            boolean consideraEntradasPlanejadas,
            boolean consideraEntradasFirmes) {
        
        Calendario calendario = supplyPlanningProjection.getCalendario();

        double quantidadeDiasEstoque = 0;
        for (int i = posicaoPeriodo + 1; i < calendario.getNumeroPeriodosTotais(); i++) {

            double variacaoEstoque = getSaldoEntradasSaidas(
                    supplyPlanningProjection,
                    i, material, tipoPlano, unidadeMedida,
                    consideraEntradasPlanejadas, consideraEntradasFirmes);

            double novaQuantidadeEstoque = quantidadeEstoque + variacaoEstoque;
                        
            if (novaQuantidadeEstoque >= 0.00001)  {
                quantidadeDiasEstoque += calendario.getNumeroDiasNoPeriodo(i);
            } else {
                // # dias * estoque de abertura do período / consumo total período
                quantidadeDiasEstoque += calendario.getNumeroDiasNoPeriodo(i) 
                        * quantidadeEstoque / (-variacaoEstoque);
                return quantidadeDiasEstoque;
            }
            
            quantidadeEstoque = Math.max(novaQuantidadeEstoque,0);
            
        }
        
        return quantidadeDiasEstoque;
        
    }
    
    /**
     * Retorna um número associado à variação do estoque no período
     * Positivo : mais entradas que saídas
     * Negativo : mais saídas que entradas
     * @return
     */
    public static double getSaldoEntradasSaidas(
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida,
            boolean consideraEntradasPlanejadas,
            boolean consideraEntradasFirmes) {
        
        return 
                + ((consideraEntradasPlanejadas) ? supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodo, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida) : 0)
                + ((consideraEntradasFirmes) ? supplyPlanningProjection.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodo, material, FirmePlanejado.ORDEM, tipoPlano, unidadeMedida) : 0)
                + ((consideraEntradasPlanejadas) ? supplyPlanningProjection.getQuantidadeProductionPlan(posicaoPeriodo, material, tipoPlano, Constantes.FirmePlanejado.PLANEJADO, unidadeMedida) : 0)
                + ((consideraEntradasFirmes) ? supplyPlanningProjection.getQuantidadeProductionPlan(posicaoPeriodo, material, tipoPlano, Constantes.FirmePlanejado.ORDEM, unidadeMedida) : 0)
                // estoque em trânsito é 100% atribuído ao tipoDemanda baseline
                + ((consideraEntradasFirmes) ? supplyPlanningProjection.getQuantidadeEstoqueTransito(posicaoPeriodo, material, unidadeMedida) : 0)
                - getDemandaDiretaConsideradaEIndiretaParaProjecaoEstoque(supplyPlanningProjection, posicaoPeriodo, material, tipoPlano, unidadeMedida);

    }
    
    /**
     * Usado para se gerar um sub supply projection (ex. plano supply M5) com granularidade diferente do original com o propósito de
     * se projetar um estoque inicial futuro a ser consumido em outro supply projection (ex. plano supply M6)
     * Retorna um número associado à variação do estoque no período
     * Positivo : mais entradas que saídas
     * Negativo : mais saídas que entradas
     * @return
     */
    public static double getSaldoEntradasSaidasConvertendoDeCalendarioOrigemParaCalendarioTarget(
            SupplyPlanningProjection supplyPlanningProjectionCalendarioOrigem,
            SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget,
            int posicaoPeriodoNoCalendarioTarget, Produto material,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedida,
            boolean consideraEntradasPlanejadas,
            boolean consideraEntradasFirmes) {

        Location location = supplyPlanningProjectionCalendarioOrigem.getLocation();

        return 
                + ((consideraEntradasPlanejadas) ? 
                        splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget.getValorNoCalendarioTargetSplitTemporal(
                                location, material,
                                posicaoPeriodo -> supplyPlanningProjectionCalendarioOrigem.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodo, material, FirmePlanejado.PLANEJADO, tipoPlano, unidadeMedida),
                                posicaoPeriodoNoCalendarioTarget)
                        : 0)
                + ((consideraEntradasFirmes) ? 
                        splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget.getValorNoCalendarioTargetSplitTemporal(
                                location, material,
                                posicaoPeriodo -> supplyPlanningProjectionCalendarioOrigem.getQuantidadeDistributionPlanInbound(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodo, material, FirmePlanejado.ORDEM, tipoPlano, unidadeMedida),
                                posicaoPeriodoNoCalendarioTarget)
                        : 0)
                + ((consideraEntradasPlanejadas) ? 
                        splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget.getValorNoCalendarioTargetSplitTemporal(
                                location, material,
                                posicaoPeriodo -> supplyPlanningProjectionCalendarioOrigem.getQuantidadeProductionPlan(posicaoPeriodo, material, tipoPlano, Constantes.FirmePlanejado.PLANEJADO, unidadeMedida),
                                posicaoPeriodoNoCalendarioTarget)
                        : 0)
                + ((consideraEntradasFirmes) ? 
                        splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget.getValorNoCalendarioTargetSplitTemporal(
                                location, material,
                                posicaoPeriodo -> supplyPlanningProjectionCalendarioOrigem.getQuantidadeProductionPlan(posicaoPeriodo, material, tipoPlano, Constantes.FirmePlanejado.ORDEM, unidadeMedida),
                                posicaoPeriodoNoCalendarioTarget)
                        : 0)
                + ((consideraEntradasFirmes) ?
                        splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget.getValorNoCalendarioTargetSplitTemporal(
                                location, material,
                                posicaoPeriodo -> supplyPlanningProjectionCalendarioOrigem.getQuantidadeEstoqueTransito(posicaoPeriodo, material, unidadeMedida),
                                posicaoPeriodoNoCalendarioTarget)
                        : 0)
                - splitTemporalProjectionPorDfuCalendarioOrigemParaCalendarioTarget.getValorNoCalendarioTargetSplitTemporal(
                        location, material,
                        posicaoPeriodo -> getDemandaDiretaConsideradaEIndiretaParaProjecaoEstoque(supplyPlanningProjectionCalendarioOrigem, posicaoPeriodo, material, tipoPlano, unidadeMedida),
                        posicaoPeriodoNoCalendarioTarget);

    }
    
    public static double getQuantidadeTransferenciaArredondadaParaLoteMinimoEMultiplo(
            VersaoMalha versaoMalha,
            Location locationOrigem, Location locationDestino, Produto material, 
            double quantidade, UnidadeMedida unidadeMedidaTarget,
            Constantes.ModoArredondamento modoArredondamento, 
            SupplyNetworkProjection supplyNetworkProjection,
            LocalDateTime dataHorarioStatusMateriais) {

        double loteMinimo = supplyNetworkProjection
                .getLoteMinimoTransporteNaUnidadeTarget(
                        versaoMalha, locationOrigem, locationDestino, material, unidadeMedidaTarget,
                        dataHorarioStatusMateriais)
                .orElse(0);
        quantidade = Math.max(quantidade, loteMinimo);
        
        OptionalDouble optionalMultiplo = supplyNetworkProjection.getMultiploTransporteNaUnidadeTarget(
                versaoMalha, locationOrigem, locationDestino, material, unidadeMedidaTarget,
                dataHorarioStatusMateriais);
        
        return arredondaQuantidadeParaMinimoEMultiplo(quantidade, loteMinimo, optionalMultiplo, modoArredondamento);

    }
    
    public static double getQuantidadeProducaoArredondadaParaLoteMinimoEMultiplo(
            Location location, Produto material, 
            double quantidade, UnidadeMedida unidadeMedidaTarget,
            Constantes.ModoArredondamento modoArredondamento, 
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection,
            LocalDateTime dataHorarioStatusMateriais) throws UnitOfMeasureConversionException {
        
        double loteMinimo = clusterEParametrosProjection.getSNPLoteMinimoProducao(material, location, unidadeMedidaTarget, unidadeMedidaProjection).orElse(0);
        quantidade = Math.max(quantidade, loteMinimo);
        
        OptionalDouble optionalMultiplo = clusterEParametrosProjection.getSNPMultiploProducao(material, location, unidadeMedidaTarget, unidadeMedidaProjection);
        
        return arredondaQuantidadeParaMinimoEMultiplo(quantidade, loteMinimo, optionalMultiplo, modoArredondamento);
        
    }
    
    private static double arredondaQuantidadeParaMinimoEMultiplo(
            double quantidade, double loteMinimo, OptionalDouble optionalMultiplo,
            Constantes.ModoArredondamento modoArredondamento) {
        
        if (optionalMultiplo.isPresent()) {
            double multiplo = optionalMultiplo.getAsDouble();
            double percentualMultiplo = quantidade / multiplo;
            double valorArredondado;
            switch (modoArredondamento) {
                case ARREDONDA_PARA_CIMA:
                    valorArredondado = Math.ceil(percentualMultiplo) * multiplo;
                    break;
                case ARREDONDA_PARA_BAIXO:
                    valorArredondado = Math.floor(percentualMultiplo) * multiplo;
                    if (valorArredondado < loteMinimo) {
                        valorArredondado = Math.ceil(percentualMultiplo) * multiplo;
                    }
                    break;
                case ARREDONDA:
                    valorArredondado = Math.round(percentualMultiplo) * multiplo;
                    if (valorArredondado < loteMinimo) {
                        valorArredondado = Math.ceil(percentualMultiplo) * multiplo;
                    }
                    break;
                default:
                    valorArredondado = quantidade;
                    
            }
            return valorArredondado;
        } else {
            return quantidade;
        }
        
    }
    
    /**
     * Arredonda todo o plano inbound de um período para lote minimo/multiplo
     * @param modoArredondamento informa se arredondamento é para cima/baixo/round
     */
    public static void arredondaQuantidadesPlanejadasParaLoteMinimoEMultiplo(
            Constantes.TipoPlano tipoPlano,
            Constantes.ModoArredondamento modoArredondamento,
            Collection<DistributionPlanItem> distributionPlanItemSet,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        ParametrosGlobais parametrosGlobais = supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais();
        
        for (DistributionPlanItem distributionPlanItem : distributionPlanItemSet) {
            VersaoMalha versaoMalha = distributionPlanItem.getSupplyPlan().getVersaoMalha();
            LocalDateTime dataHorarioStatusMateriais = distributionPlanItem.getSupplyPlan().getDataInicioPlano();
            double quantidadeInicial = distributionPlanItem.getQuantidadeOrdemPlanejada(tipoPlano);
            if (quantidadeInicial > 0) {
                double novaQuantidade = getQuantidadeTransferenciaArredondadaParaLoteMinimoEMultiplo(
                        versaoMalha, distributionPlanItem.getLocationOrigem(), distributionPlanItem.getLocationDestino(), 
                        distributionPlanItem.getProduto(), quantidadeInicial, distributionPlanItem.getUnidadeMedida(parametrosGlobais), 
                        modoArredondamento, supplyNetworkProjection, dataHorarioStatusMateriais);

                distributionPlanItem.setQuantidade(novaQuantidade, FirmePlanejado.PLANEJADO, tipoPlano);
            }
        }
        
    }

    /**
     * Calcula quantos segmentos de aging/lote seriam necessários para modelar
     * shelf-life e tempo de processo no otimizador Enterprise.
     *
     * <p>No Community os cadastros de shelf-life, aging por lote e writeoff
     * não fazem parte da edição. O método permanece nesta classe porque a regra
     * matemática é compartilhável e porque o modelo Enterprise reutiliza
     * rotinas puras de Supply Planning. Quando os parâmetros não existem, o
     * retorno vazio deixa o chamador Enterprise pular a componente.</p>
     *
     * @param location location avaliada
     * @param material material avaliado
     * @param calendario calendario da rodada
     * @param clusterEParametrosProjection projection de parâmetros já carregada
     * @return número de segmentos ou vazio quando aging/shelf-life não se aplica
     */
    public static Optional<Integer> getNumeroSegmentosLotes(
            Location location,
            Produto material,
            Calendario calendario,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        Optional<Integer> optionalPrazoValidade =
                clusterEParametrosProjection.getPrazoValidadeEmPeriodos(location, material, calendario);
        Optional<Integer> optionalTempoProcesso =
                clusterEParametrosProjection.getTempoProcessoEmPeriodos(location, material, calendario);

        if (optionalPrazoValidade.isPresent() && optionalTempoProcesso.isPresent()) {
            int prazoValidade = optionalPrazoValidade.orElseThrow(() -> new IllegalStateException(
                    "Shelf Life period count disappeared while calculating lot segmentation."));
            int tempoProcesso = optionalTempoProcesso.orElseThrow(() -> new IllegalStateException(
                    "Process Time period count disappeared while calculating lot segmentation."));

            if (tempoProcesso > prazoValidade) {
                throw new IllegalArgumentException(
                        "Process Time cannot be larger than Shelf Life for Material "
                                + material.getId() + " / Location " + location.getId());
            }

            return Optional.of(Math.max(tempoProcesso, prazoValidade));
        }

        if (optionalPrazoValidade.isPresent()) {
            return optionalPrazoValidade;
        }

        if (optionalTempoProcesso.isPresent()) {
            /*
             * Segmento adicional para acomodar itens que já passaram pelo
             * tempo de processo e poderão ser vendidos.
             */
            return optionalTempoProcesso.map(tempoProcesso -> tempoProcesso + 1);
        }

        return Optional.empty();

    }

}
