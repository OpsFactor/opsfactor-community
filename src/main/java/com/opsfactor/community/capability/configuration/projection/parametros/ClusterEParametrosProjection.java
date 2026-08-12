package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.cluster.domain.location.*;
import com.opsfactor.community.capability.cluster.domain.produto.*;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais.ModeloCadastroProdutoLocation;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocationInterface;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProdutoInterface;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.Getter;

import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Projection central de parametros, materiais, locations e clusters Community.
 *
 * <p>Esta classe e o snapshot em memoria usado por Demand Planning, Supply
 * Planning e projections auxiliares para evitar consultas JPA durante calculos.
 * Características públicas de material/location também integram a fotografia,
 * permitindo que filtros físicos sejam resolvidos sem consultas JPA durante
 * os cálculos. Agrupadores configuráveis e filtros salvos privados continuam
 * pertencendo ao Enterprise.</p>
 */
public class ClusterEParametrosProjection {

    /**
     * Data de referencia usada para avaliar status de material/location e regras
     * de cluster.
     */
    LocalDateTime dataReferencia;
        
    /**
     * Materiais carregados no snapshot.
     */
    @Getter
    protected Set<Produto> materialSet;

    /**
     * Indice materialId -> material persistido.
     */
    @Getter
    protected Map<String,Produto> materialMap;

    /**
     * Locations carregadas no snapshot.
     */
    @Getter
    protected Set<Location> locationSet;

    /**
     * Cache lazy das locations ativas.
     */
    protected Set<Location> locationAtivaSet;

    /**
     * Indice locationId -> location persistida.
     */
    @Getter
    protected Map<String,Location> locationMap;

    /** Índice público de características materiais, com seus valores já carregados. */
    @Getter
    protected Map<String, CaracteristicaProduto> caracteristicaProdutoMap;

    /** Índice público de características de location, com seus valores já carregados. */
    @Getter
    protected Map<String, CaracteristicaLocation> caracteristicaLocationMap;

    /**
     * Location efetiva para ler parametros material/location de cada location
     * do snapshot.
     *
     * <p>A referencia e resolvida uma unica vez pela factory, para que os
     * calculos subsequentes nao toquem na associacao JPA lazy nem possam seguir
     * uma cadeia de espelhamentos. O valor e sempre a propria location ou a
     * sua referencia direta, ambas canonicas no snapshot.</p>
     */
    protected Map<Location, Location> locationForProductLocationParametersMap;
    
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


    // PARAMETROS
    @Getter
    protected ParametrosGlobais parametrosGlobais;
    protected Map<Location,Map<Produto,ParametrosProdutoLocation>> mapaParametrosProdutoLocation;
        
    /**
     * Extrai location do projection a partir de seu Id
     * Caso não encontre lança um NoResultException
     * @param locationId
     * @return 
     */
    public Location getLocationPersistida (String locationId) {
        Location location = locationMap.get(locationId);
        if (location == null) throw new NoResultException("Location " + locationId + " not available in Projection");
        return location;
    }

    /** Resolve uma característica material da mesma fotografia usada pelo cálculo. */
    public CaracteristicaProduto getCaracteristicaProdutoDeId(String caracteristicaProdutoId) {

        CaracteristicaProduto caracteristicaProduto = caracteristicaProdutoMap.get(caracteristicaProdutoId);
        if (caracteristicaProduto == null) {
            throw new NoResultException(
                    "Material Characteristic " + caracteristicaProdutoId + " not available in Projection");
        }
        return caracteristicaProduto;

    }

    /** Resolve uma característica de location da mesma fotografia usada pelo cálculo. */
    public CaracteristicaLocation getCaracteristicaLocationDeId(String caracteristicaLocationId) {

        CaracteristicaLocation caracteristicaLocation = caracteristicaLocationMap.get(caracteristicaLocationId);
        if (caracteristicaLocation == null) {
            throw new NoResultException(
                    "Location Characteristic " + caracteristicaLocationId + " not available in Projection");
        }
        return caracteristicaLocation;

    }
    
    /**
     * Extrai material do projection a partir de seu Id
     * Caso não encontre lança um NoResultException
     * @param materialId
     * @return 
     */
    public Produto getMaterialPersistido (String materialId) {
        Produto material = materialMap.get(materialId);
        if (material == null) throw new NoResultException("Material " + materialId + " not available in Projection");
        return material;
    }

    /**
     * Extrai o ParametrosProdutoLocation da combinação de material e location
     * Usa os parametros da propria combinacao material/location ou, quando
     * houver referencia cadastrada, da location de referencia direta.
     * @param material
     * @param location
     * @return
     */
    private Optional<ParametrosProdutoLocation> getParametrosProdutoLocation(Produto material, Location location) {

        Location locationBuscada = getLocationForProductLocationParameters(location);

        return Optional.ofNullable(mapaParametrosProdutoLocation
                .getOrDefault(locationBuscada, new HashMap<>())
                .getOrDefault(material, null));

    }

    public double getSNPFrequenciaReabascecimentoDias(Produto material, Location location) {

        /*
         * Frequencia de reabastecimento passou a ser parametro da otimizacao de
         * politica de estoques. O Community preserva o campo transicional no
         * schema, mas qualquer projection operacional deve neutralizar o valor
         * para que bases legadas nao alterem o heuristico.
         */
        return 0.0;

    }

    /**
     * Resolves configured shelf life from the in-memory parameter snapshot.
     *
     * <p>A positive material/location value takes precedence over the shared
     * material default. The projection already canonicalizes the material and
     * parameter location, so this method never follows a JPA relationship or
     * performs a database query while an optimization is running.</p>
     */
    public OptionalDouble getShelfLifeDays(Location location, Produto material) {

        Produto persistedMaterial = getMaterialPersistido(material.getId());
        Double materialLocationShelfLifeDays = getParametrosProdutoLocation(persistedMaterial, location)
                .map(ParametrosProdutoLocation::getPrazoValidadeDiasCadastrado)
                .orElse(0.0d);

        if (materialLocationShelfLifeDays != null && materialLocationShelfLifeDays > 0.0d) {
            return OptionalDouble.of(materialLocationShelfLifeDays);
        }

        Double materialShelfLifeDays = persistedMaterial.getShelfLifeDays();
        return materialShelfLifeDays != null && materialShelfLifeDays > 0.0d
                ? OptionalDouble.of(materialShelfLifeDays)
                : OptionalDouble.empty();

    }

    /**
     * Converts the configured shelf life to whole calendar periods.
     */
    public Optional<Integer> getShelfLifePeriods(
            Location location,
            Produto material,
            Calendario calendario) {

        OptionalDouble shelfLifeDays = getShelfLifeDays(location, material);
        if (shelfLifeDays.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((int) Math.floor(
                calendario.converteDiasParaPeriodosCalendario(shelfLifeDays.getAsDouble())));

    }

    /**
     * Returns the smallest configured shelf life across the supplied locations.
     */
    public Optional<Integer> getMinimumShelfLifePeriods(
            Produto material,
            Collection<Location> locationsToEvaluate,
            Calendario calendario) {

        return locationsToEvaluate.stream()
                .map(location -> getShelfLifeDays(location, material))
                .filter(OptionalDouble::isPresent)
                .map(shelfLifeDays -> (int) Math.floor(
                        calendario.converteDiasParaPeriodosCalendario(shelfLifeDays.getAsDouble())))
                .min(Comparator.naturalOrder());

    }

    /**
     * Retorna a location efetiva para leitura de parametros material/location.
     *
     * <p>A projection nunca consulta a associacao JPA da location neste ponto:
     * a factory ja materializou e canonicalizou o unico salto permitido. Isso
     * preserva a previsibilidade do snapshot em loops de Demand e Supply
     * Planning e elimina o risco de N+1.</p>
     */
    private Location getLocationForProductLocationParameters(Location location) {

        if (location == null) {
            throw new IllegalArgumentException(
                    "Location is required to read material/location parameters.");
        }

        Location locationNoSnapshot = getLocationPersistida(location.getId());
        Location locationEfetiva = locationForProductLocationParametersMap.get(locationNoSnapshot);
        if (locationEfetiva == null) {
            throw new IllegalStateException(
                    "Location "
                            + locationNoSnapshot.getId()
                            + " has no material/location parameter reference in the projection snapshot.");
        }

        return locationEfetiva;

    }
    
    /**
     * Aceita location = null. Neste caso, se usa status global do material
     * @param material
     * @param location
     * @param dataReferencia
     * @return 
     */
    public Constantes.StatusProduto getStatusProduto(Produto material, Location location, LocalDateTime dataReferencia) {
        
        Location locationBuscada = getLocationForProductLocationParameters(location);
        
        // configuração principal : material/location
        if (locationBuscada != null &&
                mapaParametrosProdutoLocation.containsKey(locationBuscada) && 
                mapaParametrosProdutoLocation.get(locationBuscada).containsKey(material)) {
            
            return mapaParametrosProdutoLocation.get(locationBuscada).get(material).getStatusProduto(dataReferencia, parametrosGlobais);
            
        // se não houver cadastro material/location utiliza-se status global do material
        } else {
            
            return material.getStatusProduto(dataReferencia, parametrosGlobais);
            
        }
        
    }
    
    /**
     * 
     * @param location
     * @param dataReferencia
     * @return 
     */
    public Constantes.StatusProduto getStatusLocation(Location location, LocalDate dataReferencia) {
        
        return location.getStatusLocation(dataReferencia, getParametrosGlobais());
        
    }

    /**
     * Retorna a unidade padrão material/location ou, caso esta nao exista, a
     * unidade padrao do material.
     * Caso não exista retorna a unidade de medida 'UN'
     * @param material
     * @param location
     * @return 
     */
    public UnidadeMedida getSNPUnidadeMedidaPadrao(Produto material, Location location) {
        
        Location locationBuscada = getLocationForProductLocationParameters(location);
        
        Produto materialComDadosCompletos = getMaterialMap().get(material.getId());

        Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = FuncoesMap.getElementoDeNestedMap(
                mapaParametrosProdutoLocation,
                ParametrosProdutoLocation.class,
                locationBuscada, material);

        return optionalParametrosProdutoLocation
                .map(ParametrosProdutoLocation::getUnidadeMedidaPadraoCadastrado)
                .orElseGet(() -> getSNPUnidadeMedidaPadrao(material));

    }
    
    public UnidadeMedida getSNPUnidadeMedidaPadrao(Produto material) {
        
        Produto materialComDadosCompletos = getMaterialMap().get(material.getId());
        
        // configuração principal : material/location
        if (materialComDadosCompletos.getUnidadeMedidaPadraoCadastrado() != null) { 
            return materialComDadosCompletos.getUnidadeMedidaPadraoCadastrado();
        // nenhuma configuração foi feita : retorna valor padrão
        } else {
            return parametrosGlobais.getUnidadeMedidaPadraoSNP();
        }
        
    }
    
    public UnidadeMedida getSNPUnidadeMedidaPadraoGlobal() {
        
        return parametrosGlobais.getUnidadeMedidaPadraoSNP();
        
    }
    
    public UnidadeMedida getSNPUnidadeMedidaProdutoLocationCadastrado(Produto material, Location location) {
        
        Location locationBuscada = getLocationForProductLocationParameters(location);
        
        if (mapaParametrosProdutoLocation.containsKey(locationBuscada) && 
                mapaParametrosProdutoLocation.get(locationBuscada).containsKey(material)) {
            return mapaParametrosProdutoLocation.get(locationBuscada).get(material).getUnidadeMedidaPadraoCadastrado();
        } else {
            return null;
        }
    }
     
    
    public UnidadeMedida getSNPUnidadeMedidaLoteMinimoEMultiploProducao(
            Produto material, Location location) {
        
        // A location efetiva ja foi resolvida no snapshot em um unico salto.
        Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = getParametrosProdutoLocation(material, location);
        
        return optionalParametrosProdutoLocation
                .map(parametrosProdutoLocation ->
                        parametrosProdutoLocation.getUnidadeMedidaLoteMinimoMultiploProducao(getParametrosGlobais()))
                .orElseGet(() -> getSNPUnidadeMedidaPadrao(material, location));
        
    }
    
    
    /**
     * Retorna o lote mínimo para o material/location ou , caso este cadastro não exista,
     * para o material. Retorna 0 caso não haja nenhuma informação cadastrada
     * @param material
     * @param location
     * @return 
     */
    public OptionalDouble getSNPLoteMinimoProducao(
            Produto material, Location location, 
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {
        
        // A location efetiva ja foi resolvida no snapshot em um unico salto.
        Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = getParametrosProdutoLocation(material, location);
        
        if (optionalParametrosProdutoLocation.isPresent()) {
            ParametrosProdutoLocation parametrosProdutoLocation = optionalParametrosProdutoLocation.orElseThrow(
                    () -> new IllegalStateException("Parâmetros material/location presentes não podem desaparecer"));
            OptionalDouble loteMinimoProducaoUnidadeOrigem = parametrosProdutoLocation.getLoteMinimoProducao();
            if (loteMinimoProducaoUnidadeOrigem.isEmpty()) return OptionalDouble.empty();

            UnidadeMedida unidadeMedidaOrigem = parametrosProdutoLocation.getUnidadeMedidaLoteMinimoMultiploProducao(getParametrosGlobais());
            return OptionalDouble.of(loteMinimoProducaoUnidadeOrigem.getAsDouble() *
                    unidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget));
        }
            
        return OptionalDouble.empty();
        
    }
    
    /**
     * Múltiplo para requisições geradas no SNP (na unidade de medida padrão material/location)
     * Retorna nulo caso não haja unidade cadastrada nem em parametros material/location
     * e nem para o material
     * @param material
     * @param location
     * @return 
     */
    public OptionalDouble getSNPMultiploProducao(
            Produto material, Location location, 
            UnidadeMedida unidadeMedidaTarget,
            UnidadeMedidaProjection unidadeMedidaProjection) {
        
        // A location efetiva ja foi resolvida no snapshot em um unico salto.
        Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = getParametrosProdutoLocation(material, location);
                
        if (optionalParametrosProdutoLocation.isPresent()) {
            ParametrosProdutoLocation parametrosProdutoLocation = optionalParametrosProdutoLocation.orElseThrow(
                    () -> new IllegalStateException("Parâmetros material/location presentes não podem desaparecer"));
            OptionalDouble multiploProducaoUnidadeOrigem = parametrosProdutoLocation.getMultiploProducao();
            UnidadeMedida unidadeMedidaOrigem = parametrosProdutoLocation.getUnidadeMedidaLoteMinimoMultiploProducao(getParametrosGlobais());
            if (multiploProducaoUnidadeOrigem.isPresent()) {
                return OptionalDouble.of(
                        multiploProducaoUnidadeOrigem.getAsDouble() *
                        unidadeMedidaProjection.getConversaoParaUnidadeDestino(material, unidadeMedidaOrigem, unidadeMedidaTarget));
            }
        }
        
        return OptionalDouble.empty();
        
    }
    
    public Optional<Integer> getDPHorizonteCongeladoEmPeriodos(Location location, Produto material, Calendario calendarioDemandPlan) {
        
        Location locationBuscada = getLocationForProductLocationParameters(location);
        
        Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = Optional.ofNullable(mapaParametrosProdutoLocation
                .getOrDefault(locationBuscada, new HashMap<>()) 
                .getOrDefault(material, null));
        if (optionalParametrosProdutoLocation.isPresent()) {
            ParametrosProdutoLocation parametrosProdutoLocation = optionalParametrosProdutoLocation.orElseThrow(
                    () -> new IllegalStateException("Parâmetros material/location presentes não podem desaparecer"));
            if (parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDpCadastrado() != null) {
                return Optional.of(
                    (int) Math.ceil((double) calendarioDemandPlan.converteDiasParaPeriodosCalendario(parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDp())));
            } else if (parametrosGlobais.getDiasHorizonteCongelado() != null) {
                return Optional.of(
                    (int) Math.ceil((double) calendarioDemandPlan.converteDiasParaPeriodosCalendario(parametrosGlobais.getDiasHorizonteCongelado())));
            } else {
                return Optional.empty();
            }
        } else if (parametrosGlobais.getDiasHorizonteCongelado() != null) {
            return Optional.of(
                    (int) Math.ceil((double) calendarioDemandPlan.converteDiasParaPeriodosCalendario(parametrosGlobais.getDiasHorizonteCongelado())));
        } else {
            return Optional.empty();
        }
        
    }
    
    public Optional<Integer> getDPHorizonteCongeladoEmPeriodos(Collection<Location> locations, Collection<Produto> materiais, Calendario calendarioDemandPlan) {
        
        Integer totalizador = null;
        for (Location location : locations) {
            for (Produto material : materiais) {
                Optional<Integer> optionalValor = getDPHorizonteCongeladoEmPeriodos(location, material, calendarioDemandPlan);
                if (optionalValor.isPresent()) {
                    int valor = optionalValor.orElseThrow(() -> new IllegalStateException(
                            "Horizonte congelado presente não pode desaparecer durante totalização"));
                    if (totalizador == null) { 
                        totalizador = valor;
                    } else {
                        totalizador = Math.max(totalizador, valor);
                    }
                }
            }
        }
        
        return Optional.ofNullable(totalizador);
        
    }
    
    public UnidadeMedida getDPUnidadeVendas(Produto material) {
        
        Produto materialNoMapa = materialMap.get(material.getId());
        return materialNoMapa.getUnidadeMedidaVendas(parametrosGlobais);
        
    }
    
    public String getValorCaracteristicaProduto(Produto material, CaracteristicaProdutoInterface caracteristica) {

        // A própria característica pública resolve seu valor no material do snapshot.
        return caracteristica.getValorCaracteristicaDeProduto(material);
    }

    public String getValorCaracteristicaLocation(Location location, CaracteristicaLocationInterface caracteristica) {

        // A própria característica pública resolve seu valor na location do snapshot.
        return caracteristica.getValorCaracteristicaDeLocation(location);
    }

    public Set<Location> getLocationsDeClusterLocations(ClusterLocations clusterLocations, boolean somenteLocationsAtivas) {
        if (somenteLocationsAtivas) {
            return getLocationsAtivas().stream()
                    .filter(location -> getClusterLocationsDeLocation(location).equals(clusterLocations))
                    .collect(Collectors.toSet());
        } else {
            return getLocationSet().stream()
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
                
        if (clusterProdutos instanceof ClusterMateriais) {
            return getMateriaisDeClusterProdutosDemandPlanning((ClusterMateriais) clusterProdutos, somenteMateriaisAtivos);
        }
        
        return new HashSet<>();
    }

    public Set<Produto> getMateriaisDeClusterProdutosAtivosNaLocation(ClusterProdutos clusterProdutos, Location location) {

        return getMateriaisDeClusterProdutos(clusterProdutos, true)
                .stream()
                .filter(material -> isDfuAtiva(material, location))
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
        return getMaterialSet().stream()
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
        return getMateriaisAtivosEmLocation(location).stream()
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
                                .anyMatch(statusRegraAlocacao -> statusRegraAlocacao.equals(getStatusProduto(material, null, dataReferencia))))
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
            Location locationPersistida = locationMap.get(location.getId());
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

    public boolean isDfuAtiva(Produto material, Location location) {
        
        if (!location.getAtivo()) return false;
        if (!material.getAtivo()) return false;
        if (location == null) return material.getAtivo();
        
        Location locationBuscada = getLocationForProductLocationParameters(location);
        
        Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = Optional.ofNullable(mapaParametrosProdutoLocation
                .getOrDefault(locationBuscada, new HashMap<>()) 
                .getOrDefault(material, null));
        if (optionalParametrosProdutoLocation.isEmpty() && parametrosGlobais.getModeloCadastroProdutoLocation().equals(ModeloCadastroProdutoLocation.INATIVO_SE_AUSENTE)) return false;
        return optionalParametrosProdutoLocation
                .map(ParametrosProdutoLocation::getAtivoCadastrado)
                .orElseGet(material::getAtivo);
        
    }
    
    public Set<Produto> getMateriaisAtivosEmLocation(Location location) {
        return getMaterialSet().stream()
                .filter(x -> isDfuAtiva(x, location))
                .collect(Collectors.toSet());
    }
    
    public Set<Produto> getMateriais(boolean apenasMateriaisAtivos) {
        return (apenasMateriaisAtivos) ?
                getMateriaisAtivos()
                : getMaterialSet();
    }
    public Set<Produto> getMateriaisAtivos() {
        return Produto.filtraMaterialSetAtivos(getMaterialSet());
    }

    public Set<Location> getLocations(boolean apenasLocationsAtivas) {
        return (apenasLocationsAtivas) ?
                getLocationsAtivas()
                : getLocationSet();
    }
    public Set<Location> getLocationsAtivas() {
        if (locationAtivaSet != null) return locationAtivaSet;
        locationAtivaSet = Location.filtraLocationsAtivasSet(getLocationSet());
        return locationAtivaSet;
    }    

    public Set<Location> getLocationsComExecucaoDP() {
        
        return clusterLocationsList.stream()
                .filter(x -> x.getParametrosClusterLocations().getPlanejaDP())
                .flatMap(x -> getLocationsAtivasDeClusterLocations(x).stream())
                .collect(Collectors.toSet());
        
    }
    
    public UnidadeMedida getTransferenciaUnidadeMedida(Produto material, Location locationOrigem, Location locationDestino) {
        Produto materialComCamposPopulados = getMaterialMap().get(material.getId());
        return materialComCamposPopulados.getUnidadeMedidaTransferencia(getParametrosGlobais());
    }

    public int getDPHorizonteForecastDias() {
        
        return getParametrosGlobais().getHorizonteForecastDias();
        
    }    

    /**
     * Retorna o prazo de validade em periodos para material/location.
     *
     * <p>Shelf-life, aging e writeoff fazem parte do modelo otimizado
     * Enterprise. O Community mantem este contrato apenas para que o overlay
     * privado consiga compilar contra a projection compartilhada; quando
     * executado sem a projection Enterprise complementar, a ausencia de valor
     * significa que nenhum bloco de writeoff deve ser materializado.</p>
     */
    public Optional<Integer> getPrazoValidadeEmPeriodos(
            Location location,
            Produto material,
            Calendario calendario) {

        return Optional.empty();

    }

    /**
     * Retorna o tempo de processo em periodos para material/location.
     *
     * <p>Tempo de processo detalhado e segmentacao por lote sao capacidades
     * Enterprise. No Community, o heuristico trabalha sem aging de lotes e por
     * isso este contrato permanece neutro.</p>
     */
    public Optional<Integer> getTempoProcessoEmPeriodos(
            Location location,
            Produto material,
            Calendario calendario) {

        return Optional.empty();

    }

    public boolean getMaterialPossuiAlgumTempoProcessoOuPrazoValidade(Produto material) {

        /*
         * Community nao executa shelf life, write-off ou aging por lote. O
         * overlay Enterprise pode substituir este contrato com parametros
         * completos por material/location; no Community o filtro de materiais
         * com segmentacao por lote fica sempre vazio.
         */
        return false;

    }

    public Set<Location> getLocationsAtivasComTipoLocation(Location.TipoLocation tipoLocation) {
        return Location.filtraLocationsAtivasSetComTipoLocation(locationSet, tipoLocation);
    }
    public Set<Location> getLocationsAtivasComTiposLocation(Location.TipoLocation... tiposLocation) {
        return Location.filtraLocationsAtivasSetComTiposLocation(locationSet, tiposLocation);
    }

}
