package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contrato Community do DTO de conversoes de unidade faltantes.
 *
 * <p>O DTO e usado como superficie de diagnostico para master data operacional.
 * Por isso ele deve listar apenas causas realmente publicadas pela edicao
 * Community atual e nao manter alternativas antigas comentadas no enum.</p>
 */
class UnidadeConversaoFaltanteDTOCommunityContractTest {

    @Test
    void missingConversionRequirementEnumShouldNotKeepInboundRequisitionCommented() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();
        Path unidadeConversaoFaltanteDtoSourcePath = communityWorkspaceDirectory.resolve(
                "src/main/java/com/opsfactor/community/capability/masterdata/measurement/unitofmeasure/facade/dto/UnidadeConversaoFaltanteDTO.java");
        String unidadeConversaoFaltanteDtoSource = Files.readString(
                unidadeConversaoFaltanteDtoSourcePath,
                StandardCharsets.UTF_8);

        /*
         * Requisicoes inbound/outbound de supply sao conceitos diferentes. O
         * Community so publica a causa usada pelo diagnostico atual; manter a
         * alternativa inbound comentada sugeriria uma capability disponivel.
         */
        Assertions.assertFalse(
                unidadeConversaoFaltanteDtoSource.contains("REQUISICAO_INBOUND"),
                "UnidadeConversaoFaltanteDTO nao deve manter enum Enterprise/transicional comentado.");

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
