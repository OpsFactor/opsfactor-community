package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.transactionaldata.common.aggregation.projection.AggregatedDataInterface;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Base das projections de vendas historicas observadas.
 *
 * <p>No Community, a fonte funcional de historico e o sell-out quantitativo.
 * Sell-in, pedidos, valores, campanhas e eventos pertencem ao Enterprise. O
 * package e as APIs da projection usam nomes neutros de sales para que o
 * Enterprise consiga complementar a origem documental sem mudar consumidores.</p>
 */
@SuperBuilder
@NoArgsConstructor
@Getter
public abstract class SalesProjectionAbstract {
    
    /**
     * Parametros de ambiente usados por projections que precisam resolver UOM
     * padrao ou regras de conversao. A factory Community ja normaliza UOM nula
     * nos agregados lidos do repository; manter este campo explicito ajuda os
     * overlays Enterprise a seguirem o mesmo contrato.
     */
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Projection de conversao entre unidades. Consumers devem usar este objeto
     * para converter quantidade historica em vez de consultar entidades ou
     * repositories durante calculos.
     */
    protected UnidadeMedidaProjection conversaoUnidadeMedidaProjection;

    /**
     * Calendario da extracao de sales. Chamadas com calendario externo devem
     * validar bucket antes de converter posicoes em datas.
     */
    protected Calendario calendario;

    /**
     * Escopo material/location usado para montar a projection. Campos nulos
     * representam extracao sem filtro em alguns helpers historicos; callers
     * novos devem preferir conjuntos explicitos quando possivel.
     */
    protected Set<Location> locations;
    protected Set<Produto> materiais;

    /**
     * UOM default usada pela factory quando o agregado vem sem unidade fisica.
     * O valor fica registrado na projection para debug/overlays, mas os
     * agregados devem entrar ja normalizados para evitar conversao ambigua.
     */
    protected UnidadeMedida unidadeMedidaPadraoParaNulos;

    /**
     * Valida o corpo quantitativo comum de um agregado antes de qualquer classe
     * filha mutar seu indice em memoria.
     *
     * <p>Snapshot vazio e ausencia operacional valida. Linha nula, quantidade
     * ausente/nao finita ou UOM ausente indicam snapshot quebrado de repository
     * ou overlay Enterprise e devem falhar neste ponto, antes de ficarem
     * escondidos dentro dos mapas de leitura rapida.</p>
     */
    protected void validaSalesAgregadoObrigatorio(
            AggregatedDataInterface aggregatedDataInterface,
            String contexto) {

        if (aggregatedDataInterface == null) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate is required for " + contexto + ".");
        }

        if (aggregatedDataInterface.getTotalQuantity() == null) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate quantity is required for " + contexto + ".");
        }

        if (!Double.isFinite(aggregatedDataInterface.getTotalQuantity())) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate quantity must be finite for " + contexto + ".");
        }

        if (aggregatedDataInterface.getUom() == null) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate UOM is required for " + contexto + ".");
        }

    }

    /**
     * Valida a chave material do agregado antes de usa-la como chave de mapa.
     *
     * <p>O objeto material em si nao basta para indexacao confiavel. Snapshots
     * vindos de repository, stubs ou overlays Enterprise podem reconstruir
     * entidades diferentes; por isso o id funcional precisa existir antes de a
     * projection aceitar a linha no mapa.</p>
     */
    protected void validaSalesAgregadoMaterialObrigatorio(
            Produto material,
            String contexto) {

        if (material == null) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate material is required for " + contexto + ".");
        }
        if (material.getId() == null || material.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate material id is required for " + contexto + ".");
        }

    }

    /**
     * Valida a chave location do agregado antes de usa-la como chave de mapa.
     */
    protected void validaSalesAgregadoLocationObrigatoria(
            Location location,
            String contexto) {

        if (location == null) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate location is required for " + contexto + ".");
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate location id is required for " + contexto + ".");
        }

    }

    /**
     * Valida a data de referencia antes de indexar agregados temporais.
     */
    protected void validaSalesAgregadoReferenceDateObrigatoria(
            LocalDate referenceDate,
            String contexto) {

        if (referenceDate == null) {
            throw new IllegalArgumentException(
                    "Sales projection aggregate reference date is required for " + contexto + ".");
        }

    }
    
}
