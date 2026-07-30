package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureAbstract;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Base serializavel de key figure enviada ao Planning Book.
 *
 * <p>A classe carrega valores por periodo, tooltips e classes visuais extras.
 * O Community usa apenas KFs padrao e edicao direta; especializacoes privadas
 * podem reaproveitar este contrato desde que validem as KFs publicadas no
 * RuntimeInfo e nas bordas de service.</p>
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class KeyFigureDTOAbstract <KFDATA extends DFUDataKeyFigureAbstract, KFDTO extends KeyFigureDTOAbstract<KFDATA, KFDTO>> {

    public String keyFigure; // ex : Stock, Planned Production, etc
    public Map<String,Double> values; // valores a serem apresentados em cada periodo (no formato YYYYMM, YYYYWW ou YYYYMMDD)
    /*
     * Sidecar opcional: ausencia de uma entrada significa celula disponivel.
     * Uma razao torna a celula N/A e impede que zero-fill ou agregacao exibam
     * um subtotal monetario parcial como valor valido.
     */
    public Map<String,String> unavailableReasons;
    public Map<LocalDateTime,String> toolTips; // tooltips para cada periodo (no formato YYYYMM, YYYYWW ou YYYYMMDD)
    public Map<LocalDateTime,Set<String>> additionalClasses; // permite a inclusão de diferentes estilos para cada célula

    /*
     * Linhas com editMode = 'cellEdit' modificam a celula diretamente.
     * editMode = 'detailEdit' abre modal AG-Grid com detalhes especificos para
     * edicao. Valor nulo significa sem edicao e sem modal.
     */
    public EditMode editMode;

    public KeyFigureDTOAbstract(String keyFigure, EditMode editMode) {
        this.keyFigure = keyFigure;
        this.editMode = editMode;
    }

    public abstract void importaDadosDFUDataKeyFigure(Calendario calendario, List<KFDATA> keyFigureData);
    public void importaDadosDFUDataKeyFigureAbstract(Calendario calendario, List<DFUDataKeyFigureAbstract> keyFigureData) {
        importaDadosDFUDataKeyFigure(calendario, (List<KFDATA>) keyFigureData);
    }

    @JsonIgnore
    public abstract KFDTO getCopiaSomenteComKeyFigureIdEEditMode();
    @JsonIgnore
    public abstract KFDTO getCopiaCompleta();


    public void incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(KFDTO keyFigureDtoAIncorporar) {

        if (!keyFigureDtoAIncorporar.keyFigure.equals(keyFigure)) {
            throw new IllegalArgumentException("Only key figures of the same type can be aggregated");
        }
        if (!keyFigureDtoAIncorporar.editMode.equals(editMode)) {
            throw new IllegalArgumentException("Only key figures with the same Edit Mode can be aggregated");
        }

        // chama addValueForPeriod para cada elemento Value de A e B
        incorporaValoresDeKeyFigure(keyFigureDtoAIncorporar);
        // concatena valores dos tooltips dos filhos
        consolidateTooltips(keyFigureDtoAIncorporar);
        // inclui o maior número de classes 'custom' para cada célula
        consolidateAdditionalClasses(keyFigureDtoAIncorporar);
        // propaga o primeiro motivo de indisponibilidade por periodo e remove
        // qualquer valor parcial que tenha sido somado antes dele.
        consolidateUnavailableReasons(keyFigureDtoAIncorporar);

    }

    public abstract void incorporaValoresDeKeyFigure(KFDTO keyFigureDtoAIncorporar);

    /**
     * Importa o estado lateral vindo da projection para as chaves de periodo
     * efetivamente usadas pelo DTO.
     */
    public void importaUnavailableReasons(
            Calendario calendario,
            Map<LocalDateTime, String> unavailableReasonsByDate) {

        if (unavailableReasonsByDate == null || unavailableReasonsByDate.isEmpty()) {
            return;
        }
        if (unavailableReasons == null) {
            unavailableReasons = new LinkedHashMap<>();
        }

        unavailableReasonsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> unavailableReasons.putIfAbsent(
                        calendario.getUltimaDataHorarioPeriodo(entry.getKey()).toString(),
                        entry.getValue()));
        removeUnavailableValues();

    }

    /**
     * Preserva a primeira causa por periodo quando folhas sao agregadas.
     */
    public void consolidateUnavailableReasons(KFDTO keyFigureDtoAIncorporar) {

        if (keyFigureDtoAIncorporar.unavailableReasons == null
                || keyFigureDtoAIncorporar.unavailableReasons.isEmpty()) {
            return;
        }
        if (unavailableReasons == null) {
            unavailableReasons = new LinkedHashMap<>();
        }
        keyFigureDtoAIncorporar.unavailableReasons.forEach(unavailableReasons::putIfAbsent);
        removeUnavailableValues();

    }

    /** Retorna se o periodo nao pode receber nem exibir valor numerico. */
    public boolean hasUnavailableReason(String period) {

        return unavailableReasons != null && unavailableReasons.containsKey(period);

    }

    /** Elimina valores parciais nos periodos marcados como indisponiveis. */
    private void removeUnavailableValues() {

        if (values == null || unavailableReasons == null || unavailableReasons.isEmpty()) {
            return;
        }
        unavailableReasons.keySet().forEach(values::remove);

    }

    public void consolidateTooltips(KFDTO keyFigureDtoAIncorporar) {

        // concatena valores dos tooltips dos filhos
        if (keyFigureDtoAIncorporar.toolTips != null && !keyFigureDtoAIncorporar.toolTips.isEmpty()) {
            if (toolTips == null) toolTips = new HashMap<>();
            ((Map<LocalDateTime,String>) keyFigureDtoAIncorporar.toolTips).entrySet().stream().forEach(entryDataETooltipString -> {

                LocalDateTime data = entryDataETooltipString.getKey();
                String toolTip = entryDataETooltipString.getValue();

                if (toolTip == null || toolTip.equals("")) return;

                FuncoesMap.updateElementoNoNestedMap(
                        "",
                        tooltipAnterior -> tooltipAnterior + "-" + toolTip,
                        String.class,
                        toolTips,
                        data);
            });
        }

    }

    /**
     * Consolida classes (geralmente diferenciação da cor de fundo da célula do planning book) de duas key figures
     * @param keyFigureDtoAIncorporar
     */
    public void consolidateAdditionalClasses(KFDTO keyFigureDtoAIncorporar) {

        // concatena valores dos tooltips dos filhos
        if (keyFigureDtoAIncorporar.additionalClasses != null && !keyFigureDtoAIncorporar.additionalClasses.isEmpty()) {
            if (additionalClasses == null) additionalClasses = new HashMap<>();
            ((Map<LocalDateTime,Set<String>>) keyFigureDtoAIncorporar.additionalClasses).entrySet().stream().forEach(x ->  {
                additionalClasses.computeIfAbsent(x.getKey(), set -> new HashSet<>()).addAll(x.getValue());
            });
        }

    }

    public void addAdditionalClass(LocalDateTime dataHorario, String additionalClass) {
        additionalClasses
                .computeIfAbsent(
                        dataHorario,
                        x -> new HashSet<>())
                .add(additionalClass);
    }

    public void updateTooltip(LocalDateTime dataHorario, UnaryOperator<String> funcaoDeAtualizacao) {
        FuncoesMap.updateElementoNoNestedMap(
                "",
                funcaoDeAtualizacao,
                String.class,
                toolTips,
                dataHorario);
    }


    
}
