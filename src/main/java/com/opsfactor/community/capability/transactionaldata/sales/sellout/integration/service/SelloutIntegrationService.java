package com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.service;

import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationDataDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.dto.SelloutIntegrationFiltroDto;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.mapper.SelloutIntegrationMapper;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.integration.mapper.SelloutIntegrationSupportData;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.domain.Sellout;
import com.opsfactor.community.capability.transactionaldata.sales.sellout.repository.SelloutRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.calendar.Calendario;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao do historico de vendas Community.
 *
 * <p>Apesar do nome historico {@code Sellout}, este service representa a unica
 * fonte de vendas transacionais permitida no Community. Ele persiste apenas
 * quantidade por documento/location/material/data/UOM, sem sell-in, sales
 * orders, valores, precos ou campanhas.</p>
 */
@Service
public class SelloutIntegrationService implements IntegrationServiceInterface<SelloutIntegrationDataDto, SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO, Sellout,SelloutIntegrationSupportData,SelloutIntegrationMapper, SelloutIntegrationFiltroDto> {

    /**
     * Fonte de locations validas para o historico sell-out. Usa o service para
     * respeitar o contrato local de exclusao da location default.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Fonte das unidades de medida aceitas no historico transacional.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository canonico de sell-out Community. Sell-in e sales orders possuem
     * repositories/cargas Enterprise separados e nao entram neste service.
     */
    @Autowired
    private SelloutRepository selloutRepository;

    /**
     * Fonte dos materiais aceitos pelo historico sell-out.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Mapper que valida e copia o payload de venda historica Community.
     */
    @Autowired
    private SelloutIntegrationMapper selloutIntegrationMapper;

    /**
     * Devolve o mapper usado pela infraestrutura generica de carga.
     */
    @Override
    public SelloutIntegrationMapper getMapper() {

        return selloutIntegrationMapper;

    }

    /**
     * Salva o batch de sell-out em uma unica chamada ao repository. O metodo
     * retorna lista vazia por contrato historico da infraestrutura de upload.
     */
    @Override
    @Transactional
    public List<Sellout> saveEntityList(Collection<Sellout> entityList) {

        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    selloutRepository.saveAll(entityList),
                    "Sell-out saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove em lote os registros selecionados pelo fluxo generico de
     * integracao.
     */
    @Override
    public void removeEntityList(Collection<Sellout> entityList) {

        if (!entityList.isEmpty()) {
            selloutRepository.deleteAll(entityList);
        }

    }

    /**
     * Mensagem publica mantida por compatibilidade com a API de upload.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Sell-out data saved";

    }

    /**
     * Materializa lookups de location, material e UOM uma vez por batch. Isso
     * evita que o mapper gere N+1 ao validar cada linha de venda historica.
     */
    @Override
    public SelloutIntegrationSupportData getSupportData() {
        
        SelloutIntegrationSupportData selloutIntegrationSupportData = new SelloutIntegrationSupportData();

        /*
         * Sell-out e a unica venda historica Community. Validar support data
         * aqui evita que snapshot quebrado de location/material/UOM seja
         * confundido com referencia inexistente de uma linha do arquivo.
         */
        selloutIntegrationSupportData.mapaLocationPorId = getMapaPorIdObrigatorio(
                locationService.findAllWithoutDefault(),
                location -> location.getId(),
                "Location snapshot");
        selloutIntegrationSupportData.mapaMaterialPorId = getMapaPorIdObrigatorio(
                produtoRepository.findAll(),
                material -> material.getId(),
                "Material snapshot");
        selloutIntegrationSupportData.mapaUnidadeMedidaPorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");
        
        return selloutIntegrationSupportData;
        
    }

    /**
     * Tamanho de lote historico para cargas de venda.
     */
    @Override
    public int getBatchSize() {

        return 5000;

    }

    /**
     * Busca documentos ja persistidos para reconciliar o batch atual. O id do
     * documento e a chave operacional de sell-out no Community.
     */
    @Override
    public Collection<Sellout> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO> dtoBatchList) {

        Collection<SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO> selloutPrimaryKeyDtoCollection =
                validaSelloutPrimaryKeyCollection(dtoBatchList);

        if (selloutPrimaryKeyDtoCollection.isEmpty()) {
            return List.of();
        }

        Set<String> selloutIds = selloutPrimaryKeyDtoCollection.stream()
                .map(x -> x.documentId)
                .collect(Collectors.toSet());

        return selloutRepository.customFindBySelloutIdIn(selloutIds);

    }

    /**
     * Retorna todos os sell-outs apenas para fluxos full-load explicitamente
     * solicitados. Extracoes operacionais devem preferir filtro por data.
     */
    @Override
    public Collection<Sellout> getAllPersistedEntities() {

        return selloutRepository.customFindAll();

    }

    /**
     * Extrai venda historica por janela obrigatoria de datas e, opcionalmente,
     * tipo de location destino. O filtro por data protege o Community contra
     * leituras amplas acidentais de historico transacional.
     */
    @Override
    public Collection<Sellout> getFilteredPersistedEntities(SelloutIntegrationFiltroDto filtroDto) {

        validaJanelaDatasSellout(filtroDto, "No Start/End dates were supplied");
        if (filtroDto.locationType == null || filtroDto.locationType.isEmpty()) {
            return selloutRepository.customFindByDataVendaBetween(
                    filtroDto.startDate.atStartOfDay(),
                    Calendario.getUltimoSegundoData(filtroDto.endDate));
        } else {
            return selloutRepository.customFindByDataVendaBetweenAndLocationDestinoTypeIn(
                    filtroDto.startDate.atStartOfDay(),
                    Calendario.getUltimoSegundoData(filtroDto.endDate),
                    filtroDto.locationType);
        }

    }

    /**
     * Remove sell-out em uma janela fechada de datas. A chamada direta permanece
     * para compatibilidade com fluxos existentes, mas exige periodo completo.
     */
    public String removeSelloutDeRangeDatas(LocalDate dataInicial, LocalDate dataFinal) {

        validaJanelaDatasSellout(dataInicial, dataFinal, "No start/end dates were provided");

        LocalDateTime inicio = dataInicial.atStartOfDay();
        LocalDateTime fim = dataFinal.plusDays(1).atStartOfDay().minusNanos(1);

        selloutRepository.deleteByDataVendaBetween(inicio, fim);

        return "Sellout data Removed from " + dataInicial + " to " + dataFinal;

    }

    /**
     * Remove historico sell-out somente por filtro validado. Sell-in e sales
     * orders possuem fluxos Enterprise proprios e nao sao afetados por este
     * metodo.
     */
    @Override
    public void removeFilteredPersistedEntities(SelloutIntegrationFiltroDto filtroDto) {

        validaJanelaDatasSellout(filtroDto, "No start/end dates were provided");
        removeSelloutDeRangeDatas(filtroDto.startDate, filtroDto.endDate);

    }

    /**
     * Garante que toda leitura ou exclusao temporal de sell-out descreva uma
     * janela fechada valida.
     *
     * <p>Sem esta verificacao, uma data final anterior a inicial seria aceita
     * pelo repository como consulta/remocao vazia, escondendo um erro de
     * integracao e quebrando a simetria entre exportacao e overwrite.</p>
     */
    private void validaJanelaDatasSellout(
            SelloutIntegrationFiltroDto filtroDto,
            String mensagemAusenciaPeriodo) {

        if (filtroDto == null) {
            throw new DataUploadException(mensagemAusenciaPeriodo);
        }
        validaJanelaDatasSellout(
                filtroDto.startDate,
                filtroDto.endDate,
                mensagemAusenciaPeriodo);

    }

    /**
     * Valida o intervalo sem depender do DTO para os endpoints diretos de
     * exclusao por data.
     */
    private void validaJanelaDatasSellout(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String mensagemAusenciaPeriodo) {

        if (dataInicial == null || dataFinal == null) {
            throw new DataUploadException(mensagemAusenciaPeriodo);
        }
        if (dataFinal.isBefore(dataInicial)) {
            throw new DataUploadException(
                    "End date must be after or equal to start date.");
        }

    }

    /**
     * Valida as chaves naturais recebidas antes de consultar o repository.
     *
     * <p>O lookup transforma os documentos em {@link Set} para consultar o
     * banco uma unica vez. Uma carga com o mesmo `documentId` duas vezes deve
     * falhar aqui, com indice do payload, em vez de ser deduplicada
     * silenciosamente pela estrutura auxiliar.</p>
     */
    private static Collection<SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO> validaSelloutPrimaryKeyCollection(
            Collection<SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO> selloutPrimaryKeyDtoCollection) {

        if (selloutPrimaryKeyDtoCollection == null) {
            throw new DataUploadException("Sell-out primary key collection is required.");
        }

        Set<String> documentIds = new HashSet<>();
        int indice = 0;
        for (SelloutIntegrationDataDto.SelloutPrimaryKeyIntegrationDTO selloutPrimaryKeyIntegrationDTO
                : selloutPrimaryKeyDtoCollection) {
            if (selloutPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Sell-out primary key collection item at index " + indice + " is required.");
            }
            if (selloutPrimaryKeyIntegrationDTO.documentId == null
                    || selloutPrimaryKeyIntegrationDTO.documentId.isBlank()) {
                throw new DataUploadException("Sell-out documentId is required.");
            }
            if (!documentIds.add(selloutPrimaryKeyIntegrationDTO.documentId)) {
                throw new DataUploadException(
                        "Sell-out primary key collection item at index "
                                + indice
                                + " has duplicated Sell-out documentId "
                                + selloutPrimaryKeyIntegrationDTO.documentId
                                + ".");
            }
            indice++;
        }

        return selloutPrimaryKeyDtoCollection;

    }

}
