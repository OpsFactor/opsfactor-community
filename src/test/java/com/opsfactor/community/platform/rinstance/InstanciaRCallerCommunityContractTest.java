package com.opsfactor.community.platform.rinstance;

import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Contratos de fronteira do caller R Community.
 *
 * <p>Este teste nao executa R. Ele protege apenas o script gerado pelo Java,
 * garantindo que o modulo Community continue restrito aos modelos estatisticos
 * permitidos e nao volte a embutir chamadas R de modelos Enterprise.</p>
 */
class InstanciaRCallerCommunityContractTest {

    private static final List<String> FORBIDDEN_ENTERPRISE_R_SCRIPT_TOKENS = List.of(
            "xreg",
            "tbats",
            "prophet",
            "stl(",
            "mstl",
            "chronos",
            "seas(");

    @Test
    void generatedRScriptShouldNotUseEnterpriseForecastModels() throws IOException {

        Path instanciaRCallerSourcePath = resolveCommunityWorkspaceDirectory()
                .resolve("src/main/java/com/opsfactor/community/platform/rinstance/InstanciaRCaller.java");
        List<String> violations = new ArrayList<>();

        List<String> sourceLines = Files.readAllLines(instanciaRCallerSourcePath, StandardCharsets.UTF_8);
        for (int lineIndex = 0; lineIndex < sourceLines.size(); lineIndex++) {
            String sourceLine = sourceLines.get(lineIndex);

            /*
             * Comentarios podem documentar a fronteira Enterprise. O que nao
             * pode existir no Community e uma chamada efetiva code.addRCode(...)
             * para modelos R que dependem de dados/capabilities privados.
             */
            if (sourceLine.contains("addRCode")
                    && containsForbiddenEnterpriseRScriptToken(sourceLine)) {
                violations.add("InstanciaRCaller.java:" + (lineIndex + 1) + ": " + sourceLine.trim());
            }
        }

        Assertions.assertTrue(
                violations.isEmpty(),
                "InstanciaRCaller Community nao deve gerar script R de modelos Enterprise:\n"
                        + String.join("\n", violations));

    }

    @Test
    void treatedHistoryShouldRejectShortArrayBeforeRRuntime() {

        Calendario calendario = getCalendarioForecastTeste();
        double[] historicoCurto = new double[]{10.0d};

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> InstanciaRCaller.geraForecastAutoArima(
                        historicoCurto,
                        calendario));

        Assertions.assertEquals(
                "Community R ARIMA forecast requires treated historical sales array with at least "
                        + calendario.getNumeroPeriodosPassados()
                        + " past periods, received 1.",
                illegalArgumentException.getMessage());

    }

    @Test
    void treatedHistoryShouldRejectNonFiniteValuesBeforeRRuntime() {

        Calendario calendario = getCalendarioForecastTeste();
        double[] historicoComValorNaoFinito =
                new double[]{
                        10.0d,
                        Double.NaN,
                        30.0d
                };

        IllegalStateException illegalStateException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> InstanciaRCaller.geraForecastExponentialSmoothing(
                        historicoComValorNaoFinito,
                        calendario));

        Assertions.assertEquals(
                "Community R Exponential Smoothing forecast requires finite treated historical values. Index 1 has value NaN.",
                illegalStateException.getMessage());

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

    private static Calendario getCalendarioForecastTeste() {

        return Calendario.criaCalendarioDeOffsetsDias(
                Constantes.TamanhoBucket.DIARIO,
                LocalDateTime.of(2026, 1, 10, 0, 0),
                0,
                3,
                2,
                0);

    }

    private static boolean containsForbiddenEnterpriseRScriptToken(String sourceLine) {

        String lowercaseSourceLine = sourceLine.toLowerCase();
        return FORBIDDEN_ENTERPRISE_R_SCRIPT_TOKENS.stream().anyMatch(lowercaseSourceLine::contains);

    }

}
