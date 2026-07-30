package com.opsfactor.community.capability.lowlevelcode.facade.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;

/**
 * Grafo tecnico de low level code retornado pelo Supply Planning Community.
 *
 * <p>O grafo mostra o caminho operacional por materiais, locations, roteiros,
 * listas tecnicas e linhas inbound prioritarias. Ele nao carrega custos,
 * fretes, flows otimizados, constraint tracking ou diagnostico de solver.</p>
 */
@Getter
public class LowLevelCodeDTO {

    /** Nodes do grafo tecnico. */
    public Set<LowLevelCodeNodeDTO> nodeDTOSet = new HashSet<>();

    /** Edges direcionais do grafo tecnico. */
    public Set<LowLevelCodeEdgeDTO> edgeDTOSet = new HashSet<>();

    /**
     * Atualiza o nivel de todos os nodes a partir das edges existentes.
     */
    public void atualizaLevels() {

        for (LowLevelCodeNodeDTO nodeDTO : nodeDTOSet) {
            nodeDTO.setLevel(getLevel(nodeDTO));
        }

    }

    /**
     * Calcula o nivel de um node individual dentro do grafo atual.
     */
    public int getLevel(LowLevelCodeNodeDTO nodeDTO) {

        return getLevel(1, nodeDTO.getId(), edgeDTOSet);

    }

    /**
     * Caminha recursivamente pelas edges que saem do node atual.
     *
     * <p>A cada chamada removemos as edges ja exploradas para evitar loop
     * infinito caso o grafo recebido contenha ciclo.</p>
     */
    private int getLevel(int levelAtual, String idNodeDTO, Set<LowLevelCodeEdgeDTO> edgesRestantes) {

        Set<LowLevelCodeEdgeDTO> edgesComOrigemNoNode = edgesRestantes.stream()
                .filter(x -> x.from.equals(idNodeDTO))
                .collect(Collectors.toSet());
        
        Set<LowLevelCodeEdgeDTO> edgesRestantesProximaIteracao = new HashSet<>(edgesRestantes);
        edgesRestantesProximaIteracao.removeAll(edgesComOrigemNoNode);
        
        int levelFinal = levelAtual;
        for (LowLevelCodeEdgeDTO edgeDTO : edgesComOrigemNoNode) {
            int levelEdge = getLevel(levelAtual + 1, edgeDTO.to, edgesRestantesProximaIteracao);
            levelFinal = Math.max(levelAtual, levelEdge);
        }
        
        return levelFinal;

    }
                
}
