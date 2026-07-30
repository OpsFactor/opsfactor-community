package com.opsfactor.community.capability.masterdata.network.supplynetwork.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.OptionalDouble;

/**
 * Parametros especificos de um material dentro de uma linha de transporte.
 *
 * <p>O Community usa esta linha para lead time, prioridade e restricoes fisicas
 * simples de lote/multiplo no Supply Planning heuristico. Tributacao, tipo de
 * operacao fiscal, custos de transporte e demais dimensoes economicas ficam no
 * Enterprise.</p>
 */
@Data
@EqualsAndHashCode(of="linhaTransporteProdutoCompositeKey")
@Entity 
@NoArgsConstructor
@RequiredArgsConstructor
public class LinhaTransporteProduto implements Serializable {
    
    @EmbeddedId    
    @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
    private LinhaTransporteProdutoCompositeKey linhaTransporteProdutoCompositeKey;
                
    @Data
    @Embeddable
    @NoArgsConstructor
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class LinhaTransporteProdutoCompositeKey implements Serializable {

        @ManyToOne(fetch = FetchType.LAZY) // geralmente esta classe é acessada a partir de LinhaTransporte. esta anotação permite uma busca se linha de transporte join fetch mapa de linha transporte produto sem consultas adicionais de linhas de transporte e produtos
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private LinhaTransporte linhaTransporte;
        
        @ManyToOne(fetch = FetchType.LAZY) // geralmente esta classe é acessada a partir de LinhaTransporte. esta anotação permite uma busca se linha de transporte join fetch mapa de linha transporte produto sem consultas adicionais de linhas de transporte e produtos
        @NonNull // null check pelo lombok : também usado para definir campos obrigatórios no construtor lombok
        private Produto produto;

    }
    
    /*
     * Override material/linha em dias. Null herda a linha base; valor presente
     * deve ser nao negativo para nao converter um cadastro errado em lead time
     * zero dentro da projection.
     */
    private Integer leadTimeDias;
    /**
     * Prioridade de uso da linha de transporte, onde 0 representa a prioridade máxima
     * Sobrescreve a prioridade da LinhaTransporte
     */
    private Integer prioridade;

    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaLoteMinimoMultiploTransporte;
    /*
     * Overrides fisicos por material seguem a mesma regra da linha base: null
     * herda o valor da linha de transporte; valor cadastrado precisa ser
     * fisicamente valido antes de chegar a projections e calculos.
     */
    private Double loteMinimoTransporte;
    private Double multiploTransporte;

    private Boolean ativo;
    
    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }

    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    public Integer getLeadTimeDias() {
        if (leadTimeDias == null) {
            return getLinhaTransporte().getLeadTimeDiasInteiro();
        }
        if (leadTimeDias < 0) {
            throw new IllegalStateException(
                    "Transportation line material lead time days must be non-negative for "
                            + getLocationOrigem().getId()
                            + " -> "
                            + getLocationDestino().getId()
                            + " / material "
                            + getProduto().getId()
                            + ": "
                            + leadTimeDias
                            + ".");
        }
        return leadTimeDias;

    }
    
    public Integer getLeadTimeDiasCadastrado() {
        return leadTimeDias;
    }
    
    public Location getLocationOrigem() {
        return getLinhaTransporte().getLinhaTransporteCompositeKey().getLocationOrigem();
    }
    
    public Location getLocationDestino() {
        return getLinhaTransporte().getLinhaTransporteCompositeKey().getLocationDestino();
    }

    public LinhaTransporte getLinhaTransporte() {
        if (linhaTransporteProdutoCompositeKey != null) {
            return linhaTransporteProdutoCompositeKey.getLinhaTransporte();
        }
        return null;
    }
    
    public Produto getProduto() {
        if (linhaTransporteProdutoCompositeKey != null) {
            return linhaTransporteProdutoCompositeKey.getProduto();
        }
        return null;
    }
    
    public void setLinhaTransporte(LinhaTransporte linhaTransporte) {
        linhaTransporteProdutoCompositeKey.setLinhaTransporte(linhaTransporte);
    }
    
    public void setProduto(Produto produto) {
        linhaTransporteProdutoCompositeKey.setProduto(produto);
    }
    
    /**
     * Retorna conversão para # periodos com arredondamento para baixo
     * @param calendario
     * @return 
     */
    public Integer getLeadTimePeriodos(Calendario calendario) {
        return (int) Math.floor(calendario.converteDiasParaPeriodosCalendario(getLeadTimeDias()));
    }
    
    public Integer getPrioridade() {
        return (prioridade == null) ? Integer.MAX_VALUE : prioridade;
    }
    
    /**
     * Para carga de arquivos : pode trazer valor nulo
     * @return 
     */
    public Integer getPrioridadeCadastrada() {
        return prioridade;
    }
    
    public VersaoMalha getVersaoMalha() {
        return getLinhaTransporteProdutoCompositeKey().getLinhaTransporte().getVersaoMalha();
    }
    
    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploTransporte(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaLoteMinimoMultiploTransporte == null) ? 
                getLinhaTransporte().getUnidadeMedidaLoteMinimoMultiploTransporte(getProduto(), parametrosGlobais) 
                : unidadeMedidaLoteMinimoMultiploTransporte;
    }
    
    public UnidadeMedida getUnidadeMedidaLoteMinimoMultiploTransporteCadastrada() {
        return unidadeMedidaLoteMinimoMultiploTransporte;
    }
    
    public Double getLoteMinimoTransporte() {

        return (loteMinimoTransporte == null)
                ? getLinhaTransporte().getLoteMinimoTransporte()
                : loteMinimoTransporte;

    }
    public Double getLoteMinimoTransporteCadastrado() {
        return loteMinimoTransporte;
    }
    
    public OptionalDouble getMultiploTransporte() {

        return (multiploTransporte == null)
                ? getLinhaTransporte().getMultiploTransporte()
                : OptionalDouble.of(multiploTransporte);

    }
    public Double getMultiploTransporteCadastrado() {
        return multiploTransporte;
    }
    /**
     * Valida override material/linha que permite zero como ausencia efetiva de
     * lote minimo, mas nao permite negativo ou valor nao finito.
     */
    /**
     * Valida override material/linha que ativa arredondamento por multiplo. Zero
     * presente seria ambiguo e quebraria o coeficiente da restricao de multiplo.
     */
    
}

