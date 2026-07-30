package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain;

import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecalho de uma politica operacional de estoque.
 *
 * <p>No Community, esta entidade permanece porque o Supply Planning
 * heuristico depende dela para resolver safety stock por material/location.
 * Ela nao representa o modulo Enterprise de Inventory Policy Optimization:
 * simulacoes, resultados otimizados e parametrizacao economica ficam fora
 * deste agregado.</p>
 */
@Getter
@Setter
@Entity
@NoArgsConstructor 
@EqualsAndHashCode(of = "id")
public class PoliticaEstoques implements Serializable {

    @Id
    private String id;
    
    /**
     * Prioridade de desempate quando mais de uma politica estiver vigente para
     * a mesma combinacao material/location/periodo.
     */
    private Integer prioridade;
    
    /**
     * Inicio da vigencia operacional da politica.
     */
    private LocalDateTime dataHorarioInicio;

    /**
     * Fim da vigencia operacional da politica.
     */
    private LocalDateTime dataHorarioFim;
    
    /**
     * Regras material/location que carregam os parametros operacionais de
     * safety stock consumidos pelo heuristico.
     */
    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "politicaEstoquesMaterialLocationCompositeKey.politicaEstoques", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PoliticaEstoquesMaterialLocation> politicaEstoquesMaterialLocationList = new ArrayList<>();
    
    public int getPrioridade() {
        return (prioridade == null) ? Integer.MAX_VALUE : prioridade;
    }
    
    public Integer getPrioridadeCadastrada() {
        return prioridade;
    }
        
}
