package com.opsfactor.community.capability.supplyplanning.distributionplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlanningDataContract;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.Constantes.FirmePlanejado;
import com.opsfactor.community.platform.utility.Constantes.ReferenciaPeriodo;
import com.opsfactor.community.platform.utility.Constantes.TipoPlano;
import lombok.*;
import org.javatuples.Pair;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.ToDoubleFunction;

/**
 * Linha de transferencia ou compra planejada no Supply Plan Community.
 *
 * <p>A entidade guarda quantidades firmes/planejadas para plano irrestrito,
 * restrito e de trabalho. As parcelas de atendimento de demanda direta sao
 * usadas pelo heuristico para separar abastecimento de demanda e formacao de
 * estoque, sem depender de pedidos transacionais Enterprise.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="key")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "distribution_plan_item")
public class DistributionPlanItem {

    /**
     * Chave composta de ReplenishmentPlan
     */
    @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
    @EmbeddedId
    private DistributionPlanItemKey key;

    /**
     * Chave composta de ReplenishmentPlanLinha
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @RequiredArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class DistributionPlanItemKey implements Serializable {

        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @ManyToOne(optional = false)
        private SupplyPlan supplyPlan;

        /**
         * Local onde será feita a reposição de estoques
         */
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location locationDestino;
                
        /**
         * Location de origem do produto. Segue o cadastro das linhas de transporte
         */
        @ManyToOne(optional = false)
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        private Location locationOrigem;
        
        @NonNull // torna campo obrigatório e parâmetro do construtor gerado pelo @Data (lombok)
        @ManyToOne(optional = false)
        private Produto produto;
        
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private LocalDateTime dataExpedicao;
        
        /**
         * A data de referência indica qual o período para o qual se está sugerindo reposição de estoque.
         * Pode representar uma data / semana / mês
         */
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private LocalDateTime dataRecebimento;
                
    }
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedida;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeRequisicaoBaseline;
    /**
     * Quantidade planejada irrestrita da transferencia/compra.
     *
     * <p>Valores nulos representam ausencia operacional e sao tratados como
     * zero pelos getters efetivos. Valores presentes negativos ou nao finitos
     * indicam snapshot de Supply Planning inconsistente e devem falhar antes
     * de alimentar capacidade logistica, estoque em transito ou Planning Book.</p>
     */
    private Double quantidadeOrdemPlanejadaIrrestrita;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeRequisicaoBaselineAtendida;
    private Double quantidadeOrdemPlanejadaRestrita;

    private Double quantidadeOrdemPlanejadaTrabalho;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadePedidoBaseline;
    private Double quantidadeOrdemFirmeIrrestrita;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadePedidoBaselineAtendido;
    private Double quantidadeOrdemFirmeRestrita;

    private Double quantidadeOrdemFirmeTrabalho;

    
    // Usado pelo Constrained Plan heurístico : Indicam a fração das ordens firmes e planejadas
    // que atendem a uma carteira no final da cadeia. O valor restante será para formação de estoque de ciclo/segurança
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeRequisicaoAtendimentoCarteira;
    private Double parcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta;
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadeRequisicaoAtendimentoCarteiraAtendida;
    private Double parcelaOrdemPlanejadaRestritaAtendimentoDemandaDireta;

    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadePedidoAtendimentoCarteira;
    private Double parcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta;
    @Deprecated
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Float quantidadePedidoAtendimentoCarteiraAtendido;
    private Double parcelaOrdemFirmeRestritaAtendimentoDemandaDireta;
    
    /**
     * Construtor que popula datas expedicao e recebimento com base no lead time
     */
    public DistributionPlanItem(
            SupplyPlan supplyPlan,
            Produto material,
            Location locationOrigem, Location locationDestino, 
            Calendario calendario,
            ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodoReferencia,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        this.key = new DistributionPlanItemKey(
                supplyPlan, locationDestino, locationOrigem, material, 
                LocalDateTime.now(), LocalDateTime.now());
        
        Pair<LocalDateTime,LocalDateTime> datasExpedicaoERecebimento = getDatasExpedicaoERecebimentoDeReferencia(
                referenciaPeriodo, calendario, posicaoPeriodoReferencia, getVersaoMalha(), material, 
                locationOrigem, locationDestino, supplyNetworkProjection);
        
        this.key.dataExpedicao = datasExpedicaoERecebimento.getValue0();
        this.key.dataRecebimento = datasExpedicaoERecebimento.getValue1();
        
    }
    
    public DistributionPlanItem(
            SupplyPlan supplyPlan,
            Produto produto,
            Location locationDestino, Location locationOrigem, 
            LocalDate dataExpedicao, LocalDate dataRecebimento) {
        
        this.key = new DistributionPlanItemKey(
                supplyPlan, locationDestino, locationOrigem, produto, dataExpedicao.atStartOfDay(), dataRecebimento.atStartOfDay());

    }

    public Location getLocationDestino() {
        return key.getLocationDestino();
    }
    
    public Produto getProduto() {
        return key.getProduto();
    }
    
    public VersaoMalha getVersaoMalha() {
        return getSupplyPlan().getVersaoMalha();
    }
    
    public UnidadeMedida getUnidadeMedida(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedida == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedida;
    }
    
    public SupplyPlan getSupplyPlan() {
        return key.getSupplyPlan();
    }
    
    public Location getLocationOrigem() {
        return key.getLocationOrigem();
    }
    
    public LocalDateTime getDataExpedicao() {
        return key.getDataExpedicao();
    }
    
    public LocalDateTime getDataRecebimento() {
        return key.getDataRecebimento();
    }

    public double getQuantidadeOrdemPlanejadaIrrestrita() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeRequisicaoBaseline,
                "legacy unrestricted planned distribution")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeOrdemPlanejadaIrrestrita,
                "unrestricted planned distribution");
    }

    public double getQuantidadeOrdemPlanejadaTrabalho() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeOrdemPlanejadaTrabalho,
                "work planned distribution");
    }

    public double getQuantidadeOrdemFirmeTrabalho() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeOrdemFirmeTrabalho,
                "work firm distribution");
    }

    public double getParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeRequisicaoAtendimentoCarteira,
                "legacy unrestricted planned direct demand parcel")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                parcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta,
                "unrestricted planned direct demand parcel");
    }

    public double getQuantidadeOrdemPlanejadaRestrita() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeRequisicaoBaselineAtendida,
                "legacy restricted planned distribution")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeOrdemPlanejadaRestrita,
                "restricted planned distribution");
    }

    public double getParcelaOrdemPlanejadaRestritaAtendimentoDemandaDireta() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeRequisicaoAtendimentoCarteiraAtendida,
                "legacy restricted planned direct demand parcel")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                parcelaOrdemPlanejadaRestritaAtendimentoDemandaDireta,
                "restricted planned direct demand parcel");
    }

    public double getQuantidadeOrdemFirmeIrrestrita() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadePedidoBaseline,
                "legacy unrestricted firm distribution")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeOrdemFirmeIrrestrita,
                "unrestricted firm distribution");
    }

    public double getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadePedidoAtendimentoCarteira,
                "legacy unrestricted firm direct demand parcel")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                parcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta,
                "unrestricted firm direct demand parcel");
    }

    public double getQuantidadeOrdemFirmeRestrita() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadePedidoBaselineAtendido,
                "legacy restricted firm distribution")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadeOrdemFirmeRestrita,
                "restricted firm distribution");
    }

    public double getParcelaOrdemFirmeRestritaAtendimentoDemandaDireta() {
        return getQuantidadeDistribuicaoNaoNegativaOuZero(
                quantidadePedidoAtendimentoCarteiraAtendido,
                "legacy restricted firm direct demand parcel")
                + getQuantidadeDistribuicaoNaoNegativaOuZero(
                parcelaOrdemFirmeRestritaAtendimentoDemandaDireta,
                "restricted firm direct demand parcel");
    }

    /**
     * Move as quatro parcelas baseline de distribuicao para os destinos
     * canônicos da mesma série em uma transação offline já bloqueada.
     *
     * <p>As quatro quantidades efetivas sempre foram a soma de uma parcela
     * baseline e uma parcela canônica. O cutover grava essa soma no campo
     * canônico correspondente e anula a coluna histórica, sem tocar nas
     * parcelas de atendimento direto, no plano de trabalho ou em outra linha.</p>
     *
     * @return quantidade de colunas baseline anuladas nesta linha.
     */
    public int transferLegacyBaselineToCanonicalFields() {

        double plannedOrderUnconstrained = getQuantidadeOrdemPlanejadaIrrestrita();
        double plannedOrderConstrained = getQuantidadeOrdemPlanejadaRestrita();
        double firmOrderUnconstrained = getQuantidadeOrdemFirmeIrrestrita();
        double firmOrderConstrained = getQuantidadeOrdemFirmeRestrita();
        int clearedLegacyColumnCount = 0;

        if (quantidadeRequisicaoBaseline != null) {
            quantidadeOrdemPlanejadaIrrestrita = plannedOrderUnconstrained;
            quantidadeRequisicaoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadeRequisicaoBaselineAtendida != null) {
            quantidadeOrdemPlanejadaRestrita = plannedOrderConstrained;
            quantidadeRequisicaoBaselineAtendida = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadePedidoBaseline != null) {
            quantidadeOrdemFirmeIrrestrita = firmOrderUnconstrained;
            quantidadePedidoBaseline = null;
            clearedLegacyColumnCount++;
        }
        if (quantidadePedidoBaselineAtendido != null) {
            quantidadeOrdemFirmeRestrita = firmOrderConstrained;
            quantidadePedidoBaselineAtendido = null;
            clearedLegacyColumnCount++;
        }

        return clearedLegacyColumnCount;

    }

    /**
     * Valida quantidades persistidas de distribuicao antes de qualquer soma
     * entre coluna legada/nova ou conversao de unidade.
     *
     * <p>Os campos historicos `quantidadeRequisicao*`/`quantidadePedido*` ainda
     * podem existir em bases migradas. Eles entram na mesma regra dos campos
     * novos: `null` e ausencia operacional, mas valor presente negativo ou nao
     * finito torna o snapshot fisico invalido.</p>
     */
    private double getQuantidadeDistribuicaoNaoNegativaOuZero(
            Number quantidadeDistribuicao,
            String nomeCampoQuantidade) {

        if (quantidadeDistribuicao == null) {
            return 0.0d;
        }

        double valorQuantidadeDistribuicao = quantidadeDistribuicao.doubleValue();
        if (!Double.isFinite(valorQuantidadeDistribuicao)
                || valorQuantidadeDistribuicao < 0.0d) {
            throw new IllegalStateException(
                    "Distribution plan quantity "
                            + nomeCampoQuantidade
                            + " must be finite and non-negative for "
                            + getContextoLinhaParaMensagem()
                            + ": "
                            + valorQuantidadeDistribuicao
                            + ".");
        }
        return valorQuantidadeDistribuicao;

    }

    private String getContextoLinhaParaMensagem() {

        return "material "
                + getProdutoIdParaMensagem()
                + " / origin "
                + getLocationOrigemIdParaMensagem()
                + " / destination "
                + getLocationDestinoIdParaMensagem()
                + " / shipping date "
                + getDataExpedicaoParaMensagem()
                + " / receipt date "
                + getDataRecebimentoParaMensagem();

    }

    private String getProdutoIdParaMensagem() {

        if (key == null
                || key.getProduto() == null
                || key.getProduto().getId() == null) {
            return "<sem-material>";
        }
        return key.getProduto().getId();

    }

    private String getLocationOrigemIdParaMensagem() {

        if (key == null
                || key.getLocationOrigem() == null
                || key.getLocationOrigem().getId() == null) {
            return "<sem-origin>";
        }
        return key.getLocationOrigem().getId();

    }

    private String getLocationDestinoIdParaMensagem() {

        if (key == null
                || key.getLocationDestino() == null
                || key.getLocationDestino().getId() == null) {
            return "<sem-destination>";
        }
        return key.getLocationDestino().getId();

    }

    private String getDataExpedicaoParaMensagem() {

        if (key == null
                || key.getDataExpedicao() == null) {
            return "<sem-data-expedicao>";
        }
        return key.getDataExpedicao().toString();

    }

    private String getDataRecebimentoParaMensagem() {

        if (key == null
                || key.getDataRecebimento() == null) {
            return "<sem-data-recebimento>";
        }
        return key.getDataRecebimento().toString();

    }


    public double getQuantidade(FirmePlanejado firmePlanejado, TipoPlano tipoPlano) {
        switch(firmePlanejado) {
            case ORDEM:
                return getQuantidadeOrdemFirme(tipoPlano);
            case PLANEJADO:
                return getQuantidadeOrdemPlanejada(tipoPlano);
            case TOTAL:
                return getQuantidadeOrdemFirme(tipoPlano) + getQuantidadeOrdemPlanejada(tipoPlano);
            default:
                throw unsupportedFirmePlanejado("getQuantidade", firmePlanejado);
        }
    }
    
    public double getParcelaParaAtendimentoIndiretoDemandaDireta(FirmePlanejado firmePlanejado, TipoPlano tipoPlano) {
        switch (firmePlanejado) {            
            case ORDEM:
                return getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(tipoPlano);
            case PLANEJADO:
                return getParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta(tipoPlano);
            case TOTAL:
                return getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(tipoPlano) + getParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta(tipoPlano);
            default:
                throw unsupportedFirmePlanejado("getParcelaParaAtendimentoIndiretoDemandaDireta", firmePlanejado);
        }
    }
    
    public void setParcelaParaAtendimentoDemandaDireta(double valor, FirmePlanejado firmePlanejado, TipoPlano tipoPlano) {
        switch (firmePlanejado) {
            case ORDEM:
                switch (tipoPlano) {
                    case PLANO_IRRESTRITO:
                        setParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(valor);
                        break;
                    case PLANO_RESTRITO:
                        setParcelaOrdemFirmeRestritaAtendimentoDemandaDireta(valor);
                        break;
                    default:
                        throw unsupportedTipoPlanoIrrestritoRestrito("setParcelaParaAtendimentoDemandaDireta", tipoPlano);
                }
                break;
            case PLANEJADO:
                switch (tipoPlano) {
                    case PLANO_IRRESTRITO:
                        setParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta(valor);
                        break;
                    case PLANO_RESTRITO:
                        setParcelaOrdemPlanejadaRestritaAtendimentoDemandaDireta(valor);
                        break;
                    default:
                        throw unsupportedTipoPlanoIrrestritoRestrito("setParcelaParaAtendimentoDemandaDireta", tipoPlano);
                }
                break;
            default:
                throw unsupportedFirmePlanejado("setParcelaParaAtendimentoDemandaDireta", firmePlanejado);
        }
    }
    
    public double getQuantidadeNaUnidadeMedidaTarget(
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        return unidadeMedidaProjection.getConversaoParaUnidadeDestino(getProduto(), getUnidadeMedida(parametrosGlobais), unidadeMedidaTarget)
                * getQuantidade(firmePlanejado, tipoPlano);
        
    }
    
    public double getQuantidadeAtendimentoCarteiraNaUnidadeMedidaTarget(
            FirmePlanejado firmePlanejado, TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget, UnidadeMedidaProjection unidadeMedidaProjection) {
        
        return unidadeMedidaProjection.getConversaoParaUnidadeDestino(getProduto(), unidadeMedidaTarget, unidadeMedidaTarget) 
                * getParcelaParaAtendimentoIndiretoDemandaDireta(firmePlanejado, tipoPlano);
        
    }

    /**
     * Alias funcional Community para a parcela de atendimento da demanda direta.
     *
     * <p>O nome antigo fazia referencia a carteira, mas no recorte Community a
     * unica fonte futura e o Demand Plan. Mantemos o calculo sobre as mesmas
     * parcelas fisicas, com nomenclatura alinhada ao contrato aberto.</p>
     */
    public double getQuantidadeAtendimentoDemandaDiretaNaUnidadeMedidaTarget(
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        return getQuantidadeAtendimentoCarteiraNaUnidadeMedidaTarget(
                firmePlanejado,
                tipoPlano,
                unidadeMedidaTarget,
                unidadeMedidaProjection);

    }
    
    public double getQuantidadeOrdemFirme(TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                return getQuantidadeOrdemFirmeIrrestrita();
            case PLANO_RESTRITO:
                return getQuantidadeOrdemFirmeRestrita();
            case PLANO_TRABALHO:
                return getQuantidadeOrdemFirmeIrrestrita();
            default:
                throw unsupportedTipoPlano("getQuantidadeOrdemFirme", tipoPlano);
        }
    }
    
    public double getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta(TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                return getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta();
            case PLANO_RESTRITO:
                return getParcelaOrdemFirmeRestritaAtendimentoDemandaDireta();
            default:
                throw unsupportedTipoPlanoIrrestritoRestrito("getParcelaOrdemFirmeIrrestritaAtendimentoDemandaDireta", tipoPlano);
        }
    }
    
    public double getQuantidadeOrdemPlanejada(TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                return getQuantidadeOrdemPlanejadaIrrestrita();
            case PLANO_RESTRITO:
                return getQuantidadeOrdemPlanejadaRestrita();
            case PLANO_TRABALHO:
                return getQuantidadeOrdemPlanejadaTrabalho();
            default:
                throw unsupportedTipoPlano("getQuantidadeOrdemPlanejada", tipoPlano);
        }
    }
    
    private void setQuantidadeOrdemPlanejada(double valor, TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeOrdemPlanejadaIrrestrita(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeOrdemPlanejadaRestrita(valor);
                return;
            case PLANO_TRABALHO:
                setQuantidadeOrdemPlanejadaTrabalho(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeOrdemPlanejada", tipoPlano);
        }
    }
    
    private void setQuantidadeOrdemFirme(double valor, TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                setQuantidadeOrdemFirmeIrrestrita(valor);
                return;
            case PLANO_RESTRITO:
                setQuantidadeOrdemFirmeRestrita(valor);
                return;
            case PLANO_TRABALHO:
                setQuantidadeOrdemFirmeTrabalho(valor);
                return;
            default:
                throw unsupportedTipoPlano("setQuantidadeOrdemFirme", tipoPlano);
        }
    }
    
    public void setQuantidade(double valor, FirmePlanejado firmePlanejado, TipoPlano tipoPlano) {
        switch (firmePlanejado) {
            case ORDEM:
                setQuantidadeOrdemFirme(valor, tipoPlano);
                return;
            case PLANEJADO:
                setQuantidadeOrdemPlanejada(valor, tipoPlano);
                return;
            default:
                throw unsupportedFirmePlanejado("setQuantidade", firmePlanejado);
        }
    }
    
    public void setQuantidadeEmUnidadeMedida(
            double valor, 
            UnidadeMedida unidadeMedidaValor,
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano, 
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        double conversaoParaUnidadeMedidaPadraoOrigem = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                unidadeMedidaValor,
                getUnidadeMedida(parametrosGlobais));

        setQuantidade(valor * conversaoParaUnidadeMedidaPadraoOrigem, firmePlanejado, tipoPlano);

    }
    
    public double getParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta(TipoPlano tipoPlano) {
        switch (tipoPlano) {
            case PLANO_IRRESTRITO:
                return getParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta();
            case PLANO_RESTRITO:
                return getParcelaOrdemPlanejadaRestritaAtendimentoDemandaDireta();
            default:
                throw unsupportedTipoPlanoIrrestritoRestrito("getParcelaOrdemPlanejadaIrrestritaAtendimentoDemandaDireta", tipoPlano);
        }
    }
    
    public double getQuantidadeNaUomPadraoLocationOrigem(
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        UnidadeMedida unidadeMedidaPadraoOrigem = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(
                getProduto(),
                getLocationOrigem());
        double conversaoParaUnidadeMedidaPadraoOrigem = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaPadraoOrigem);

        return getQuantidade(firmePlanejado, tipoPlano) * conversaoParaUnidadeMedidaPadraoOrigem;
        
    }
    
    public double getQuantidadeNaUomPadraoLocationDestino(
            FirmePlanejado firmePlanejado,
            TipoPlano tipoPlano,
            ClusterEParametrosProjection clusterEParametrosProjection,
            UnidadeMedidaProjection unidadeMedidaProjection) {

        ParametrosGlobais parametrosGlobais = unidadeMedidaProjection.getParametrosGlobais();
        UnidadeMedida unidadeMedidaPadraoDestino = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(
                getProduto(),
                getLocationDestino());
        double conversaoParaUnidadeMedidaPadraoDestino = unidadeMedidaProjection.getConversaoParaUnidadeDestino(
                getProduto(),
                getUnidadeMedida(parametrosGlobais),
                unidadeMedidaPadraoDestino);

        return getQuantidade(firmePlanejado, tipoPlano) * conversaoParaUnidadeMedidaPadraoDestino;
        
    }

    /**
     * Obtém o periodo de recebimento a partir do periodo de expedição ou vice versa
     * @param referenciaPeriodo
     * @param calendario
     * @param posicaoPeriodoReferencia
     * @param material
     * @param locationOrigem
     * @param locationDestino
     * @param supplyNetworkProjection
     * @return Pair com 0:PosicaoPeriodoExpedicao e 1:PosicaoPeriodoRecebimento
     */
    public static Pair<Integer,Integer> getPosicaoPeriodosExpedicaoERecebimentoDeReferencia (
            ReferenciaPeriodo referenciaPeriodo, Calendario calendario, int posicaoPeriodoReferencia, 
            VersaoMalha versaoMalha,
            Produto material, Location locationOrigem, Location locationDestino,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        // ReferenciaPeriodo = consumo capacidade : periodoExpedicao = periodoReferencia
        // ReferenciaPeriodo = disponibilizacao material : periodoExpeicao = periodoReferencia - lead time dias
        int posicaoPeriodoExpedicao = (referenciaPeriodo.equals(ReferenciaPeriodo.CONSUMO_CAPACIDADE)) ?
                posicaoPeriodoReferencia
                : calendario.getPosicaoPeriodoAposOffsetDoFimPeriodoReferencia(
                        posicaoPeriodoReferencia,
                        - supplyNetworkProjection.getLeadTimeDiasEntreOrigemDestinoParaMaterial(
                                versaoMalha, locationOrigem, locationDestino, material, calendario.getDataHorarioInicialPresente()).orElse(0),
                        Constantes.TamanhoBucket.DIARIO);
        // ReferenciaPeriodo = consumo capacidade : periodoRecebimento = periodoReferencia + lead time dias
        // ReferenciaPeriodo = disponibilizacao material : periodoRecebimento = periodoReferencia 
        int posicaoPeriodoRecebimento = (referenciaPeriodo.equals(ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL)) ?
                posicaoPeriodoReferencia 
                : calendario.getPosicaoPeriodoAposOffsetDoInicioPeriodoReferencia(
                        posicaoPeriodoReferencia, 
                        supplyNetworkProjection.getLeadTimeDiasEntreOrigemDestinoParaMaterial(
                                versaoMalha, locationOrigem, locationDestino, material, calendario.getDataHorarioInicialPresente()).orElse(0),
                        Constantes.TamanhoBucket.DIARIO);
        
        return Pair.with(posicaoPeriodoExpedicao, posicaoPeriodoRecebimento);
        
    }
    
    /**
     * Obtém a data de recebimento a partir do período expedição ou vice versa
     * @param referenciaPeriodo
     * @param calendario
     * @param posicaoPeriodoReferencia
     * @param material
     * @param locationOrigem
     * @param locationDestino
     * @param supplyNetworkProjection
     * @return Pair com 0:data expedicao, apontando para o início do período e 1:data Recebimento, apontando para o final do período
     */
    public static Pair<LocalDateTime,LocalDateTime> getDatasExpedicaoERecebimentoDeReferencia (
            ReferenciaPeriodo referenciaPeriodo, Calendario calendario, int posicaoPeriodoReferencia, 
            VersaoMalha versaoMalha,
            Produto material, Location locationOrigem, Location locationDestino,
            SupplyNetworkProjection supplyNetworkProjection) {
        
        Pair<Integer,Integer> periodosExpedicaoERecebimento = getPosicaoPeriodosExpedicaoERecebimentoDeReferencia(
                referenciaPeriodo, calendario, posicaoPeriodoReferencia, versaoMalha, material, locationOrigem, locationDestino, supplyNetworkProjection);
        
        int posicaoPeriodoExpedicao = periodosExpedicaoERecebimento.getValue0();
        int posicaoPeriodoRecebimento = periodosExpedicaoERecebimento.getValue1();
        
        LocalDateTime dataExpedicao = calendario.getPrimeiraDataHorarioPeriodo(posicaoPeriodoExpedicao);
        LocalDateTime dataRecebimento = calendario.getUltimoSegundoPeriodo(posicaoPeriodoRecebimento);
                
        return Pair.with(dataExpedicao, dataRecebimento);
        
    }
    
    public double getQuantidadeNaUnidadeMedidaTarget(
            ToDoubleFunction<DistributionPlanItem> extratorQuantidade,
            UnidadeMedidaProjection unidadeMedidaProjection,
            UnidadeMedida unidadeMedidaTarget) {
        
        return unidadeMedidaProjection.funcaoGetQuantidadeNaUnidadeTarget(
                extratorQuantidade, 
                DistributionPlanItem::getProduto, 
                x -> x.getUnidadeMedida(unidadeMedidaProjection.getParametrosGlobais()),
                unidadeMedidaTarget)
                .applyAsDouble(this);
        
    }

    private IllegalArgumentException unsupportedTipoPlano(
            String operationName,
            TipoPlano tipoPlano) {

        return SupplyPlanningDataContract.unsupportedTipoPlano(
                DistributionPlanItem.class,
                operationName,
                tipoPlano,
                TipoPlano.PLANO_IRRESTRITO,
                TipoPlano.PLANO_RESTRITO,
                TipoPlano.PLANO_TRABALHO);

    }

    private IllegalArgumentException unsupportedTipoPlanoIrrestritoRestrito(
            String operationName,
            TipoPlano tipoPlano) {

        return SupplyPlanningDataContract.unsupportedTipoPlano(
                DistributionPlanItem.class,
                operationName,
                tipoPlano,
                TipoPlano.PLANO_IRRESTRITO,
                TipoPlano.PLANO_RESTRITO);

    }

    private IllegalArgumentException unsupportedFirmePlanejado(
            String operationName,
            FirmePlanejado firmePlanejado) {

        return SupplyPlanningDataContract.unsupportedFirmePlanejado(
                DistributionPlanItem.class,
                operationName,
                firmePlanejado,
                FirmePlanejado.ORDEM,
                FirmePlanejado.PLANEJADO,
                FirmePlanejado.TOTAL);

    }

}
