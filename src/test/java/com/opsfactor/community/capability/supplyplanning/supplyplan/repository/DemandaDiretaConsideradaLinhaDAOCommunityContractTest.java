package com.opsfactor.community.capability.supplyplanning.supplyplan.repository;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Contratos Community da borda JDBC de demanda direta considerada.
 *
 * <p>O DAO grava a fotografia em batch e deve falhar antes de abrir SQL quando
 * a chave material/location/supply plan/periodo estiver incompleta. Esses
 * testes exercitam somente a validacao anterior ao `JdbcTemplate`, portanto nao
 * precisam de conexao com banco.</p>
 */
class DemandaDiretaConsideradaLinhaDAOCommunityContractTest {

    @Test
    void saveInBatchShouldRejectNullCollectionAndNullItemsBeforeJdbcAccess() {

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();

        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(null),
                "Direct demand considered JDBC batch collection is required.");
        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(Arrays.asList((DemandaDiretaConsideradaLinha) null)),
                "Direct demand considered JDBC batch line at index 0 is required.");
        Assertions.assertDoesNotThrow(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of()));

    }

    @Test
    void saveInBatchShouldRejectIncompleteCompositeKeyBeforeJdbcAccess() {

        DemandaDiretaConsideradaLinhaDAO demandaDiretaConsideradaLinhaDAO =
                new DemandaDiretaConsideradaLinhaDAO();

        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of(new DemandaDiretaConsideradaLinha())),
                "Direct demand considered JDBC batch line at index 0 must have a composite key.");
        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of(getLinhaComSupplyPlanSemId())),
                "Direct demand considered JDBC batch line at index 0 must have a Supply Plan id.");
        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of(getLinhaComLocationSemId())),
                "Direct demand considered JDBC batch line at index 0 must have a location id.");
        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of(getLinhaComMaterialSemId())),
                "Direct demand considered JDBC batch line at index 0 must have a material id.");
        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of(getLinhaComDataReferenciaNula())),
                "Direct demand considered JDBC batch line at index 0 must have a reference date.");
        assertIllegalArgumentMessage(
                () -> demandaDiretaConsideradaLinhaDAO.saveInBatch(List.of(getLinhaComUnidadeMedidaSemId())),
                "Direct demand considered JDBC batch line at index 0 must have a unit of measure id when unit of measure is explicitly provided.");

    }

    private static DemandaDiretaConsideradaLinha getLinhaComSupplyPlanSemId() {

        return getLinha(
                new SupplyPlan(),
                new Location("LOC"),
                new Produto("MAT"),
                LocalDateTime.of(2026, 7, 3, 0, 0));

    }

    private static DemandaDiretaConsideradaLinha getLinhaComLocationSemId() {

        return getLinha(
                getSupplyPlanComId(),
                new Location(),
                new Produto("MAT"),
                LocalDateTime.of(2026, 7, 3, 0, 0));

    }

    private static DemandaDiretaConsideradaLinha getLinhaComMaterialSemId() {

        return getLinha(
                getSupplyPlanComId(),
                new Location("LOC"),
                new Produto(),
                LocalDateTime.of(2026, 7, 3, 0, 0));

    }

    private static DemandaDiretaConsideradaLinha getLinhaComDataReferenciaNula() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha = getLinha(
                getSupplyPlanComId(),
                new Location("LOC"),
                new Produto("MAT"),
                LocalDateTime.of(2026, 7, 3, 0, 0));
        setField(
                demandaDiretaConsideradaLinha.getDemandaDiretaConsideradaLinhaCompositeKey(),
                "dataReferencia",
                null);
        return demandaDiretaConsideradaLinha;

    }

    private static DemandaDiretaConsideradaLinha getLinhaComUnidadeMedidaSemId() {

        DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha = getLinha(
                getSupplyPlanComId(),
                new Location("LOC"),
                new Produto("MAT"),
                LocalDateTime.of(2026, 7, 3, 0, 0));
        demandaDiretaConsideradaLinha.setUnidadeMedida(new UnidadeMedida());
        return demandaDiretaConsideradaLinha;

    }

    private static SupplyPlan getSupplyPlanComId() {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(42L);
        return supplyPlan;

    }

    private static DemandaDiretaConsideradaLinha getLinha(
            SupplyPlan supplyPlan,
            Location location,
            Produto material,
            LocalDateTime dataReferencia) {

        DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey compositeKey =
                new DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey();
        compositeKey.setSupplyPlan(supplyPlan);
        compositeKey.setLocation(location);
        compositeKey.setMaterial(material);
        compositeKey.setDataReferencia(dataReferencia);

        return new DemandaDiretaConsideradaLinha(
                compositeKey);

    }

    private static void assertIllegalArgumentMessage(
            Executable executable,
            String expectedMessage) {

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                executable);

        Assertions.assertEquals(expectedMessage, illegalArgumentException.getMessage());

    }

    private static void setField(
            Object target,
            String fieldName,
            Object value) {

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError(
                    "Nao foi possivel preparar snapshot quebrado para teste Community.",
                    reflectiveOperationException);
        }

    }

}
