package com.opsfactor.community.capability.supplyplanning.service.heuristic;

import com.opsfactor.community.capability.lowlevelcode.engine.LowLevelCode;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regressões de fronteira para o lifecycle da fotografia central heurística. */
class NivelamentoCapacidadePlanoIrrestritoHeuristicoServiceCommunityContractTest {

    @Test
    void flagDesligadoNaoExigeFotografiaNemCapacidade() {

        PerfilExecucaoSupplyPlan perfil = new PerfilExecucaoSupplyPlan();
        assertFalse(new NivelamentoCapacidadePlanoIrrestritoHeuristicoService().aplica(
                null, perfil, null, null, null, null));

    }

    @Test
    void heuristicServiceDeveGerarRestritoAntesDoResidualIrrestrito() throws Exception {

        Path sourcePath = Path.of("src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/HeuristicoService.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        int inicializacaoRestrita = source.indexOf("supplyPlanningBiProjection.atualizaPlanoRestritoComPlanoIrrestrito()");
        int geracaoRestrita = source.indexOf("executaPlanoRestritoNiveladoPorLowLevelCode(");
        int inicializacaoIrrestrita = source.indexOf(
                "supplyPlanningBiProjection.atualizaPlanoIrrestritoComPlanoRestritoSemSobrescreverDemanda()");
        int geracaoIrrestrita = source.indexOf(
                "Constantes.TipoPlano.PLANO_IRRESTRITO,",
                inicializacaoIrrestrita);
        int checkpointFinal = source.indexOf(
                "salvaCheckpointSupplyPlanningBiProjection(supplyPlanningBiProjection, true)");

        assertTrue(inicializacaoRestrita >= 0
                        && inicializacaoRestrita < geracaoRestrita
                        && geracaoRestrita < inicializacaoIrrestrita,
                "O plano restrito deve ser gerado e nivelado antes da complementação irrestrita.");
        assertTrue(inicializacaoIrrestrita < geracaoIrrestrita
                        && geracaoIrrestrita < checkpointFinal,
                "O irrestrito deve partir do restrito e acrescentar somente o residual antes do checkpoint.");
        assertFalse(source.contains("if (nivelamentoAplicado)"),
                "A cadeia dependente não deve nascer inteira antes do primeiro nivelamento.");
        assertFalse(source.contains("constrainedPlanService.restringePlano("),
                "O restrito já nivelado não pode sofrer uma segunda passada de restrição.");
        assertTrue(source.contains("atualizaEstoquesDoPlano(")
                        && source.indexOf("atualizaEstoquesDoPlano(") < inicializacaoIrrestrita,
                "O estoque restrito deve ser reprojetado depois dos níveis e antes da cópia ao irrestrito.");

    }

    @Test
    void residualIrrestritoDevePartirDoRestritoSemSobrescreverDemanda() throws Exception {

        Path projectionSourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/supplyplan/projection/"
                        + "SupplyPlanningBiProjection.java");
        String projectionSource = Files.readString(projectionSourcePath, StandardCharsets.UTF_8);

        assertTrue(projectionSource.contains(
                        "atualizaPlanoIrrestritoComPlanoRestritoSemSobrescreverDemanda()")
                        && projectionSource.contains(
                        "productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoRestrita()"),
                "O residual irrestrito deve partir da produção restrita da mesma fotografia.");

    }

    @Test
    void firmesDevemSerReservadosAntesDoFairShare() throws Exception {

        String source = readLevelingSource();
        assertTrue(source.contains("Constantes.FirmePlanejado.ORDEM")
                        && source.indexOf("Constantes.FirmePlanejado.ORDEM") < source.indexOf("aplicaFairShare("),
                "Ordens firmes devem reduzir a capacidade residual antes do fair-share planejado.");

    }

    @Test
    void origensDevemRespeitarPrioridadeAntesDeAbrirAlternativaSeguinte() throws Exception {

        String source = readLevelingSource();
        assertTrue(source.contains("TreeSet<Integer> prioridadesOrigem")
                        && source.contains("for (Integer prioridadeOrigem : prioridadesOrigem)"),
                "O nivelamento deve percorrer origens pela prioridade efetiva da lane.");

    }

    @Test
    void capacidadeDeOrigemCompartilhadaDeveSerRateadaEntreNecessidadesConcorrentes() throws Exception {

        String source = readLevelingSource();
        assertTrue(source.contains("quantidadeCandidatosPorNecessidade")
                        && source.contains("quantidadeResidual() / quantidadeCandidatosPorNecessidade"),
                "Candidatos da mesma necessidade devem receber fração antes do comprometimento de capacidade.");

    }

    @Test
    void origemAlternativaDevePermitirBuildAheadAntesDaExpedicao() throws Exception {

        String source = readLevelingSource();
        assertTrue(source.contains("posicaoPeriodoExpedicao() - antecipacao")
                        && source.contains("getPosicaoPeriodoPresente()"),
                "Build-ahead deve caminhar de expedição para períodos anteriores sem atravessar o presente.");

    }

    @Test
    void origemAlternativaDeveEstarEmLowLevelCodeEstritamentePosterior() {

        NivelamentoCapacidadePlanoIrrestritoHeuristicoService service =
                new NivelamentoCapacidadePlanoIrrestritoHeuristicoService();
        LowLevelCode lowLevelCode = Mockito.mock(LowLevelCode.class);
        Location locationOrigem = Mockito.mock(Location.class);
        Produto material = Mockito.mock(Produto.class);

        Mockito.when(lowLevelCode.getLowLevelCode(locationOrigem, material))
                .thenReturn(Optional.of(2), Optional.of(3), Optional.of(4), Optional.empty());

        assertFalse(service.origemAlternativaPertenceALowLevelCodePosterior(
                        lowLevelCode, 3, locationOrigem, material),
                "Uma DFU já processada não pode ser reaberta pelo rebalanceamento.");
        assertFalse(service.origemAlternativaPertenceALowLevelCodePosterior(
                        lowLevelCode, 3, locationOrigem, material),
                "Uma DFU do mesmo nível também deve ser rejeitada, independentemente da ordem das locations.");
        assertTrue(service.origemAlternativaPertenceALowLevelCodePosterior(
                        lowLevelCode, 3, locationOrigem, material),
                "Somente uma DFU de nível posterior pode receber produção rebalanceada.");
        assertFalse(service.origemAlternativaPertenceALowLevelCodePosterior(
                        lowLevelCode, 3, locationOrigem, material),
                "Uma DFU sem posição topológica não pode ser usada como alternativa.");
        assertFalse(service.origemAlternativaPertenceALowLevelCodePosterior(
                        null, 3, locationOrigem, material),
                "Sem contexto topológico, o fluxo deve falhar fechado para origens remotas.");

    }

    @Test
    void planoRestritoDeveNivelarCadaLowLevelCodeAntesDeGerarOProximo() throws Exception {

        Path heuristicSourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/"
                        + "HeuristicoService.java");
        String heuristicSource = Files.readString(heuristicSourcePath, StandardCharsets.UTF_8);
        Path supplyPlanningSourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/engine/SupplyPlanning.java");
        String supplyPlanningSource = Files.readString(supplyPlanningSourcePath, StandardCharsets.UTF_8);
        String levelingSource = readLevelingSource();

        assertFalse(supplyPlanningSource.contains("priorizaInboundProducaoViavel"),
                "Uma lane inbound não pode suprimir a produção local viável.");
        int metodoRestrito = heuristicSource.indexOf("executaPlanoRestritoNiveladoPorLowLevelCode(");
        int loopLowLevelCode = heuristicSource.indexOf("for (int posicaoLowLevelCode", metodoRestrito);
        int fotografiaAntesDoNivel = heuristicSource.indexOf("capturaFotografiaPlano(", loopLowLevelCode);
        int geracaoDoNivel = heuristicSource.indexOf("executaPlanoPosicaoLowLevelCode(", fotografiaAntesDoNivel);
        int nivelamentoDoNivel = heuristicSource.indexOf("aplicaIncrementosGeradosApos(", geracaoDoNivel);

        assertTrue(metodoRestrito >= 0
                        && loopLowLevelCode > metodoRestrito
                        && fotografiaAntesDoNivel > loopLowLevelCode
                        && geracaoDoNivel > fotografiaAntesDoNivel
                        && nivelamentoDoNivel > geracaoDoNivel,
                "Cada nível deve ser gerado e nivelado antes de avançar para seus componentes.");
        assertFalse(heuristicSource.contains("possuiAlteracoes("),
                "O fluxo acíclico por nível não deve preservar uma cadeia dependente antiga em passagens globais.");
        assertTrue(heuristicSource.contains("lowLevelCode,")
                        && heuristicSource.contains("posicaoLowLevelCode);"),
                "O nivelamento deve receber a posição topológica corrente para filtrar origens remotas.");
        assertTrue(levelingSource.contains("quantidadePlanejadaTotal - quantidadeNivelavel")
                        && levelingSource.contains("quantidadeAtual - necessidade.quantidadeOriginal()"),
                "A capacidade existente deve ficar reservada e somente o novo delta pode ser realocado.");

    }

    @Test
    void restritoDeveDescartarResidualSemCapacidadeEIrrestritoDeveReporNaPrimaria() throws Exception {

        String source = readLevelingSource();
        assertTrue(source.contains("boolean mantemResidualNaOrigemPrimaria")
                        && source.contains(
                        "if (mantemResidualNaOrigemPrimaria && necessidade.quantidadeResidual() > EPSILON)"),
                "Somente o irrestrito pode devolver residual sem capacidade à origem primária.");

    }

    @Test
    void restritoDeveSelecionarLaneSecundariaQueAtendaLeadTime() throws Exception {

        Path supplyPlanningSourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/engine/SupplyPlanning.java");
        String source = Files.readString(supplyPlanningSourcePath, StandardCharsets.UTF_8);

        assertTrue(source.contains("getLinhaTransporteInboundViavelParaDataNecessidade(")
                        && source.contains("getLinhaTransporteInboundViavelListOrdenadaPorPrioridade(")
                        && source.contains("posicaoPeriodoNecessidade - leadTimePeriodos")
                        && source.contains(".findFirst()"),
                "O restrito deve escolher a primeira lane por prioridade que ainda cumpra a data necessária.");

    }

    @Test
    void persistenciaDeFirmesDeveSerCondicionadaPelaSpiEnterpriseSemLiberarDistribuicao() throws Exception {

        Path sourcePath = Path.of("src/main/java/com/opsfactor/community/capability/supplyplanning/service/SupplyPlanService.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        assertTrue(source.contains("SupplyPlanFirmProductionOrdersSpi")
                        && source.contains("deveNeutralizarOrdensFirmesProducaoCommunity()"),
                "Produção firme deve depender da capability Enterprise opcional.");
        assertTrue(source.contains("distributionPlanItemCollection.forEach(this::neutralizaOrdensFirmesCommunity)"),
                "Transferências firmes devem continuar neutralizadas no Community.");

    }

    private String readLevelingSource() throws Exception {

        return Files.readString(Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/"
                        + "NivelamentoCapacidadePlanoIrrestritoHeuristicoService.java"), StandardCharsets.UTF_8);

    }

}
