package com.opsfactor.community.capability.supplyplanning.supplyplan.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.supplyplanning.distributionplan.domain.DistributionPlanItem;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.domain.InventoryPlanLinha;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint.RestricaoPredefinidaGrupo;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Supply Plan Community executado pelo motor heuristico.
 *
 * <p>O plano guarda linhas de distribuicao, producao, estoque e demanda direta
 * considerada. A associação de preset constraint é compartilhada para manter
 * a tabela única; suas regras, setup plan, process chains e resultados de
 * otimização pertencem ao Enterprise.</p>
 */
@Data // lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="id")
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Entity
public class SupplyPlan implements Comparable<SupplyPlan> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ToString.Include
    private String descricao;
    
    private LocalDateTime horarioGeracao;
    
    String usuarioGeradorPlano;
    
    /**
     * Guarda referência ao plano de suporte usado para se projetar o estoque inicial deste plano
     * Long para não depender de outros registros da tabela de supply plan (permitindo exclusão)
     */
    Long supplyPlanIdParaProjecaoEstoqueInicial;
        
    /** representa o período a partir do qual o plano será gerado */
    @ToString.Include
    private LocalDateTime dataInicioPlano;
    private LocalDateTime dataFimPlano; // calculada no momento da geração do plano
    
    @ToString.Include
    @Enumerated(EnumType.ORDINAL)
    private Constantes.TamanhoBucket tamanhoBucket;

    /** Demand Plan de referência para a geração deste distribution plan */
    @ManyToOne
    private DemandPlan demandPlan;
    
    @ManyToOne
    private VersaoMalha versaoMalha;
    
    /** Supply plan em nível mais agregado que serve de referência para a cobertura target de estoques */
    @ManyToOne
    private SupplyPlan supplyPlanReferencia;
    
    @ManyToOne
    private PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan;

    /**
     * Grupo compartilhado associado ao plano para preset constraints.
     *
     * <p>A associação é propositalmente unidirecional e lazy: o Community não
     * navega para as regras Enterprise, enquanto o Enterprise pode resolver o
     * grupo a partir de um plano sem tabela ou vínculo transitório paralelo.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private RestricaoPredefinidaGrupo presetConstraintGroup;

    @OneToMany(mappedBy = "key.supplyPlan", orphanRemoval = true)
    private Set<DistributionPlanItem> linhasDistributionPlan = new HashSet<>();
    
    @OneToMany(mappedBy = "productionPlanLinhaCompositeKey.supplyPlan", orphanRemoval = true)
    private Set<ProductionPlanLinha> linhasProductionPlan = new HashSet<>();
    
    @OneToMany(mappedBy = "inventoryPlanLinhaCompositeKey.supplyPlan", orphanRemoval = true)
    private Set<InventoryPlanLinha> linhasInventoryPlan = new HashSet<>();

    @OneToMany(mappedBy = "demandaDiretaConsideradaLinhaCompositeKey.supplyPlan", orphanRemoval = true)
    private Set<DemandaDiretaConsideradaLinha> linhasDemandaDiretaConsiderada = new HashSet<>();

    public enum TipoLinhaSupplyPlan {
        PRODUCTION, DISTRIBUTION, INVENTORY
    }
    
    public void addDistributionPlanItem(DistributionPlanItem distributionPlanItem) {
        linhasDistributionPlan.add(distributionPlanItem);
    }
    
    public void addProductionPlanLinha(ProductionPlanLinha productionPlanLinha) {
        linhasProductionPlan.add(productionPlanLinha);
    }
    
    public LocalDateTime getHorarioGeracao() {
        return (horarioGeracao == null) ? LocalDateTime.MAX : horarioGeracao;
    }
    
    public PerfilExecucaoSupplyPlan getPerfilExecucaoSupplyPlan() {
        return (perfilExecucaoSupplyPlan == null) ? new PerfilExecucaoSupplyPlan() : perfilExecucaoSupplyPlan;
    }

    /**
     * Retorna exatamente o perfil persistido, sem o fallback historico do getter
     * publico.
     *
     * <p>O fallback de {@link #getPerfilExecucaoSupplyPlan()} preserva
     * compatibilidade com calculos antigos que tratavam perfil ausente como
     * perfil default. Em validacoes de snapshot salvo, porem, esse fallback
     * esconderia uma quebra real de persistencia. Services que precisam provar
     * que o Supply Plan esta completo devem usar este metodo.</p>
     */
    public PerfilExecucaoSupplyPlan getPerfilExecucaoSupplyPlanCadastrado() {

        return perfilExecucaoSupplyPlan;

    }

    /**
     * Traz calendário com períodos futuros (incluindo período presente) do supply plan
     * @return
     */
    public Calendario getCalendarioDoSupplyPlan(ParametrosGlobais parametrosGlobais) {
        
        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ?
                dataInicial.plusDays(getPerfilExecucaoSupplyPlan().getHorizontePlanoDiasMaximo(parametrosGlobais) - 1) 
                : getDataFimPlano();
        
        Calendario calendario = Calendario.criaCalendarioPeriodosFuturosDeDatas(tamanhoBucketConsiderado, dataInicial, dataFinal);
        
        return calendario;
        
    }
    
    public Calendario getCalendarioDoSupplyPlanComPeriodoPassadoParaEstoqueInicial(ParametrosGlobais parametrosGlobais) {
        
        Calendario calendarioSomentePeriodosFuturos = getCalendarioDoSupplyPlan(parametrosGlobais);
        
        return Calendario.criaCalendarioDeOffsetsPeriodos(
                calendarioSomentePeriodosFuturos.getTamanhoBucket(), 
                calendarioSomentePeriodosFuturos.getDataHorarioInicialPresente(), 
                0, 
                1, 
                calendarioSomentePeriodosFuturos.getNumeroPeriodosFuturos(), 
                calendarioSomentePeriodosFuturos.getNumeroPeriodosFuturosAdicional());
                
    }

    public Calendario getCalendarioDoSupplyPlanParaLocationComPeriodoPassadoParaEstoqueInicial(ClusterEParametrosProjection clusterEParametrosProjection, Location location) {

        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ?
                dataInicial.plusDays(getPerfilExecucaoSupplyPlan().getHorizontePlanoDias(clusterEParametrosProjection, location) - 1)
                : getDataFimPlano();

        return Calendario.criaCalendarioDeDatas(
                tamanhoBucketConsiderado,
                dataInicial.minusSeconds(1), // para acomodar período do estoque inicial
                dataInicial,
                dataFinal);

    }

    /**
     * Mesmo que o método getCalendarioDoSupplyPlan(parametrosGlobais), mas traz horizonte específico da location
     * @return
     */
    public Calendario getCalendarioDoSupplyPlanParaLocation(ClusterEParametrosProjection clusterEParametrosProjection, Location location) {
        
        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ? 
                dataInicial.plusDays(getPerfilExecucaoSupplyPlan().getHorizontePlanoDias(clusterEParametrosProjection, location) - 1) 
                : getDataFimPlano();
        
        Calendario calendario = Calendario.criaCalendarioPeriodosFuturosDeDatas(tamanhoBucketConsiderado, dataInicial, dataFinal);
        
        return calendario;
        
    }

    @Override
    public int compareTo(SupplyPlan supplyPlan) {
        return getId().compareTo(supplyPlan.getId());
    }

}
