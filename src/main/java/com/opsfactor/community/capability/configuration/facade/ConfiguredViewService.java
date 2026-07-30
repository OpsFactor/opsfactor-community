package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView.TipoView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.planningbook.domain.AjusteCelulaPlanningBook;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import com.opsfactor.community.capability.planningbook.facade.dto.SelectedPlanningBookCellDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import jakarta.annotation.Nullable;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service de views configuradas e escopo material/location do Planning Book.
 *
 * <p>No Community, views servem para filtros e preferencias simples. Selecao
 * dinamica de KFs, agrupamentos por caracteristica e detalhes sku/location
 * configuraveis pertencem ao Enterprise.</p>
 */
@Service
public class ConfiguredViewService {

    /**
     * Factory que materializa a view filtrada em memoria para os services de
     * Planning Book. O @Autowired fica explicito para diferenciar bean de
     * estado local.
     */
    @Autowired
    private ConfiguredViewProjectionFactory configuredViewProjectionFactory;

    /**
     * Repository das configuracoes persistidas de view.
     */
    @Autowired
    private ConfiguredViewRepository configuredViewRepository;

    /**
     * Factory de parametros/cluster usada para montar projections coerentes
     * com a UOM e filtros de material/location da view.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;
    
    public ConfiguredView getConfiguredView(
            String userId, String nomeView, ConfiguredView.TipoView tipoView) {

        return configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                userId, nomeView, tipoView).orElseThrow(() -> new NoResultException(
                        "No configured view found for user " + userId + ", view " + nomeView + " for " + tipoView.toString()));
            
    }    
    
    public ConfiguredViewProjection getConfiguredViewProjectionComDTOSelecaoCelulas(
            String userId, String nomeView, ConfiguredView.TipoView tipoView,
            TamanhoBucket tamanhoBucket,
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO,
            ClusterEParametrosProjection clusterEParametrosProjection) {
    
        return getConfiguredViewProjectionComDTOSelecaoCelulas(
                userId, nomeView, tipoView, 
                tamanhoBucket,
                Collections.singleton(selectedPlanningBookCellDTO), 
                clusterEParametrosProjection,
                null);
    
    }    
    
    public ConfiguredViewProjection getConfiguredViewProjectionComDTOSelecaoCelulas(
            String userId, String nomeView, ConfiguredView.TipoView tipoView,
            TamanhoBucket tamanhoBucket,
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable BiFunction<Produto,Location,Boolean> filtrosAdicionaisDfus) {
    
        return getConfiguredViewProjectionComDTOSelecaoCelulas(
                userId, nomeView, tipoView, 
                tamanhoBucket,
                Collections.singleton(selectedPlanningBookCellDTO), 
                clusterEParametrosProjection,
                filtrosAdicionaisDfus);
    
    }    
    
    /**
     * Cria um ConfiguredViewProjection populando o Set<DetalheSelecao> detalhesSelecaoAAtualizar
     * com os valores de Collection<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOCollection
     * @param userId
     * @param nomeView
     * @param tipoView
     * @param selectedPlanningBookCellDTOCollection DTOs que irão popular o Set<DetalheSelecao> detalhesSelecaoAAtualizar do projection
     * @param clusterEParametrosProjection
     * @return 
     */
    public ConfiguredViewProjection getConfiguredViewProjectionComDTOSelecaoCelulas(
            String userId, String nomeView, ConfiguredView.TipoView tipoView,
            TamanhoBucket tamanhoBucket,
            Collection<SelectedPlanningBookCellDTO> selectedPlanningBookCellDTOCollection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable BiFunction<Produto,Location,Boolean> filtrosAdicionaisDfus) {
     
        if (selectedPlanningBookCellDTOCollection == null) {
            throw new IllegalArgumentException("Planning Book selected cell collection is required");
        }
        if (tamanhoBucket == null) {
            throw new IllegalArgumentException("Planning Book selected cell bucket size is required");
        }


        ConfiguredViewProjection configuredViewProjection = configuredViewProjectionFactory.getConfiguredViewProjection(
                userId, nomeView, tipoView, 
                clusterEParametrosProjection,
                filtrosAdicionaisDfus);
        configuredViewProjection = configuredViewProjection;
        
        // inicializa o set de agrupamentos 'a atualizar'
        configuredViewProjection.setDetalhesSelecaoAAtualizar(new HashSet<>());
        configuredViewProjection.setErroAtualizacaoPorDetalheSelecao(new HashMap<>());
        
        for (SelectedPlanningBookCellDTO selectedPlanningBookCellDTO : selectedPlanningBookCellDTOCollection) {

            PlanningBookDfuScope planningBookDfuScopeAAtualizar = getPlanningBookDfuScopeAAtualizar(
                    selectedPlanningBookCellDTO,
                    configuredViewProjection,
                    clusterEParametrosProjection);

            // cria o DetalheSelecao, que traz todas as informações (periodo, quantidade e DFU) da célula modificada
            AjusteCelulaPlanningBook ajusteCelulaPlanningBook = new AjusteCelulaPlanningBook(
                    Calendario.getUltimaDataHorarioPeriodo(selectedPlanningBookCellDTO.period, tamanhoBucket), 
                    selectedPlanningBookCellDTO.keyFigure, 
                    selectedPlanningBookCellDTO.uom,
                    selectedPlanningBookCellDTO.newValue,
                    selectedPlanningBookCellDTO.oldValue,
                    planningBookDfuScopeAAtualizar);
            
            // carrega o agrupamento como 'a atualizar' no projection
            configuredViewProjection.getDetalhesSelecaoAAtualizar().add(ajusteCelulaPlanningBook);
            
        }
        
        return configuredViewProjection;
        
    }

    /**
     * Community aceita ajustes apenas no menor nivel material/location. Payloads
     * sem uma das duas dimensoes representam ajuste agregado e pertencem ao
     * Enterprise, mesmo que uma view legada ainda tente enviar esse formato.
     */
    protected PlanningBookDfuScope getPlanningBookDfuScopeAAtualizar(
            SelectedPlanningBookCellDTO selectedPlanningBookCellDTO,
            ConfiguredViewProjection configuredViewProjection,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        validaSelectedPlanningBookCellDTOCommunity(selectedPlanningBookCellDTO);

        Produto materialSelecionado = clusterEParametrosProjection.getMaterialPersistido(selectedPlanningBookCellDTO.getMaterialId());
        Location locationSelecionada = clusterEParametrosProjection.getLocationPersistida(selectedPlanningBookCellDTO.getLocationId());
        validaDfuSelecionadoPertenceAViewCommunity(
                configuredViewProjection,
                materialSelecionado,
                locationSelecionada);

        return PlanningBookDfuScope.deMaterialLocation(materialSelecionado, locationSelecionada);

    }

    protected void validaSelectedPlanningBookCellDTOCommunity(SelectedPlanningBookCellDTO selectedPlanningBookCellDTO) {

        if (selectedPlanningBookCellDTO == null) {
            throw new IllegalArgumentException("Planning Book selected cell is required");
        }

        if (selectedPlanningBookCellDTO.parentSelection != null) {
            throw new RequiresEnterpriseVersionException(
                    "Planning Book parent selections");
        }

        if (!selectedPlanningBookCellDTO.isMaterialLevel() || !selectedPlanningBookCellDTO.isLocationLevel()) {
            throw new RequiresEnterpriseVersionException("Planning Book aggregated adjustments");
        }
        if (selectedPlanningBookCellDTO.getMaterialId() == null || selectedPlanningBookCellDTO.getMaterialId().isBlank()) {
            throw new IllegalArgumentException("Planning Book selected cell material id is required");
        }
        if (selectedPlanningBookCellDTO.getLocationId() == null || selectedPlanningBookCellDTO.getLocationId().isBlank()) {
            throw new IllegalArgumentException("Planning Book selected cell location id is required");
        }
        if (selectedPlanningBookCellDTO.period == null) {
            throw new IllegalArgumentException("Planning Book selected cell period is required");
        }
        if (selectedPlanningBookCellDTO.keyFigure == null || selectedPlanningBookCellDTO.keyFigure.isBlank()) {
            throw new IllegalArgumentException("Planning Book selected cell key figure is required");
        }
        if (selectedPlanningBookCellDTO.uom == null || selectedPlanningBookCellDTO.uom.isBlank()) {
            throw new IllegalArgumentException("Planning Book selected cell unit of measure is required");
        }
        if (selectedPlanningBookCellDTO.newValue == null || !Double.isFinite(selectedPlanningBookCellDTO.newValue)) {
            throw new IllegalArgumentException("Planning Book selected cell new value must be finite");
        }

    }

    /**
     * Garante que a linha material/location enviada pelo front realmente faz
     * parte da view Community antes de criar o ajuste. O Community nao usa
     * estruturas de agregacao ou filtros DFU por caracteristica para resolver
     * selecoes ambiguas; qualquer divergencia deve falhar explicitamente.
     *
     * <p>Esta guarda fica no service, e nao dentro da projection, porque aqui
     * conhecemos a semantica operacional: estamos validando uma celula recebida
     * do Planning Book antes de persistir ajuste. Projection quebrada, entidade
     * selecionada ausente ou DFU fora da view devem falhar como erro funcional
     * claro, antes que `FiltroDFUProjection` ou `ConfiguredView` gerem NPEs
     * pouco diagnosticaveis.</p>
     */
    protected void validaDfuSelecionadoPertenceAViewCommunity(
            ConfiguredViewProjection configuredViewProjection,
            Produto materialSelecionado,
            Location locationSelecionada) {

        if (configuredViewProjection == null) {
            throw new IllegalArgumentException(
                    "Configured view projection is required for Planning Book selected cell validation");
        }
        if (configuredViewProjection.getDfuProjectionFiltrado() == null) {
            throw new IllegalArgumentException(
                    "Filtered DFU projection is required for Planning Book selected cell validation");
        }
        validaMaterialSelecionadoPlanningBookCommunity(materialSelecionado);
        validaLocationSelecionadaPlanningBookCommunity(locationSelecionada);

        if (!configuredViewProjection.getDfuProjectionFiltrado().contemCombinacaoLocationMaterial(
                locationSelecionada,
                materialSelecionado)) {
            throw new IllegalArgumentException(
                    "Material " + materialSelecionado.getId()
                            + " and location " + locationSelecionada.getId()
                            + " are not accessible for view "
                            + getNomeViewConfiguredViewProjectionCommunity(configuredViewProjection));
        }

    }

    /**
     * Valida a chave funcional do material selecionado pelo front. O lookup
     * anterior em `ClusterEParametrosProjection` deveria retornar uma entidade
     * persistida, mas manter a guarda aqui torna o contrato da celula selecionada
     * independente de como a projection foi construida em testes ou overlays.
     */
    private void validaMaterialSelecionadoPlanningBookCommunity(
            Produto materialSelecionado) {

        if (materialSelecionado == null) {
            throw new IllegalArgumentException(
                    "Selected material is required for Planning Book selected cell validation");
        }
        if (materialSelecionado.getId() == null || materialSelecionado.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Selected material id is required for Planning Book selected cell validation");
        }

    }

    /**
     * Valida a chave funcional da location selecionada pelo front antes de
     * consultar a projection filtrada de DFUs.
     */
    private void validaLocationSelecionadaPlanningBookCommunity(
            Location locationSelecionada) {

        if (locationSelecionada == null) {
            throw new IllegalArgumentException(
                    "Selected location is required for Planning Book selected cell validation");
        }
        if (locationSelecionada.getId() == null || locationSelecionada.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Selected location id is required for Planning Book selected cell validation");
        }

    }

    /**
     * Retorna o nome da view para mensagens funcionais de rejeicao.
     *
     * <p>Neste caminho a projection vem da `ConfiguredViewProjectionFactory`,
     * que sempre popula a entidade de view. Se esse contrato for quebrado,
     * falhamos explicitamente em vez de esconder o problema atras de uma mensagem
     * generica ou de uma excecao nula durante a montagem da mensagem.</p>
     */
    private String getNomeViewConfiguredViewProjectionCommunity(
            ConfiguredViewProjection configuredViewProjection) {

        if (configuredViewProjection.getConfiguredView() == null) {
            throw new IllegalArgumentException(
                    "Configured view is required for Planning Book selected cell validation");
        }
        if (configuredViewProjection.getConfiguredView().getNomeView() == null
                || configuredViewProjection.getConfiguredView().getNomeView().isBlank()) {
            throw new IllegalArgumentException(
                    "Configured view name is required for Planning Book selected cell validation");
        }

        return configuredViewProjection.getConfiguredView().getNomeView();

    }

    /**
     * Garante que a projection central de parametros/master data existe antes
     * de montar ou auditar views material/location.
     *
     * <p>`ConfiguredViewService` e uma borda transversal usada por Demand e
     * Supply Planning Books. Projection nula ou sem parametros globais indica
     * snapshot quebrado; seguir adiante faria a falha aparecer como erro de
     * factory, lookup de material/location ou stream em paralelo.</p>
     */
    public Map<Location,Set<Produto>> getDTOsNaoCobertosPorConfiguredViews(TipoView tipoView) {

        if (tipoView == null) {
            throw new IllegalArgumentException(
                    "Configured view type is required for configured view coverage");
        }
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        List<ConfiguredView> configuredViewsCobertura =
                getConfiguredViewsCoberturaCommunity(
                        configuredViewRepository.customFindByConfiguredViewCompositeKeyTipoView(tipoView));
        
        Map<Location,Set<Produto>> mapaDFUs = configuredViewsCobertura.parallelStream()
                .flatMap(configuredView -> {
                    ConfiguredViewProjection configuredViewProjection = configuredViewProjectionFactory.getConfiguredViewProjection(configuredView, clusterEParametrosProjection);
                    
                    return configuredViewProjection.getDfuProjectionFiltrado().getDFUs().stream();
                })
                .distinct()
                .collect(Collectors.groupingBy(DFU::getLocation, Collectors.mapping(DFU::getProduto, Collectors.toSet())));
        
        Map<Location,Set<Produto>> mapaDFUsNaoCobertos = new HashMap<>();
        
        for (Location location : clusterEParametrosProjection.getLocationsAtivas()) {
            for (Produto material : clusterEParametrosProjection.getMateriaisAtivosEmLocation(location)) {
                
                Set<Produto> materiaisEmViews = mapaDFUs.get(location);
                
                if (materiaisEmViews == null || !materiaisEmViews.contains(material)) {
                    mapaDFUsNaoCobertos
                            .computeIfAbsent(location, x -> new HashSet<>())
                            .add(material);
                }
            }
        }
        
        return mapaDFUsNaoCobertos;        
        
    }

    /**
     * Valida a fotografia de views usada para calcular cobertura material/location.
     *
     * <p>Lista vazia significa que nenhuma view cobre DFUs e, portanto, todas
     * as DFUs ativas seguem candidatas a `nao cobertas`. Lista nula ou item nulo
     * indica quebra do repository/snapshot e deve falhar antes da factory de
     * projection, onde a causa ficaria escondida em NPE de chave composta ou
     * filtro de DFU.</p>
     */
    private List<ConfiguredView> getConfiguredViewsCoberturaCommunity(
            List<ConfiguredView> configuredViewsCobertura) {

        if (configuredViewsCobertura == null) {
            throw new IllegalArgumentException(
                    "Configured view collection is required for configured view coverage");
        }

        int indiceConfiguredView = 0;
        for (ConfiguredView configuredView : configuredViewsCobertura) {
            if (configuredView == null) {
                throw new IllegalArgumentException(
                        "Configured view at index "
                                + indiceConfiguredView
                                + " is required for configured view coverage");
            }
            indiceConfiguredView++;
        }

        return configuredViewsCobertura;

    }
    /**
     * Valida o resultado devolvido pela factory antes do service consumir campos
     * mutaveis da projection.
     *
     * <p>A factory Community deve sempre devolver uma `ConfiguredViewProjection`
     * com `FiltroDFUProjection` populada. Se isso nao acontecer, o problema e
     * estrutural do snapshot/factory, nao uma DFU fora da view nem uma feature
     * Enterprise bloqueada. Falhar aqui evita NPEs ao inicializar selecoes,
     * percorrer DFUs ou montar a cobertura de views.</p>
     */
    
}
