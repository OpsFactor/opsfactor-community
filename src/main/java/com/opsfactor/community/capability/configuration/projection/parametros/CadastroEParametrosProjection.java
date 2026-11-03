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
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import com.opsfactor.community.platform.utility.FuncoesMap;
import lombok.Getter;

import jakarta.persistence.NoResultException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cadastros e parâmetros operacionais independentes de classificação Community.
 *
 * <p>Esta classe e o snapshot em memoria usado por Demand Planning, Supply
 * Planning e projections auxiliares para evitar consultas JPA durante calculos.
 * Características públicas de material/location também integram a fotografia,
 * permitindo que filtros físicos sejam resolvidos sem consultas JPA durante
 * os cálculos. Agrupadores configuráveis e filtros salvos privados continuam
 * pertencendo ao Enterprise.</p>
 */
public class CadastroEParametrosProjection {
    /** Referência temporal do cadastro; não identifica uma versão de clustering. */
    @Getter
    private final LocalDateTime dataReferencia = LocalDateTime.now();

        
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
    protected volatile Set<Location> locationAtivaSet;

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
            CalendarioSimples calendario) {

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
            CalendarioSimples calendario) {

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
                    (int) Math.ceil((double) calendarioDemandPlan.converteDiasParaPeriodosCalendario(parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDp(), calendarioDemandPlan.getPosicaoPeriodoPresente())));
            } else if (parametrosGlobais.getDiasHorizonteCongelado() != null) {
                return Optional.of(
                    (int) Math.ceil((double) calendarioDemandPlan.converteDiasParaPeriodosCalendario(parametrosGlobais.getDiasHorizonteCongelado(), calendarioDemandPlan.getPosicaoPeriodoPresente())));
            } else {
                return Optional.empty();
            }
        } else if (parametrosGlobais.getDiasHorizonteCongelado() != null) {
            return Optional.of(
                    (int) Math.ceil((double) calendarioDemandPlan.converteDiasParaPeriodosCalendario(parametrosGlobais.getDiasHorizonteCongelado(), calendarioDemandPlan.getPosicaoPeriodoPresente())));
        } else {
            return Optional.empty();
        }
        
    }
    
    public Optional<Integer> getDPHorizonteCongeladoEmPeriodos(Collection<Location> locations, Collection<Produto> materiais, Calendario calendarioDemandPlan) {
        
        Integer totalizador = null;
        for (Location location : locations) {
            for (Produto material : materiais) {
                Optional<Integer> optionalValor = getDPHorizonteCongeladoEmPeriodos(location, material,
                        calendarioDemandPlan);
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
        locationAtivaSet = Collections.unmodifiableSet(Location.filtraLocationsAtivasSet(getLocationSet()));
        return locationAtivaSet;
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
