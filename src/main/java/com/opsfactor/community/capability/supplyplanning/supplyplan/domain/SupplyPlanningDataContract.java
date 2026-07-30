package com.opsfactor.community.capability.supplyplanning.supplyplan.domain;

import com.opsfactor.community.platform.utility.Constantes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Mensagens padronizadas para guardas de contrato das entidades de planning
 * data do Supply Planning.
 *
 * <p>As linhas de Supply Plan compartilham enums amplos com outras areas da
 * plataforma para manter compatibilidade de DTO/schema. Nem todo valor desses
 * enums e materializado em toda entidade: `REMESSA`, `BUDGET`, `HISTORICO` e
 * `PLANO_NAO_ATENDIDO`, por exemplo, pertencem a outras bordas ou a
 * capabilities Enterprise. Este helper evita mensagens genericas de rotina
 * ausente e documenta, no proprio erro, qual subconjunto cada metodo aceita.
 * Os retornos usam {@link IllegalArgumentException} porque o erro e o valor
 * recebido pelo metodo de linha, nao uma funcionalidade pendente.</p>
 */
public final class SupplyPlanningDataContract {

    private SupplyPlanningDataContract() {

    }

    public static IllegalArgumentException unsupportedTipoPlano(
            Class<?> ownerClass,
            String operationName,
            Constantes.TipoPlano tipoPlano,
            Constantes.TipoPlano... tipoPlanoSuportadoArray) {

        return new IllegalArgumentException(
                ownerClass.getSimpleName() + "." + operationName
                        + " supports plan variants " + formatEnumValues(tipoPlanoSuportadoArray)
                        + ", received " + tipoPlano
                        + ". Budget, historical and unmet-demand variants are not stored in this Supply Planning line attribute.");

    }

    public static IllegalArgumentException unsupportedFirmePlanejado(
            Class<?> ownerClass,
            String operationName,
            Constantes.FirmePlanejado firmePlanejado,
            Constantes.FirmePlanejado... firmePlanejadoSuportadoArray) {

        return new IllegalArgumentException(
                ownerClass.getSimpleName() + "." + operationName
                        + " supports firm/planned buckets " + formatEnumValues(firmePlanejadoSuportadoArray)
                        + ", received " + firmePlanejado
                        + ". Firm deliveries/remessas are transactional Enterprise data and are not stored in this Supply Planning line attribute.");

    }

    public static IllegalArgumentException unsupportedEnumValue(
            Class<?> ownerClass,
            String operationName,
            String parameterName,
            Object receivedValue,
            String supportedValuesDescription,
            String contractDescription) {

        return new IllegalArgumentException(
                ownerClass.getSimpleName() + "." + operationName
                        + " supports " + parameterName + " " + supportedValuesDescription
                        + ", received " + receivedValue
                        + ". " + contractDescription);

    }

    public static IllegalArgumentException unsupportedTotalWrite(
            Class<?> ownerClass,
            String operationName) {

        return new IllegalArgumentException(
                ownerClass.getSimpleName() + "." + operationName
                        + " cannot write TOTAL directly. TOTAL is a read aggregation; write the segmented Demand Plan and client-orders values explicitly.");

    }

    private static String formatEnumValues(Enum<?>[] enumValueArray) {

        return Arrays.stream(enumValueArray)
                .map(Enum::name)
                .collect(Collectors.joining(", ", "[", "]"));

    }

}
