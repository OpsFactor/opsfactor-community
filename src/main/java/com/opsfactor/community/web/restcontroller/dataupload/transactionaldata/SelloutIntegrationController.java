package com.opsfactor.community.web.restcontroller.dataupload.transactionaldata;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.dto.SelloutReportParametrosDTO;
import com.opsfactor.community.web.dto.template.AgGridDTO;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.transactionaldata.sales.saleshistory.facade.dto.FirstAndLastDateDTO;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.facade.SelloutFacade;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.service.SelloutIntegrationService;
import com.opsfactor.community.web.restcontroller.dataupload.IntegrationControllerAbstract;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints do historico de vendas permitido no OpsFactor Community.
 *
 * <p>O subpath canonico {@code sellout} representa apenas venda historica
 * observada em quantidade. Sell-in, sales orders, valores, precos e
 * campanhas/eventos permanecem no OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Transactional Data"),
        @Tag(name = "Sales")
})
@RestController
public class SelloutIntegrationController extends IntegrationControllerAbstract<Sellout, SelloutIntegrationDataDto, SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO, SelloutIntegrationFiltroDto, SelloutIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "sellout";
    }

    /**
     * Sell-out ja migrou para o namespace FILE canonico. A abstracao ainda
     * mantem aliases temporarios para outros controllers Community que nao
     * pertencem a este recorte, por isso este controller fixa sua propria raiz.
     */
    @Override
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    /**
     * Sell-out ja migrou para o namespace JSON canonico. A raiz local impede
     * que o registro dinamico publique o alias historico de data upload.
     */
    @Override
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

    /**
     * Service concreto usado pelos endpoints filtrados por periodo. Estes
     * endpoints continuam Community porque sell-out quantitativo e a
     * unica fonte transacional de vendas aberta nesta edicao.
     */
    @Autowired
    private SelloutIntegrationService selloutIntegrationService;

    /**
     * Service front usado apenas para metadados do historico sell-out, como
     * primeira/ultima data disponivel.
     */
    @Autowired
    private SelloutFacade selloutFrontService;

    /**
     * Exporta sell-out historico no formato AgGrid usado pelo consumidor
     * legado, reutilizando integralmente a fachada projection-driven
     * Community. Nao cria query, DTO de leitura ou regra de filtro nova.
     */
    @PostMapping("api/secured/historical/sellout")
    @Secured("ROLE_ADMIN")
    public AgGridDTO getHistoricalSellout(
            @RequestBody SelloutReportParametrosDTO selloutReportParametrosDTO) {

        return selloutFrontService.getSelloutParaExportacaoAgGrid(
                selloutReportParametrosDTO);

    }

    /**
     * Baixa arquivo de sell-out dentro de uma janela obrigatoria de datas.
     */
    @GetMapping("api/secured/data/file/sellout/{dataInicial}/{dataFinal}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getSelloutFile(@PathVariable("dataInicial") String dataInicialString,
                                         @PathVariable("dataFinal") String dataFinalString) {

        LocalDate dataInicial = LocalDate.parse(dataInicialString);
        LocalDate dataFinal = LocalDate.parse(dataFinalString);

        SelloutIntegrationFiltroDto selloutIntegrationFiltroDto = new SelloutIntegrationFiltroDto();
        selloutIntegrationFiltroDto.startDate = dataInicial;
        selloutIntegrationFiltroDto.endDate = dataFinal;

        return selloutIntegrationService.getFilteredFileContents(selloutIntegrationFiltroDto);

    }

    /**
     * Baixa JSON de sell-out dentro de uma janela obrigatoria de datas.
     */
    @GetMapping("api/secured/data/sellout/{dataInicial}/{dataFinal}")
    @Secured("ROLE_ADMIN")
    public List<SelloutIntegrationDataDto> getSelloutDTO(
            @PathVariable("dataInicial") String dataInicialString,
            @PathVariable("dataFinal") String dataFinalString) {

        LocalDate dataInicial = LocalDate.parse(dataInicialString);
        LocalDate dataFinal = LocalDate.parse(dataFinalString);

        SelloutIntegrationFiltroDto selloutIntegrationFiltroDto = new SelloutIntegrationFiltroDto();
        selloutIntegrationFiltroDto.startDate = dataInicial;
        selloutIntegrationFiltroDto.endDate = dataFinal;

        return selloutIntegrationService.getFilteredDTOList(selloutIntegrationFiltroDto);

    }

    /**
     * Remove sell-out somente dentro de uma janela obrigatoria de datas.
     */
    @DeleteMapping("api/secured/data/sellout/{dataInicial}/{dataFinal}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> removeSelloutDeRangeDatas(
            @PathVariable("dataInicial") String dataInicialString,
            @PathVariable("dataFinal") String dataFinalString) {

        LocalDate dataInicial = LocalDate.parse(dataInicialString);
        LocalDate dataFinal = LocalDate.parse(dataFinalString);

        return getWebControllerTaskSchedulingService().runImediatoSync(
                () -> selloutIntegrationService.removeSelloutDeRangeDatas(dataInicial, dataFinal),
                "DeleteSellout");

    }

    /**
     * Retorna a primeira e a ultima data de sell-out disponivel para a tela.
     */
    @GetMapping("api/secured/sellout/firstandlastdate")
    @Secured("ROLE_ADMIN")
    public FirstAndLastDateDTO getSelloutFirstAndLastDateDTO() {

        return selloutFrontService.getSelloutFirstAndLastDateDTO();

    }

}
