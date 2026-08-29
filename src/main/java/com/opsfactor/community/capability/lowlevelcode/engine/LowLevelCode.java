package com.opsfactor.community.capability.lowlevelcode.engine;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.platform.bi.AgregacaoDFU;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.DFUMalhaCircularDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeEdgeDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeNodeDTO;
import com.opsfactor.community.platform.exception.CircularNetworkException;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Calcula o low level code das DFUs material/location usadas pelo Supply Planning.
 *
 * <p>O low level code ordena a malha para que o heuristico processe primeiro as
 * DFUs mais proximas da demanda e depois caminhe para fornecedores, producao e
 * pontos de transbordo. A entidade fisica de material ainda e {@link Produto},
 * mas a rotina usa a nomenclatura material na borda e em variaveis novas.</p>
 */
@Slf4j
public class LowLevelCode {
    
    VersaoMalha versaoMalha;
    /*
     * Data usada para avaliar status/validade de materiais e linhas da malha
     * durante a ordenacao por low level code.
     */
    LocalDateTime dataHorarioReferenciaStatusMaterial;
    
    @Getter
    MaterialProjection materialProjection;
    @Getter
    LocationProjection locationProjection;
    
    @Getter
    private SupplyNetworkProjection supplyNetworkProjection;
    // low level code -> location -> materiais
    // não inclui locations de clientes : estas são sempre low level code 1 e não ficam no mapa
    // para se economizar memória
    private Map<Integer,Map<Location,Set<Produto>>> lowLevelCodeLocationsInternasEFornecedores = new HashMap<>();
    // usado para indicar erros de circularidade
    @Getter
    Set<DFUMalhaCircularDTO> detalheErroCircularidade = new HashSet<>();
    
    
    public LowLevelCode(
            SupplyNetworkProjection supplyNetworkProjection, 
            VersaoMalha versaoMalha, 
            MaterialProjection materialProjection,
            LocationProjection locationProjection,
            LocalDateTime dataHorarioReferenciaStatusMaterial) {
        
        this.supplyNetworkProjection = supplyNetworkProjection;
        this.versaoMalha = versaoMalha;
        this.dataHorarioReferenciaStatusMaterial = dataHorarioReferenciaStatusMaterial;
        this.materialProjection = materialProjection;
        this.locationProjection = locationProjection;
    }
    
    public Optional<Integer> getLowLevelCode(Location location, Produto material) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        
        switch (location.getTipoLocation()) {
            case CLIENTE_FINAL:
                if (clusterEParametrosProjection.isDfuAtiva(material, location)) return Optional.of(1);
                return Optional.empty();
            case REGIAO_COMERCIAL:
                if (clusterEParametrosProjection.isDfuAtiva(material, location)) return Optional.of(1);
                return Optional.empty();
            default:
                Integer lowLevelCode = null;
                return lowLevelCodeLocationsInternasEFornecedores.entrySet().stream()
                        .filter(entry -> entry.getValue().getOrDefault(location, new HashSet<>()).contains(material))
                        .findAny()
                        .map(entry -> entry.getKey());
        }
        
    }
    
    public Set<Integer> getLowLevelCodes() {
        return lowLevelCodeLocationsInternasEFornecedores.keySet();
    }
    
    public Set<Location> getLocationsLowLevelCode(int lowLevelCode) {
        
        Set<Location> locationsInternasOuFornecedores = new HashSet(lowLevelCodeLocationsInternasEFornecedores
                .getOrDefault(lowLevelCode, new HashMap<>())
                .keySet());
        
        // locations de clientes fazem parte do low level code 1
        if (lowLevelCode == 1) {
            ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
            locationsInternasOuFornecedores.addAll(locationProjection.getLocationsAtivasSetComTiposLocation(
                    LocationAbstract.TipoLocation.CLIENTE_FINAL, LocationAbstract.TipoLocation.REGIAO_COMERCIAL));
        }
        
        return locationsInternasOuFornecedores;
        
    }
    
    public Set<Produto> getMateriaisLowLevelCodeEmLocation(int lowLevelCode, Location location) {

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();

        switch (location.getTipoLocation()) {
            case CLIENTE_FINAL:
                // locatiosn clientes não podem fazer parte de low level code diferente de 1
                if (lowLevelCode != 1) return new HashSet<>();
                return materialProjection.getMateriaisAtivosEmLocation(location);
            case REGIAO_COMERCIAL:
                // locatiosn clientes não podem fazer parte de low level code diferente de 1
                if (lowLevelCode != 1) return new HashSet<>();
                return materialProjection.getMateriaisAtivosEmLocation(location);
            default:
                return lowLevelCodeLocationsInternasEFornecedores
                        .getOrDefault(lowLevelCode, new HashMap<>())
                        .getOrDefault(location, new HashSet<>());
        }
        
    }
    
    public Set<DFU> getDFUs() {

        Set<DFU> setDFUs = new HashSet<>();
        for (Map<Location, Set<Produto>> subMapa : lowLevelCodeLocationsInternasEFornecedores.values()) {
            for (Entry<Location,Set<Produto>> entry : subMapa.entrySet()) {
                for (Produto material : entry.getValue()) {
                    setDFUs.add(new DFU(material, entry.getKey()));
                }
            }
        }
        
        return setDFUs;
            
    }

    public Set<DFU> getDFUsNoLowLevelCode(int lowLevelCode) {

        if (lowLevelCode < 1) {
            throw getUnsupportedLowLevelCodeException(lowLevelCode);
        }

        if (lowLevelCode == 1) {
            ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
            return locationProjection.getLocationsAtivasSetComTiposLocation(
                    LocationAbstract.TipoLocation.CLIENTE_FINAL, LocationAbstract.TipoLocation.REGIAO_COMERCIAL)
                    .stream()
                    .flatMap(locationCliente -> materialProjection.getMateriaisAtivosEmLocation(locationCliente)
                            .stream()
                            .map(material -> new DFU(material, locationCliente)))
                    .collect(Collectors.toSet());
        } else {

            Set<DFU> setDFUs = new HashSet<>();

            Map<Location, Set<Produto>> mapaMateriaisPorLocation = lowLevelCodeLocationsInternasEFornecedores
                    .getOrDefault(lowLevelCode,
                            new HashMap<>());

            for (Entry<Location, Set<Produto>> entry : mapaMateriaisPorLocation.entrySet()) {
                Location location = entry.getKey();
                for (Produto material : entry.getValue()) {
                    setDFUs.add(new DFU(material, location));
                }
            }

            return setDFUs;
        }

    }

    private IllegalArgumentException getUnsupportedLowLevelCodeException(int lowLevelCode) {

        return new IllegalArgumentException(
                "LowLevelCode can return DFUs only for levels greater than or equal to 1; received "
                        + lowLevelCode
                        + ". Level 1 represents final customer/commercial-region demand and upstream levels must be positive.");

    }

    public Map<Integer,Set<DFU>> getMapaDFUsPorLowLevelCode() {
        
        Map<Integer,Set<DFU>> mapaDFUs = new HashMap<>();
        
        for (Entry<Integer, Map<Location, Set<Produto>>> entry1 : lowLevelCodeLocationsInternasEFornecedores.entrySet()) {
            Set<DFU> dfuSetParaLowLevelCode = new HashSet<>();
            
            mapaDFUs.put(entry1.getKey(), dfuSetParaLowLevelCode);
            
            for (Entry<Location,Set<Produto>> entry2 : entry1.getValue().entrySet()) {
                
                Location location = entry2.getKey();
                for (Produto material : entry2.getValue()) {
                    
                    dfuSetParaLowLevelCode.add(new DFU(material, location));
                    
                }
            }
            
        }     
        
        return mapaDFUs;
                
    }
    
    public OptionalInt getUltimoLowLevelCode() {
        
        return lowLevelCodeLocationsInternasEFornecedores.keySet().stream()
                .mapToInt(x -> x)
                .max();
        
    }
    
    public int getNumeroDFUsLowLevelCode(int lowLevelCode) {
        
        int numeroDFUs = (int) lowLevelCodeLocationsInternasEFornecedores
                .getOrDefault(lowLevelCode, new HashMap<>()).values().stream()
                .flatMap(x -> x.stream())
                .count();
        
        if (lowLevelCode == 1) {
            ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
            numeroDFUs += locationProjection.getLocationsAtivasSetComTiposLocation(
                    LocationAbstract.TipoLocation.CLIENTE_FINAL, LocationAbstract.TipoLocation.REGIAO_COMERCIAL).size();
        }
        
        return numeroDFUs;
        
    }
    
    /**
     * 1 : 1o nível em termos de proximidade da demanda (primeiro nível a planejar)
     * Clientes finais nao entram no conjunto cartesiano material/location porque ficam no primeiro nivel de demanda.
     * A rotina calcula os niveis seguintes apenas para locations internas, pontos de transbordo e fornecedores.
     * <p>Malha circular ou sem caminho de avanco falha como estado invalido no
     * metodo recursivo, porque a heuristica nao pode calcular dependencias de
     * abastecimento sem uma ordenacao topologica finita.</p>
     */
    public void atualizaMapaDFUsPorLowLevelCode() {
        
        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        
        Map<Integer,Set<DFU>> mapaDFUsOrdenadosPorLowLevelCode = new HashMap<>();
        
        Set<Location> locationsInternasEFornecedores = locationProjection.getLocationsAtivasSetComTiposLocation(
                LocationAbstract.TipoLocation.INTERNA, LocationAbstract.TipoLocation.PONTO_TRANSBORDO, LocationAbstract.TipoLocation.FORNECEDOR);
        Set<Produto> materiaisAtivos = materialProjection.getMateriaisAtivos();
        
        // cria todas as combinações de materiais com locations
        
        log.info("Gerando produto cartesiano de materiais e locations internas/fornecedores");
        Set<DFU> setDFUsLocationsInternasEFornecedores = AgregacaoDFU.getDFUListDeProdutoCartesianoLocationMaterial(
                locationsInternasEFornecedores, materiaisAtivos, supplyNetworkProjection.getClusterEParametrosProjection());
        log.info("{} DFUs de locations internas/fornecedores sendo processadas",
                setDFUsLocationsInternasEFornecedores.size());

        // roda passando uma cópia dos collections para evitar interferência
        // calcula do low level code 2 em diante
        mapaDFUsOrdenadosPorLowLevelCode = getDFUsOrdenadosPorLowLevelCode(
                0,
                setDFUsLocationsInternasEFornecedores,
                mapaDFUsOrdenadosPorLowLevelCode,
                supplyNetworkProjection,
                versaoMalha,
                dataHorarioReferenciaStatusMaterial);
        
        // cria o mapa lowLevelCode -> Location -> Materiais a partir do mapa lowLevelCode -> DFUs
        for (Integer lowLevelCode : mapaDFUsOrdenadosPorLowLevelCode.keySet()) {
            
            Set<DFU> dfuSetLowLevelCode = mapaDFUsOrdenadosPorLowLevelCode.get(lowLevelCode);
            
            // mapa interno de Location -> Set<Produto>, onde Produto e a entidade fisica de material
            Map<Location,Set<Produto>> mapaMateriaisPorLocation = dfuSetLowLevelCode.stream()
                    .collect(Collectors.groupingBy(
                            DFU::getLocation, 
                            Collectors.mapping(
                                    DFU::getProduto, 
                                    Collectors.toSet())));
            
            lowLevelCodeLocationsInternasEFornecedores.put(lowLevelCode, mapaMateriaisPorLocation);
            
        }
        
    }   
    /**
     * 1 : 1o nível em termos de proximidade da demanda (primeiro nível a planejar)
     * @param ultimoLowLevelCode
     * @param dfusRestantes
     * @param mapaDFUsOrdenadosPorLowLevelCode
     * @return mapa de DFUs por low level code.
     *
     * <p>Se duas iteracoes consecutivas nao reduzem a lista de DFUs restantes,
     * a malha contem ciclo ou caminho inconsistente para o recorte executado e
     * o metodo falha com {@link IllegalStateException}.</p>
     */
    private Map<Integer,Set<DFU>> getDFUsOrdenadosPorLowLevelCode(
            int ultimoLowLevelCode,
            Set<DFU> dfusRestantes,
            Map<Integer,Set<DFU>> mapaDFUsOrdenadosPorLowLevelCode,
            SupplyNetworkProjection supplyNetworkProjection,
            VersaoMalha versaoMalha,
            LocalDateTime dataHorarioReferenciaStatusMaterial) {
        
        // nenhum DFU restante a processar
        if (dfusRestantes.size() == 0) {
            return mapaDFUsOrdenadosPorLowLevelCode;
        }
                
        int lowLevelCodeAtual = ultimoLowLevelCode + 1;
        log.info("Low level Code {}", lowLevelCodeAtual);
        mapaDFUsOrdenadosPorLowLevelCode.computeIfAbsent(lowLevelCodeAtual, x -> new HashSet<>());
        
        ClusterEParametrosProjection clustersEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();
        
        Set<DFU> dfusARemover = new HashSet<>();

        for (DFU dfu : dfusRestantes) {

            // DFUs inativas não são consideradas
            if (!clustersEParametrosProjection.isDfuAtiva(dfu.getProduto(), dfu.getLocation())) {
                dfusARemover.add(dfu);
                continue;
            }
            
            Set<LinhaTransporte> linhasTransporteOutboundPrioritarias = supplyNetworkProjection.getLinhaTransportePrioritariaSetOutbound(
                    versaoMalha, 
                    dfu.getLocation(), 
                    dfu.getProduto(), 
                    dataHorarioReferenciaStatusMaterial,
                    locationProjection.getLocationsAtivasOuNuloSeLocationProjectionCompleto())
                    .stream()
                    .collect(Collectors.toSet());
            
            // otimização para 1o low level code, para evitar buscas desnecessárias em bases de lista técnica
            // locations que abastecem clientes só podem ser low level code 2 em diante
            // também importante pois as DFUs de clientes foram removidas de dfusRestantes : sem isso
            // as locations que abastecem clientes serão alocadas no nível 1 ao invés do nível 2
            if (linhasTransporteOutboundPrioritarias.stream()
                    .anyMatch(x -> x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL) || x.getLocationDestino().getTipoLocation().equals(LocationAbstract.TipoLocation.REGIAO_COMERCIAL))) {
                if (lowLevelCodeAtual == 1) {
                    continue;
                }
            }

            // checa se algum dos destinos da linha de transporte (p/ material da DFU)
            // ainda consta na lista de DFUs não processadas. caso afirmativo, vai para
            // próxima DFU
            if (linhasTransporteOutboundPrioritarias.stream()
                    .anyMatch(x -> dfusRestantes.contains(
                            new DFU(dfu.getProduto(), x.getLocationDestino())))) {
                continue;
            }

            // o material e input de alguma lista tecnica prioritaria
            // de outro material output que faz parte das dfusRestantes?
            Set<ListaTecnica> listasTecnicasOndeMaterialEInput = supplyNetworkProjection.getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                    dfu.getLocation(), dfu.getProduto(), 
                    false, // o low level code usa somente o recorte produtivo do heurístico
                    materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto());
            if (listasTecnicasOndeMaterialEInput.stream()
                    .anyMatch(x -> dfusRestantes.contains(
                            new DFU(x.getMaterialOutput(), dfu.getLocation())))) {
                continue;
            }
            
            // se a DFU não tiver nenhum sucessor a juzante : 
            // location outbound ou output de receita produção
            dfusARemover.add(dfu);
            mapaDFUsOrdenadosPorLowLevelCode.get(lowLevelCodeAtual).add(dfu);
        }
        
        // verifca se há circularidade
        if (mapaDFUsOrdenadosPorLowLevelCode.get(lowLevelCodeAtual).size() == 0 && lowLevelCodeAtual > 1) {
            
            Set<Location> locationsRestantes = dfusRestantes.parallelStream()
                    .map(x -> x.getLocation())
                    .collect(Collectors.toSet());
            
            String erro = "Circular network identified with " + dfusRestantes.size() + " remaining DFUs at " + locationsRestantes.size() + " remaining locations";
            throw new CircularNetworkException(erro, lowLevelCodeAtual, mapaDFUsOrdenadosPorLowLevelCode, dfusRestantes);
        }
        
        dfusRestantes.removeAll(dfusARemover);
                
        // chamada recursiva
        return getDFUsOrdenadosPorLowLevelCode(
                lowLevelCodeAtual, dfusRestantes, 
                mapaDFUsOrdenadosPorLowLevelCode,
                supplyNetworkProjection,
                versaoMalha,
                dataHorarioReferenciaStatusMaterial);
        
    }    
    
    public void atualizaDetalheErroCircularidade(
            CircularNetworkException e) {
        
        detalheErroCircularidade.clear();
        Set<DFU> dfusRestantes = new HashSet<>(e.getDfusRestantes());
        List<CircularNetworkEdge> circularNetworkEdges = getCircularNetworkEdges(dfusRestantes);
        List<Set<DFU>> circularComponents = getCircularComponents(dfusRestantes, circularNetworkEdges);

        int circularNetworkId = 1;
        for (Set<DFU> circularComponent : circularComponents) {
            int currentCircularNetworkId = circularNetworkId;
            circularNetworkEdges.stream()
                    .filter(edge -> circularComponent.contains(edge.origin()) && circularComponent.contains(edge.destination()))
                    .sorted(Comparator.comparing(CircularNetworkEdge::masterData)
                            .thenComparing(CircularNetworkEdge::masterDataId)
                            .thenComparing(edge -> getDfuKey(edge.origin())))
                    .forEach(edge -> detalheErroCircularidade.add(DFUMalhaCircularDTO.builder()
                            .masterData(edge.masterData())
                            .masterDataId(edge.masterDataId())
                            .lowLevelCode(e.getUltimoLowLevelCode())
                            .materialId(edge.origin().getProduto().getId())
                            .outputMaterialId(edge.masterData().equals("Bill of Materials")
                                    ? edge.destination().getProduto().getId()
                                    : null)
                            .circularNetworkId(currentCircularNetworkId)
                            .build()));
            circularNetworkId++;
        }
        
    }    

    /**
     * Monta somente as arestas prioritárias entre as DFUs que restaram quando
     * o algoritmo detectou circularidade. A montagem reaproveita as mesmas
     * consultas em memória do Low Level Code; não cria repository, join ou
     * nova projection.
     */
    private List<CircularNetworkEdge> getCircularNetworkEdges(Set<DFU> dfusRestantes) {

        List<CircularNetworkEdge> edges = new ArrayList<>();
        for (DFU dfu : dfusRestantes) {
            supplyNetworkProjection.getLinhaTransportePrioritariaSetOutbound(
                            versaoMalha,
                            dfu.getLocation(),
                            dfu.getProduto(),
                            dataHorarioReferenciaStatusMaterial,
                            locationProjection.getLocationsAtivasOuNuloSeLocationProjectionCompleto())
                    .forEach(linhaTransporte -> {
                        DFU destination = new DFU(dfu.getProduto(), linhaTransporte.getLocationDestino());
                        if (dfusRestantes.contains(destination)) {
                            edges.add(new CircularNetworkEdge(
                                    dfu,
                                    destination,
                                    "Transportation Line",
                                    linhaTransporte.getLocationOrigem().getId() + "-" + linhaTransporte.getLocationDestino().getId()));
                        }
                    });

            supplyNetworkProjection.getListaTecnicaViavelPrioritariaSetOndeMaterialEInput(
                            dfu.getLocation(),
                            dfu.getProduto(),
                            false,
                            materialProjection.getMateriaisAtivosOuNuloSeMaterialProjectionCompleto())
                    .forEach(listaTecnica -> {
                        DFU destination = new DFU(listaTecnica.getMaterialOutput(), dfu.getLocation());
                        if (dfusRestantes.contains(destination)) {
                            edges.add(new CircularNetworkEdge(
                                    dfu,
                                    destination,
                                    "Bill of Materials",
                                    listaTecnica.getId()));
                        }
                    });
        }
        return edges;

    }

    /**
     * Identifica componentes fortemente conectados no subgrafo restante. Um
     * componente com duas ou mais DFUs, ou um auto-loop, é uma circularidade
     * concreta e recebe um identificador único no DTO operacional.
     */
    private List<Set<DFU>> getCircularComponents(
            Set<DFU> dfusRestantes,
            List<CircularNetworkEdge> edges) {

        Map<DFU, List<DFU>> outboundByDfu = new HashMap<>();
        Map<DFU, List<DFU>> inboundByDfu = new HashMap<>();
        dfusRestantes.forEach(dfu -> {
            outboundByDfu.put(dfu, new ArrayList<>());
            inboundByDfu.put(dfu, new ArrayList<>());
        });
        edges.forEach(edge -> {
            outboundByDfu.get(edge.origin()).add(edge.destination());
            inboundByDfu.get(edge.destination()).add(edge.origin());
        });

        List<DFU> finishingOrder = new ArrayList<>();
        Set<DFU> visited = new HashSet<>();
        dfusRestantes.stream().sorted(Comparator.comparing(this::getDfuKey))
                .forEach(dfu -> collectFinishingOrder(dfu, outboundByDfu, visited, finishingOrder));

        List<Set<DFU>> circularComponents = new ArrayList<>();
        visited.clear();
        Collections.reverse(finishingOrder);
        for (DFU dfu : finishingOrder) {
            if (!visited.add(dfu)) {
                continue;
            }
            Set<DFU> component = new HashSet<>();
            collectComponent(dfu, inboundByDfu, visited, component);
            boolean hasSelfLoop = edges.stream().anyMatch(edge -> edge.origin().equals(dfu) && edge.destination().equals(dfu));
            if (component.size() > 1 || hasSelfLoop) {
                circularComponents.add(component);
            }
        }
        circularComponents.sort(Comparator.comparing(component -> component.stream()
                .map(this::getDfuKey).min(String::compareTo).orElseThrow()));
        return circularComponents;

    }

    private void collectFinishingOrder(
            DFU dfu,
            Map<DFU, List<DFU>> outboundByDfu,
            Set<DFU> visited,
            List<DFU> finishingOrder) {

        if (!visited.add(dfu)) {
            return;
        }
        outboundByDfu.get(dfu).stream().sorted(Comparator.comparing(this::getDfuKey))
                .forEach(destination -> collectFinishingOrder(destination, outboundByDfu, visited, finishingOrder));
        finishingOrder.add(dfu);

    }

    private void collectComponent(
            DFU dfu,
            Map<DFU, List<DFU>> inboundByDfu,
            Set<DFU> visited,
            Set<DFU> component) {

        component.add(dfu);
        inboundByDfu.get(dfu).stream().sorted(Comparator.comparing(this::getDfuKey))
                .filter(visited::add)
                .forEach(origin -> collectComponent(origin, inboundByDfu, visited, component));

    }

    private String getDfuKey(DFU dfu) {

        return dfu.getLocation().getId() + "\u0000" + dfu.getProduto().getId();

    }

    private record CircularNetworkEdge(
            DFU origin,
            DFU destination,
            String masterData,
            String masterDataId) {
    }
    
    public static void atualizaLowLevelCodeDTOComDecomposicaoQR(LowLevelCodeDTO lowLevelCodeDTO, String materialId) {
        
        List<LowLevelCodeNodeDTO> nodeDTOList = new ArrayList(lowLevelCodeDTO.nodeDTOSet);
        
        // equações na forma
        // 1 * levelNode1 = 1 * levelNode4 + 1
        // ou
        // 1 * levelNode1 - 1 * levelNode4 = 1
        // uma equação adicional determinando
        // 1 * levelNodeMaterial = 1
        double[][] matrizEquacoesLevelNodes = new double[lowLevelCodeDTO.edgeDTOSet.size() + 1][lowLevelCodeDTO.nodeDTOSet.size()];
        
        // 1a equação : level do material = 1
        int posicaoNodeMaterial = nodeDTOList.indexOf(LowLevelCodeNodeDTO.builder()
                .id("Material " + materialId)
                .build());
        matrizEquacoesLevelNodes[0][posicaoNodeMaterial] = 1;
        
        // equações para cada um dos edges. origem = destino + 1
        int posicaoEquacaoEdges = 1;
        for (LowLevelCodeEdgeDTO edgeDTO : lowLevelCodeDTO.edgeDTOSet) {
            int posicaoNodeOrigem = nodeDTOList.indexOf(LowLevelCodeNodeDTO.builder()
                .id(edgeDTO.from)
                .build());
            int posicaoNodeDestino = nodeDTOList.indexOf(LowLevelCodeNodeDTO.builder()
                .id(edgeDTO.to)
                .build());
            
            matrizEquacoesLevelNodes[posicaoEquacaoEdges][posicaoNodeOrigem] = 1;
            matrizEquacoesLevelNodes[posicaoEquacaoEdges][posicaoNodeDestino] = -1;
            
            posicaoEquacaoEdges++;
        }
        
        // constantes sempre = 1 (lado direito da equação)
        double[] arrayConstantes = new double[lowLevelCodeDTO.edgeDTOSet.size() + 1];
        MetodosUtilidade.setaArray(arrayConstantes, 1);
        
        RealMatrix coefficients = new Array2DRowRealMatrix(matrizEquacoesLevelNodes, false);
        RealVector constants = new ArrayRealVector(arrayConstantes, false);
        
        DecompositionSolver solver = new QRDecomposition(coefficients).getSolver();
        RealVector solution = solver.solve(constants);
        
        for (int i = 0; i < nodeDTOList.size() ; i++) {
            nodeDTOList.get(i).level = (int) Math.round(solution.getEntry(i));
        }
        
    }
}
