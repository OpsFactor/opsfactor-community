package com.opsfactor.community.capability.demandplanning.configuration.projection.forecast;

import com.opsfactor.community.capability.demandplanning.configuration.domain.ParametrosModeloEstatisticoAbstract;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.List;

/**
 * Parametros tecnicos do modelo Chronos/foundation model.
 *
 * <p>O Community nao executa Chronos. Esta classe fica no modelo compartilhado
 * porque `ParametrosForecastProjection` e as entidades de configuracao sao a
 * ponte natural para o overlay Enterprise de foundation models. A borda
 * Community continua bloqueando o modelo `CHRONOS` e qualquer parametro
 * Chronos ativo antes de salvar ou executar.</p>
 *
 * <p>Neste checkpoint apenas a flag persistida de reconciliacao agregado vs.
 * nivel MAPE fica configuravel. Modelo, device, quantis e timeout permanecem
 * defaults tecnicos do runtime Enterprise privado e nao sao expostos no contrato
 * editavel Community.</p>
 */
@Getter
@Setter
public class ParametrosChronos {

    /*
     * Community nao executa Chronos e nao deve publicar um provider/model id
     * concreto para uma capability bloqueada. O runtime Enterprise define o
     * modelo real no seu proprio request tecnico de foundation model.
     */
    public static final String MODELO_PADRAO = "chronos-default-sentinel";
    public static final String DEVICE_MAP_PADRAO = "cpu";
    public static final List<Double> QUANTIS_PADRAO = List.of(0.1D, 0.5D, 0.9D);
    public static final Duration TIMEOUT_PADRAO = Duration.ofMinutes(30);
    public static final boolean FORCA_FORECAST_AGREGADO_PADRAO = false;

    /**
     * Identificador tecnico do foundation model usado pelo runtime Enterprise.
     */
    public String modelId;

    /**
     * Device map tecnico usado pelo runtime Python/Chronos Enterprise.
     */
    public String deviceMap;

    /**
     * Quantis calculados pelo modelo para bounds inferior/superior.
     */
    public List<Double> quantileLevels;

    /**
     * Timeout maximo da chamada externa Chronos.
     */
    public Duration timeout;

    /**
     * Quando verdadeiro, o agregado superior permanece autoritativo e o nivel
     * intermediario e reescalado. Quando falso, o agregado pode ser recalculado
     * a partir da soma do nivel intermediario gerado pelo foundation model.
     */
    public boolean forceAggregatedForecast;

    public ParametrosChronos() {

        this(
                MODELO_PADRAO,
                DEVICE_MAP_PADRAO,
                QUANTIS_PADRAO,
                TIMEOUT_PADRAO,
                FORCA_FORECAST_AGREGADO_PADRAO);

    }

    public ParametrosChronos(
            ParametrosModeloEstatisticoAbstract parametrosModeloEstatisticoAbstract) {

        this(
                MODELO_PADRAO,
                DEVICE_MAP_PADRAO,
                QUANTIS_PADRAO,
                TIMEOUT_PADRAO,
                parametrosModeloEstatisticoAbstract != null
                        && parametrosModeloEstatisticoAbstract.getChronosForcaForecastAgregado() != null
                        && parametrosModeloEstatisticoAbstract.getChronosForcaForecastAgregado());

    }

    public ParametrosChronos(
            ParametrosChronos parametrosChronos) {

        this(
                parametrosChronos == null ? MODELO_PADRAO : parametrosChronos.getModelId(),
                parametrosChronos == null ? DEVICE_MAP_PADRAO : parametrosChronos.getDeviceMap(),
                parametrosChronos == null ? QUANTIS_PADRAO : parametrosChronos.getQuantileLevels(),
                parametrosChronos == null ? TIMEOUT_PADRAO : parametrosChronos.getTimeout(),
                parametrosChronos != null && parametrosChronos.isForceAggregatedForecast());

    }

    public ParametrosChronos(
            String modelId,
            String deviceMap,
            List<Double> quantileLevels,
            Duration timeout,
            boolean forceAggregatedForecast) {

        this.modelId = modelId == null || modelId.isBlank() ? MODELO_PADRAO : modelId;
        this.deviceMap = deviceMap == null || deviceMap.isBlank() ? DEVICE_MAP_PADRAO : deviceMap;
        this.quantileLevels = copiaQuantisValidos(quantileLevels);
        this.timeout = getTimeoutValido(timeout);
        this.forceAggregatedForecast = forceAggregatedForecast;

    }

    /**
     * Copia os quantis configurados para o foundation model.
     *
     * <p>Mesmo no Community, Chronos aparece como opcao bloqueada e seus
     * parametros podem existir como estrutura compartilhada para o overlay
     * Enterprise. A projection nao deve aceitar quantil fora do intervalo
     * probabilistico, porque isso so falharia muito depois na chamada Python.</p>
     */
    private static List<Double> copiaQuantisValidos(List<Double> quantileLevels) {

        if (quantileLevels == null || quantileLevels.isEmpty()) {
            return QUANTIS_PADRAO;
        }

        for (Double quantileLevel : quantileLevels) {
            if (quantileLevel == null
                    || !Double.isFinite(quantileLevel)
                    || quantileLevel <= 0D
                    || quantileLevel >= 1D) {
                throw new IllegalArgumentException("Quantil Chronos invalido: " + quantileLevel);
            }
        }
        return List.copyOf(quantileLevels);

    }

    /**
     * Normaliza o timeout tecnico do foundation model.
     *
     * <p>Timeout nulo significa usar o default do runtime. Zero ou negativo nao
     * e ausencia: faria a execucao externa expirar imediatamente ou depender de
     * comportamento de baixo nivel do `Process.waitFor`.</p>
     */
    private static Duration getTimeoutValido(Duration timeout) {

        if (timeout == null) {
            return TIMEOUT_PADRAO;
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout Chronos deve ser positivo.");
        }
        return timeout;

    }

}
