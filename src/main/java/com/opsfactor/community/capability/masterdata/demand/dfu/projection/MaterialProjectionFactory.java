package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.cluster.domain.produto.ClusterProdutos;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory de escopos de materiais usados por projections em memoria.
 *
 * <p>A classe substituiu a antiga factory de filtros, mas no Community nao
 * reabre o conceito funcional de "Material Filter" do perfil Supply. Os
 * metodos podem montar subconjuntos tecnicos para uma rotina especifica; o
 * perfil Community sempre usa o escopo completo.</p>
 */
public class MaterialProjectionFactory {

    /** Única pseudo-característica material suportada pelo modelo Community. */
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
     * Recorta o snapshot de materiais já carregado pelo único atributo técnico
     * de material publicado no Community: {@code materialId}.
     *
     * <p>Esta operação é deliberadamente menor que o filtro dinâmico legado.
     * Características de material cadastráveis pertencem ao Enterprise e não
     * são reintroduzidas aqui. Não há repository, join ou query: a factory
     * percorre uma vez o conjunto que já está no
     * {@link ClusterEParametrosProjection} e devolve uma projection imutável.</p>
     *
     * @param valuesByMaterialCharacteristicId valores do corpo legado; listas
     *                                         vazias não restringem o escopo
     * @param clusterEParametrosProjection fotografia base em memória
     * @param activeMaterialsOnly define se a fotografia inicial inclui somente
     *                            materiais ativos
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

    /** Recusa atributos Enterprise em vez de ignorar filtro e ampliar o escopo. */
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

    private static void validaMaterial(
            Produto material,
            String mensagemErro) {

        if (material == null) {
            throw new IllegalArgumentException(mensagemErro);
        }

    }

}
