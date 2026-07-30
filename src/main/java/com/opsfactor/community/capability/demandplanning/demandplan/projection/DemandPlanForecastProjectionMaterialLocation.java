package com.opsfactor.community.capability.demandplanning.demandplan.projection;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.platform.calendar.Calendario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Serie de forecast no menor nivel funcional do Community: material/location.
 *
 * <p>Demand Planning Community persiste e exibe Planning Book nesse nivel,
 * sem agregacoes editaveis por caracteristica. Forecasts agregados sempre
 * desagregam de volta para uma lista deste subtipo antes da persistencia.</p>
 */
@AllArgsConstructor // necessário para builder
@NoArgsConstructor
@Getter
public class DemandPlanForecastProjectionMaterialLocation extends DemandPlanForecastProjection {

    /*
     * Identidade funcional da serie totalmente desagregada. Consumers devem usar
     * getters para deixar claro que material/location sao metadados da serie,
     * nao arrays de calculo como demanda ou forecastBaseline.
     */
    private Location location;
    private Produto material;

    public DemandPlanForecastProjectionMaterialLocation(
            Calendario calendario,
            UnidadeMedida unidadeMedida,
            Location location,
            Produto material,
            boolean preencheHorizonteForecastComDemandaHistorica) {
        super(calendario, unidadeMedida, preencheHorizonteForecastComDemandaHistorica);
        validaIdentidadeMaterialLocation(
                location,
                material);
        this.location = location;
        this.material = material;

    }

    @Override
    public List<DemandPlanForecastProjectionMaterialLocation> getDemandPlanForecastProjectionMaterialLocationList() {

        /*
         * O construtor vazio existe para testes/processors que manipulam apenas
         * arrays. Quando a instancia passa a ser usada como leaf real de
         * forecast, material/location precisam existir para nao vazar nulos em
         * agregacoes, desagregacoes ou persistencia do Planning Book.
         */
        validaIdentidadeMaterialLocation(
                location,
                material);

        return List.of(this);

    }

    /**
     * Não há nada para agregar pois este (material/location) já é o menor nível de agregação
     */
    @Override
    public void agregaForecastEDemandaHistoricaDemandPlanForecastProjectionAPartirNivelDesagregado() {
        return;
    }

    private static void validaIdentidadeMaterialLocation(
            Location location,
            Produto material) {

        if (location == null) {
            throw new IllegalArgumentException(
                    "Demand Plan forecast material/location projection requires location.");
        }

        if (material == null) {
            throw new IllegalArgumentException(
                    "Demand Plan forecast material/location projection requires material.");
        }

    }

}
