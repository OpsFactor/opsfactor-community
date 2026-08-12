package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.ConversaoUnidadeProduto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnitOfMeasureConversionLegacyRatioState;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.ConversaoUnidadeIntegrationDataDto.ConversaoUnidadePrimaryKeyIntegrationDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Garante que os contratos Community de upload gravam exclusivamente o par
 * canonico de quantidades e nao preservam a coluna direta depreciada.
 */
class ConversaoUnidadeCanonicalUploadMapperTest {

    @Test
    void globalUploadShouldAllowConversionBetweenAnyRegisteredUnits() {

        UnidadeMedida unidadeMedidaOrigem = new UnidadeMedida("ML");
        UnidadeMedida unidadeMedidaDestino = new UnidadeMedida("G");
        ConversaoUnidadeIntegrationMapper conversaoUnidadeIntegrationMapper =
                new ConversaoUnidadeIntegrationMapper();

        ConversaoUnidade conversaoUnidade = conversaoUnidadeIntegrationMapper
                .createNewEntityWithPrimaryKeyFromPrimaryKeyDTO(
                        new ConversaoUnidadePrimaryKeyIntegrationDTO("ML", "G"),
                        ConversaoUnidadeIntegrationSupportData.builder()
                                .uomPorId(java.util.Map.of(
                                        "ML", unidadeMedidaOrigem,
                                        "G", unidadeMedidaDestino))
                                .build());

        Assertions.assertSame(unidadeMedidaOrigem, conversaoUnidade.getUnidadeMedidaOrigem());
        Assertions.assertSame(unidadeMedidaDestino, conversaoUnidade.getUnidadeMedidaDestino());

    }

    @Test
    void globalCanonicalUploadShouldClearDeprecatedDirectRatio() {

        ConversaoUnidade conversaoUnidade = new ConversaoUnidade(
                new ConversaoUnidade.ConversaoUnidadeCompositeKey(
                        new UnidadeMedida("KG"),
                        new UnidadeMedida("UN")));
        conversaoUnidade.setQuantidadeUnidadeDestinoPorUnidadeOrigem(2.0);

        new ConversaoUnidadeIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(
                conversaoUnidade,
                ConversaoUnidadeIntegrationDataDto.builder()
                        .originQuantity(4.0)
                        .targetQuantity(10.0)
                        .build(),
                null,
                null);

        Assertions.assertNull(conversaoUnidade.getQuantidadeUnidadeDestinoPorUnidadeOrigemCadastrado());
        Assertions.assertEquals(4.0, conversaoUnidade.getQuantidadeUnidadeOrigemCadastrado());
        Assertions.assertEquals(10.0, conversaoUnidade.getQuantidadeUnidadeDestinoCadastrado());
        Assertions.assertEquals(
                UnitOfMeasureConversionLegacyRatioState.CANONICAL_ONLY,
                conversaoUnidade.getLegacyRatioState());

    }

    @Test
    void materialCanonicalUploadShouldClearDeprecatedDirectRatio() {

        ConversaoUnidadeProduto conversaoUnidadeProduto = new ConversaoUnidadeProduto(
                new ConversaoUnidadeProduto.ConversaoUnidadeProdutoCompositeKey(
                        new Produto("MAT-1"),
                        new UnidadeMedida("KG"),
                        new UnidadeMedida("UN")));
        conversaoUnidadeProduto.setQuantidadeUnidadeDestinoPorUnidadeOrigem(2.0);

        new ConversaoUnidadeProdutoIntegrationMapper().updateEntityNonPrimaryFieldsFromDTO(
                conversaoUnidadeProduto,
                ConversaoUnidadeProdutoIntegrationDataDto.builder()
                        .originQuantity(4.0)
                        .targetQuantity(10.0)
                        .build(),
                null,
                null);

        Assertions.assertEquals(4.0, conversaoUnidadeProduto.getQuantidadeUnidadeOrigemCadastrado());
        Assertions.assertEquals(10.0, conversaoUnidadeProduto.getQuantidadeUnidadeDestinoCadastrado());
        Assertions.assertEquals(
                UnitOfMeasureConversionLegacyRatioState.CANONICAL_ONLY,
                conversaoUnidadeProduto.getLegacyRatioState());

    }

}
