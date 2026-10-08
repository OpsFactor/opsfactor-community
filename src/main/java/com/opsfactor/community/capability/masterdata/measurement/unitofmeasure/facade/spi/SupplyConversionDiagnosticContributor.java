package com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.spi;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO;

import java.util.Collection;

/**
 * Adds read-only diagnostics from optional edition sources. Community does not
 * supply such sources or load private costs. Implementations must preserve
 * explicit failures and reuse their execution projections instead of querying
 * entities inside material/period loops.
 */
public interface SupplyConversionDiagnosticContributor {

    /** Diagnose the same conversions required by the installed planning engine. */
    Collection<UnidadeConversaoFaltanteDTO> findGaps(SupplyConversionDiagnosticContext context);

}
