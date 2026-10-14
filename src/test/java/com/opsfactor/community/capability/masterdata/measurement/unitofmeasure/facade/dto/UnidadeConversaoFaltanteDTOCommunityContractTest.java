package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
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
    void reportSerializesExactlyFiveColumnsAndPreservesDifferentOperations() {

        var first = UnidadeConversaoFaltanteDTO.builder().materialId("ITEM")
                .originUnitOfMeasure("BOX").targetUnitOfMeasure("TON")
                .reason("Production Routing Operation UOM").additionalInformation("Routing ROUTING / Operation 10").build();
        var second = UnidadeConversaoFaltanteDTO.builder().materialId("ITEM")
                .originUnitOfMeasure("BOX").targetUnitOfMeasure("TON")
                .reason("Production Routing Operation UOM").additionalInformation("Routing ROUTING / Operation 20").build();
        var serialized = new ObjectMapper().valueToTree(first);
        Set<String> fields = new HashSet<>();
        serialized.fieldNames().forEachRemaining(fields::add);
        Assertions.assertEquals(Set.of("materialId", "originUnitOfMeasure", "targetUnitOfMeasure", "reason", "additionalInformation"), fields);
        Assertions.assertEquals(2, Set.of(first, second).size());
        Assertions.assertEquals("Production routing base quantity for operation time and cost",
                UnidadeConversaoFaltanteDTO.economicReason("Run cost", "Tempo operacional do roteiro ROUTING"));
        Assertions.assertEquals("Routing ROUTING; Location SITE",
                UnidadeConversaoFaltanteDTO.economicInformation("Tempo operacional do roteiro ROUTING", "SITE", "SITE"));

    }

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
