package com.opsfactor.community.capability.planningbook.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.FiltroDFUProjection;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Escopo material/location de uma linha ou selecao do Planning Book Community.
 *
 * <p>O Community nao possui agrupamento visual por caracteristicas nem filtros
 * dinamicos de DFU. Esta classe substitui o envelope legado de agrupamento por
 * caracteristicas no fluxo Community e deixa explicito que a unidade de
 * materializacao da resposta e sempre material/location. O escopo ainda pode
 * conter varias DFUs quando uma operacao funcional autorizada precisar
 * processa-las em conjunto.</p>
 *
 * <p>Um escopo pode representar todos os DFUs filtrados de uma view, usado na
 * montagem inicial da grade, ou exatamente um material/location, usado quando
 * o front envia uma celula alterada. Em ambos os casos a intersecao final com
 * as combinacoes ativas continua sendo feita por {@link FiltroDFUProjection}.</p>
 */
@Getter
@EqualsAndHashCode
public class PlanningBookDfuScope {

    private final Set<Produto> materiais;
    private final Set<Location> locations;

    private PlanningBookDfuScope(
            Set<Produto> materiais,
            Set<Location> locations) {

        validaMateriais(materiais);
        validaLocations(locations);
        this.materiais = Collections.unmodifiableSet(new HashSet<>(materiais));
        this.locations = Collections.unmodifiableSet(new HashSet<>(locations));

    }

    /**
     * Cria o escopo completo da view Community, depois da aplicacao dos filtros
     * simples de material/location e dos filtros ad-hoc internos do fluxo.
     */
    public static PlanningBookDfuScope deMateriaisELocations(
            Set<Produto> materiais,
            Set<Location> locations) {

        return new PlanningBookDfuScope(materiais, locations);

    }

    /**
     * Cria o escopo de uma celula selecionada no Planning Book Community.
     * Payloads sem uma das dimensoes sao bloqueados antes desta chamada.
     */
    public static PlanningBookDfuScope deMaterialLocation(
            Produto material,
            Location location) {

        return new PlanningBookDfuScope(
                Collections.singleton(material),
                Collections.singleton(location));

    }

    /**
     * Material unico do escopo, quando ele representa uma linha DFU. Retorna
     * {@code null} para escopos maiores usados apenas na montagem da grade.
     */
    public Produto getMaterialUnicoOuNulo() {

        return materiais.size() == 1 ? materiais.iterator().next() : null;

    }

    /**
     * Location unica do escopo, quando ele representa uma linha DFU. Retorna
     * {@code null} para escopos maiores usados apenas na montagem da grade.
     */
    public Location getLocationUnicaOuNula() {

        return locations.size() == 1 ? locations.iterator().next() : null;

    }

    /**
     * Converte o escopo em projection de DFU, preservando apenas combinacoes
     * ja autorizadas pela view e pelas regras de DFU ativa.
     */
    public FiltroDFUProjection getNovoDFUProjectionInterseccaoComProjectionFiltro(
            FiltroDFUProjection dfuProjectionFiltro,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (dfuProjectionFiltro == null) {
            throw new IllegalArgumentException(
                    "PlanningBookDfuScope requires DFU filter projection to create an intersection projection.");
        }
        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException(
                    "PlanningBookDfuScope requires cluster/parameter projection to create an intersection projection.");
        }

        FiltroDFUProjection novoDFUProjection = new FiltroDFUProjection(
                dfuProjectionFiltro.getTodasCombinacoesLocationMaterialHabilitadas(),
                clusterEParametrosProjection);

        for (Location location : locations) {
            for (Produto material : materiais) {
                if (dfuProjectionFiltro.contemCombinacaoLocationMaterial(location, material)) {
                    novoDFUProjection.addDFU(location, material);
                }
            }
        }

        return novoDFUProjection;

    }

    /**
     * Retorna o produto cartesiano material/location do escopo. A filtragem de
     * combinacoes ativas deve ser feita antes quando isso importar para o fluxo.
     */
    public List<DFU> getDfusEscopo() {

        return locations.stream()
                .flatMap(location -> materiais.stream()
                        .map(material -> new DFU(material, location)))
                .collect(Collectors.toList());

    }

    public boolean contemMaterialLocation(
            Produto material,
            Location location) {

        validaMaterial(material, "PlanningBookDfuScope material is required to test scope membership.");
        validaLocation(location, "PlanningBookDfuScope location is required to test scope membership.");
        return materiais.contains(material) && locations.contains(location);

    }

    /**
     * Colunas de material esperadas no DTO do Planning Book para um escopo de
     * linha DFU. Escopos com varios materiais retornam mapa vazio porque nao
     * representam uma linha editavel Community.
     */
    public Map<String, String> getColunasMaterialPlanningBook() {

        Produto material = getMaterialUnicoOuNulo();
        if (material == null) {
            return Collections.emptyMap();
        }
        validaMaterial(material, "PlanningBookDfuScope material is required to create Planning Book material columns.");

        return Map.of(
                "materialId", material.getId(),
                "materialDescription", material.getDescricao());

    }

    /**
     * Colunas de location esperadas no DTO do Planning Book para um escopo de
     * linha DFU. Escopos com varias locations retornam mapa vazio porque nao
     * representam uma linha editavel Community.
     */
    public Map<String, String> getColunasLocationPlanningBook() {

        Location location = getLocationUnicaOuNula();
        if (location == null) {
            return Collections.emptyMap();
        }
        validaLocation(location, "PlanningBookDfuScope location is required to create Planning Book location columns.");

        return Map.of(
                "locationId", location.getId(),
                "locationDescription", location.getDescricao());

    }

    public String getDescricaoEscopo() {

        String descricaoMateriais = materiais.stream()
                .map(Produto::getId)
                .sorted()
                .collect(Collectors.joining(","));
        String descricaoLocations = locations.stream()
                .map(Location::getId)
                .sorted()
                .collect(Collectors.joining(","));

        return "materials=[" + descricaoMateriais + "], locations=[" + descricaoLocations + "]";

    }

    /**
     * Valida o escopo de materiais do Planning Book Community.
     *
     * <p>Escopo vazio e valido para views sem DFUs apos filtro. Colecao nula,
     * item nulo ou material sem id indicam snapshot/payload quebrado e devem
     * falhar antes de montar linhas, DTOs ou projections derivadas.</p>
     */
    private static void validaMateriais(Set<Produto> materiais) {

        if (materiais == null) {
            throw new IllegalArgumentException("PlanningBookDfuScope material set is required.");
        }

        int index = 0;
        for (Produto material : materiais) {
            validaMaterial(
                    material,
                    "PlanningBookDfuScope material set contains null material at index " + index + ".");
            index++;
        }

    }

    /**
     * Valida o escopo de locations do Planning Book Community.
     */
    private static void validaLocations(Set<Location> locations) {

        if (locations == null) {
            throw new IllegalArgumentException("PlanningBookDfuScope location set is required.");
        }

        int index = 0;
        for (Location location : locations) {
            validaLocation(
                    location,
                    "PlanningBookDfuScope location set contains null location at index " + index + ".");
            index++;
        }

    }

    private static void validaMaterial(
            Produto material,
            String message) {

        if (material == null) {
            throw new IllegalArgumentException(message);
        }
        if (material.getId() == null || material.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "PlanningBookDfuScope material id is required.");
        }

    }

    private static void validaLocation(
            Location location,
            String message) {

        if (location == null) {
            throw new IllegalArgumentException(message);
        }
        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "PlanningBookDfuScope location id is required.");
        }

    }

}
