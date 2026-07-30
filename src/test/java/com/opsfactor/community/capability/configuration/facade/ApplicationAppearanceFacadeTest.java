package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.facade.dto.application.ApplicationAppearanceDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contratos focais da fachada Community de identidade visual.
 */
public class ApplicationAppearanceFacadeTest {

    @Test
    public void serviceShouldUseGlobalParametersSingletonAndPersistValidatedLogo() throws Exception {

        CapturingParametrosGlobaisService parametrosGlobaisService =
                new CapturingParametrosGlobaisService(new ParametrosGlobais());
        ApplicationAppearanceFacade applicationAppearanceFrontService =
                createApplicationAppearanceFrontService(parametrosGlobaisService);

        ApplicationAppearanceDTO applicationAppearanceDTO =
                applicationAppearanceFrontService.saveTopbarLogo(
                        " data:image/PNG;base64,aGVsbG8= ",
                        "  planning-logo.png  ");

        Assertions.assertEquals("data:image/PNG;base64,aGVsbG8=", applicationAppearanceDTO.getTopbarLogoDataUrl());
        Assertions.assertEquals("planning-logo.png", applicationAppearanceDTO.getTopbarLogoFileName());
        Assertions.assertEquals("image/png", applicationAppearanceDTO.getTopbarLogoContentType());
        Assertions.assertTrue(applicationAppearanceDTO.isCustomTopbarLogo());
        Assertions.assertEquals(1, parametrosGlobaisService.saveCallCount.get());
        Assertions.assertSame(
                parametrosGlobaisService.parametrosGlobais,
                parametrosGlobaisService.savedParametrosGlobais,
                "A fachada deve atualizar o singleton existente, sem criar entidade paralela.");

        Field parametrosGlobaisServiceField =
                ApplicationAppearanceFacade.class.getDeclaredField("parametrosGlobaisService");
        Assertions.assertNotNull(parametrosGlobaisServiceField.getAnnotation(Autowired.class));

    }

    @Test
    public void resetTopbarLogoShouldClearEveryAppearanceField() {

        ParametrosGlobais parametrosGlobais = new ParametrosGlobais();
        parametrosGlobais.setTopbarLogoDataUrl("data:image/png;base64,aGVsbG8=");
        parametrosGlobais.setTopbarLogoFileName("old-logo.png");
        parametrosGlobais.setTopbarLogoContentType("image/png");
        CapturingParametrosGlobaisService parametrosGlobaisService =
                new CapturingParametrosGlobaisService(parametrosGlobais);
        ApplicationAppearanceFacade applicationAppearanceFrontService =
                createApplicationAppearanceFrontService(parametrosGlobaisService);

        ApplicationAppearanceDTO applicationAppearanceDTO =
                applicationAppearanceFrontService.resetTopbarLogo();

        Assertions.assertNull(applicationAppearanceDTO.getTopbarLogoDataUrl());
        Assertions.assertNull(applicationAppearanceDTO.getTopbarLogoFileName());
        Assertions.assertNull(applicationAppearanceDTO.getTopbarLogoContentType());
        Assertions.assertFalse(applicationAppearanceDTO.isCustomTopbarLogo());
        Assertions.assertEquals(1, parametrosGlobaisService.saveCallCount.get());

    }

    @Test
    public void saveTopbarLogoShouldRejectInvalidDataUrlsBeforePersistence() {

        assertInvalidLogo("data:image/svg+xml;base64,PHN2Zy8+", "Logo type must be PNG, JPEG, WEBP, or GIF.");
        assertInvalidLogo("data:image/png;base64,not-valid-base64%", "Illegal base64 character");
        assertInvalidLogo("https://example.invalid/logo.png", "Logo must be sent as a base64 image data URL.");

    }

    @Test
    public void saveTopbarLogoShouldRejectPayloadLargerThanOneMiBAndTruncateFileName() {

        CapturingParametrosGlobaisService parametrosGlobaisService =
                new CapturingParametrosGlobaisService(new ParametrosGlobais());
        ApplicationAppearanceFacade applicationAppearanceFrontService =
                createApplicationAppearanceFrontService(parametrosGlobaisService);
        String dataUrlOverOneMiB = "data:image/gif;base64," + Base64.getEncoder()
                .encodeToString(new byte[1024 * 1024 + 1]);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> applicationAppearanceFrontService.saveTopbarLogo(dataUrlOverOneMiB, "logo.gif"));

        Assertions.assertEquals("Logo file must be 1 MB or smaller.", illegalArgumentException.getMessage());
        Assertions.assertEquals(0, parametrosGlobaisService.saveCallCount.get());

        String fileNameOverLimit = "x".repeat(256);
        ApplicationAppearanceDTO applicationAppearanceDTO =
                applicationAppearanceFrontService.saveTopbarLogo(
                        "data:image/webp;base64,aGVsbG8=",
                        fileNameOverLimit);
        Assertions.assertEquals(255, applicationAppearanceDTO.getTopbarLogoFileName().length());

    }

    private static void assertInvalidLogo(String topbarLogoDataUrl, String expectedMessage) {

        CapturingParametrosGlobaisService parametrosGlobaisService =
                new CapturingParametrosGlobaisService(new ParametrosGlobais());
        ApplicationAppearanceFacade applicationAppearanceFrontService =
                createApplicationAppearanceFrontService(parametrosGlobaisService);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> applicationAppearanceFrontService.saveTopbarLogo(topbarLogoDataUrl, "logo"));

        Assertions.assertTrue(
                illegalArgumentException.getMessage().contains(expectedMessage),
                "Mensagem deveria preservar o motivo do payload invalido.");
        Assertions.assertEquals(0, parametrosGlobaisService.saveCallCount.get());

    }

    private static ApplicationAppearanceFacade createApplicationAppearanceFrontService(
            ParametrosGlobaisService parametrosGlobaisService) {

        ApplicationAppearanceFacade applicationAppearanceFrontService =
                new ApplicationAppearanceFacade();
        try {
            Field parametrosGlobaisServiceField = ApplicationAppearanceFacade.class
                    .getDeclaredField("parametrosGlobaisService");
            parametrosGlobaisServiceField.setAccessible(true);
            parametrosGlobaisServiceField.set(applicationAppearanceFrontService, parametrosGlobaisService);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not inject global parameters service for test.", exception);
        }

        return applicationAppearanceFrontService;

    }

    private static class CapturingParametrosGlobaisService extends ParametrosGlobaisService {

        private final AtomicInteger saveCallCount = new AtomicInteger();
        private final ParametrosGlobais parametrosGlobais;
        private ParametrosGlobais savedParametrosGlobais;

        private CapturingParametrosGlobaisService(ParametrosGlobais parametrosGlobais) {

            this.parametrosGlobais = parametrosGlobais;

        }

        @Override
        public ParametrosGlobais getParametrosGlobais() {

            return parametrosGlobais;

        }

        @Override
        public ParametrosGlobais saveParametrosGlobais(ParametrosGlobais parametrosGlobais) {

            saveCallCount.incrementAndGet();
            savedParametrosGlobais = parametrosGlobais;
            return parametrosGlobais;

        }

    }

}
