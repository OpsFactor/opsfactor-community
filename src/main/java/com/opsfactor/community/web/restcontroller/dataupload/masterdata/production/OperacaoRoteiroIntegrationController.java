package com.opsfactor.community.web.restcontroller.dataupload.masterdata.production;

import com.opsfactor.community.web.dto.controller.ResponseDTO;
import com.opsfactor.community.capability.masterdata.production.operation.integration.service.OperacaoRoteiroIntegrationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Data upload Community das operacoes basicas de roteiro.
 *
 * <p>Este controller e manual porque a carga de operacoes de roteiro usa
 * arquivo processado, sem DTO JSON generico. A API
 * carrega somente recurso, sequencia, quantidade base, UOM e horas por
 * quantidade base, suficientes para o Supply Planning heuristico. Setup fino,
 * turnos, manutencao, custos e line scheduling ficam no OpsFactor Enterprise.</p>
 */
@Slf4j
@Tags({
        @Tag(name = "Master Data"),
        @Tag(name = "Production")
})
@RestController
public class OperacaoRoteiroIntegrationController {

    /**
     * Service de integracao das operacoes basicas de roteiro usadas pelo
     * Supply Planning heuristico. Setup detalhado, turnos e line scheduling
     * ficam fora deste bean Community.
     */
    @Autowired
    private OperacaoRoteiroIntegrationService operacaoRoteiroIntegrationService;

    /**
     * Executor web imediato usado para manter a carga Community sincronizada.
     */
    @Autowired
    private WebControllerTaskSchedulingService webControllerTaskSchedulingService;

    /**
     * Baixa o arquivo de operacoes basicas de roteiro pela raiz FILE canonica.
     */
    @GetMapping("api/secured/data/file/operationproductionrouting")
    @Secured("ROLE_ADMIN")
    public List<List<Object>> getOperacaoRoteiroFile() {

        return operacaoRoteiroIntegrationService.getFile();

    }

    /**
     * Importa o arquivo de operacoes basicas de roteiro de forma sincronizada.
     */
    @PostMapping("api/secured/data/file/operationproductionrouting")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseDTO> saveOperacaoRoteiroFile(@RequestParam("file") MultipartFile multipartFile) {

        return webControllerTaskSchedulingService.runImediatoSync(
                ThrowingSupplier.unchecked(() -> {
                    ProcessedFile processedFile = FileProcessing.getProcessedFileFromMultipartFile(multipartFile);
                    return operacaoRoteiroIntegrationService.saveFile(processedFile);
                }),
                "SaveRoutingOperationsFile");

    }

}
