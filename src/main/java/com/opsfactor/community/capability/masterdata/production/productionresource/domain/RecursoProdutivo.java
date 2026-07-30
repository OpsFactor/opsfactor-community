package com.opsfactor.community.capability.masterdata.production.productionresource.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recurso produtivo operacional usado pelo Supply Planning Community.
 *
 * <p>O Community consome disponibilidade em horas e eficiencia para o
 * heuristico e para o plano restrito simples. Custos de recurso, alocacao por
 * turno, line scheduling e demais dimensoes economicas/temporais avancadas
 * pertencem ao Enterprise.</p>
 */
@Entity
@Data
@Builder
@ToString(of="id")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class RecursoProdutivo implements Serializable, Comparable<RecursoProdutivo> {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne
    private Location location;

    private String descricao;

    private Boolean ativo;
    
    /*
     * Eficiencia produtiva usada como divisor no consumo de horas de operacao.
     * Null preserva o default historico 1.0; valor cadastrado precisa ser
     * finito e positivo para nao transformar erro de cadastro em consumo de
     * capacidade artificialmente enorme.
     */
    private Float eficiencia;
    
    // caso a capacidade se dê em quantidade / dia
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private UnidadeMedida unidadeMedidaCapacidadeEmUom;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "disponibilidadeRecursoProdutivoCompositeKey.recursoProdutivo", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DisponibilidadeRecursoProdutivo> disponibilidadesRecursoProdutivo = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "recursoProdutivo", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<OperacaoRoteiro> operacaoRoteiroSet = new HashSet<>();

    /**
     * Retorna lista de materiais que fazem parte de alguma Receita Producao Operacao
     * associada a este recurso (operação final ou intermediária de uma receita)
     * @return 
     */
    public Set<Produto> getMateriaisOutputRoteirosAtivos() {
        return getOperacaoRoteiroSet().stream()
                .filter(x -> x.getRoteiro().getAtivo())
                .map(OperacaoRoteiro::getMaterialOutput)
                .distinct()
                .collect(Collectors.toSet());
    }
    
    public Set<Produto> getMateriaisOutputTodosRoteiros() {
        return getOperacaoRoteiroSet().stream()
                .map(OperacaoRoteiro::getMaterialOutput)
                .distinct()
                .collect(Collectors.toSet());
    }
        
    public Float getDisponibilidadeHorasPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        return (float) getDisponibilidadesRecursoProdutivo().stream()
                .filter(x -> x.getDataReferencia().isAfter(dataInicial.minusDays(1)) &&
                        x.getDataReferencia().isBefore(dataFinal.plusDays(1)))
                .mapToDouble(DisponibilidadeRecursoProdutivo::getHorasDisponiveis)
                .sum();
    }
    
    public Float getDisponibilidadeHorasPeriodo(Calendario calendario, int posicaoPeriodo) {
        
        // método do calendário já considera difrenças entre tamanhoBucket do calendário e da tabela de capacidades logísticas
        return (float) calendario.consolidaDadosNoCalendario(posicaoPeriodo, Constantes.TamanhoBucket.DIARIO, localDateTime -> 
                getDisponibilidadesRecursoProdutivo().stream()
                        .filter(x -> x.getDataReferencia().equals(localDateTime.toLocalDate()))
                        .findAny()
                        .map(x -> x.getHorasDisponiveis())
                        .orElse(0f));
        
    }
            
    public boolean getAtivo() {
        return (ativo == null) ? true : ativo;
    }
    
    /**
     * Método usado para data upload
     * @return 
     */
    public Boolean getAtivoCadastrado() {
        return ativo;
    }
    
    public float getEficiencia() {

        if (eficiencia == null) {
            return 1.0f;
        }
        if (!Float.isFinite(eficiencia) || eficiencia <= 0.0f) {
            throw new IllegalStateException(
                    "Production resource efficiency must be finite and positive for resource "
                            + getId()
                            + ": "
                            + eficiencia
                            + ".");
        }
        return eficiencia;

    }
    
    public Float getEficienciaCadastrado() {
        return eficiencia;
    }
    
    public UnidadeMedida getUnidadeMedidaCapacidadeEmUom(ParametrosGlobais parametrosGlobais) {
        return (unidadeMedidaCapacidadeEmUom == null) ? parametrosGlobais.getUnidadeMedidaPadraoSNP() : unidadeMedidaCapacidadeEmUom;
    }
    public UnidadeMedida getUnidadeMedidaCapacidadeEmUomCadastrado() {
        return unidadeMedidaCapacidadeEmUom;
    }
    
    @Override
    public int compareTo(RecursoProdutivo recursoProdutivo) {
        return getId().compareTo(recursoProdutivo.getId());
    }
    
}
