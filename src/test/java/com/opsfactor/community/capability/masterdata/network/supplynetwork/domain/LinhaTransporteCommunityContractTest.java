package com.opsfactor.community.capability.masterdata.network.supplynetwork.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalDouble;

/**
 * Contratos Community das restricoes fisicas de uma linha de transporte.
 */
class LinhaTransporteCommunityContractTest {

    @Test
    void minimumLotShouldTreatNullAsOperationalAbsence() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();

        Assertions.assertEquals(
                0.0d,
                linhaTransporte.getLoteMinimoTransporte());

    }

    @Test
    void leadTimeShouldRejectNegativeValueInsteadOfMaskingAsZero() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();
        linhaTransporte.setLeadTimeDias(-1.0d);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporte::getLeadTimeDiasInteiro);

        Assertions.assertEquals(
                "Transportation line lead time days must be finite and non-negative for ORIGIN -> DEST: -1.0.",
                illegalStateException.getMessage());

    }

    @Test
    void minimumLotShouldRejectNonFiniteValue() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();
        linhaTransporte.setLoteMinimoTransporte(Double.POSITIVE_INFINITY);

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                linhaTransporte::getLoteMinimoTransporte);

        Assertions.assertEquals(
                "Transportation line minimum lot must be finite and non-negative for ORIGIN -> DEST: Infinity.",
                illegalStateException.getMessage());

    }

    @Test
    void multipleShouldTreatNullAsOperationalAbsence() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();

        Assertions.assertTrue(linhaTransporte.getMultiploTransporte().isEmpty());

    }

    @Test
    void multipleShouldReturnPositiveValueWhenRegistered() {

        LinhaTransporte linhaTransporte =
                criaLinhaTransporte();
        linhaTransporte.setMultiploTransporte(12.5d);

        OptionalDouble optionalMultiploTransporte =
                linhaTransporte.getMultiploTransporte();

        Assertions.assertTrue(optionalMultiploTransporte.isPresent());
        Assertions.assertEquals(
                12.5d,
                optionalMultiploTransporte.getAsDouble());

    }

    @Test
    void sourceShouldNotKeepUnsavedTransientProductLineCommentedIntoMap() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path linhaTransporteSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/network/supplynetwork/domain/LinhaTransporte.java");
        String linhaTransporteSource = Files.readString(
                linhaTransporteSourcePath,
                StandardCharsets.UTF_8);

        /*
         * `getLinhaTransporteProduto` pode criar um objeto transiente para
         * consultar defaults, mas o proprio Javadoc do metodo define que esse
         * objeto nao entra no mapa persistido em memoria.
         */
        Assertions.assertFalse(
                linhaTransporteSource.contains("//mapaLinhaTransporteProduto.put"),
                "LinhaTransporte nao deve manter alternativa comentada que salvaria o item transiente no mapa.");

    }

    private static LinhaTransporte criaLinhaTransporte() {

        return new LinhaTransporte(
                new LinhaTransporte.LinhaTransporteCompositeKey(
                        new VersaoMalha("NETWORK"),
                        new Location("ORIGIN"),
                        new Location("DEST")));

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }
}
