package com.opsfactor.community.capability.supplyplanning.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.platform.exception.UnitOfMeasureConversionException;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.IncompatibleCalendarException;
import com.opsfactor.community.platform.utility.Constantes;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metodos puros para capacidade e consumo de producao no heuristico Community.
 *
 * <p>O recorte aberto considera capacidade produtiva por horas/dia. Capacidade
 * por UOM, turnos, custos de recurso, line scheduling e parallel routing
 * pertencem ao Enterprise.</p>
 */
public class ProductionPlanning {
    
    /**
     * Retorna a capacidade total em horas de um recurso para um certo período
     * @return
     */
    public static double getCapacidadeRecursoHoras(Calendario calendario,
            RecursoProdutivo recursoProdutivo, int posicaoPeriodo) {

        double horasDisponiveis = recursoProdutivo.getDisponibilidadeHorasPeriodo(
                calendario.getPrimeiraDataPeriodo(posicaoPeriodo), 
                calendario.getUltimaDataPeriodo(posicaoPeriodo));
        
        return horasDisponiveis;
    }
    
    public static void setPercentualQuantidadeOriginalOutputProductionPlanLinha(
            double percentualQuantidadeOriginal,
            SupplyPlanningProjection supplyPlanningProjection, int posicaoPeriodo, Produto material,
            RecursoProdutivo recursoProdutivo, Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado) throws UnitOfMeasureConversionException {
        
        UnidadeMedida unidadeMedidaPadrao = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                material, supplyPlanningProjection.getLocation());

        double quantidadeTotalOrdens = supplyPlanningProjection.getQuantidadeProductionPlan(posicaoPeriodo, material, recursoProdutivo, tipoPlano, firmePlanejado, unidadeMedidaPadrao);
        double modificacaoValorTotal = (percentualQuantidadeOriginal - 1) * quantidadeTotalOrdens;
        
        modificaOutputProductionPlanLinha(
                modificacaoValorTotal,
                supplyPlanningProjection, posicaoPeriodo, 
                material, recursoProdutivo, tipoPlano, firmePlanejado);
        
    }
    
    public static void modificaOutputProductionPlanLinha(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection,
            int posicaoPeriodo,
            Produto material,
            RecursoProdutivo recursoProdutivo,
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado) {
             
        Location location = supplyPlanningProjection.getLocation();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        MaterialProjection materialProjection = supplyPlanningProjection.getMaterialProjection();

        UnidadeMedida unidadeMedidaPadrao = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                material, supplyPlanningProjection.getLocation());

        double quantidade = supplyPlanningProjection.getQuantidadeProductionPlan(posicaoPeriodo, material, recursoProdutivo, tipoPlano, firmePlanejado, unidadeMedidaPadrao);

        // Percentual aplicado sobre linhas planejadas. Componentes Enterprise
        // de demanda permanecem zerados no Community, mesmo que as colunas
        // fisicas existam no schema transicional.
        double percentualQuantidade = 1f;

        if (modificacaoValorTotal == 0) return;
        
        // Reducao da quantidade original: escala a linha planejada sem expor
        // selecao de componente de demanda ao usuario Community.
        if (modificacaoValorTotal < 0) {

            double quantidadeAReduzir = -modificacaoValorTotal;

            if (quantidadeAReduzir > 0) {
                double ajuste = Math.min(quantidadeAReduzir, Math.max(0,quantidade));
                if (quantidade  > 0) {
                    percentualQuantidade = (quantidade - ajuste) / quantidade;
                }
                quantidadeAReduzir -= ajuste;
            }

            // lista de linhas production plan que serão afetadas
            Collection<ProductionPlanLinha> productionPlanLinhasRecursoProdutivo;
            if (recursoProdutivo != null) {
                productionPlanLinhasRecursoProdutivo = supplyPlanningProjection.getProductionPlanLinhaOutput(
                        posicaoPeriodo, material, recursoProdutivo);
            } else {
                productionPlanLinhasRecursoProdutivo = supplyPlanningProjection.getProductionPlanLinhaOutput(
                        posicaoPeriodo, material);
            }
            
            Set<VersaoProducao> versoesProducaoProductionPlanLinhas = productionPlanLinhasRecursoProdutivo.stream()
                    .map(x -> x.getVersaoProducaoAlocadaOuTemporariaSeInexistente(supplyNetworkProjection))
                    .collect(Collectors.toSet());
                        
            for (VersaoProducao versaoProducaoModificada : versoesProducaoProductionPlanLinhas) {
                
                double valorAtualBaseline = supplyPlanningProjection.getQuantidadeProductionPlan(
                        posicaoPeriodo, material, versaoProducaoModificada, tipoPlano, firmePlanejado, unidadeMedidaPadrao);

                supplyPlanningProjection.setQuantidadeProductionPlan(
                        posicaoPeriodo, material,
                        versaoProducaoModificada,
                        percentualQuantidade * valorAtualBaseline,
                        tipoPlano, firmePlanejado,
                        unidadeMedidaPadrao);
                
            }
            
        // incremento da quantidade original : vai tudo para ajuste supply no roteiro prioritário
        } else if (modificacaoValorTotal >= 0) {

            double quantidadeAIncrementar = modificacaoValorTotal;
            
            // Community nao possui line scheduling nem roteiros paralelos:
            // incrementos manuais recaem sempre sobre a versao prioritaria.
            boolean consideraVersoesProducaoParalelas = false;
            Optional<VersaoProducao> optionalVersaoProducaoPrioritaria =  supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                    location, material, consideraVersoesProducaoParalelas, materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto());
            
            if (!optionalVersaoProducaoPrioritaria.isPresent()) return;

            // A ausencia de versao prioritaria ja foi tratada acima como
            // "nada a incrementar". A partir daqui a versao e obrigatoria para
            // manter explicito onde o incremento manual sera gravado.
            VersaoProducao versaoProducaoPrioritaria = optionalVersaoProducaoPrioritaria
                    .orElseThrow(() -> new NoSuchElementException(
                            "ProductionPlanning expected a priority production version after the presence check; material="
                                    + material.getId()
                                    + ", location="
                                    + location.getId()));
            
            double valorProducaoAtualNaVersaoProducaoPrioritaria = supplyPlanningProjection.getQuantidadeProductionPlan(
                    posicaoPeriodo, material, versaoProducaoPrioritaria, tipoPlano, firmePlanejado, unidadeMedidaPadrao);
            
            supplyPlanningProjection.setQuantidadeProductionPlan(
                    posicaoPeriodo, material, versaoProducaoPrioritaria,
                    valorProducaoAtualNaVersaoProducaoPrioritaria + quantidadeAIncrementar, 
                    tipoPlano, firmePlanejado, unidadeMedidaPadrao);
            
        }
    }
    
    public static void setPercentualQuantidadeOriginalOutputNoProductionPlanLinha(
            double percentualQuantidadeOriginal,
            SupplyPlanningProjection supplyPlanningProjection, int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado) throws IncompatibleCalendarException, UnitOfMeasureConversionException {

        UnidadeMedida unidadeMedidaPadrao = supplyPlanningProjection.getClusterEParametrosProjection().getSNPUnidadeMedidaPadrao(
                material, supplyPlanningProjection.getLocation());

        double quantidadeTotalSugestoes = supplyPlanningProjection.getQuantidadeProductionPlan(
                posicaoPeriodo, material, tipoPlano, firmePlanejado, unidadeMedidaPadrao);
        double modificacaoValorTotal = (percentualQuantidadeOriginal - 1) * quantidadeTotalSugestoes;
        
        modificaOutputProductionPlanLinha(
                modificacaoValorTotal,
                supplyPlanningProjection, posicaoPeriodo, material, 
                tipoPlano, firmePlanejado);
        
    }

    public static void modificaOutputProductionPlanLinha(
            double modificacaoValorTotal,
            SupplyPlanningProjection supplyPlanningProjection, int posicaoPeriodo, Produto material,
            Constantes.TipoPlano tipoPlano, Constantes.FirmePlanejado firmePlanejado) {
        
        modificaOutputProductionPlanLinha(
                modificacaoValorTotal, supplyPlanningProjection, posicaoPeriodo, material, null, 
                tipoPlano, firmePlanejado);
                
    }

    public static Map<RecursoProdutivo, List<Float>> getCapacidadeRecursoEmHorasPorPeriodo(
            Location location, Calendario calendario) {
        
        Map<RecursoProdutivo, List<Float>> mapaCapacidadeEmHorasPorRecurso = new HashMap<>();
        
        for (RecursoProdutivo recursoProdutivo : location.getRecursosProdutivosAtivos()) {
            
            mapaCapacidadeEmHorasPorRecurso.put(recursoProdutivo, new ArrayList<>());
            
            for (int i=calendario.getPosicaoPeriodoInicialFuturo(); 
                    i<calendario.getNumeroPeriodosTotais(); i++) {
                
                LocalDate dataInicialPeriodo = calendario.getPrimeiraDataPeriodo(i);
                LocalDate dataFinalPeriodo = calendario.getUltimaDataPeriodo(i);
                
                float disponibilidadeHoras = recursoProdutivo.getDisponibilidadeHorasPeriodo(
                        dataInicialPeriodo, dataFinalPeriodo);
                
                mapaCapacidadeEmHorasPorRecurso.get(recursoProdutivo).add(disponibilidadeHoras);
            }
        }
        return mapaCapacidadeEmHorasPorRecurso;
    }

}
