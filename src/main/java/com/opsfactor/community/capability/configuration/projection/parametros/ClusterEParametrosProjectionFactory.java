package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsPaisEstado;
import com.opsfactor.community.capability.cluster.domain.location.RegraAlocacaoClusterLocationsTipoLocation;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutosDemandPlanning;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutos;
import com.opsfactor.community.capability.cluster.domain.produto.RegraAlocacaoClusterProdutosStatus;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosDemandPlanningRepository;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsPaisEstadoRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsTipoLocationRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosStatusRepository;
import com.opsfactor.community.capability.cluster.service.ClusteringService;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import com.opsfactor.community.platform.utility.FuncoesMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory da projection central de parametros, clusters e master data basico.
 *
 * <p>O Community carrega somente material/location, parametros globais,
 * parametros material/location e regras de cluster permitidas na edicao. Regras
 * por caracteristica e filtros/agregadores dinamicos sao bloqueados e ficam no
 * overlay Enterprise.</p>
 */
@Component
public class ClusterEParametrosProjectionFactory {

    /**
     * Service dos parametros globais Community.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Repository de parametros material/location com fetch dos relacionamentos
     * usados em calculo.
     */
    @Autowired
    private ParametrosProdutoLocationRepository parametrosProdutoLocationRepository;

    /**
     * Service de materiais ativos/inativos para o snapshot.
     */
    @Autowired
    private MaterialService materialService;

    /**
     * Service de locations, excluindo a default tecnica.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Service que resolve clusters padrao de materiais.
     */
    @Autowired
    private ClusteringService clusteringService;

    /**
     * Repository dos clusters de materiais usados pelo Demand Planning.
     */
    @Autowired
    private ClusterProdutosDemandPlanningRepository clusterMateriaisDemandPlanningRepository;

    /**
     * Repository dos clusters de locations.
     */
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;

    /**
     * Valores de regra de alocacao por status de material.
     */
    @Autowired
    private RegraAlocacaoClusterProdutosStatusRepository regraAlocacaoClusterProdutosStatusRepository;

    /**
     * Valores de regra de alocacao por pais/estado de location.
     */
    @Autowired
    private RegraAlocacaoClusterLocationsPaisEstadoRepository regraAlocacaoClusterLocationsPaisEstadoRepository;

    /**
     * Valores de regra de alocacao por tipo de location.
     */
    @Autowired
    private RegraAlocacaoClusterLocationsTipoLocationRepository regraAlocacaoClusterLocationsTipoLocationRepository;

    public ClusterEParametrosProjection getParametrosProjectionBase() {

        List<Produto> materialList = materialService.getMateriais(false);
        List<Location> locationList = locationService.findAllWithoutDefault();

        return getParametrosProjectionBase(locationList, materialList);

    }

    public ClusterEParametrosProjection getParametrosProjectionBase(
            Set<Location> locationSet) {

        List<Produto> materialList = materialService.getMateriais(false);

        return getParametrosProjectionBase(locationSet, materialList);

    }

    public ClusterEParametrosProjection getParametrosProjectionBase(
            Collection<Location> locationCollection,
            Collection<Produto> materialCollection) {

        validaSnapshotComId(
                materialCollection,
                Produto::getId,
                "material");
        validaSnapshotComId(
                locationCollection,
                Location::getId,
                "location");

        /*
         * A validation acima roda antes da reducao para Set. Isso preserva a
         * cardinalidade real recebida do repository/service e permite acusar
         * ids duplicados em vez de deixar a collection deduplicar silenciosamente.
         */
        Set<Produto> materialSet = new LinkedHashSet<>(materialCollection);
        Set<Location> locationSet = new LinkedHashSet<>(locationCollection);

        ClusterEParametrosProjection clusterEParametrosProjection = new ClusterEParametrosProjection();

        // mapa pre-calculado de alocação location -> cluster (instanciado na criação do projection)
        clusterEParametrosProjection.clusterLocationsPorLocation = new HashMap<>();
        clusterEParametrosProjection.clusterProdutosDemandPlanningPorMaterial = new HashMap<>();

        // materiais e locations
        clusterEParametrosProjection.materialSet = Collections.unmodifiableSet(materialSet);
        clusterEParametrosProjection.materialMap = Collections.unmodifiableMap(materialSet.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x)));

        clusterEParametrosProjection.locationSet = Collections.unmodifiableSet(locationSet);
        clusterEParametrosProjection.locationMap = Collections.unmodifiableMap(locationSet.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x)));
        clusterEParametrosProjection.locationForProductLocationParametersMap =
                getLocationForProductLocationParametersMap(
                        clusterEParametrosProjection.locationSet,
                        clusterEParametrosProjection.locationMap);

        /*
         * Caracteristicas de material/location/material-location, filtros por
         * caracteristicas e regras de cluster por caracteristica pertencem ao
         * Enterprise. O Community mantem somente material/location como
         * dimensoes diretas e nao consulta as tabelas de caracteristicas ao
         * montar a projection central.
         */

        /*
         * Carrega os clusters de materiais de Demand Planning. Os campos
         * internos da projection ainda preservam `ClusterProdutosDemandPlanning`
         * porque a entidade JPA transicional herda de ClusterProdutos, mas a
         * factory trabalha conceitualmente com materiais.
         */
        clusterEParametrosProjection.clusterProdutosPadraoDP = clusteringService.getClusterProdutosDemandPlanningDefault();
        List<ClusterProdutosDemandPlanning> clusterProdutosDemandPlanningList =
                clusterMateriaisDemandPlanningRepository.findAll()
                        .stream()
                        .sorted(Comparator.comparing(ClusterProdutos::getPrioridade))
                        .collect(Collectors.toList());
        validaClustersMateriaisDemandPlanning(clusterProdutosDemandPlanningList);
        clusterEParametrosProjection.clusterProdutosDemandPlanningList =
                Collections.unmodifiableList(clusterProdutosDemandPlanningList);
        clusterEParametrosProjection.clusterProdutosDemandPlanningMap = clusterEParametrosProjection.clusterProdutosDemandPlanningList.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x));

        clusterEParametrosProjection.valoresStatusRegraAlocacaoClusterProdutos = regraAlocacaoClusterProdutosStatusRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(RegraAlocacaoClusterProdutosStatus::getRegraAlocacaoClusterProdutos, Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterEParametrosProjection.valoresStatusRegraAlocacaoClusterProdutos);

        // clusters de produtos já vêm com @OneToMany RegraAlocacaoClusterProdutos prepopulados
        // no entanto, cada RegraAlocacao possui dois @OneToMany que não são inicialmente populados : aqui se força a atualização pelo Hibernate
        for (ClusterProdutos clusterProdutos : clusterEParametrosProjection.getClusterProdutosDemandPlanningList()) {
            for (RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos : clusterProdutos.getRegrasAlocacaoClusterProdutos()) {
                // força a atualização dos @OneToMany a partir do banco de dados
                regraAlocacaoClusterProdutos.getRegraAlocacaoClusterProdutosStatusSet();
            }
        }

        // CARREGA LISTA DE CLUSTERS DE LOCATIONS
        List<ClusterLocations> clusterLocationsList =
                clusterLocationsRepository.customFindAll()
                        .stream()
                        .sorted(Comparator.comparingInt(ClusterLocations::getPrioridade))
                        .collect(Collectors.toList());
        validaClustersLocations(clusterLocationsList);
        clusterEParametrosProjection.clusterLocationsList =
                Collections.unmodifiableList(clusterLocationsList);
        clusterEParametrosProjection.clusterLocationsMap = clusterEParametrosProjection.clusterLocationsList.stream()
                .collect(Collectors.toMap(
                        ClusterLocations::getId,
                        Function.identity()));

        clusterEParametrosProjection.paisEstadoRegraAlocacaoClusterLocations = regraAlocacaoClusterLocationsPaisEstadoRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(RegraAlocacaoClusterLocationsPaisEstado::getRegraAlocacaoClusterLocations, Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterEParametrosProjection.paisEstadoRegraAlocacaoClusterLocations);
        clusterEParametrosProjection.tipoLocationRegraAlocacaoClusterLocations = regraAlocacaoClusterLocationsTipoLocationRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(RegraAlocacaoClusterLocationsTipoLocation::getRegraAlocacaoClusterLocations, Collectors.toSet()));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterEParametrosProjection.tipoLocationRegraAlocacaoClusterLocations);

        // popula parametros material-location e parametros globais, que sao usados em todos os processos
        clusterEParametrosProjection.parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();

        clusterEParametrosProjection.mapaParametrosProdutoLocation = new HashMap<>();
        List<ParametrosProdutoLocation> parametrosProdutoLocationList =
                parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne();
        validaParametrosProdutoLocation(parametrosProdutoLocationList);
        clusterEParametrosProjection.mapaParametrosProdutoLocation = parametrosProdutoLocationList
                .stream()
                .collect(Collectors.groupingBy(ParametrosProdutoLocation::getLocation,
                        Collectors.toMap(ParametrosProdutoLocation::getProduto, Function.identity())));
        FuncoesMap.convertToNestedUnmodifiableMap(clusterEParametrosProjection.mapaParametrosProdutoLocation);

        // Pré-calcula a alocação material -> cluster DP para eliminar recálculo em loops críticos.
        // A regra de status é avaliada no nível global do material (sem material/location).
        clusterEParametrosProjection.clusterProdutosDemandPlanningPorMaterial = Collections.unmodifiableMap(
                clusterEParametrosProjection.getMaterialSet().stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                clusterEParametrosProjection::getClusterProdutosDemandPlanningSemCache)));

        // Pré-calcula a alocação location -> cluster para eliminar recálculo em loops críticos.
        clusterEParametrosProjection.clusterLocationsPorLocation = Collections.unmodifiableMap(
                clusterEParametrosProjection.getLocationSet().stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                clusterEParametrosProjection::getClusterLocationsDeLocationSemCache)));

        return clusterEParametrosProjection;
    }

    /**
     * Materializa a referencia direta de parametros material/location no
     * snapshot central.
     *
     * <p>O repository que abastece a factory faz fetch da associacao to-one;
     * aqui apenas a convertemos para a instancia canonica do snapshot. A
     * referencia precisa pertencer ao mesmo snapshot, pois buscar uma location
     * por item neste ponto introduziria N+1 e deixaria a fotografia de dados
     * incompleta. Nao ha recursao: cada location pode apontar somente para a
     * propria instancia ou para um unico alvo direto.</p>
     */
    private Map<Location, Location> getLocationForProductLocationParametersMap(
            Collection<Location> locationCollection,
            Map<String, Location> locationMap) {

        Map<Location, Location> effectiveLocationMap = new HashMap<>();
        for (Location location : locationCollection) {
            Location referenceLocation = location.getReferenceLocationForProductLocationParameters();
            if (referenceLocation == null) {
                effectiveLocationMap.put(location, location);
                continue;
            }

            String referenceLocationId = referenceLocation.getId();
            if (referenceLocationId == null || referenceLocationId.isBlank()) {
                throw new IllegalStateException(
                        "Location "
                                + location.getId()
                                + " has a material/location parameter reference without id.");
            }

            Location referenceLocationInSnapshot = locationMap.get(referenceLocationId);
            if (referenceLocationInSnapshot == null) {
                throw new IllegalStateException(
                        "Location "
                                + location.getId()
                                + " references location "
                                + referenceLocationId
                                + " for material/location parameters, but that reference is not in the projection snapshot.");
            }
            effectiveLocationMap.put(location, referenceLocationInSnapshot);
        }

        return Collections.unmodifiableMap(effectiveLocationMap);

    }

    /**
     * Valida colecoes base de material/location antes de indexar por id.
     *
     * <p>A `ClusterEParametrosProjection` e o snapshot central consumido por
     * Demand, Supply e projections auxiliares. Um material/location sem id ou
     * duplicado transforma todos os mapas derivados em uma fotografia ambigua;
     * por isso a factory falha aqui, antes de `Collectors.toMap(...)` produzir
     * erro generico ou aceitar chave nula.</p>
     */
    private <T> void validaSnapshotComId(
            Collection<T> snapshotCollection,
            Function<T, String> idFunction,
            String itemDescription) {

        if (snapshotCollection == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection requires "
                            + itemDescription
                            + " snapshot collection.");
        }

        Set<String> itemIds = new HashSet<>();
        int indice = 0;
        for (T item : snapshotCollection) {
            if (item == null) {
                throw new IllegalArgumentException(
                        "Cluster and parameters projection "
                                + itemDescription
                                + " snapshot item at index "
                                + indice
                                + " is required.");
            }

            String itemId = idFunction.apply(item);
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException(
                        "Cluster and parameters projection "
                                + itemDescription
                                + " snapshot item at index "
                                + indice
                                + " must have id.");
            }
            if (!itemIds.add(itemId)) {
                throw new IllegalArgumentException(
                        "Cluster and parameters projection "
                                + itemDescription
                                + " snapshot has duplicated id "
                                + itemId
                                + ".");
            }
            indice++;
        }

    }

    /**
     * Valida colecoes retornadas por repositories internos do snapshot central.
     *
     * <p>Lista vazia e snapshot valido para uma base ainda sem clusters/regras
     * cadastrados. Colecao nula ou item nulo, porem, indicam quebra de contrato
     * de repository/stub e devem falhar antes de `stream()` ou `groupingBy`
     * produzir uma excecao menos contextual.</p>
     */
    

    /**
     * Valida clusters de materiais antes do mapa por id usado em Demand
     * Planning.
     *
     * <p>Repository nulo ou item nulo ja falham em
     * {@link #getRepositoryCollectionObrigatoria(Collection, String)}. Aqui a
     * factory fecha o contrato funcional do snapshot: cada cluster de material
     * precisa ter id persistido e esse id deve ser unico, pois o mapa
     * `clusterProdutosDemandPlanningMap` e a alocacao material -> cluster
     * dependem dessa chave.</p>
     */
    private void validaClustersMateriaisDemandPlanning(
            Collection<ClusterProdutosDemandPlanning> clusterProdutosDemandPlanningCollection) {

        Set<Long> clusterProdutosDemandPlanningIds = new HashSet<>();
        int indice = 0;
        for (ClusterProdutosDemandPlanning clusterProdutosDemandPlanning : clusterProdutosDemandPlanningCollection) {
            if (clusterProdutosDemandPlanning.getId() == null) {
                throw new IllegalStateException(
                        "Demand Planning material cluster repository item at index "
                                + indice
                                + " requires id for Cluster and parameters projection.");
            }
            if (!clusterProdutosDemandPlanningIds.add(clusterProdutosDemandPlanning.getId())) {
                throw new IllegalStateException(
                        "Demand Planning material cluster repository has duplicated id "
                                + clusterProdutosDemandPlanning.getId()
                                + " for Cluster and parameters projection.");
            }
            indice++;
        }

    }

    /**
     * Valida clusters de locations antes do mapa por id usado pelos processos.
     */
    private void validaClustersLocations(
            Collection<ClusterLocations> clusterLocationsCollection) {

        Set<Long> clusterLocationsIds = new HashSet<>();
        int indice = 0;
        for (ClusterLocations clusterLocations : clusterLocationsCollection) {
            if (clusterLocations.getId() == null) {
                throw new IllegalStateException(
                        "Location cluster repository item at index "
                                + indice
                                + " requires id for Cluster and parameters projection.");
            }
            if (!clusterLocationsIds.add(clusterLocations.getId())) {
                throw new IllegalStateException(
                        "Location cluster repository has duplicated id "
                                + clusterLocations.getId()
                                + " for Cluster and parameters projection.");
            }
            indice++;
        }

    }

    /**
     * Valida parametros material/location antes de montar o mapa duplo.
     *
     * <p>A entidade usa chave composta material/location. Mesmo em testes com
     * instancias distintas, duas linhas com os mesmos ids funcionais devem ser
     * tratadas como duplicidade real, porque `Collectors.toMap(...)` indexa por
     * material dentro de cada location e nao deve decidir implicitamente qual
     * configuracao vence.</p>
     */
    private void validaParametrosProdutoLocation(
            Collection<ParametrosProdutoLocation> parametrosProdutoLocationCollection) {

        Set<ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey>
                chavesParametrosProdutoLocation =
                new HashSet<>();
        int indice = 0;
        for (ParametrosProdutoLocation parametrosProdutoLocation : parametrosProdutoLocationCollection) {
            if (parametrosProdutoLocation.getParametrosProdutoLocationCompositeKey() == null
                    || parametrosProdutoLocation.getProduto() == null
                    || parametrosProdutoLocation.getLocation() == null) {
                throw new IllegalStateException(
                        "Material/location parameter repository item at index "
                                + indice
                                + " requires material and location for Cluster and parameters projection.");
            }
            if (parametrosProdutoLocation.getProduto().getId() == null
                    || parametrosProdutoLocation.getProduto().getId().isBlank()) {
                throw new IllegalStateException(
                        "Material/location parameter repository item at index "
                                + indice
                                + " requires material id for Cluster and parameters projection.");
            }
            if (parametrosProdutoLocation.getLocation().getId() == null
                    || parametrosProdutoLocation.getLocation().getId().isBlank()) {
                throw new IllegalStateException(
                        "Material/location parameter repository item at index "
                                + indice
                                + " requires location id for Cluster and parameters projection.");
            }

            ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey
                    chaveParametrosProdutoLocation =
                    parametrosProdutoLocation.getParametrosProdutoLocationCompositeKey();
            if (!chavesParametrosProdutoLocation.add(chaveParametrosProdutoLocation)) {
                throw new IllegalStateException(
                        "Material/location parameter repository has duplicated key material "
                                + parametrosProdutoLocation.getProduto().getId()
                                + " / location "
                                + parametrosProdutoLocation.getLocation().getId()
                                + " for Cluster and parameters projection.");
            }
            indice++;
        }

    }

    /**
     * Retorna o snapshot completo de clusters e parametros materializado em cache.
     *
     * <p>Essa projection e compartilhada por calculos que precisam consultar
     * parametros de produto/location sem reabrir leituras repetidas nos
     * repositories. A entrada de cache deve ser invalidada pelos fluxos que
     * alteram os cadastros ou parametros de configuracao usados no snapshot.</p>
     */
    @Cacheable(value = "clusterEParametrosProjection", sync = true)
    public ClusterEParametrosProjection getParametrosProjectionCompletoDeCache() {

        return getParametrosProjectionBase();

    }

}
