package com.opsfactor.community.capability.supplyplanning.productionplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlanningDataContract;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Linha de producao planejada do Supply Plan Community.
 *
 * <p>A linha armazena producao firme e planejada para os planos irrestrito,
 * restrito e de trabalho. Valores antigos de baseline permanecem apenas para
 * compatibilidade de bases migradas; novos calculos Community devem usar as
 * colunas explicitas de ordem firme/planejada.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="productionPlanLinhaCompositeKey")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class ProductionPlanLinha {

    /**
     * Chave composta de ReplenishmentPlan
     */
    @EmbeddedId
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    private ProductionPlanLinhaCompositeKey productionPlanLinhaCompositeKey;

    /**
     * Chave composta de ReplenishmentPlanLinha
     */
    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class ProductionPlanLinhaCompositeKey implements Serializable {

        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private SupplyPlan supplyPlan;

        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location location;
                
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private VersaoProducao versaoProducao;
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Roteiro roteiro;
        
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private ListaTecnica listaTecnica;
        
        /**
         * A data de referência indica qual o período para o qual se está sugerindo reposição de estoque.
         * Pode representar uma data / semana / mês
         */
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private LocalDateTime dataReferencia;
        
    }
        
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedida;
    
    @NonNull
    @ManyToOne(optional = false)
    private Produto materialOutput;
        
    // produção sem considerar restrição do recurso produtivo
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeSugestaoProducaoBaseline;
    private Double quantidadeOrdemPlanejadaProducaoIrrestrita;

    // produção após restrições e fair share do recurso produtivo
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeSugestaoProducaoBaselineAtendida;
    private Double quantidadeOrdemPlanejadaProducaoRestrita;

    // versão de trabalho para ajustes manuais
    private Double quantidadeOrdemPlanejadaProducaoTrabalho;

    // representa a quantidade de ordens de produção firmes
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeOrdemProducaoBaseline;
    private Double quantidadeOrdemFirmeProducaoIrrestrita;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeOrdemProducaoBaselineAtendida;
    private Double quantidadeOrdemFirmeProducaoRestrita;

    // versão de trabalho para ajustes manuais
    private Double quantidadeOrdemFirmeProducaoTrabalho;

    public SupplyPlan getSupplyPlan() {
        return productionPlanLinhaCompositeKey.getSupplyPlan();
    }
    
    public VersaoProducao getVersaoProducao() {
        return productionPlanLinhaCompositeKey.getVersaoProducao();
    }
        
    public Roteiro getRoteiro() {
        return getProductionPlanLinhaCompositeKey().getRoteiro();
    }
    
    public ListaTecnica getListaTecnica() {
        return getProductionPlanLinhaCompositeKey().getListaTecnica();
    }
    
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }
    
    public UnidadeMedida getUnidadeMedidaCadastrado() {
        return unidadeMedida;
    }
    
    public LocalDateTime getDataReferencia() {
        return productionPlanLinhaCompositeKey.getDataReferencia();
    }
        
    /**
     * Retorna os materiais input da lista técnica do production plan linha.
     * Usa a lista técnica armazenada em supplyNetworkProjection, para garantir que 
     * dados de componentes não precisem ser extraídos via Lazy Loading
     * @param supplyNetworkProjection
     * @return 
     */
    public Set<Produto> getMateriaisInput(SupplyNetworkProjection supplyNetworkProjection) {
        // extrai lista técnica de SupplyNetworkProjection para evitar erro 'failed to lazily initialize a collection of role : com.opsfactor.community.capability.masterdata.domain.production.ListaTecnica.listaTecnicaComponenteSet'
        ListaTecnica listaTecnica = supplyNetworkProjection.getListaTecnicaFromId(
                productionPlanLinhaCompositeKey.getListaTecnica().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Lista técnica "
                                + productionPlanLinhaCompositeKey.getListaTecnica().getId()
                                + " não encontrada na SupplyNetworkProjection"));
        return listaTecnica.getMateriaisInput();
    }
        
    public Location getLocation() {
        return productionPlanLinhaCompositeKey.getLocation();
    }
    
    public VersaoProducao getVersaoProducaoCadastrada() {
        return getProductionPlanLinhaCompositeKey().getVersaoProducao();
    }
    
    /**
     * Se versao producao for do tipo inexistente, retorna nulo
     * caso contrário (cadastrada ou temporária), retorna a própria versão produção
     * @return
     */
    public VersaoProducao getVersaoProducaoAlocadaOuNulaSeInexistente() {
        return getVersaoProducaoCadastrada().isVersaoProducaoInexistente() ? null : getVersaoProducaoCadastrada();
    }
    
    /**
     * Se versao producao for do tipo inexistente ou temporária, retorna nulo
     * caso contrário (cadastrada), retorna a própria versão produção
     * @return 
     */
    public VersaoProducao getVersaoProducaoAlocadaOuNulaSeInexistenteOuTemporaria() {
        return (getVersaoProducaoCadastrada().isVersaoProducaoInexistente() || getVersaoProducaoCadastrada().isVersaoProducaoTemporaria()) ? null : getVersaoProducaoCadastrada();
    }
    
    /**
     * Se versao producao for do tipo inexistente, retorna a versao producao temporaria para o roteiro/lista tecnica
     * caso contrário (cadastrada ou temporária), retorna a própria versão produção
     * @param supplyNetworkProjection
     * @return 
     */
    public VersaoProducao getVersaoProducaoAlocadaOuTemporariaSeInexistente(SupplyNetworkProjection supplyNetworkProjection) {
        return VersaoProducao.getVersaoProducaoAlocadaOuTemporariaSeInexistente(
                getVersaoProducaoCadastrada(), getRoteiro(), getListaTecnica(), supplyNetworkProjection);
    }
    
    public double getQuantidadeOrdemPlanejadaProducaoIrrestrita() {
        return Math.max(
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeSugestaoProducaoBaseline,
                        "legacy unrestricted planned production"),
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeOrdemPlanejadaProducaoIrrestrita,
                        "unrestricted planned production"));
    }

    public double getQuantidadeOrdemPlanejadaProducaoTrabalho() {
        return getQuantidadeProducaoNaoNegativaOuZero(
                quantidadeOrdemPlanejadaProducaoTrabalho,
                "work planned production");
    }

    public double getQuantidadeOrdemFirmeProducaoTrabalho() {
        return getQuantidadeProducaoNaoNegativaOuZero(
                quantidadeOrdemFirmeProducaoTrabalho,
                "work firm production");
    }

    public double getQuantidadeOrdemPlanejadaProducaoRestrita() {
        return Math.max(
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeSugestaoProducaoBaselineAtendida,
                        "legacy restricted planned production"),
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeOrdemPlanejadaProducaoRestrita,
                        "restricted planned production"));
    }

    public double getQuantidadeOrdemFirmeProducaoIrrestrita() {
        return Math.max(
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeOrdemProducaoBaseline,
                        "legacy unrestricted firm production"),
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeOrdemFirmeProducaoIrrestrita,
                        "unrestricted firm production"));
    }

    public double getQuantidadeOrdemFirmeProducaoRestrita() {
        return Math.max(
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeOrdemProducaoBaselineAtendida,
                        "legacy restricted firm production"),
                getQuantidadeProducaoNaoNegativaOuZero(
                        quantidadeOrdemFirmeProducaoRestrita,
                        "restricted firm production"));
    }

    /**
     * Regulariza as parcelas baseline de producao na coluna canônica da mesma
     * série, preservando a regra histórica de maior valor efetivo.
     *
     * <p>Produção não soma baseline e valor canônico: cada getter já publicava
     * o maior entre ambos. O cutover materializa esse máximo no destino e só
     * então anula a origem. Assim não escolhe uma precedência nova quando as
     * duas colunas coexistem e não altera Working Plan.</p>
     *
     * @return quantidade de colunas baseline anuladas nesta linha.
     */
    public int transferLegacyBaselineToCanonicalFields() {

        double plannedProductionUnconstrained = getQuantidadeOrdemPlanejadaProducaoIrrestrita();
        double plannedProductionConstrained = getQuantidadeOrdemPlanejadaProducaoRestrita();
        double firmProductionUnconstrained = getQuantidadeOrdemFirmeProducaoIrrestrita();
        double firmProductionConstrained = getQuantidadeOrdemFirmeProducaoRestrita();
        int clearedLegacyColumnCount = 0;

        if (quantidadeSugestaoProducaoBaseline != null) {
            quantidadeOrdemPlanejadaProducaoIrrestrita = plannedProductionUnconstrained;
            quantidadeSugestaoProducaoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeSugestaoProducaoBaselineAtendida != null) {
            quantidadeOrdemPlanejadaProducaoRestrita = plannedProductionConstrained;
            quantidadeSugestaoProducaoBaselineAtendida = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeOrdemProducaoBaseline != null) {
            quantidadeOrdemFirmeProducaoIrrestrita = firmProductionUnconstrained;
            quantidadeOrdemProducaoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeOrdemProducaoBaselineAtendida != null) {
            quantidadeOrdemFirmeProducaoRestrita = firmProductionConstrained;
            quantidadeOrdemProducaoBaselineAtendida = null;
            clearedLegacyColumnCount++;
        }

        return clearedLegacyColumnCount;

    }

    /**
     * Valida quantidades persistidas de producao antes de qualquer soma,
     * comparacao entre coluna legada/nova ou conversao de unidade.
     *
     * <p>Campos nulos continuam ausencia operacional zero. Valor presente
     * negativo ou nao finito indica dado corrompido de planejamento e nao pode
     * ser mascarado por `Math.max(0, quantidade)`, pois essas linhas alimentam
     * capacidade consumida, upper bounds Enterprise e Planning Book.</p>
     */
    private double getQuantidadeProducaoNaoNegativaOuZero(
            Number quantidadeProducao,
            String nomeCampoQuantidade) {

        if (quantidadeProducao == null) {
            return 0.0d;
        }

        double valorQuantidadeProducao = quantidadeProducao.doubleValue();
        if (!Double.isFinite(valorQuantidadeProducao)
                || valorQuantidadeProducao < 0.0d) {
            throw new IllegalStateException(
                    "Production plan quantity "
                            + nomeCampoQuantidade
                            + " must be finite and non-negative for "
                            + getContextoLinhaParaMensagem()
                            + ": "
                            + valorQuantidadeProducao
                            + ".");
        }
        return valorQuantidadeProducao;

    }

    private String getContextoLinhaParaMensagem() {

        return "material "
                + getMaterialOutputIdParaMensagem()
                + " / location "
                + getLocationIdParaMensagem()
                + " / reference date "
                + getDataReferenciaParaMensagem();

    }

    private String getMaterialOutputIdParaMensagem() {

        return (materialOutput == null || materialOutput.getId() == null)
                ? "<sem-material>"
                : materialOutput.getId();

    }

    private String getLocationIdParaMensagem() {

        if (productionPlanLinhaCompositeKey == null
                || productionPlanLinhaCompositeKey.getLocation() == null
                || productionPlanLinhaCompositeKey.getLocation().getId() == null) {
            return "<sem-location>";
        }
        return productionPlanLinhaCompositeKey.getLocation().getId();

    }

    private String getDataReferenciaParaMensagem() {

        if (productionPlanLinhaCompositeKey == null
                || productionPlanLinhaCompositeKey.getDataReferencia() == null) {
            return "<sem-data>";
        }
        return productionPlanLinhaCompositeKey.getDataReferencia().toString();

    }
    
    public double getQuantidade(
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaTarget = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterialOutput(), getUnidadeMedida(parametrosGlobais), unidadeMedidaTarget);

        return getQuantidade(tipoPlano, firmePlanejado) * conversaoParaUnidadeMedidaTarget;

    }
    
    public double getQuantidade(
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado) {
                
        switch(firmePlanejado) {
            case ORDEM:
                return getQuantidadeOrdemFirmeProducao(tipoPlano);
            case PLANEJADO:
                return getQuantidadeOrdemPlanejadaProducao(tipoPlano);
            case TOTAL:
                return (getQuantidadeOrdemFirmeProducao(tipoPlano) + getQuantidadeOrdemPlanejadaProducao(tipoPlano));
            default:
                throw unsupportedFirmePlanejado("getQuantidade", firmePlanejado);
        }
        
    }
        
    private double getQuantidadeOrdemFirmeProducao(Constantes.TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                return getQuantidadeOrdemFirmeProducaoIrrestrita();
            case PLANO_RESTRITO:
                return getQuantidadeOrdemFirmeProducaoRestrita();
            case PLANO_TRABALHO:
                return getQuantidadeOrdemFirmeProducaoTrabalho(); // retorna o irrestrito
            default:
                throw unsupportedTipoPlano("getQuantidadeOrdemFirmeProducao", tipoPlano);
        }
    }
    
    private double getQuantidadeOrdemPlanejadaProducao(Constantes.TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                return getQuantidadeOrdemPlanejadaProducaoIrrestrita();
            case PLANO_RESTRITO:
                return getQuantidadeOrdemPlanejadaProducaoRestrita();
            case PLANO_TRABALHO:
                return getQuantidadeOrdemPlanejadaProducaoTrabalho();
            default:
                throw unsupportedTipoPlano("getQuantidadeOrdemPlanejadaProducao", tipoPlano);
        }
    }

    public void setQuantidade(
            double valor,
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaValor,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaProductionPlanLinha = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterialOutput(), unidadeMedidaValor, getUnidadeMedida(parametrosGlobais));
        
        switch(firmePlanejado) {
            case ORDEM:
                setQuantidadeOrdemFirmeProducao(valor * conversaoParaUnidadeMedidaProductionPlanLinha, tipoPlano);
                return;
            case PLANEJADO:
                setQuantidadeOrdemPlanejadaProducao(valor * conversaoParaUnidadeMedidaProductionPlanLinha, tipoPlano);
                return;
            default:
                throw unsupportedFirmePlanejado("setQuantidade", firmePlanejado);
        }
    }
        
    private void setQuantidadeOrdemFirmeProducao(double valor, Constantes.TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeOrdemFirmeProducaoIrrestrita(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeOrdemFirmeProducaoRestrita(valor);
                return;
            case PLANO_TRABALHO:
                setQuantidadeOrdemFirmeProducaoTrabalho(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeOrdemFirmeProducao", tipoPlano);
        }
    }
    
    private void setQuantidadeOrdemPlanejadaProducao(double valor, Constantes.TipoPlano tipoPlano)  {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeOrdemPlanejadaProducaoIrrestrita(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeOrdemPlanejadaProducaoRestrita(valor);
                return;
            case PLANO_TRABALHO:
                setQuantidadeOrdemPlanejadaProducaoTrabalho(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeOrdemPlanejadaProducao", tipoPlano);
        }
    }

    private IllegalArgumentException unsupportedTipoPlano(
            String operationName,
            Constantes.TipoPlano tipoPlano) {

        return SupplyPlanningDataContract.unsupportedTipoPlano(
                ProductionPlanLinha.class,
                operationName,
                tipoPlano,
                Constantes.TipoPlano.PLANO_IRRESTRITO,
                Constantes.TipoPlano.PLANO_RESTRITO,
                Constantes.TipoPlano.PLANO_TRABALHO);

    }

    private IllegalArgumentException unsupportedFirmePlanejado(
            String operationName,
            Constantes.FirmePlanejado firmePlanejado) {

        return SupplyPlanningDataContract.unsupportedFirmePlanejado(
                ProductionPlanLinha.class,
                operationName,
                firmePlanejado,
                Constantes.FirmePlanejado.ORDEM,
                Constantes.FirmePlanejado.PLANEJADO,
                Constantes.FirmePlanejado.TOTAL);

    }
    
    public double getQuantidadeNaUnidadeMedidaTarget(
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeTarget = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getMaterialOutput(), getUnidadeMedida(parametrosGlobais), unidadeMedidaTarget);

        return getQuantidade(tipoPlano, firmePlanejado) * conversaoParaUnidadeTarget;
        
    }

    /**
     * 1) Unidade output production plan linha -> Unidade output lista técnica
     * 2) Qtde a unidade output lista técnica -> Qtde na unidade input lista técnica
     * 3) Unidade input lista técnica -> Unidade target indicada
     * @return
     */
    public double getQuantidadeMaterialInputConsumidoNoProductionPlanLinhaPorUnidadeProductionPlanLinhaOutput(
            Produto materialInput,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadeMedidaMaterialInput) {
                
        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();
        
        return getListaTecnica().getQuantidadeInputDeQuantidadeOutput(
                materialInput, 
                getUnidadeMedida(parametrosGlobais),
                1, 
                unidadeMedidaMaterialInput,
                unidadeMedidaProjection)
                .orElse(0f);

    }

    public double getQuantidadeMaterialInputConsumido(
            Produto materialInput,
            Constantes.FirmePlanejado firmePlanejado,
            Constantes.TipoPlano tipoPlano,
            UnidadeMedidaProjection unidadeMedidaProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedida unidadeMedidaMaterialInput) {

        double quantidadeTargetUOMMaterialInputPorUnidadeOutput = getQuantidadeMaterialInputConsumidoNoProductionPlanLinhaPorUnidadeProductionPlanLinhaOutput(
                materialInput, unidadeMedidaProjection, clusterEParametrosProjection, unidadeMedidaMaterialInput);

        return getQuantidade(tipoPlano, firmePlanejado) * quantidadeTargetUOMMaterialInputPorUnidadeOutput;
        
    }

    /**
     * Retorna o consumo total em horas ou quantidade (sugestões + ordens firmes) consumidas 
     * por recurso produtivo para este production plan linha
     * A definição entre horas/quantidade se dá através do TipoCapacidade do RecursoProdutivo
     * @return
     */
    public Map<RecursoProdutivo,Double> getCapacidadeConsumidaPorRecursoProdutivoEmHorasOuQuantidade(
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado,
            PerfilExecucaoSupplyPlan.TipoCapacidadeProdutiva tipoCapacidadeProdutiva,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        ParametrosGlobais parametrosGlobais = supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais();
        
        double quantidade = getQuantidade(
                tipoPlano, firmePlanejado,
                getUnidadeMedida(parametrosGlobais), 
                supplyNetworkProjection.getConversaoUnidadeMedidaProjection());

        return supplyNetworkProjection.getConsumoCapacidadePorRecursoProdutivoEmHorasOuQuantidade(
                getRoteiro(), quantidade, getUnidadeMedida(parametrosGlobais), tipoCapacidadeProdutiva);
        
    }
    
    public Map<RecursoProdutivo,Double> getCapacidadeConsumidaPorRecursoProdutivoEmHoras(
            Constantes.TipoPlano tipoPlano,
            Constantes.FirmePlanejado firmePlanejado,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        ParametrosGlobais parametrosGlobais = supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais();

        double quantidade = getQuantidade(
                tipoPlano, firmePlanejado,
                getUnidadeMedida(parametrosGlobais), 
                supplyNetworkProjection.getConversaoUnidadeMedidaProjection());

        return supplyNetworkProjection.getConsumoCapacidadePorRecursoProdutivoEmHoras(
                getRoteiro(), quantidade, getUnidadeMedida(parametrosGlobais));
        
    }
        
    /**
     * Verifica alinhamento location e material output com roteiro e lista tecnica
     */
    public void verificaConsistencia() {
        if (!getLocation().equals(getRoteiro().getLocation())) throw new IllegalStateException("Location " + getLocation().getId() + " different than routing location " + getRoteiro().getLocation().getId());
        if (!getMaterialOutput().equals(getRoteiro().getMaterialOutput())) throw new IllegalStateException("Output Material " + getMaterialOutput().getId() + " different than routing Output Material " + getRoteiro().getMaterialOutput().getId());

        if (!getLocation().equals(getListaTecnica().getLocation())) throw new IllegalStateException("Location " + getLocation().getId() + " different than bill of materials location " + getListaTecnica().getLocation().getId());
        if (!getMaterialOutput().equals(getListaTecnica().getMaterialOutput())) throw new IllegalStateException("Output Material " + getMaterialOutput().getId() + " different than bill of materials Output Material " + getListaTecnica().getMaterialOutput().getId());
    }

}
