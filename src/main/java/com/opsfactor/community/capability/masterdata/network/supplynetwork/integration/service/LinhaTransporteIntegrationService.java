package com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao da transportation lane origem/destino Community.
 *
 * <p>Persiste somente a malha operacional usada pelo heuristico. O mapper
 * bloqueia distancia e demais dados que alimentariam mapa, frete, baricentro ou
 * Supply Network Flows Enterprise.</p>
 */
@Service
public class LinhaTransporteIntegrationService implements IntegrationServiceInterface<LinhaTransporteIntegrationDataDto, LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte, LinhaTransporteIntegrationSupportData, LinhaTransporteIntegrationMapper, EmptyIntegrationDataFilter> {

    /**
     * Repository canonico da lane origem/destino. A carga Community trabalha em
     * lote para evitar `save` individual em cadastros grandes de malha.
     */
    @Autowired
    private LinhaTransporteRepository linhaTransporteRepository;

    /**
     * Fonte das versoes de malha aceitas pela carga. Versao de malha segue no
     * Community porque o heuristico precisa separar redes operacionais.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Fonte das unidades opcionais de lote minimo/multiplo de transporte. UOM
     * fisica e Community; custos/frete por unidade ficam fora deste contrato.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Fonte das locations origem e destino. Dados geograficos/mapa/visoes GIS
     * permanecem Enterprise, mas a chave operacional da location e Community.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper da linha de transporte Community. Ele centraliza validacao e copia
     * dos campos permitidos pela edicao aberta.
     */
    @Autowired
    private LinhaTransporteIntegrationMapper linhaTransporteIntegrationMapper;

    /**
     * Devolve o mapper usado pela infraestrutura generica de carga.
     */
    @Override
    public LinhaTransporteIntegrationMapper getMapper() {

        return linhaTransporteIntegrationMapper;

    }

    /**
     * Persiste o batch atual de transportation lanes em uma unica chamada ao
     * repository. Lista vazia representa batch sem efeito, nao erro de carga.
     */
    @Override
    public List<LinhaTransporte> saveEntityList(Collection<LinhaTransporte> entityList) {

        if (!entityList.isEmpty()) {
            /*
             * O retorno do saveAll alimenta o mapa de entidades persistidas do
             * batch. Validamos aqui para que uma anomalia do repository falhe
             * com contexto funcional de malha, antes de virar NPE generico na
             * infraestrutura de integracao.
             */
            return validaSavedEntityCollection(
                    linhaTransporteRepository.saveAll(entityList),
                    "Transportation Lane saved collection",
                    entityList.size());
        }
        return new ArrayList<>();

    }

    /**
     * Remove em lote as transportation lanes selecionadas pela infraestrutura
     * generica de integracao.
     */
    @Override
    public void removeEntityList(Collection<LinhaTransporte> entityList) {

        if (!entityList.isEmpty()) {
            linhaTransporteRepository.deleteInBatch(entityList);
        }

    }

    /**
     * Mensagem publica mantida por compatibilidade com a API de upload.
     */
    @Override
    public String getSaveSuccessMessage() {

        return "Transportation Lane data uploaded";

    }

    /**
     * Materializa o suporte de lookup do batch. As consultas sao feitas uma vez
     * por batch para que o mapper nao gere N+1 ao validar versao, locations e
     * UOM.
     */
    @Override
    public LinhaTransporteIntegrationSupportData getSupportData() {

        LinhaTransporteIntegrationSupportData linhaTransporteIntegrationSupportData =
                new LinhaTransporteIntegrationSupportData();

        /*
         * A mesma fotografia de locations alimenta origem e destino. Carregar
         * uma vez evita round-trip redundante e deixa a validacao de snapshot
         * unica para ambas as dimensoes.
         */
        List<Location> locations = locationRepository.findAll();

        linhaTransporteIntegrationSupportData.mapaVersaoMalhaPorId = getMapaPorIdObrigatorio(
                versaoMalhaRepository.findAll(),
                versaoMalha -> versaoMalha.getId(),
                "Supply Network Version snapshot");
        linhaTransporteIntegrationSupportData.mapaLocationOrigemPorId = getMapaPorIdObrigatorio(
                locations,
                location -> location.getId(),
                "Location snapshot");
        linhaTransporteIntegrationSupportData.mapaLocationDestinoPorId = getMapaPorIdObrigatorio(
                locations,
                location -> location.getId(),
                "Location snapshot");
        linhaTransporteIntegrationSupportData.mapaUomLoteMinimoMultiploTransportePorId = getMapaPorIdObrigatorio(
                unidadeMedidaRepository.findAll(),
                unidadeMedida -> unidadeMedida.getId(),
                "Unit of Measure snapshot");

        /*
         * A chave null representa a UOM vazia na interface de linhas de
         * transporte. Assim o mapper encontra explicitamente o valor null e
         * atualiza o banco sem tratar ausencia como erro de dependencia.
         */
        linhaTransporteIntegrationSupportData.mapaUomLoteMinimoMultiploTransportePorId.put(null, null);

        return linhaTransporteIntegrationSupportData;

    }

    /**
     * Tamanho de lote historico da carga de malha. Mantido estavel para evitar
     * mudanca operacional silenciosa em imports grandes.
     */
    @Override
    public int getBatchSize() {

        return 1000;

    }

    /**
     * Busca lanes ja persistidas para reconciliar upsert/delete do batch. A
     * consulta usa conjuntos de ids por dimensao para uma unica ida ao banco.
     */
    @Override
    public Collection<LinhaTransporte> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(
            Collection<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> dtoBatchList) {

        validaLinhaTransportePrimaryKeyCollection(dtoBatchList);

        if (dtoBatchList.isEmpty()) {
            return List.of();
        }

        Set<String> versaoMalhaIds = dtoBatchList.stream()
                .map(dto -> dto.supplyNetworkVersionId)
                .collect(Collectors.toSet());
        Set<String> locationOrigemIds = dtoBatchList.stream()
                .map(dto -> dto.originLocationId)
                .collect(Collectors.toSet());
        Set<String> locationDestinoIds = dtoBatchList.stream()
                .map(dto -> dto.destinationLocationId)
                .collect(Collectors.toSet());

        return linhaTransporteRepository.findByLinhaTransporteCompositeKeyVersaoIdInAndLinhaTransporteCompositeKeyLocationOrigemIdInAndLinhaTransporteCompositeKeyLocationDestinoIdIn(versaoMalhaIds, locationOrigemIds, locationDestinoIds);

    }

    /**
     * Valida a chave composta da transportation lane antes de reduzi-la para
     * conjuntos por dimensao.
     *
     * <p>A consulta usa ids de versao/origem/destino em `Set` apenas para
     * carregar o envelope persistido de forma eficiente. A chave funcional
     * completa continua sendo unica no payload e duplicidade deve falhar na
     * borda de integracao.</p>
     */
    private void validaLinhaTransportePrimaryKeyCollection(
            Collection<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> dtoBatchList) {

        if (dtoBatchList == null) {
            throw new DataUploadException("Transportation Lane primary key collection is required.");
        }

        Map<String, Map<String, Set<String>>> destinosPorVersaoEOrigem =
                new HashMap<>();
        int index = 0;
        for (LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO linhaTransportePrimaryKeyIntegrationDTO : dtoBatchList) {
            if (linhaTransportePrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Transportation Lane primary key collection item at index "
                                + index
                                + " is required.");
            }
            if (linhaTransportePrimaryKeyIntegrationDTO.supplyNetworkVersionId == null
                    || linhaTransportePrimaryKeyIntegrationDTO.supplyNetworkVersionId.isBlank()
                    || linhaTransportePrimaryKeyIntegrationDTO.originLocationId == null
                    || linhaTransportePrimaryKeyIntegrationDTO.originLocationId.isBlank()
                    || linhaTransportePrimaryKeyIntegrationDTO.destinationLocationId == null
                    || linhaTransportePrimaryKeyIntegrationDTO.destinationLocationId.isBlank()) {
                throw new DataUploadException(
                        "Transportation Lane upload primary key must include supply network version, origin location and destination location");
            }

            if (!destinosPorVersaoEOrigem
                    .computeIfAbsent(
                            linhaTransportePrimaryKeyIntegrationDTO.supplyNetworkVersionId,
                            ignored -> new HashMap<>())
                    .computeIfAbsent(
                            linhaTransportePrimaryKeyIntegrationDTO.originLocationId,
                            ignored -> new HashSet<>())
                    .add(linhaTransportePrimaryKeyIntegrationDTO.destinationLocationId)) {
                throw new DataUploadException(
                        "Transportation Lane primary key collection item at index "
                                + index
                                + " has duplicated key supplyNetworkVersionId "
                                + linhaTransportePrimaryKeyIntegrationDTO.supplyNetworkVersionId
                                + " / originLocationId "
                                + linhaTransportePrimaryKeyIntegrationDTO.originLocationId
                                + " / destinationLocationId "
                                + linhaTransportePrimaryKeyIntegrationDTO.destinationLocationId
                                + ".");
            }
            index++;
        }

    }

    /**
     * Retorna todas as lanes para operacoes full-load da infraestrutura de
     * integracao.
     */
    @Override
    public Collection<LinhaTransporte> getAllPersistedEntities() {

        return linhaTransporteRepository.findAll();

    }

}
