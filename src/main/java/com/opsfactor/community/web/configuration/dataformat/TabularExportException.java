package com.opsfactor.community.web.configuration.dataformat;

import java.io.IOException;

/** A flat-file format cannot represent a hierarchical output without losing structure. */
public final class TabularExportException extends IOException {

    public TabularExportException(DataFormat format, String field) {

        super("Não é possível exportar para " + format + ": a saída contém um objeto com múltiplos níveis "
                + "hierárquicos no campo '" + field + "'. Este formato aceita apenas uma tabela plana.");

    }

    /** The HTTP boundary suggests only representations available in the current edition. */
    public String message(DataFormatPolicy policy) {

        return getMessage() + (policy.supports(DataFormat.ARROW) && policy.supports(DataFormat.PARQUET)
                ? " Use JSON, Arrow ou Parquet para preservar a hierarquia."
                : " Consulte a API JSON para preservar a hierarquia.");

    }
}
