package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;
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
public class CommunityProductionOverviewDTO {

    public List<LocalDateTime> finalDateTimeByPeriod = new ArrayList<>();
    public List<ProductionResourceCapacityDTO> capacityByProductionResource = new ArrayList<>();
    public List<StockAndProductionDTO> stockAndProductionByLocationAndMaterialGrouping = new ArrayList<>();
    public List<DirectAndIndirectDemandDTO> directAndIndirectDemandByLocationAndMaterialGrouping = new ArrayList<>();
    public List<ProductionResourceOccupationDTO> occupationAndProductionByProductionResourceAndMaterialGrouping = new ArrayList<>();

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

    /** Estoque, produção e inbound por location e agrupamento de materiais. */
    public static class StockAndProductionDTO extends CommunityMaterialCharacteristicGroupingDTO {

        public String locationId;
        public String quantityUomId;
        public float[] constrainedInventory;
        public float[] unconstrainedInventory;
        public float[] constrainedProduction;
        public float[] unconstrainedProduction;
        public float[] constrainedInbound;
        public float[] unconstrainedInbound;

        public StockAndProductionDTO(
                String locationId,
                Map<String, String> materialCharacteristicValues,
                String quantityUomId,
                int numberOfPeriods) {

            super(materialCharacteristicValues);
            this.locationId = locationId;
            this.quantityUomId = quantityUomId;
            constrainedInventory = new float[numberOfPeriods];
            unconstrainedInventory = new float[numberOfPeriods];
            constrainedProduction = new float[numberOfPeriods];
            unconstrainedProduction = new float[numberOfPeriods];
            constrainedInbound = new float[numberOfPeriods];
            unconstrainedInbound = new float[numberOfPeriods];

        }
    }

    /** Demanda direta e indireta por location e agrupamento de materiais. */
    public static class DirectAndIndirectDemandDTO extends CommunityMaterialCharacteristicGroupingDTO {

        public String locationId;
        public String quantityUomId;
        public float[] constrainedDirectDemand;
        public float[] unconstrainedDirectDemand;
        public float[] constrainedIndirectDemand;
        public float[] unconstrainedIndirectDemand;

        public DirectAndIndirectDemandDTO(
                String locationId,
                Map<String, String> materialCharacteristicValues,
                String quantityUomId,
                int numberOfPeriods) {

            super(materialCharacteristicValues);
            this.locationId = locationId;
            this.quantityUomId = quantityUomId;
            constrainedDirectDemand = new float[numberOfPeriods];
            unconstrainedDirectDemand = new float[numberOfPeriods];
            constrainedIndirectDemand = new float[numberOfPeriods];
            unconstrainedIndirectDemand = new float[numberOfPeriods];

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
