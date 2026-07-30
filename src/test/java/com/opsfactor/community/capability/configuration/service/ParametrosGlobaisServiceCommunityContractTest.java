package com.opsfactor.community.capability.configuration.service;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.repository.ParametrosGlobaisRepository;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contratos Community do service de modelo de ParametrosGlobais.
 *
 * <p>A fachada web bloqueia payloads Enterprise, mas este service tambem e uma
 * fronteira interna importante: calculos podem receber a entidade diretamente
 * via `ParametrosGlobaisService`. Por isso leitura e salvamento precisam
 * neutralizar dados legados antes de qualquer Planning Book, forecast ou Supply
 * Planning observar defaults historicos incompatíveis com o Community.</p>
 */
public class ParametrosGlobaisServiceCommunityContractTest {

    @Test
    public void serviceShouldUseExplicitAutowiredRepository() throws Exception {

        Field parametrosGlobaisRepositoryField =
                ParametrosGlobaisService.class.getDeclaredField("parametrosGlobaisRepository");
        Autowired autowired = parametrosGlobaisRepositoryField.getAnnotation(Autowired.class);

        Assertions.assertNotNull(
                autowired,
                "ParametrosGlobaisService.parametrosGlobaisRepository deve declarar @Autowired explicitamente.");
        Assertions.assertTrue(
                autowired.required(),
                "ParametrosGlobaisService.parametrosGlobaisRepository deve ser bean obrigatorio.");

    }

    @Test
    public void getParametrosGlobaisShouldApplyCommunityCutToLegacyDatabaseValues() {

        ParametrosGlobais parametrosGlobaisLegado = criaParametrosGlobaisComValoresEnterprise();
        parametrosGlobaisLegado.setId(0L);
        ParametrosGlobaisService parametrosGlobaisService = criaParametrosGlobaisService(
                parametrosGlobaisLegado,
                new AtomicReference<>());

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();

        assertParametrosGlobaisCommunity(parametrosGlobais);

    }

    @Test
    public void saveParametrosGlobaisShouldApplyCommunityCutBeforeRepositorySave() {

        AtomicReference<ParametrosGlobais> savedParametrosGlobaisReference = new AtomicReference<>();
        ParametrosGlobaisService parametrosGlobaisService = criaParametrosGlobaisService(
                Optional.empty(),
                savedParametrosGlobaisReference);
        ParametrosGlobais parametrosGlobaisLegado = criaParametrosGlobaisComValoresEnterprise();

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.saveParametrosGlobais(parametrosGlobaisLegado);

        Assertions.assertSame(parametrosGlobaisLegado, parametrosGlobais);
        Assertions.assertSame(parametrosGlobaisLegado, savedParametrosGlobaisReference.get());
        assertParametrosGlobaisCommunity(savedParametrosGlobaisReference.get());

    }

    @Test
    public void stockoutDohModelShouldKeepThePersistedOrdinalOfTheRenamedEnterpriseOption() {

        /*
         * ParametrosGlobais persiste DPModeloDemandaBase por ordinal. O nome
         * tecnico foi corrigido para expressar DOH de estoque de fim de
         * periodo, mas sua posicao precisa continuar a mesma para que a base
         * existente seja lida sem DML/DDL.
         */
        Assertions.assertEquals(
                1,
                Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO.ordinal());
        Assertions.assertEquals(
                "DOH_ESTOQUE_FIM_PERIODO",
                Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO.name());

    }

    private static ParametrosGlobaisService criaParametrosGlobaisService(
            ParametrosGlobais parametrosGlobais,
            AtomicReference<ParametrosGlobais> savedParametrosGlobaisReference) {

        return criaParametrosGlobaisService(Optional.of(parametrosGlobais), savedParametrosGlobaisReference);

    }

    private static ParametrosGlobaisService criaParametrosGlobaisService(
            Optional<ParametrosGlobais> parametrosGlobaisOptional,
            AtomicReference<ParametrosGlobais> savedParametrosGlobaisReference) {

        ParametrosGlobaisRepository parametrosGlobaisRepository =
                (ParametrosGlobaisRepository) Proxy.newProxyInstance(
                        ParametrosGlobaisRepository.class.getClassLoader(),
                        new Class<?>[]{ParametrosGlobaisRepository.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "customFindComDependencias" -> parametrosGlobaisOptional;
                            case "save" -> {
                                ParametrosGlobais parametrosGlobais = (ParametrosGlobais) args[0];
                                savedParametrosGlobaisReference.set(parametrosGlobais);
                                yield parametrosGlobais;
                            }
                            case "toString" -> "ParametrosGlobaisRepository test double";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new AssertionError(
                                    "Repository method should not be called by ParametrosGlobaisService test: "
                                            + method.getName());
                        });

        ParametrosGlobaisService parametrosGlobaisService = new ParametrosGlobaisService();
        try {
            Field parametrosGlobaisRepositoryField =
                    ParametrosGlobaisService.class.getDeclaredField("parametrosGlobaisRepository");
            parametrosGlobaisRepositoryField.setAccessible(true);
            parametrosGlobaisRepositoryField.set(parametrosGlobaisService, parametrosGlobaisRepository);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Nao foi possivel injetar ParametrosGlobaisRepository no teste", exception);
        }
        return parametrosGlobaisService;

    }

    private static ParametrosGlobais criaParametrosGlobaisComValoresEnterprise() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setId(999L);
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLIN);
        parametrosGlobais.setModeloDemandaBase(Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO);
        parametrosGlobais.setDiasHistoricosDoh(Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH + 1);
        parametrosGlobais.setDiasHistoricosDohStockout(Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT + 1);
        parametrosGlobais.setModeloNormalizacao(Constantes.DPModeloNormalizacao.PERCENTIS);
        parametrosGlobais.setDiasHistoricosNormalizacao(Constantes.DP_PADRAO_DIAS_NORMALIZACAO + 1);
        parametrosGlobais.setPercentilOutliersVenda(Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA + 0.01d);
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineProduto(true);
        parametrosGlobais.setPermiteAjusteAgregadoSemBaselineLocation(true);
        parametrosGlobais.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(true);
        parametrosGlobais.setQuantidadesEmPedidosRepresentamSaldoRestante(true);
        parametrosGlobais.setConsideraPedidosBacklog(true);
        parametrosGlobais.setNumeroDiasProdutoNovo(30);
        parametrosGlobais.setNumeroDiasLocationNova(30);
        parametrosGlobais.setDiasHistoricosCurva(90);
        parametrosGlobais.setCalculaCustoEstoque(true);
        parametrosGlobais.setDiasHistoricosDeployment(45);
        parametrosGlobais.setUnidadeMedidaPadraoDeployment(new UnidadeMedida("PAL"));
        parametrosGlobais.setUnidadeMedidaPadraoPricing(new UnidadeMedida("BRL"));
        parametrosGlobais.setTamanhoBucketOTB(Constantes.TamanhoBucket.DIARIO);
        parametrosGlobais.setHorizonteOTBDias(60);
        parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaPeso(new UnidadeMedida("KG"));
        parametrosGlobais.setUnidadeMedidaPadraoCapacidadeLogisticaVolume(new UnidadeMedida("M3"));
        parametrosGlobais.setLogDataUploadPedidos(true);
        parametrosGlobais.setLogDataUploadRemessas(true);
        return parametrosGlobais;

    }

    private static void assertParametrosGlobaisCommunity(ParametrosGlobais parametrosGlobais) {

        Assertions.assertEquals(0L, parametrosGlobais.getId());
        Assertions.assertEquals(Constantes.TipoDocumentoVenda.SELLOUT, parametrosGlobais.getTipoDocumentoVenda());
        Assertions.assertEquals(Constantes.DPModeloDemandaBase.DESATIVADO, parametrosGlobais.getModeloDemandaBase());
        Assertions.assertEquals(
                Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH,
                parametrosGlobais.getDiasHistoricosDoh());
        Assertions.assertEquals(
                Constantes.GLOBAL_PADRAO_DIAS_HISTORICOS_DOH_STOCKOUT,
                parametrosGlobais.getDiasHistoricosDohStockout());
        Assertions.assertEquals(Constantes.DPModeloNormalizacao.DESATIVADO, parametrosGlobais.getModeloNormalizacao());
        Assertions.assertEquals(
                Constantes.DP_PADRAO_DIAS_NORMALIZACAO,
                parametrosGlobais.getDiasHistoricosNormalizacao());
        Assertions.assertEquals(
                Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA,
                parametrosGlobais.getPercentilOutliersVenda());
        Assertions.assertFalse(parametrosGlobais.getPermiteAjusteAgregadoSemBaselineProduto());
        Assertions.assertFalse(parametrosGlobais.getPermiteAjusteAgregadoSemBaselineLocation());
        Assertions.assertFalse(parametrosGlobais.getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo());
        Assertions.assertFalse(parametrosGlobais.getQuantidadesEmPedidosRepresentamSaldoRestante());
        Assertions.assertFalse(parametrosGlobais.isConsideraPedidosBacklogCadastradoAtivo());
        Assertions.assertEquals(0, parametrosGlobais.getNumeroDiasProdutoNovo());
        Assertions.assertEquals(0, parametrosGlobais.getNumeroDiasLocationNova());
        Assertions.assertEquals(0, parametrosGlobais.getDiasHistoricosCurva());
        Assertions.assertFalse(parametrosGlobais.getCalculaCustoEstoque());
        Assertions.assertEquals(0, parametrosGlobais.getDiasHistoricosDeployment());
        Assertions.assertEquals("UN", parametrosGlobais.getUnidadeMedidaPadraoDeployment().getId());
        Assertions.assertEquals("UN", parametrosGlobais.getUnidadeMedidaPadraoPricing().getId());
        Assertions.assertEquals(Constantes.TamanhoBucket.MENSAL, parametrosGlobais.getTamanhoBucketOTB());
        Assertions.assertEquals(0, parametrosGlobais.getHorizonteOTBDias());
        Assertions.assertNull(parametrosGlobais.getUnidadeMedidaPadraoCapacidadeLogisticaPesoCadastrado());
        Assertions.assertNull(parametrosGlobais.getUnidadeMedidaPadraoCapacidadeLogisticaVolumeCadastrado());
        Assertions.assertFalse(parametrosGlobais.getLogDataUploadPedidos());
        Assertions.assertFalse(parametrosGlobais.getLogDataUploadRemessas());

    }

}
