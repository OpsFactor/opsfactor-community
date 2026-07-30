package com.opsfactor.community.capability.supplyplanning.inventoryplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Projection location-específica do safety stock usado por motores de Supply
 * Planning.
 * <p>
 * Este objeto concentra duas fontes de verdade da série:
 * - a política estrutural de safety stock cadastrada para a location;
 * - os valores físicos já resolvidos em etapa prévia, quando o cálculo não está
 * em {@code QUANTITY}.
 * <p>
 * Com isso, loaders Enterprise deixam de depender diretamente de objetos
 * transicionais do pipeline e passam a consumir um contrato explícito de input.
 *
 * <p>A classe permanece como objeto JavaBean, e nao como {@code record}, para
 * preservar getters usados por projections e loaders.</p>
 */
@Getter
@SuppressWarnings("ClassCanBeRecord")
public class SafetyStockProjection {

    /**
     * Location dona da série de safety stock.
     */
    private final Location location;

    /**
     * Política de estoques usada para decidir se a série entra no modelo e qual
     * é o modo de cálculo do safety stock.
     */
    private final PoliticaEstoquesProjection politicaEstoquesProjection;

    /**
     * Valores físicos já resolvidos de safety stock por material/período.
     * <p>
     * Esse mapa é opcional: ele só é preenchido quando uma etapa anterior já
     * calculou o safety stock em quantidade e a rodada atual deve tratá-lo como
     * dado fixo de entrada.
     */
    private final Map<Produto, Map<Integer, Double>> valoresEstoqueSegurancaPredefinidosPorMaterialPeriodo;

    public SafetyStockProjection(
            Location location,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            Map<Produto, Map<Integer, Double>> valoresEstoqueSegurancaPredefinidosPorMaterialPeriodo) {
        this.location = location;
        this.politicaEstoquesProjection = politicaEstoquesProjection;
        this.valoresEstoqueSegurancaPredefinidosPorMaterialPeriodo =
                valoresEstoqueSegurancaPredefinidosPorMaterialPeriodo == null
                        ? Collections.emptyMap()
                        : valoresEstoqueSegurancaPredefinidosPorMaterialPeriodo;
    }

    /**
     * Indica se existe alguma política de estoques disponível para a location.
     */
    public boolean verificaSeHaPoliticaEstoquesMaterialLocationCadastrada() {
        return politicaEstoquesProjection != null
                && politicaEstoquesProjection.verificaSeHaPoliticaEstoquesMaterialLocationCadastrada();
    }

    /**
     * Replica o gate estrutural do legado para a família
     * {@code GAP_ESTOQUE_SEGURANCA}.
     * <p>
     * Mesmo quando a quantidade efetiva resolvida acabar em zero, a série deve
     * continuar sendo declarada para fins de debug estrutural do modelo.
     */
    public boolean deveDeclararGapEstoqueSeguranca(int periodo, Produto material) {
        if (!verificaSeHaPoliticaEstoquesMaterialLocationCadastrada()) {
            return false;
        }

        return politicaEstoquesProjection.getSNPModeloOperacional(periodo, material, location)
                .equals(Constantes.SNPModeloOperacional.MTS)
                && politicaEstoquesProjection.getSNPEstoqueSegurancaDrpOuTargetKanban(periodo, material, location) > 0.0d;
    }

    /**
     * Resolve a quantidade física efetiva do safety stock na unidade padrão SNP.
     * <p>
     * Regras:
     * - quando o cálculo já está em {@code QUANTITY}, a própria política traz a
     * quantidade final;
     * - nos demais modos, a rodada atual consome apenas o valor previamente
     * resolvido na etapa de safety stock.
     */
    public double getQuantidadeEstoqueSeguranca(int periodo, Produto material) {

        if (!verificaSeHaPoliticaEstoquesMaterialLocationCadastrada()) {
            return 0.0d;
        }

        Constantes.SNPCalculoSafetyStock snpCalculoSafetyStock =
                politicaEstoquesProjection.getSNPModeloCalculoSafetyStock(periodo, material, location);

        if (snpCalculoSafetyStock == null) {
            throw getUnsupportedSafetyStockCalculationException(
                    periodo,
                    material);
        }

        /*
         * O Community executa somente o contrato fisico ja conhecido:
         * QUANTITY vem diretamente da politica; DAYS precisa ter sido
         * previamente convertido para quantidade pela etapa de safety stock.
         * Um valor nulo/futuro aqui indicaria projection incompleta ou schema
         * privado vazando para a borda Community.
         */
        return switch (snpCalculoSafetyStock) {
            case QUANTITY -> politicaEstoquesProjection.getSNPEstoqueSegurancaDrpOuTargetKanban(
                    periodo,
                    material,
                    location);
            case DAYS -> valoresEstoqueSegurancaPredefinidosPorMaterialPeriodo
                    .getOrDefault(material, Collections.emptyMap())
                    .getOrDefault(periodo, 0.0d);
        };

    }

    private IllegalStateException getUnsupportedSafetyStockCalculationException(
            int periodo,
            Produto material) {

        return new IllegalStateException(
                "SafetyStockProjection requires QUANTITY or DAYS safety stock calculation before resolving "
                        + "Community physical quantity; calculation model=null"
                        + ", material="
                        + getMaterialId(material)
                        + ", location="
                        + getLocationId()
                        + ", period="
                        + periodo);

    }

    private String getMaterialId(Produto material) {

        if (material == null) {
            return "null";
        }

        return material.getId();

    }

    private String getLocationId() {

        if (location == null) {
            return "null";
        }

        return location.getId();

    }

}
