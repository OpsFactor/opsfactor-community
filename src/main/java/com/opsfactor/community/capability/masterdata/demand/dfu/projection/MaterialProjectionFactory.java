package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory de escopos de materiais usados por projections em memoria.
 *
 * <p>Além dos subconjuntos técnicos, esta classe é a dona compartilhada da
 * semântica pública de seleção por ids e características: ids explícitos são
 * intersectados com as características, características diferentes usam AND
 * e valores da mesma característica usam OR.</p>
 */
public class MaterialProjectionFactory {

    /** Compatibilidade temporária com o payload antigo de Production Overview. */
    private static final String MATERIAL_ID_CHARACTERISTIC_ID = "materialId";

    // não se criam instâncias desta classe
    private MaterialProjectionFactory() {
    }
    
    public static MaterialProjection getProjectionUnicoMaterial(Produto material, ClusterEParametrosProjection clusterEParametrosProjection) {

        validaMaterial(material, "MaterialProjectionFactory received null material for single-material projection.");

        MaterialProjection materialProjection = new MaterialProjection();
        materialProjection.setMateriais = Collections.unmodifiableSet(Set.of(material));
        materialProjection.clusterEParametrosProjection = clusterEParametrosProjection;
        
        return materialProjection;
        
    }

    public static MaterialProjection getProjectionDeDfus(Collection<DFU> dfus, ClusterEParametrosProjection clusterEParametrosProjection) {

        validaDfus(dfus);

        Set<Produto> materiais = dfus.stream().map(DFU::getProduto).collect(Collectors.toSet());
        return getProjectionSetMateriais(materiais, clusterEParametrosProjection);
    }

    public static MaterialProjection getProjectionSetMateriais(Collection<Produto> materiais, ClusterEParametrosProjection clusterEParametrosProjection) {

        validaMateriais(materiais);

        MaterialProjection materialProjection = new MaterialProjection();
        materialProjection.setMateriais = Collections.unmodifiableSet(new HashSet(materiais));
        materialProjection.clusterEParametrosProjection = clusterEParametrosProjection;

        return materialProjection;
        
    }

    public static MaterialProjection getProjectionClusterMateriais(ClusterProdutos clusterMateriais, ClusterEParametrosProjection clusterEParametrosProjection, boolean somenteMateriaisAtivos) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);
        if (clusterMateriais == null) {
            throw new IllegalArgumentException("MaterialProjectionFactory received null material cluster.");
        }

        MaterialProjection materialProjection = new MaterialProjection();
        materialProjection.setMateriais = Collections.unmodifiableSet(clusterEParametrosProjection.getMateriaisDeClusterProdutos(clusterMateriais, somenteMateriaisAtivos));
        materialProjection.clusterEParametrosProjection = clusterEParametrosProjection;

        return materialProjection;

    }

    public static MaterialProjectionCompleto getMaterialProjectionCompleto(ClusterEParametrosProjection clusterEParametrosProjection) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);

        MaterialProjectionCompleto materialProjectionCompleto = new MaterialProjectionCompleto();
        materialProjectionCompleto.setMateriais = Collections.unmodifiableSet(clusterEParametrosProjection.getMateriaisAtivos());
        materialProjectionCompleto.clusterEParametrosProjection = clusterEParametrosProjection;

        return materialProjectionCompleto;
    }

    /**
     * Mantém compatibilidade com o adapter antigo que representa material ids
     * como a pseudo-característica {@code materialId}.
     *
     * <p>Novos fluxos devem usar
     * {@link #getMaterialProjectionFiltroCombinacoesCaracteristicasIds(Map, Collection, ClusterEParametrosProjection, boolean)},
     * que recebe ids e características em dimensões separadas.</p>
     */
    public static MaterialProjection getProjectionByMaterialCharacteristicValues(
            Map<String, ? extends Collection<String>> valuesByMaterialCharacteristicId,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean activeMaterialsOnly) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);
        validateCommunityMaterialCharacteristicFilters(valuesByMaterialCharacteristicId);
        Set<Produto> sourceMaterials = clusterEParametrosProjection.getMateriais(activeMaterialsOnly);
        Set<String> requestedMaterialIds = getRequestedMaterialIds(valuesByMaterialCharacteristicId);

        if (requestedMaterialIds.isEmpty()) {
            return getProjectionSetMateriais(sourceMaterials, clusterEParametrosProjection);
        }

        Set<Produto> filteredMaterials = sourceMaterials.stream()
                .filter(material -> requestedMaterialIds.stream()
                        .anyMatch(requestedId -> requestedId.equalsIgnoreCase(material.getId())))
                .collect(Collectors.toSet());
        return getProjectionSetMateriais(filteredMaterials, clusterEParametrosProjection);

    }

    /**
     * Converte ids públicos de características e materiais no recorte
     * canônico sobre o snapshot central.
     *
     * <p>O método recupera o contrato do legado sem realizar consultas por
     * item: características, seus valores e materiais já estão indexados no
     * {@link ClusterEParametrosProjection}.</p>
     */
    public static MaterialProjection getMaterialProjectionFiltroCombinacoesCaracteristicasIds(
            Map<String, ? extends Collection<String>> valuesByMaterialCharacteristicId,
            Collection<String> materialIds,
            ClusterEParametrosProjection clusterEParametrosProjection,
            boolean activeMaterialsOnly) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);

        return getMaterialProjectionFiltroCombinacoesCaracteristicasIds(
                valuesByMaterialCharacteristicId,
                materialIds,
                clusterEParametrosProjection.getMateriais(activeMaterialsOnly),
                clusterEParametrosProjection);

    }

    /**
     * Aplica a mesma semântica canônica sobre candidatos previamente
     * restringidos pelo caller.
     *
     * <p>Este overload permite ao Enterprise compor filtros privados ou salvos
     * depois do recorte físico comum, sem duplicar a resolução de ids e
     * características.</p>
     */
    public static MaterialProjection getMaterialProjectionFiltroCombinacoesCaracteristicasIds(
            Map<String, ? extends Collection<String>> valuesByMaterialCharacteristicId,
            Collection<String> materialIds,
            Collection<Produto> candidateMaterials,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        validaClusterEParametrosProjection(clusterEParametrosProjection);
        validaMateriais(candidateMaterials);

        Map<CaracteristicaProduto, Set<String>> valuesByMaterialCharacteristic =
                resolveMaterialCharacteristicValues(
                        valuesByMaterialCharacteristicId,
                        clusterEParametrosProjection);
        Set<Produto> filteredMaterials = new LinkedHashSet<>(candidateMaterials);

        if (materialIds != null && !materialIds.isEmpty()) {
            Set<Produto> explicitlySelectedMaterials = materialIds.stream()
                    .map(clusterEParametrosProjection::getMaterialPersistido)
                    .collect(Collectors.toSet());
            filteredMaterials.retainAll(explicitlySelectedMaterials);
        }

        if (!valuesByMaterialCharacteristic.isEmpty()) {
            filteredMaterials.removeIf(material ->
                    !matchesAllMaterialCharacteristics(
                            material,
                            valuesByMaterialCharacteristic));
        }

        return getProjectionSetMateriais(filteredMaterials, clusterEParametrosProjection);

    }
    
    public static MaterialProjection getMaterialProjectionDePerfilExecucaoSupplyPlan(
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalArgumentException("Supply Planning execution profile is required for material projection.");
        }
        validaClusterEParametrosProjection(clusterEParametrosProjection);

        /*
         * Community nao possui Material Filter configuravel no perfil de execucao de
         * Supply Planning. A assinatura permanece recebendo o perfil para
         * manter o contrato dos callers, mas o escopo material e sempre o
         * conjunto completo de materiais ativos carregados no projection base.
         */
        MaterialProjection materialProjection = getMaterialProjectionCompleto(clusterEParametrosProjection);
        materialProjection.clusterEParametrosProjection = clusterEParametrosProjection;
        
        return materialProjection;
        
    }

    /**
     * Valida a projection base que acompanha todo escopo de materiais.
     */
    private static void validaClusterEParametrosProjection(
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (clusterEParametrosProjection == null) {
            throw new IllegalArgumentException("Cluster/parameter projection is required for material projection.");
        }

    }

    /**
     * Valida uma colecao explicita de materiais antes de publica-la como set
     * imutavel.
     *
     * <p>Colecao vazia e valida: representa um recorte tecnico sem materiais.
     * Colecao nula ou item nulo indica snapshot quebrado de DFU/filtro tecnico
     * e deve falhar antes de virar NPE em calculo paralelo.</p>
     */
    private static void validaMateriais(Collection<Produto> materiais) {

        if (materiais == null) {
            throw new IllegalArgumentException("MaterialProjectionFactory received null material collection.");
        }

        int indice = 0;
        for (Produto material : materiais) {
            validaMaterial(
                    material,
                    "MaterialProjectionFactory received null material at index " + indice + ".");
            indice++;
        }

    }

    /**
     * Valida DFUs antes de extrair seus materiais.
     */
    private static void validaDfus(Collection<DFU> dfus) {

        if (dfus == null) {
            throw new IllegalArgumentException("MaterialProjectionFactory received null DFU collection.");
        }

        int indice = 0;
        for (DFU dfu : dfus) {
            if (dfu == null) {
                throw new IllegalArgumentException("MaterialProjectionFactory received null DFU at index " + indice + ".");
            }
            validaMaterial(
                    dfu.getProduto(),
                    "MaterialProjectionFactory received DFU without material at index " + indice + ".");
            indice++;
        }

    }

    /** Valida o payload de compatibilidade da pseudo-característica materialId. */
    private static void validateCommunityMaterialCharacteristicFilters(
            Map<String, ? extends Collection<String>> valuesByMaterialCharacteristicId) {

        if (valuesByMaterialCharacteristicId == null) {
            return;
        }
        for (Map.Entry<String, ? extends Collection<String>> entry :
                valuesByMaterialCharacteristicId.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException(
                        "Community material characteristic id is required when provided.");
            }
            if (!MATERIAL_ID_CHARACTERISTIC_ID.equals(entry.getKey())) {
                throw new IllegalArgumentException(
                        "Community supports only the materialId material characteristic filter.");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "Community material characteristic values must not be null.");
            }
        }

    }

    /** Achata os ids técnicos não vazios, preservando a comparação case-insensitive do legado. */
    private static Set<String> getRequestedMaterialIds(
            Map<String, ? extends Collection<String>> valuesByMaterialCharacteristicId) {

        if (valuesByMaterialCharacteristicId == null) {
            return Set.of();
        }
        Collection<String> materialIds = valuesByMaterialCharacteristicId.get(
                MATERIAL_ID_CHARACTERISTIC_ID);
        if (materialIds == null || materialIds.isEmpty()) {
            return Set.of();
        }
        return materialIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

    }

    /**
     * Resolve ids de características no mapa indexado do snapshot e descarta
     * somente dimensões com seleção vazia, que não restringem o escopo.
     */
    private static Map<CaracteristicaProduto, Set<String>> resolveMaterialCharacteristicValues(
            Map<String, ? extends Collection<String>> valuesByMaterialCharacteristicId,
            ClusterEParametrosProjection clusterEParametrosProjection) {

        if (valuesByMaterialCharacteristicId == null) {
            return Map.of();
        }

        Map<CaracteristicaProduto, Set<String>> resolvedValues = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Collection<String>> entry :
                valuesByMaterialCharacteristicId.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "Material characteristic values must not be null.");
            }
            if (entry.getValue().isEmpty()) {
                continue;
            }

            CaracteristicaProduto materialCharacteristic =
                    clusterEParametrosProjection.getCaracteristicaProdutoDeId(entry.getKey());
            Set<String> selectedValues = new LinkedHashSet<>();
            for (String selectedValue : entry.getValue()) {
                if (selectedValue == null) {
                    throw new IllegalArgumentException(
                            "Material characteristic values must not contain null.");
                }
                selectedValues.add(selectedValue);
            }
            resolvedValues.put(materialCharacteristic, selectedValues);
        }

        return Collections.unmodifiableMap(resolvedValues);

    }

    /**
     * Implementa AND entre características e OR entre os valores selecionados
     * da mesma característica, com comparação case-insensitive do legado.
     */
    private static boolean matchesAllMaterialCharacteristics(
            Produto material,
            Map<CaracteristicaProduto, Set<String>> valuesByMaterialCharacteristic) {

        return valuesByMaterialCharacteristic.entrySet().stream()
                .allMatch(entry -> entry.getKey()
                        .findValorCaracteristicaDeProduto(material)
                        .map(configuredValue -> entry.getValue().stream()
                                .anyMatch(selectedValue ->
                                        selectedValue.equalsIgnoreCase(configuredValue)))
                        .orElse(false));

    }

    private static void validaMaterial(
            Produto material,
            String mensagemErro) {

        if (material == null) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

}
