package com.opsfactor.community.capability.demandplanning.configuration.projection.factory;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosDemandPlanNivelCluster;
import com.opsfactor.community.capability.demandplanning.configuration.domain.PerfilExecucaoDemandPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosDemandPlanProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosForecastProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.ParametrosGeraisDemandPlanningProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjection;
import com.opsfactor.community.capability.demandplanning.configuration.projection.aggregation.ParametrosDemandPlanNivelClusterProjectionSimples;
import com.opsfactor.community.capability.demandplanning.configuration.repository.ParametrosDemandPlanNivelClusterRepository;
import com.opsfactor.community.capability.demandplanning.configuration.repository.PerfilExecucaoDemandPlanRepository;
import com.opsfactor.community.platform.utility.FuncoesMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.NoResultException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory Community dos parametros de Demand Planning.
 *
 * <p>A edicao Community materializa apenas parametros manuais de cluster
 * material/location. Auto-fit, regression tree, parametros por node e qualquer
 * configuracao derivada de modelos Enterprise devem ser adicionados por uma
 * factory Enterprise {@code @Primary}, sem ampliar as dependencias deste modulo
 * aberto.</p>
 */
@Component
public class ParametrosDemandPlanningProjectionFactory {

    /**
     * Projection central de parametros globais, clusters, materiais e
     * locations ativos. O Community usa essa base para cruzar todos os pares
     * cluster location x cluster material.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Repository do perfil de execucao Demand Planning usado para resolver
     * chamadas por id e manter a factory independente da camada web/service.
     */
    @Autowired
    private PerfilExecucaoDemandPlanRepository perfilExecucaoDemandPlanRepository;

    /**
     * Repository dos parametros manuais por cluster location/material. No
     * Community esta e a unica fonte de parametrizacao granular de Demand
     * Planning; auto-fit e regression tree pertencem ao Enterprise.
     */
    @Autowired
    private ParametrosDemandPlanNivelClusterRepository parametrosDemandPlanNivelClusterRepository;

    /**
     * Materializa a projection completa de parametros de Demand Planning para
     * o perfil informado.
     */
    public ParametrosDemandPlanProjection getParametrosDemandPlanProjection(PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) {

        validaPerfilExecucaoDemandPlan(perfilExecucaoDemandPlan);

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();
        validaClusterEParametrosProjection(clusterEParametrosProjection);

        // id cluster locations -> id cluster materiais -> (se existir) parametrosDemandPlanNivelCluster
        Map<Long, Map<Long, ParametrosDemandPlanNivelCluster>> mapaParametrosDemandPlanNivelCluster =
                getMapaParametrosDemandPlanNivelClusterValidado(perfilExecucaoDemandPlan);

        ParametrosDemandPlanProjection parametrosDemandPlanProjection = ParametrosDemandPlanProjection.builder()
                .parametrosGlobais(clusterEParametrosProjection.getParametrosGlobais())
                .perfilExecucaoDemandPlan(perfilExecucaoDemandPlan)
                        .mapaParametrosPorClusterLocationsClusterMateriais(clusterEParametrosProjection
                        .getClusterLocationsList()
                        .parallelStream()
                        .flatMap(clusterLocations -> clusterEParametrosProjection.getClusterMateriaisDemandPlanningList()
                                .stream()
                                .map(clusterMateriaisDemandPlanning -> getParametrosDemandPlanNivelClusterProjection(perfilExecucaoDemandPlan, clusterLocations, clusterMateriaisDemandPlanning, mapaParametrosDemandPlanNivelCluster)))
                        .collect(Collectors.groupingBy(
                                ParametrosDemandPlanNivelClusterProjection::getClusterLocations,
                                Collectors.toMap(
                                        ParametrosDemandPlanNivelClusterProjection::getClusterMateriaisDemandPlanning,
                                        Function.identity())
                        ))
                )
                .build();

        return enriqueceParametrosDemandPlanProjection(
                parametrosDemandPlanProjection);

    }

    /**
     * Permite que overlays especializados substituam, de forma integral e
     * previamente materializada, os parametros efetivos de cada combinacao de
     * clusters do perfil.
     *
     * <p>O Community devolve a projection manual sem alteracao. O hook e
     * chamado uma unica vez por perfil, depois da leitura em lote dos
     * parametros manuais e antes de qualquer rodada paralela de Demand
     * Planning. Isso permite que uma edicao privada carregue suas selecoes em
     * lote e devolva projections imutaveis por cluster, sem acoplar o modelo
     * Community a entidades privadas nem introduzir consultas por DFU.</p>
     *
     * @param parametrosDemandPlanProjection projection Community ja completa
     *                                         para o perfil solicitado
     * @return projection efetiva a ser consumida pela execucao
     */
    protected ParametrosDemandPlanProjection enriqueceParametrosDemandPlanProjection(
            ParametrosDemandPlanProjection parametrosDemandPlanProjection) {

        return parametrosDemandPlanProjection;

    }

    /**
     * Valida o perfil antes de qualquer acesso a repository ou projection
     * central.
     *
     * <p>A factory e chamada por services e por rotinas de forecast paralelas.
     * Falhar aqui com uma mensagem funcional evita NPE tardio em threads de
     * cluster quando a chamada chega sem o perfil ou sem a chave do perfil.</p>
     */
    private void validaPerfilExecucaoDemandPlan(PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) {

        if (perfilExecucaoDemandPlan == null) {
            throw new IllegalArgumentException("Demand Planning execution profile is required.");
        }
        if (perfilExecucaoDemandPlan.getId() == null || perfilExecucaoDemandPlan.getId().isBlank()) {
            throw new IllegalArgumentException("Demand Planning execution profile id is required.");
        }

    }

    /**
     * Valida a fotografia central antes de gerar o produto cartesiano de
     * clusters location/material.
     *
     * <p>Lista vazia e no-op valido: nenhuma projection granular sera criada.
     * Lista nula, item nulo, id ausente ou id duplicado indicam snapshot
     * estrutural quebrado da projection central e devem falhar antes do
     * {@code parallelStream}, onde a excecao ficaria menos diagnostica.</p>
     */
    private void validaClusterEParametrosProjection(ClusterEParametrosProjection clusterEParametrosProjection) {

        if (clusterEParametrosProjection == null) {
            throw new IllegalStateException("Cluster/parameter projection returned null for Demand Planning parameters.");
        }
        if (clusterEParametrosProjection.getParametrosGlobais() == null) {
            throw new IllegalStateException("Cluster/parameter projection returned null global parameters for Demand Planning parameters.");
        }

        validaClusterLocationsList(
                clusterEParametrosProjection.getClusterLocationsList(),
                "Demand Planning cluster location list");
        validaClusterMateriaisDemandPlanningList(
                clusterEParametrosProjection.getClusterMateriaisDemandPlanningList(),
                "Demand Planning material cluster list");

    }

    /**
     * Materializa o mapa de parametros persistidos por cluster location/material
     * depois de validar a fotografia vinda do repository.
     *
     * <p>O registro pode nao existir para uma combinacao; nesse caso a factory
     * cria uma entidade default mais abaixo. O que nao pode acontecer e o
     * repository devolver entidade nula, cluster nulo, id ausente, perfil
     * diferente ou duas linhas para a mesma combinacao tecnica. O retorno em
     * {@link List} preserva a cardinalidade do snapshot ate esta validacao,
     * evitando que a estrutura do repository esconda duplicidades antes do
     * mapa funcional.</p>
     */
    private Map<Long, Map<Long, ParametrosDemandPlanNivelCluster>> getMapaParametrosDemandPlanNivelClusterValidado(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) {

        List<ParametrosDemandPlanNivelCluster> parametrosDemandPlanNivelClusterList =
                parametrosDemandPlanNivelClusterRepository.findByPerfilExecucaoDemandPlanId(
                        perfilExecucaoDemandPlan.getId());

        if (parametrosDemandPlanNivelClusterList == null) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned null collection for execution profile "
                            + perfilExecucaoDemandPlan.getId() + ".");
        }

        Map<Long, Map<Long, ParametrosDemandPlanNivelCluster>> mapaParametrosDemandPlanNivelCluster =
                new HashMap<>();
        Set<String> chavesClusterJaCarregadas = new HashSet<>();
        int indice = 0;
        for (ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster : parametrosDemandPlanNivelClusterList) {
            validaParametrosDemandPlanNivelCluster(
                    parametrosDemandPlanNivelCluster,
                    perfilExecucaoDemandPlan,
                    indice,
                    chavesClusterJaCarregadas);

            Long clusterLocationsId = parametrosDemandPlanNivelCluster.getClusterLocations().getId();
            Long clusterMateriaisDemandPlanningId =
                    parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning().getId();
            mapaParametrosDemandPlanNivelCluster
                    .computeIfAbsent(clusterLocationsId, ignored -> new HashMap<>())
                    .put(clusterMateriaisDemandPlanningId, parametrosDemandPlanNivelCluster);
            indice++;
        }

        return mapaParametrosDemandPlanNivelCluster;

    }

    /**
     * Valida uma linha persistida de parametros por cluster antes de indexa-la.
     */
    private void validaParametrosDemandPlanNivelCluster(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster,
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            int indice,
            Set<String> chavesClusterJaCarregadas) {

        if (parametrosDemandPlanNivelCluster == null) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned null item at index "
                            + indice + ".");
        }
        if (parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan() == null
                || parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId() == null
                || parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId().isBlank()) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned item without execution profile id at index "
                            + indice + ".");
        }
        if (!perfilExecucaoDemandPlan.getId()
                .equals(parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId())) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned item for execution profile "
                            + parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan().getId()
                            + " while loading " + perfilExecucaoDemandPlan.getId() + ".");
        }
        if (parametrosDemandPlanNivelCluster.getClusterLocations() == null
                || parametrosDemandPlanNivelCluster.getClusterLocations().getId() == null) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned item without cluster location id at index "
                            + indice + ".");
        }
        if (parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning() == null
                || parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning().getId() == null) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned item without material cluster id at index "
                            + indice + ".");
        }

        String chaveCluster = parametrosDemandPlanNivelCluster.getClusterLocations().getId()
                + "|"
                + parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning().getId();
        if (!chavesClusterJaCarregadas.add(chaveCluster)) {
            throw new IllegalStateException(
                    "Demand Planning cluster-level parameter repository returned duplicated cluster key "
                            + chaveCluster + " for execution profile " + perfilExecucaoDemandPlan.getId() + ".");
        }

    }

    /**
     * Valida a lista de clusters de locations usada para montar todas as
     * combinacoes possiveis de parametros de forecast.
     */
    private void validaClusterLocationsList(
            List<ClusterLocations> clusterLocationsList,
            String snapshotName) {

        if (clusterLocationsList == null) {
            throw new IllegalStateException(snapshotName + " returned null collection.");
        }

        Set<Long> idsCarregados = new HashSet<>();
        for (int indice = 0; indice < clusterLocationsList.size(); indice++) {
            ClusterLocations clusterLocations = clusterLocationsList.get(indice);
            if (clusterLocations == null) {
                throw new IllegalStateException(snapshotName + " returned null item at index " + indice + ".");
            }
            if (clusterLocations.getId() == null) {
                throw new IllegalStateException(snapshotName + " returned item without id at index " + indice + ".");
            }
            if (!idsCarregados.add(clusterLocations.getId())) {
                throw new IllegalStateException(snapshotName + " returned duplicated id " + clusterLocations.getId() + ".");
            }
        }

    }

    /**
     * Valida a lista de clusters de materiais usada para montar todas as
     * combinacoes possiveis de parametros de forecast.
     */
    private void validaClusterMateriaisDemandPlanningList(
            List<ClusterProdutosDemandPlanning> clusterMateriaisDemandPlanningList,
            String snapshotName) {

        if (clusterMateriaisDemandPlanningList == null) {
            throw new IllegalStateException(snapshotName + " returned null collection.");
        }

        Set<Long> idsCarregados = new HashSet<>();
        for (int indice = 0; indice < clusterMateriaisDemandPlanningList.size(); indice++) {
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                    clusterMateriaisDemandPlanningList.get(indice);
            if (clusterMateriaisDemandPlanning == null) {
                throw new IllegalStateException(snapshotName + " returned null item at index " + indice + ".");
            }
            if (clusterMateriaisDemandPlanning.getId() == null) {
                throw new IllegalStateException(snapshotName + " returned item without id at index " + indice + ".");
            }
            if (!idsCarregados.add(clusterMateriaisDemandPlanning.getId())) {
                throw new IllegalStateException(snapshotName + " returned duplicated id "
                        + clusterMateriaisDemandPlanning.getId() + ".");
            }
        }

    }

    /**
     * Resolve o perfil por id e materializa a projection de parametros.
     */
    public ParametrosDemandPlanProjection getParametrosDemandPlanProjectionDeCache(String perfilExecucaoDemandPlanId) {
        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = perfilExecucaoDemandPlanRepository
                .findById(perfilExecucaoDemandPlanId)
                .orElseThrow(() -> new NoResultException("No execution profile found with id " + perfilExecucaoDemandPlanId));
        return getParametrosDemandPlanProjectionDeCache(perfilExecucaoDemandPlan);
    }

    /**
     * Mantem a assinatura historica de cache, mas hoje delega para a montagem
     * completa Community. O cache efetivo fica nas projections/factories
     * centrais chamadas por este metodo.
     */
    public ParametrosDemandPlanProjection getParametrosDemandPlanProjectionDeCache(PerfilExecucaoDemandPlan perfilExecucaoDemandPlan) {
        ParametrosDemandPlanProjection parametrosDemandPlanProjection = getParametrosDemandPlanProjection(perfilExecucaoDemandPlan);
        return parametrosDemandPlanProjection;
    }

    /**
     * Cria a projection de parametros para uma combinacao cluster
     * location/material, usando defaults neutros quando ainda nao ha registro
     * persistido para o perfil.
     */
    public ParametrosDemandPlanNivelClusterProjection getParametrosDemandPlanNivelClusterProjection(
            PerfilExecucaoDemandPlan perfilExecucaoDemandPlan,
            ClusterLocations clusterLocations,
            ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning,
            Map<Long, Map<Long, ParametrosDemandPlanNivelCluster>> mapaParametrosDemandPlanNivelCluster) {

        ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster = FuncoesMap.getElementoDeNestedMap(
                mapaParametrosDemandPlanNivelCluster,
                ParametrosDemandPlanNivelCluster.class,
                clusterLocations.getId(), clusterMateriaisDemandPlanning.getId())
                .orElse(new ParametrosDemandPlanNivelCluster(
                        new ParametrosDemandPlanNivelCluster.ParametrosDemandPlanNivelClusterCompositeKey(
                                perfilExecucaoDemandPlan,
                                clusterMateriaisDemandPlanning,
                                clusterLocations)));

        ParametrosDemandPlanNivelClusterProjection parametrosDemandPlanNivelClusterProjection = getParametrosDemandPlanNivelClusterProjection(
                parametrosDemandPlanNivelCluster);
        parametrosDemandPlanNivelClusterProjection.setPerfilExecucaoDemandPlan(perfilExecucaoDemandPlan);
        parametrosDemandPlanNivelClusterProjection.setUseExecutionProfileAutofitModel(
                parametrosDemandPlanNivelCluster.usesExecutionProfileAutofitModel());

        return parametrosDemandPlanNivelClusterProjection;

    }

    /**
     * Converte a entidade de parametros por cluster na projection consumida
     * pela rodada de forecast.
     */
    public ParametrosDemandPlanNivelClusterProjection getParametrosDemandPlanNivelClusterProjection(
            ParametrosDemandPlanNivelCluster parametrosDemandPlanNivelCluster) {

        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        PerfilExecucaoDemandPlan perfilExecucaoDemandPlan = parametrosDemandPlanNivelCluster.getPerfilExecucaoDemandPlan();

        ClusterLocations clusterLocations = parametrosDemandPlanNivelCluster.getClusterLocations();
        ClusterProdutosDemandPlanning clusterMateriaisDemandPlanning =
                parametrosDemandPlanNivelCluster.getClusterMateriaisDemandPlanning();

        ParametrosGlobais parametrosGlobais = clusterEParametrosProjection.getParametrosGlobais();

        /*
         * Community usa sempre parametros configurados manualmente no nivel
         * cluster. Auto-fit, arvore de regressao e parametros por node sao
         * capacidades Enterprise. Os ids MAPE ficam na projection como
         * metadados transicionais para o overlay Enterprise, mas nao sao lidos
         * por nenhum fluxo Community.
         */
        ParametrosForecastProjection parametrosForecastProjection =
                new ParametrosForecastProjection(
                        parametrosDemandPlanNivelCluster,
                        parametrosGlobais);
        parametrosForecastProjection.setNivelAgregacaoMaterialMapeId(
                perfilExecucaoDemandPlan.getNivelAgregacaoMaterialMapeId());
        parametrosForecastProjection.setNivelAgregacaoLocationMapeId(
                perfilExecucaoDemandPlan.getNivelAgregacaoLocationMapeId());

        return new ParametrosDemandPlanNivelClusterProjectionSimples(
                perfilExecucaoDemandPlan,
                clusterLocations,
                clusterMateriaisDemandPlanning,
                new ParametrosGeraisDemandPlanningProjection(
                        parametrosDemandPlanNivelCluster,
                        parametrosGlobais,
                        false),
                parametrosForecastProjection);

    }

}
