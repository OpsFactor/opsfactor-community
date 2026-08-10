package com.opsfactor.community.web.restcontroller.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.configuration.facade.ParametrosGlobaisFacade;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import jakarta.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Valida o contrato Community da API de Global Parameters.
 *
 * <p>A tela compartilhada pode continuar enviando campos Enterprise, mas esta
 * borda deve aceitar apenas sell-out e parametros operacionais basicos.</p>
 */
class ParametrosGlobaisControllerTest {

    private static final Set<String> COMMUNITY_ACCEPTED_FIELD_NAMES = Set.of(
            "id",
            "timeZone",
            "modeloCadastroProdutoLocation",
            "demandPlanningHistoricalDisplayPeriods",
            "horizonteForecastDias",
            "diasHistoricosForecastEstatistico",
            "demandPlanningGenerateForecastForDiscontinuedMaterials",
            "diasHorizonteCongelado",
            "unidadeMedidaPadraoDP",
            "safetyStockConsiderIndirectDemand",
            "unidadeMedidaPadraoSNP",
            "exibeLocationsClienteFinalLowLevelCode");

    @Test
    void validaParametrosGlobaisCommunityShouldAcceptCommunityDefaults() throws Exception {

        invokeValidaParametrosGlobaisCommunity(
                criaParametrosGlobaisDTOCommunity());

    }

    @Test
    void updateParametrosGlobaisApiShouldCopySafetyStockIndirectDemandToSharedEntity() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        ParametrosGlobaisController parametrosGlobaisController =
                criaControllerComParametrosGlobais(parametrosGlobais);
        ParametrosGlobaisController.ParametrosGlobaisDTO dto =
                criaParametrosGlobaisDTOCommunity();
        dto.setSafetyStockConsiderIndirectDemand(false);

        ParametrosGlobaisController.ParametrosGlobaisDTO resposta =
                parametrosGlobaisController.updateParametrosGlobaisApi(dto);

        Assertions.assertFalse(parametrosGlobais.getIncluiDemandaIndiretaNoSafetyStock());
        Assertions.assertEquals(Boolean.FALSE, resposta.getSafetyStockConsiderIndirectDemand());

    }

    @Test
    void updateParametrosGlobaisApiShouldRoundTripDemandPlanningHistoricalDisplayPeriods() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        ParametrosGlobaisController parametrosGlobaisController =
                criaControllerComParametrosGlobais(parametrosGlobais);
        ParametrosGlobaisController.ParametrosGlobaisDTO dto =
                criaParametrosGlobaisDTOCommunity();
        dto.setDemandPlanningHistoricalDisplayPeriods(18);

        ParametrosGlobaisController.ParametrosGlobaisDTO resposta =
                parametrosGlobaisController.updateParametrosGlobaisApi(dto);

        Assertions.assertEquals(18, parametrosGlobais.getPeriodosHistoricosTelaDP());
        Assertions.assertEquals(18, resposta.getDemandPlanningHistoricalDisplayPeriods());

    }

    @Test
    void updateParametrosGlobaisApiShouldRoundTripDiscontinuedMaterialForecastDefault() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        ParametrosGlobaisController parametrosGlobaisController =
                criaControllerComParametrosGlobais(parametrosGlobais);
        ParametrosGlobaisController.ParametrosGlobaisDTO dto =
                criaParametrosGlobaisDTOCommunity();
        dto.setDemandPlanningGenerateForecastForDiscontinuedMaterials(false);

        ParametrosGlobaisController.ParametrosGlobaisDTO resposta =
                parametrosGlobaisController.updateParametrosGlobaisApi(dto);

        Assertions.assertFalse(parametrosGlobais.getDpGeraForecastParaDescontinuados());
        Assertions.assertEquals(
                Boolean.FALSE,
                resposta.getDemandPlanningGenerateForecastForDiscontinuedMaterials());

    }

    @Test
    void updateParametrosGlobaisApiShouldRoundTripLowLevelCodeFinalCustomerLocationVisibility() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        ParametrosGlobaisController parametrosGlobaisController =
                criaControllerComParametrosGlobais(parametrosGlobais);
        ParametrosGlobaisController.ParametrosGlobaisDTO dto =
                criaParametrosGlobaisDTOCommunity();
        dto.setExibeLocationsClienteFinalLowLevelCode(true);

        ParametrosGlobaisController.ParametrosGlobaisDTO resposta =
                parametrosGlobaisController.updateParametrosGlobaisApi(dto);

        Assertions.assertTrue(parametrosGlobais.getExibeLocationsClienteFinalLowLevelCode());
        Assertions.assertEquals(
                Boolean.TRUE,
                resposta.getExibeLocationsClienteFinalLowLevelCode());

    }

    @Test
    void getParametrosGlobaisApiShouldExposeLowLevelCodeFinalCustomerLocationVisibility() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setExibeLocationsClienteFinalLowLevelCode(true);
        ParametrosGlobaisController parametrosGlobaisController =
                criaControllerComParametrosGlobais(parametrosGlobais);

        ParametrosGlobaisController.ParametrosGlobaisDTO resposta =
                parametrosGlobaisController.getParametrosGlobaisApi();

        Assertions.assertEquals(
                Boolean.TRUE,
                resposta.getExibeLocationsClienteFinalLowLevelCode());

    }

    @Test
    void fromCommunityShouldExposeDefaultDemandPlanningHistoricalDisplayPeriods() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();

        Assertions.assertEquals(
                120,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getDemandPlanningHistoricalDisplayPeriods());

    }

    @Test
    void fromCommunityShouldExposeEffectiveDiscontinuedMaterialForecastDefault() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();

        Assertions.assertEquals(
                Boolean.TRUE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getDemandPlanningGenerateForecastForDiscontinuedMaterials());

        parametrosGlobais.setDpGeraForecastParaDescontinuados(false);

        Assertions.assertEquals(
                Boolean.FALSE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getDemandPlanningGenerateForecastForDiscontinuedMaterials());

    }

    @Test
    void fromCommunityShouldNeutralizeForecastRoundingToSalesUom() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setDpArredondaParaUnidadeVenda(true);

        Assertions.assertEquals(
                Boolean.FALSE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getDpArredondaParaUnidadeVenda());

    }

    @Test
    void fromCommunityShouldExposeEffectiveSafetyStockIndirectDemandDefault() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();

        Assertions.assertEquals(
                Boolean.TRUE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getSafetyStockConsiderIndirectDemand());

        parametrosGlobais.setIncluiDemandaIndiretaNoSafetyStock(false);

        Assertions.assertEquals(
                Boolean.FALSE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getSafetyStockConsiderIndirectDemand());

    }

    @Test
    void fromCommunityShouldExposeEffectiveLowLevelCodeFinalCustomerLocationVisibility() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();

        Assertions.assertEquals(
                Boolean.FALSE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getExibeLocationsClienteFinalLowLevelCode());

        parametrosGlobais.setExibeLocationsClienteFinalLowLevelCode(true);

        Assertions.assertEquals(
                Boolean.TRUE,
                ParametrosGlobaisController.ParametrosGlobaisDTO.fromCommunity(parametrosGlobais)
                        .getExibeLocationsClienteFinalLowLevelCode());

    }

    @Test
    void unidadeMedidaHelpersShouldDeclareNullableContracts() throws Exception {

        /*
         * Unidade de medida padrao e opcional no DTO HTTP. Null ou string vazia
         * significam "sem unidade configurada", enquanto ids preenchidos sao
         * resolvidos pelo service de master data no fluxo produtivo.
         */
        assertNullableMethod(
                ParametrosGlobaisController.class,
                "resolveUnidadeMedida",
                String.class);
        assertNullableMethod(
                ParametrosGlobaisController.ParametrosGlobaisDTO.class,
                "resolveUnidadeMedidaId",
                UnidadeMedida.class);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldAcceptJsonPropertyLabelsForNeutralEnums() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setTipoDocumentoVenda("Sell-out");
        parametrosGlobaisDTO.setModeloDemandaBase("Inactive");
        parametrosGlobaisDTO.setModeloNormalizacao("Inactive");

        /*
         * RuntimeInfo publica labels JSON para o front compartilhado. A API de
         * Global Parameters aceita esses labels nos campos String transicionais,
         * mas continua aplicando exatamente o mesmo recorte Community.
         */
        Assertions.assertDoesNotThrow(() -> invokeValidaParametrosGlobaisCommunity(
                parametrosGlobaisDTO));

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectNonPositiveForecastHorizon() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setHorizonteForecastDias(0);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidaParametrosGlobaisCommunity(parametrosGlobaisDTO));

        Assertions.assertEquals(
                "Global Parameters forecast horizon must be positive.",
                illegalArgumentException.getMessage());

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectNonPositiveDemandPlanningHistoricalDisplayPeriods() {

        for (int invalidValue : new int[] {0, -1}) {
            ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                    criaParametrosGlobaisDTOCommunity();
            parametrosGlobaisDTO.setDemandPlanningHistoricalDisplayPeriods(invalidValue);

            IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> invokeValidaParametrosGlobaisCommunity(parametrosGlobaisDTO));

            Assertions.assertEquals(
                    "Global Parameters Demand Planning historical display periods must be positive.",
                    illegalArgumentException.getMessage());
        }

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectNonPositiveStatisticalHistoryWindow() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setDiasHistoricosForecastEstatistico(0);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidaParametrosGlobaisCommunity(parametrosGlobaisDTO));

        Assertions.assertEquals(
                "Global Parameters statistical forecast history window must be positive.",
                illegalArgumentException.getMessage());

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectNegativeFrozenHorizon() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setDiasHorizonteCongelado(-1);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidaParametrosGlobaisCommunity(parametrosGlobaisDTO));

        Assertions.assertEquals(
                "Global Parameters frozen horizon must be zero or positive.",
                illegalArgumentException.getMessage());

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectEveryNonCommunityField() throws Exception {

        /*
         * O DTO de Global Parameters e uma borda pequena mas central do front
         * compartilhado. Campos fora da allowlist Community devem falhar com
         * RequiresEnterpriseVersionException quando recebem valor ativo.
         */
        for (Field field : ParametrosGlobaisController.ParametrosGlobaisDTO.class.getDeclaredFields()) {
            if (field.isSynthetic() || COMMUNITY_ACCEPTED_FIELD_NAMES.contains(field.getName())) {
                continue;
            }

            Object enterpriseFieldValue = getEnterpriseFieldValue(field);
            Assertions.assertNotNull(
                    enterpriseFieldValue,
                    "Campo sem valor de teste Enterprise configurado: " + field.getName());

            ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                    criaParametrosGlobaisDTOCommunity();
            field.setAccessible(true);
            field.set(parametrosGlobaisDTO, enterpriseFieldValue);

            assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);
        }

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectSellInOrSalesOrders() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLIN.name());

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectSellInJsonPropertyLabel() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setTipoDocumentoVenda("Sell-in");

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectSalesOrdersDocumentType() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.PEDIDO.name());

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectStockoutAndOutlierNormalization() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisStockoutDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisStockoutDTO.setModeloDemandaBase(Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO.name());

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisOutlierDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisOutlierDTO.setModeloNormalizacao(Constantes.DPModeloNormalizacao.PERCENTIS.name());

        assertRequiresEnterpriseVersionException(parametrosGlobaisStockoutDTO);
        assertRequiresEnterpriseVersionException(parametrosGlobaisOutlierDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectStockoutDohWindow() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setDiasHistoricosDoh(30);

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectEnterpriseOperationalFlags() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setQuantidadesEmPedidosRepresentamSaldoRestante(true);

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectAggregatedAdjustments() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setPermiteAjusteAgregadoSemBaselineProduto(true);

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectAggregatedLocationAdjustments() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setPermiteAjusteAgregadoSemBaselineLocation(true);

        /*
         * O Planning Book Community trabalha no nivel material/location. Ajuste
         * agregado por location deve falhar como o ajuste agregado por material.
         */
        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectDeliveriesConsumingAvailability() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(true);

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectFleetCapacityUnits() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setUnidadeMedidaPadraoCapacidadeLogisticaPeso("KG");

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectFleetCapacityVolumeUnits() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setUnidadeMedidaPadraoCapacidadeLogisticaVolume("M3");

        /*
         * Frotas e capacidade logistica por volume pertencem ao Enterprise; a
         * API Community aceita apenas capacidade produtiva operacional.
         */
        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectCurveCalculation() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setDiasHistoricosCurva(90);

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectNewMaterialTreatment() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisDTO.setNumeroDiasMaterialNovo(30);

        assertRequiresEnterpriseVersionException(parametrosGlobaisDTO);

    }

    @Test
    void validaParametrosGlobaisCommunityShouldRejectNegativeDisabledEnterpriseIntegers() {

        /*
         * Esses campos pertencem a capabilities Enterprise e nao sao copiados
         * para a entidade pelo controller Community. Zero e ausencia sao
         * neutros; qualquer outro valor, inclusive negativo, deve falhar para
         * evitar payload invalido aceito e ignorado silenciosamente.
         */
        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisStockoutDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisStockoutDTO.setDiasHistoricosDoh(-1);

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisCurvaDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisCurvaDTO.setDiasHistoricosCurva(-1);

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisMaterialNovoDTO =
                criaParametrosGlobaisDTOCommunity();
        parametrosGlobaisMaterialNovoDTO.setNumeroDiasMaterialNovo(-1);

        assertRequiresEnterpriseVersionException(parametrosGlobaisStockoutDTO);
        assertRequiresEnterpriseVersionException(parametrosGlobaisCurvaDTO);
        assertRequiresEnterpriseVersionException(parametrosGlobaisMaterialNovoDTO);

    }

    @Test
    void parametrosGlobaisDTOShouldRejectHistoricalNumeroDiasProdutoNovoField() {

        Assertions.assertThrows(
                JsonProcessingException.class,
                () -> new ObjectMapper().readValue(
                        "{\"numeroDiasProdutoNovo\":30}",
                        ParametrosGlobaisController.ParametrosGlobaisDTO.class));

    }

    @Test
    void parametrosGlobaisDTOShouldRoundTripCanonicalNumeroDiasMaterialNovoField() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO = objectMapper.readValue(
                "{\"numeroDiasMaterialNovo\":30}",
                ParametrosGlobaisController.ParametrosGlobaisDTO.class);

        Assertions.assertEquals(30, parametrosGlobaisDTO.getNumeroDiasMaterialNovo());
        Assertions.assertTrue(
                objectMapper.writeValueAsString(parametrosGlobaisDTO)
                        .contains("\"numeroDiasMaterialNovo\""));

    }

    @Test
    void aplicaParametrosEnterpriseDesabilitadosCommunityShouldClearTransactionalBacklog() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLIN);
        parametrosGlobais.setConsideraPedidosBacklog(true);

        invokeAplicaParametrosEnterpriseDesabilitadosCommunity(
                parametrosGlobais);

        Assertions.assertEquals(
                Constantes.TipoDocumentoVenda.SELLOUT,
                parametrosGlobais.getTipoDocumentoVenda());
        Assertions.assertFalse(parametrosGlobais.isConsideraPedidosBacklogCadastradoAtivo());

    }

    private static ParametrosGlobaisController.ParametrosGlobaisDTO criaParametrosGlobaisDTOCommunity() {

        ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO =
                new ParametrosGlobaisController.ParametrosGlobaisDTO();
        parametrosGlobaisDTO.setTipoDocumentoVenda(Constantes.TipoDocumentoVenda.SELLOUT.name());
        parametrosGlobaisDTO.setModeloDemandaBase(Constantes.DPModeloDemandaBase.DESATIVADO.name());
        parametrosGlobaisDTO.setDiasHistoricosDoh(0);
        parametrosGlobaisDTO.setModeloNormalizacao(Constantes.DPModeloNormalizacao.DESATIVADO.name());
        parametrosGlobaisDTO.setPermiteAjusteAgregadoSemBaselineProduto(false);
        parametrosGlobaisDTO.setPermiteAjusteAgregadoSemBaselineLocation(false);
        parametrosGlobaisDTO.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(false);
        parametrosGlobaisDTO.setUnidadeMedidaPadraoCapacidadeLogisticaPeso(null);
        parametrosGlobaisDTO.setUnidadeMedidaPadraoCapacidadeLogisticaVolume(null);
        parametrosGlobaisDTO.setDiasHistoricosCurva(0);
        parametrosGlobaisDTO.setNumeroDiasMaterialNovo(0);
        parametrosGlobaisDTO.setQuantidadesEmPedidosRepresentamSaldoRestante(false);
        return parametrosGlobaisDTO;

    }

    private static ParametrosGlobaisController criaControllerComParametrosGlobais(
            ParametrosGlobais parametrosGlobais) throws ReflectiveOperationException {

        ParametrosGlobaisController parametrosGlobaisController = new ParametrosGlobaisController();
        CapturingParametrosGlobaisFrontService parametrosGlobaisFrontService =
                new CapturingParametrosGlobaisFrontService(parametrosGlobais);
        injetaCampo(
                parametrosGlobaisController,
                "parametrosGlobaisFrontService",
                parametrosGlobaisFrontService);
        injetaCampo(
                parametrosGlobaisController,
                "parametrosGlobaisControllerPolicy",
                new ParametrosGlobaisControllerPolicy());
        return parametrosGlobaisController;

    }

    private static void injetaCampo(Object target, String fieldName, Object fieldValue)
            throws ReflectiveOperationException {

        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, fieldValue);

    }

    private static void assertRequiresEnterpriseVersionException(
            ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO) {

        Assertions.assertThrows(
                RequiresEnterpriseVersionException.class,
                () -> invokeValidaParametrosGlobaisCommunity(
                        parametrosGlobaisDTO));

    }

    private static void invokeValidaParametrosGlobaisCommunity(
            ParametrosGlobaisController.ParametrosGlobaisDTO parametrosGlobaisDTO) {

        new ParametrosGlobaisControllerPolicy().validaParametrosGlobaisDTO(parametrosGlobaisDTO);

    }

    private static void invokeAplicaParametrosEnterpriseDesabilitadosCommunity(
            ParametrosGlobais parametrosGlobais) {

        new ParametrosGlobaisControllerPolicy().aplicaParametrosEdicao(
                parametrosGlobais,
                criaParametrosGlobaisDTOCommunity());

    }

    private static Object getEnterpriseFieldValue(Field field) {

        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        if ("tipoDocumentoVenda".equals(fieldName)) {
            return Constantes.TipoDocumentoVenda.SELLIN.name();
        }
        if ("modeloDemandaBase".equals(fieldName)) {
            return Constantes.DPModeloDemandaBase.DOH_ESTOQUE_FIM_PERIODO.name();
        }
        if ("modeloNormalizacao".equals(fieldName)) {
            return Constantes.DPModeloNormalizacao.PERCENTIS.name();
        }
        if ("diasHistoricosNormalizacao".equals(fieldName)) {
            return Constantes.DP_PADRAO_DIAS_NORMALIZACAO + 1;
        }
        if ("percentilOutliersVenda".equals(fieldName)) {
            return Constantes.DP_PADRAO_PERCENTIL_OUTLIERS_VENDA + 0.01d;
        }
        if (String.class.equals(fieldType)) {
            return "enterprise-value";
        }
        if (Integer.class.equals(fieldType)) {
            return 30;
        }
        if (Double.class.equals(fieldType)) {
            return 0.99d;
        }
        if (Boolean.class.equals(fieldType)) {
            return true;
        }
        return null;

    }

    private static void assertNullableMethod(
            Class<?> ownerClass,
            String methodName,
            Class<?>... parameterTypes) throws NoSuchMethodException {

        Method method = ownerClass.getDeclaredMethod(methodName, parameterTypes);
        Assertions.assertTrue(
                method.isAnnotationPresent(Nullable.class),
                methodName + " deve declarar @Nullable porque unidade de medida vazia e contrato HTTP valido.");

    }

    /**
     * Dobra da fachada para provar o mapeamento controller--entidade sem
     * depender de Spring ou repository. A entidade capturada e o mesmo
     * aggregate que o controller recebe e devolve na atualizacao HTTP.
     */
    private static class CapturingParametrosGlobaisFrontService extends ParametrosGlobaisFacade {

        private final ParametrosGlobais parametrosGlobais;

        private CapturingParametrosGlobaisFrontService(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

        @Override
        public ParametrosGlobais saveParametrosGlobais(ParametrosGlobais parametrosGlobais) {

            Assertions.assertSame(this.parametrosGlobais, parametrosGlobais);
            return parametrosGlobais;

        }

    }

}
