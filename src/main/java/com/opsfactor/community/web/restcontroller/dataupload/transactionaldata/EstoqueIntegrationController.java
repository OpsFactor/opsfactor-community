package com.opsfactor.community.web.restcontroller.dataupload.transactionaldata;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.platform.integration.dto.IntegrationDto;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.dto.EstoqueIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.integration.service.EstoqueIntegrationService;
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
 * Endpoints de estoque inicial da edicao Community.
 *
 * <p>O subpath {@code stock} preserva a nomenclatura operacional do contrato
 * canonico. O payload representa apenas snapshot simples de estoque por
 * location/material/data; dados de lote/batch sao Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Transactional Data"),
        @Tag(name = "Stock")
})
@RestController
public class EstoqueIntegrationController extends IntegrationControllerAbstract<Estoque, EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, EstoqueIntegrationService, IntegrationOptionsDto> {

    @Override
    protected String getSubPath() {
        return "stock";
    }

    /**
     * Estoque ja migrou para o namespace FILE canonico. A abstracao ainda
     * mantem aliases temporarios para outros controllers Community que nao
     * pertencem a este recorte, por isso este controller fixa sua propria raiz.
     */
    @Override
    public List<String> getRootFilePaths() {

        return List.of("api/secured/data/file/");

    }

    /**
     * Estoque ja migrou para o namespace JSON canonico. A raiz local impede
     * que o registro dinamico publique o alias historico de data upload.
     */
    @Override
    public List<String> getRootJsonPaths() {

        return List.of("api/secured/data/");

    }

    /**
     * Service concreto usado pelos endpoints filtrados por periodo. No
     * Community, estoque e apenas snapshot inicial quantitativo.
     */
    @Autowired
    private EstoqueIntegrationService estoqueIntegrationService;

    /**
     * Baixa o arquivo de estoque dentro de uma janela obrigatoria de datas.
     * Esta borda nunca expõe download amplo sem periodo.
     */
    @GetMapping("api/secured/data/file/stock/{dataInicial}/{dataFinal}")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getEstoqueFile(@PathVariable("dataInicial") String dataInicialString,
                                         @PathVariable("dataFinal") String dataFinalString) {

        LocalDate dataInicial = LocalDate.parse(dataInicialString);
        LocalDate dataFinal = LocalDate.parse(dataFinalString);

        return estoqueIntegrationService.getFilteredFileContents(
                new EstoqueIntegrationFiltroDto(
                        dataInicial,
                        dataFinal,
                        null));

    }

    /**
     * Baixa o JSON de estoque dentro de uma janela obrigatoria de datas.
     */
    @GetMapping("api/secured/data/stock/{dataInicial}/{dataFinal}")
    @Secured("ROLE_ADMIN")
    public List<EstoqueIntegrationDataDto> getEstoqueDTO(
            @PathVariable("dataInicial") String dataInicialString,
            @PathVariable("dataFinal") String dataFinalString) {

        LocalDate dataInicial = LocalDate.parse(dataInicialString);
        LocalDate dataFinal = LocalDate.parse(dataFinalString);

        return estoqueIntegrationService.getFilteredDTOList(
                new EstoqueIntegrationFiltroDto(
                        dataInicial,
                        dataFinal,
                        null));

    }

    /**
     * Remove estoque somente dentro de uma janela obrigatoria de datas.
     */
    @DeleteMapping("api/secured/data/stock/{dataInicial}/{dataFinal}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> removeEstoqueDeRangeDatas(
            @PathVariable("dataInicial") String dataInicialString,
            @PathVariable("dataFinal") String dataFinalString) {

        LocalDate dataInicial = LocalDate.parse(dataInicialString);
        LocalDate dataFinal = LocalDate.parse(dataFinalString);

        IntegrationDto<EstoqueIntegrationDataDto, EstoqueIntegrationDataDto.EstoquePrimaryKeyIntegrationDTO, EstoqueIntegrationFiltroDto, IntegrationOptionsDto> integrationDto = new IntegrationDto<>();
        integrationDto.dataFilter = new EstoqueIntegrationFiltroDto(
                dataInicial,
                dataFinal,
                null);

        return deleteDtoOuFiltro(integrationDto);

    }

}
