package com.opsfactor.community.platform.integration.mapper;

import com.opsfactor.community.capability.configuration.integration.mapper.ParametrosMaterialLocationIntegrationMapper;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.mapper.EstoqueIntegrationMapper;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.mapper.SelloutIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.location.integration.mapper.LocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesIntegrationMapper;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.integration.mapper.PoliticaEstoquesMaterialLocationIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.VersaoMalhaIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.DisponibilidadeRecursoProdutivoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaComponenteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.RecursoProdutivoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.routing.integration.mapper.RoteiroIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeIntegrationMapper;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.mapper.ConversaoUnidadeProdutoIntegrationMapper;
import com.opsfactor.community.capability.supplyplanning.inventoryplan.integration.mapper.InventoryPlanIntegrationMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Guarda transversal dos headers publicos de data upload Community.
 *
 * <p>Os testes por dominio continuam validando a ordem exata das colunas mais
 * sensiveis. Esta boundary test existe para impedir regressao estrutural em
 * qualquer mapper Community novo ou existente: headers publicados precisam ser
 * beans Spring explicitos, imutaveis e livres de termos que representam
 * superficies Enterprise-only ja recortadas da edicao aberta.</p>
 */
class IntegrationMapperHeadersCommunityBoundaryTest {

    private static final String COMMUNITY_DTO_MAIN_SOURCE_DIRECTORY =
            "src/main/java";

    private static final List<String> ENTERPRISE_ONLY_HEADER_TERMS = List.of(
            "enterprise",
            "price",
            "cost",
            "cogs",
            "margin",
            "p&l",
            "tax",
            "latitude",
            "longitude",
            "campaign",
            "event",
            "sell-in",
            "sales order",
            "firm order",
            "batch",
            "aging",
            "expiration",
            "warehouse",
            "fleet",
            "vehicle",
            "last mile",
            "maintenance",
            "shift",
            "line scheduling",
            "parallel");

    @Test
    void communityIntegrationMapperSourceSurfaceShouldStayExplicitlyEnumerated() throws IOException {

        Path communityWorkspaceDirectory = resolveCommunityWorkspaceDirectory();

        /*
         * O Community congela a superficie aberta de data upload. Mesmo que a
         * guarda arquitetural geral aprove um novo arquivo, o mapper tambem
         * precisa entrar nesta lista para ter headers, termos Enterprise e
         * imutabilidade revisados explicitamente.
         */
        Assertions.assertEquals(
                findCommunityIntegrationMapperSimpleNames(communityWorkspaceDirectory),
                getCommunityHeaderMappers()
                        .stream()
                        .map(HeaderMapper::name)
                        .sorted()
                        .toList(),
                "Todo *IntegrationMapper Community deve estar enumerado na guarda transversal de headers.");

    }

    @Test
    void communityIntegrationMappersShouldPublishStableOpenHeaders() {

        for (HeaderMapper headerMapper : getCommunityHeaderMappers()) {
            List<String> processedFileHeaders = headerMapper.integrationMapper().getProcessedFileHeaders();

            /*
             * Headers de XLSX/CSV sao superficie publica. Duplicidade ou texto
             * vazio quebram leitura posicional e podem fazer o front ou scripts
             * externos interpretarem uma coluna Enterprise como coluna aberta.
             */
            Assertions.assertFalse(
                    processedFileHeaders.isEmpty(),
                    headerMapper.name() + " must publish at least one Community header.");
            Assertions.assertEquals(
                    processedFileHeaders.size(),
                    new HashSet<>(processedFileHeaders).size(),
                    headerMapper.name() + " must not publish duplicate headers.");
            Assertions.assertTrue(
                    processedFileHeaders.stream().noneMatch(processedFileHeader ->
                            processedFileHeader == null || processedFileHeader.isBlank()),
                    headerMapper.name() + " must not publish null or blank headers.");

            assertMapperIsSpringComponent(headerMapper);
            assertHeadersAreImmutable(headerMapper, processedFileHeaders);
            assertHeadersDoNotExposeEnterpriseTerms(headerMapper, processedFileHeaders);
        }

    }

    private static List<HeaderMapper> getCommunityHeaderMappers() {

        /*
         * A lista e intencionalmente explicita: quando um novo mapper publico
         * de data upload for adicionado ao Community, ele precisa passar por
         * revisao de contrato e entrar aqui com os mesmos guardrails.
         */
        return List.of(
                new HeaderMapper("ParametrosMaterialLocationIntegrationMapper", new ParametrosMaterialLocationIntegrationMapper()),
                new HeaderMapper("EstoqueIntegrationMapper", new EstoqueIntegrationMapper()),
                new HeaderMapper("SelloutIntegrationMapper", new SelloutIntegrationMapper()),
                new HeaderMapper("LocationIntegrationMapper", new LocationIntegrationMapper()),
                new HeaderMapper("ProdutoIntegrationMapper", new ProdutoIntegrationMapper()),
                new HeaderMapper("PoliticaEstoquesIntegrationMapper", new PoliticaEstoquesIntegrationMapper()),
                new HeaderMapper("PoliticaEstoquesMaterialLocationIntegrationMapper", new PoliticaEstoquesMaterialLocationIntegrationMapper()),
                new HeaderMapper("VersaoMalhaIntegrationMapper", new VersaoMalhaIntegrationMapper()),
                new HeaderMapper("LinhaTransporteIntegrationMapper", new LinhaTransporteIntegrationMapper()),
                new HeaderMapper("LinhaTransporteProdutoIntegrationMapper", new LinhaTransporteProdutoIntegrationMapper()),
                new HeaderMapper("DisponibilidadeRecursoProdutivoIntegrationMapper", new DisponibilidadeRecursoProdutivoIntegrationMapper()),
                new HeaderMapper("ListaTecnicaComponenteIntegrationMapper", new ListaTecnicaComponenteIntegrationMapper()),
                new HeaderMapper("ListaTecnicaIntegrationMapper", new ListaTecnicaIntegrationMapper()),
                new HeaderMapper("RecursoProdutivoIntegrationMapper", new RecursoProdutivoIntegrationMapper()),
                new HeaderMapper("RoteiroIntegrationMapper", new RoteiroIntegrationMapper()),
                new HeaderMapper("VersaoProducaoIntegrationMapper", new VersaoProducaoIntegrationMapper()),
                new HeaderMapper("ConversaoUnidadeIntegrationMapper", new ConversaoUnidadeIntegrationMapper()),
                new HeaderMapper("ConversaoUnidadeProdutoIntegrationMapper", new ConversaoUnidadeProdutoIntegrationMapper()),
                new HeaderMapper("InventoryPlanIntegrationMapper", new InventoryPlanIntegrationMapper()));

    }

    private static void assertMapperIsSpringComponent(HeaderMapper headerMapper) {

        Assertions.assertTrue(
                headerMapper.integrationMapper().getClass().isAnnotationPresent(Component.class),
                headerMapper.name() + " must be an explicit Spring component.");

    }

    private static void assertHeadersAreImmutable(
            HeaderMapper headerMapper,
            List<String> processedFileHeaders) {

        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Regression Header"),
                headerMapper.name() + " must publish immutable headers.");

    }

    private static void assertHeadersDoNotExposeEnterpriseTerms(
            HeaderMapper headerMapper,
            List<String> processedFileHeaders) {

        Set<String> lowerCaseHeaders = new HashSet<>();
        for (String processedFileHeader : processedFileHeaders) {
            lowerCaseHeaders.add(processedFileHeader.toLowerCase(Locale.ROOT));
        }

        for (String processedFileHeader : lowerCaseHeaders) {
            for (String enterpriseOnlyHeaderTerm : ENTERPRISE_ONLY_HEADER_TERMS) {
                Assertions.assertFalse(
                        processedFileHeader.contains(enterpriseOnlyHeaderTerm),
                        headerMapper.name()
                                + " exposes Enterprise-only term `"
                                + enterpriseOnlyHeaderTerm
                                + "` in header `"
                                + processedFileHeader
                                + "`.");
            }
        }

    }

    private static Path resolveCommunityWorkspaceDirectory() {

        Path currentDirectory = Paths.get("").toAbsolutePath().normalize();
        while (currentDirectory != null
                && !"opsfactor-community".equals(currentDirectory.getFileName().toString())) {
            currentDirectory = currentDirectory.getParent();
        }
        if (currentDirectory == null) {
            throw new IllegalStateException("Could not resolve opsfactor-community workspace directory.");
        }
        return currentDirectory;

    }

    private static List<String> findCommunityIntegrationMapperSimpleNames(
            Path communityWorkspaceDirectory) throws IOException {

        Path communityDtoSourceDirectory = communityWorkspaceDirectory.resolve(COMMUNITY_DTO_MAIN_SOURCE_DIRECTORY);
        try (Stream<Path> pathStream = Files.walk(communityDtoSourceDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("IntegrationMapper.java"))
                    .map(path -> path.getFileName().toString().replace(".java", ""))
                    .sorted()
                    .toList();
        }

    }

    private record HeaderMapper(
            String name,
            IntegrationMapperInterface<?, ?, ?, ?> integrationMapper) {
    }

}
