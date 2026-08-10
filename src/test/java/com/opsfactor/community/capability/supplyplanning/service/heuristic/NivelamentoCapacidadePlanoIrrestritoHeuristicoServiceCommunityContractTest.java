package com.opsfactor.community.capability.supplyplanning.service.heuristic;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
    void heuristicServiceDevePersistirBaselineAntesDaRestricaoESegundoCheckpointComZeros() throws Exception {

        Path sourcePath = Path.of("src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/HeuristicoService.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        int baselineCheckpoint = source.indexOf("salvaCheckpointSupplyPlanningBiProjection(supplyPlanningBiProjection, false)");
        int leveling = source.indexOf("nivelamentoCapacidadePlanoIrrestritoHeuristicoService.aplica(");
        int zeroCheckpoint = source.indexOf("salvaCheckpointSupplyPlanningBiProjection(supplyPlanningBiProjection, true)");
        int constrainedPlan = source.indexOf("constrainedPlanService.restringePlano(");

        assertTrue(baselineCheckpoint >= 0 && baselineCheckpoint < leveling,
                "O baseline irrestrito deve ser persistido antes do nivelamento opcional.");
        assertTrue(leveling < zeroCheckpoint && zeroCheckpoint < constrainedPlan,
                "O nivelamento deve persistir zeros em checkpoint próprio antes do plano restrito.");
        assertTrue(source.contains("if (nivelamentoAplicado)"),
                "O segundo checkpoint deve ser condicional a uma realocação efetiva.");
        assertTrue(source.contains("lowLevelCode,\n                supplyPlanningBiProjection);"),
                "O constrained plan deve receber a mesma fotografia alterada pelo nivelamento.");

    }

    @Test
    void constrainedDeveCopiarIrrestritoNiveladoNaMesmaFotografia() throws Exception {

        Path constrainedSourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/service/heuristic/"
                        + "ConstrainedPlanService.java");
        String constrainedSource = Files.readString(constrainedSourcePath, StandardCharsets.UTF_8);
        Path projectionSourcePath = Path.of(
                "src/main/java/com/opsfactor/community/capability/supplyplanning/supplyplan/projection/"
                        + "SupplyPlanningBiProjection.java");
        String projectionSource = Files.readString(projectionSourcePath, StandardCharsets.UTF_8);

        int resetSnapshot = constrainedSource.indexOf(
                "supplyPlanningBiProjection.atualizaPlanoRestritoComPlanoIrrestrito()");
        int constrainedLoop = constrainedSource.indexOf("for (int i");

        assertTrue(resetSnapshot >= 0 && resetSnapshot < constrainedLoop,
                "A baseline restrita deve ser materializada no snapshot antes dos cortes de capacidade.");
        assertTrue(projectionSource.contains(
                        "setQuantidadeOrdemPlanejadaProducaoRestrita(\n"
                                + "                    productionPlanLinha.getQuantidadeOrdemPlanejadaProducaoIrrestrita())"),
                "A produção realocada no irrestrito precisa iniciar o constrained na mesma linha em memória.");

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
