package com.opsfactor.community.platform.bi.facade.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resposta achatada compatível com o Production Overview legado.
 *
 * <p>As séries de quantidade sempre usam a UOM selecionada. A ocupação e a
 * capacidade permanecem separadas dessas séries, pois podem representar horas
 * ou capacidade em quantidade conforme o perfil de execução.</p>
 */
public class CommunityProductionOverviewDTO extends CommunitySupplyOverviewBaseDTO {

    public List<ProductionResourceCapacityDTO> capacityByProductionResource = new ArrayList<>();
    public List<ProductionResourceOccupationDTO> occupationAndProductionByProductionResourceAndMaterialGrouping = new ArrayList<>();

    /** Cria a resposta de produção a partir do bloco físico compartilhado. */
    public CommunityProductionOverviewDTO(CommunitySupplyOverviewBaseDTO base) {

        super(base);

    }

    /** Permite preencher diretamente a resposta quando não há bloco base prévio. */
    public CommunityProductionOverviewDTO() {

    }

    /** Capacidade registrada por recurso e período, na dimensão própria do recurso. */
    public static class ProductionResourceCapacityDTO {

        public String locationId;
        public String productionResourceId;
        public float[] capacityInHoursOrQuantity;

        public ProductionResourceCapacityDTO(String locationId, String productionResourceId, int numberOfPeriods) {

            this.locationId = locationId;
            this.productionResourceId = productionResourceId;
            capacityInHoursOrQuantity = new float[numberOfPeriods];

        }
    }

    /** Volume e ocupação por recurso e agrupamento, preservando as duas rodadas. */
    public static class ProductionResourceOccupationDTO extends CommunityMaterialCharacteristicGroupingDTO {

        public String locationId;
        public String productionResourceId;
        public String uomId;
        public float[] constrainedProductionQuantity;
        public float[] unconstrainedProductionQuantity;
        public float[] constrainedOccupationInHoursOrQuantity;
        public float[] unconstrainedOccupationInHoursOrQuantity;
        public float[] setupInHours;

        public ProductionResourceOccupationDTO(
                String locationId,
                String productionResourceId,
                Map<String, String> materialCharacteristicValues,
                String uomId,
                int numberOfPeriods) {

            super(materialCharacteristicValues);
            this.locationId = locationId;
            this.productionResourceId = productionResourceId;
            this.uomId = uomId;
            constrainedProductionQuantity = new float[numberOfPeriods];
            unconstrainedProductionQuantity = new float[numberOfPeriods];
            constrainedOccupationInHoursOrQuantity = new float[numberOfPeriods];
            unconstrainedOccupationInHoursOrQuantity = new float[numberOfPeriods];
            setupInHours = new float[numberOfPeriods];

        }
    }
}
