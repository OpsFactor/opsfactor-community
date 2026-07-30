package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Projection operacional de politicas de estoque para Supply Planning.
 *
 * <p>O Community usa esta projection apenas para safety stock operacional:
 * modelo de calculo, modelo de reabastecimento, modelo operacional, estoque de
 * seguranca e estoque maximo. Frequencia de reabastecimento passou a pertencer
 * ao modulo Enterprise de Inventory Policy Optimization; por isso a projection
 * Community neutraliza esse valor mesmo quando a base legada ainda possui dado
 * preenchido.</p>
 */
public class PoliticaEstoquesProjection {
    
    /**
     * Calendario do plano em execucao, usado para expandir vigencias das
     * politicas por posicao de periodo.
     */
    @Getter
    protected Calendario calendario;

    /**
     * Snapshot em memoria da politica vigente por periodo, location e material.
     * Ausencia de chave significa ausencia de override cadastrado e aciona os
     * defaults operacionais do Community.
     */
    protected Map<Integer,Map<Location,Map<Produto,PoliticaEstoquesMaterialLocation>>> mapaPoliticaEstoquesVigenteParaPeridoLocationMaterial = new ConcurrentHashMap<>();

    /**
     * Projection base usada para resolver defaults de material quando nao ha
     * politica especifica material/location.
     */
    @Getter
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    public boolean verificaSeHaPoliticaEstoquesMaterialLocationCadastrada() {
        return (mapaPoliticaEstoquesVigenteParaPeridoLocationMaterial != null && !mapaPoliticaEstoquesVigenteParaPeridoLocationMaterial.isEmpty());
    }

    private Optional<PoliticaEstoquesMaterialLocation> getPoliticaEstoquesMaterialLocation(int posicaoPeriodo, Produto material, Location location) {
        return FuncoesMap.getElementoDeNestedMap(
                mapaPoliticaEstoquesVigenteParaPeridoLocationMaterial,
                PoliticaEstoquesMaterialLocation.class,
                posicaoPeriodo, location, material);
    }

    public Constantes.SNPCalculoSafetyStock getSNPModeloCalculoSafetyStock(int posicaoPeriodo, Produto material, Location location) {
        
        Optional<PoliticaEstoquesMaterialLocation> optionalPoliticaEstoquesMaterialLocation = getPoliticaEstoquesMaterialLocation(posicaoPeriodo, material, location);
        
        return optionalPoliticaEstoquesMaterialLocation
                .map(politica -> politica.getCalculoSafetyStockCadastrado())
                .orElse(Constantes.SNPCalculoSafetyStock.DAYS);
                
    }
    
    public Constantes.SNPModeloOperacional getSNPModeloOperacional(int posicaoPeriodo, Produto material, Location location) {
        
        Optional<PoliticaEstoquesMaterialLocation> optionalPoliticaEstoquesMaterialLocation = getPoliticaEstoquesMaterialLocation(posicaoPeriodo, material, location);
        
        return optionalPoliticaEstoquesMaterialLocation
                .map(politica -> politica.getModeloOperacionalCadastrado())
                .orElse(clusterEParametrosProjection
                        .getMaterialPersistido(material.getId())
                        .getModeloOperacional());
                
    }
    
    public Constantes.SNPModeloReabastecimento getSNPModeloReabastecimento(int posicaoPeriodo, Produto material, Location location) {
        
        Optional<PoliticaEstoquesMaterialLocation> optionalPoliticaEstoquesMaterialLocation = getPoliticaEstoquesMaterialLocation(posicaoPeriodo, material, location);

        return optionalPoliticaEstoquesMaterialLocation
                .map(politica -> politica.getModeloReabastecimentoCadastrado())
                .orElse(Constantes.SNPModeloReabastecimento.DRP);

    }

    public double getSNPEstoqueSegurancaDrpOuTargetKanban(int posicaoPeriodo, Produto material, Location location) {
        
        Optional<PoliticaEstoquesMaterialLocation> optionalPoliticaEstoquesMaterialLocation = getPoliticaEstoquesMaterialLocation(posicaoPeriodo, material, location);
        
        return optionalPoliticaEstoquesMaterialLocation
                .map(politica -> politica.getEstoqueSegurancaDrpOuTargetKanban())
                .orElse(0.0);
                
    }

    public double getSNPEstoqueMaximoDrp(int posicaoPeriodo, Produto material, Location location) {

        Optional<PoliticaEstoquesMaterialLocation> optionalPoliticaEstoquesMaterialLocation = getPoliticaEstoquesMaterialLocation(posicaoPeriodo, material, location);

        return optionalPoliticaEstoquesMaterialLocation
                .map(politica -> politica.getEstoqueMaximoDrp())
                .orElse(0.0);

    }

    public double getSNPFrequenciaReabastecimentoDias(int posicaoPeriodo, Produto material, Location location) {

        /*
         * Frequencia de reabastecimento alimenta a otimizacao de politica de
         * estoques, nao o safety stock operacional Community. Retornar zero
         * impede que bases legadas ou chamadas diretas reabram comportamento
         * Enterprise sem passar pelo overlay privado correspondente.
         */
        return 0.0;

    }

}
