package com.opsfactor.community.capability.masterdata.architecture.integration.mapper;

import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaComponenteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.integration.mapper.ListaTecnicaIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.integration.mapper.DisponibilidadeRecursoProdutivoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.productionversion.integration.mapper.VersaoProducaoSimplesIntegrationMapper;
import com.opsfactor.community.capability.masterdata.production.routing.integration.mapper.RoteiroIntegrationMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Contrato dos headers de dados produtivos operacionais no Community.
 *
 * <p>Mesmo enquanto a decisao de publicar controllers de upload produtivo fica
 * em aberto, os mappers existentes precisam continuar restritos ao conjunto
 * minimo consumido pelo heuristico: BOM simples, componente, roteiro e versao
 * simples. Capacidades como custos, setup detalhado, turnos, manutencao, line
 * scheduling e parallel routing/output pertencem ao Enterprise.</p>
 */
public class ProductionIntegrationMapperHeadersCommunityTest {

    @Test
    public void billOfMaterialsHeadersShouldExposeOnlyCommunityColumns() {

        ListaTecnicaIntegrationMapper listaTecnicaIntegrationMapper =
                new ListaTecnicaIntegrationMapper();

        List<String> processedFileHeaders = listaTecnicaIntegrationMapper.getProcessedFileHeaders();

        assertMapperIsSpringComponent(ListaTecnicaIntegrationMapper.class);
        Assertions.assertEquals(List.of(
                "Bill of Materials Id",
                "Description",
                "Location Id",
                "Output Material Id",
                "Output Quantity",
                "Output Unit of Measure Id",
                "Priority",
                "Active (true/false or 1/0)",
                "Bill of Materials can be used without production version"
        ), processedFileHeaders);
        assertNoEnterpriseProductionHeader(processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);

    }

    @Test
    public void billOfMaterialsComponentHeadersShouldExposeOnlyCommunityColumns() {

        ListaTecnicaComponenteIntegrationMapper listaTecnicaComponenteIntegrationMapper =
                new ListaTecnicaComponenteIntegrationMapper();

        List<String> processedFileHeaders = listaTecnicaComponenteIntegrationMapper.getProcessedFileHeaders();

        assertMapperIsSpringComponent(ListaTecnicaComponenteIntegrationMapper.class);
        Assertions.assertEquals(List.of(
                "Bill of Materials Id",
                "Component Material Id",
                "Component Material Quantity Unit of Measure Id",
                "Component Material Quantity"
        ), processedFileHeaders);
        assertNoEnterpriseProductionHeader(processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);

    }

    @Test
    public void routingHeadersShouldExposeOnlyCommunityColumns() {

        RoteiroIntegrationMapper roteiroIntegrationMapper =
                new RoteiroIntegrationMapper();

        List<String> processedFileHeaders = roteiroIntegrationMapper.getProcessedFileHeaders();

        assertMapperIsSpringComponent(RoteiroIntegrationMapper.class);
        Assertions.assertEquals(List.of(
                "Routing Id",
                "Description",
                "Location Id",
                "Output Material Id",
                "Routing can be used without production version",
                "Priority",
                "Active (true/false or 1/0)"
        ), processedFileHeaders);
        assertNoEnterpriseProductionHeader(processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);

    }

    @Test
    public void singleOutputProductionVersionHeadersShouldExposeOnlyCommunityColumns() {

        VersaoProducaoSimplesIntegrationMapper versaoProducaoSimplesIntegrationMapper =
                new VersaoProducaoSimplesIntegrationMapper();

        List<String> processedFileHeaders = versaoProducaoSimplesIntegrationMapper.getProcessedFileHeaders();

        assertMapperIsSpringComponent(VersaoProducaoSimplesIntegrationMapper.class);
        Assertions.assertEquals(List.of(
                "Id",
                "Location Id",
                "Priority",
                "Output Material Id",
                "Routing Id",
                "Bill of Materials Id",
                "Active"
        ), processedFileHeaders);
        assertNoEnterpriseProductionHeader(processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);

    }

    @Test
    public void productionResourceAvailabilityHeadersShouldExposeOnlyCommunityColumns() {

        DisponibilidadeRecursoProdutivoIntegrationMapper disponibilidadeRecursoProdutivoIntegrationMapper =
                new DisponibilidadeRecursoProdutivoIntegrationMapper();

        List<String> processedFileHeaders = disponibilidadeRecursoProdutivoIntegrationMapper.getProcessedFileHeaders();

        assertMapperIsSpringComponent(DisponibilidadeRecursoProdutivoIntegrationMapper.class);
        Assertions.assertEquals(List.of(
                "Production Resource Id",
                "Reference Date",
                "Available Hours"
        ), processedFileHeaders);
        assertNoEnterpriseProductionHeader(processedFileHeaders);
        assertHeadersAreImmutable(processedFileHeaders);

    }

    private static void assertMapperIsSpringComponent(Class<?> mapperClass) {

        Assertions.assertTrue(
                mapperClass.isAnnotationPresent(Component.class),
                "Mapper de integracao produtiva Community precisa ser bean Spring explicito: " + mapperClass);

    }

    private static void assertHeadersAreImmutable(List<String> processedFileHeaders) {

        /*
         * Headers de data upload sao contrato publico: se uma lista mutavel for
         * alterada por engano em runtime, o Community pode passar a exportar
         * colunas Enterprise sem o backend ter implementacao correspondente.
         */
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> processedFileHeaders.add("Enterprise Test Header"));

    }

    private static void assertNoEnterpriseProductionHeader(List<String> processedFileHeaders) {

        String joinedHeaders = String.join(" | ", processedFileHeaders).toLowerCase(Locale.ROOT);

        /*
         * A lista e propositalmente textual, porque estes nomes sao os headers
         * que chegariam ao usuario em XLSX/CSV. O teste protege a superficie
         * publica, nao detalhes internos dos DTOs transicionais.
         */
        Assertions.assertFalse(joinedHeaders.contains("enterprise"));
        Assertions.assertFalse(joinedHeaders.contains("cost"));
        Assertions.assertFalse(joinedHeaders.contains("price"));
        Assertions.assertFalse(joinedHeaders.contains("setup"));
        Assertions.assertFalse(joinedHeaders.contains("maintenance"));
        Assertions.assertFalse(joinedHeaders.contains("shift"));
        Assertions.assertFalse(joinedHeaders.contains("line scheduling"));
        Assertions.assertFalse(joinedHeaders.contains("parallel"));
        Assertions.assertFalse(joinedHeaders.contains("capacity quantity"));
        Assertions.assertFalse(joinedHeaders.contains("resource cost"));

    }

}
