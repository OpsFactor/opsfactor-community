package com.opsfactor.community.capability.masterdata.architecture.facade.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.product.material.facade.dto.ProdutoDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaLocationDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.TipoCaracteristicaDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.ConversaoUnidadeMedidaDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrato de shape dos DTOs REST de master data Community.
 *
 * <p>Data upload possui seus proprios DTOs. Estes DTOs atendem APIs REST
 * legadas/operacionais de listagem e edicao simples. Campos de GIS, deployment,
 * caracteristicas dinamicas ou pricing so podem permanecer quando o service
 * correspondente os bloqueia explicitamente.</p>
 */
public class MasterdataDtoCommunityContractTest {

    @Test
    public void productDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "id",
                        "description",
                        "active",
                        "materialStatus"),
                getDeclaredFieldNames(ProdutoDTO.class));

    }

    @Test
    public void productDtoShouldRejectHistoricalProductStatusPayloadField() {

        Assertions.assertThrows(
                JsonProcessingException.class,
                () -> new ObjectMapper().readValue(
                        "{\"productStatus\":\"Active\"}",
                        ProdutoDTO.class));

    }

    @Test
    public void productDtoShouldRoundTripCanonicalMaterialStatusPayloadField() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ProdutoDTO produtoDTO = objectMapper.readValue(
                "{\"materialStatus\":\"Active\"}",
                ProdutoDTO.class);

        Assertions.assertEquals("Active", produtoDTO.getMaterialStatus());
        Assertions.assertTrue(
                objectMapper.writeValueAsString(produtoDTO).contains("\"materialStatus\""));

    }

    @Test
    public void locationDtoShouldExposeOnlyCommunityFieldsAndExplicitEnterpriseGuardFields() {

        /*
         * Latitude/longitude, characteristicValues, showInDeployment e
         * restricoes logisticas gerais permanecem apenas para compatibilidade
         * do endpoint legado; `LocationDtoService` deve rejeitar qualquer
         * valor preenchido antes de repository. A excecao e
         * applyInboundConstraints, publicado e persistido pelo Community
         * porque o heuristico de Supply o consome diretamente.
         */
        Assertions.assertEquals(
                Set.of(
                        "id",
                        "locationType",
                        "description",
                        "active",
                        "country",
                        "state",
                        "city",
                        "latitude",
                        "longitude",
                        "characteristicValues",
                        "showInSupplyPlanningBook",
                        "showInProductionPlanningBook",
                        "showInDeployment",
                        "applyInboundConstraints",
                        "applyLogisticsConstraints",
                        "applyProductionConstraints",
                        "safetyStockConsiderIndirectDemand"),
                getDeclaredFieldNames(LocationDTO.class));

    }

    @Test
    public void characteristicDtosShouldStayAsCompatibilityEnvelopeOnly() {

        Assertions.assertEquals(
                Set.of(
                        "caracteristicaId",
                        "descricao",
                        "tipoCaracteristica",
                        "listaAtributos",
                        "atributo"),
                getDeclaredFieldNames(CaracteristicaProdutoDTO.class));
        Assertions.assertEquals(
                Set.of(
                        "caracteristicaId",
                        "descricao",
                        "tipoCaracteristica",
                        "listaAtributos",
                        "atributo"),
                getDeclaredFieldNames(CaracteristicaLocationDTO.class));
        Assertions.assertEquals(
                Set.of(
                        TipoCaracteristicaDTO.BINARIO,
                        TipoCaracteristicaDTO.NUMERICO,
                        TipoCaracteristicaDTO.CATEGORICO),
                Set.of(TipoCaracteristicaDTO.values()));

    }

    @Test
    public void unitConversionDtoShouldExposeOnlyCommunityFields() {

        Assertions.assertEquals(
                Set.of(
                        "materialId",
                        "originUomId",
                        "targetUomId",
                        "conversionCoefficient",
                        "stepByStep"),
                getDeclaredFieldNames(ConversaoUnidadeMedidaDTO.class));

    }

    private static Set<String> getDeclaredFieldNames(Class<?> dtoClass) {

        return Arrays.stream(dtoClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

    }

}
