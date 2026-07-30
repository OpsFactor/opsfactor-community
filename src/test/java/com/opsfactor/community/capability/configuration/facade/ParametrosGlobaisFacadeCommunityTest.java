package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Contratos Community da fachada de Global Parameters.
 *
 * <p>Global Parameters e uma das bordas mais sensiveis do front compartilhado:
 * varios campos continuam no schema para desserializar payloads Enterprise ou
 * bancos legados, mas a edicao Community deve aceitar somente sell-out,
 * limpeza historica inativa, sem pedidos/remessas, sem ajuste agregado e sem
 * capacidade logistica de frota.</p>
 */
public class ParametrosGlobaisFacadeCommunityTest {

    @Test
    public void serviceShouldUseExplicitAutowiredBeanFields() throws Exception {

        Field parametrosGlobaisServiceField =
                ParametrosGlobaisFacade.class.getDeclaredField("parametrosGlobaisService");
        Autowired autowired = parametrosGlobaisServiceField.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                "ParametrosGlobaisFrontService.parametrosGlobaisService deve declarar @Autowired explicitamente.");
        Assertions.assertTrue(
                autowired.required(),
                "ParametrosGlobaisFrontService.parametrosGlobaisService deve ser bean obrigatorio.");

    }

    @Test
    public void saveParametrosGlobaisShouldDelegateWhenPayloadUsesOnlyCommunityValues() {

        CapturingParametrosGlobaisService capturingParametrosGlobaisService =
                new CapturingParametrosGlobaisService();
        ParametrosGlobaisFacade parametrosGlobaisFrontService =
                criaParametrosGlobaisFrontService(capturingParametrosGlobaisService);
        ParametrosGlobais parametrosGlobais = criaParametrosGlobaisCommunityNeutro();

        ParametrosGlobais savedParametrosGlobais =
                parametrosGlobaisFrontService.saveParametrosGlobais(parametrosGlobais);

        Assertions.assertSame(parametrosGlobais, savedParametrosGlobais);
        Assertions.assertSame(parametrosGlobais, capturingParametrosGlobaisService.savedParametrosGlobais);
        Assertions.assertEquals(1, capturingParametrosGlobaisService.saveCallCount.get());

    }

    @Test
    public void saveParametrosGlobaisShouldAllowSafetyStockIndirectDemandChoices() {

        for (boolean safetyStockConsiderIndirectDemand : List.of(true, false)) {
            CapturingParametrosGlobaisService capturingParametrosGlobaisService =
                    new CapturingParametrosGlobaisService();
            ParametrosGlobaisFacade parametrosGlobaisFrontService =
                    criaParametrosGlobaisFrontService(capturingParametrosGlobaisService);
            ParametrosGlobais parametrosGlobais = criaParametrosGlobaisCommunityNeutro();
            parametrosGlobais.setIncluiDemandaIndiretaNoSafetyStock(
                    safetyStockConsiderIndirectDemand);

            ParametrosGlobais savedParametrosGlobais =
                    parametrosGlobaisFrontService.saveParametrosGlobais(parametrosGlobais);

            Assertions.assertSame(parametrosGlobais, savedParametrosGlobais);
            Assertions.assertEquals(1, capturingParametrosGlobaisService.saveCallCount.get());
        }

    }

    @Test
    public void saveParametrosGlobaisShouldRejectMissingPayloadBeforePersistence() {

        CapturingParametrosGlobaisService capturingParametrosGlobaisService =
                new CapturingParametrosGlobaisService();
        ParametrosGlobaisFacade parametrosGlobaisFrontService =
                criaParametrosGlobaisFrontService(capturingParametrosGlobaisService);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parametrosGlobaisFrontService.saveParametrosGlobais(null));

        Assertions.assertEquals(
                "Global Parameters payload is required.",
                illegalArgumentException.getMessage());
        Assertions.assertEquals(
                0,
                capturingParametrosGlobaisService.saveCallCount.get(),
                "Payload nulo deve falhar antes do service de modelo.");

    }

    @Test
    public void saveParametrosGlobaisShouldRejectEnterpriseSalesDocumentTypesBeforePersistence() {

        for (Constantes.TipoDocumentoVenda tipoDocumentoVendaEnterprise :
                List.of(Constantes.TipoDocumentoVenda.SELLIN, Constantes.TipoDocumentoVenda.PEDIDO)) {

            assertRequiresEnterpriseBeforePersistence(
                    parametrosGlobais -> parametrosGlobais.setTipoDocumentoVenda(tipoDocumentoVendaEnterprise),
                    "Global Parameters sales document type");

        }

    }

    @Test
    public void saveParametrosGlobaisShouldRejectEnterpriseHistoricalCleaningParametersBeforePersistence() {

        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setModeloDemandaBase(
                        Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO),
                "Global Parameters stockout normalization model");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setDiasHistoricosDoh(
                        Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH + 1),
                "Global Parameters stockout normalization DOH");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setDiasHistoricosDohStockout(
                        Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT + 1),
                "Global Parameters stockout normalization threshold");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setModeloNormalizacao(
                        Constantes.DPModeloNormalizacao.PERCENTIS),
                "Global Parameters outlier/campaign normalization model");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setDiasHistoricosNormalizacao(
                        Constantes.DP_PADRAO_DIAS_NORMALIZACAO + 1),
                "Global Parameters outlier normalization window");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setPercentilOutliersVenda(
                        Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA + 0.01d),
                "Global Parameters outlier percentile");

    }

    @Test
    public void saveParametrosGlobaisShouldRejectNonFiniteOutlierPercentileBeforePersistence() {

        /*
         * O percentil de outlier e parte da limpeza historica bloqueada no
         * Community. Mesmo quando a tela compartilhada envia um numero em vez
         * de null/default, o payload precisa ser finito; `NaN` nao pode ser
         * tratado como parametro ausente nem chegar ao service de modelo para
         * saneamento silencioso.
         */
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setPercentilOutliersVenda(Double.NaN),
                "Global Parameters outlier percentile");

    }

    @Test
    public void saveParametrosGlobaisShouldRejectAggregatedTransactionalAndNewEntityParametersBeforePersistence() {

        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setPermiteAjusteAgregadoSemBaselineProduto(true),
                "Global Parameters aggregated material adjustment");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setPermiteAjusteAgregadoSemBaselineLocation(true),
                "Global Parameters aggregated location adjustment");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(true),
                "Global Parameters deliveries consume availability");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setQuantidadesEmPedidosRepresentamSaldoRestante(true),
                "Global Parameters transactional order remaining quantity");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setConsideraPedidosBacklog(true),
                "Global Parameters transactional order backlog");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setCalculaCustoEstoque(true),
                "Global Parameters inventory cost calculation");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setNumeroDiasProdutoNovo(30),
                "Global Parameters new material logic");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setNumeroDiasLocationNova(30),
                "Global Parameters new location logic");

    }

    @Test
    public void saveParametrosGlobaisShouldRejectFleetCapacityUnitsBeforePersistence() {

        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaPeso(
                        new UnidadeMedida("KG")),
                "Global Parameters fleet capacity weight unit");
        assertRequiresEnterpriseBeforePersistence(
                parametrosGlobais -> parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaVolume(
                        new UnidadeMedida("M3")),
                "Global Parameters fleet capacity volume unit");

    }

    private static void assertRequiresEnterpriseBeforePersistence(
            Consumer<ParametrosGlobais> parametrosGlobaisConsumer,
            String expectedFeatureName) {

        CapturingParametrosGlobaisService capturingParametrosGlobaisService =
                new CapturingParametrosGlobaisService();
        ParametrosGlobaisFacade parametrosGlobaisFrontService =
                criaParametrosGlobaisFrontService(capturingParametrosGlobaisService);
        ParametrosGlobais parametrosGlobais = criaParametrosGlobaisCommunityNeutro();
        parametrosGlobaisConsumer.accept(parametrosGlobais);

        RequiresEnterpriseVersionException requiresEnterpriseVersionException = Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> parametrosGlobaisFrontService.saveParametrosGlobais(parametrosGlobais));

        Assertions.assertTrue(
                requiresEnterpriseVersionException.getMessage().contains(expectedFeatureName),
                "Mensagem deveria citar a feature bloqueada: " + expectedFeatureName);
        Assertions.assertEquals(
                0,
                capturingParametrosGlobaisService.saveCallCount.get(),
                "Payload Enterprise deve falhar antes de persistir ParametrosGlobais.");

    }

    private static ParametrosGlobaisFacade criaParametrosGlobaisFrontService(
            ParametrosGlobaisService parametrosGlobaisService) {

        ParametrosGlobaisFacade parametrosGlobaisFrontService = new ParametrosGlobaisFacade();
        try {
            Field parametrosGlobaisServiceField =
                    ParametrosGlobaisFacade.class.getDeclaredField("parametrosGlobaisService");
            parametrosGlobaisServiceField.setAccessible(true);
            parametrosGlobaisServiceField.set(parametrosGlobaisFrontService, parametrosGlobaisService);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Nao foi possivel injetar ParametrosGlobaisService no teste", exception);
        }
        return parametrosGlobaisFrontService;

    }

    private static ParametrosGlobais criaParametrosGlobaisCommunityNeutro() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT);
        parametrosGlobais.setModeloDemandaBase(Constantes.DPModeloDemandaBase.DESATIVADO);
        parametrosGlobais.setModeloNormalizacao(Constantes.DPModeloNormalizacao.DESATIVADO);
        /*
         * Alguns getters de ParametrosGlobais preservam defaults historicos
         * true quando o campo fisico esta nulo. O payload neutro do teste
         * representa o que a tela Community deve enviar depois do saneamento do
         * service de modelo: capacidade bloqueada explicitamente desligada.
         */
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineProduto(false);
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineLocation(false);
        parametrosGlobais.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(false);
        parametrosGlobais.setQuantidadesEmPedidosRepresentamSaldoRestante(false);
        parametrosGlobais.setConsideraPedidosBacklog(false);
        parametrosGlobais.setNumeroDiasProdutoNovo(0);
        parametrosGlobais.setNumeroDiasLocationNova(0);
        return parametrosGlobais;

    }

    private static class CapturingParametrosGlobaisService extends ParametrosGlobaisService {

        private final AtomicInteger saveCallCount = new AtomicInteger();
        private ParametrosGlobais savedParametrosGlobais;

        @Override
        public ParametrosGlobais saveParametrosGlobais(ParametrosGlobais parametrosGlobais) {

            saveCallCount.incrementAndGet();
            savedParametrosGlobais = parametrosGlobais;
            return parametrosGlobais;

        }

    }

}
