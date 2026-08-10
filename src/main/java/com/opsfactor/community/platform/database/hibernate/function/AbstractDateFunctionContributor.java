package com.opsfactor.community.platform.database.hibernate.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Classe base usada para registrar as funcoes SQL auxiliares de data
 * consumidas pelas queries JPQL legadas do projeto.
 *
 * Os contributors concretos sao descobertos automaticamente pelo Hibernate 6
 * por meio do arquivo ServiceLoader localizado em:
 * {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}
 *
 * Cada banco registra os mesmos nomes de funcao com padroes SQL especificos do
 * seu dialeto, permitindo preservar as queries atuais durante a migracao para
 * Hibernate 6.
 */
public abstract class AbstractDateFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        Dialect dialect = functionContributions.getDialect();

        if (!supportsDialect(dialect)) {
            return;
        }

        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        /*
         * As projections de agregacao expõem LocalDate. Registrar as funcoes
         * como java.sql.Date funciona no H2/MariaDB por conversao implicita,
         * mas no SQLite o proxy de interface recebe null para o alias de data.
         * O tipo Java 8 explicito mantém o contrato igual nos três dialetos.
         */
        var localDateBasicType = functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.LOCAL_DATE);

        functionRegistry.registerPattern("ULTIMO_DIA_MES_SEM_HORARIO", getUltimoDiaMesSemHorarioPattern(), localDateBasicType);
        functionRegistry.registerPattern("DOMINGO_DA_SEMANA_SEM_HORARIO", getDomingoDaSemanaSemHorarioPattern(), localDateBasicType);
        functionRegistry.registerPattern("DATA_SEM_HORARIO", getDataSemHorarioPattern(), localDateBasicType);
    }

    /**
     * Restringe o registro ao dialeto alvo, permitindo que todos os
     * contributors sejam descobertos globalmente via ServiceLoader sem poluir
     * as configuracoes dos demais bancos.
     */
    protected abstract boolean supportsDialect(Dialect dialect);

    /**
     * Retorna o fragmento SQL que normaliza a data para o ultimo dia do mes.
     */
    protected abstract String getUltimoDiaMesSemHorarioPattern();

    /**
     * Retorna o fragmento SQL que normaliza a data para o domingo da semana.
     */
    protected abstract String getDomingoDaSemanaSemHorarioPattern();

    /**
     * Retorna o fragmento SQL que remove o componente de horario de uma
     * expressao de data/hora.
     */
    protected abstract String getDataSemHorarioPattern();
}
