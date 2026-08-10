package com.opsfactor.community.capability.configuration.facade.mapper;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewCharacteristicFilter;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredViewKeyFigure;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewCaracteristicaDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewKeyFigureDTO;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewDTO;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct das views configuradas consumidas pelo Planning Book.
 *
 * <p>No Community a conversão fixa nível material/location, publica somente os
 * filtros por atributos públicos e mantém ausentes apresentação e agrupamento.
 * A seleção standard de key figures é carregada em lote pelo service.
 * Workflows, agrupamentos e key figures privadas/customizadas pertencem ao
 * Enterprise.</p>
 */
// spring : mapper pode ser extraído com @Autowired. uses : outros mappers dos quais este depende
@Mapper(componentModel = "spring")
public interface ConfiguredViewAutoMapper {
    
    // ENTIDADE - DTO ------------------------------------------------------------------------------------------------------
    // ENTIDADE - DTO : CONFIGURED VIEW
    @Mapping(source = "configuredView.configuredViewCompositeKey.userId", target = "userId")
    @Mapping(source = "configuredView.configuredViewCompositeKey.nomeView", target = "viewName")
    @Mapping(source = "configuredView.tipoView", target = "viewType")
    @Mapping(expression = "java(getKeyFigureAjusteDemandaDiretaTotalCommunity())", target = "directDemandUpdateKeyFigure")
    @Mapping(expression = "java(toCharacteristicFilterDTOList(configuredView.getMaterialCharacteristicFilterSet()))", target = "materialCharacteristicDetailList")
    @Mapping(expression = "java(toCharacteristicFilterDTOList(configuredView.getLocationCharacteristicFilterSet()))", target = "locationCharacteristicDetailList")
    @Mapping(expression = "java(java.util.List.of())", target = "materialLocationCharacteristicDetailList")
    @Mapping(expression = "java(true)", target = "showMaterialLevel")
    @Mapping(expression = "java(true)", target = "showLocationLevel")
    @Mapping(source = "configuredView.numeroPeriodosHistoricosDemandPlanningBook", target = "numberHistoricalSalesPeriodsDemandPlanningBook")
    @Mapping(expression = "java(configuredView.getUnidadeMedidaView(parametrosGlobais).getId())", target = "unitOfMeasure")
    @Mapping(expression = "java(java.util.List.of())", target = "keyFigureList")
    @Mapping(source = "configuredView.submissaoAutomaticaAlteracoes", target = "autoSubmitChanges")
    @Mapping(source = "configuredView.permiteAlteracaoHorizonteCongelado", target = "allowInputFrozenHorizon")
    @Mapping(source = "configuredView.exibeVendaMediaHistorica", target = "showHistoricalAverage")
    @Mapping(source = "configuredView.exibeMateriaisDescontinuados", target = "showDiscontinuedMaterials")
    @Mapping(source = "configuredView.exibeVendaMediaHistorica", target = "showAverageHistoricalSales")
    @Mapping(source = "configuredView.exibeDfusSemFaturamentoNoHorizonteHistorico", target = "showDfusWithoutHistoricalSalesOverHistoricalPeriod")
    @Mapping(source = "configuredView.demandPlanWorkflowId", target = "demandPlanWorkflowId")
    @Mapping(source = "configuredView.demandPlanWorkflowStageId", target = "demandPlanWorkflowStageId")
    @Mapping(expression = "java(sortedIdFilters(configuredView.getMaterialIdFilterSet()))", target = "materialIdFilterList")
    @Mapping(expression = "java(sortedIdFilters(configuredView.getLocationIdFilterSet()))", target = "locationIdFilterList")
    public ConfiguredViewDTO converte(ConfiguredView configuredView, ParametrosGlobais parametrosGlobais);
    
    public default List<ConfiguredViewDTO> converteConfiguredViewDTOList(List<ConfiguredView> configuredViewList, ParametrosGlobais parametrosGlobais) {
        return configuredViewList.stream().map(x -> converte(x, parametrosGlobais)).collect(Collectors.toList());
    }

    /**
     * Acrescenta ao DTO a fotografia já carregada em batch pelo service.
     *
     * <p>O mapper não percorre relação JPA da ConfiguredView: a entidade não
     * possui coleção inversa e o método recebe somente as linhas da view
     * corrente. Isso preserva a listagem sem N+1.</p>
     */
    default ConfiguredViewDTO converteComKeyFigures(
            ConfiguredView configuredView,
            ParametrosGlobais parametrosGlobais,
            List<ConfiguredViewKeyFigure> configuredViewKeyFigures) {

        ConfiguredViewDTO configuredViewDTO = converte(configuredView, parametrosGlobais);
        configuredViewDTO.keyFigureList = configuredViewKeyFigures.stream()
                .sorted(Comparator.comparingInt(ConfiguredViewKeyFigure::getPosition)
                        .thenComparing(ConfiguredViewKeyFigure::getKeyFigureId))
                .map(configuredViewKeyFigure -> {
                    ConfiguredViewKeyFigureDTO dto =
                            new ConfiguredViewKeyFigureDTO();
                    dto.keyFigure = configuredViewKeyFigure.getKeyFigureId();
                    dto.position = configuredViewKeyFigure.getPosition();
                    dto.allowChanges = configuredViewKeyFigure.getAllowChangesCadastrado();
                    return dto;
                })
                .collect(Collectors.toList());
        return configuredViewDTO;

    }
    
    default String getKeyFigureAjusteDemandaDiretaTotalCommunity() {

        return MetodosUtilidade.getValorJsonPropertyDeEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA);

    }

    /** Produz uma fotografia determinística dos filtros simples Community. */
    default List<String> sortedIdFilters(java.util.Set<String> idFilters) {

        if (idFilters == null) {
            return List.of();
        }

        return idFilters.stream().sorted().collect(Collectors.toList());

    }

    /**
     * Agrupa a fotografia relacional em uma linha de DTO por característica.
     *
     * <p>A ordenação torna a resposta determinística sem acessar catálogo ou
     * relação JPA adicional. Campos de agrupamento e posição permanecem nulos.</p>
     */
    default List<ConfiguredViewCaracteristicaDTO> toCharacteristicFilterDTOList(
            java.util.Set<ConfiguredViewCharacteristicFilter> configuredViewCharacteristicFilters) {

        if (configuredViewCharacteristicFilters == null) {
            return List.of();
        }

        Map<String, List<String>> valuesByCharacteristicId = configuredViewCharacteristicFilters.stream()
                .sorted(Comparator.comparing(ConfiguredViewCharacteristicFilter::getCharacteristicId)
                        .thenComparing(ConfiguredViewCharacteristicFilter::getFilteredValue))
                .collect(Collectors.groupingBy(
                        ConfiguredViewCharacteristicFilter::getCharacteristicId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                ConfiguredViewCharacteristicFilter::getFilteredValue,
                                Collectors.toList())));

        return valuesByCharacteristicId.entrySet().stream()
                .map(entry -> {
                    ConfiguredViewCaracteristicaDTO configuredViewCaracteristicaDTO =
                            new ConfiguredViewCaracteristicaDTO();
                    configuredViewCaracteristicaDTO.characteristicId = entry.getKey();
                    configuredViewCaracteristicaDTO.filteredValues = entry.getValue();
                    return configuredViewCaracteristicaDTO;
                })
                .collect(Collectors.toList());

    }

}
