package com.opsfactor.community.platform.bi.facade.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bloco físico comum dos overviews de Supply.
 *
 * <p>O contrato contém somente a escala temporal, estoque, produção, inbound
 * e demanda. Capacidade produtiva e capacidade logística pertencem às
 * especializações Community e Enterprise, respectivamente.</p>
 */
public class CommunitySupplyOverviewBaseDTO {

    public List<LocalDateTime> finalDateTimeByPeriod = new ArrayList<>();
    public List<StockAndProductionDTO> stockAndProductionByLocationAndMaterialGrouping = new ArrayList<>();
    public List<DirectAndIndirectDemandDTO> directAndIndirectDemandByLocationAndMaterialGrouping = new ArrayList<>();

    /** Constrói o bloco vazio que será preenchido pela factory Community. */
    public CommunitySupplyOverviewBaseDTO() {

    }

    /** Copia o bloco físico para uma especialização sem compartilhar as listas mutáveis. */
    protected CommunitySupplyOverviewBaseDTO(CommunitySupplyOverviewBaseDTO base) {

        finalDateTimeByPeriod.addAll(base.finalDateTimeByPeriod);
        stockAndProductionByLocationAndMaterialGrouping.addAll(
                base.stockAndProductionByLocationAndMaterialGrouping);
        directAndIndirectDemandByLocationAndMaterialGrouping.addAll(
                base.directAndIndirectDemandByLocationAndMaterialGrouping);

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

        public StockAndProductionDTO(String locationId, Map<String, String> materialCharacteristicValues,
                String quantityUomId, int numberOfPeriods) {

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

        public DirectAndIndirectDemandDTO(String locationId, Map<String, String> materialCharacteristicValues,
                String quantityUomId, int numberOfPeriods) {

            super(materialCharacteristicValues);
            this.locationId = locationId;
            this.quantityUomId = quantityUomId;
            constrainedDirectDemand = new float[numberOfPeriods];
            unconstrainedDirectDemand = new float[numberOfPeriods];
            constrainedIndirectDemand = new float[numberOfPeriods];
            unconstrainedIndirectDemand = new float[numberOfPeriods];

        }
    }
}
