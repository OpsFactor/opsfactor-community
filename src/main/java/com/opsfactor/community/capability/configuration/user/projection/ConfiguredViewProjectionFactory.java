package com.opsfactor.community.capability.configuration.user.projection;

import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView;
import com.opsfactor.community.capability.configuration.user.domain.ConfiguredView.TipoView;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import com.opsfactor.community.capability.planningbook.domain.PlanningBookDfuScope;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureInterface;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.configuration.user.repository.ConfiguredViewRepository;
import com.opsfactor.community.capability.planningbook.keyfigure.service.KeyFigureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import jakarta.persistence.NoResultException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Factory da projection de User View usada pelo Planning Book Community.
 *
 * <p>O Community sempre trabalha no nivel material/location. Por isso esta
 * factory nao materializa filtros, colunas ou agrupamentos por caracteristicas
 * dinamicas. A selecao persistida de key figures standard e carregada depois,
 * uma vez por abertura, pela KeyFigureProjectionFactory; key figures
 * privadas/customizadas continuam pertencendo ao Enterprise.</p>
 *
 * <p>Filtros ad-hoc de DFU continuam permitidos porque sao criados pelo proprio
 * fluxo Community, por exemplo para esconder combinacoes sem venda historica.
 * Esses filtros nao sao configuracoes persistidas pelo usuario e nao dependem
 * de estruturas Enterprise.</p>
 */
@Slf4j
@Component
public class ConfiguredViewProjectionFactory {

    /**
     * Repository de views persistidas por usuario/nome/tipo. A factory usa a
     * entidade como envelope de compatibilidade; filtros e agrupamentos
     * permanecem bloqueados no Community. A selecao de key figures standard
     * e resolvida em lote na abertura do Planning Book, fora desta factory.
     */
    @Autowired
    private ConfiguredViewRepository configuredViewRepository;

    /**
     * Monta a projection de uma view ja carregada, sem filtro ad-hoc de DFU.
     */
    public ConfiguredViewProjection getConfiguredViewProjection(
            ConfiguredView configuredView, 
            ClusterEParametrosProjection clusterEParametrosProjection) {
        
        return getConfiguredViewProjection(
                configuredView,
                clusterEParametrosProjection,
                null);
        
    }
    
    /**
     * Resolve a view persistida por chave e monta a projection aplicando um
     * filtro ad-hoc de DFU criado pelo proprio fluxo Community.
     */
    public ConfiguredViewProjection getConfiguredViewProjection(
            String userId, String nomeView, TipoView tipoView, 
            ClusterEParametrosProjection clusterEParametrosProjection,
            BiFunction<Produto,Location,Boolean> filtrosAdicionaisDfus) {

        ConfiguredView configuredView = resolveConfiguredViewObrigatoria(
                userId,
                nomeView,
                tipoView);
        
        return getConfiguredViewProjection(
                configuredView,
                clusterEParametrosProjection,
                filtrosAdicionaisDfus);
        
    }
    
    /**
     * Resolve a view persistida por chave e monta a projection sem filtros
     * ad-hoc adicionais.
     */
    public ConfiguredViewProjection getConfiguredViewProjection(
            String userId, String nomeView, TipoView tipoView, 
            ClusterEParametrosProjection clusterEParametrosProjection) {

        ConfiguredView configuredView = resolveConfiguredViewObrigatoria(
                userId,
                nomeView,
                tipoView);
        
        return getConfiguredViewProjection(
                configuredView,
                clusterEParametrosProjection,
                null);
        
    }
    
    /**
     * Monta a projection operacional da view para o Planning Book Community.
     */
    public ConfiguredViewProjection getConfiguredViewProjection(
            ConfiguredView configuredView,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable BiFunction<Produto,Location,Boolean> filtrosAdicionaisDfus) {

        validaConfiguredViewProjectionInputsCommunity(
                configuredView,
                clusterEParametrosProjection);
        
        ConfiguredViewProjection configuredViewProjection = new ConfiguredViewProjection();
        configuredViewProjection.setConfiguredView(configuredView);
        configuredViewProjection.setClusterEParametrosProjection(clusterEParametrosProjection);

        /*
         * MATERIAIS E LOCATIONS FILTRADOS
         *
         * No Community, filtros persistidos por caracteristicas reais e
         * estruturas de agregacao configuraveis nao sao suportados. A view
         * parte diretamente dos materiais e locations ativos da projection
         * central e aplica apenas filtros ad-hoc de DFU criados pelo proprio
         * fluxo Community.
         */
        Set<Produto> materiaisFiltrados = new HashSet<>(clusterEParametrosProjection.getMateriaisAtivos());

        Set<Location> locationsFiltradas = new HashSet<>(clusterEParametrosProjection.getLocationsAtivas());
        
        // Aplica apenas filtros ad-hoc de DFU criados pelo proprio fluxo
        // Community, como esconder DFUs sem faturamento historico. Filtros
        // diretos por caracteristica material-location/DFU sao Enterprise e
        // nao entram na ConfiguredViewProjection Community.
        aplicaFiltrosMaterialLocationEmMateriaisFiltradosELocationsFiltradasECriaDFUProjection(
                materiaisFiltrados,
                locationsFiltradas,
                configuredViewProjection,
                clusterEParametrosProjection,
                filtrosAdicionaisDfus);
                
        /*
         * O Planning Book Community sempre abre a view no menor nivel
         * material/location. Portanto a projection carrega um unico escopo com
         * todos os materiais e locations filtrados; a abertura em linhas DFU e
         * feita depois pelo PlanningBookService.
         */
        configuredViewProjection.setPlanningBookDfuScopes(new HashSet<>());
        configuredViewProjection.getPlanningBookDfuScopes().add(PlanningBookDfuScope.deMateriaisELocations(
                materiaisFiltrados,
                locationsFiltradas));
        
        // A selecao/ordenacao persistida e carregada em lote na abertura pela
        // KeyFigureProjectionFactory. Esta lista inicialmente vazia aciona o
        // catalogo padrao caso a view nao possua uma fotografia persistida.
        configuredViewProjection.keyFiguresOrdenadasParaExibicao = new ArrayList<>();

        // ESPECÍFICO PARA DEMAND PLANNING ------------------------------------------------------------
        if (configuredView.getTipoView().equals(TipoView.DEMANDPLANNINGBOOK)) {
            configuraDemandPlanningBookProjection(configuredViewProjection);
        }
         
        return configuredViewProjection;
    }

    /**
     * Configura a Key Figure que recebe ajustes no Planning Book de Demand
     * Planning.
     *
     * <p>O Community fixa {@code Demand Adjustment}. O hook permite que o
     * overlay Enterprise resolva uma etapa de workflow a partir dos escalares
     * compartilhados da ConfiguredView, sem copiar a montagem integral da
     * projection nem criar relacao JPA para entidades privadas.</p>
     */
    protected void configuraDemandPlanningBookProjection(
            ConfiguredViewProjection configuredViewProjection) {

        KeyFigureInterface keyFigureAjusteDemandaTotal =
                KeyFigureService.getKeyFigureStandardDeKeyFigureStandardEnum(KeyFigureStandardEnum.AJUSTE_DEMANDA);
        configuredViewProjection.setKeyFigureAjusteDemandaTotal(keyFigureAjusteDemandaTotal);

    }

    /**
     * Resolve a entidade persistida de view por chave funcional.
     *
     * <p>View inexistente segue erro funcional (`NoResultException`). Retorno
     * nulo do repository no lugar de `Optional` indica contrato quebrado e deve
     * falhar antes de montar projection material/location ou aplicar filtros
     * ad-hoc.</p>
     */
    private ConfiguredView resolveConfiguredViewObrigatoria(
            String userId,
            String nomeView,
            TipoView tipoView) {

        if (tipoView == null) {
            throw new IllegalArgumentException(
                    "Configured view type is required for Planning Book projection.");
        }

        Optional<ConfiguredView> configuredViewOptional =
                configuredViewRepository.findByConfiguredViewCompositeKeyUserIdAndConfiguredViewCompositeKeyNomeViewAndConfiguredViewCompositeKeyTipoView(
                        userId,
                        nomeView,
                        tipoView);
        if (configuredViewOptional == null) {
            throw new IllegalStateException(
                    "Configured View repository returned null Optional for user "
                            + userId
                            + ", view "
                            + nomeView
                            + " and type "
                            + tipoView
                            + ".");
        }

        return configuredViewOptional.orElseThrow(() -> new NoResultException(
                "No configured view found for user "
                        + userId
                        + ", view "
                        + nomeView
                        + " for "
                        + tipoView));

    }

    /**
     * Valida os snapshots usados na materializacao da view Community.
     *
     * <p>A factory parte da projection central de parametros/master data para
     * abrir o Planning Book em material/location. Se `ConfiguredView` ou
     * `ClusterEParametrosProjection` vierem incompletos, falhamos antes de
     * acessar sets ativos, tipo da view ou key figures padrao.</p>
     */
    private void validaConfiguredViewProjectionInputsCommunity(
            ConfiguredView configuredView,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (configuredView == null) {
            throw new IllegalArgumentException(
                    "Configured view is required for Planning Book projection.");
        }
        if (configuredView.getConfiguredViewCompositeKey() == null) {
            throw new IllegalArgumentException(
                    "Configured view primary key is required for Planning Book projection.");
        }
        if (configuredView.getTipoView() == null) {
            throw new IllegalArgumentException(
                    "Configured view type is required for Planning Book projection.");
        }
        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection is required for Planning Book projection.");
        }
        if (clusterEParametrosProjection.getMaterialSet() == null) {
            throw new IllegalArgumentException(
                    "Material snapshot is required in cluster and parameters projection for Planning Book projection.");
        }
        if (clusterEParametrosProjection.getLocationSet() == null) {
            throw new IllegalArgumentException(
                    "Location snapshot is required in cluster and parameters projection for Planning Book projection.");
        }
        validaMaterialSnapshotPlanningBookCommunity(clusterEParametrosProjection.getMaterialSet());
        validaLocationSnapshotPlanningBookCommunity(clusterEParametrosProjection.getLocationSet());

    }

    /**
     * Valida a fotografia de materiais antes de aplicar filtros de ativo.
     *
     * <p>A projection central normalmente ja vem da
     * `ClusterEParametrosProjectionFactory`, que valida ids e duplicidades. A
     * factory de view, entretanto, tambem possui entry point direto para testes,
     * overlays e callers que ja tenham uma projection em maos. Esta guarda
     * evita NPE em `Produto.filtraMaterialSetAtivos(...)` e impede escopo de
     * Planning Book com chave funcional ambigua.</p>
     */
    private void validaMaterialSnapshotPlanningBookCommunity(
            Set<Produto> materiais) {

        Set<String> materialIds = new HashSet<>();
        int index = 0;
        for (Produto material : materiais) {
            if (material == null) {
                throw new IllegalArgumentException(
                        "Material at index " + index + " is required in cluster and parameters projection for Planning Book projection.");
            }
            if (material.getId() == null || material.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Material id is required in cluster and parameters projection for Planning Book projection.");
            }
            if (!materialIds.add(material.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate material id " + material.getId() + " found in cluster and parameters projection for Planning Book projection.");
            }
            index++;
        }

    }

    /**
     * Valida a fotografia de locations antes de aplicar filtros de ativo.
     */
    private void validaLocationSnapshotPlanningBookCommunity(
            Set<Location> locations) {

        Set<String> locationIds = new HashSet<>();
        int index = 0;
        for (Location location : locations) {
            if (location == null) {
                throw new IllegalArgumentException(
                        "Location at index " + index + " is required in cluster and parameters projection for Planning Book projection.");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Location id is required in cluster and parameters projection for Planning Book projection.");
            }
            if (!locationIds.add(location.getId())) {
                throw new IllegalArgumentException(
                        "Duplicate location id " + location.getId() + " found in cluster and parameters projection for Planning Book projection.");
            }
            index++;
        }

    }

    /**
     * Falha para texto nulo ou em branco antes de repository/projection.
     */
        
    private void aplicaFiltrosMaterialLocationEmMateriaisFiltradosELocationsFiltradasECriaDFUProjection(
            Set<Produto> materiaisAFiltrar,
            Set<Location> locationsAFiltrar,
            ConfiguredViewProjection configuredViewProjection,
            ClusterEParametrosProjection clusterEParametrosProjection,
            @Nullable BiFunction<Produto,Location,Boolean> filtrosAdicionaisDfus) {
        
        // Sem filtro ad-hoc, a projection pode representar todas as
        // combinacoes material/location e deixar a propria FiltroDFUProjection
        // aplicar a regra de DFU ativa quando os dados forem iterados.
        if (filtrosAdicionaisDfus == null) {
            FiltroDFUProjection dfuProjection = new FiltroDFUProjection(locationsAFiltrar, materiaisAFiltrar, clusterEParametrosProjection);
            configuredViewProjection.dfuProjectionFiltrado = dfuProjection;
            
        } else {
            FiltroDFUProjection dfuProjection = new FiltroDFUProjection(false, clusterEParametrosProjection);
            
            Set<Produto> materiaisRemanescentes = new HashSet<>();
            Set<Location> locationsRemanescentes = new HashSet<>();

            for (Produto material : materiaisAFiltrar) {
                for (Location location : locationsAFiltrar) {
                    boolean passouNosFiltros = filtrosAdicionaisDfus.apply(material, location);
                    
                    if (passouNosFiltros) {
                        materiaisRemanescentes.add(material);
                        locationsRemanescentes.add(location);
                        dfuProjection.addDFU(location, material);
                    }
                    
                    
                }
            }

            /*
             * Os sets recebidos tambem sao usados logo depois para montar os
             * agrupamentos tecnicos do Planning Book. Portanto a atualizacao
             * precisa ser feita in-place, nao apenas na variavel local, para
             * que a projection inteira reflita o filtro ad-hoc de DFU.
             */
            materiaisAFiltrar.clear();
            materiaisAFiltrar.addAll(materiaisRemanescentes);
            locationsAFiltrar.clear();
            locationsAFiltrar.addAll(locationsRemanescentes);
            configuredViewProjection.dfuProjectionFiltrado = dfuProjection;
            
        }
        
    }
        
}
