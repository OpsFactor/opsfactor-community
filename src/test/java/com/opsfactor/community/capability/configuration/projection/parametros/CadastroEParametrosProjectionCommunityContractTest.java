package com.opsfactor.community.capability.configuration.projection.parametros;

import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocationId;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProdutoId;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contrato Community da projection central de parametros.
 *
 * <p>Caracteristicas reais de material/location sao Enterprise e nao existem
 * fisicamente no modelo Community. Este contrato preserva apenas
 * pseudo-caracteristicas tecnicas, como ID, usadas por fluxos simples que
 * precisam tratar material/location como dimensoes diretas.</p>
 */
public class CadastroEParametrosProjectionCommunityContractTest {

    @Test
    public void getValorCaracteristicaProdutoShouldKeepMaterialIdPseudoCharacteristic() {

        CadastroEParametrosProjection cadastroEParametrosProjection = new CadastroEParametrosProjection();
        Produto material = new Produto();
        material.setId("M1");

        Assertions.assertEquals(
                "M1",
                cadastroEParametrosProjection.getValorCaracteristicaProduto(material, new CaracteristicaProdutoId()));

    }

    @Test
    public void getValorCaracteristicaLocationShouldKeepLocationIdPseudoCharacteristic() {

        CadastroEParametrosProjection cadastroEParametrosProjection = new CadastroEParametrosProjection();
        Location location = new Location("L1");

        Assertions.assertEquals(
                "L1",
                cadastroEParametrosProjection.getValorCaracteristicaLocation(location, new CaracteristicaLocationId()));

    }

    @Test
    public void sourceShouldNotKeepCurveClusterAllocationImplementationCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path cadastroEParametrosProjectionSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/configuration/projection/parametros/CadastroEParametrosProjection.java");
        String cadastroEParametrosProjectionSource = Files.readString(
                cadastroEParametrosProjectionSourcePath,
                StandardCharsets.UTF_8);

        /*
         * Alocacao por curva nao faz parte do recorte Community atual. O codigo
         * aberto deve manter apenas a regra ativa por status e a falha explicita
         * de caracteristicas Enterprise, sem algoritmo antigo comentado.
         */
        Assertions.assertFalse(
                cadastroEParametrosProjectionSource.contains("case CURVA")
                        || cadastroEParametrosProjectionSource.contains("regraAlocacaoClusterProdutos.getCurvaSet()"),
                "CadastroEParametrosProjection nao deve manter implementacao antiga de curva comentada.");

    }

    private Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();
        if (Files.isDirectory(currentDirectory.resolve("opsfactor-community/src/main/java"))) {
            return currentDirectory.resolve("opsfactor-community");
        }

        if ("community".equals(currentDirectory.getFileName().toString())
                || currentDirectory.getFileName().toString().startsWith("community-")) {
            return currentDirectory.getParent();
        }

        return currentDirectory;

    }

}
