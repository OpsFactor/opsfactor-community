package com.opsfactor.community.capability.transactionaldata.sales.saleshistory.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Intervalo observado em dados historicos carregados na plataforma.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FirstAndLastDateDTO {

    public LocalDateTime firstDate;
    public LocalDateTime lastDate;

}
