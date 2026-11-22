package com.opsfactor.community.web.restcontroller.configuration;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.facade.ParametrosGlobaisFacade;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.service.UnidadeMedidaService;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Exercita a fronteira controller–facade com flags Enterprise omitidas. */
class ParametrosGlobaisMinimalUpdateTest {

    @Test
    void minimalCommunityPayloadPersistsSupplyUnitWithoutEnablingEnterpriseAdjustments() throws Exception {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        ParametrosGlobaisService service = mock(ParametrosGlobaisService.class);
        when(service.getParametrosGlobais()).thenReturn(parametrosGlobais);
        when(service.saveParametrosGlobais(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UnidadeMedidaService unidadeMedidaService = mock(UnidadeMedidaService.class);
        when(unidadeMedidaService.getUnidadeMedida("TON")).thenReturn(new UnidadeMedida("TON"));
        ParametrosGlobaisController controller = controller(service, unidadeMedidaService);

        ParametrosGlobaisController.ParametrosGlobaisDTO dto =
                new ParametrosGlobaisController.ParametrosGlobaisDTO();
        dto.setUnidadeMedidaPadraoSNP("TON");
        ParametrosGlobaisController.ParametrosGlobaisDTO response = controller.updateParametrosGlobaisApi(dto);

        assertEquals("TON", response.getUnidadeMedidaPadraoSNP());
        assertFalse(parametrosGlobais.getPermiteAjusteAgregadoSemBaselineProduto());
        assertFalse(parametrosGlobais.getPermiteAjusteAgregadoSemBaselineLocation());
        assertFalse(parametrosGlobais.getRemessasConsomemDisponibilidadeNoPrimeiroPeriodo());
        assertFalse(parametrosGlobais.getQuantidadesEmPedidosRepresentamSaldoRestante());
        verify(service).saveParametrosGlobais(parametrosGlobais);

    }

    @Test
    void explicitEnterpriseAdjustmentStillFailsBeforeSaving() throws Exception {

        ParametrosGlobaisService service = mock(ParametrosGlobaisService.class);
        UnidadeMedidaService unidadeMedidaService = mock(UnidadeMedidaService.class);
        ParametrosGlobaisController controller = controller(service, unidadeMedidaService);

        ParametrosGlobaisController.ParametrosGlobaisDTO dto =
                new ParametrosGlobaisController.ParametrosGlobaisDTO();
        dto.setUnidadeMedidaPadraoSNP("TON");
        dto.setPermiteAjusteAgregadoSemBaselineProduto(true);

        assertThrows(RequiresEnterpriseVersionException.class, () -> controller.updateParametrosGlobaisApi(dto));
        verify(service, never()).saveParametrosGlobais(any());

        dto.setPermiteAjusteAgregadoSemBaselineProduto(null);
        dto.setPermiteAjusteAgregadoSemBaselineLocation(true);
        assertThrows(RequiresEnterpriseVersionException.class, () -> controller.updateParametrosGlobaisApi(dto));
        dto.setPermiteAjusteAgregadoSemBaselineLocation(null);
        dto.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(true);
        assertThrows(RequiresEnterpriseVersionException.class, () -> controller.updateParametrosGlobaisApi(dto));
        dto.setRemessasConsomemDisponibilidadeNoPrimeiroPeriodo(null);
        dto.setQuantidadesEmPedidosRepresentamSaldoRestante(true);
        assertThrows(RequiresEnterpriseVersionException.class, () -> controller.updateParametrosGlobaisApi(dto));
        verify(service, never()).saveParametrosGlobais(any());

    }

    /** Conecta os serviços reais de fronteira a dependências isoladas de persistência. */
    private static ParametrosGlobaisController controller(
            ParametrosGlobaisService service,
            UnidadeMedidaService unidadeMedidaService) throws ReflectiveOperationException {

        ParametrosGlobaisFacade facade = new ParametrosGlobaisFacade();
        inject(facade, "parametrosGlobaisService", service);
        ParametrosGlobaisController controller = new ParametrosGlobaisController();
        inject(controller, "parametrosGlobaisFrontService", facade);
        inject(controller, "unidadeMedidaService", unidadeMedidaService);
        inject(controller, "parametrosGlobaisControllerPolicy", new ParametrosGlobaisControllerPolicy());
        return controller;

    }

    private static void inject(Object target, String name, Object value) throws ReflectiveOperationException {

        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);

    }
}
