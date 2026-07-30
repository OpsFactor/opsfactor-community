package com.opsfactor.community.web.dto.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base serializavel dos DTOs expostos pelas bordas Community.
 *
 * <p>A classe centraliza as anotacoes Jackson comuns para payloads vindos do
 * front compartilhado, permitindo ignorar propriedades desconhecidas durante a
 * transicao Community/Enterprise sem replicar configuracao em cada DTO.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // HERDADO AUTOMATICAMENTE PELAS CLASSES QUE EXTENDEM DTO
@JsonIgnoreProperties(ignoreUnknown = true) // HERDADO AUTOMATICAMENTE PELAS CLASSES QUE EXTENDEM DTO : ao importar JSON com propriedades não mapeadas elas serão ignoradas ao invés de gerar erro
// no @SuperBuilder, campos final (ex. listas com inicialização como ArrayList) não são apresentados no builder e sempre são inicializados
@SuperBuilder // TODAS AS CLASSES QUE EXTENDEM DTO PODEM OPCIONALMENTE INCLUIR @SuperBuilder
@NoArgsConstructor // AS CLASSES QUE EXTENDEM DTO PODEM OPCIONALMENTE INCLUIR @NoArgsConstructor
@Getter // HERDADO AUTOMATICAMENTE PELAS CLASSES QUE EXTENDEM DTO
@Setter // HERDADO AUTOMATICAMENTE PELAS CLASSES QUE EXTENDEM DTO
public abstract class DTO implements Serializable {
}
