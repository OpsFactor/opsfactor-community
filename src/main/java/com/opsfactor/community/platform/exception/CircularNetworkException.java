package com.opsfactor.community.platform.exception;

import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Erro funcional usado quando o low level code identifica ciclo na malha.
 *
 * <p>A exception carrega o ultimo nivel calculado e as DFUs remanescentes para
 * facilitar diagnostico de cadastro sem recomputar a projection de rede.</p>
 */
public class CircularNetworkException extends RuntimeException {
    
    private int ultimoLowLevelCode;
    private Map<Integer,Set<DFU>> mapaDFUsOrdenadosPorLowLevelCode; 
    private Collection<DFU> dfusRestantes;
    
    public CircularNetworkException(
            String errorMessage, 
            int ultimoLowLevelCode, 
            Map<Integer,Set<DFU>> mapaDFUsOrdenadosPorLowLevelCode,
            Collection<DFU> dfusRestantes) {
        super(errorMessage);
        this.ultimoLowLevelCode = ultimoLowLevelCode;
        this.mapaDFUsOrdenadosPorLowLevelCode = mapaDFUsOrdenadosPorLowLevelCode;
        this.dfusRestantes = dfusRestantes;
    }
    
    public int getUltimoLowLevelCode() {
        return ultimoLowLevelCode;
    }
    
    public Collection<DFU> getDfusRestantes() {
        return dfusRestantes;
    }
    
    public Map<Integer,Set<DFU>> getMapaDFUsOrdenadosPorLowLevelCode() {
        return mapaDFUsOrdenadosPorLowLevelCode;
    }
}
