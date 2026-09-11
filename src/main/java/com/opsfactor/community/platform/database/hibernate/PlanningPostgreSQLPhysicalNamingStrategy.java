package com.opsfactor.community.platform.database.hibernate;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.nio.charset.StandardCharsets;

/**
 * Mantém a convenção de nomes da plataforma e o limite físico do PostgreSQL.
 *
 * <p>O PostgreSQL padrão armazena no máximo 63 bytes por identificador, mesmo
 * quando o DDL informa um nome maior entre aspas. Sem aplicar o mesmo corte no
 * mapeamento, o Hibernate procura o nome integral nos metadados e tenta adicionar
 * novamente uma coluna que já existe com o nome truncado.</p>
 *
 * <p>Usamos o prefixo que o servidor já armazenou, sem acrescentar hash ou sufixo:
 * mudar essa regra quebraria a compatibilidade com as bases existentes. Nomes
 * distintos com o mesmo prefixo continuam sendo uma colisão real de modelagem;
 * esta estratégia não os renomeia silenciosamente nem ignora erros de DDL.
 * A configuração é exclusiva do perfil PostgreSQL, sem alterar nomes MySQL.</p>
 */
public class PlanningPostgreSQLPhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    /** Limite padrão do PostgreSQL (NAMEDATALEN - 1), em bytes, não caracteres. */
    private static final int MAX_IDENTIFIER_LENGTH_BYTES = 63;

    /**
     * Aplica o limite após a conversão usual para snake_case e minúsculas.
     * O ponto de extensão comum do Hibernate cobre catálogo, schema, tabela,
     * sequência e coluna, preservando inclusive o indicador de nome entre aspas.
     */
    @Override
    protected Identifier getIdentifier(String name, boolean quoted, JdbcEnvironment jdbcEnvironment) {

        Identifier identifier = super.getIdentifier(name, quoted, jdbcEnvironment);
        byte[] identifierBytes = identifier.getText().getBytes(StandardCharsets.UTF_8);
        if (identifierBytes.length <= MAX_IDENTIFIER_LENGTH_BYTES) {
            return identifier;
        }

        // Reproduz o corte do PostgreSQL sem dividir um caractere UTF-8. O byte
        // na posição do corte é o primeiro que NÃO será incluído. Se ele for
        // continuação (10xxxxxx), recuamos até o início do caractere inteiro.
        int truncatedLength = MAX_IDENTIFIER_LENGTH_BYTES;
        while ((identifierBytes[truncatedLength] & 0xC0) == 0x80) {
            truncatedLength--;
        }

        String truncatedName = new String(identifierBytes, 0, truncatedLength, StandardCharsets.UTF_8);
        return new Identifier(truncatedName, identifier.isQuoted());

    }
}
