package com.opsfactor.community.platform.database;

import com.opsfactor.community.platform.calendar.CalendarioSimples;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;

import java.time.LocalDateTime;

/**
 * Planejamento puro da migração opt-in: identifica receitas comprováveis sem
 * abrir banco nem escolher valores implícitos de negócio.
 */
public final class CalendarProfileMigrationPlan {

    private CalendarProfileMigrationPlan() {

    }

    /** Uma pendência explícita impede a gravação daquele cadastro, sem esconder a causa. */
    public record Resultado(TamanhoBucket bucketSize, Integer numberOfBasePeriods, String pendencia) {

        public boolean podeMigrar() {

            return pendencia == null;

        }

    }

    /**
     * Os valores devem vir das colunas, não dos getters com fallback.
     * No legado, horizonte null devolve um período no getter de quantidade, mas
     * apenas um dia no getter de duração. Convertê-lo para um mês mudaria a regra.
     */
    public static Resultado planejarDemand(TamanhoBucket bucketCadastrado, Integer horizonteCadastrado) {

        if (bucketCadastrado == null || horizonteCadastrado == null) {
            return new Resultado(null, null, "Bucket/horizonte ausente: confirmar a receita no cadastro; "
                    + "o horizonte Demand nulo possui defaults históricos divergentes de dias e períodos.");
        }
        if (horizonteCadastrado <= 0) {
            return new Resultado(null, null, "Horizonte Demand deve ser positivo.");
        }
        return new Resultado(bucketCadastrado, horizonteCadastrado, null);

    }

    /**
     * Reutiliza exatamente o resolvedor de datas do plano histórico. Não deriva
     * seu calendário do perfil de execução, que pode ter sido editado depois.
     */
    public static Resultado planejarSupplyPlan(TamanhoBucket bucketCadastrado,
                                               LocalDateTime inicio, LocalDateTime fim) {

        if (bucketCadastrado == null || inicio == null || fim == null) {
            return new Resultado(null, null, "Plano sem bucket ou datas completas: não é seguro reconstruir a receita.");
        }
        if (inicio.isAfter(fim)) {
            return new Resultado(null, null, "Datas históricas invertidas: corrigir a inconsistência antes de migrar.");
        }
        CalendarioSimples calendarioHistorico = CalendarioSimples.criaCalendarioPeriodosFuturosDeDatas(
                bucketCadastrado, inicio, fim);
        return new Resultado(bucketCadastrado, calendarioHistorico.getNumeroPeriodosFuturos(), null);

    }

}
