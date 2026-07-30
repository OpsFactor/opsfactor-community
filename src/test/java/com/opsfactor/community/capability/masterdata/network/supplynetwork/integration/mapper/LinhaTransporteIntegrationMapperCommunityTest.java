package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFileRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Valida o contrato Community de distancia em malha. Distancia e um dado de
 * visualizacao/geografia/frete Enterprise; o heuristico Community trabalha com
 * lead time, prioridade e lotes.
 */
public class LinhaTransporteIntegrationMapperCommunityTest {

    private static final Set<String> COMMUNITY_ACCEPTED_LANE_FIELD_NAMES = Set.of(
            "priority",
            "leadTimeDays",
            "enableDiscontinuedMaterials",
            "enablePresalesMaterials",
            "enableAllMaterials",
            "multipleMinimumTransferLotSizeUomId",
            "minimumTransferLotSize",
            "multipleTransfer",
            "active");

    private static final Set<String> COMMUNITY_ACCEPTED_MATERIAL_LANE_FIELD_NAMES = Set.of(
            "priority",
            "leadTimeDays",
            "multipleMinimumTransferLotSizeUomId",
            "minimumTransferLotSize",
            "multipleTransfer",
            "active");

    @Test
    public void transportationLaneMappersShouldBeSpringComponents() {

        Assertions.assertTrue(LinhaTransporteIntegrationMapper.class.isAnnotationPresent(Component.class));
        Assertions.assertTrue(LinhaTransporteProdutoIntegrationMapper.class.isAnnotationPresent(Component.class));

    }

    @Test
    public void transportationLaneShouldRejectDistanceCommunity() {

        LinhaTransporteIntegrationMapper linhaTransporteIntegrationMapper =
                new LinhaTransporteIntegrationMapper();
        LinhaTransporte linhaTransporte = getLinhaTransporte();
        LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto =
                LinhaTransporteIntegrationDataDto.builder()
                        .priority(1)
                        .leadTimeDays(2d)
                        .distanceKm(100d)
                        .active(true)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> linhaTransporteIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        linhaTransporte,
                        linhaTransporteIntegrationDataDto,
                        null,
                        null));

    }

    @Test
    public void transportationLaneMaterialShouldRejectDistanceCommunity() {

        LinhaTransporteProdutoIntegrationMapper linhaTransporteProdutoIntegrationMapper =
                new LinhaTransporteProdutoIntegrationMapper();
        LinhaTransporteProduto linhaTransporteProduto =
                new LinhaTransporteProduto(
                        new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                getLinhaTransporte(),
                                new Produto("MAT_01")));
        LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDto =
                LinhaTransporteProdutoIntegrationDataDto.builder()
                        .priority(1)
                        .leadTimeDays(2)
                        .distanceKm(100d)
                        .active(true)
                        .build();

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> linhaTransporteProdutoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                        linhaTransporteProduto,
                        linhaTransporteProdutoIntegrationDataDto,
                        null,
                        null));

    }

    @Test
    public void transportationLaneShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        LinhaTransporteIntegrationMapper linhaTransporteIntegrationMapper =
                new LinhaTransporteIntegrationMapper();

        /*
         * A lane Community aceita apenas parametros operacionais do heuristico.
         * Campos como distancia/frete/mapa devem ficar fora do contrato publico
         * e falhar caso cheguem via payload compartilhado.
         */
        for (Field field : LinhaTransporteIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_LANE_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto =
                    LinhaTransporteIntegrationDataDto.builder()
                            .priority(1)
                            .leadTimeDays(2d)
                            .active(true)
                            .build();
            field.setAccessible(true);
            field.set(linhaTransporteIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> linhaTransporteIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            getLinhaTransporte(),
                            linhaTransporteIntegrationDataDto,
                            null,
                            null));
        }

    }

    @Test
    public void transportationLaneMaterialShouldRejectEveryNonCommunityField() throws IllegalAccessException {

        LinhaTransporteProdutoIntegrationMapper linhaTransporteProdutoIntegrationMapper =
                new LinhaTransporteProdutoIntegrationMapper();

        /*
         * Overrides de lane por material seguem o mesmo recorte: prioridade,
         * lead time, lote/multiplo e status sao Community; distancia por
         * material e capacidade Enterprise.
         */
        for (Field field : LinhaTransporteProdutoIntegrationDataDto.class.getDeclaredFields()) {
            if (COMMUNITY_ACCEPTED_MATERIAL_LANE_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDto =
                    LinhaTransporteProdutoIntegrationDataDto.builder()
                            .priority(1)
                            .leadTimeDays(2)
                            .active(true)
                            .build();
            field.setAccessible(true);
            field.set(linhaTransporteProdutoIntegrationDataDto, getEnterpriseFieldValue(field));

            Assertions.assertThrows(
                    RequiresEnterpriseVersionException.class,
                    () -> linhaTransporteProdutoIntegrationMapper.updateEntityNonPrimaryFieldsFromDTO(
                            new LinhaTransporteProduto(
                                    new LinhaTransporteProduto.LinhaTransporteProdutoCompositeKey(
                                            getLinhaTransporte(),
                                            new Produto("MAT_01"))),
                            linhaTransporteProdutoIntegrationDataDto,
                            null,
                            null));
        }

    }

    @Test
    public void transportationLaneExportShouldHideDistanceCommunity() {

        LinhaTransporteIntegrationMapper linhaTransporteIntegrationMapper =
                new LinhaTransporteIntegrationMapper();
        LinhaTransporte linhaTransporte = getLinhaTransporte();
        linhaTransporte.setDistanciaKm(100d);
        linhaTransporte.setHabilitadoProdutosDescontinuados(true);
        linhaTransporte.setHabilitadoProdutosNaoLancados(false);
        linhaTransporte.setHabilitadoProdutosNaoCadastradosLinhaTransporte(true);
        linhaTransporte.setLoteMinimoTransporte(10d);
        linhaTransporte.setMultiploTransporte(5d);
        linhaTransporte.setAtivo(true);

        LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto =
                linhaTransporteIntegrationMapper.getDtoWithoutPrimaryKeyFromEntity(linhaTransporte);
        ProcessedFileRow processedFileRow =
                linhaTransporteIntegrationMapper.convertEntityToProcessedFileRow(
                        linhaTransporte,
                        null);

        Assertions.assertNull(linhaTransporteIntegrationDataDto.distanceKm);
        Assertions.assertEquals(12, processedFileRow.getRowSize());
        Assertions.assertEquals(linhaTransporte.getHabilitadoProdutosDescontinuadosCadastrado(), processedFileRow.getColumnValue(5));
        Assertions.assertEquals(linhaTransporte.getHabilitadoProdutosNaoLancadosCadastrado(), processedFileRow.getColumnValue(6));
        Assertions.assertEquals(linhaTransporte.getHabilitadoProdutosNaoCadastradosLinhaTransporteCadastrado(), processedFileRow.getColumnValue(7));
        Assertions.assertEquals(linhaTransporte.getLoteMinimoTransporteCadastrado(), processedFileRow.getColumnValue(9));
        Assertions.assertEquals(linhaTransporte.getMultiploTransporteCadastrado(), processedFileRow.getColumnValue(10));
        Assertions.assertEquals(linhaTransporte.getAtivoCadastrado(), processedFileRow.getColumnValue(11));

    }

    @Test
    public void transportationLaneHeadersShouldExposeOnlyCommunityColumns() {

        LinhaTransporteIntegrationMapper linhaTransporteIntegrationMapper =
                new LinhaTransporteIntegrationMapper();

        List<String> processedFileHeaders = linhaTransporteIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertEquals(List.of(
                "Supply Network Version Id",
                "Origin Location Id",
                "Destination Location Id",
                "Priority (0 = largest priority)",
                "Lead Time (Days)",
                "Enable Transportation Line for Discontinued Materials (TRUE/FALSE or 1/0)",
                "Enable Transportation Line for Materials not yet launched (TRUE/FALSE or 1/0)",
                "Enable Transportation Line for All Materials (TRUE/FALSE or 1/0)",
                "Multiple Minimum Transfer Lot Size UOM Id",
                "Minimum Transfer Lot Size",
                "Multiple Transfer",
                "Active : TRUE/FALSE or 1/0 (Default = True if empty)"
        ), processedFileHeaders);

        Assertions.assertFalse(processedFileHeaders.stream().anyMatch(processedFileHeader ->
                processedFileHeader.contains("Enterprise")
                        || processedFileHeader.contains("Distance")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Distance (Km)"));

    }

    @Test
    public void transportationLaneMaterialHeadersShouldExposeOnlyCommunityColumns() {

        LinhaTransporteProdutoIntegrationMapper linhaTransporteProdutoIntegrationMapper =
                new LinhaTransporteProdutoIntegrationMapper();

        List<String> processedFileHeaders = linhaTransporteProdutoIntegrationMapper.getProcessedFileHeaders();

        Assertions.assertEquals(List.of(
                "Supply Network Version Id",
                "Origin Location Id",
                "Destination Location Id",
                "Material Id",
                "Priority (0 = largest priority)",
                "Lead Time (Days)",
                "Multiple Minimum Transfer Lot Size UOM Id",
                "Minimum Transfer Lot Size",
                "Multiple Transfer",
                "Active : TRUE/FALSE or 1/0 (Default = True if empty)"
        ), processedFileHeaders);

        Assertions.assertFalse(processedFileHeaders.stream().anyMatch(processedFileHeader ->
                processedFileHeader.contains("Enterprise")
                        || processedFileHeader.contains("Distance")
                        || processedFileHeader.contains("Discontinued")
                        || processedFileHeader.contains("not yet launched")));
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Distance (Km)"));

    }

    private LinhaTransporte getLinhaTransporte() {

        return new LinhaTransporte(
                new LinhaTransporte.LinhaTransporteCompositeKey(
                        new VersaoMalha("SN_01"),
                        new Location("ORIGEM"),
                        new Location("DESTINO")));

    }

    private Object getEnterpriseFieldValue(Field field) {

        if (field.getType().equals(Double.class)) {
            return 100.0d;
        }

        throw new IllegalArgumentException("Unsupported Enterprise field type in test: " + field);

    }

}
