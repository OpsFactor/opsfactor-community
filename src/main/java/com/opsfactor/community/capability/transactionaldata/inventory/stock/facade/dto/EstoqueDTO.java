package com.opsfactor.community.capability.transactionaldata.inventory.stock.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.opsfactor.community.platform.serialization.facade.dto.CustomDateDeserializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EstoqueDTO {
    @JsonDeserialize(using = CustomDateDeserializer.class)
    public LocalDateTime reference_date;
    public String location_id;
    public String material_id;
    public Double quantity;

    @Override
    public String toString() {
        return "Stock {" +
                "reference_date=" + reference_date +
                ", location_id='" + location_id + '\'' +
                ", material_id='" + material_id + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
