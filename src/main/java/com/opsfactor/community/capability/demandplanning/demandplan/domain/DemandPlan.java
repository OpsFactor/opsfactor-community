package com.opsfactor.community.capability.demandplanning.demandplan.domain;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Demand Plan Community com linhas material/location e historico fisico.
 *
 * <p>O Community permite modelos estatisticos abertos e ajustes via Planning
 * Book em nivel material/location. Custom key figures persistidas, pricing,
 * P&amp;L e artefatos economicos ficam no Enterprise.</p>
 */
@Data
@NoArgsConstructor 
@AllArgsConstructor 
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(of="id")
@Entity
public class DemandPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;
        
    @ToString.Include
    private String descricao;

    @ManyToOne
    private PerfilExecucaoDemandPlan perfilExecucaoDemandPlan;
    
    private LocalDateTime horarioGeracao;
    
    String usuarioGeradorPlano;
    
    /** representa o período a partir do qual o plano será gerado */
    @ToString.Include
    private LocalDateTime dataInicioPlano;
    private LocalDateTime dataFimPlano; // calculada no momento da geração do plano
    
    /** datas entre as quais se pode editar o plano no planning book */
    private LocalDate dataInicioEdicao;
    private LocalDate dataFimEdicao;
    
    @ToString.Include
    @Enumerated(EnumType.ORDINAL)
    private Constantes.TamanhoBucket tamanhoBucket;
    
    @OneToMany(mappedBy = "key.demandPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DemandPlanItem> linhasDemandPlan = new HashSet();

    @OneToMany(mappedBy = "key.demandPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HistoricoDemandPlanItem> linhasHistoricoDemandPlan = new HashSet();
    
    @OneToMany(mappedBy = "demandPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SupplyPlan> supplyPlans = new HashSet();
    
    @ManyToOne
    private DemandPlan demandPlanCopiadoNoHorizonteCongelado;
    
    public void addDemandPlanItem(DemandPlanItem demandPlanItem) {
        if(demandPlanItem != null){
            demandPlanItem.getKey().setDemandPlan(this);
            linhasDemandPlan.add(demandPlanItem);
        }
    }
    
    public Constantes.TamanhoBucket getTamanhoBucket() {
        return (tamanhoBucket == null) ? Constantes.TamanhoBucket.MENSAL : tamanhoBucket;
    }
    
    public LocalDateTime getHorarioGeracao() {
        return (horarioGeracao == null) ? LocalDateTime.MAX : horarioGeracao;
    }

    /**
     * Retorna exatamente o horario persistido, sem o fallback historico do
     * getter publico.
     *
     * <p>O fallback de {@link #getHorarioGeracao()} preserva ordenacoes antigas
     * que tratavam horario ausente como fim da lista. Em validacoes de snapshot
     * salvo, porem, esse fallback esconderia uma quebra real do cabecalho gerado
     * pelo service.</p>
     */
    public LocalDateTime getHorarioGeracaoCadastrado() {

        return horarioGeracao;

    }

    /**
     * Retorna exatamente o tamanho de bucket persistido, sem o fallback historico
     * do getter publico.
     *
     * <p>O fallback de {@link #getTamanhoBucket()} existe para compatibilidade
     * com rotinas antigas que assumiam mensal quando o campo vinha nulo. A
     * geracao Community precisa provar que o plano salvo tem o bucket material
     * escolhido pelo perfil antes de criar linhas em paralelo.</p>
     */
    public Constantes.TamanhoBucket getTamanhoBucketCadastrado() {

        return tamanhoBucket;

    }
    
    public LocalDate getDataInicioEdicao() {
        return (dataInicioEdicao == null) ? LocalDate.now() : dataInicioEdicao;
    }
    public LocalDate getDataFimEdicao() {
        return (dataFimEdicao == null) ? LocalDate.now() : dataFimEdicao;
    }
    
    public Calendario getCalendarioDoDemandPlanSemHistorico(ClusterEParametrosProjection clusterEParametrosProjection) {
        
        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ? 
                dataInicial.plusDays(clusterEParametrosProjection.getDPHorizonteForecastDias() - 1) 
                : getDataFimPlano();
        
        Calendario calendario = Calendario.criaCalendarioDeDatas(
                tamanhoBucketConsiderado, 
                dataInicial, 
                dataInicial, 
                dataFinal);
        
        return calendario;
        
    }
    
    /**
     * Calendário que considera atributos dataInicioPlano e dataFimPlano do objeto DemandPlan
     * Como o número de períodos passados depende da combinação de ClusterProdutosDemandPlanning e ClusterLocations
     * este método traz o valor máximo de períodos possível
     * @return
     */
    public Calendario getCalendarioDoDemandPlanComHistoricoMaximo(
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) {
        
        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ? 
                dataInicial.plusDays(parametrosDemandPlanProjection
                        .getPerfilExecucaoDemandPlan()
                        .getNumeroDiasHorizontePlanejamento(dataInicial)
                        - 1)
                : getDataFimPlano();
        
        Calendario calendario = Calendario.criaCalendarioDeDatas(
                tamanhoBucketConsiderado,
                dataInicial.minusDays(
                        parametrosDemandPlanProjection.getNumeroMaximoDiasHistoricoVendasParaForecast()),
                dataInicial,
                dataFinal);
        
        return calendario;
        
    }

    public Calendario getCalendarioDoDemandPlanComNumeroPeriodosHistoricosFixo(PerfilExecucaoDemandPlan perfilExecucaoDemandPlan, int numeroPeriodosPassados) {

        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ?
                dataInicial.plusDays(perfilExecucaoDemandPlan
                        .getNumeroDiasHorizontePlanejamento(dataInicial)
                        - 1)
                : getDataFimPlano();

        Calendario calendario = Calendario.criaCalendarioDeOffsetsPeriodos(
                tamanhoBucketConsiderado,
                dataInicial,
                0,
                numeroPeriodosPassados,
                Calendario.getOffsetPeriodosEntreDataHorarios(
                        dataInicial,
                        dataFinal,
                        tamanhoBucketConsiderado) + 1,
                0);

        return calendario;

    }

    /**
     * Calendário que considera atributos dataInicioPlano e dataFimPlano do objeto DemandPlan
     * Como o número de períodos passados depende da combinação de ClusterProdutosDemandPlanning e ClusterLocations
     * este método traz o valor específico do histórico para os clusters passados como argumento
     * @return
     */
    public Calendario getCalendarioDoDemandPlan(
            ParametrosDemandPlanProjection parametrosDemandPlanProjection,
            ClusterProdutosDemandPlanning clusterProdutosDemandPlanning,
            ClusterLocations clusterLocations) {
        
        Constantes.TamanhoBucket tamanhoBucketConsiderado = (getTamanhoBucket() == null) ? Constantes.TamanhoBucket.MENSAL : getTamanhoBucket();
        LocalDateTime dataInicial = (getDataInicioPlano() == null) ? Calendario.getPrimeiraDataHorarioPeriodo(LocalDateTime.now(), tamanhoBucketConsiderado) : getDataInicioPlano();
        // data final não-efetiva : deveria ser a última data do último período
        // no entanto, como se exporta o calendário e não a data este deixa de ser um problema
        LocalDateTime dataFinal = (getDataFimPlano() == null) ? 
                dataInicial.plusDays(
                        parametrosDemandPlanProjection
                                .getPerfilExecucaoDemandPlan()
                                .getNumeroDiasHorizontePlanejamento(dataInicial)
                                - 1)
                : getDataFimPlano();
        
        Calendario calendario = Calendario.criaCalendarioDeDatas(
                tamanhoBucketConsiderado, 
                dataInicial.minusDays(
                        parametrosDemandPlanProjection
                                .getParametrosDemandPlanNivelClusterProjection(clusterLocations, clusterProdutosDemandPlanning)
                                .getParametrosGeraisDemandPlanningProjection()
                                .diasHistoricosForecastEstatistico),
                dataInicial, dataFinal);
        
        return calendario;
        
    }
        
}
