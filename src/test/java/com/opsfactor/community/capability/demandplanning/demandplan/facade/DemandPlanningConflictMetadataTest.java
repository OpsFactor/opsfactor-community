package com.opsfactor.community.capability.demandplanning.demandplan.facade;

import com.opsfactor.community.capability.configuration.facade.dto.ConfiguredViewSelectionDTO;
import com.opsfactor.community.capability.configuration.user.projection.ConfiguredViewProjection;
import com.opsfactor.community.capability.planningbook.facade.PlanningBookService;
import com.opsfactor.community.capability.planningbook.facade.dto.PlanningBookDTO;
import com.opsfactor.community.capability.planningbook.keyfigure.projection.KeyFigureProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.service.KeyFigureService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemandPlanningConflictMetadataTest {

    @Test
    void openingPublishesCommunityFamilyAndPreservesExistingImmutableParameters() {
        ExposedFacade facade = fixture(Map.of("existing", "preserved"));

        PlanningBookDTO book = facade.read();

        assertEquals(Set.of("Baseline", "Demand Adjustment", "Direct Demand"), family(book));
        assertEquals("preserved", book.additionalParameters.get("existing"));
    }

    @Test
    void updatedWorkbookAlsoPublishesFamilyWhenOriginalParametersAreAbsent() {
        ExposedFacade facade = fixture(null);

        assertEquals(Set.of("Baseline", "Demand Adjustment", "Direct Demand"), family(facade.updated()));
    }

    private ExposedFacade fixture(Map<String, String> parameters) {
        ExposedFacade facade = new ExposedFacade();
        PlanningBookService books = mock(PlanningBookService.class);
        when(books.getPlanningBookDTO(any())).thenAnswer(ignored -> PlanningBookDTO.builder()
                .additionalParameters(parameters).build());
        ReflectionTestUtils.setField(facade, "planningBookService", books);
        ReflectionTestUtils.setField(facade, "keyFigureService", new KeyFigureService());
        return facade;
    }

    private Set<String> family(PlanningBookDTO book) {
        return Set.copyOf(Arrays.asList(book.additionalParameters
                .get("directDemandAdjustmentKeyFigures").split("\\|")));
    }

    private static class ExposedFacade extends DemandPlanningFacade {
        PlanningBookDTO read() {
            return montaPlanningBookDTOParaLeitura(mock(KeyFigureProjection.class),
                    new ConfiguredViewProjection(), new ConfiguredViewSelectionDTO());
        }

        PlanningBookDTO updated() {
            return montaPlanningBookDTOAposAtualizacao(mock(KeyFigureProjection.class),
                    new ConfiguredViewProjection(), List.of());
        }
    }
}
