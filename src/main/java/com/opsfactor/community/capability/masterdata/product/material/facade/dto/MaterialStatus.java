package com.opsfactor.community.capability.masterdata.product.material.facade.dto;

/**
 * Status operacional publico de material.
 *
 * <p>O Community pode ler e exibir o status, mas tratamentos especificos para
 * material novo, sucessao e forecasts especiais pertencem ao Enterprise.</p>
 */
public enum MaterialStatus {
    NOT_RELEASED, NEW, REGULAR, DISCONTINUED
}
