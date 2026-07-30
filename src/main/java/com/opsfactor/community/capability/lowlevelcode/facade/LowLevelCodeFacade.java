package com.opsfactor.community.capability.lowlevelcode.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.location.domain.LocationAbstract;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.operation.domain.OperacaoRoteiro;
import com.opsfactor.community.capability.masterdata.production.routing.domain.Roteiro;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjectionFactory;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.DFUMalhaCircularDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeEdgeDTO;
import com.opsfactor.community.capability.lowlevelcode.facade.dto.LowLevelCodeNodeDTO;
import com.opsfactor.community.platform.exception.CircularNetworkException;
import com.opsfactor.community.capability.lowlevelcode.engine.LowLevelCode;
import com.opsfactor.community.capability.masterdata.demand.dfu.facade.mapper.DFUAutoMapper;
import com.opsfactor.community.platform.exception.SupplyPlanException;
import com.opsfactor.community.capability.masterdata.demand.dfu.facade.dto.DFUDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servico front para visualizacao tecnica de low level code no Supply Planning.
 *
 * <p>No Community, esta visualizacao ajuda a explicar a explosao heuristica da
 * malha, BOM e roteiros usados pelo plano. Ela nao representa Supply Network
 * Flows Enterprise, mapa geografico, frete, custos, line scheduling ou parallel
 * routing/output.</p>
 */
@Service
@Slf4j
public class LowLevelCodeFacade {

    /**
     * Factory da malha operacional usada pelo Supply Planning Community.
     * Mantem o calculo em projections indexadas, evitando consultas JPA dentro
     * da recursao do low level code.
     */
    @Autowired
    private SupplyNetworkProjectionFactory supplyNetworkProjectionFactory;

    /**
     * Factory dos parametros globais, materiais e locations ativos. O low
     * level code precisa da visao completa para caminhar por insumos, WIP e
     * origens alternativas sem voltar ao banco a cada DFU.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;

    /**
     * Repositorio da versao de malha selecionada pelo usuario.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Repositorio do material raiz consultado no grafo tecnico.
     */
    @Autowired
    private ProdutoRepository produtoRepository;    

    /**
     * Mapper das DFUs calculadas em memoria para o DTO consumido pelo front.
     */
    @Autowired
    private DFUAutoMapper dfuAutoMapper;

    /**
     * Calcula a distribuicao de DFUs por low level code de uma versao de malha.
     *
     * <p>Este endpoint auxilia a verificacao tecnica do motor heuristico. Ele
     * nao materializa Supply Network Flows, custos, fretes nem explicabilidade
     * de restricoes Enterprise.</p>
     */
    public Map<Integer,Set<DFUDTO>> getLowLevelCodePorDFU(String versaoMalhaId, LocalDateTime dataReferencia) {

        validaVersaoMalhaLowLevelCodeCommunity(versaoMalhaId);
        if (dataReferencia == null) {
            throw new IllegalArgumentException("Low Level Code reference date is required");
        }

        VersaoMalha versaoMalha = versaoMalhaRepository.findById(versaoMalhaId).get();
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        MaterialProjection materialProjection = MaterialProjectionFactory.getMaterialProjectionCompleto(clusterEParametrosProjection);
        LocationProjection locationProjection = LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);
        
        log.info("Iniciando geração do Mapa DFUs");
        LowLevelCode lowLevelCode = new LowLevelCode(supplyNetworkProjection, versaoMalha, materialProjection, locationProjection, dataReferencia);
        lowLevelCode.atualizaMapaDFUsPorLowLevelCode();
        log.info("Mapa DFUs por LLC gerado. Convertendo para DTO");
        return dfuAutoMapper.converteMapaDFUParaMapaDTO(lowLevelCode.getMapaDFUsPorLowLevelCode());
    }

    /**
     * Monta o grafo tecnico de abastecimento/producao para um material.
     *
     * <p>A resposta mostra nodes e edges de locations, materiais, roteiros,
     * recursos produtivos e listas tecnicas. O Community usa apenas a versao de
     * producao simples prioritaria e a linha inbound prioritaria; parallel
     * routing/output e analises de rede/custos ficam no Enterprise.</p>
     */
    public LowLevelCodeDTO getLowLevelCodeDTO(String versaoMalhaId, String materialId) {

        validaVersaoMalhaLowLevelCodeCommunity(versaoMalhaId);
        validaMaterialLowLevelCodeCommunity(materialId);

        VersaoMalha versaoMalha = versaoMalhaRepository.findById(versaoMalhaId).get();
        
        Produto material = produtoRepository.findById(materialId).get();
        
        LocalDateTime dataAtual = LocalDateTime.now();

        // traz parametros e malha completa para todos os materiais e location
        // importante para poder processar insumos e insumos de insumos
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();
        
        LowLevelCodeDTO lowLevelCodeDTO = new LowLevelCodeDTO();

        MaterialProjection materialProjection = MaterialProjectionFactory.getProjectionUnicoMaterial(material, clusterEParametrosProjection);
        LocationProjection locationProjection = LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);

        // low level codes para este SKU
        LowLevelCode lowLevelCode = new LowLevelCode(supplyNetworkProjection, versaoMalha, materialProjection, locationProjection, dataAtual);
        lowLevelCode.atualizaMapaDFUsPorLowLevelCode();

        // ponto de partida da recursão : todas as DFUs de 1o nível 
        // onde o planejamento DP está habilitado (fontes de demanda)
        validaMaterialAtivoEmAlgumaLocationParaLowLevelCodeCommunity(
                material,
                lowLevelCode.getLowLevelCodes());
        Set<Location> locationsPrimeiroLowLevelCode = lowLevelCode.getLocationsLowLevelCode(1);
        
        Set<DFU> dfusProcessadas = new HashSet<>();
        
        // adiciona primeiro node : material
        LowLevelCodeNodeDTO lowLevelCodeNodeDTOMaterial = LowLevelCodeNodeDTO.builder()
                .tipo("Material")
                .id("Material " + material.getId())
                .label("Material " + material.getId())
                .build();
        lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOMaterial);
        
        // roda a recursão para cada DFU de 1o nível
        for (Location location : locationsPrimeiroLowLevelCode) {
            
            // Adiciona ligacao location de demanda -> material.
            LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOMaterialLocation = LowLevelCodeEdgeDTO.builder()
                    .from("Location " + location.getId())
                    .to("Material " + material.getId())
                    .build();
            lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOMaterialLocation);
            
            Optional<LowLevelCodeDTO> optionalLowLevelCodeDTORecursao = getLowLevelCodeDTO(
                    versaoMalha,
                    material, location,
                    supplyNetworkProjection,
                    dfusProcessadas,
                    false,
                    dataAtual);
            optionalLowLevelCodeDTORecursao.ifPresent(lowLevelCodeDTORecursao -> {
                lowLevelCodeDTO.nodeDTOSet.addAll(lowLevelCodeDTORecursao.nodeDTOSet);
                lowLevelCodeDTO.edgeDTOSet.addAll(lowLevelCodeDTORecursao.edgeDTOSet);
            });
            
        }
        
        if (!clusterEParametrosProjection.getParametrosGlobais().getExibeLocationsClienteFinalLowLevelCode()) {
            
            Set<String> locationClienteIdSet = locationProjection.getLocationsAtivasSetComTiposLocation(
                    LocationAbstract.TipoLocation.CLIENTE_FINAL)
                    .stream()
                    .map(x -> "Location " + x.getId())
                    .collect(Collectors.toSet());

            lowLevelCodeDTO.nodeDTOSet = lowLevelCodeDTO.nodeDTOSet.stream()
                    .filter(x -> !locationClienteIdSet.contains(x.id))
                    .collect(Collectors.toSet());
            
            lowLevelCodeDTO.edgeDTOSet = lowLevelCodeDTO.edgeDTOSet.stream()
                    .filter(x -> !locationClienteIdSet.contains(x.to))
                    .collect(Collectors.toSet());
                        
        }
        
        /*
         * Os niveis sao calculados depois da limpeza de cliente final para que
         * o layout do front reflita exatamente o grafo exibido ao usuario. O
         * Community nao aplica filtros adicionais de lead time/custo aqui: a
         * aresta tecnica ajuda a explicar a malha usada pelo heuristico.
         */
        lowLevelCodeDTO.atualizaLevels();
        return lowLevelCodeDTO;
    }

    /**
     * Valida a versao de malha recebida pela visualizacao de Low Level Code.
     *
     * <p>A versao de malha e a raiz do grafo tecnico. Ausencia ou texto em
     * branco deve falhar antes de repositories e projections.</p>
     */
    private void validaVersaoMalhaLowLevelCodeCommunity(String versaoMalhaId) {

        if (versaoMalhaId == null || versaoMalhaId.isBlank()) {
            throw new IllegalArgumentException("Supply Network Version is null or empty");
        }

    }

    /**
     * Valida o material raiz da visualizacao material-especifica.
     */
    private void validaMaterialLowLevelCodeCommunity(String materialId) {

        if (materialId == null || materialId.isBlank()) {
            throw new IllegalArgumentException("Material Id is null or empty");
        }

    }

    /**
     * Carrega a versao de malha usada pelo diagnostico Community de Low Level
     * Code.
     *
     * <p>`Optional.empty()` preserva a semantica funcional de cadastro ausente.
     * `Optional` nulo vindo do repository indica contrato Spring Data quebrado e
     * precisa falhar antes de projections de parametros, malha ou DFU.</p>
     */
    

    /**
     * Carrega o material raiz do grafo de Low Level Code.
     *
     * <p>Material inexistente continua erro funcional da chamada. Retorno nulo
     * no lugar de `Optional` e erro estrutural de repository e deve parar antes
     * de MaterialProjectionFactory, recursao de BOM ou montagem parcial de
     * nodes/edges para o front.</p>
     */
    

    /**
     * Valida que a rotina de low level code encontrou ao menos uma location
     * ativa para o material solicitado.
     *
     * <p>O Community nao possui fallback Enterprise de rede, mapa ou analise
     * avancada para explicar material fora da malha. Se a rotina nao encontrou
     * nenhum low level code, o grafo tecnico nao tem ponto de partida valido e
     * deve falhar antes de criar nodes/edges parciais para o front.</p>
     */
    private void validaMaterialAtivoEmAlgumaLocationParaLowLevelCodeCommunity(
            Produto material,
            Set<Integer> lowLevelCodes) {

        if (lowLevelCodes.isEmpty()) {
            throw new SupplyPlanException(
                    "Selected material " + material.getId() + " is not active in any location");
        }

    }

    /**
     * Garante que o snapshot central de parametros/master data existe antes do
     * calculo de Low Level Code.
     *
     * <p>MaterialProjectionFactory e LocationProjectionFactory assumem que a
     * projection contem {@link ParametrosGlobais}. Sem essa validacao, uma
     * falha de cache/factory apareceria mais tarde como {@link NullPointerException}
     * dentro do calculo recursivo, dificultando diferenciar problema de
     * bootstrap Community de lacuna funcional Enterprise.</p>
     */
    /**
     * Garante que a projection de malha esta completa o suficiente para o
     * caminho Community de Low Level Code.
     *
     * <p>A recursao usa a projection para resolver roteiros, BOMs, linhas
     * inbound e tambem le {@link ParametrosGlobais} a partir da propria
     * projection de malha. Por isso a validacao inclui o snapshot de parametros
     * embutido nela, mesmo quando a chamada publica tambem carrega
     * {@link ClusterEParametrosProjection} separadamente para Material/Location
     * projections.</p>
     */
    /**
     * Retorna recursivamente o caminho heuristico para um par material/location.
     *
     * <p>O metodo mantem {@code dfusProcessadas} como controle local da
     * chamada para impedir loop infinito em malha circular. O service nao
     * guarda estado em campo, entao chamadas paralelas do front permanecem
     * isoladas.</p>
     *
     * @return grafo parcial do caminho encontrado ou vazio quando a DFU ja foi
     * processada na mesma recursao.
     */
    private Optional<LowLevelCodeDTO> getLowLevelCodeDTO(
            VersaoMalha versaoMalha,
            Produto material, Location location, 
            SupplyNetworkProjection supplyNetworkProjection, 
            Set<DFU> dfusProcessadas,
            boolean explosaoDeListaTecnica,
            LocalDateTime dataHorarioReferenciaStatusMateriais) {
              
        // não permite loop infinito em caso de circularidade, parando o processo
        if (dfusProcessadas.contains(new DFU(material, location))) return Optional.empty();
        dfusProcessadas.add(new DFU(material, location));      
                
        ParametrosGlobais parametrosGlobais = supplyNetworkProjection.getClusterEParametrosProjection().getParametrosGlobais();
        
        LowLevelCodeDTO lowLevelCodeDTO = new LowLevelCodeDTO();
        
        // Location : sempre insere. se for duplicado não há problema pois se insere em um Set
        if (location.getTipoLocation().equals(LocationAbstract.TipoLocation.CLIENTE_FINAL)) {
            LowLevelCodeNodeDTO lowLevelCodeNodeDTOLocation = LowLevelCodeNodeDTO.builder()
                    .tipo("Client")
                    .id("Location " + location.getId())
                    .label("Client " + location.getId())
                    .build();
            lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOLocation);
        } else {
            LowLevelCodeNodeDTO lowLevelCodeNodeDTOLocation = LowLevelCodeNodeDTO.builder()
                    .tipo("Location")
                    .id("Location " + location.getId())
                    .label("Location " + location.getId())
                    .build();
            lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOLocation);
        }

        /*
         * Parallel routing/output e capacidade Enterprise. Mesmo nesta
         * visualizacao auxiliar, o Community considera somente a versao de
         * producao simples prioritaria.
         */
        boolean consideraVersoesProducaoParalelas = false;
        Set<Roteiro> roteirosPrioritarios = supplyNetworkProjection.getRoteirosDeVersaoProducaoPrioritaria(
                location, 
                material, 
                consideraVersoesProducaoParalelas,
                null);
        
        if (!roteirosPrioritarios.isEmpty()) {
            
            for (Roteiro roteiroPrioritario : roteirosPrioritarios) {

                // Production Routing Operation
                String idFrontRoteiro = "Routing " + roteiroPrioritario.getId();
                LowLevelCodeNodeDTO lowLevelCodeNodeDTOReceitaOperacao = LowLevelCodeNodeDTO.builder()
                    .tipo("Production Routing Operation")
                    .id(idFrontRoteiro)
                    .label(idFrontRoteiro)
                    .build();
                lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOReceitaOperacao);

                // em explosão de LT, se 'aproveita' o gancho da location atual e se apresenta o roteiro
                // diretamente após o insumo
                if (explosaoDeListaTecnica) {
                    LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOReceitaOperacao = LowLevelCodeEdgeDTO.builder()
                            .from(idFrontRoteiro)
                            .to("Material " + material.getId() + " - Location " + location.getId())
                            .build();
                    lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOReceitaOperacao);
                // em outras situações, o roteiro deve apontar para a location em questão
                } else {
                    LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOReceitaOperacao = LowLevelCodeEdgeDTO.builder()
                            .from(idFrontRoteiro)
                            .to("Location " + location.getId())
                            .build();
                    lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOReceitaOperacao);
                }

                for (OperacaoRoteiro operacaoRoteiro : roteiroPrioritario.getOperacaoRoteiroListOrdenadaPorPosicaoAsc()) {

                    // Production Routing Operation
                    String idFrontOperacaoRoteiro = "Operation " + operacaoRoteiro.getPosicao();
                    LowLevelCodeNodeDTO lowLevelCodeNodeDTOOperacaoRoteiro = LowLevelCodeNodeDTO.builder()
                        .tipo("Production Routing Operation")
                        .id(idFrontRoteiro + " - " + idFrontOperacaoRoteiro)
                        .label(idFrontOperacaoRoteiro)
                        .build();
                    lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOOperacaoRoteiro);

                    LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOOperacaoRoteiro = LowLevelCodeEdgeDTO.builder()
                            .from(idFrontRoteiro + " - " + idFrontOperacaoRoteiro)
                            .to(idFrontRoteiro)//"Production Resource " + recursoProdutivo.getId())
                            .build();
                    lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOOperacaoRoteiro);

                    String idRecursoProdutivo = "Production Resource " + operacaoRoteiro.getRecursoProdutivo().getId();
                    LowLevelCodeNodeDTO lowLevelCodeNodeDTORecursoProdutivo = LowLevelCodeNodeDTO.builder()
                        .tipo("Production Resource")
                        .id(idRecursoProdutivo)
                        .label(idRecursoProdutivo)
                        .build();
                    lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTORecursoProdutivo);

                    LowLevelCodeEdgeDTO lowLevelCodeEdgeDTORecursoProdutivo = LowLevelCodeEdgeDTO.builder()
                            .from(idRecursoProdutivo)
                            .to(idFrontRoteiro + " - " + idFrontOperacaoRoteiro)
                            .build();
                    lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTORecursoProdutivo);                
                }

                // lista técnica
                Set<ListaTecnica> listasTecnicasPrioritarias = supplyNetworkProjection.getListasTecnicasDeVersaoProducaoPrioritaria(
                        location, 
                        material, 
                        consideraVersoesProducaoParalelas,
                        null);

                if (!listasTecnicasPrioritarias.isEmpty()) {
                    
                    for (ListaTecnica listaTecnicaPrioritaria : listasTecnicasPrioritarias) {
                    
                        String idListaTecnica = "Bill of Materials " + listaTecnicaPrioritaria.getId();
                        LowLevelCodeNodeDTO lowLevelCodeNodeDTOListaTecnica = LowLevelCodeNodeDTO.builder()
                            .tipo("Bill of Materials")
                            .id(idListaTecnica)
                            .label(idListaTecnica)
                            .build();
                        lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOListaTecnica);

                        LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOListaTecnica = LowLevelCodeEdgeDTO.builder()
                                .from(idListaTecnica)
                                .to(idFrontRoteiro)
                                .build();
                        lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOListaTecnica);

                        // RECURSÃO para cada um dos materiais filhos
                        for (ListaTecnicaComponente listaTecnicaComponente : listaTecnicaPrioritaria.getListaTecnicaComponenteSet()) {

                            String materialId = "Material " + listaTecnicaComponente.getMaterialComponente().getId() + " - Location " + location.getId();
                            LowLevelCodeNodeDTO lowLevelCodeNodeDTOListaTecnicaComponente = LowLevelCodeNodeDTO.builder()
                                .tipo("Material")
                                .id(materialId)
                                .label("Material " + listaTecnicaComponente.getMaterialComponente().getId())
                                .build();
                            lowLevelCodeDTO.nodeDTOSet.add(lowLevelCodeNodeDTOListaTecnicaComponente);

                            LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOListaTecnicaComponente = LowLevelCodeEdgeDTO.builder()
                                    .from(materialId)
                                    .to(idListaTecnica)
                                    .label(listaTecnicaComponente.getQuantidade() + listaTecnicaComponente.getUnidadeMedidaMaterialComponente(parametrosGlobais).getId() +
                                            " of component for each " + listaTecnicaPrioritaria.getQuantidade() + listaTecnicaPrioritaria.getUnidadeMedidaMaterialOutput(parametrosGlobais).getId() + " of output")
                                    .build();
                            lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOListaTecnicaComponente);

                            // para cada componente LT roda sua própria malha (chamada recursiva)
                            Optional<LowLevelCodeDTO> optionalLowLevelCodeDTOInput = getLowLevelCodeDTO(
                                    versaoMalha,
                                    listaTecnicaComponente.getMaterialComponente(), location, 
                                    supplyNetworkProjection, dfusProcessadas,
                                    true, dataHorarioReferenciaStatusMateriais);
                            optionalLowLevelCodeDTOInput.ifPresent(lowLevelCodeDTOInput -> {
                                lowLevelCodeDTO.nodeDTOSet.addAll(lowLevelCodeDTOInput.nodeDTOSet);
                                lowLevelCodeDTO.edgeDTOSet.addAll(lowLevelCodeDTOInput.edgeDTOSet);
                            });

                        }

                    }         
                }
            }
        } else {
            // Não há roteiro : se buscam linhas de transporte inbound para abastecer location
            Optional<LinhaTransporte> linhaTransporteInboundPrioritaria = supplyNetworkProjection.getLinhaTransporteViavelPrioritariaInbound(
                    versaoMalha, 
                    location, 
                    material, 
                    dataHorarioReferenciaStatusMateriais,
                    null);

            if (linhaTransporteInboundPrioritaria.isPresent()) {

                LinhaTransporte linhaTransporteInbound = linhaTransporteInboundPrioritaria
                        .orElseThrow(() -> new SupplyPlanException(
                                "Internal error resolving priority inbound lane for Low Level Code."));
                int leadTimeEmDiasOrigemPrioritaria = supplyNetworkProjection
                .getLeadTimeEmDiasDeOrigemPrioritaria(
                        versaoMalha,
                        location,
                        material,
                        dataHorarioReferenciaStatusMateriais,
                        null)
                .orElseThrow(() -> new SupplyPlanException(
                        "Lead time not found for priority inbound origin in Low Level Code for material "
                                + material.getId()
                                + " and destination location "
                                + location.getId() + "."));

                // primeira 'aparição' do material : seta apontando da origem para o código do material
                // única ocorrência de DFU do material é sua explosão de lista técnica no centro de origem
                if (explosaoDeListaTecnica) {
                    LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOListaTecnica = LowLevelCodeEdgeDTO.builder()
                            .from("Location " + linhaTransporteInbound.getLocationOrigem().getId())
                            .to("Material " + material.getId() + " - Location " + location.getId())
                            .label("Lead Time : " + leadTimeEmDiasOrigemPrioritaria)
                            .build();
                    lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOListaTecnica);
                // caso contrário : seta apontando da location origem para location destino
                } else {
                    LowLevelCodeEdgeDTO lowLevelCodeEdgeDTOListaTecnica = LowLevelCodeEdgeDTO.builder()
                            .from("Location " + linhaTransporteInbound.getLocationOrigem().getId())
                            .to("Location " + linhaTransporteInbound.getLocationDestino().getId())
                            .label("Lead Time : " + leadTimeEmDiasOrigemPrioritaria)
                            .build();
                    lowLevelCodeDTO.edgeDTOSet.add(lowLevelCodeEdgeDTOListaTecnica);
                }

                // realiza chamada recursiva para processar o material na nova location (origem)
                Optional<LowLevelCodeDTO> optionalLowLevelCodeDTOLocationOrigemLinhaTransporte = getLowLevelCodeDTO(
                        versaoMalha,
                        material, linhaTransporteInbound.getLocationOrigem(),
                        supplyNetworkProjection, dfusProcessadas,
                        false,
                        dataHorarioReferenciaStatusMateriais);

                optionalLowLevelCodeDTOLocationOrigemLinhaTransporte.ifPresent(lowLevelCodeDTOLocationOrigemLinhaTransporte -> {
                    lowLevelCodeDTO.nodeDTOSet.addAll(lowLevelCodeDTOLocationOrigemLinhaTransporte.nodeDTOSet);
                    lowLevelCodeDTO.edgeDTOSet.addAll(lowLevelCodeDTOLocationOrigemLinhaTransporte.edgeDTOSet);
                });

            }
        }   
        
        return Optional.of(lowLevelCodeDTO);
        
    }

    /**
     * Resolve o lead time exibido no grafo de Low Level Code para a origem
     * inbound prioritaria.
     *
     * <p>A linha inbound prioritaria ja foi encontrada; se o lead time nao
     * estiver disponivel, o grafo visual ficaria enganoso. O Community falha
     * explicitamente em vez de desenhar a aresta com prazo vazio ou zero.</p>
     */
    /**
     * Retorna detalhes de circularidade de malha, quando o calculo de low level
     * code identifica ciclo.
     *
     * <p>Esse diagnostico e operacional Community e serve para corrigir a
     * malha. Ele nao substitui Constraint Tracker/root cause Enterprise.</p>
     */
    public Set<DFUMalhaCircularDTO> getDFUMalhaCircularDTOSet (String versaoMalhaId, LocalDateTime dataReferencia) {

        validaVersaoMalhaLowLevelCodeCommunity(versaoMalhaId);
        if (dataReferencia == null) {
            throw new IllegalArgumentException("Low Level Code circular-network reference date is required");
        }

        VersaoMalha versaoMalha = versaoMalhaRepository.findById(versaoMalhaId).get();
        
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        SupplyNetworkProjection supplyNetworkProjection = supplyNetworkProjectionFactory.getSupplyNetworkProjectionCompletoDeCache();

        MaterialProjection materialProjection = MaterialProjectionFactory.getMaterialProjectionCompleto(clusterEParametrosProjection);
        LocationProjection locationProjection = LocationProjectionFactory.getLocationProjectionCompleto(clusterEParametrosProjection);
        
        LowLevelCode lowLevelCode = new LowLevelCode(supplyNetworkProjection, versaoMalha, materialProjection, locationProjection, dataReferencia);
        
        try {
            lowLevelCode.atualizaMapaDFUsPorLowLevelCode();
        } catch (CircularNetworkException e) {
            lowLevelCode.atualizaDetalheErroCircularidade(e);
            return lowLevelCode.getDetalheErroCircularidade();
        }
        
        return new HashSet<>();
        
    } 
}
