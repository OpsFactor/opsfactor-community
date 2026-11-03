package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaLocationRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
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
 * por caracteristica e filtros/agregadores dinamicos usam somente o catalogo
 * Community publicado; capacidades analiticas privadas continuam no overlay
 * Enterprise.</p>
 */
@Component
public class CadastroEParametrosProjectionFactory {

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

    /** Catálogo público de características materiais, carregado em lote no snapshot. */
    @Autowired
    private CaracteristicaMaterialRepository caracteristicaMaterialRepository;

    /** Catálogo público de características de location, carregado em lote no snapshot. */
    @Autowired
    private CaracteristicaLocationRepository caracteristicaLocationRepository;

    /**
     * Service que resolve clusters padrao de materiais.
     */

    /**
     * Repository dos clusters de materiais usados pelo Demand Planning.
     */

    /**
     * Repository dos clusters de locations.
     */

    /**
     * Valores de regra de alocacao por status de material.
     */

    /** Valores de caracteristica das regras de alocacao de material. */

    /**
     * Valores de regra de alocacao por pais/estado de location.
     */

    /**
     * Valores de regra de alocacao por tipo de location.
     */

    public CadastroEParametrosProjection getParametrosProjectionBase() {

        return getParametrosProjectionBase(locationService.findAllWithoutDefault(),
                materialService.getMateriais(false));

    }

    public CadastroEParametrosProjection getParametrosProjectionBase(Collection<Location> locationCollection) {

        return getParametrosProjectionBase(locationCollection, materialService.getMateriais(false));

    }

    /** Materializa cadastros sem introduzir qualquer identidade de classificação. */
    public CadastroEParametrosProjection getParametrosProjectionBase(
            Collection<Location> locationCollection, Collection<Produto> materialCollection) {

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

        CadastroEParametrosProjection cadastroEParametrosProjection = new CadastroEParametrosProjection();


        // materiais e locations
        cadastroEParametrosProjection.materialSet = Collections.unmodifiableSet(materialSet);
        cadastroEParametrosProjection.materialMap = Collections.unmodifiableMap(materialSet.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x)));

        cadastroEParametrosProjection.locationSet = Collections.unmodifiableSet(locationSet);
        cadastroEParametrosProjection.locationMap = Collections.unmodifiableMap(locationSet.stream()
                .collect(Collectors.toMap(x -> x.getId(), x -> x)));

        /*
         * O legado resolve filtros pelo snapshot central. Mantemos o mesmo
         * desenho: dois fetches em lote carregam catálogo, valores e respectivos
         * owners antes de qualquer loop de material/location.
         */
        cadastroEParametrosProjection.caracteristicaProdutoMap = Collections.unmodifiableMap(
                caracteristicaMaterialRepository.findAllWithValues().stream()
                        .collect(Collectors.toMap(
                                CaracteristicaProduto::getId,
                                Function.identity())));
        cadastroEParametrosProjection.caracteristicaLocationMap = Collections.unmodifiableMap(
                caracteristicaLocationRepository.findAllWithValues().stream()
                        .collect(Collectors.toMap(
                                CaracteristicaLocation::getId,
                                Function.identity())));
        cadastroEParametrosProjection.locationForProductLocationParametersMap =
                getLocationForProductLocationParametersMap(
                        cadastroEParametrosProjection.locationSet,
                        cadastroEParametrosProjection.locationMap);

        // popula parametros material-location e parametros globais, que sao usados em todos os processos
        cadastroEParametrosProjection.parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();

        cadastroEParametrosProjection.mapaParametrosProdutoLocation = new HashMap<>();
        List<ParametrosProdutoLocation> parametrosProdutoLocationList =
                parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne();
        validaParametrosProdutoLocation(parametrosProdutoLocationList);
        cadastroEParametrosProjection.mapaParametrosProdutoLocation = parametrosProdutoLocationList
                .stream()
                .collect(Collectors.groupingBy(ParametrosProdutoLocation::getLocation,
                        Collectors.toMap(ParametrosProdutoLocation::getProduto, Function.identity())));
        FuncoesMap.convertToNestedUnmodifiableMap(cadastroEParametrosProjection.mapaParametrosProdutoLocation);

        return cadastroEParametrosProjection;
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
     * <p>A `CadastroEParametrosProjection` e o snapshot central consumido por
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
     * A validação geral de coleção obrigatória já rejeita esses casos. Aqui a
     * factory fecha o contrato funcional do snapshot: cada cluster de material
     * precisa ter id persistido e esse id deve ser unico, pois o mapa
     * `clusterProdutosDemandPlanningMap` e a alocacao material -> cluster
     * dependem dessa chave.</p>
     */


    /**
     * Valida clusters de locations antes do mapa por id usado pelos processos.
     */


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
    @Cacheable(value = "cadastroEParametrosProjection", sync = true)
    public CadastroEParametrosProjection getParametrosProjectionCompletoDeCache() {

        return getParametrosProjectionBase();

    }

}
