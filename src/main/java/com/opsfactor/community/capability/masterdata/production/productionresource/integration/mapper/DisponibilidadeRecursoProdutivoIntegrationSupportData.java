package com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;

import java.util.Map;

/**
 * Dados auxiliares para conversao de disponibilidade diaria de recurso
 * produtivo.
 *
 * <p>O mapper precisa apenas localizar o recurso produtivo operacional.
 * Unidades de medida, turnos e calendarios detalhados sao deliberately ausentes
 * neste support data Community.</p>
 */
public class DisponibilidadeRecursoProdutivoIntegrationSupportData {

    public Map<String, RecursoProdutivo> mapaRecursoProdutivoPorId;

}
