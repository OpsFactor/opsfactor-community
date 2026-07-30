package com.opsfactor.community.platform.runtime.facade.dto;

import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contrato minimo usado pelos front-ends Community/Enterprise para diferenciar
 * a edicao em execucao.
 *
 * <p>O Community nao depende de variavel de ambiente para decidir sua edicao.
 * Quando o modulo Enterprise estiver presente no classpath, ele deve registrar
 * uma implementacao `@Primary` de `RuntimeInfoService`, sobrescrevendo apenas a
 * origem destes metadados.</p>
 *
 * <p>`edition` e um identificador de maquina em lowercase: `community` ou
 * `enterprise`. Nome de produto, logo e rotulo visual pertencem ao front e nao
 * devem gerar campos redundantes neste contrato.</p>
 *
 * <p>Este DTO deve ser montado a partir de catalogos estaticos de runtime. Ele
 * nao deve consultar dados cadastrados no tenant, banco de dados, key figures
 * customizadas ou parametros de cliente. A SPA deve carrega-lo uma vez no
 * bootstrap/login e reaproveitar o resultado em um store global para renderizar
 * menus, badges e opcoes bloqueadas.</p>
 *
 * <p>Menus e rotas 100% Enterprise nao precisam virar listas exaustivas neste
 * DTO. O front Community/Enterprise deve possuir um catalogo estatico de menu
 * com `requiredEdition`; o RuntimeInfo fornece a `edition` atual para decidir
 * se o item fica habilitado, bloqueado com badge Enterprise ou oculto. As
 * listas `...Options` deste DTO ficam reservadas para seletores/cards
 * compartilhados onde Community e Enterprise aparecem na mesma tela e o backend
 * precisa informar exatamente quais valores sao selecionaveis neste runtime.</p>
 *
 * <p>`availableDemandPlanningForecastModels` publica os valores JSON que o
 * front pode habilitar na selecao de modelo estatistico. A lista nao substitui
 * as validacoes de backend; ela e apenas uma pista de runtime para renderizar a
 * UI correta antes de o usuario enviar payloads.</p>
 *
 * <p>Os campos `...Options` sao o catalogo visual completo usado pela SPA para
 * mostrar opcoes Community e Enterprise no mesmo seletor. Cada item indica a
 * edicao minima requerida e se a opcao esta selecionavel no runtime atual. No
 * Community, por exemplo, `Chronos`, `Hierarchical Reconciliation`,
 * `Sell-in`, `Sales Orders` e `Optimizer` aparecem com
 * `requiredEdition = enterprise`, `availableInCurrentRuntime = false`,
 * `disabled = true` e `disabledReason` explicito, o que permite renderizar
 * cinza, badge Enterprise e bloqueio de clique sem chamar endpoints funcionais
 * que vao falhar no backend.</p>
 *
 * <p>`availableDemandPlanningSplitModels` segue a mesma logica para os modelos
 * de split/desagregacao do forecast.</p>
 *
 * <p>`availableDemandPlanningSmoothingModels` publica os modelos de limpeza
 * historica/outlier smoothing disponiveis na configuracao de forecast. No
 * Community a lista contem apenas `Inactive`; o Enterprise deve acrescentar
 * somente modelos que ja possuam processor real migrado.</p>
 *
 * <p>`availableDemandPlanningHistoricalDocumentTypes` publica os tipos de
 * documento historico aceitos no runtime. No Community a lista contem apenas
 * Sell-out; overlays Enterprise podem acrescentar somente fontes que ja tenham
 * factories/projections proprias migradas. Sales Orders pode aparecer no
 * Enterprise quando houver projection/factory e API de carga propria, mas segue
 * bloqueado visualmente no Community pelo catalogo `...Options`.</p>
 *
 * <p>`availableSupplyPlanningExecutionModels` publica os modelos de execucao de
 * Supply Planning selecionaveis no runtime. No Community a lista contem apenas
 * `Heuristic`; o Enterprise acrescenta opcoes privadas, como `Optimizer` e
 * `Process Chain`. A lista e informativa para a UI: se um motor selecionado
 * ainda depender de SPI privada nao migrada, o backend continua falhando de
 * forma explicita no service de execucao.</p>
 *
 * <p>`visibleDemandPlanningBookKeyFigures`,
 * `selectableDemandPlanningBookKeyFigures` e
 * `editableDemandPlanningBookKeyFigures` separam, respectivamente, o recorte
 * padrao da grade, as linhas que uma Configured View pode selecionar e as
 * linhas que aceitam ajuste manual. O front usa essas listas para renderizar a
 * grade material/location Community e marcar/bloquear key figures Enterprise
 * sem depender de uma flag externa. Uma key figure selecionavel nao precisa
 * entrar no catalogo default da grade.</p>
 *
 * <p>`visibleSupplyPlanningBookKeyFigures`,
 * `selectableSupplyPlanningBookKeyFigures` e
 * `editableSupplyPlanningBookKeyFigures` separam o recorte padrao, a
 * selecao explicita de Configured View e a edicao manual do Planning Book de
 * Supply. Como Supply tipa as key figures publicas por plano, estes valores
 * podem ser ids reais da grade, como `Stock-Working Plan`. Key figures
 * privadas Enterprise usam deliberadamente a identidade raw sem sufixo de
 * plano, pois essa e a representacao aceita pela fronteira de Configured
 * View.</p>
 */
public record RuntimeInfoDTO(
        String edition,
        List<String> availableDemandPlanningForecastModels,
        List<RuntimeInfoOptionDTO> demandPlanningForecastModelOptions,
        List<String> availableDemandPlanningSplitModels,
        List<RuntimeInfoOptionDTO> demandPlanningSplitModelOptions,
        List<String> availableDemandPlanningStockoutTreatmentModels,
        List<RuntimeInfoOptionDTO> demandPlanningStockoutTreatmentModelOptions,
        List<String> availableDemandPlanningSmoothingModels,
        List<RuntimeInfoOptionDTO> demandPlanningSmoothingModelOptions,
        List<String> availableDemandPlanningUpliftModels,
        List<RuntimeInfoOptionDTO> demandPlanningUpliftModelOptions,
        List<String> availableDemandPlanningHistoricalDocumentTypes,
        List<RuntimeInfoOptionDTO> demandPlanningHistoricalDocumentTypeOptions,
        List<String> availableSupplyPlanningExecutionModels,
        List<RuntimeInfoOptionDTO> supplyPlanningExecutionModelOptions,
        List<String> visibleDemandPlanningBookKeyFigures,
        List<String> selectableDemandPlanningBookKeyFigures,
        List<String> editableDemandPlanningBookKeyFigures,
        List<String> visibleSupplyPlanningBookKeyFigures,
        List<String> selectableSupplyPlanningBookKeyFigures,
        List<String> editableSupplyPlanningBookKeyFigures) {

    public RuntimeInfoDTO {

        edition = validateEdition(edition);

        /*
         * RuntimeInfo e carregado uma vez pela SPA e fica em store global. Se
         * algum catalogo estatico for conectado errado, preferimos falhar na
         * montagem do DTO com o nome exato do campo em vez de vazar
         * NullPointerException durante serializacao ou bootstrap do front.
         */
        availableDemandPlanningForecastModels = copyRuntimeInfoList(
                "availableDemandPlanningForecastModels",
                availableDemandPlanningForecastModels);
        demandPlanningForecastModelOptions = copyRuntimeInfoOptionList(
                "demandPlanningForecastModelOptions",
                demandPlanningForecastModelOptions);
        availableDemandPlanningSplitModels = copyRuntimeInfoList(
                "availableDemandPlanningSplitModels",
                availableDemandPlanningSplitModels);
        demandPlanningSplitModelOptions = copyRuntimeInfoOptionList(
                "demandPlanningSplitModelOptions",
                demandPlanningSplitModelOptions);
        availableDemandPlanningStockoutTreatmentModels = copyRuntimeInfoList(
                "availableDemandPlanningStockoutTreatmentModels",
                availableDemandPlanningStockoutTreatmentModels);
        demandPlanningStockoutTreatmentModelOptions = copyRuntimeInfoOptionList(
                "demandPlanningStockoutTreatmentModelOptions",
                demandPlanningStockoutTreatmentModelOptions);
        availableDemandPlanningSmoothingModels = copyRuntimeInfoList(
                "availableDemandPlanningSmoothingModels",
                availableDemandPlanningSmoothingModels);
        demandPlanningSmoothingModelOptions = copyRuntimeInfoOptionList(
                "demandPlanningSmoothingModelOptions",
                demandPlanningSmoothingModelOptions);
        availableDemandPlanningUpliftModels = copyRuntimeInfoList(
                "availableDemandPlanningUpliftModels",
                availableDemandPlanningUpliftModels);
        demandPlanningUpliftModelOptions = copyRuntimeInfoOptionList(
                "demandPlanningUpliftModelOptions",
                demandPlanningUpliftModelOptions);
        availableDemandPlanningHistoricalDocumentTypes = copyRuntimeInfoList(
                "availableDemandPlanningHistoricalDocumentTypes",
                availableDemandPlanningHistoricalDocumentTypes);
        demandPlanningHistoricalDocumentTypeOptions = copyRuntimeInfoOptionList(
                "demandPlanningHistoricalDocumentTypeOptions",
                demandPlanningHistoricalDocumentTypeOptions);
        availableSupplyPlanningExecutionModels = copyRuntimeInfoList(
                "availableSupplyPlanningExecutionModels",
                availableSupplyPlanningExecutionModels);
        supplyPlanningExecutionModelOptions = copyRuntimeInfoOptionList(
                "supplyPlanningExecutionModelOptions",
                supplyPlanningExecutionModelOptions);
        visibleDemandPlanningBookKeyFigures = copyRuntimeInfoList(
                "visibleDemandPlanningBookKeyFigures",
                visibleDemandPlanningBookKeyFigures);
        selectableDemandPlanningBookKeyFigures = copyRuntimeInfoList(
                "selectableDemandPlanningBookKeyFigures",
                selectableDemandPlanningBookKeyFigures);
        editableDemandPlanningBookKeyFigures = copyRuntimeInfoList(
                "editableDemandPlanningBookKeyFigures",
                editableDemandPlanningBookKeyFigures);
        visibleSupplyPlanningBookKeyFigures = copyRuntimeInfoList(
                "visibleSupplyPlanningBookKeyFigures",
                visibleSupplyPlanningBookKeyFigures);
        selectableSupplyPlanningBookKeyFigures = copyRuntimeInfoList(
                "selectableSupplyPlanningBookKeyFigures",
                selectableSupplyPlanningBookKeyFigures);
        editableSupplyPlanningBookKeyFigures = copyRuntimeInfoList(
                "editableSupplyPlanningBookKeyFigures",
                editableSupplyPlanningBookKeyFigures);

        /*
         * A UI usa `available...` para habilitar seleção e `...Options` para
         * renderizar badge/edição. Todo valor selecionável precisa existir no
         * catálogo visual e todo item visual marcado como disponível precisa
         * aparecer na lista selecionável correspondente.
         */
        validateAvailableValuesAndOptionsAligned(
                "availableDemandPlanningForecastModels",
                availableDemandPlanningForecastModels,
                "demandPlanningForecastModelOptions",
                demandPlanningForecastModelOptions);
        validateAvailableValuesAndOptionsAligned(
                "availableDemandPlanningSplitModels",
                availableDemandPlanningSplitModels,
                "demandPlanningSplitModelOptions",
                demandPlanningSplitModelOptions);
        validateAvailableValuesAndOptionsAligned(
                "availableDemandPlanningStockoutTreatmentModels",
                availableDemandPlanningStockoutTreatmentModels,
                "demandPlanningStockoutTreatmentModelOptions",
                demandPlanningStockoutTreatmentModelOptions);
        validateAvailableValuesAndOptionsAligned(
                "availableDemandPlanningSmoothingModels",
                availableDemandPlanningSmoothingModels,
                "demandPlanningSmoothingModelOptions",
                demandPlanningSmoothingModelOptions);
        validateAvailableValuesAndOptionsAligned(
                "availableDemandPlanningUpliftModels",
                availableDemandPlanningUpliftModels,
                "demandPlanningUpliftModelOptions",
                demandPlanningUpliftModelOptions);
        validateAvailableValuesAndOptionsAligned(
                "availableDemandPlanningHistoricalDocumentTypes",
                availableDemandPlanningHistoricalDocumentTypes,
                "demandPlanningHistoricalDocumentTypeOptions",
                demandPlanningHistoricalDocumentTypeOptions);
        validateAvailableValuesAndOptionsAligned(
                "availableSupplyPlanningExecutionModels",
                availableSupplyPlanningExecutionModels,
                "supplyPlanningExecutionModelOptions",
                supplyPlanningExecutionModelOptions);

    }

    /**
     * Constroi uma lista visual de opcoes para um seletor da SPA.
     *
     * <p>`productCatalogValues` e o universo que a UI deve mostrar no seletor;
     * `communityValues` identifica quais desses valores pertencem ao contrato
     * Community; `currentRuntimeAvailableValues` identifica o que pode ser
     * selecionado no runtime atual. A diferenca entre "requer Enterprise" e
     * "esta disponivel agora" e importante: uma opcao pode ser Enterprise e
     * ainda nao estar migrada neste checkpoint, caso em que ela continua
     * bloqueada tambem na edicao Enterprise.</p>
     */
    public static List<RuntimeInfoOptionDTO> buildRuntimeInfoOptionList(
            List<String> productCatalogValues,
            List<String> communityValues,
            List<String> currentRuntimeAvailableValues) {

        List<String> productCatalogValueList = copyRuntimeInfoList(
                "productCatalogValues",
                productCatalogValues);
        List<String> communityValueList = copyRuntimeInfoList(
                "communityValues",
                communityValues);
        List<String> currentRuntimeAvailableValueList = copyRuntimeInfoList(
                "currentRuntimeAvailableValues",
                currentRuntimeAvailableValues);

        /*
         * O catálogo visual é o universo que a SPA consegue renderizar. Se uma
         * opção Community ou selecionável ficar fora dele, o front receberia um
         * valor funcional sem metadados para badge/bloqueio.
         */
        validateCatalogContainsValues(
                "productCatalogValues",
                productCatalogValueList,
                "communityValues",
                communityValueList);
        validateCatalogContainsValues(
                "productCatalogValues",
                productCatalogValueList,
                "currentRuntimeAvailableValues",
                currentRuntimeAvailableValueList);

        Set<String> communityValueSet = new HashSet<>(communityValueList);
        Set<String> currentRuntimeAvailableValueSet = new HashSet<>(currentRuntimeAvailableValueList);

        return productCatalogValueList
                .stream()
                .map(productCatalogValue -> new RuntimeInfoOptionDTO(
                        productCatalogValue,
                        communityValueSet.contains(productCatalogValue) ? "community" : "enterprise",
                        currentRuntimeAvailableValueSet.contains(productCatalogValue)))
                .toList();

    }

    private static void validateCatalogContainsValues(
            String catalogFieldName,
            List<String> catalogValues,
            String constrainedFieldName,
            List<String> constrainedValues) {

        Set<String> catalogValueSet = new HashSet<>(catalogValues);
        List<String> missingValues = constrainedValues
                .stream()
                .filter(constrainedValue -> !catalogValueSet.contains(constrainedValue))
                .toList();

        if (!missingValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "RuntimeInfoDTO." + constrainedFieldName
                            + " contains values absent from RuntimeInfoDTO."
                            + catalogFieldName + ": " + missingValues);
        }

    }

    private static void validateAvailableValuesAndOptionsAligned(
            String availableValuesFieldName,
            List<String> availableValues,
            String optionValuesFieldName,
            List<RuntimeInfoOptionDTO> optionValues) {

        Set<String> optionValueSet = new HashSet<>(
                optionValues
                        .stream()
                        .map(RuntimeInfoOptionDTO::value)
                        .toList());
        List<String> missingOptionValues = availableValues
                .stream()
                .filter(availableValue -> !optionValueSet.contains(availableValue))
                .toList();

        if (!missingOptionValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "RuntimeInfoDTO." + availableValuesFieldName
                            + " contains values absent from RuntimeInfoDTO."
                            + optionValuesFieldName + ": " + missingOptionValues);
        }

        Set<String> availableValueSet = new HashSet<>(availableValues);
        List<String> optionValuesMarkedAsAvailableButAbsentFromAvailableList =
                optionValues
                        .stream()
                        .filter(RuntimeInfoOptionDTO::availableInCurrentRuntime)
                        .map(RuntimeInfoOptionDTO::value)
                        .filter(optionValue -> !availableValueSet.contains(optionValue))
                        .toList();

        if (!optionValuesMarkedAsAvailableButAbsentFromAvailableList.isEmpty()) {
            throw new IllegalArgumentException(
                    "RuntimeInfoDTO." + optionValuesFieldName
                            + " marks values as available but they are absent from RuntimeInfoDTO."
                            + availableValuesFieldName + ": "
                            + optionValuesMarkedAsAvailableButAbsentFromAvailableList);
        }

    }

    private static String validateEdition(String edition) {

        if (!"community".equals(edition) && !"enterprise".equals(edition)) {
            throw new IllegalArgumentException("RuntimeInfoDTO.edition must be community or enterprise.");
        }

        return edition;

    }

    private static String validateRequiredString(
            String fieldName,
            String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value;

    }

    private static List<String> copyRuntimeInfoList(
            String fieldName,
            List<String> runtimeInfoValues) {

        if (runtimeInfoValues == null) {
            throw new IllegalArgumentException("RuntimeInfoDTO." + fieldName + " is required.");
        }

        return List.copyOf(runtimeInfoValues);

    }

    private static List<RuntimeInfoOptionDTO> copyRuntimeInfoOptionList(
            String fieldName,
            List<RuntimeInfoOptionDTO> runtimeInfoOptionValues) {

        if (runtimeInfoOptionValues == null) {
            throw new IllegalArgumentException("RuntimeInfoDTO." + fieldName + " is required.");
        }

        return List.copyOf(runtimeInfoOptionValues);

    }

    /**
     * Descreve uma opcao de seletor/card para renderizacao da SPA.
     *
     * <p>`value` e sempre o label JSON ja aceito pelos DTOs. `requiredEdition`
     * e `community` ou `enterprise`; o front usa esse campo para badge/icone.
     * `availableInCurrentRuntime` diz se o clique/selecao deve ser habilitado
     * neste runtime especifico. `disabled` e seu inverso materializado para o
     * front nao precisar repetir regra em toda tela; `disabledReason` e o texto
     * curto para tooltip/badge. O backend segue validando tudo novamente nos
     * services e controllers.</p>
     */
    public record RuntimeInfoOptionDTO(
            String value,
            String requiredEdition,
            boolean availableInCurrentRuntime,
            boolean disabled,
            @Nullable String disabledReason) {

        public RuntimeInfoOptionDTO(
                String value,
                String requiredEdition,
                boolean availableInCurrentRuntime) {

            this(
                    value,
                    requiredEdition,
                    availableInCurrentRuntime,
                    !availableInCurrentRuntime,
                    getDefaultDisabledReason(requiredEdition, availableInCurrentRuntime));

        }

        public RuntimeInfoOptionDTO {

            value = validateRequiredString("RuntimeInfoOptionDTO.value", value);
            requiredEdition = validateEdition(requiredEdition);
            if (disabled == availableInCurrentRuntime) {
                throw new IllegalArgumentException(
                        "RuntimeInfoOptionDTO.disabled must be the inverse of availableInCurrentRuntime.");
            }
            disabledReason = normalizeDisabledReason(disabled, disabledReason);

        }

    }

    @Nullable
    private static String getDefaultDisabledReason(
            String requiredEdition,
            boolean availableInCurrentRuntime) {

        if (availableInCurrentRuntime) {
            return null;
        }

        return "enterprise".equals(validateEdition(requiredEdition))
                ? "Requires OpsFactor Enterprise."
                : "Unavailable in this runtime.";

    }

    @Nullable
    private static String normalizeDisabledReason(
            boolean disabled,
            @Nullable String disabledReason) {

        if (!disabled) {
            return null;
        }

        return validateRequiredString(
                "RuntimeInfoOptionDTO.disabledReason",
                disabledReason);

    }

}
