package com.opsfactor.community.capability.demandplanning.configuration.domain;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.*;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
// lombok: @ToString, @EqualsAndHashCode, @Getter on all fields @Setter on all non-final fields, and @RequiredArgsConstructor
@EqualsAndHashCode(of="id")
@ToString(of="id")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
public class PerfilExecucaoDemandPlan {

    @Id
    @Column(length = 50)
    @NonNull
    private String id;

    private String descricao;

    private Constantes.TamanhoBucket tamanhoBucket;

    @Getter(AccessLevel.NONE)
    @Enumerated(EnumType.ORDINAL)
    private Constantes.TipoDocumentoVenda tipoDocumentoVenda;

    private Boolean restringePeriodosEdicaoPlano;
    private @Nullable Integer periodoInicialEdicaoPlano;
    private @Nullable Integer periodoFinalEdicaoPlano;

    /*
     * Horizonte operacional do Demand Plan em periodos.
     *
     * Null preserva o default historico de um periodo. Valor explicitamente
     * cadastrado precisa ser positivo: services/front bloqueiam payloads novos,
     * mas o dominio tambem protege snapshots antigos, cargas diretas e
     * consumidores Enterprise que consultam a entidade sem passar pela borda
     * REST Community.
     */
    private Integer numeroPeriodosHorizontePlanejamento;

    @ManyToOne
    private UnidadeMedida unidadeMedidaPadraoDP;

    /*
     * IDs Enterprise dos niveis de agregacao usados para MAPE/HTS.
     *
     * A entidade de perfil e compartilhada entre Community e Enterprise. Por
     * isso os campos ficam aqui como ponte de schema, mas o Community bloqueia
     * qualquer escrita/leitura funcional desses valores no service de borda e
     * no mapper. O Enterprise valida os ids contra seus repositories privados
     * antes de persistir.
     */
    private String nivelAgregacaoMaterialMapeId;
    private String nivelAgregacaoLocationMapeId;

    @OneToMany(mappedBy = "parametrosDemandPlanNivelClusterCompositeKey.perfilExecucaoDemandPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ParametrosDemandPlanNivelCluster> parametrosForecast = new HashSet<>();

    public Constantes.TamanhoBucket getTamanhoBucket() {
        return (tamanhoBucket == null) ? Constantes.TamanhoBucket.MENSAL : tamanhoBucket;
    }

    public Constantes.TipoDocumentoVenda getTipoDocumentoVenda(ParametrosGlobais parametrosGlobais) {
        return (tipoDocumentoVenda == null) ? parametrosGlobais.getTipoDocumentoVenda() : tipoDocumentoVenda;
    }

    public boolean getRestringePeriodosEdicaoPlano() {
        return (restringePeriodosEdicaoPlano == null) ? false : restringePeriodosEdicaoPlano;
    }

    public int getNumeroPeriodosHorizontePlanejamento() {
        return (numeroPeriodosHorizontePlanejamento == null)
                ? 1
                : getInteiroOperacionalPositivoCadastrado(
                        numeroPeriodosHorizontePlanejamento,
                        "Demand Planning execution profile planning horizon in periods");
    }

    public int getNumeroDiasHorizontePlanejamento(LocalDateTime dataHorarioReferencia) {
        if (numeroPeriodosHorizontePlanejamento == null) {
            return 1;
        }

        int numeroPeriodosHorizontePlanejamentoCadastrado =
                getInteiroOperacionalPositivoCadastrado(
                        numeroPeriodosHorizontePlanejamento,
                        "Demand Planning execution profile planning horizon in periods");

        return Calendario.getOffsetPeriodosEntreDataHorarios(
                dataHorarioReferencia,
                Calendario.getPrimeiraDataHorarioPeriodoCalendarioComOffset(
                        dataHorarioReferencia,
                        numeroPeriodosHorizontePlanejamentoCadastrado,
                        getTamanhoBucket()),
                Constantes.TamanhoBucket.DIARIO);
    }

    /**
     * Valida inteiros operacionais que ja vieram preenchidos no snapshot.
     *
     * <p>Nao recebe default porque esse helper representa apenas o caminho de
     * valor cadastrado. Assim fica explicito, no metodo consumidor, quando
     * `null` e default publico e quando zero/negativo e dado invalido que nao
     * deve ser mascarado por truncamento.</p>
     */
    private static int getInteiroOperacionalPositivoCadastrado(
            Integer valorOperacionalCadastrado,
            String descricaoCampo) {

        if (valorOperacionalCadastrado <= 0) {
            throw new IllegalArgumentException(
                    descricaoCampo
                            + " must be positive when explicitly configured: "
                            + valorOperacionalCadastrado
                            + ".");
        }

        return valorOperacionalCadastrado;
    }

}
