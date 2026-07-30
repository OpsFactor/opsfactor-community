package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.configuration.facade.dto.application.ApplicationAppearanceDTO;
import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Set;

/**
 * Fachada de front para leitura e manutencao da identidade visual global.
 *
 * <p>O Community deliberadamente nao cria entidade, tabela ou repository
 * exclusivos para aparencia. A configuracao e parte do singleton
 * {@link ParametrosGlobais}, portanto cada gravacao segue o fluxo ja
 * estabelecido de {@link ParametrosGlobaisService}.</p>
 */
@Service
public class ApplicationAppearanceFacade {

    private static final int LOGO_MAX_BYTES = 1024 * 1024;
    private static final int FILE_NAME_MAX_LENGTH = 255;
    private static final String DATA_URL_BASE64_MARKER = ";base64,";
    private static final Set<String> SUPPORTED_LOGO_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif"
    );

    /**
     * Service do registro singleton que concentra leitura e persistencia dos
     * parametros globais Community.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Retorna somente os campos de aparencia permitidos para a SPA.
     */
    public ApplicationAppearanceDTO getApplicationAppearance() {

        return ApplicationAppearanceDTO.from(parametrosGlobaisService.getParametrosGlobais());

    }

    /**
     * Salva um logo de topo validado, ou remove o logo customizado quando o
     * data URL vier vazio.
     */
    public ApplicationAppearanceDTO saveTopbarLogo(
            String topbarLogoDataUrl,
            String topbarLogoFileName) {

        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();

        if (topbarLogoDataUrl == null || topbarLogoDataUrl.trim().isEmpty()) {
            clearTopbarLogo(parametrosGlobais);
            return ApplicationAppearanceDTO.from(
                    parametrosGlobaisService.saveParametrosGlobais(parametrosGlobais));
        }

        String normalizedTopbarLogoDataUrl = topbarLogoDataUrl.trim();
        String contentType = validateAndResolveContentType(normalizedTopbarLogoDataUrl);
        parametrosGlobais.setTopbarLogoDataUrl(normalizedTopbarLogoDataUrl);
        parametrosGlobais.setTopbarLogoFileName(normalizeFileName(topbarLogoFileName));
        parametrosGlobais.setTopbarLogoContentType(contentType);
        return ApplicationAppearanceDTO.from(
                parametrosGlobaisService.saveParametrosGlobais(parametrosGlobais));

    }

    /**
     * Remove explicitamente o logo customizado e restaura o asset padrao da
     * interface.
     */
    public ApplicationAppearanceDTO resetTopbarLogo() {

        return saveTopbarLogo(null, null);

    }

    /**
     * Limpa os tres campos que formam um logo customizado de forma atomica no
     * objeto gerenciado antes de delegar a persistencia do singleton.
     */
    private void clearTopbarLogo(ParametrosGlobais parametrosGlobais) {

        parametrosGlobais.setTopbarLogoDataUrl(null);
        parametrosGlobais.setTopbarLogoFileName(null);
        parametrosGlobais.setTopbarLogoContentType(null);

    }

    /**
     * Confere formato data URL, MIME permitido, integridade Base64 e tamanho
     * real do arquivo decodificado. O limite funcional e 1 MiB, menor que o
     * limite de armazenamento do campo LOB.
     */
    private String validateAndResolveContentType(String topbarLogoDataUrl) {

        int base64MarkerIndex = topbarLogoDataUrl.indexOf(DATA_URL_BASE64_MARKER);
        if (!topbarLogoDataUrl.startsWith("data:image/") || base64MarkerIndex <= "data:".length()) {
            throw new IllegalArgumentException("Logo must be sent as a base64 image data URL.");
        }

        String contentType = topbarLogoDataUrl
                .substring("data:".length(), base64MarkerIndex)
                .toLowerCase();
        if (!SUPPORTED_LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Logo type must be PNG, JPEG, WEBP, or GIF.");
        }

        String base64Payload = topbarLogoDataUrl.substring(
                base64MarkerIndex + DATA_URL_BASE64_MARKER.length());
        byte[] decodedPayload = Base64.getDecoder().decode(base64Payload);
        if (decodedPayload.length > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("Logo file must be 1 MB or smaller.");
        }

        return contentType;

    }

    /**
     * Mantem o nome apenas como metadado de exibicao, limitado ao tamanho
     * seguro para uma coluna String comum.
     */
    private String normalizeFileName(String topbarLogoFileName) {

        if (topbarLogoFileName == null) {
            return null;
        }

        String normalizedFileName = topbarLogoFileName.trim();
        if (normalizedFileName.isEmpty()) {
            return null;
        }

        if (normalizedFileName.length() <= FILE_NAME_MAX_LENGTH) {
            return normalizedFileName;
        }

        return normalizedFileName.substring(0, FILE_NAME_MAX_LENGTH);

    }

}
