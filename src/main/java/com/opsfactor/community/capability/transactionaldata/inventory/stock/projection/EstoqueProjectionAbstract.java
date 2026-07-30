package com.opsfactor.community.capability.transactionaldata.inventory.stock.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedDataInterface;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Base das projections de estoque transacional usadas em calculos Community.
 *
 * <p>O Community trabalha com snapshots quantitativos simples de estoque por
 * material/location/UOM/data. Estoque por lote, validade, aging/writeoff e
 * producao em batch pertencem ao Enterprise.</p>
 */
@SuperBuilder
@Getter
public abstract class EstoqueProjectionAbstract {

    /**
     * Projection base usada para defaults de UOM e contexto de DFU.
     */
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Projection de conversoes entre unidades usada nas leituras quantitativas.
     */
    protected UnidadeMedidaProjection conversaoUnidadeMedidaProjection;

    /**
     * Escopo material/location considerado na extracao.
     */
    protected Set<Location> locations;
    protected Set<Produto> materiais;

    /**
     * Calendario usado para extracao e consultas por posicao de periodo.
     */
    protected Calendario calendario;

    /**
     * UOM default aplicada quando o agregado historico chega sem unidade.
     */
    protected UnidadeMedida unidadeMedidaPadraoParaNulos;

    /**
     * Valida o corpo quantitativo comum de um agregado de estoque antes de
     * qualquer classe filha mutar seu indice em memoria.
     *
     * <p>Diferente de sales, a UOM nula permanece permitida aqui porque a
     * factory de estoque possui fallback operacional explicito para unidade
     * padrao. Quantidade nula ou nao finita, por outro lado, representa
     * snapshot quebrado e nao pode entrar no estoque inicial Community.</p>
     */
    protected void validaEstoqueAgregadoObrigatorio(
            AggregatedDataInterface aggregatedDataInterface,
            String contexto) {

        if (aggregatedDataInterface == null) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate is required for " + contexto + ".");
        }

        if (aggregatedDataInterface.getTotalQuantity() == null) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate quantity is required for " + contexto + ".");
        }

        if (!Double.isFinite(aggregatedDataInterface.getTotalQuantity())) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate quantity must be finite for " + contexto + ".");
        }

    }

    /**
     * Valida a chave material do agregado antes de usa-la como chave de mapa.
     *
     * <p>O objeto material em si nao basta para indexacao confiavel. Snapshots
     * vindos de repository, testes ou overlays Enterprise podem reconstruir
     * entidades diferentes; por isso o id funcional precisa existir antes de a
     * projection aceitar a linha no mapa.</p>
     */
    protected void validaEstoqueAgregadoMaterialObrigatorio(
            Produto material,
            String contexto) {

        if (material == null) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate material is required for " + contexto + ".");
        }
        if (material.getId() == null || material.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate material id is required for " + contexto + ".");
        }

    }

    /**
     * Valida a chave location do agregado antes de usa-la como chave de mapa.
     */
    protected void validaEstoqueAgregadoLocationObrigatoria(
            Location location,
            String contexto) {

        if (location == null) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate location is required for " + contexto + ".");
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate location id is required for " + contexto + ".");
        }

    }

    /**
     * Valida a data de referencia antes de indexar agregados temporais.
     */
    protected void validaEstoqueAgregadoReferenceDateObrigatoria(
            LocalDate referenceDate,
            String contexto) {

        if (referenceDate == null) {
            throw new IllegalArgumentException(
                    "Stock projection aggregate reference date is required for " + contexto + ".");
        }

    }

}
