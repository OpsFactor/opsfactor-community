package com.opsfactor.community.capability.supplyplanning.service.spi;

/**
 * SPI implementada pelo Enterprise para process chains de Supply Planning.
 *
 * <p>A implementacao Enterprise deve orquestrar as etapas e delegar para os
 * services especificos, sem permitir process chains aninhados. Diferente de um
 * motor atomico, a process chain pode ser chamada antes da montagem das
 * projections compartilhadas: ela expande a cadeia e cada etapa chama o fluxo
 * principal novamente com um perfil {@code HEURISTICO} ou {@code OTIMIZADOR},
 * garantindo que filtros, malha, capacidade e politica de estoque sejam
 * preparados com os parametros da propria etapa.</p>
 */
public interface SupplyPlanProcessChainServiceSpi extends SupplyPlanExecutionServiceSpi {
}
