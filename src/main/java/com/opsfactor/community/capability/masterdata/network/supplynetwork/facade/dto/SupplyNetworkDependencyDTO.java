package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/** Common contract for every vertex returned by the supply-network explorer. */
public abstract class SupplyNetworkDependencyDTO {

    public enum ElementType {
        @JsonProperty("Material-Location") MATERIAL_LOCATION,
        @JsonProperty("Production Version") PRODUCTION_VERSION,
        @JsonProperty("Routing-Bom Combination") ROUTING_BOM_COMBINATION,
        @JsonProperty("Bill of Materials") BILL_OF_MATERIALS,
        @JsonProperty("Routing") ROUTING,
        @JsonProperty("Production Resource") PRODUCTION_RESOURCE,
        @JsonProperty("Transportation Line") TRANSPORTATION_LINE
    }

    /**
     * Resultado lógico: VIABLE, BLOCKED ou UNKNOWN. Independente de expansão visual;
     * UNKNOWN comunica análise inconclusiva e nunca deve ser apresentado como verde.
     */
    public String viabilityStatus;
    /** COMPLETE, CYCLE_REFERENCE, SHARED_REFERENCE, DEPTH_LIMIT ou DISPLAY_LIMIT. */
    public String expansionStatus;
    /** ID canônico do material/location no snapshot; não depende do caminho visual. */
    public String nodeId;
    /** Identidade da alternativa de produção/inbound, quando aplicável a este estágio. */
    public String alternativeId;
    /** Alvo canônico de uma referência terminal; substitui ponteiros Java a outros DTOs. */
    public String referenceNodeId;
    /** SCC circular identificada na análise; presença do grupo não determina viabilidade. */
    public String cycleGroupId;
    /** Motivo do corte desta ocorrência; não substitui analysisCutReason do material/location. */
    public String cutReason;
    public String inspectionMessage;
    /**
     * Defeitos locais preservados inclusive em referências e cortes, mesmo se houver
     * outra alternativa válida. Mantêm cadastros remediáveis visíveis dentro de ciclos.
     */
    public List<String> localBlockingReasons = new ArrayList<>();

    /** Compatibilidade: true para VIABLE, false para BLOCKED e null para UNKNOWN. */
    public Boolean viableStep;
    public ElementType elementType;
}
