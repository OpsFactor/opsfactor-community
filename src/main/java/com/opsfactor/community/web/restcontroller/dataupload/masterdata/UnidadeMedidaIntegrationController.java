package com.opsfactor.community.web.restcontroller.dataupload.masterdata;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.dto.UnidadeMedidaDataUploadDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.integration.service.UnidadeMedidaIntegrationService;
import com.opsfactor.community.platform.utility.fileprocessing.FileProcessing;
import com.opsfactor.community.platform.utility.fileprocessing.ProcessedFile;
import com.opsfactor.community.platform.scheduler.facade.WebControllerTaskSchedulingService;
import com.pivovarit.function.ThrowingSupplier;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Data upload Community de unidades de medida.
 *
 * <p>Unidade de medida e conversao quantitativa sao infraestrutura operacional
 * compartilhada pelo Community. Este controller nao envolve preco, custo,
 * fiscal, P&L ou qualquer dado economico Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Unit of Measure")
})
@RestController
public class UnidadeMedidaIntegrationController {

    /**
     * Service de integracao de unidades de medida e conversoes globais
     * quantitativas. Este bean nao lida com custos, precos ou dados
     * financeiros Enterprise.
     */
    @Autowired
    private UnidadeMedidaIntegrationService unidadeMedidaIntegrationService;

    /**
     * Executor web imediato usado para manter a carga Community sincronizada.
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Baixa o arquivo de unidades de medida pelo contrato FILE canonico.
     */
    @GetMapping("api/secured/data/file/unitofmeasure")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getUnitOfMeasureFile() {

        return unidadeMedidaIntegrationService.getFile();

    }

    /**
     * Baixa unidades de medida pelo contrato JSON canonico.
     */
    @GetMapping("api/secured/data/unitofmeasure")
    @Secured("ROLE_ADMIN")
    public List<UnidadeMedidaDataUploadDTO> getUnitOfMeasureDTOList() {

        return unidadeMedidaIntegrationService.getDTOList();

    }

    /**
     * Importa unidades de medida a partir do contrato FILE canonico.
     */
    @PostMapping("api/secured/data/file/unitofmeasure")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> saveUnitOfMeasureFile(@RequestParam("file") MultipartFile multipartFile) {

        return webControllerTaskSchedulingService.runImediatoSync(
                ThrowingSupplier.unchecked(() -> {
                    ProcessedFile processedFile = FileProcessing.getProcessedFileFromMultipartFile(multipartFile);
                    return unidadeMedidaIntegrationService.saveFile(processedFile);
                }),
                "SaveUnitOfMeasureFile");

    }

    /**
     * Importa unidades de medida a partir do contrato JSON canonico.
     */
    @PostMapping("api/secured/data/unitofmeasure")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> saveUnitOfMeasureDTOList(
            @RequestBody List<UnidadeMedidaDataUploadDTO> unidadeMedidaDataUploadDTOList) {

        return webControllerTaskSchedulingService.runImediatoSync(
                () -> unidadeMedidaIntegrationService.saveDTOList(unidadeMedidaDataUploadDTOList),
                "SaveUnitOfMeasureJson");

    }

}
