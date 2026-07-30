package com.opsfactor.community.capability.masterdata.network.supplynetwork.facade;

import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.dto.LinhaTransporteProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.mapper.LinhaTransporteProdutoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporte;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.LinhaTransporteProduto;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.domain.VersaoMalha;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteProdutoRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.LinhaTransporteRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.repository.VersaoMalhaRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.dto.VersaoMalhaDTO;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.LinhaTransporteIntegrationService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.integration.service.LinhaTransporteProdutoIntegrationService;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.facade.mapper.VersaoMalhaAutoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service front da malha simples Community.
 *
 * <p>Esta borda expõe apenas versao de malha, transportation lanes
 * origem/destino e transportation lanes por material usadas pelo Supply
 * Planning heuristico. Supply Network Flows, mapa, baricentro, frota, last mile
 * e custos logisticos pertencem ao Enterprise e nao devem aparecer aqui.</p>
 */
@Service
public class LinhaTransporteFacade {

    /**
     * Repository das lanes origem/destino operacionais.
     */
    @Autowired
    private LinhaTransporteRepository linhaTransporteRepository;

    /**
     * Repository das lanes material-especificas.
     */
    @Autowired
    private LinhaTransporteProdutoRepository linhaTransporteProdutoRepository;

    /**
     * Repository das versoes de malha selecionaveis no Community.
     */
    @Autowired
    private VersaoMalhaRepository versaoMalhaRepository;

    /**
     * Repository da origem padrao de materia-prima configurada na versao de
     * malha Community.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Mapper usado para manter a borda front alinhada ao mesmo contrato de
     * upload da transportation lane.
     */
    @Autowired
    private LinhaTransporteIntegrationMapper linhaTransporteIntegrationMapper;

    /**
     * Service de integracao reutilizado para obter support data e mapa de
     * entidades persistidas sem duplicar regra de conversao.
     */
    @Autowired
    private LinhaTransporteIntegrationService linhaTransporteIntegrationService;

    /**
     * Mapper usado para o contrato de lane/material.
     */
    @Autowired
    private LinhaTransporteProdutoIntegrationMapper linhaTransporteProdutoIntegrationMapper;

    /**
     * Service de integracao de lane/material reutilizado pela borda front.
     */
    @Autowired
    private LinhaTransporteProdutoIntegrationService linhaTransporteProdutoIntegrationService;

    /**
     * Mapper de versao de malha para DTOs da tela.
     */
    @Autowired
    private VersaoMalhaAutoMapper versaoMalhaAutoMapper;

    /**
     * Lista versoes de malha disponiveis para seletores de supply network.
     */
    public List<VersaoMalhaDTO> getVersaoMalhaDTOList() {

        /*
         * O DTO acessa o id da origem padrao. A consulta dedicada ja traz as
         * duas relacoes de origem, evitando um select lazy por versao de
         * malha durante a serializacao da lista administrativa.
         */
        List<VersaoMalha> versaoMalhaList = versaoMalhaRepository.customFindAll();
        validaVersaoMalhaListCarregadaCommunity(versaoMalhaList);

        List<VersaoMalhaDTO> versaoMalhaDTOList =
                versaoMalhaAutoMapper.converteListaEntidadesParaDTOs(versaoMalhaList);
        validaVersaoMalhaDTOListCarregadaCommunity(versaoMalhaDTOList);

        return versaoMalhaDTOList;

    }

    /**
     * Salva uma versao de malha simples. Nenhuma configuracao Enterprise de
     * otimizacao de rede e derivada deste cadastro.
     */
    public void saveVersaoMalhaDTO(VersaoMalhaDTO versaoMalhaDTO) {

        validaVersaoMalhaDTOCommunity(versaoMalhaDTO);

        VersaoMalha versaoMalha = getVersaoMalhaParaSaveCommunity(versaoMalhaDTO.getId());
        versaoMalha.setDescricao(versaoMalhaDTO.getDescription());
        versaoMalha.setLeadTimeDiasLocationOrigemPadraoMateriasPrimas(
                getLeadTimeDiasOrigemPadraoMateriaPrimaValidadoCommunity(versaoMalhaDTO));
        versaoMalha.setLocationOrigemPadraoMateriasPrimas(
                getLocationOrigemPadraoMateriaPrimaCommunity(versaoMalhaDTO));

        /*
         * A tela trata o save como sucesso de cadastro da versao de malha.
         * Validar a fotografia devolvida pelo repository evita sucesso
         * silencioso quando a persistencia retorna nulo ou sem chave.
         */
        VersaoMalha versaoMalhaSalva = versaoMalhaRepository.save(versaoMalha);
        validaVersaoMalhaSalvaCommunity(
                versaoMalhaSalva,
                versaoMalhaDTO.getId());

    }

    /**
     * Remove uma versao de malha nao default. A versao `Default` e protegida
     * porque pode ser referencia operacional minima de ambientes Community.
     */
    public void removeVersaoMalhaDTO(VersaoMalhaDTO versaoMalhaDTO) {

        validaVersaoMalhaDTOCommunity(versaoMalhaDTO);
        if (versaoMalhaDTO.getId().equals("Default")) {
            throw new IllegalArgumentException("Default Supply Network Version cannot be removed");
        }
        versaoMalhaRepository.delete(versaoMalhaAutoMapper.converte(versaoMalhaDTO));

    }

    /**
     * Valida a chave da versao de malha antes de mapper/repository.
     */
    private void validaVersaoMalhaDTOCommunity(VersaoMalhaDTO versaoMalhaDTO) {

        if (versaoMalhaDTO == null) {
            throw new IllegalArgumentException("Supply Network Version payload is required.");
        }
        if (isBlank(versaoMalhaDTO.getId())) {
            throw new IllegalArgumentException("Supply Network Version id is required.");
        }

    }

    /**
     * Carrega a versao existente para preservar os atributos fora do contrato
     * Community; em uma criacao, monta somente o agregado minimo pelo id.
     */
    private VersaoMalha getVersaoMalhaParaSaveCommunity(String versaoMalhaId) {

        Optional<VersaoMalha> versaoMalhaOptional = versaoMalhaRepository.findById(versaoMalhaId);
        if (versaoMalhaOptional == null) {
            throw new IllegalStateException(
                    "Supply Network Version repository returned null Optional for front save id "
                            + versaoMalhaId
                            + ".");
        }

        VersaoMalha versaoMalha = versaoMalhaOptional.orElse(new VersaoMalha(versaoMalhaId));
        if (isBlank(versaoMalha.getId()) || !versaoMalhaId.equals(versaoMalha.getId())) {
            throw new IllegalStateException(
                    "Loaded Supply Network Version id must match front save id "
                            + versaoMalhaId
                            + ".");
        }

        return versaoMalha;

    }

    /**
     * Reaplica na borda administrativa a validacao que protege a criacao de
     * linhas temporarias na SupplyNetworkProjectionFactory.
     */
    private Double getLeadTimeDiasOrigemPadraoMateriaPrimaValidadoCommunity(
            VersaoMalhaDTO versaoMalhaDTO) {

        Double leadTimeDays = versaoMalhaDTO.getDefaultRawMaterialOriginLeadTimeDays();
        if (leadTimeDays != null && (!Double.isFinite(leadTimeDays) || leadTimeDays < 0.0d)) {
            throw new IllegalArgumentException(
                    "Default Raw Material Origin lead time days must be finite and non-negative.");
        }

        return leadTimeDays;

    }

    /**
     * Resolve uma unica location por payload. Id nulo representa limpeza
     * explicita do default, sem executar consulta desnecessaria.
     */
    private Location getLocationOrigemPadraoMateriaPrimaCommunity(
            VersaoMalhaDTO versaoMalhaDTO) {

        String locationId = versaoMalhaDTO.getDefaultRawMaterialOriginLocationId();
        if (locationId == null) {
            return null;
        }
        if (locationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Default Raw Material Origin Location id must not be blank.");
        }

        Optional<Location> locationOptional = locationRepository.findById(locationId);
        if (locationOptional == null) {
            throw new IllegalStateException(
                    "Location repository returned null Optional for Default Raw Material Origin Location id "
                            + locationId
                            + ".");
        }

        Location location = locationOptional.orElseThrow(() -> new IllegalArgumentException(
                "Default Raw Material Origin Location " + locationId + " not found."));
        if (isBlank(location.getId()) || !locationId.equals(location.getId())) {
            throw new IllegalStateException(
                    "Loaded Default Raw Material Origin Location id must match requested location.");
        }

        return location;

    }

    /**
     * Valida o snapshot de versoes de malha antes do mapper de listagem.
     *
     * <p>Lista vazia e valida para bases novas. Cada versao carregada precisa
     * ter id, pois lanes origem/destino e lanes por material usam esse
     * cabecalho como parte da chave funcional do Supply Network Community.</p>
     */
    private void validaVersaoMalhaListCarregadaCommunity(List<VersaoMalha> versaoMalhaList) {

        if (versaoMalhaList == null) {
            throw new IllegalStateException("Supply Network Version list snapshot is required.");
        }

        for (int index = 0; index < versaoMalhaList.size(); index++) {
            VersaoMalha versaoMalha = versaoMalhaList.get(index);

            if (versaoMalha == null) {
                throw new IllegalStateException(
                        "Supply Network Version at index " + index + " is required in list snapshot.");
            }
            if (isBlank(versaoMalha.getId())) {
                throw new IllegalStateException(
                        "Supply Network Version at index " + index + " has no id in list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de versoes de malha devolvida pelo mapper.
     *
     * <p>O seletor da SPA usa somente este DTO depois da listagem. Mesmo com a
     * entidade validada, um mapper quebrado nao pode publicar versao sem id,
     * pois lanes e perfis Supply dependem dessa chave para navegar a malha
     * Community.</p>
     */
    private void validaVersaoMalhaDTOListCarregadaCommunity(
            List<VersaoMalhaDTO> versaoMalhaDTOList) {

        if (versaoMalhaDTOList == null) {
            throw new IllegalStateException("Supply Network Version DTO list snapshot is required.");
        }

        for (int index = 0; index < versaoMalhaDTOList.size(); index++) {
            VersaoMalhaDTO versaoMalhaDTO = versaoMalhaDTOList.get(index);

            if (versaoMalhaDTO == null) {
                throw new IllegalStateException(
                        "Supply Network Version DTO at index " + index + " is required in list snapshot.");
            }
            if (isBlank(versaoMalhaDTO.getId())) {
                throw new IllegalStateException(
                        "Supply Network Version DTO at index " + index + " has no id in list snapshot.");
            }
        }

    }

    /**
     * Valida o snapshot salvo da versao de malha.
     *
     * <p>Versionamento de malha e o cabeçalho minimo do Supply Network
     * Community. Sem id salvo, lanes origem/destino e lanes por material nao
     * conseguem formar chave funcional para o heuristico.</p>
     */
    private void validaVersaoMalhaSalvaCommunity(
            VersaoMalha versaoMalhaSalva,
            String versaoMalhaIdEsperada) {

        if (versaoMalhaSalva == null) {
            throw new IllegalStateException("Saved Supply Network Version snapshot is required.");
        }
        if (isBlank(versaoMalhaSalva.getId())) {
            throw new IllegalStateException("Saved Supply Network Version id is required.");
        }
        if (!versaoMalhaIdEsperada.equals(versaoMalhaSalva.getId())) {
            throw new IllegalStateException("Saved Supply Network Version id must match requested id.");
        }

    }

    /**
     * Carrega a versao de malha usada pelas listagens front de transportation
     * lanes Community.
     *
     * <p>Versao ausente continua `NoResultException`, preservando a semantica
     * historica da tela. Retorno nulo do repository no lugar de `Optional` e
     * erro estrutural e deve falhar antes de consultar lanes origem/destino ou
     * lanes material-especificas.</p>
     */
    private VersaoMalha getVersaoMalhaObrigatoriaTransportationLaneFrontCommunity(
            String versaoMalhaId) {

        Optional<VersaoMalha> versaoMalhaOptional = versaoMalhaRepository.findById(versaoMalhaId);

        if (versaoMalhaOptional == null) {
            throw new IllegalStateException(
                    "Supply Network Version repository returned null Optional for transportation lane front id "
                            + versaoMalhaId
                            + ".");
        }

        VersaoMalha versaoMalha = versaoMalhaOptional
                .orElseThrow(() -> new NoResultException("Supply Network Id " + versaoMalhaId + " not found"));
        if (isBlank(versaoMalha.getId())) {
            throw new IllegalStateException(
                    "Supply Network Version snapshot id is required for transportation lane front id "
                            + versaoMalhaId
                            + ".");
        }
        if (!versaoMalhaId.equals(versaoMalha.getId())) {
            throw new IllegalStateException(
                    "Supply Network Version snapshot id must match transportation lane front id "
                            + versaoMalhaId
                            + ".");
        }

        return versaoMalha;

    }

    /**
     * Retorna as lanes origem/destino de uma versao de malha.
     */
    public List<LinhaTransporteIntegrationDataDto> getLinhaTransporteIntegrationDataDtoList(String versaoMalhaId) {

        validaVersaoMalhaIdCommunity(versaoMalhaId);

        VersaoMalha versaoMalha =
                getVersaoMalhaObrigatoriaTransportationLaneFrontCommunity(versaoMalhaId);

        /*
         * A listagem e a borda que alimenta a SPA. Validamos a fotografia
         * carregada antes do mapper para evitar que uma consulta quebrada seja
         * mascarada por conversao parcial de DTO.
         */
        List<LinhaTransporte> linhaTransporteList =
                linhaTransporteRepository.customFindForFrontByVersaoMalha(versaoMalha);
        validaLinhaTransporteListCarregadaCommunity(
                linhaTransporteList,
                versaoMalhaId);

        List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDataDtoList =
                linhaTransporteIntegrationMapper.convertEntityCollectionToDTOList(linhaTransporteList);
        validaLinhaTransporteIntegrationDataDtoListCarregadaCommunity(
                linhaTransporteIntegrationDataDtoList,
                versaoMalhaId);

        return linhaTransporteIntegrationDataDtoList;

    }

    /**
     * Salva uma lane origem/destino a partir do mesmo DTO de upload, garantindo
     * que tela e carga em massa compartilhem validacoes.
     */
    public void saveLinhaTransporteIntegrationDataDto(LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDTO) {

        validaLinhaTransporteIntegrationDataDtoCommunity(linhaTransporteIntegrationDTO);

        List<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> dtoBatchList =
                List.of(linhaTransporteIntegrationDTO.primaryKeyDto);

        LinhaTransporte linhaTransporteSalva = linhaTransporteRepository.save(linhaTransporteIntegrationMapper.convertDTOToEntity(
                linhaTransporteIntegrationDTO, 
                linhaTransporteIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(dtoBatchList),
                linhaTransporteIntegrationService.getSupportData(),
                null));
        validaLinhaTransporteSalvaCommunity(linhaTransporteSalva);

    }

    /**
     * Remove lanes origem/destino selecionadas na tela. O mapa de entidades
     * persistidas e montado uma unica vez para o lote, evitando repeticao de
     * consulta/conversao dentro do loop.
     */
    public void removeLinhaTransporteIntegrationDataDtoList(List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDTOList) {

        validaLinhaTransporteIntegrationDataDtoListCommunity(linhaTransporteIntegrationDTOList);

        List<LinhaTransporte> entitiesToDelete = new ArrayList<>();
        
        LinhaTransporteIntegrationSupportData supportData = linhaTransporteIntegrationService.getSupportData();

        List<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO> primaryKeyDtoList = linhaTransporteIntegrationDTOList
                .stream()
                .map(linhaTransporteIntegrationDTO -> linhaTransporteIntegrationDTO.primaryKeyDto)
                .collect(Collectors.toList());

        Map<LinhaTransporteIntegrationDataDto.LinhaTransportePrimaryKeyIntegrationDTO, LinhaTransporte> persistedEntitiesByPrimaryKey =
                linhaTransporteIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(primaryKeyDtoList);

        for (LinhaTransporteIntegrationDataDto dto : linhaTransporteIntegrationDTOList) {
            entitiesToDelete.add(linhaTransporteIntegrationMapper.convertDTOToEntity(
                    dto, 
                    persistedEntitiesByPrimaryKey,
                    supportData,
                    null));
        }
        
        validaLinhaTransporteEntityListParaDeleteCommunity(entitiesToDelete);
        linhaTransporteRepository.deleteAll(entitiesToDelete);

    }

    /**
     * Retorna as lanes material-especificas de uma versao de malha.
     */
    public List<LinhaTransporteProdutoIntegrationDataDto> getLinhaTransporteProdutoIntegrationDataDtoList(String versaoMalhaId) {

        validaVersaoMalhaIdCommunity(versaoMalhaId);

        VersaoMalha versaoMalha =
                getVersaoMalhaObrigatoriaTransportationLaneFrontCommunity(versaoMalhaId);

        /*
         * Lane/material e uma fotografia dependente da lane base. A validacao
         * explicita evita que produto sem chave ou lane de outra versao de
         * malha passe para o mapper e gere uma tela aparentemente consistente.
         */
        List<LinhaTransporteProduto> linhaTransporteProdutoList =
                linhaTransporteProdutoRepository.customFindForFrontByVersaoMalha(versaoMalha);
        validaLinhaTransporteProdutoListCarregadaCommunity(
                linhaTransporteProdutoList,
                versaoMalhaId);

        List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDataDtoList =
                linhaTransporteProdutoIntegrationMapper.convertEntityCollectionToDTOList(linhaTransporteProdutoList);
        validaLinhaTransporteProdutoIntegrationDataDtoListCarregadaCommunity(
                linhaTransporteProdutoIntegrationDataDtoList,
                versaoMalhaId);

        return linhaTransporteProdutoIntegrationDataDtoList;

    }

    /**
     * Salva uma lane/material usando o mesmo DTO de upload.
     */
    public void saveLinhaTransporteProdutoIntegrationDataDto(
            LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDTO) {

        validaLinhaTransporteProdutoIntegrationDataDtoCommunity(linhaTransporteProdutoIntegrationDTO);

        List<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO> dtoBatchList =
                List.of(linhaTransporteProdutoIntegrationDTO.primaryKeyDto);
        
        LinhaTransporteProduto linhaTransporteProdutoSalva = linhaTransporteProdutoRepository.save(linhaTransporteProdutoIntegrationMapper.convertDTOToEntity(
                linhaTransporteProdutoIntegrationDTO, 
                linhaTransporteProdutoIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(dtoBatchList),
                linhaTransporteProdutoIntegrationService.getSupportData(),
                null));
        validaLinhaTransporteProdutoSalvaCommunity(linhaTransporteProdutoSalva);

    }

    /**
     * Remove lanes/material selecionadas na tela. Assim como na lane base, o
     * mapa de persistidos e calculado uma vez para o lote.
     */
    public void removeLinhaTransporteProdutoIntegrationDataDtoList(List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDTOList) {

        validaLinhaTransporteProdutoIntegrationDataDtoListCommunity(linhaTransporteProdutoIntegrationDTOList);

        List<LinhaTransporteProduto> entitiesToDelete = new ArrayList<>();

        LinhaTransporteProdutoIntegrationSupportData supportData = linhaTransporteProdutoIntegrationService.getSupportData();

        List<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO> primaryKeyDtoList = linhaTransporteProdutoIntegrationDTOList
                .stream()
                .map(linhaTransporteProdutoIntegrationDTO -> linhaTransporteProdutoIntegrationDTO.primaryKeyDto)
                .collect(Collectors.toList());

        Map<LinhaTransporteProdutoIntegrationDataDto.LinhaTransporteProdutoPrimaryKeyIntegrationDTO, LinhaTransporteProduto> persistedEntitiesByPrimaryKey =
                linhaTransporteProdutoIntegrationService.getPersistedEntityMapFromPrimaryKeyDtoCollection(primaryKeyDtoList);

        for (LinhaTransporteProdutoIntegrationDataDto dto : linhaTransporteProdutoIntegrationDTOList) {
            entitiesToDelete.add(linhaTransporteProdutoIntegrationMapper.convertDTOToEntity(
                    dto,
                    persistedEntitiesByPrimaryKey,
                    supportData,
                    null));
        }
        
        validaLinhaTransporteProdutoEntityListParaDeleteCommunity(entitiesToDelete);
        linhaTransporteProdutoRepository.deleteAll(entitiesToDelete);

    }

    /**
     * Valida versao de malha usada em consultas da malha simples.
     */
    private void validaVersaoMalhaIdCommunity(String versaoMalhaId) {

        if (isBlank(versaoMalhaId)) {
            throw new IllegalArgumentException("Supply Network Version id is required.");
        }

    }

    /**
     * Valida chave origem/destino da transportation lane.
     *
     * <p>Campos economicos/geograficos Enterprise continuam sob
     * responsabilidade do mapper/servico de integracao. Esta guarda cobre
     * apenas a forma minima necessaria para montar support data e entity map.</p>
     */
    private void validaLinhaTransporteIntegrationDataDtoCommunity(
            LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDTO) {

        if (linhaTransporteIntegrationDTO == null) {
            throw new IllegalArgumentException("Transportation Line payload is required.");
        }
        if (linhaTransporteIntegrationDTO.primaryKeyDto == null) {
            throw new IllegalArgumentException("Transportation Line primary key is required.");
        }
        if (isBlank(linhaTransporteIntegrationDTO.primaryKeyDto.supplyNetworkVersionId)) {
            throw new IllegalArgumentException("Transportation Line supply network version id is required.");
        }
        if (isBlank(linhaTransporteIntegrationDTO.primaryKeyDto.originLocationId)) {
            throw new IllegalArgumentException("Transportation Line origin location id is required.");
        }
        if (isBlank(linhaTransporteIntegrationDTO.primaryKeyDto.destinationLocationId)) {
            throw new IllegalArgumentException("Transportation Line destination location id is required.");
        }

    }

    /**
     * Valida lote de remocao de transportation lanes.
     */
    private void validaLinhaTransporteIntegrationDataDtoListCommunity(
            List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDTOList) {

        if (linhaTransporteIntegrationDTOList == null || linhaTransporteIntegrationDTOList.isEmpty()) {
            throw new IllegalArgumentException("Transportation Line payload list is required.");
        }
        for (LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDTO : linhaTransporteIntegrationDTOList) {
            validaLinhaTransporteIntegrationDataDtoCommunity(linhaTransporteIntegrationDTO);
        }

    }

    /**
     * Valida chave origem/destino/material da transportation lane por material.
     */
    private void validaLinhaTransporteProdutoIntegrationDataDtoCommunity(
            LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDTO) {

        if (linhaTransporteProdutoIntegrationDTO == null) {
            throw new IllegalArgumentException("Transportation Line - Material payload is required.");
        }
        if (linhaTransporteProdutoIntegrationDTO.primaryKeyDto == null) {
            throw new IllegalArgumentException("Transportation Line - Material primary key is required.");
        }
        if (isBlank(linhaTransporteProdutoIntegrationDTO.primaryKeyDto.supplyNetworkVersionId)) {
            throw new IllegalArgumentException("Transportation Line - Material supply network version id is required.");
        }
        if (isBlank(linhaTransporteProdutoIntegrationDTO.primaryKeyDto.originLocationId)) {
            throw new IllegalArgumentException("Transportation Line - Material origin location id is required.");
        }
        if (isBlank(linhaTransporteProdutoIntegrationDTO.primaryKeyDto.destinationLocationId)) {
            throw new IllegalArgumentException("Transportation Line - Material destination location id is required.");
        }
        if (isBlank(linhaTransporteProdutoIntegrationDTO.primaryKeyDto.materialId)) {
            throw new IllegalArgumentException("Transportation Line - Material material id is required.");
        }

    }

    /**
     * Valida lote de remocao de lanes material-especificas.
     */
    private void validaLinhaTransporteProdutoIntegrationDataDtoListCommunity(
            List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDTOList) {

        if (linhaTransporteProdutoIntegrationDTOList == null || linhaTransporteProdutoIntegrationDTOList.isEmpty()) {
            throw new IllegalArgumentException("Transportation Line - Material payload list is required.");
        }
        for (LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDTO : linhaTransporteProdutoIntegrationDTOList) {
            validaLinhaTransporteProdutoIntegrationDataDtoCommunity(linhaTransporteProdutoIntegrationDTO);
        }

    }

    /**
     * Valida a fotografia de transportation lanes carregada para listagem.
     *
     * <p>Lista vazia e valida, pois uma versao de malha Community pode ser
     * criada antes das lanes. Itens nulos, chaves parciais ou versao diferente
     * da solicitada indicam inconsistência estrutural da consulta.</p>
     */
    private void validaLinhaTransporteListCarregadaCommunity(
            List<LinhaTransporte> linhaTransporteList,
            String versaoMalhaId) {

        if (linhaTransporteList == null) {
            throw new IllegalStateException(
                    "Transportation Line list snapshot is required for Supply Network Version "
                            + versaoMalhaId
                            + ".");
        }

        for (int indiceLinhaTransporte = 0;
                indiceLinhaTransporte < linhaTransporteList.size();
                indiceLinhaTransporte++) {
            LinhaTransporte linhaTransporte = linhaTransporteList.get(indiceLinhaTransporte);

            if (linhaTransporte == null) {
                throw new IllegalStateException(
                        "Transportation Line at index "
                                + indiceLinhaTransporte
                                + " is required in list snapshot for Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
            if (hasLinhaTransporteKeyIncompletaCommunity(linhaTransporte)) {
                throw new IllegalStateException(
                        "Transportation Line at index "
                                + indiceLinhaTransporte
                                + " has incomplete key in list snapshot.");
            }
            if (!versaoMalhaId.equals(linhaTransporte.getVersaoMalha().getId())) {
                throw new IllegalStateException(
                        "Transportation Line at index "
                                + indiceLinhaTransporte
                                + " must match requested Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
        }

    }

    /**
     * Valida a fotografia de lanes material-especificas carregada para
     * listagem front.
     */
    private void validaLinhaTransporteProdutoListCarregadaCommunity(
            List<LinhaTransporteProduto> linhaTransporteProdutoList,
            String versaoMalhaId) {

        if (linhaTransporteProdutoList == null) {
            throw new IllegalStateException(
                    "Transportation Line - Material list snapshot is required for Supply Network Version "
                            + versaoMalhaId
                            + ".");
        }

        for (int indiceLinhaTransporteProduto = 0;
                indiceLinhaTransporteProduto < linhaTransporteProdutoList.size();
                indiceLinhaTransporteProduto++) {
            LinhaTransporteProduto linhaTransporteProduto =
                    linhaTransporteProdutoList.get(indiceLinhaTransporteProduto);

            if (linhaTransporteProduto == null) {
                throw new IllegalStateException(
                        "Transportation Line - Material at index "
                                + indiceLinhaTransporteProduto
                                + " is required in list snapshot for Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
            if (linhaTransporteProduto.getLinhaTransporteProdutoCompositeKey() == null
                    || hasLinhaTransporteKeyIncompletaCommunity(linhaTransporteProduto.getLinhaTransporte())
                    || linhaTransporteProduto.getProduto() == null
                    || isBlank(linhaTransporteProduto.getProduto().getId())) {
                throw new IllegalStateException(
                        "Transportation Line - Material at index "
                                + indiceLinhaTransporteProduto
                                + " has incomplete key in list snapshot.");
            }
            if (!versaoMalhaId.equals(linhaTransporteProduto.getLinhaTransporte().getVersaoMalha().getId())) {
                throw new IllegalStateException(
                        "Transportation Line - Material at index "
                                + indiceLinhaTransporteProduto
                                + " must match requested Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
        }

    }

    /**
     * Valida a fotografia DTO de transportation lanes devolvida pelo mapper.
     *
     * <p>A entidade ja foi validada antes da conversao, mas a SPA consome o
     * DTO final. A chave precisa permanecer completa e a distancia, campo
     * Enterprise, nao pode vazar na resposta Community.</p>
     */
    private void validaLinhaTransporteIntegrationDataDtoListCarregadaCommunity(
            List<LinhaTransporteIntegrationDataDto> linhaTransporteIntegrationDataDtoList,
            String versaoMalhaId) {

        if (linhaTransporteIntegrationDataDtoList == null) {
            throw new IllegalStateException(
                    "Transportation Line DTO list snapshot is required for Supply Network Version "
                            + versaoMalhaId
                            + ".");
        }

        for (int indiceLinhaTransporte = 0;
                indiceLinhaTransporte < linhaTransporteIntegrationDataDtoList.size();
                indiceLinhaTransporte++) {
            LinhaTransporteIntegrationDataDto linhaTransporteIntegrationDataDto =
                    linhaTransporteIntegrationDataDtoList.get(indiceLinhaTransporte);

            if (linhaTransporteIntegrationDataDto == null) {
                throw new IllegalStateException(
                        "Transportation Line DTO at index "
                                + indiceLinhaTransporte
                                + " is required in list snapshot for Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
            if (linhaTransporteIntegrationDataDto.primaryKeyDto == null) {
                throw new IllegalStateException(
                        "Transportation Line DTO at index "
                                + indiceLinhaTransporte
                                + " has no primary key in list snapshot.");
            }
            if (isBlank(linhaTransporteIntegrationDataDto.primaryKeyDto.supplyNetworkVersionId)
                    || isBlank(linhaTransporteIntegrationDataDto.primaryKeyDto.originLocationId)
                    || isBlank(linhaTransporteIntegrationDataDto.primaryKeyDto.destinationLocationId)) {
                throw new IllegalStateException(
                        "Transportation Line DTO at index "
                                + indiceLinhaTransporte
                                + " has incomplete key in list snapshot.");
            }
            if (!versaoMalhaId.equals(linhaTransporteIntegrationDataDto.primaryKeyDto.supplyNetworkVersionId)) {
                throw new IllegalStateException(
                        "Transportation Line DTO at index "
                                + indiceLinhaTransporte
                                + " must match requested Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
            if (linhaTransporteIntegrationDataDto.distanceKm != null) {
                throw new IllegalStateException(
                        "Transportation Line DTO at index "
                                + indiceLinhaTransporte
                                + " must not expose distance in Community list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de transportation lanes por material devolvida
     * pelo mapper.
     */
    private void validaLinhaTransporteProdutoIntegrationDataDtoListCarregadaCommunity(
            List<LinhaTransporteProdutoIntegrationDataDto> linhaTransporteProdutoIntegrationDataDtoList,
            String versaoMalhaId) {

        if (linhaTransporteProdutoIntegrationDataDtoList == null) {
            throw new IllegalStateException(
                    "Transportation Line - Material DTO list snapshot is required for Supply Network Version "
                            + versaoMalhaId
                            + ".");
        }

        for (int indiceLinhaTransporteProduto = 0;
                indiceLinhaTransporteProduto < linhaTransporteProdutoIntegrationDataDtoList.size();
                indiceLinhaTransporteProduto++) {
            LinhaTransporteProdutoIntegrationDataDto linhaTransporteProdutoIntegrationDataDto =
                    linhaTransporteProdutoIntegrationDataDtoList.get(indiceLinhaTransporteProduto);

            if (linhaTransporteProdutoIntegrationDataDto == null) {
                throw new IllegalStateException(
                        "Transportation Line - Material DTO at index "
                                + indiceLinhaTransporteProduto
                                + " is required in list snapshot for Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
            if (linhaTransporteProdutoIntegrationDataDto.primaryKeyDto == null) {
                throw new IllegalStateException(
                        "Transportation Line - Material DTO at index "
                                + indiceLinhaTransporteProduto
                                + " has no primary key in list snapshot.");
            }
            if (isBlank(linhaTransporteProdutoIntegrationDataDto.primaryKeyDto.supplyNetworkVersionId)
                    || isBlank(linhaTransporteProdutoIntegrationDataDto.primaryKeyDto.originLocationId)
                    || isBlank(linhaTransporteProdutoIntegrationDataDto.primaryKeyDto.destinationLocationId)
                    || isBlank(linhaTransporteProdutoIntegrationDataDto.primaryKeyDto.materialId)) {
                throw new IllegalStateException(
                        "Transportation Line - Material DTO at index "
                                + indiceLinhaTransporteProduto
                                + " has incomplete key in list snapshot.");
            }
            if (!versaoMalhaId.equals(linhaTransporteProdutoIntegrationDataDto.primaryKeyDto.supplyNetworkVersionId)) {
                throw new IllegalStateException(
                        "Transportation Line - Material DTO at index "
                                + indiceLinhaTransporteProduto
                                + " must match requested Supply Network Version "
                                + versaoMalhaId
                                + ".");
            }
            if (linhaTransporteProdutoIntegrationDataDto.distanceKm != null) {
                throw new IllegalStateException(
                        "Transportation Line - Material DTO at index "
                                + indiceLinhaTransporteProduto
                                + " must not expose distance in Community list snapshot.");
            }
        }

    }

    /**
     * Valida as entidades efetivamente montadas para delete de transportation
     * lanes.
     *
     * <p>A validacao anterior cobre o payload da tela. Esta guarda cobre o
     * snapshot pos-mapper: support data quebrado, mapa de persistidos
     * incompleto ou mapper regressivo nao podem chegar ao `deleteAll` com
     * entidade nula ou chave parcial.</p>
     */
    private void validaLinhaTransporteEntityListParaDeleteCommunity(
            List<LinhaTransporte> linhaTransporteListParaDelete) {

        if (linhaTransporteListParaDelete == null || linhaTransporteListParaDelete.isEmpty()) {
            throw new IllegalStateException("Mapped Transportation Line entity list is required for delete.");
        }

        int indiceLinhaTransporte = 0;
        for (LinhaTransporte linhaTransporte : linhaTransporteListParaDelete) {
            if (linhaTransporte == null) {
                throw new IllegalStateException(
                        "Mapped Transportation Line entity at index "
                                + indiceLinhaTransporte
                                + " is required for delete.");
            }
            if (hasLinhaTransporteKeyIncompletaCommunity(linhaTransporte)) {
                throw new IllegalStateException(
                        "Mapped Transportation Line entity at index "
                                + indiceLinhaTransporte
                                + " has incomplete key for delete.");
            }
            indiceLinhaTransporte++;
        }

    }

    /**
     * Valida as entidades material-especificas antes do `deleteAll`.
     */
    private void validaLinhaTransporteProdutoEntityListParaDeleteCommunity(
            List<LinhaTransporteProduto> linhaTransporteProdutoListParaDelete) {

        if (linhaTransporteProdutoListParaDelete == null || linhaTransporteProdutoListParaDelete.isEmpty()) {
            throw new IllegalStateException("Mapped Transportation Line - Material entity list is required for delete.");
        }

        int indiceLinhaTransporteProduto = 0;
        for (LinhaTransporteProduto linhaTransporteProduto : linhaTransporteProdutoListParaDelete) {
            if (linhaTransporteProduto == null) {
                throw new IllegalStateException(
                        "Mapped Transportation Line - Material entity at index "
                                + indiceLinhaTransporteProduto
                                + " is required for delete.");
            }
            if (linhaTransporteProduto.getLinhaTransporteProdutoCompositeKey() == null
                    || hasLinhaTransporteKeyIncompletaCommunity(linhaTransporteProduto.getLinhaTransporte())
                    || linhaTransporteProduto.getProduto() == null
                    || isBlank(linhaTransporteProduto.getProduto().getId())) {
                throw new IllegalStateException(
                        "Mapped Transportation Line - Material entity at index "
                                + indiceLinhaTransporteProduto
                                + " has incomplete key for delete.");
            }
            indiceLinhaTransporteProduto++;
        }

    }

    /**
     * Valida a transportation lane salva antes de retornar sucesso ao front.
     */
    private void validaLinhaTransporteSalvaCommunity(LinhaTransporte linhaTransporteSalva) {

        if (linhaTransporteSalva == null) {
            throw new IllegalStateException("Saved Transportation Line snapshot is required.");
        }
        if (hasLinhaTransporteKeyIncompletaCommunity(linhaTransporteSalva)) {
            throw new IllegalStateException("Saved Transportation Line key is required.");
        }

    }

    /**
     * Valida a lane/material salva antes de retornar sucesso ao front.
     *
     * <p>A lane/material depende da chave completa da lane base mais o
     * material. Se qualquer parte voltar incompleta, o heuristico nao consegue
     * aplicar override material-especifico de prioridade, lead time ou lotes.</p>
     */
    private void validaLinhaTransporteProdutoSalvaCommunity(
            LinhaTransporteProduto linhaTransporteProdutoSalva) {

        if (linhaTransporteProdutoSalva == null) {
            throw new IllegalStateException("Saved Transportation Line - Material snapshot is required.");
        }
        if (linhaTransporteProdutoSalva.getLinhaTransporteProdutoCompositeKey() == null
                || hasLinhaTransporteKeyIncompletaCommunity(linhaTransporteProdutoSalva.getLinhaTransporte())
                || linhaTransporteProdutoSalva.getProduto() == null
                || isBlank(linhaTransporteProdutoSalva.getProduto().getId())) {
            throw new IllegalStateException("Saved Transportation Line - Material key is required.");
        }

    }

    private boolean hasLinhaTransporteKeyIncompletaCommunity(
            LinhaTransporte linhaTransporte) {

        return linhaTransporte == null
                || linhaTransporte.getLinhaTransporteCompositeKey() == null
                || linhaTransporte.getVersaoMalha() == null
                || isBlank(linhaTransporte.getVersaoMalha().getId())
                || linhaTransporte.getLocationOrigem() == null
                || isBlank(linhaTransporte.getLocationOrigem().getId())
                || linhaTransporte.getLocationDestino() == null
                || isBlank(linhaTransporte.getLocationDestino().getId());

    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }

}
