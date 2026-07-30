package com.opsfactor.community.capability.planningbook.facade;

import com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure.KeyFigureDTOPadrao;
import com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure.KeyFigureDTORelacaoEntreValores;
import com.opsfactor.community.capability.planningbook.facade.dto.specializedkeyfigure.KeyFigureDTOCoberturaEstoque;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.planningbook.domain.AjusteCelulaPlanningBook;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.planningbook.facade.dto.ColumnDefDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.GroupDTO;
import com.opsfactor.community.capability.planningbook.facade.dto.KeyFigureDTOAbstract;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandard;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Montador comum dos DTOs de Planning Book Community.
 *
 * <p>Demand e Supply entregam a esta classe projections de key figures ja
 * filtradas para o recorte material/location. O service monta colunas, grupos,
 * tooltips e parametros adicionais sem acessar banco ou decidir capabilities
 * Enterprise.</p>
 */
@Service
public class PlanningBookService {
    
    public PlanningBookDTO getPlanningBookDTO(KeyFigureProjection keyFigureProjection) {

        Calendario calendario = keyFigureProjection.getCalendario();
        ConfiguredViewProjection configuredViewProjection =
                keyFigureProjection.getConfiguredViewProjection();
        ConfiguredView configuredView = configuredViewProjection.getConfiguredView();
        ClusterEParametrosProjection clusterEParametrosProjection =
                configuredViewProjection.getClusterEParametrosProjection();
        validaKeyFiguresApresentadas(keyFigureProjection);

        Map<String,String> additionalParameters = new HashMap<>();

        // Community nao expoe group-by configuravel. Os dois parametros abaixo
        // controlam somente o caso especifico de rateio sem referencia; nao
        // mudam a granularidade material/location da resposta.
        additionalParameters.put("allowAggregatedAdjustmentsForZeroReferenceMaterial", "false");
        additionalParameters.put("allowAggregatedAdjustmentsForZeroReferenceLocation", "false");
        
        List<String> periodList = calendario.getListDataHorariosFinaisPorPeriodo().stream().map(LocalDateTime::toString).collect(Collectors.toList());
        if (configuredView.getExibeVendaMediaHistorica() && calendario.getNumeroPeriodosPassados() > 0) {
            periodList.add("Average Historical Sales");
        }

        PlanningBookDTO planningBookDTO = PlanningBookDTO.builder()
                .viewName(configuredView.getNomeView())
                .viewType(configuredView.getTipoView())
                .autoSubmitChanges(configuredView.getSubmissaoAutomaticaAlteracoes())
                .bucketSize(calendario.getTamanhoBucket().toString())
                .keyFigures(keyFigureProjection.getKeyFiguresApresentadosEOrdenados().stream().map(keyFigure -> keyFigure.getId()).collect(Collectors.toList()))
                .columnDefs(getColumnDefDTOList(calendario, configuredViewProjection))
                .groups(getGroupDTOList(keyFigureProjection))
                .additionalParameters(additionalParameters)
                .periodList(periodList)
                .uom(configuredViewProjection.getUnidadeMedidaView(clusterEParametrosProjection.getParametrosGlobais()).getId())
                .build();

        atualizaPeriodosSemInformacaoComValorZero(planningBookDTO, calendario);
        atualizaListaErrosParaExibicaoLog(planningBookDTO, configuredViewProjection);
        
        return planningBookDTO;
        
    }
    
    /**
     * Monta o contrato de colunas do Planning Book sem transportar templates de
     * renderizacao. O front-end atual decide como exibir celulas editaveis,
     * tooltips, locked cells, updated cells e classes visuais a partir dos
     * campos semanticos enviados nos DTOs de key figure.
     *
     * @param calendario
     * @param configuredViewProjection
     * @return 
     */
    private List<ColumnDefDTO> getColumnDefDTOList(Calendario calendario,
                                                   ConfiguredViewProjection configuredViewProjection) {
        List<ColumnDefDTO> columnDefs = new ArrayList<>();
        // Retorna formatter 'yyyy-MM-dd' para buckets diario/semanal
        // yyyy-MM para buckets mensais
        // 'yyyy-MM-dd hh:mm:ss' para os demais buckets
        DateTimeFormatter formatter = calendario.getDateTimeFormatter();
        /*
         * O contrato base do Community e sempre uma linha por
         * material/location. Agrupamentos Enterprise sao aplicados sobre este
         * DTO depois da montagem e nao podem retirar dimensoes da fotografia
         * compartilhada.
         */
        columnDefs.add(ColumnDefDTO.builder()
                .name("Location Id")
                .field("locationId")
                .dataColumn(false)
                .dimension("location")
                .enableFiltering(true)
                .enablePinning(true)
                .build());
        columnDefs.add(ColumnDefDTO.builder()
                .name("Location Description")
                .field("locationDescription")
                .dataColumn(false)
                .enableFiltering(true)
                .enablePinning(true)
                .build());
        
        columnDefs.add(ColumnDefDTO.builder()
                .name("Material Id")
                .field("materialId")
                .dimension("material")
                .dataColumn(false)
                .enableFiltering(true)
                .enablePinning(true)
                .build());
        columnDefs.add(ColumnDefDTO.builder()
                .name("Material Description")
                .field("materialDescription")
                .dataColumn(false)
                .enableFiltering(true)
                .enablePinning(true)
                .build());
        
        // adiciona especificação da coluna 'Line', indicando demand, replenishment, production, etc
        columnDefs.add(ColumnDefDTO.builder()
                .name("Key Figure")
                .field("keyFigure")
                .dataColumn(false)
                .enableFiltering(true)
                .enablePinning(true)
                .build());
        
        // adiciona especificação da coluna 'UOM', indicando a unidade de medida padrão do material nesta location
        columnDefs.add(ColumnDefDTO.builder()
                .name("UOM")
                .field("uom")
                .dataColumn(false)
                .build());

        // adiciona coluna de vendas médias históricas
        if (configuredViewProjection.getConfiguredView().getExibeVendaMediaHistorica()) {
            columnDefs.add(ColumnDefDTO.builder()
                .name("Average Historical Sales")
                .field("Average Historical Sales")
                .dataColumn(true)
                .cellClass("pastPeriods")
                .build());
        }
        
        // adiciona especificações das colunas com períodos passados
        // cor cinza para diferenciar dos períodos futuros (class = pastPeriods)
        for (int i=0; i<calendario.getPosicaoPeriodoPresente(); i++) {
            columnDefs.add(ColumnDefDTO.builder()
                .name(calendario.getDescricaoPeriodoDePosicaoPeriodo(i))
                .field(calendario.getUltimaDataPeriodo(i).toString())//.format(formatter))
                .dataColumn(true)
                .cellClass("pastPeriods")
                .build());
        }
        for (int i = calendario.getPosicaoPeriodoPresente(); i < calendario.getNumeroPeriodosTotais(); i++) {
            columnDefs.add(ColumnDefDTO.builder()
                    .name(calendario.getDescricaoPeriodoDePosicaoPeriodo(i))
                    .field(calendario.getUltimaDataPeriodo(i).toString())//.format(formatter))
                    .dataColumn(true)
                    .enableCellEdit(true)
                    .build());
        }
        
        return columnDefs;
    }

    /**
     * Na geração inicial do PlanningBookDTO os períodos sem dados não constam em planningBookDTO.groups.keyFigures.values
     * Para garantir que o front-end processe corretamente as informações os períodos não-opcionais (ex. não se inclui Media Venda Historica)
     * serão atualizados com valor 0 quando inexistente
     * @param planningBookDTO
     */
    private void atualizaPeriodosSemInformacaoComValorZero(PlanningBookDTO planningBookDTO, Calendario calendario) {
        List<String> periodListNaoOpcionais = calendario.getListDataHorariosFinaisPorPeriodo().stream().map(LocalDateTime::toString).collect(Collectors.toList());
        for (GroupDTO groupDTO : planningBookDTO.groups) {
            atualizaPeriodosSemInformacaoComValorZero(groupDTO, periodListNaoOpcionais);
        }
    }

    private void atualizaPeriodosSemInformacaoComValorZero(GroupDTO groupDTO, List<String> periodListNaoOpcionais) {
        for (KeyFigureDTOAbstract keyFigureDTO : groupDTO.keyFigures) {
            for (String periodoNaoOpcional : periodListNaoOpcionais) {
                if (keyFigureDTO.values == null) keyFigureDTO.values = new HashMap<>();
                if (!keyFigureDTO.values.containsKey(periodoNaoOpcional)
                        && !keyFigureDTO.hasUnavailableReason(periodoNaoOpcional)) {
                    keyFigureDTO.values.put(periodoNaoOpcional, 0.0);
                }
            }
        }
        if (groupDTO.subGroups != null) {
            for (GroupDTO subGroupDTO : groupDTO.subGroups) {
                atualizaPeriodosSemInformacaoComValorZero(subGroupDTO, periodListNaoOpcionais);
            }
        }
    }
    
    private List<GroupDTO> getGroupDTOList(
            KeyFigureProjection keyFigureProjection) {

        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.getConfiguredViewProjection();
        Set<PlanningBookDfuScope> planningBookDfuScopes;

        /*
         * Quando o request traz uma selecao explicita de celulas a atualizar,
         * o DTO de resposta deve refletir somente esses escopos. Isso evita
         * que uma edicao pontual em Planning Book material/location devolva
         * linhas fora do recorte que o usuario acabou de tocar.
         */
        if (configuredViewProjection.getDetalhesSelecaoAAtualizar() != null) {

            planningBookDfuScopes = configuredViewProjection.getDetalhesSelecaoAAtualizar().stream()
                    .map(AjusteCelulaPlanningBook::getPlanningBookDfuScope)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            /*
             * Sem selecao explicita, a view inteira e o recorte autoritativo.
             * No Community esse recorte continua restrito a material/location;
             * uma operacao com varias DFUs nao altera a granularidade visual.
             */
            planningBookDfuScopes = configuredViewProjection.getPlanningBookDfuScopes();
        }

        /*
         * Cada escopo pode conter varias combinacoes e escopos de atualizacao
         * podem se sobrepor. O LinkedHashSet elimina repeticoes sem criar uma
         * chave paralela de ids; DFU ja e o value object reconhecido para a
         * combinacao material/location.
         */
        Set<DFU> dfusDetalhados = new LinkedHashSet<>();
        for (PlanningBookDfuScope planningBookDfuScope : planningBookDfuScopes) {
            FiltroDFUProjection dfuProjectionDoEscopo =
                    planningBookDfuScope.getNovoDFUProjectionInterseccaoComProjectionFiltro(
                            configuredViewProjection.getDfuProjectionFiltrado(),
                            configuredViewProjection.getClusterEParametrosProjection());
            dfusDetalhados.addAll(dfuProjectionDoEscopo.getDFUs());
        }

        return dfusDetalhados.stream()
                .sorted(Comparator
                        .comparing((DFU dfu) -> dfu.getLocation().getId())
                        .thenComparing(dfu -> dfu.getProduto().getId()))
                .map(dfu -> criaGroupDTOMaterialLocation(
                        keyFigureProjection,
                        dfu))
                .collect(Collectors.toList());

    }

    /**
     * Valida a projection de KFs antes de montar qualquer parte do DTO.
     *
     * <p>Demand/Supply ja entregam uma projection materializada no fluxo
     * normal. Esta guarda protege chamadas diretas, testes e overlays, mantendo
     * a falha na borda do Planning Book em vez de um NPE em colunas, periodos
     * ou grupos.</p>
     */
    private void validaKeyFiguresApresentadas(KeyFigureProjection keyFigureProjection) {

        if (keyFigureProjection.getKeyFiguresApresentadosEOrdenados() == null) {
            throw new IllegalArgumentException(
                    "Presented key figures are required for Planning Book DTO.");
        }
        int index = 0;
        for (KeyFigureInterface keyFigure : keyFigureProjection.getKeyFiguresApresentadosEOrdenados()) {
            if (keyFigure == null) {
                throw new IllegalArgumentException(
                        "Presented key figure at index " + index + " is required for Planning Book DTO.");
            }
            index++;
        }

    }

    private void populaGroupDTOComKeyFiguresPopuladasComDados(
            GroupDTO groupDTO,
            KeyFigureProjection keyFigureProjection,
            @Nullable Produto material,
            @Nullable Location location) {

        // adiciona key figures + valores ao nível mais desagregado --------------------------------------
        for (KeyFigureInterface keyFigure : keyFigureProjection.getKeyFiguresApresentadosEOrdenados()) {

            String keyFigureId = keyFigure.getId();
            EditMode editMode = keyFigureProjection.getEditMode(keyFigure);

            List<DFUDataKeyFigureAbstract> dadosKeyFigure = keyFigureProjection.getDadosKeyFigure(
                    keyFigure,
                    material,
                    location);

            KeyFigureDTOAbstract keyFigureDTO = criaKeyFigureDTO(keyFigure, keyFigureId, editMode);
            keyFigureDTO.importaDadosDFUDataKeyFigureAbstract(
                    keyFigureProjection.getCalendario(),
                    dadosKeyFigure);
            keyFigureDTO.importaUnavailableReasons(
                    keyFigureProjection.getCalendario(),
                    keyFigureProjection.getUnavailableReasons(keyFigure, material, location));
            groupDTO.keyFigures.add(keyFigureDTO);
        }

    }

    /**
     * Monta uma linha folha do contrato Community. A linha sempre possui
     * material e location reais; qualquer pai visual e criado posteriormente
     * pelo overlay Enterprise a partir destas folhas.
     */
    private GroupDTO criaGroupDTOMaterialLocation(
            KeyFigureProjection keyFigureProjection,
            DFU dfu) {

        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.getConfiguredViewProjection();
        ClusterEParametrosProjection clusterEParametrosProjection = configuredViewProjection.getClusterEParametrosProjection();
        Calendario calendario = keyFigureProjection.getCalendario();
        Produto material = dfu.getProduto();
        Location location = dfu.getLocation();
        if (material == null) {
            throw new IllegalStateException(
                    "Community Planning Book cannot create a leaf without material. DFU: " + dfu);
        }
        if (location == null) {
            throw new IllegalStateException(
                    "Community Planning Book cannot create a leaf without location. DFU: " + dfu);
        }

        GroupDTO groupDTO = new GroupDTO();
        groupDTO.locationDescriptionCols = new HashMap<>();
        groupDTO.locationDescriptionCols.put("locationId", location.getId());
        groupDTO.locationDescriptionCols.put("locationDescription", location.getDescricao());
        groupDTO.materialDescriptionCols = new HashMap<>();
        groupDTO.materialDescriptionCols.put("materialId", material.getId());
        groupDTO.materialDescriptionCols.put("materialDescription", material.getDescricao());

        populaGroupDTOComKeyFiguresPopuladasComDados(
                groupDTO,
                keyFigureProjection,
                material,
                location);
        populaGroupDTOComColunasOpcionais(
                groupDTO,
                keyFigureProjection);

        if (!configuredViewProjection.getConfiguredView().getPermiteAlteracaoHorizonteCongelado()) {
            populaGroupDTOComAdditionalClassHorizonteCongelado(
                    groupDTO,
                    location,
                    material,
                    configuredViewProjection.getDfuProjectionFiltrado(),
                    calendario,
                    clusterEParametrosProjection);
        }

        populaGroupDTOComAdditionalClassETooltipDeErro(
                groupDTO,
                configuredViewProjection);

        return groupDTO;

    }

    private void populaGroupDTOComColunasOpcionais(
            GroupDTO groupDTO,
            KeyFigureProjection keyFigureProjection) {

        ConfiguredViewProjection configuredViewProjection = keyFigureProjection.getConfiguredViewProjection();
        ConfiguredView configuredView = configuredViewProjection.getConfiguredView();
        Calendario calendario = keyFigureProjection.getCalendario();

        for (KeyFigureInterface keyFigure : keyFigureProjection.getKeyFiguresApresentadosEOrdenados()) {
            if (keyFigure instanceof KeyFigureStandard) {
                // popula coluna de vendas históricas médias, se configurada e se key figures de demanda total ou venda histórica
                if (((KeyFigureStandard) keyFigure).getKeyFigureStandardEnum().equals(KeyFigureStandardEnum.DEMANDA_DIRETA_TOTAL_DP)
                    || ((KeyFigureStandard) keyFigure).getKeyFigureStandardEnum().equals(KeyFigureStandardEnum.HISTORICO_VENDAS)) {

                    int numeroPeriodosHistoricosVendas = configuredView.getNumeroPeriodosHistoricosDemandPlanningBook();
                    if (configuredView.getExibeVendaMediaHistorica() && numeroPeriodosHistoricosVendas > 0) {
                        // filtra o KeyFigureDTO do GroupDTO associado a este KeyFigure
                        KeyFigureDTOPadrao keyFigureDTO = (KeyFigureDTOPadrao) groupDTO.keyFigures
                                .stream()
                                .filter(keyFigureDTOIterado -> keyFigureDTOIterado.getKeyFigure().equals(keyFigure.getId()))
                                .findAny()
                                .orElseThrow(() -> new IllegalStateException(
                                        "Planning Book group does not contain key figure "
                                                + keyFigure.getId()
                                                + " required to calculate Average Historical Sales."));

                        double valorAcumulado = 0;
                        for (int posicaoPeriodoPassado = calendario.getPosicaoPeriodoPresente() - numeroPeriodosHistoricosVendas; posicaoPeriodoPassado < calendario.getPosicaoPeriodoPresente(); posicaoPeriodoPassado++) {
                            valorAcumulado += (double) keyFigureDTO.values.getOrDefault(
                                    calendario.getUltimaDataHorarioPeriodo(
                                            posicaoPeriodoPassado).toString(), 0.0);
                        }
                        keyFigureDTO.values.put("Average Historical Sales", valorAcumulado / calendario.getNumeroPeriodosPassados());
                    }

                }
            }
        }

    }
    
    private static void populaGroupDTOComAdditionalClassHorizonteCongelado(
            GroupDTO groupDTO, 
            @Nullable Location locationFiltradaNullable,
            @Nullable Produto materialFiltradoNullable,
            FiltroDFUProjection dfuProjectionFiltradoParaAgrupamento,
            Calendario calendario,
            ClusterEParametrosProjection clusterEParametrosProjection) {
        
        List<DFU> dfusConsideradasParaCalculoHorizonteCongelado = dfuProjectionFiltradoParaAgrupamento.getDFUsViaveis(
                (materialFiltradoNullable == null) ? null : Set.of(materialFiltradoNullable), 
                (locationFiltradaNullable == null) ? null : Set.of(locationFiltradaNullable));
        
        int horizonteCongeladoPeriodos = dfusConsideradasParaCalculoHorizonteCongelado.parallelStream()
                .mapToInt(dfu -> clusterEParametrosProjection.getDPHorizonteCongeladoEmPeriodos(
                        dfu.getLocation(), dfu.getProduto(), calendario).orElse(0))
                .max().orElse(0);
        
        for (int i = calendario.getPosicaoPeriodoPresente(); i < calendario.getPosicaoPeriodoPresente() + horizonteCongeladoPeriodos; i++) {
            int iParaLambda = i;
            for (KeyFigureDTOAbstract keyFigureDTO : groupDTO.getKeyFigures()) {
                if (keyFigureDTO.additionalClasses == null) keyFigureDTO.additionalClasses = new HashMap<>();
                keyFigureDTO.addAdditionalClass(
                        calendario.getUltimaDataHorarioPeriodo(iParaLambda),
                        "crosshatch");
            }
        }
                
    }

    
    private static void populaGroupDTOComAdditionalClassETooltipDeErro(GroupDTO groupDTO, ConfiguredViewProjection configuredViewProjection) {
        
        if (configuredViewProjection.getErroAtualizacaoPorDetalheSelecao() == null) return;
        
        for (Entry<AjusteCelulaPlanningBook,String> detalheSelecaoEErro : configuredViewProjection.getErroAtualizacaoPorDetalheSelecao().entrySet()) {

            AjusteCelulaPlanningBook ajusteCelulaPlanningBook = detalheSelecaoEErro.getKey();
            String keyFigureId = ajusteCelulaPlanningBook.getKeyFigureId();
            
            String mensagemErro = detalheSelecaoEErro.getValue();
            
            PlanningBookDfuScope planningBookDfuScope = ajusteCelulaPlanningBook.getPlanningBookDfuScope();
            
            if (groupDTORepresentaPlanningBookDfuScope(groupDTO, planningBookDfuScope)) {
                    
                KeyFigureDTOAbstract keyFigureDTO = groupDTO.keyFigures.stream()
                        .filter(kfDto -> kfDto.keyFigure.equals(keyFigureId))
                        .findAny().orElseGet(() -> {

                            KeyFigureDTOPadrao novoKeyFigureDTO = KeyFigureDTOPadrao.builder()
                                    .keyFigure(keyFigureId)
                                    .additionalClasses(new HashMap<>())
                                    .build();

                            groupDTO.keyFigures.add(novoKeyFigureDTO);

                            return novoKeyFigureDTO;

                        });

                if (keyFigureDTO.additionalClasses == null) {
                    keyFigureDTO.additionalClasses = new HashMap<>();
                }
                if (keyFigureDTO.toolTips == null) {
                    keyFigureDTO.toolTips = new HashMap<>();
                }

                keyFigureDTO.addAdditionalClass(
                        ajusteCelulaPlanningBook.getDataHorarioReferencia(),
                        "errorclass");
                keyFigureDTO.updateTooltip(
                        ajusteCelulaPlanningBook.getDataHorarioReferencia(),
                        valorAnterior -> "Inserted value of " + ajusteCelulaPlanningBook.getValorNovo() + " was not processed. Cell has been replaced by original value. Error cause : " + mensagemErro);

                continue;

            }       
        }
        
    }

    /** Cria o DTO cuja agregacao corresponde ao contrato da Key Figure. */
    private KeyFigureDTOAbstract criaKeyFigureDTO(
            KeyFigureInterface keyFigure,
            String keyFigureId,
            EditMode editMode) {

        return switch (keyFigure.getModeloAgregacaoKeyFigure()) {
            case RELACAO_ENTRE_VALORES ->
                    new KeyFigureDTORelacaoEntreValores(keyFigureId, editMode);
            case COBERTURA_ESTOQUE ->
                    new KeyFigureDTOCoberturaEstoque(keyFigureId, editMode);
            default -> new KeyFigureDTOPadrao(keyFigureId, editMode);
        };

    }

    private static boolean groupDTORepresentaPlanningBookDfuScope(
            GroupDTO groupDTO,
            PlanningBookDfuScope planningBookDfuScope) {

        return Objects.equals(
                getValorColunaPlanningBookCommunity(groupDTO.materialDescriptionCols, "materialId"),
                getValorColunaPlanningBookCommunity(planningBookDfuScope.getColunasMaterialPlanningBook(), "materialId"))
                && Objects.equals(
                getValorColunaPlanningBookCommunity(groupDTO.locationDescriptionCols, "locationId"),
                getValorColunaPlanningBookCommunity(planningBookDfuScope.getColunasLocationPlanningBook(), "locationId"));

    }

    private static String getValorColunaPlanningBookCommunity(
            Map<String, String> colunas,
            String coluna) {

        return (colunas == null) ? "" : colunas.getOrDefault(coluna, "");

    }
    
    private static void atualizaListaErrosParaExibicaoLog(PlanningBookDTO planningBookDTO, ConfiguredViewProjection configuredViewProjection) {
        
        if (configuredViewProjection.getErroAtualizacaoPorDetalheSelecao() == null) return;
        
        List<String> listaErros = new ArrayList<>();
        
        for (Entry<AjusteCelulaPlanningBook,String> detalheSelecaoEErro : configuredViewProjection.getErroAtualizacaoPorDetalheSelecao().entrySet()) {

            AjusteCelulaPlanningBook ajusteCelulaPlanningBook = detalheSelecaoEErro.getKey();
            String keyFigureId = ajusteCelulaPlanningBook.getKeyFigureId();
            
            String mensagemErro = detalheSelecaoEErro.getValue();
            
            PlanningBookDfuScope planningBookDfuScope = ajusteCelulaPlanningBook.getPlanningBookDfuScope();
         
            String dimensoesMaterial = planningBookDfuScope.getColunasMaterialPlanningBook().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .reduce("", (a,b) -> a + "-" + b);
            String dimensoesLocation = planningBookDfuScope.getColunasLocationPlanningBook().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .reduce("", (a,b) -> a + "-" + b);
            
            String erro = "Error updating Key Figure " + keyFigureId + 
                    " for line "  + dimensoesMaterial + "-" + dimensoesLocation + 
                    " and period " + ajusteCelulaPlanningBook.getDataHorarioReferencia() +
                    ". Error message : " + mensagemErro;
            
            listaErros.add(erro);
            
        }
        
        planningBookDTO.setErrorMessage(listaErros);
                
    }
        
}
