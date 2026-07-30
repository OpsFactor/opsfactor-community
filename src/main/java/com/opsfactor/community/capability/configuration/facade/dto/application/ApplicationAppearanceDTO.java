package com.opsfactor.community.capability.configuration.facade.dto.application;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import lombok.Data;

/**
 * Contrato de transferencia da aparencia global da aplicacao.
 *
 * <p>O DTO impede que a borda HTTP exponha a entidade singleton de parametros
 * globais, que tambem contem configuracoes operacionais sem relacao com a
 * identidade visual.</p>
 */
@Data
public class ApplicationAppearanceDTO {

    /**
     * Imagem customizada do topo em formato data URL base64.
     */
    private String topbarLogoDataUrl;

    /**
     * Nome original do arquivo enviado pelo administrador.
     */
    private String topbarLogoFileName;

    /**
     * Tipo MIME validado no momento do salvamento.
     */
    private String topbarLogoContentType;

    /**
     * Indica se a SPA deve usar o logo customizado em vez do asset padrao.
     */
    private boolean customTopbarLogo;

    /**
     * Converte o registro singleton para o contrato estrito da aparencia.
     */
    public static ApplicationAppearanceDTO from(ParametrosGlobais parametrosGlobais) {

        if (parametrosGlobais == null) {
            throw new IllegalArgumentException("Global parameters are required.");
        }

        ApplicationAppearanceDTO applicationAppearanceDTO = new ApplicationAppearanceDTO();
        applicationAppearanceDTO.setTopbarLogoDataUrl(parametrosGlobais.getTopbarLogoDataUrl());
        applicationAppearanceDTO.setTopbarLogoFileName(parametrosGlobais.getTopbarLogoFileName());
        applicationAppearanceDTO.setTopbarLogoContentType(parametrosGlobais.getTopbarLogoContentType());
        applicationAppearanceDTO.setCustomTopbarLogo(
                parametrosGlobais.getTopbarLogoDataUrl() != null
                        && !parametrosGlobais.getTopbarLogoDataUrl().trim().isEmpty());
        return applicationAppearanceDTO;

    }

}
