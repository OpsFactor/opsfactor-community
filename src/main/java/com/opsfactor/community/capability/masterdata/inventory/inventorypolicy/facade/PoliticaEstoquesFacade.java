package com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade;

import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoques;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.domain.PoliticaEstoquesMaterialLocation;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesMaterialLocationRepository;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.repository.PoliticaEstoquesRepository;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.facade.dto.PoliticaEstoquesDTO;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service de fronteira para politicas operacionais de safety stock.
 *
 * <p>O Community permite manter regras simples por material/location para que o
 * Supply Planning heuristico calcule estoque de seguranca. A otimizacao de
 * politica de estoques permanece Enterprise; por isso a API rejeita
 * frequencia de reabastecimento e sempre limpa esse campo antes de persistir
 * as linhas.</p>
 */
@Service
public class PoliticaEstoquesFacade {

    /**
     * Repository da politica operacional de safety stock.
     *
     * <p>Este bean persiste somente o cadastro usado pelo Supply Planning
     * heuristico. Otimizacao de politica de estoques e seus resultados ficam
     * fora desta service Community.</p>
     */
    @Autowired
    private PoliticaEstoquesRepository politicaEstoquesRepository;

    /**
     * Repository das regras material/location da politica operacional.
     *
     * <p>A lista recebida pela API Community e tratada como snapshot completo
     * da politica. Por isso a persistencia remove as linhas antigas e grava o
     * novo conjunto em batch.</p>
     */
    @Autowired
    private PoliticaEstoquesMaterialLocationRepository politicaEstoquesMaterialLocationRepository;

    /**
     * Repository de material usado para validar que cada regra aponta para um
     * cadastro existente antes de persistir a linha material/location.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository de location usado para validar que cada regra aponta para um
     * cadastro existente antes de persistir a linha material/location.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Lista as politicas operacionais de safety stock disponiveis no Community.
     */
    public List<PoliticaEstoquesDTO> getPoliticaEstoquesDTOList() {

        List<PoliticaEstoques> politicaEstoquesList =
                politicaEstoquesRepository.customFindAllWithMaterialLocation();
        validaPoliticaEstoquesListCarregadaCommunity(politicaEstoquesList);

        return politicaEstoquesList
                .stream()
                .sorted(Comparator.comparing(PoliticaEstoques::getId))
                .map(this::getPoliticaEstoquesDTO)
                .toList();

    }

    /**
     * Valida a fotografia de politicas carregada para listagem.
     *
     * <p>Lista vazia e valida porque uma instalacao Community nova pode ainda
     * nao ter politica operacional de safety stock. Lista nula, item nulo ou
     * politica sem id indicam quebra de repository/snapshot e precisam falhar
     * antes da ordenacao por id ou da conversao para DTO.</p>
     */
    private void validaPoliticaEstoquesListCarregadaCommunity(
            List<PoliticaEstoques> politicaEstoquesList) {

        if (politicaEstoquesList == null) {
            throw new IllegalStateException(
                    "Inventory policy repository returned null list for Community listing.");
        }

        for (int index = 0; index < politicaEstoquesList.size(); index++) {
            PoliticaEstoques politicaEstoques = politicaEstoquesList.get(index);
            if (politicaEstoques == null) {
                throw new IllegalStateException(
                        "Inventory policy repository returned null item at index "
                                + index
                                + " for Community listing.");
            }
            if (politicaEstoques.getId() == null || politicaEstoques.getId().isBlank()) {
                throw new IllegalStateException(
                        "Inventory policy repository returned item without id at index "
                                + index
                                + " for Community listing.");
            }
        }

    }

    /**
     * Carrega uma politica operacional especifica, incluindo regras
     * material/location, sem expor parametros de otimizacao de politica de
     * estoques.
     */
    public PoliticaEstoquesDTO getPoliticaEstoquesDTO(String politicaEstoquesId) {

        return getPoliticaEstoquesDTO(getPoliticaEstoquesObrigatoria(politicaEstoquesId));

    }

    /**
     * Persiste a politica operacional Community como snapshot completo.
     *
     * <p>Payloads com frequencia de reabastecimento sao rejeitados antes de
     * qualquer acesso aos repositories porque esse campo pertence ao modulo
     * Enterprise de otimizacao de politica de estoques.</p>
     */
    @Transactional
    public void savePoliticaEstoquesDTO(PoliticaEstoquesDTO politicaEstoquesDTO) {

        validaPayloadBasicoPoliticaEstoquesCommunity(politicaEstoquesDTO);
        validaPoliticaEstoquesCommunity(politicaEstoquesDTO);
        validaChavesMaterialLocationPoliticaEstoquesCommunity(politicaEstoquesDTO);

        /*
         * Daqui em diante todas as chaves funcionais ja foram validadas e os
         * campos Enterprise ja foram bloqueados. Repositories podem assumir
         * payload estruturalmente completo.
         */
        Optional<PoliticaEstoques> politicaEstoquesOptional = politicaEstoquesRepository
                .findById(politicaEstoquesDTO.getId());

        /*
         * Optional.empty() e o caminho funcional de criacao de nova politica.
         * Optional nulo indica repository quebrado e deve falhar antes de montar
         * o header ou remover as linhas material/location existentes.
         */
        if (politicaEstoquesOptional == null) {
            throw new IllegalStateException(
                    "Inventory policy repository returned null Optional for save id "
                            + politicaEstoquesDTO.getId()
                            + ".");
        }

        PoliticaEstoques politicaEstoques = politicaEstoquesOptional
                .orElseGet(PoliticaEstoques::new);

        politicaEstoques.setId(politicaEstoquesDTO.getId());
        politicaEstoques.setPrioridade(politicaEstoquesDTO.getPrioridade());
        politicaEstoques.setDataHorarioInicio(politicaEstoquesDTO.getDataHorarioInicio());
        politicaEstoques.setDataHorarioFim(politicaEstoquesDTO.getDataHorarioFim());
        /*
         * A policy e o cabeçalho funcional usado pelas linhas material/location.
         * Validamos o snapshot salvo antes de remover linhas antigas para
         * evitar que uma persistencia quebrada deixe o cadastro sem filhos e
         * ainda assim retorne sucesso para a tela.
         */
        PoliticaEstoques politicaEstoquesSalva = politicaEstoquesRepository.save(politicaEstoques);
        validaPoliticaEstoquesSalvaCommunity(politicaEstoquesSalva);
        final PoliticaEstoques politicaEstoquesParaLinhasMaterialLocation = politicaEstoquesSalva;

        /*
         * Community trata a lista material/location como snapshot completo da
         * politica. Isto deixa o endpoint simples e evita merge parcial
         * silencioso de regras antigas que poderiam afetar safety stock.
         */
        politicaEstoquesMaterialLocationRepository
                .removeByPoliticaEstoquesMaterialLocationCompositeKeyPoliticaEstoquesId(
                        politicaEstoquesParaLinhasMaterialLocation.getId());
        List<PoliticaEstoquesMaterialLocation> politicaEstoquesMaterialLocationList =
                politicaEstoquesDTO.getMaterialLocationList()
                        .stream()
                        .map(politicaEstoquesMaterialLocationDTO ->
                                getPoliticaEstoquesMaterialLocation(
                                        politicaEstoquesParaLinhasMaterialLocation,
                                        politicaEstoquesMaterialLocationDTO))
                        .toList();
        List<PoliticaEstoquesMaterialLocation> politicaEstoquesMaterialLocationListSalva =
                politicaEstoquesMaterialLocationRepository.saveAll(politicaEstoquesMaterialLocationList);
        validaPoliticaEstoquesMaterialLocationSalvaCommunity(
                politicaEstoquesMaterialLocationListSalva,
                politicaEstoquesMaterialLocationList.size());

    }

    /**
     * Valida o envelope e a lista da politica antes de qualquer regra
     * Enterprise ou repository.
     */
    private void validaPayloadBasicoPoliticaEstoquesCommunity(
            PoliticaEstoquesDTO politicaEstoquesDTO) {

        if (politicaEstoquesDTO == null) {
            throw new IllegalArgumentException("Inventory policy payload must be provided");
        }
        if (politicaEstoquesDTO.getId() == null || politicaEstoquesDTO.getId().isBlank()) {
            throw new IllegalArgumentException("Inventory policy id must be provided");
        }
        if (politicaEstoquesDTO.getMaterialLocationList() == null) {
            throw new IllegalArgumentException("Inventory policy material/location list must be provided");
        }
        for (PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO :
                politicaEstoquesDTO.getMaterialLocationList()) {

            if (politicaEstoquesMaterialLocationDTO == null) {
                throw new IllegalArgumentException(
                        "Inventory policy material/location list cannot contain null values");
            }
        }

    }

    /**
     * Valida as chaves material/location depois do bloqueio Enterprise.
     *
     * <p>Assim, payload que tenta usar frequencia de reabastecimento continua
     * sendo classificado como Enterprise mesmo que venha com ids incompletos.
     * Payload Community sem material/location, por outro lado, falha antes dos
     * repositories de material e location.</p>
     */
    private void validaChavesMaterialLocationPoliticaEstoquesCommunity(
            PoliticaEstoquesDTO politicaEstoquesDTO) {

        for (PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO :
                politicaEstoquesDTO.getMaterialLocationList()) {

            if (politicaEstoquesMaterialLocationDTO.getMaterialId() == null
                    || politicaEstoquesMaterialLocationDTO.getMaterialId().isBlank()) {
                throw new IllegalArgumentException("Inventory policy material id must be provided");
            }
            if (politicaEstoquesMaterialLocationDTO.getLocationId() == null
                    || politicaEstoquesMaterialLocationDTO.getLocationId().isBlank()) {
                throw new IllegalArgumentException("Inventory policy location id must be provided");
            }
        }

    }

    /**
     * Remove a politica operacional e suas linhas material/location.
     */
    @Transactional
    public void deletePoliticaEstoques(String politicaEstoquesId) {

        PoliticaEstoques politicaEstoques = getPoliticaEstoquesObrigatoria(politicaEstoquesId);
        politicaEstoquesMaterialLocationRepository
                .removeByPoliticaEstoquesMaterialLocationCompositeKeyPoliticaEstoquesId(politicaEstoquesId);
        politicaEstoquesRepository.delete(politicaEstoques);

    }

    private PoliticaEstoques getPoliticaEstoquesObrigatoria(String politicaEstoquesId) {

        if (politicaEstoquesId == null || politicaEstoquesId.isBlank()) {
            throw new IllegalArgumentException("Inventory policy id must be provided");
        }

        Optional<PoliticaEstoques> politicaEstoquesOptional = politicaEstoquesRepository
                .customFindById(politicaEstoquesId);

        /*
         * Politica inexistente segue ausencia funcional. Retorno nulo do
         * repository quebra a fronteira de leitura/delete e deve falhar antes
         * de converter DTO ou remover linhas associadas.
         */
        if (politicaEstoquesOptional == null) {
            throw new IllegalStateException(
                    "Inventory policy repository returned null Optional for lookup id "
                            + politicaEstoquesId
                            + ".");
        }

        return politicaEstoquesOptional
                .orElseThrow(() -> new NoSuchElementException("Inventory policy " + politicaEstoquesId + " not found"));

    }

    private PoliticaEstoquesDTO getPoliticaEstoquesDTO(PoliticaEstoques politicaEstoques) {

        PoliticaEstoquesDTO politicaEstoquesDTO = new PoliticaEstoquesDTO();
        politicaEstoquesDTO.setId(politicaEstoques.getId());
        politicaEstoquesDTO.setPrioridade(politicaEstoques.getPrioridadeCadastrada());
        politicaEstoquesDTO.setDataHorarioInicio(politicaEstoques.getDataHorarioInicio());
        politicaEstoquesDTO.setDataHorarioFim(politicaEstoques.getDataHorarioFim());
        politicaEstoquesDTO.setMaterialLocationList(
                politicaEstoques.getPoliticaEstoquesMaterialLocationList()
                        .stream()
                        .sorted(Comparator
                                .comparing((PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation) ->
                                        politicaEstoquesMaterialLocation.getMaterial().getId())
                                .thenComparing(politicaEstoquesMaterialLocation ->
                                        politicaEstoquesMaterialLocation.getLocation().getId()))
                        .map(this::getPoliticaEstoquesMaterialLocationDTO)
                        .toList());
        return politicaEstoquesDTO;

    }

    private PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO getPoliticaEstoquesMaterialLocationDTO(
            PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation) {

        PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO =
                new PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO();
        politicaEstoquesMaterialLocationDTO.setMaterialId(politicaEstoquesMaterialLocation.getMaterial().getId());
        politicaEstoquesMaterialLocationDTO.setLocationId(politicaEstoquesMaterialLocation.getLocation().getId());
        politicaEstoquesMaterialLocationDTO.setModeloReabastecimento(
                politicaEstoquesMaterialLocation.getModeloReabastecimentoCadastrado());
        politicaEstoquesMaterialLocationDTO.setModeloOperacional(
                politicaEstoquesMaterialLocation.getModeloOperacionalCadastrado());
        politicaEstoquesMaterialLocationDTO.setCalculoSafetyStock(
                politicaEstoquesMaterialLocation.getCalculoSafetyStockCadastrado());
        politicaEstoquesMaterialLocationDTO.setEstoqueSegurancaDrpOuTargetKanban(
                politicaEstoquesMaterialLocation.getEstoqueSegurancaDrpOuTargetKanbanCadastrado());
        politicaEstoquesMaterialLocationDTO.setEstoqueMaximoDrp(
                politicaEstoquesMaterialLocation.getEstoqueMaximoDrpCadastrado());
        politicaEstoquesMaterialLocationDTO.setFrequenciaReabastecimentoDias(
                getFrequenciaReabastecimentoDiasParaResposta(
                        politicaEstoquesMaterialLocation));
        return politicaEstoquesMaterialLocationDTO;

    }

    private PoliticaEstoquesMaterialLocation getPoliticaEstoquesMaterialLocation(
            PoliticaEstoques politicaEstoques,
            PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO) {

        Optional<Produto> materialOptional =
                produtoRepository.findById(politicaEstoquesMaterialLocationDTO.getMaterialId());

        /*
         * Material ausente continua erro funcional da regra. Optional nulo e
         * repository quebrado e deve falhar antes de criar a chave composta da
         * linha material/location.
         */
        if (materialOptional == null) {
            throw new IllegalStateException(
                    "Material repository returned null Optional for inventory policy material id "
                            + politicaEstoquesMaterialLocationDTO.getMaterialId()
                            + ".");
        }

        Produto material = materialOptional
                .orElseThrow(() -> new NoSuchElementException(
                        "Material " + politicaEstoquesMaterialLocationDTO.getMaterialId() + " not found"));
        Optional<Location> locationOptional =
                locationRepository.findById(politicaEstoquesMaterialLocationDTO.getLocationId());

        /*
         * Location segue a mesma regra do material: ausencia funcional e
         * diferente de repository quebrado.
         */
        if (locationOptional == null) {
            throw new IllegalStateException(
                    "Location repository returned null Optional for inventory policy location id "
                            + politicaEstoquesMaterialLocationDTO.getLocationId()
                            + ".");
        }

        Location location = locationOptional
                .orElseThrow(() -> new NoSuchElementException(
                        "Location " + politicaEstoquesMaterialLocationDTO.getLocationId() + " not found"));

        PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation = new PoliticaEstoquesMaterialLocation(
                new PoliticaEstoquesMaterialLocation.PoliticaEstoquesMaterialLocationCompositeKey(
                        politicaEstoques,
                        material,
                        location));
        politicaEstoquesMaterialLocation.setModeloReabastecimento(
                politicaEstoquesMaterialLocationDTO.getModeloReabastecimento());
        politicaEstoquesMaterialLocation.setModeloOperacional(
                politicaEstoquesMaterialLocationDTO.getModeloOperacional());
        politicaEstoquesMaterialLocation.setCalculoSafetyStock(
                politicaEstoquesMaterialLocationDTO.getCalculoSafetyStock());
        politicaEstoquesMaterialLocation.setEstoqueSegurancaDrpOuTargetKanban(
                politicaEstoquesMaterialLocationDTO.getEstoqueSegurancaDrpOuTargetKanban());
        politicaEstoquesMaterialLocation.setEstoqueMaximoDrp(
                politicaEstoquesMaterialLocationDTO.getEstoqueMaximoDrp());
        politicaEstoquesMaterialLocation.setFrequenciaReabastecimentoDias(
                getFrequenciaReabastecimentoDiasParaPersistencia(
                        politicaEstoquesMaterialLocationDTO));
        return politicaEstoquesMaterialLocation;

    }

    /**
     * Protege a API Community contra parametros de otimizacao de politica de
     * estoques. Safety stock operacional continua permitido; frequencia de
     * reabastecimento fica reservada ao Enterprise.
     */
    private void validaPoliticaEstoquesCommunity(PoliticaEstoquesDTO politicaEstoquesDTO) {

        for (PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO :
                politicaEstoquesDTO.getMaterialLocationList()) {

            validaFrequenciaReabastecimentoDias(
                    politicaEstoquesMaterialLocationDTO.getFrequenciaReabastecimentoDias());
        }

    }

    /**
     * Mantem a frequencia de reabastecimento neutra no contrato Community.
     *
     * <p>A coluna persiste na tabela compartilhada por compatibilidade de
     * schema, mas nao participa da resposta da API Community. O overlay
     * Enterprise a reabre sem precisar duplicar a persistencia do snapshot de
     * politica.</p>
     */
    protected Double getFrequenciaReabastecimentoDiasParaResposta(
            PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation) {

        return null;

    }

    /**
     * Protege o payload Community contra a frequencia privada de reposicao.
     */
    protected void validaFrequenciaReabastecimentoDias(Double frequenciaReabastecimentoDias) {

        if (frequenciaReabastecimentoDias != null) {
            throw new RequiresEnterpriseVersionException("Inventory policy optimization replenishment frequency");
        }

    }

    /**
     * Mantem a escrita Community neutra depois da validacao do payload.
     *
     * <p>O Enterprise substitui somente este ponto de extensao e devolve o
     * valor recebido no DTO; todo o restante do snapshot, incluindo validacao
     * de chaves e persistencia em batch, continua compartilhado.</p>
     */
    protected Double getFrequenciaReabastecimentoDiasParaPersistencia(
            PoliticaEstoquesDTO.PoliticaEstoquesMaterialLocationDTO politicaEstoquesMaterialLocationDTO) {

        return null;

    }

    /**
     * Valida o retorno salvo do cabeçalho da politica operacional.
     *
     * <p>As linhas material/location usam o id da policy como parte da chave.
     * Se o repository devolver nulo ou uma entidade sem id, a substituicao do
     * snapshot completo nao pode prosseguir para o remove/saveAll das linhas.</p>
     */
    private void validaPoliticaEstoquesSalvaCommunity(PoliticaEstoques politicaEstoquesSalva) {

        if (politicaEstoquesSalva == null) {
            throw new IllegalArgumentException("Saved inventory policy snapshot is required");
        }
        if (politicaEstoquesSalva.getId() == null || politicaEstoquesSalva.getId().isBlank()) {
            throw new IllegalArgumentException("Saved inventory policy id is required");
        }

    }

    /**
     * Valida o retorno salvo das regras material/location da politica.
     *
     * <p>O endpoint trata a lista como snapshot completo: depois que as linhas
     * antigas sao removidas, um retorno quebrado do `saveAll` nao pode ser
     * considerado sucesso silencioso, pois isso deixaria o safety stock com
     * fotografia parcial ou nao auditavel.</p>
     */
    private void validaPoliticaEstoquesMaterialLocationSalvaCommunity(
            List<PoliticaEstoquesMaterialLocation> politicaEstoquesMaterialLocationListSalva,
            int numeroPoliticasEstoquesMaterialLocationEsperado) {

        if (politicaEstoquesMaterialLocationListSalva == null) {
            throw new IllegalArgumentException("Saved inventory policy material/location collection is required");
        }
        if (politicaEstoquesMaterialLocationListSalva.size()
                != numeroPoliticasEstoquesMaterialLocationEsperado) {
            throw new IllegalArgumentException(
                    "Saved inventory policy material/location collection size "
                            + politicaEstoquesMaterialLocationListSalva.size()
                            + " differs from expected size "
                            + numeroPoliticasEstoquesMaterialLocationEsperado);
        }
        int indicePoliticaEstoquesMaterialLocation = 0;
        for (PoliticaEstoquesMaterialLocation politicaEstoquesMaterialLocation :
                politicaEstoquesMaterialLocationListSalva) {

            if (politicaEstoquesMaterialLocation == null) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " is required");
            }
            if (politicaEstoquesMaterialLocation.getPoliticaEstoquesMaterialLocationCompositeKey() == null) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have a primary key");
            }
            if (politicaEstoquesMaterialLocation.getPoliticaEstoques() == null) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have an inventory policy");
            }
            if (politicaEstoquesMaterialLocation.getPoliticaEstoques().getId() == null
                    || politicaEstoquesMaterialLocation.getPoliticaEstoques().getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have an inventory policy id");
            }
            if (politicaEstoquesMaterialLocation.getMaterial() == null) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have a material");
            }
            if (politicaEstoquesMaterialLocation.getMaterial().getId() == null
                    || politicaEstoquesMaterialLocation.getMaterial().getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have a material id");
            }
            if (politicaEstoquesMaterialLocation.getLocation() == null) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have a location");
            }
            if (politicaEstoquesMaterialLocation.getLocation().getId() == null
                    || politicaEstoquesMaterialLocation.getLocation().getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Saved inventory policy material/location item at index "
                                + indicePoliticaEstoquesMaterialLocation
                                + " must have a location id");
            }
            indicePoliticaEstoquesMaterialLocation++;
        }

    }

}
