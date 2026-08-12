package com.opsfactor.community.capability.supplyplanning.supplyplan.integration.service;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.supplyplanning.supplyplan.integration.dto.FulfilledDemandIntegrationDataDto;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.DemandaDiretaConsideradaLinhaRepository;
import com.opsfactor.community.capability.supplyplanning.supplyplan.repository.SupplyPlanRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.platform.utility.Constantes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Tests the read-only physical Fulfilled Demand contract. */
public class FulfilledDemandIntegrationServiceTest {

    @Test
    public void exportShouldUsePersistedDirectDemandLinesWithoutAUnitOfMeasureSelector() {

        LocalDateTime referenceDate = LocalDateTime.of(2027, 2, 28, 23, 59, 59);
        SupplyPlan supplyPlan = supplyPlan(2L);
        DemandaDiretaConsideradaLinha directDemandLine = directDemandLine(
                supplyPlan,
                referenceDate,
                120D,
                100D);
        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        DemandaDiretaConsideradaLinhaRepository directDemandLineRepository =
                Mockito.mock(DemandaDiretaConsideradaLinhaRepository.class);
        Mockito.when(supplyPlanRepository.findById(2L)).thenReturn(Optional.of(supplyPlan));
        Mockito.when(directDemandLineRepository.customFindAllBySupplyPlan(supplyPlan))
                .thenReturn(List.of(directDemandLine));
        FulfilledDemandIntegrationService service = getService(
                supplyPlanRepository,
                directDemandLineRepository);

        List<FulfilledDemandIntegrationDataDto> result = service.getFulfilledDemandDtoList(2L);

        Assertions.assertEquals(1, result.size());
        FulfilledDemandIntegrationDataDto fulfilledDemand = result.getFirst();
        Assertions.assertEquals(2L, fulfilledDemand.getSupplyPlanId());
        Assertions.assertEquals("PAPER_MILL", fulfilledDemand.getLocationId());
        Assertions.assertEquals("Paper Mill", fulfilledDemand.getLocationDescription());
        Assertions.assertEquals("FG_COPY_A4", fulfilledDemand.getMaterialId());
        Assertions.assertEquals("Copy Paper A4", fulfilledDemand.getMaterialDescription());
        Assertions.assertEquals(referenceDate, fulfilledDemand.getReferenceDate());
        Assertions.assertEquals("MT", fulfilledDemand.getUnitOfMeasureId());
        Assertions.assertEquals(120D, fulfilledDemand.getUnconstrainedDemand());
        Assertions.assertEquals(100D, fulfilledDemand.getFulfilledDemand());
        Assertions.assertEquals(20D, fulfilledDemand.getUnmetDemand());
        Assertions.assertEquals(100D / 120D, fulfilledDemand.getFulfillmentRate());

        List<List<Object>> file = service.getFile(2L);
        Assertions.assertEquals("Fulfilled Demand", file.getFirst().get(8));
        Assertions.assertEquals(100D, file.get(1).get(8));

        service.getFulfilledDemandDtoListByPeriod(2L, LocalDate.of(2027, 2, 1));
        Mockito.verify(directDemandLineRepository).customFindAllBySupplyPlanAndDataReferenciaBetween(
                Mockito.eq(supplyPlan),
                Mockito.any(LocalDateTime.class),
                Mockito.any(LocalDateTime.class));

    }

    @Test
    public void exportShouldRejectFulfilledQuantityAboveUnconstrainedDemand() {

        SupplyPlan supplyPlan = supplyPlan(2L);
        SupplyPlanRepository supplyPlanRepository = Mockito.mock(SupplyPlanRepository.class);
        DemandaDiretaConsideradaLinhaRepository directDemandLineRepository =
                Mockito.mock(DemandaDiretaConsideradaLinhaRepository.class);
        Mockito.when(supplyPlanRepository.findById(2L)).thenReturn(Optional.of(supplyPlan));
        DemandaDiretaConsideradaLinha directDemandLine = directDemandLine(
                supplyPlan,
                LocalDateTime.of(2027, 2, 28, 23, 59, 59),
                100D,
                120D);
        Mockito.when(directDemandLineRepository.customFindAllBySupplyPlan(supplyPlan))
                .thenReturn(List.of(directDemandLine));
        FulfilledDemandIntegrationService service = getService(
                supplyPlanRepository,
                directDemandLineRepository);

        DataUploadException error = Assertions.assertThrows(
                DataUploadException.class,
                () -> service.getFulfilledDemandDtoList(2L));

        Assertions.assertTrue(error.getMessage().contains("exceeds unconstrained demand"));

    }

    /** Creates the minimum persisted plan metadata required to resolve a calendar bucket. */
    private SupplyPlan supplyPlan(Long supplyPlanId) {

        SupplyPlan supplyPlan = new SupplyPlan();
        supplyPlan.setId(supplyPlanId);
        supplyPlan.setTamanhoBucket(Constantes.TamanhoBucket.MENSAL);
        return supplyPlan;

    }

    /** Mocks one joined repository row, preserving the line-level physical UOM. */
    private DemandaDiretaConsideradaLinha directDemandLine(
            SupplyPlan supplyPlan,
            LocalDateTime referenceDate,
            double unconstrainedDemand,
            double fulfilledDemand) {

        Location location = new Location("PAPER_MILL");
        location.setDescricao("Paper Mill");
        Produto material = new Produto("FG_COPY_A4");
        material.setDescricao("Copy Paper A4");
        UnidadeMedida unitOfMeasure = Mockito.mock(UnidadeMedida.class);
        Mockito.when(unitOfMeasure.getId()).thenReturn("MT");
        DemandaDiretaConsideradaLinha directDemandLine =
                Mockito.mock(DemandaDiretaConsideradaLinha.class);
        Mockito.when(directDemandLine.getSupplyPlan()).thenReturn(supplyPlan);
        Mockito.when(directDemandLine.getLocation()).thenReturn(location);
        Mockito.when(directDemandLine.getMaterial()).thenReturn(material);
        Mockito.when(directDemandLine.getDataReferencia()).thenReturn(referenceDate);
        Mockito.when(directDemandLine.getUnidadeMedidaCadastrado()).thenReturn(unitOfMeasure);
        Mockito.when(directDemandLine.getQuantidadeDemandaDiretaIrrestrita())
                .thenReturn(unconstrainedDemand);
        Mockito.when(directDemandLine.getQuantidadeDemandaDiretaRestrita())
                .thenReturn(fulfilledDemand);
        return directDemandLine;

    }

    /** Wires only the repositories used by the physical, batch-loaded export. */
    private FulfilledDemandIntegrationService getService(
            SupplyPlanRepository supplyPlanRepository,
            DemandaDiretaConsideradaLinhaRepository directDemandLineRepository) {

        FulfilledDemandIntegrationService service = new FulfilledDemandIntegrationService();
        ReflectionTestUtils.setField(service, "supplyPlanRepository", supplyPlanRepository);
        ReflectionTestUtils.setField(service, "directDemandLineRepository", directDemandLineRepository);
        return service;

    }

}
