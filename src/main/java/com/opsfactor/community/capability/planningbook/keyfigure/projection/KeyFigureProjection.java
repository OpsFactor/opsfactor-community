package com.opsfactor.community.capability.planningbook.keyfigure.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.demandplanning.demandplan.domain.DemandPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.platform.projection.inmemorybi.applied.BIProjectionMaterialLocationPeriodo;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.projection.SalesProjectionLocationMaterialData;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.demandplanning.demandplan.projection.DemandPlanningProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureAbstract;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureCoberturaEstoque;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigurePadrao;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.dfudata.DFUDataKeyFigureRelacaoEntreValores;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.capability.masterdata.calendar.temporalsplit.projection.SplitTemporalProjectionPorDfu;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.Getter;
import org.javatuples.Pair;

import jakarta.annotation.Nullable;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Projection em memoria das Key Figures exibidas no Planning Book Community.
 *
 * <p>A factory popula esta classe com calendario, view, projections auxiliares
 * e um BI material/location/periodo. Os metodos publicos de escrita validam as
 * chaves funcionais antes de indexar dados para que Demand, Supply e overlays
 * Enterprise nao criem linhas de Planning Book com material/location, periodo,
 * key figure ou valor quebrados.</p>
 */
@Getter
public class KeyFigureProjection {

    /*
     * View material/location usada para resolver escopo, unidade de exibicao e
     * filtros DFU. Agrupamentos por caracteristica pertencem ao Enterprise.
     */
    ConfiguredViewProjection configuredViewProjection;

    UnidadeMedidaProjection unidadeMedidaProjection;
    SupplyNetworkProjection supplyNetworkProjection;

    /*
     * Calendario da grade exibida. Todos os dados adicionados ao BI precisam
     * usar datas pertencentes a este calendario ou posicoes derivadas dele.
     */
    Calendario calendario;

    List<KeyFigureInterface> keyFiguresApresentadosEOrdenados = new ArrayList<>();

    /*
     * Sobrescrita transitória de edição para a grade corrente. Ela não altera a
     * configuração persistida da Key Figure: serve a cenários em que um overlay
     * materializa um dado histórico ou de auditoria apenas para consulta.
     */
    Set<KeyFigureInterface> keyFiguresSomenteLeitura = new HashSet<>();

    // Apenas para DP. Usado para compor o calculo da demanda direta DP.
    List<KeyFigureInterface> keyFiguresTotalizacaoDemanda = new ArrayList<>();

    /*
     * Indice material/location/periodo/key figure usado pelo Planning Book.
     * Deve ser criado pela factory antes de qualquer add/get de dados.
     */
    BIProjectionMaterialLocationPeriodo<DFUDataKeyFigureAbstract> biEmMemoriaDFUDataKeyFigure;

    /*
     * Estado lateral de indisponibilidade por celula. Valores numericos
     * continuam exclusivamente no BI; uma razao aqui significa que a celula
     * nao pode ser interpretada como zero nem agregada parcialmente.
     */
    private final Map<Location,
            Map<Produto, Map<LocalDateTime, Map<KeyFigureInterface, String>>>>
            unavailableReasonsByLocationMaterialDateAndKeyFigure = new HashMap<>();

    // Planos de referência
    DemandPlan demandPlan;
    SupplyPlan supplyPlan;

    // Usados pelo factory para evitar múltiplas chamadas para criação dos projections
    // Serão apagados na etapa de conversão para DTOs para evitar consumo excessivo de memória
    @Transient
    DemandPlanningProjection demandPlanningProjectionReferenciaCache;
    @Transient
    SupplyPlanningProjection supplyPlanningProjectionCache;
    @Transient
    PoliticaEstoquesProjection politicaEstoquesProjectionCache;
    @Transient
    SalesProjectionLocationMaterialData salesProjectionCache;
    @Transient
    SplitTemporalProjectionPorDfu splitTemporalProjectionPorDfuCalendarioDPParaSNPCache;

    public DFUDataKeyFigurePadrao addDadoDFUKeyFigurePadrao(
            Location location,
            Produto material,
            int periodoCalendario,
            KeyFigureInterface keyFigure,
            double valor) {

        validaCalendarioKeyFigureProjection(
                "KeyFigureProjection requires calendar before adding Key Figure data by period.");
        return addDadoDFUKeyFigurePadrao(
                location,
                material,
                calendario.getUltimaDataHorarioPeriodo(periodoCalendario),
                keyFigure,
                valor);

    }

    public DFUDataKeyFigurePadrao addDadoDFUKeyFigurePadrao(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure,
            double valor) {

        validaBiKeyFigureProjection(
                "KeyFigureProjection requires BI before adding Key Figure data.");
        validaDadoDfuKeyFigurePadrao(
                location,
                material,
                data,
                keyFigure,
                valor);

        DFUDataKeyFigurePadrao novoDadoKeyFigure = DFUDataKeyFigurePadrao.builder()
                .location(location)
                .produto(material)
                .data(data)
                .keyFigure(keyFigure)
                .valor(valor)
                .build();

        biEmMemoriaDFUDataKeyFigure.addDadoAoBI(novoDadoKeyFigure);

        return novoDadoKeyFigure;

    }

    public DFUDataKeyFigurePadrao getOrAddDadoDFUKeyFigurePadrao(
            Location location,
            Produto material,
            int periodoCalendario,
            KeyFigureInterface keyFigure) {

        validaCalendarioKeyFigureProjection(
                "KeyFigureProjection requires calendar before getting or adding Key Figure data by period.");
        validaBiKeyFigureProjection(
                "KeyFigureProjection requires BI before getting or adding Key Figure data.");
        validaDfuKeyFigurePadrao(
                location,
                material,
                calendario.getUltimaDataHorarioPeriodo(periodoCalendario),
                keyFigure);

        return biEmMemoriaDFUDataKeyFigure.getValores(
                material, location,
                Pair.with("periodo", periodoCalendario),
                Pair.with("KeyFigure", keyFigure))
                .findFirst()
                .map(dfuDataKeyFigureAbstract -> (DFUDataKeyFigurePadrao) dfuDataKeyFigureAbstract)
                .orElseGet(() -> {
                    DFUDataKeyFigurePadrao dfuDataKeyFigurePadraoNovo = DFUDataKeyFigurePadrao.builder()
                            .keyFigure(keyFigure)
                            .location(location)
                            .produto(material)
                            .data(calendario.getUltimaDataHorarioPeriodo(periodoCalendario))
                            .build();
                    biEmMemoriaDFUDataKeyFigure.addDadoAoBI(dfuDataKeyFigurePadraoNovo);
                    return dfuDataKeyFigurePadraoNovo;
                });

    }

    public DFUDataKeyFigurePadrao getOrAddDadoDFUKeyFigurePadrao(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure) {

        validaCalendarioKeyFigureProjection(
                "KeyFigureProjection requires calendar before getting or adding Key Figure data by date.");
        validaDataReferenciaKeyFigureProjection(
                data,
                "KeyFigureProjection period reference is required to get or add Key Figure data.");
        return getOrAddDadoDFUKeyFigurePadrao(
                location,
                material,
                calendario.getPosicaoPeriodo(data),
                keyFigure);

    }

    public List<DFUDataKeyFigureAbstract> getDadosKeyFigure(
            KeyFigureInterface keyFigure,
            @Nullable Produto material,
            @Nullable Location location) {

        validaBiKeyFigureProjection(
                "KeyFigureProjection requires BI before reading Key Figure data.");
        validaMaterialOpcional(material);
        validaLocationOpcional(location);

        return biEmMemoriaDFUDataKeyFigure.getValores(
                material,
                location,
                Pair.with("KeyFigure", keyFigure))
                .collect(Collectors.toList());

    }

    public List<DFUDataKeyFigureAbstract> getDadosKeyFigure(KeyFigureInterface keyFigure) {
        validaBiKeyFigureProjection(
                "KeyFigureProjection requires BI before reading Key Figure data.");
        return biEmMemoriaDFUDataKeyFigure.getValores(
                Pair.with("KeyFigure", keyFigure))
                .collect(Collectors.toList());
    }

    /**
     * Adiciona os componentes de uma razão para que o Planning Book consolide
     * corretamente os filhos sem somar taxas já calculadas.
     */
    public DFUDataKeyFigureRelacaoEntreValores addDadoDFUKeyFigureRelacaoEntreValores(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure,
            double valorNominador,
            double valorDenominador) {

        validaBiKeyFigureProjection("KeyFigureProjection requires BI before adding Key Figure ratio data.");
        validaDadoDfuKeyFigurePadrao(location, material, data, keyFigure, valorNominador);
        if (!Double.isFinite(valorDenominador)) {
            throw new IllegalArgumentException("KeyFigureProjection ratio denominator must be finite before adding Key Figure data.");
        }
        DFUDataKeyFigureRelacaoEntreValores dadoKeyFigure = DFUDataKeyFigureRelacaoEntreValores.builder()
                .location(location)
                .produto(material)
                .data(data)
                .keyFigure(keyFigure)
                .numeratorValue(valorNominador)
                .denominatorValue(valorDenominador)
                .build();
        biEmMemoriaDFUDataKeyFigure.addDadoAoBI(dadoKeyFigure);
        return dadoKeyFigure;

    }

    /**
     * Registra os componentes da trajetória de estoque de uma DFU. O DTO
     * especializado os agrega antes de calcular a cobertura exibida.
     */
    public DFUDataKeyFigureCoberturaEstoque addDadoDFUKeyFigureCoberturaEstoque(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure,
            double quantidadeEstoqueProjetado,
            double saldoEntradasSaidas) {

        validaBiKeyFigureProjection("KeyFigureProjection requires BI before adding stock coverage data.");
        validaDadoDfuKeyFigurePadrao(location, material, data, keyFigure, quantidadeEstoqueProjetado);
        if (!Double.isFinite(saldoEntradasSaidas)) {
            throw new IllegalArgumentException("KeyFigureProjection stock coverage balance must be finite before adding Key Figure data.");
        }
        DFUDataKeyFigureCoberturaEstoque dadoKeyFigure = DFUDataKeyFigureCoberturaEstoque.builder()
                .location(location)
                .produto(material)
                .data(data)
                .keyFigure(keyFigure)
                .quantidadeEstoqueProjetado(quantidadeEstoqueProjetado)
                .saldoEntradasSaidas(saldoEntradasSaidas)
                .build();
        biEmMemoriaDFUDataKeyFigure.addDadoAoBI(dadoKeyFigure);
        return dadoKeyFigure;

    }

    public List<DFUDataKeyFigureAbstract> getDadosKeyFigures(Collection<KeyFigureInterface> keyFigures) {
        validaBiKeyFigureProjection(
                "KeyFigureProjection requires BI before reading multiple Key Figures.");
        validaKeyFiguresObrigatorias(keyFigures);
        return biEmMemoriaDFUDataKeyFigure.getTodosValores()
                .filter(keyFigureData -> keyFigures.contains(keyFigureData.getKeyFigure()))
                .collect(Collectors.toList());
    }


    public boolean getPermiteEdicao(KeyFigureInterface keyFigure) {

        // Permissao por key figure na User View pertence ao Enterprise.
        // O Community sempre usa o comportamento padrao da propria KF standard.
        return !keyFiguresSomenteLeitura.contains(keyFigure)
                && keyFigure.getPadraoPermiteEdicao();

    }

    /**
     * Registra a primeira razao de indisponibilidade de uma celula da
     * projection sem inserir valor numerico artificial no BI.
     */
    public void defineUnavailableReason(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure,
            String unavailableReason) {

        validaDfuKeyFigurePadrao(location, material, data, keyFigure);
        if (unavailableReason == null || unavailableReason.isBlank()) {
            throw new IllegalArgumentException(
                    "KeyFigureProjection unavailable reason is required before marking a Key Figure cell unavailable.");
        }

        if (FuncoesMap.getElementoDeNestedMap(
                unavailableReasonsByLocationMaterialDateAndKeyFigure,
                String.class,
                location,
                material,
                data,
                keyFigure).isEmpty()) {
            FuncoesMap.adicionaElementoAoNestedMap(
                    unavailableReason,
                    unavailableReasonsByLocationMaterialDateAndKeyFigure,
                    location,
                    material,
                    data,
                    keyFigure);
        }

    }

    /**
     * Retorna as razoes de indisponibilidade da key figure no recorte de uma
     * folha material/location. Ausencia no mapa representa celula disponivel.
     */
    public Map<LocalDateTime, String> getUnavailableReasons(
            KeyFigureInterface keyFigure,
            Produto material,
            Location location) {


        Map<LocalDateTime, Map<KeyFigureInterface, String>> unavailableReasonsByDate =
                unavailableReasonsByLocationMaterialDateAndKeyFigure
                        .getOrDefault(location, Map.of())
                        .getOrDefault(material, Map.of());
        return unavailableReasonsByDate.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(keyFigure))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(keyFigure),
                        (firstReason, ignoredReason) -> firstReason,
                        LinkedHashMap::new));

    }

    public EditMode getEditMode(KeyFigureInterface keyFigure) {

        // Mesma regra de getPermiteEdicao: ignorar qualquer configuracao
        // Enterprise persistida junto da Configured View.
        return getPermiteEdicao(keyFigure) ? keyFigure.getEditModePadrao() : EditMode.NOEDIT;

    }

    /**
     * Marca uma Key Figure apresentada nesta projection como exclusivamente de
     * leitura, sem alterar seu cadastro persistido ou o default usado por
     * outras views.
     */
    public void defineKeyFigureComoSomenteLeitura(KeyFigureInterface keyFigure) {

        keyFiguresSomenteLeitura.add(keyFigure);

    }

    private void validaDadoDfuKeyFigurePadrao(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure,
            double valor) {

        validaDfuKeyFigurePadrao(
                location,
                material,
                data,
                keyFigure);
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException(
                    "KeyFigureProjection value must be finite before adding Key Figure data.");
        }

    }

    private void validaDfuKeyFigurePadrao(
            Location location,
            Produto material,
            LocalDateTime data,
            KeyFigureInterface keyFigure) {

        validaDataReferenciaKeyFigureProjection(
                data,
                "KeyFigureProjection period reference is required before adding Key Figure data.");

    }

    private void validaBiKeyFigureProjection(String mensagemErro) {

        if (biEmMemoriaDFUDataKeyFigure == null) {
            throw new IllegalStateException(mensagemErro);
        }

    }

    private void validaCalendarioKeyFigureProjection(String mensagemErro) {

        if (calendario == null) {
            throw new IllegalStateException(mensagemErro);
        }

    }

    private static void validaLocationOpcional(@Nullable Location location) {

        if (location != null && (location.getId() == null || location.getId().isBlank())) {
            throw new IllegalArgumentException(
                    "KeyFigureProjection location id is required to read Key Figure data.");
        }

    }

    private static void validaMaterialOpcional(@Nullable Produto material) {

        if (material != null && (material.getId() == null || material.getId().isBlank())) {
            throw new IllegalArgumentException(
                    "KeyFigureProjection material id is required to read Key Figure data.");
        }

    }

    private static void validaDataReferenciaKeyFigureProjection(
            LocalDateTime data,
            String mensagemErro) {

        if (data == null) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

    private static void validaKeyFiguresObrigatorias(Collection<KeyFigureInterface> keyFigures) {

        if (keyFigures == null) {
            throw new IllegalArgumentException(
                    "KeyFigureProjection key figure collection is required to read multiple Key Figures.");
        }
        int index = 0;
        for (KeyFigureInterface keyFigure : keyFigures) {
            if (keyFigure == null) {
                throw new IllegalArgumentException(
                        "KeyFigureProjection key figure at index " + index + " is required to read multiple Key Figures.");
            }
            index++;
        }

    }

}
