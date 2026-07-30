package com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto;

/**
 * Tipo historico de caracteristica recebido em DTOs compartilhados.
 *
 * <p>No Community nao existe cadastro funcional de caracteristicas dinamicas
 * de material/location. Este enum permanece apenas para manter o contrato de
 * payload do front compartilhado e permitir que services rejeitem o uso real da
 * feature com {@code RequiresEnterpriseVersionException}.</p>
 */
public enum TipoCaracteristicaDTO {
    BINARIO,
    NUMERICO,
    CATEGORICO
}
