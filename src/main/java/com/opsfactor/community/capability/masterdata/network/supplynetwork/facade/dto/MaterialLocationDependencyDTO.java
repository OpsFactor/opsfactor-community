package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/** Material-location node and its inbound and production alternatives. */
public class MaterialLocationDependencyDTO extends SupplyNetworkDependencyDTO {

    public String materialId;
    public String locationId;
    /** Gate cadastral local; atividade isolada não comprova fonte de abastecimento. */
    public Boolean active;
    /** OR das alternativas produtivas: null se não há prova e ainda existe alternativa UNKNOWN. */
    public Boolean viableProduction;
    /** OR das alternativas inbound, com a mesma semântica trivalente da produção. */
    public Boolean viableInbound;
    /** Compatibilidade visual: true quando o ramo não é expandido; não é um status de viabilidade. */
    public Boolean recursionCut;
    /** Completude da análise da requisição: COMPLETE ou PARTIAL, independente da profundidade visual. */
    public String analysisStatus;
    /** Motivo do orçamento/cancelamento estrutural; distinto de DEPTH_LIMIT e cortes da apresentação. */
    public String analysisCutReason;
    /** A descoberta/resolução não terminou; status individuais já comprovados podem permanecer conclusivos. */
    public Boolean analysisTruncated;
    /** A resposta visual foi limitada em alguma ocorrência, sem invalidar a análise estrutural. */
    public Boolean displayTruncated;
    /** Quantidades do grafo lógico; não equivalem à contagem dos estágios DTO exibidos. */
    public Integer analyzedNodeCount;
    public Integer analyzedEdgeCount;
    /** Orçamento estrutural por requisição: nós, incidências e tempo. */
    public Integer maximumAnalysisNodes;
    public Integer maximumAnalysisEdges;
    public Long maximumAnalysisMillis;
    /** Orçamento independente da exibição: estágios DTO, ligações e profundidade aninhada. */
    public Integer maximumDisplayNodes;
    public Integer maximumDisplayEdges;
    public Integer maximumDisplayDepth;
    /** Profundidade solicitada e efetivamente aceita após o teto de proteção da serialização. */
    public Integer requestedTreeDepth;
    public Integer effectiveTreeDepth;
    /** Seleção original e raízes realmente retornadas; comunica corte quando a seleção é extensa. */
    public Integer requestedRootCount;
    public Integer returnedRootCount;

    /** Distância visual em material/location a partir da raiz; estágios intermediários não a incrementam. */
    public Integer depth;
    public Set<ProductionVersionDependencyDTO> productionVersionDependencies = new LinkedHashSet<>();
    public Set<TransportationLineDependencyDTO> inboundTransportationLineDependencies = new LinkedHashSet<>();
}
