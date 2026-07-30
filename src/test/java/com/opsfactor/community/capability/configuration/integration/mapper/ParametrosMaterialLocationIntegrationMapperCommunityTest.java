package com.opsfactor.community.capability.configuration.integration.mapper;

import com.opsfactor.community.capability.configuration.integration.dto.ParametrosMaterialLocationIntegrationDataDto;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Valida o contrato Community dos parametros material/location.
 *
 * <p>O cadastro material/location permanece no Community para dados operacionais
 * simples. A frequencia de reabastecimento, porem, pertence a otimizacao de
 * politica de estoques Enterprise e nao pode entrar por data upload. O horizonte
 * congelado de DP permanece Community por ser controle basico de edicao no
 * Planning Book, diferente do Reference Plan Enterprise.</p>
 */
public class ParametrosMaterialLocationIntegrationMapperCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "active",
            "introductionDate",
            "discontinuationDate",
            "productionMinimumMultipleUomId",
            "productionMinimumQuantity",
            "productionMultipleQuantity",
            "defaultUomId",
            "frozenHorizonDpInDays");

    @Test
    public void materialLocationParametersShouldRejectReorderFrequencyCommunity() {

        ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper =
                new ParametrosMaterialLocationIntegrationMapper();
        ParametrosProdutoLocation parametrosProdutoLocation = getParametrosProdutoLocation();
        ParametrosMaterialLocationIntegrationDataDto parametrosMaterialLocationIntegrationDataDto =
                ParametrosMaterialLocationIntegrationDataDto.builder()
                        .reorderFrequencyDays(7.0d)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> parametrosMaterialLocationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        parametrosProdutoLocation,
                        parametrosMaterialLocationIntegrationDataDto,
                        null,
                        null));

    }

    @Test
    public void materialLocationParametersShouldRejectProductLocationCharacteristicsCommunity() {

        ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper =
                new ParametrosMaterialLocationIntegrationMapper();
        ParametrosProdutoLocation parametrosProdutoLocation = getParametrosProdutoLocation();
        ParametrosMaterialLocationIntegrationDataDto parametrosMaterialLocationIntegrationDataDto =
                ParametrosMaterialLocationIntegrationDataDto.builder()
                        .valueByCharacteristic(Map.of("Characteristic", "Value"))
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> parametrosMaterialLocationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        parametrosProdutoLocation,
                        parametrosMaterialLocationIntegrationDataDto,
                        null,
                        null));

    }

    @Test
    public void materialLocationParametersShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper =
                new ParametrosMaterialLocationIntegrationMapper();

        /*
         * Parametros material/location sao compartilhados entre fluxos simples
         * Community e configuracoes Enterprise de politica/filtros DFU. Esta
         * allowlist deixa explicito quais campos ainda pertencem ao contrato
         * Community e exige erro de edicao Enterprise para todos os demais.
         */
        for (Field field : ParametrosMaterialLocationIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            ParametrosMaterialLocationIntegrationDataDto parametrosMaterialLocationIntegrationDataDto =
                    ParametrosMaterialLocationIntegrationDataDto.builder()
                            .build();
            field.setAccessible(true);
            field.set(parametrosMaterialLocationIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> parametrosMaterialLocationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            getParametrosProdutoLocation(),
                            parametrosMaterialLocationIntegrationDataDto,
                            null,
                            null));
        }

    }

    @Test
    public void materialLocationParametersShouldAcceptCommunityOperationalFields() {

        ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper =
                new ParametrosMaterialLocationIntegrationMapper();
        ParametrosProdutoLocation parametrosProdutoLocation = getParametrosProdutoLocation();
        LocalDateTime dataIntroducao = LocalDateTime.of(2026, 1, 10, 0, 0);
        LocalDateTime dataDescontinuacao = LocalDateTime.of(2026, 12, 31, 0, 0);
        ParametrosMaterialLocationIntegrationDataDto parametrosMaterialLocationIntegrationDataDto =
                ParametrosMaterialLocationIntegrationDataDto.builder()
                        .active(false)
                        .introductionDate(dataIntroducao)
                        .discontinuationDate(dataDescontinuacao)
                        .productionMinimumQuantity(10.0d)
                        .productionMultipleQuantity(5.0d)
                        .frozenHorizonDpInDays(14)
                        .build();
        ParametrosMaterialLocationIntegrationSupportData parametrosMaterialLocationIntegrationSupportData =
                new ParametrosMaterialLocationIntegrationSupportData();
        parametrosMaterialLocationIntegrationSupportData.mapaUnidadeMedidaPorId = Map.of();

        parametrosMaterialLocationIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                parametrosProdutoLocation,
                parametrosMaterialLocationIntegrationDataDto,
                parametrosMaterialLocationIntegrationSupportData,
                null);

        Assertions.assertFalse(parametrosProdutoLocation.getAtivoCadastrado());
        Assertions.assertEquals(dataIntroducao, parametrosProdutoLocation.getDataIntroducao());
        Assertions.assertEquals(dataDescontinuacao, parametrosProdutoLocation.getDataDescontinuacao());
        Assertions.assertEquals(10.0d, parametrosProdutoLocation.getLoteMinimoProducaoCadastrado());
        Assertions.assertEquals(5.0d, parametrosProdutoLocation.getMultiploProducaoCadastrado());
        Assertions.assertEquals(14, parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDpCadastrado());

    }

    @Test
    public void materialLocationParametersExportShouldHideReorderFrequencyCommunity() {

        ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper =
                new ParametrosMaterialLocationIntegrationMapper();
        ParametrosProdutoLocation parametrosProdutoLocation = getParametrosProdutoLocation();
        parametrosProdutoLocation.setFrequenciaReabastecimentoDias(7.0d);
        parametrosProdutoLocation.setLoteMinimoProducao(10d);
        parametrosProdutoLocation.setMultiploProducao(5d);
        parametrosProdutoLocation.setNumeroDiasHorizonteCongeladoDp(14);

        ParametrosMaterialLocationIntegrationDataDto parametrosMaterialLocationIntegrationDataDto =
                parametrosMaterialLocationIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(parametrosProdutoLocation);
        ProcessedFileRow processedFileRow =
                parametrosMaterialLocationIntegrationMapper.convertEntityToProcessedFileRow(
                        parametrosProdutoLocation,
                        null);

        Assertions.assertNull(parametrosMaterialLocationIntegrationDataDto.reorderFrequencyDays);
        Assertions.assertEquals(10, processedFileRow.getRowSize());
        Assertions.assertEquals(parametrosProdutoLocation.getLoteMinimoProducaoCadastrado(), processedFileRow.getColumnValue(6));
        Assertions.assertEquals(parametrosProdutoLocation.getMultiploProducaoCadastrado(), processedFileRow.getColumnValue(7));
        Assertions.assertEquals(parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDpCadastrado(), processedFileRow.getColumnValue(9));

    }

    @Test
    public void materialLocationParametersHeadersShouldExposeOnlyCommunityColumns() {

        ParametrosMaterialLocationIntegrationMapper parametrosMaterialLocationIntegrationMapper =
                new ParametrosMaterialLocationIntegrationMapper();

        List<String> processedFileHeaders = parametrosMaterialLocationIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertEquals(List.of(
                "Location Id",
                "Material Id",
                "Active : True/False or 1/0(if false, material is not part of lineup at location. Default = True if empty)",
                "Introduction Date",
                "Discontinuation Date",
                "Production Minimum/Multiple Unit of Measure (if empty, considers default UOM)",
                "Production Minimum Order Quantity",
                "Production Multiple Quantity",
                "Default Unit of Measure (Supply Planning)",
                "DP Frozen Horizon in Days"
        ), processedFileHeaders);

        Assertions.assertEquals(10, parametrosMaterialLocationIntegrationMapper.getDeleteProcessedFileRowPosition(null));
        Assertions.assertFalse(processedFileHeaders.stream().anyMatch(processedFileHeader ->
                processedFileHeader.contains("Enterprise")
                        || processedFileHeader.contains("Reorder Frequency")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Reorder Frequency Days"));

    }

    private ParametrosProdutoLocation getParametrosProdutoLocation() {

        return new ParametrosProdutoLocation(
                new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(
                        new Produto("MAT_01"),
                        new Location("LOC_01")));

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (field.getType().equals(Double.class)) {
            return 1.0d;
        }
        if (Map.class.isAssignableFrom(field.getType())) {
            return Map.of("EnterpriseCharacteristic", "Value");
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

}
