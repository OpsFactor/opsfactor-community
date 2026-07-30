package com.opsfactor.community.web.restcontroller.masterdata;

import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.ConversaoUnidadeMedidaDTO;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.UnidadeMedidaFacade;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.facade.dto.UnidadeConversaoFaltanteDTO;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.utility.Constantes.TamanhoBucket;
import com.opsfactor.community.platform.utility.MetodosUtilidade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API Community de unidades de medida e detalhamento de conversoes.
 *
 * <p>Conversao de UOM e infraestrutura operacional comum a Demand Planning,
 * Supply Planning heuristico e Planning Book. Ela nao habilita custos, precos,
 * P&L, cost-to-serve ou parametrizacao economica Enterprise.</p>
 */
@Slf4j
@RestController
public class UnidadeMedidaRestController {

    /**
     * Repository usado somente para listar ids de UOM existentes em seletores
     * operacionais simples.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Fachada de detalhamento de conversoes de UOM para telas operacionais do
     * Planning Book e cadastros Community.
     */
    @Autowired
    private UnidadeMedidaFacade unidadeConversaoFrontService;

    /**
     * Lista os ids de unidades de medida cadastradas no Community.
     */
    @GetMapping("/api/secured/unitofmeasure/findids")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<String>> getIdsUnidadesMedida() {

        try {
            return ResponseEntity.ok(unidadeMedidaRepository.findAll().stream()
                    .map(x -> x.getId())
                    .distinct()
                    .collect(Collectors.toList()));
        } catch (RuntimeException e) {
            log.error("Error listing Unit of Measure ids", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Lista lacunas de conversao de UOM que impedem a execucao do Supply
     * Planning Community para a referencia informada.
     *
     * <p>O caminho e um contrato legado de diagnostico. A conversao do periodo
     * e delegada ao calendario comum e toda a regra de verificacao permanece
     * em {@link UnidadeMedidaFacade}; este controller somente adapta os
     * parametros HTTP. Nao ha {@code @Secured} local porque o endpoint legado
     * era protegido apenas pela cadeia autenticada de {@code api/secured}.</p>
     */
    @GetMapping("api/secured/alerts/uomconversiongaps/snp/{referenceDate}/{bucketSize}/{supplyNetworkVersionId}/{snpExecutionProfileId}/{demandPlanVersionId}")
    public ResponseEntity<Set<UnidadeConversaoFaltanteDTO>> getGapsDeConversaoEntreUnidadesParaSNP(
            @PathVariable("referenceDate") String referenceDateAsString,
            @PathVariable("bucketSize") String bucketSizeAsString,
            @PathVariable("supplyNetworkVersionId") String supplyNetworkVersionId,
            @PathVariable("snpExecutionProfileId") String supplyPlanningExecutionProfileId,
            @PathVariable("demandPlanVersionId") Long demandPlanVersionId) {

        try {
            TamanhoBucket bucketSize = MetodosUtilidade.getValorEnumDeJsonProperty(
                    TamanhoBucket.class,
                    bucketSizeAsString);

            return ResponseEntity.ok(
                    unidadeConversaoFrontService.getUnidadeConversaoFaltanteSNPListDTO(
                            Calendario.getPrimeiraDataFromDescricaoPeriodo(
                                    referenceDateAsString,
                                    bucketSize),
                            bucketSize,
                            supplyNetworkVersionId,
                            supplyPlanningExecutionProfileId,
                            demandPlanVersionId));
        } catch (RuntimeException exception) {
            log.error("Error loading missing Unit of Measure conversions for Supply Planning", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }

    /**
     * Lista lacunas de conversão que impedem a execução de Demand Planning.
     *
     * <p>O serviço Community já recebe a referência no formato textual
     * legado e valida os dois argumentos no domínio. Esta borda apenas
     * preserva o endpoint histórico, sem converter, enriquecer ou filtrar o
     * resultado. Assim como o endpoint SNP, não há {@code @Secured} local:
     * a autorização original é a cadeia autenticada de {@code api/secured}.</p>
     */
    @GetMapping("api/secured/alerts/uomconversiongaps/dp/{demandPlanningExecutionProfileId}/{referenceDate}")
    public ResponseEntity<Set<UnidadeConversaoFaltanteDTO>> getGapsDeConversaoEntreUnidadesParaDP(
            @PathVariable("demandPlanningExecutionProfileId") String demandPlanningExecutionProfileId,
            @PathVariable("referenceDate") String referenceDate) {

        try {
            return ResponseEntity.ok(unidadeConversaoFrontService.getUnidadeConversaoFaltanteDPListDTO(
                    demandPlanningExecutionProfileId,
                    referenceDate));
        } catch (RuntimeException exception) {
            log.error("Error loading missing Unit of Measure conversions for Demand Planning", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }

    /**
     * Lista lacunas de conversao que impedem a execucao de Deployment do
     * Supply Plan informado.
     *
     * <p>O endpoint preserva o contrato legado e somente delega a regra ao
     * service. Assim como os demais alertas, nao ha {@code @Secured} local:
     * a cadeia autenticada de {@code api/secured} aplica a protecao comum.</p>
     */
    @GetMapping("api/secured/alerts/uomconversiongaps/deployment/{supplyPlanId}")
    public ResponseEntity<Set<UnidadeConversaoFaltanteDTO>> getGapsDeConversaoEntreUnidadesParaDeployment(
            @PathVariable("supplyPlanId") Long supplyPlanId) {

        try {
            return ResponseEntity.ok(
                    unidadeConversaoFrontService.getUnidadeConversaoFaltanteDeploymentListDTO(supplyPlanId));
        } catch (RuntimeException exception) {
            log.error("Error loading missing Unit of Measure conversions for Deployment", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }

    }

    /**
     * Detalha a conversao entre unidades no contexto de um material especifico.
     */
    @GetMapping("api/secured/unitofmeasure/conversiondetail/{materialId}/{originUomId}/{targetUomId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ConversaoUnidadeMedidaDTO> getDTODetalhamentoConversoesUnidade(
            @PathVariable("materialId") String materialId,
            @PathVariable("originUomId") String originUomId,
            @PathVariable("targetUomId") String targetUomId) {

        try {
            return ResponseEntity.ok(
                    unidadeConversaoFrontService.getDTODetalhamentoConversoesUnidade(materialId, originUomId, targetUomId));
        } catch (RuntimeException e) {
            log.error("Error loading material-specific Unit of Measure conversion detail", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

    /**
     * Detalha conversoes globais disponiveis entre duas unidades de medida.
     */
    @GetMapping("api/secured/unitofmeasure/conversiondetail/{originUomId}/{targetUomId}")
    @Secured("ROLE_ADMIN")
    public ResponseEntity<List<ConversaoUnidadeMedidaDTO>> getDTODetalhamentoConversoesUnidade(
            @PathVariable("originUomId") String originUomId,
            @PathVariable("targetUomId") String targetUomId) {

        try {
            return ResponseEntity.ok(
                    unidadeConversaoFrontService.getListaDTODetalhamentoConversoesUnidade(originUomId, targetUomId));
        } catch (RuntimeException e) {
            log.error("Error loading Unit of Measure conversion detail", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

    }

}
