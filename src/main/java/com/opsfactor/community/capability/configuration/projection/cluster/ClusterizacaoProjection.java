package com.opsfactor.community.capability.configuration.projection.cluster;

import com.opsfactor.community.capability.configuration.projection.parametros.CadastroEParametrosProjection;

import com.opsfactor.community.capability.cluster.domain.location.*;
import com.opsfactor.community.capability.cluster.domain.produto.*;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import lombok.Getter;

import jakarta.persistence.NoResultException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classificação de materiais e locations de uma versão explícita de regras.
 * Os cadastros e parâmetros operacionais são independentes de classificação;
 * SupplyNetworkProjection recebe somente esse contexto não versionado.
 */
public class ClusterizacaoProjection {

    @Getter
    private final String perfilClusterizacaoId;

    @Getter
    private final CadastroEParametrosProjection cadastroEParametrosProjection;

    /** Exige uma identidade de perfil e um snapshot de cadastros completo. */
    public ClusterizacaoProjection(String perfilClusterizacaoId,
                                   CadastroEParametrosProjection cadastroEParametrosProjection) {

        if (perfilClusterizacaoId == null || perfilClusterizacaoId.isBlank()) {
            throw new IllegalArgumentException("Perfil de clusterização obrigatório para o snapshot.");
        }
        this.perfilClusterizacaoId = perfilClusterizacaoId;
        this.cadastroEParametrosProjection = Objects.requireNonNull(cadastroEParametrosProjection,
                "Snapshot de cadastros e parâmetros obrigatório.");

    }

    // Alocacoes de materiais em clusters de Demand Planning.
    // Os nomes internos preservam ClusterProdutos* por heranca JPA transicional.
    @Getter
    protected ClusterMateriais clusterProdutosPadraoDP;
    @Getter
    protected List<ClusterMateriais> clusterMateriaisList; // ordenados por prioridade
    @Getter
    protected Map<Long, ClusterMateriais> clusterProdutosDemandPlanningMap;
    // mapa pre-calculado de alocação produto -> ClusterProdutosDemandPlanning (instanciado na criação do projection)
    protected Map<Produto, ClusterMateriais> clusterProdutosDemandPlanningPorMaterial;

    protected Map<RegraAlocacaoClusterProdutos, Set<RegraAlocacaoClusterProdutosStatus>> valoresStatusRegraAlocacaoClusterProdutos;

    /** Preloaded characteristic values selected by each material allocation rule. */
    protected Map<RegraAlocacaoClusterProdutos, Set<RegraAlocacaoClusterProdutosCaracteristica>> valoresCaracteristicaRegraAlocacaoClusterProdutos;

    /**
     * Alias funcional para o cluster padrao de materiais usado por Demand
     * Planning.
     */
    public ClusterMateriais getClusterMateriaisPadraoDP() {
        return clusterProdutosPadraoDP;
    }

    /**
     * Alias funcional para a lista ordenada de clusters de materiais Demand
     * Planning.
     */
    public List<ClusterMateriais> getClusterMateriaisDemandPlanningList() {
        return clusterMateriaisList;
    }

    /**
     * Alias funcional para o mapa id -> cluster de materiais Demand Planning.
     */
    public Map<Long, ClusterMateriais> getClusterMateriaisDemandPlanningMap() {
        return clusterProdutosDemandPlanningMap;
    }
    
    // ALOCAÇÕES DE LOCATIONS EM CLUSTERS
    // antecipando modelo onde possamos ter diferentes clusterizações de locations por tipo de plano
    @Getter
    protected ClusterLocations clusterLocationsPadrao;
    @Getter
    protected List<ClusterLocations> clusterLocationsList; // ordenados por prioridade
    protected Map<Long,ClusterLocations> clusterLocationsMap;
    protected Map<Location,ClusterLocations> clusterLocationsPorLocation;
    protected Map<RegraAlocacaoClusterLocations, Set<RegraAlocacaoClusterLocationsPaisEstado>> paisEstadoRegraAlocacaoClusterLocations;
    protected Map<RegraAlocacaoClusterLocations, Set<RegraAlocacaoClusterLocationsTipoLocation>> tipoLocationRegraAlocacaoClusterLocations;


    public Set<Location> getLocationsDeClusterLocations(ClusterLocations clusterLocations, boolean somenteLocationsAtivas) {

        exigirPerfil(clusterLocations.getPerfilClusterizacao().getId());

        if (somenteLocationsAtivas) {
            return cadastroEParametrosProjection.getLocationsAtivas().stream()
                    .filter(location -> getClusterLocationsDeLocation(location).equals(clusterLocations))
                    .collect(Collectors.toSet());
        } else {
            return cadastroEParametrosProjection.getLocationSet().stream()
                    .filter(location -> getClusterLocationsDeLocation(location).equals(clusterLocations))
                    .collect(Collectors.toSet());
        }
    }

    public Set<Location> getLocationsAtivasDeClusterLocations(ClusterLocations clusterLocations) {
        return getLocationsDeClusterLocations(clusterLocations, true);
    }

    public ClusterMateriais getClusterProdutosDemandPlanning(Produto material, Location locationReferencia) {
        return getClusterProdutosDemandPlanning(material);
    }

    /**
     * Alias funcional para Demand Planning Community.
     *
     * <p>O metodo legado `getClusterProdutosDemandPlanning` permanece porque o
     * tipo JPA ainda herda de `ClusterProdutos`. Codigo novo que esteja
     * expressando regra funcional de Demand Planning deve usar este alias para
     * deixar claro que o conceito exibido/publicado e cluster de materiais.</p>
     */
    public ClusterMateriais getClusterMateriaisDemandPlanning(Produto material, Location locationReferencia) {
        return getClusterProdutosDemandPlanning(material, locationReferencia);
    }

    public ClusterMateriais getClusterProdutosDemandPlanning(Produto material) {
        ClusterMateriais clusterMateriais = clusterProdutosDemandPlanningPorMaterial.get(material);
        if (clusterMateriais == null) throw new NoResultException("Material " + material.getId() + " not mapped to ClusterProdutosDemandPlanning in Projection");
        return clusterMateriais;
    }

    /**
     * Alias funcional para a consulta do cluster de materiais de uma entidade
     * material.
     */
    public ClusterMateriais getClusterMateriaisDemandPlanning(Produto material) {
        return getClusterProdutosDemandPlanning(material);
    }

    /**
     * Cálculo explícito de alocação de cluster sem uso de mapa pré-populado.
     * Usado apenas no bootstrap do projection para materializar o mapa final.
     */
    protected ClusterMateriais getClusterProdutosDemandPlanningSemCache(Produto material) {
        return getClusterProdutosDeMaterialSemCache(
                material,
                clusterMateriaisList,
                clusterProdutosPadraoDP);
    }

    public ClusterProdutos getClusterProdutosDeId(Long clusterProdutosId) {
        Optional<ClusterMateriais> optionalClusterProdutosDemandPlanning = Optional.ofNullable(clusterProdutosDemandPlanningMap.get(clusterProdutosId));
        return optionalClusterProdutosDemandPlanning.orElse(null);
    }

    public Optional<ClusterMateriais> getClusterProdutosDemandPlanningDeId(Long clusterProdutosId) {
        return Optional.ofNullable(clusterProdutosDemandPlanningMap.get(clusterProdutosId));
    }

    /**
     * Alias funcional para localizar um cluster de materiais de Demand Planning
     * por id, preservando a entidade transicional `ClusterProdutosDemandPlanning`.
     */
    public Optional<ClusterMateriais> getClusterMateriaisDemandPlanningDeId(Long clusterMateriaisId) {
        return getClusterProdutosDemandPlanningDeId(clusterMateriaisId);
    }

    
    public Set<Produto> getMateriaisDeClusterProdutos(ClusterProdutos clusterProdutos, boolean somenteMateriaisAtivos) {

        exigirPerfil(clusterProdutos.getPerfilClusterizacao().getId());

                
        if (clusterProdutos instanceof ClusterMateriais) {
            return getMateriaisDeClusterProdutosDemandPlanning((ClusterMateriais) clusterProdutos, somenteMateriaisAtivos);
        }
        
        return new HashSet<>();
    }

    public Set<Produto> getMateriaisDeClusterProdutosAtivosNaLocation(ClusterProdutos clusterProdutos, Location location) {

        return getMateriaisDeClusterProdutos(clusterProdutos, true)
                .stream()
                .filter(material -> cadastroEParametrosProjection.isDfuAtiva(material, location))
                .collect(Collectors.toSet());

    }

    public Set<DFU> getDfusAtivasDeClusterLocationsEClusterProdutos(ClusterLocations clusterLocations, ClusterProdutos clusterProdutos) {
        return getLocationsAtivasDeClusterLocations(clusterLocations)
                .parallelStream()
                .flatMap(location -> getMateriaisDeClusterProdutosAtivosNaLocation(clusterProdutos, location)
                        .stream()
                        .map(material -> new DFU(material, location)))
                .collect(Collectors.toSet());
    }
    
    public Set<Produto> getMateriaisDeClusterProdutosId(Long clusterProdutosId, boolean somenteMateriaisAtivos) {
        
        ClusterProdutos clusterProdutos = getClusterProdutosDeId(clusterProdutosId);
        
        return getMateriaisDeClusterProdutos(clusterProdutos, somenteMateriaisAtivos);
        
    }
    
    public Set<Produto> getMateriaisDeClusterProdutosDemandPlanning(ClusterMateriais clusterMateriais, boolean somenteMateriaisAtivos) {

        exigirPerfil(clusterMateriais.getPerfilClusterizacao().getId());
        return cadastroEParametrosProjection.getMaterialSet().stream()
                .filter(x -> getClusterProdutosDemandPlanning(x).equals(clusterMateriais))
                .filter(x -> !somenteMateriaisAtivos || x.getAtivo())
                .collect(Collectors.toSet());
    }

    /**
     * Alias funcional da listagem de materiais pertencentes a um cluster de
     * Demand Planning.
     */
    public Set<Produto> getMateriaisDeClusterMateriaisDemandPlanning(
            ClusterMateriais clusterMateriaisDemandPlanning,
            boolean somenteMateriaisAtivos) {
        return getMateriaisDeClusterProdutosDemandPlanning(
                clusterMateriaisDemandPlanning,
                somenteMateriaisAtivos);
    }

    public Set<Produto> getMateriaisAtivosDeClusterProdutosDemandPlanning(
            ClusterMateriais clusterMateriais) {
        return getMateriaisDeClusterProdutosDemandPlanning(clusterMateriais, true);
    }

    /**
     * Alias funcional para materiais ativos de um cluster de materiais Demand
     * Planning.
     */
    public Set<Produto> getMateriaisAtivosDeClusterMateriaisDemandPlanning(
            ClusterMateriais clusterMateriaisDemandPlanning) {
        return getMateriaisDeClusterMateriaisDemandPlanning(clusterMateriaisDemandPlanning, true);
    }

    public Set<Produto> getMateriaisDeClusterProdutosDemandPlanningAtivosNaLocation(
            ClusterMateriais clusterMateriais,
            Location location) {

        exigirPerfil(clusterMateriais.getPerfilClusterizacao().getId());
        return cadastroEParametrosProjection.getMateriaisAtivosEmLocation(location).stream()
                .filter(x -> getClusterProdutosDemandPlanning(x).equals(clusterMateriais))
                .collect(Collectors.toSet());
    }

    /**
     * Alias funcional para materiais ativos em uma location dentro de um
     * cluster de materiais Demand Planning.
     */
    public Set<Produto> getMateriaisDeClusterMateriaisDemandPlanningAtivosNaLocation(
            ClusterMateriais clusterMateriaisDemandPlanning,
            Location location) {
        return getMateriaisDeClusterProdutosDemandPlanningAtivosNaLocation(
                clusterMateriaisDemandPlanning,
                location);
    }
    
    /** Falha antes de comparar IDs locais de universos de classificação distintos. */
    private void exigirPerfil(String perfilId) {

        if (!perfilClusterizacaoId.equals(perfilId)) {
            throw new IllegalArgumentException("Cluster pertence ao perfil " + perfilId
                    + "; snapshot solicitado pertence ao perfil " + perfilClusterizacaoId + ".");
        }

    }

    private <T extends ClusterProdutos> T getClusterProdutosDeMaterialSemCache(
            Produto material,
            List<T> clusterProdutosListOrdenada,
            T clusterPadrao) { // Map<Produto,Curva> mapaProdutoCurva,
        
        for (int i=0; i<clusterProdutosListOrdenada.size(); i++) {
            T clusterProdutos = clusterProdutosListOrdenada.get(i);
            
            T clusterProdutosPrePopulado = (T) getClusterProdutosDeId(clusterProdutos.getId());
            
            boolean materialNoCluster = true;
            for (RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos : clusterProdutosPrePopulado.getRegrasAlocacaoClusterProdutos()) {
                
                if (!materialNoCluster) break;
                
                if (regraAlocacaoClusterProdutos.getRegraAlocacaoTipo() == null) {
                    continue;
                }
                switch (regraAlocacaoClusterProdutos.getRegraAlocacaoTipo()) {
                    case STATUS_PRODUTO:
                        if (!valoresStatusRegraAlocacaoClusterProdutos
                                .getOrDefault(regraAlocacaoClusterProdutos, new HashSet<>()).stream()
                                .map(RegraAlocacaoClusterProdutosStatus::getStatusProduto)
                                /*
                                 * NEW pertence ao Enterprise. Se uma base
                                 * legada trouxer uma regra persistida desse
                                 * tipo, ela nao deve classificar materiais no
                                 * Community; regra contendo somente NEW vira
                                 * nao-match e o fluxo segue para o proximo
                                 * cluster ou para o cluster padrao.
                                 */
                                .filter(statusRegraAlocacao -> !Constantes.StatusProduto.NOVO.equals(statusRegraAlocacao))
                                // regra simplificada: status sempre avaliado no cadastro global do material
                                .anyMatch(statusRegraAlocacao -> statusRegraAlocacao.equals(cadastroEParametrosProjection.getStatusProduto(material, null, cadastroEParametrosProjection.getDataReferencia()))))
                            materialNoCluster = false;
                        break;
                    case CARACTERISTICA:
                        Map<CaracteristicaProduto, List<String>> atributosConsideradosPorCaracteristica =
                                valoresCaracteristicaRegraAlocacaoClusterProdutos
                                        .getOrDefault(regraAlocacaoClusterProdutos, new HashSet<>())
                                        .stream()
                                        .collect(Collectors.groupingBy(
                                                RegraAlocacaoClusterProdutosCaracteristica::getCaracteristica,
                                                Collectors.mapping(
                                                        RegraAlocacaoClusterProdutosCaracteristica::getAtributo,
                                                        Collectors.toList())));

                        for (Map.Entry<CaracteristicaProduto, List<String>> atributoEntry
                                : atributosConsideradosPorCaracteristica.entrySet()) {
                            /*
                             * A characteristic may be intentionally absent for
                             * this material. Absence is a non-match for this
                             * rule, not a malformed calculation input: the
                             * material can still match a later cluster or the
                             * default cluster.
                             */
                            if (atributoEntry.getKey().findValorCaracteristicaDeProduto(material)
                                    .filter(atributoEntry.getValue()::contains)
                                    .isEmpty()) {
                                materialNoCluster = false;
                                break;
                            }
                        }
                        break;
                }
            }
            // se não houve nenhum impedimento (material passou por todas as regras), retorna cluster
            // caso contrário vai para próximo cluster
            if (materialNoCluster) return clusterProdutosPrePopulado;
        }
        // caso não se encaixe em nenhum cluster, retorna cluster padrão
        return clusterPadrao;
    }

    public ClusterLocations getClusterLocationsDeId(Long clusterLocationsId) {
        Optional<ClusterLocations> optionalClusterLocations = Optional.ofNullable(clusterLocationsMap.get(clusterLocationsId));
        return optionalClusterLocations.orElse(null);
    }

    public Set<Location> getLocationsDeClusterLocationsId(Long clusterLocationsId) {
        ClusterLocations clusterLocations = getClusterLocationsDeId(clusterLocationsId);
        return getLocationsAtivasDeClusterLocations(clusterLocations);
    }

    public ClusterLocations getClusterLocationsDeLocation(Location location) {
        ClusterLocations clusterLocations = clusterLocationsPorLocation.get(location);

        if (clusterLocations == null && location != null) {
            Location locationPersistida = cadastroEParametrosProjection.getLocationMap().get(location.getId());
            if (locationPersistida != null) {
                clusterLocations = clusterLocationsPorLocation.get(locationPersistida);
            }
        }

        if (clusterLocations == null) throw new NoResultException("Location " + ((location == null) ? "null" : location.getId()) + " not mapped to ClusterLocations in Projection");
        return clusterLocations;
    }

    /**
     * Cálculo explícito de alocação de cluster sem uso de mapa pré-populado.
     * Usado apenas no bootstrap do projection para materializar o mapa final.
     */
    protected ClusterLocations getClusterLocationsDeLocationSemCache(Location location) {
        return getClusterLocationsDeLocationSemCache(
                location,
                clusterLocationsList,
                clusterLocationsPadrao);
    }

    private <T extends ClusterLocations> T getClusterLocationsDeLocationSemCache(
            Location location,
            List<T> clusterLocationsListOrdenada,
            T clusterPadrao) {

        for (int i=0; i<clusterLocationsListOrdenada.size(); i++) {
            T clusterLocations = clusterLocationsListOrdenada.get(i);

            T clusterLocationsPrePopulado = (T) getClusterLocationsDeId(clusterLocations.getId());

            boolean locationNoCluster = true;
            for (RegraAlocacaoClusterLocations regraAlocacaoClusterLocations : clusterLocationsPrePopulado.getRegrasAlocacaoClusterLocations()) {

                if (!locationNoCluster) break;

                if (regraAlocacaoClusterLocations.getRegraAlocacaoTipo() == null) {
                    continue;
                }
                switch (regraAlocacaoClusterLocations.getRegraAlocacaoTipo()) {
                    case CARACTERISTICA:
                        throw new RequiresEnterpriseVersionException("Location characteristic cluster allocation");
                    case PAIS_ESTADO:
                        Set<RegraAlocacaoClusterLocationsPaisEstado> paisesEstadosConsiderados = paisEstadoRegraAlocacaoClusterLocations
                                .getOrDefault(regraAlocacaoClusterLocations, new HashSet<>());

                        String paisLocation = (location.getPais() == null) ? "" : location.getPais();
                        String estadoLocation = (location.getEstado() == null) ? "" : location.getEstado();

                        locationNoCluster = paisesEstadosConsiderados.stream()
                                .anyMatch(regraAlocacaoPaisEstado ->
                                        regraAlocacaoPaisEstado.getPais().equals(paisLocation)
                                        && regraAlocacaoPaisEstado.getEstado().equals(estadoLocation));

                        break;
                    case TIPO_LOCATION:
                        Set<RegraAlocacaoClusterLocationsTipoLocation> tiposLocationConsiderados = tipoLocationRegraAlocacaoClusterLocations
                                .getOrDefault(regraAlocacaoClusterLocations, new HashSet<>());

                        locationNoCluster = tiposLocationConsiderados.stream()
                                .anyMatch(regraAlocacaoTipoLocation ->
                                        regraAlocacaoTipoLocation.getTipoLocation().equals(location.getTipoLocation()));

                        break;
                }
            }
            // se não houve nenhum impedimento (material passou por todas as regras), retorna cluster
            // caso contrário vai para próximo cluster
            if (locationNoCluster) return clusterLocationsPrePopulado;
        }
        // caso não se encaixe em nenhum cluster, retorna cluster padrão
        return clusterPadrao;
    }

    public Set<Location> getLocationsComExecucaoDP() {
        
        return clusterLocationsList.stream()
                .filter(x -> x.getParametrosClusterLocations().getPlanejaDP())
                .flatMap(x -> getLocationsAtivasDeClusterLocations(x).stream())
                .collect(Collectors.toSet());
        
    }
}
