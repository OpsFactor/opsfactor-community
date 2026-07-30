package com.opsfactor.community.capability.configuration.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.OptionalDouble;

/**
 * Parametros operacionais especificos para uma combinacao material/location.
 *
 * <p>No Community, esta entidade concentra status de ciclo de vida, unidade de
 * medida, lote/multiplo produtivo e horizonte congelado de Demand Planning.
 * Caracteristicas material/location dinamicas, filtros e estruturas de
 * agregacao ficam no Enterprise e nao sao mantidas neste recorte.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor 
@RequiredArgsConstructor
@EqualsAndHashCode(of = "parametrosProdutoLocationCompositeKey")
public class ParametrosProdutoLocation implements Serializable {

    @EmbeddedId 
    @NonNull
    private ParametrosProdutoLocationCompositeKey parametrosProdutoLocationCompositeKey;

    @Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
    @Embeddable
    @AllArgsConstructor 
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class ParametrosProdutoLocationCompositeKey implements Serializable {

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Produto produto;

        @NonNull
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        private Location location;

    }

    /**
     * O uso de frequenciaReabastecimentoDias passou de um input para o cálculo do estoque máximo para um parâmetro usado
     * exclusivamente no módulo de otimização de estoques, servindo de meio para cálculo do estoque máximo a partir dos
     * diferentes safety stocks simulados
     */
    private Double frequenciaReabastecimentoDias; // valor padrao = 0 dias (ressuprimento a qualquer momento). pode ser especificado no nível politica estoques material/location

    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaLoteMinimoMultiploProducao;

    /*
     * Restricoes fisicas de producao por material/location. Null significa
     * ausencia operacional; valor presente deve ser validado aqui, antes de
     * chegar a ClusterEParametrosProjection, heuristico ou optimizer.
     */
    @Getter(AccessLevel.NONE)
    private Double loteMinimoProducao; 

    /*
     * Multiplo presente ativa arredondamento inteiro de producao. Zero ou valor
     * negativo nao significa ausencia: e cadastro invalido que quebraria
     * divisoes/arredondamentos em supply planning.
     */
    @Getter(AccessLevel.NONE)
    private Double multiploProducao;

    private Boolean ativo;

    @Getter(AccessLevel.NONE) // único meio de obter é através de getStatusMaterialCadastrado para evitar implementações incorretas
    @Enumerated(EnumType.ORDINAL)
    private Constantes.StatusProduto estagioCicloVida;
    private LocalDateTime dataIntroducao;
    private LocalDateTime dataDescontinuacao;
    
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaPadrao;
    
    /**
     * Para Planning Book DP: horizonte dentro do qual nao se pode ajustar o
     * forecast.
     *
     * <p>Null preserva a ausencia operacional historica e significa zero dias
     * congelados. Valor cadastrado negativo e inconsistencia de configuracao,
     * pois a projection converte este campo para periodos de calendario antes
     * de bloquear ajustes manuais.</p>
     */
    private Integer numeroDiasHorizonteCongeladoDp;
    
    @Getter(AccessLevel.NONE)
    private Double prazoValidadeDias;
    /**
     * Indica quantas horas após a produção o produto precisa para poder ser liberado
     */
    @Getter(AccessLevel.NONE)
    private Double tempoProcessoDias;
        
    public Produto getProduto() {
        return parametrosProdutoLocationCompositeKey.getProduto();
    }
    
    public Location getLocation() {
        return parametrosProdutoLocationCompositeKey.getLocation();
    }

    public Double getFrequenciaReabastecimentoDias() {
        return frequenciaReabastecimentoDias;
    }
    public Double getFrequenciaReabastecimentoDiasCadastrado() {
        return frequenciaReabastecimentoDias;
    }
    
    /**
     * Usado somente para carga de dados 'DataUpload' . Para demais usos usar getLoteMinimoRequisicoes
     * @return 
     */
    public Double getLoteMinimoProducaoCadastrado() {
        return loteMinimoProducao;
    }
    
    public OptionalDouble getLoteMinimoProducao() {

        return (loteMinimoProducao == null)
                ? OptionalDouble.empty()
                : OptionalDouble.of(loteMinimoProducao);

    }
    
    /**
     * Usado somente para carga de dados 'DataUpload' . Para demais usos usar getMultiploRequisicoes
     * @return 
     */
    public Double getMultiploProducaoCadastrado() {
        return multiploProducao;
    }
    
    /**
     * Retorna o multiplo de producao. Valor nulo indica ausencia operacional;
     * valor cadastrado precisa ser finito e positivo porque sera usado como
     * divisor no arredondamento de necessidade produtiva.
     * @return 
     */
    public OptionalDouble getMultiploProducao() {

        return (multiploProducao == null)
                ? OptionalDouble.empty()
                : OptionalDouble.of(multiploProducao);

    }

    /**
     * Valida parametros produtivos opcionais que aceitam zero como ausencia
     * efetiva de quantidade minima, mas nao aceitam negativo ou valor nao
     * finito.
     */
    /**
     * Valida parametros produtivos opcionais que, quando presentes, ativam
     * arredondamento por multiplo.
     */
        public Constantes.StatusProduto getStatusProduto(LocalDateTime dataReferencia, ParametrosGlobais parametrosGlobais) {
        if (estagioCicloVida != null) return estagioCicloVida;

        Constantes.StatusProduto statusProdutoPadrao = getProduto().getStatusProduto(dataReferencia, parametrosGlobais);
        if (dataDescontinuacao == null && dataIntroducao == null) {
            return statusProdutoPadrao;
        }
        
        if (statusProdutoPadrao.equals(Constantes.StatusProduto.DESCONTINUADO)) {
            return Constantes.StatusProduto.DESCONTINUADO;
        }
        
        if (dataDescontinuacao != null) {
            if (dataReferencia.isAfter(dataDescontinuacao) || dataReferencia.equals(dataDescontinuacao)) {
                return Constantes.StatusProduto.DESCONTINUADO;
            }
        }
        
        if (dataIntroducao == null) {
            if (statusProdutoPadrao.equals(Constantes.StatusProduto.REGULAR)) {
                return Constantes.StatusProduto.REGULAR;
            } else {
                return statusProdutoPadrao;
            }
        } else {
            int numeroDiasMaterialNovo = parametrosGlobais.getNumeroDiasProdutoNovo();
            if (numeroDiasMaterialNovo <= 0) {
                /*
                 * Community não possui janela funcional de material novo. O
                 * override material/location pode antecipar "not launched",
                 * mas não pode reabrir o status NEW no dia de introdução.
                 */
                return dataReferencia.isBefore(dataIntroducao)
                        ? Constantes.StatusProduto.NAO_LANCADO
                        : Constantes.StatusProduto.REGULAR;
            }

            LocalDateTime dataFinalNovo = dataIntroducao.plusDays(numeroDiasMaterialNovo);
            if (dataReferencia.isAfter(dataFinalNovo)) {
                return Constantes.StatusProduto.REGULAR;
            } else if (dataReferencia.isBefore(dataIntroducao)) {
                return Constantes.StatusProduto.NAO_LANCADO;
            } else {
                return Constantes.StatusProduto.NOVO;
            }
        }
    }

    public boolean getInativo() {
        return !getAtivo();
    }
    
    public boolean getAtivo() {
        if(!getProduto().getAtivo()) return false;
        return (ativo == null) ? getProduto().getAtivo() : ativo;
    }

    public Constantes.StatusProduto getEstagioCicloVidaCadastrado() {
        return estagioCicloVida;
    }

    /**
     * Usado somente para carga de dados 'DataUpload' . Para demais usos usar getAtivo
     * @return 
     */    
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    public UnidadeMedida getUnidadeMedidaPadrao(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaPadrao == null) ? getProduto().getUnidadeMedidaPadrao(parametrosGlobais) : unidadeMedidaPadrao;
    }
    
    public UnidadeMedida getUnidadeMedidaPadraoCadastrado() {
        return unidadeMedidaPadrao;
    }
    
    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploProducao(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaLoteMinimoMultiploProducao == null) ? getUnidadeMedidaPadrao(parametrosGlobais) : unidadeMedidaLoteMinimoMultiploProducao;
    }
    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado() {
        return unidadeMedidaLoteMinimoMultiploProducao;
    }
    
    public Integer getNumeroDiasHorizonteCongeladoDp() {
        if (numeroDiasHorizonteCongeladoDp == null) {
            return 0;
        }
        if (numeroDiasHorizonteCongeladoDp < 0) {
            throw new IllegalStateException(
                    "Frozen Demand Planning horizon must be non-negative for material "
                            + getProduto().getId()
                            + " / location "
                            + getLocation().getId()
                            + ": "
                            + numeroDiasHorizonteCongeladoDp
                            + ".");
        }
        return numeroDiasHorizonteCongeladoDp;
    } 
    public Integer getNumeroDiasHorizonteCongeladoDpCadastrado() {
        return numeroDiasHorizonteCongeladoDp;
    } 
    
    public Double getPrazoValidadeDiasCadastrado() {
        return prazoValidadeDias;
    }
    public Double getTempoProcessoDiasCadastrado() {
        return tempoProcessoDias;
    }
        
}
