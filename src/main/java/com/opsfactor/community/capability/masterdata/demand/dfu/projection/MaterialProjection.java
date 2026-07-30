package com.opsfactor.community.capability.masterdata.demand.dfu.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Escopo de materiais usado por projections e rotinas de calculo.
 *
 * <p>No Community esta classe nao representa o "Material Filter" configuravel
 * do perfil de Supply Planning, que pertence ao Enterprise. Ela e apenas um
 * value object em memoria para carregar todos os materiais ativos ou um
 * subconjunto produzido pelo proprio fluxo tecnico.</p>
 */
@Data
@EqualsAndHashCode(of = {"setMateriais"})
public class MaterialProjection {
    
    /**
     * Projection base que conhece clusters, status material/location e DFUs
     * ativas. O escopo de materiais e calculado contra essa base.
     */
    protected ClusterEParametrosProjection clusterEParametrosProjection;

    /**
     * Materiais candidatos do escopo em memoria. A factory publica conjuntos
     * imutaveis para que filtros tecnicos nao mudem durante calculos paralelos.
     */
    protected Set<Produto> setMateriais = new HashSet<>();

    /**
     * Cache lazy dos materiais ativos dentro do escopo.
     */
    protected Set<Produto> setMateriaisAtivos;
    
    public Set<Produto> getMaterialSet() {
        return setMateriais;
    }
    
    public Set<Produto> getMateriaisAtivos() {
        if (setMateriaisAtivos == null) {
            setMateriaisAtivos = Produto.filtraMaterialSetAtivos(setMateriais);
            setMateriaisAtivos = Collections.unmodifiableSet(setMateriaisAtivos);
        }
        return setMateriaisAtivos;
    }    
    
    /**
     * Retorna `null` quando este objeto representa o escopo completo.
     *
     * <p>Algumas queries e projections usam `null` como atalho tecnico para
     * nao aplicar filtro material. Isso nao representa um filtro funcional
     * configuravel pelo usuario no Community.</p>
     */
    public Set<Produto> getMateriaisAtivosOuNuloSeMaterialProjectionCompleto() {
        if (this instanceof MaterialProjectionCompleto) return null;
        return getMateriaisAtivos();
    }
    
    public Set<Produto> getMateriaisAtivosEmLocation(Location location) {
        return getMateriaisAtivos().stream()
                .filter(x -> clusterEParametrosProjection.isDfuAtiva(x, location))
                .collect(Collectors.toSet());
    }
    
    public boolean isMaterialFiltradoEAtivoNaLocation(Produto material, Location location) {
        return getMateriaisAtivos().contains(material) && clusterEParametrosProjection.isDfuAtiva(material, location);
    }
}
