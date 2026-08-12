package com.opsfactor.community.capability.cluster.facade;

import com.opsfactor.community.capability.cluster.facade.dto.*;
import com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto;
import com.opsfactor.community.capability.masterdata.classification.characteristic.facade.dto.CaracteristicaProdutoDTO;
import com.opsfactor.community.capability.cluster.facade.dto.allocation.AlocacaoClusterLocationDTO;
import com.opsfactor.community.capability.cluster.facade.dto.allocation.AlocacaoClusterMaterialDTO;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterLocationsMapper;
import com.opsfactor.community.capability.cluster.facade.mapper.ClusterProdutosMapper;
import com.opsfactor.community.capability.cluster.domain.location.*;
import com.opsfactor.community.capability.cluster.domain.produto.*;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjectionFactory;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterMateriaisRepository;
import com.opsfactor.community.capability.cluster.repository.material.ClusterProdutosRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.RegraAlocacaoClusterLocationsRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.produto.RegraAlocacaoClusterProdutosRepository;
import com.opsfactor.community.capability.masterdata.classification.characteristic.repository.CaracteristicaMaterialRepository;
import com.opsfactor.community.capability.cluster.facade.dto.ClusterRuleDTO;
import com.opsfactor.community.capability.masterdata.demand.dfu.facade.dto.DFUDTO;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.platform.utility.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service front de clusterizacao Community.
 *
 * <p>Clusters fazem parte do Community porque os fluxos de planejamento usam
 * a abertura por material e location. O recorte publico mantem um esquema
 * unico de clusters e aceita apenas as regras cadastradas no catalogo
 * Community.</p>
 */
@Service
public class ClusteringFacade {

    /**
     * Repository das regras de alocacao de clusters de materiais. No Community
     * ele persiste regras por status e por caracteristica publica para Demand
     * Planning.
     */
    @Autowired
    private RegraAlocacaoClusterProdutosRepository regraAlocacaoClusterProdutosRepository;

    /** Catalogo Community de caracteristicas reais dos materiais, carregado sem N+1 para validar regras. */
    @Autowired
    private CaracteristicaMaterialRepository caracteristicaMaterialRepository;

    /**
     * Repository dos clusters de locations usados por Demand Planning e
     * Planning Book.
     */
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;

    /**
     * Repository da definicao unica de clusters de materiais.
     */
    @Autowired
    private ClusterMateriaisRepository clusterMateriaisDemandPlanningRepository;

    /**
     * Repository base de clusters de materiais, usado apenas em delecoes por id
     * quando a entidade concreta ja foi validada como cluster Community.
     */
    @Autowired
    private ClusterProdutosRepository clusterProdutosRepository;

    /**
     * Repository das regras de alocacao de clusters de locations. No Community
     * aceita pais/estado e tipo de location; caracteristicas dinamicas sao
     * Enterprise.
     */
    @Autowired
    private RegraAlocacaoClusterLocationsRepository regraAlocacaoClusterLocationsRepository;

    /**
     * Factory da projection central usada para calcular alocacoes atuais de
     * materiais/locations em clusters sem reimplementar a regra de prioridade.
     */
    @Autowired
    private ClusterEParametrosProjectionFactory clusterEParametrosProjectionFactory;


    public void alocaRegrasAoClusterProdutos(ClusterProdutosDTO clusterProdutosDTO, ClusterProdutos clusterProdutos) {

        validaRegrasAlocacaoClusterProdutosCommunity(clusterProdutosDTO);

        for (RegraAlocaoClusterProdutosDTO regraAlocacaoClusterDTO : clusterProdutosDTO.getRegraAlocacaoClusterDTOList()) {
            RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutos;

            Optional<RegraAlocacaoClusterProdutos> optionalRegraAlocacaoClusterProdutos = clusterProdutos.getRegrasAlocacaoClusterProdutos().stream()
                    .filter(regraAlocacaoClusterProdutosExistente ->
                            Objects.equals(
                                    regraAlocacaoClusterProdutosExistente.getId(),
                                    regraAlocacaoClusterDTO.getId()))
                    .findAny();
            if (!optionalRegraAlocacaoClusterProdutos.isPresent()) {
                regraAlocacaoClusterProdutos = new RegraAlocacaoClusterProdutos();
            } else {
                /*
                 * Regra ja existe no cluster. A UI envia a lista completa, mas
                 * atualizacao de regra existente neste fluxo nao altera o tipo
                 * nem os filhos; remocao ja foi tratada antes pelo diff de ids.
                 */
                continue;
            }

            regraAlocacaoClusterProdutos.setClusterProdutos(clusterProdutos);
            regraAlocacaoClusterProdutos.setPrioridade(clusterProdutosDTO.getPriority());
            /*
             * O primeiro save materializa o id usado pelos filhos de status.
             * A regra so entra no agregado depois dessa fotografia salva para
             * evitar hash mutavel em Set e para falhar cedo se o repository
             * devolver snapshot nulo ou sem chave.
             */
            regraAlocacaoClusterProdutos =
                    regraAlocacaoClusterProdutosRepository.save(regraAlocacaoClusterProdutos);
            validaRegraAlocacaoClusterProdutosSalvaCommunity(regraAlocacaoClusterProdutos);
            clusterProdutos.getRegrasAlocacaoClusterProdutos().add(regraAlocacaoClusterProdutos);

            CaracteristicaProdutoDTO caracteristicaDTO = regraAlocacaoClusterDTO.getCaracteristicaDTO();

            switch (regraAlocacaoClusterDTO.getCriterio()) {
                case STATUS_PRODUTO:
                    regraAlocacaoClusterProdutos.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO);
                    if (caracteristicaDTO.getDescricao().toUpperCase().equals("NOT RELEASED")) {
                        regraAlocacaoClusterProdutos.addStatusProduto(Constantes.StatusProduto.NAO_LANCADO);
                    } else if (caracteristicaDTO.getDescricao().toUpperCase().equals("NEW")) {
                        throw new RequiresEnterpriseVersionException("New material cluster allocation");
                    } else if (caracteristicaDTO.getDescricao().toUpperCase().equals("REGULAR")) {
                        regraAlocacaoClusterProdutos.addStatusProduto(Constantes.StatusProduto.REGULAR);
                    } else if (caracteristicaDTO.getDescricao().toUpperCase().equals("DISCONTINUED")) {
                        regraAlocacaoClusterProdutos.addStatusProduto(Constantes.StatusProduto.DESCONTINUADO);
                    }
                    break;
                case CARACTERISTICA:
                    regraAlocacaoClusterProdutos.setRegraAlocacaoTipo(
                            Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA);
                    CaracteristicaProduto caracteristicaProduto = getCaracteristicaProdutoCommunity(
                            regraAlocacaoClusterDTO.getCaracteristicaDTO().getCaracteristicaId());
                    for (String atributoCaracteristica : regraAlocacaoClusterDTO.getCaracteristicaDTO().getListaAtributos()) {
                        if (!caracteristicaProduto.getValoresCaracteristica().contains(atributoCaracteristica)) {
                            throw new IllegalArgumentException(
                                    "Material characteristic value is not published by the Community catalog: "
                                            + atributoCaracteristica);
                        }
                        RegraAlocacaoClusterProdutosCaracteristica regraCaracteristica =
                                new RegraAlocacaoClusterProdutosCaracteristica(
                                        new RegraAlocacaoClusterProdutosCaracteristica
                                                .RegraAlocacaoClusterProdutosCaracteristicaCompositeKey(
                                                        regraAlocacaoClusterProdutos,
                                                        caracteristicaProduto,
                                                        atributoCaracteristica));
                        regraAlocacaoClusterProdutos.addRegraAlocacaoCaracteristica(regraCaracteristica);
                    }
                    break;
            }
        }
    }

    public void alocaRegrasAoClusterLocations(ClusterLocationsDTO clusterLocationsDTO, ClusterLocations clusterLocations) {

        validaRegrasAlocacaoClusterLocationsCommunity(clusterLocationsDTO);

        for (RegraAlocaoClusterLocationsDTO regraAlocacaoClusterDTO : clusterLocationsDTO.getRegraAlocacaoClusterDTOList()) {
            RegraAlocacaoClusterLocations regraAlocacaoClusterLocations;

            Optional<RegraAlocacaoClusterLocations> optionalRegraAlocacaoClusterLocations = clusterLocations.getRegrasAlocacaoClusterLocations().stream()
                    .filter(regraAlocacaoClusterLocationsExistente ->
                            Objects.equals(
                                    regraAlocacaoClusterLocationsExistente.getId(),
                                    regraAlocacaoClusterDTO.getId()))
                    .findAny();
            if (!optionalRegraAlocacaoClusterLocations.isPresent()) {
                regraAlocacaoClusterLocations = new RegraAlocacaoClusterLocations();
            } else {
                /*
                 * Regra ja existe no cluster. Alteracoes estruturais ficam fora
                 * deste fluxo Community; removidas ja foram sincronizadas pelo
                 * diff de ids antes de incluir novas regras.
                 */
                continue;
            }

            regraAlocacaoClusterLocations.setClusterLocations(clusterLocations);
            regraAlocacaoClusterLocations.setPrioridade(clusterLocationsDTO.getPriority());
            /*
             * Mesmo contrato dos clusters de material: o id da regra e
             * necessario antes de criar filhos pais/estado ou tipo de
             * location, e a fotografia salva precisa estar integra.
             */
            regraAlocacaoClusterLocations =
                    regraAlocacaoClusterLocationsRepository.save(regraAlocacaoClusterLocations);
            validaRegraAlocacaoClusterLocationsSalvaCommunity(regraAlocacaoClusterLocations);
            clusterLocations.getRegrasAlocacaoClusterLocations().add(regraAlocacaoClusterLocations);

            switch (regraAlocacaoClusterDTO.getCriterio()) {
                case TIPO_LOCATION:
                    regraAlocacaoClusterLocations.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterLocationsTipo.TIPO_LOCATION);
                    Location.TipoLocation tipoLocation = ((RegraAlocaoClusterLocationsTipoLocationDTO) regraAlocacaoClusterDTO).getLocationType();
                    RegraAlocacaoClusterLocationsTipoLocation regraAlocacaoClusterLocationsTipoLocation = new RegraAlocacaoClusterLocationsTipoLocation(
                            new RegraAlocacaoClusterLocationsTipoLocation.RegraAlocacaoClusterLocationsTipoLocationCompositeKey(
                                    regraAlocacaoClusterLocations, tipoLocation));
                    regraAlocacaoClusterLocations.addRegraAlocacaoTipoLocation(regraAlocacaoClusterLocationsTipoLocation);
                    break;
                case PAIS_ESTADO:
                    regraAlocacaoClusterLocations.setRegraAlocacaoTipo(Constantes.RegraAlocacaoClusterLocationsTipo.PAIS_ESTADO);
                    String pais = ((RegraAlocaoClusterLocationsPaisEstadoDTO) regraAlocacaoClusterDTO).getPais();
                    String estado = ((RegraAlocaoClusterLocationsPaisEstadoDTO) regraAlocacaoClusterDTO).getEstado();
                    RegraAlocacaoClusterLocationsPaisEstado regraAlocacaoClusterLocationsPaisEstado = new RegraAlocacaoClusterLocationsPaisEstado(
                            new RegraAlocacaoClusterLocationsPaisEstado.RegraAlocacaoClusterLocationsPaisEstadoCompositeKey(
                                    regraAlocacaoClusterLocations, pais, estado));
                    regraAlocacaoClusterLocations.addRegraAlocacaoPaisEstado(regraAlocacaoClusterLocationsPaisEstado);
                    break;
                case CARACTERISTICA:
                    throw new RequiresEnterpriseVersionException("Location characteristic cluster allocation");
            }
        }
    }

    /**
     * Community permite regras de cluster de material por status e por
     * caracteristicas cadastradas no catalogo publico.
     *
     * <p>Mesmo dentro das regras por status, o status NEW e Enterprise porque
     * depende da janela funcional de new materials. O Community preserva o enum
     * por compatibilidade de schema/DTO, mas nao permite criar regras que
     * dependam dessa classificacao.</p>
     *
     */
    void validaRegrasAlocacaoClusterProdutosCommunity(ClusterProdutosDTO clusterProdutosDTO) {

        if (clusterProdutosDTO == null) {
            throw new IllegalArgumentException("Material cluster payload is required");
        }
        for (RegraAlocaoClusterProdutosDTO regraAlocacaoClusterDTO : Optional.ofNullable(clusterProdutosDTO.getRegraAlocacaoClusterDTOList())
                .orElseGet(Collections::emptyList)) {
            if (regraAlocacaoClusterDTO == null) {
                throw new IllegalArgumentException("Material cluster allocation rule cannot be null");
            }
            if (regraAlocacaoClusterDTO.getCriterio() == null) {
                throw new IllegalArgumentException("Material cluster allocation rule criterion is required");
            }
            if (isRegraAlocacaoClusterProdutosStatusNovo(regraAlocacaoClusterDTO)) {
                throw new RequiresEnterpriseVersionException("New material cluster allocation");
            }
            if (Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO.equals(regraAlocacaoClusterDTO.getCriterio())) {
                validaRegraStatusMaterialCommunity(regraAlocacaoClusterDTO);
            }
            if (Constantes.RegraAlocacaoClusterProdutosTipo.CARACTERISTICA.equals(regraAlocacaoClusterDTO.getCriterio())) {
                validaRegraCaracteristicaMaterialCommunity(regraAlocacaoClusterDTO);
            }
        }

    }

    /**
     * Valida o valor de status selecionado na regra de cluster de material.
     *
     * <p>O DTO historico carrega o status dentro de `CaracteristicaProdutoDTO`.
     * A regra Community aceita apenas status explicitos conhecidos; payload
     * parcial deve falhar antes de criar entidade filha ou salvar uma regra que
     * depois quebraria durante a alocacao em memoria.</p>
     */
    private void validaRegraStatusMaterialCommunity(
            RegraAlocaoClusterProdutosDTO regraAlocacaoClusterDTO) {

        CaracteristicaProdutoDTO caracteristicaProdutoDTO =
                regraAlocacaoClusterDTO.getCaracteristicaDTO();
        if (caracteristicaProdutoDTO == null) {
            throw new IllegalArgumentException("Material cluster status allocation value is required");
        }
        if (caracteristicaProdutoDTO.getDescricao() == null
                || caracteristicaProdutoDTO.getDescricao().isBlank()) {
            throw new IllegalArgumentException(
                    "Material cluster status allocation description is required");
        }

    }

    /**
     * Validates the public wire shape before resolving the catalog entity.
     *
     * <p>Every characteristic rule is deliberately limited to one
     * characteristic and one-or-more values. Values are OR alternatives within
     * that characteristic; separate rules remain AND conditions of a cluster.</p>
     */
    private void validaRegraCaracteristicaMaterialCommunity(
            RegraAlocaoClusterProdutosDTO regraAlocacaoClusterDTO) {

        CaracteristicaProdutoDTO caracteristicaProdutoDTO = regraAlocacaoClusterDTO.getCaracteristicaDTO();
        if (caracteristicaProdutoDTO == null || caracteristicaProdutoDTO.getCaracteristicaId() == null
                || caracteristicaProdutoDTO.getCaracteristicaId().isBlank()) {
            throw new IllegalArgumentException("Material cluster characteristic is required");
        }
        List<String> atributoList = Optional.ofNullable(caracteristicaProdutoDTO.getListaAtributos())
                .orElseGet(Collections::emptyList);
        if (atributoList.isEmpty() || atributoList.stream().anyMatch(atributo -> atributo == null || atributo.isBlank())) {
            throw new IllegalArgumentException("At least one material characteristic value is required");
        }
    }

    /** Resolves and validates one characteristic against the preloaded Community catalog. */
    private CaracteristicaProduto getCaracteristicaProdutoCommunity(String caracteristicaId) {

        return caracteristicaMaterialRepository.findAllWithValues().stream()
                .filter(caracteristicaProduto -> caracteristicaId.equals(caracteristicaProduto.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material characteristic not found in the Community catalog: " + caracteristicaId));
    }

    /**
     * Identifica a opcao visual NEW enviada pela tela compartilhada.
     *
     * <p>O DTO historico reaproveita `CaracteristicaProdutoDTO` para carregar o
     * valor selecionado do status. Mantemos a verificacao local e explicita para
     * nao espalhar esse detalhe de tela pelo dominio de clusterizacao.</p>
     */
    private boolean isRegraAlocacaoClusterProdutosStatusNovo(RegraAlocaoClusterProdutosDTO regraAlocacaoClusterDTO) {

        if (!Constantes.RegraAlocacaoClusterProdutosTipo.STATUS_PRODUTO.equals(regraAlocacaoClusterDTO.getCriterio())) {
            return false;
        }
        if (regraAlocacaoClusterDTO.getCaracteristicaDTO() == null
                || regraAlocacaoClusterDTO.getCaracteristicaDTO().getDescricao() == null) {
            return false;
        }
        return "NEW".equalsIgnoreCase(regraAlocacaoClusterDTO.getCaracteristicaDTO().getDescricao().trim());

    }

    /**
     * Community permite clusters de location por pais/estado e tipo de
     * location. Regras por caracteristica pertencem ao Enterprise.
     */
    void validaRegrasAlocacaoClusterLocationsCommunity(ClusterLocationsDTO clusterLocationsDTO) {

        if (clusterLocationsDTO == null) {
            throw new IllegalArgumentException("Location cluster payload is required");
        }
        for (RegraAlocaoClusterLocationsDTO regraAlocacaoClusterDTO : Optional.ofNullable(clusterLocationsDTO.getRegraAlocacaoClusterDTOList())
                .orElseGet(Collections::emptyList)) {
            if (regraAlocacaoClusterDTO == null) {
                throw new IllegalArgumentException("Location cluster allocation rule cannot be null");
            }
            if (regraAlocacaoClusterDTO.getCriterio() == null) {
                throw new IllegalArgumentException("Location cluster allocation rule criterion is required");
            }
            if (Constantes.RegraAlocacaoClusterLocationsTipo.CARACTERISTICA.equals(regraAlocacaoClusterDTO.getCriterio())) {
                throw new RequiresEnterpriseVersionException("Location characteristic cluster allocation");
            }
            validaDetalheRegraClusterLocationsCommunity(regraAlocacaoClusterDTO);
        }

    }

    /**
     * Valida os campos especificos de cada criterio Community de location.
     *
     * <p>O Jackson normalmente materializa o subtipo correto pelo campo
     * `criterio`, mas payloads manuais ou incompletos ainda podem chegar aqui.
     * Validar antes do cast evita `ClassCastException` e impede persistir regra
     * pais/estado ou tipo de location sem valor funcional.</p>
     */
    private void validaDetalheRegraClusterLocationsCommunity(
            RegraAlocaoClusterLocationsDTO regraAlocacaoClusterDTO) {

        switch (regraAlocacaoClusterDTO.getCriterio()) {
            case TIPO_LOCATION:
                if (!(regraAlocacaoClusterDTO instanceof RegraAlocaoClusterLocationsTipoLocationDTO regraTipoLocationDTO)) {
                    throw new IllegalArgumentException(
                            "Location cluster type allocation payload must use Location Type fields");
                }
                if (regraTipoLocationDTO.getLocationType() == null) {
                    throw new IllegalArgumentException("Location cluster type allocation value is required");
                }
                break;
            case PAIS_ESTADO:
                if (!(regraAlocacaoClusterDTO instanceof RegraAlocaoClusterLocationsPaisEstadoDTO regraPaisEstadoDTO)) {
                    throw new IllegalArgumentException(
                            "Location cluster country/state allocation payload must use Country / State fields");
                }
                break;
            case CARACTERISTICA:
                /*
                 * CARACTERISTICA ja foi bloqueado acima como Enterprise. Este
                 * case existe apenas para manter o switch exaustivo e claro.
                 */
                break;
        }

    }

    public ClusterProdutosDTO getClusterProdutosDTO(String id) {

        Long clusterMateriaisId = parseLongIdObrigatorioCommunity(
                id,
                "Material cluster id");

        Optional<ClusterMateriais> optionalClusterProdutosDemandPlanning =
                clusterMateriaisDemandPlanningRepository.findById(clusterMateriaisId);
        /*
         * Cluster inexistente continua retornando null para preservar o
         * contrato historico da tela. Optional nulo do repository, entretanto,
         * indica quebra estrutural e deve falhar antes do mapper.
         */
        if (optionalClusterProdutosDemandPlanning == null) {
            throw new IllegalStateException(
                    "Material cluster repository returned null Optional for Community lookup id "
                            + clusterMateriaisId
                            + ".");
        }
        return optionalClusterProdutosDemandPlanning.map(ClusterProdutosMapper::convertComRegrasAlocacaoDTO).orElse(null);

    }

    public ClusterLocationsDTO getClusterLocationsDTO(String id) {
        Long clusterLocationsId = parseLongIdObrigatorioCommunity(
                id,
                "Location cluster id");

        Optional<ClusterLocations> optionalClusterLocations = clusterLocationsRepository.findById(clusterLocationsId);
        /*
         * Ausencia funcional da location cluster continua retornando null. O
         * repository nunca deve retornar Optional nulo.
         */
        if (optionalClusterLocations == null) {
            throw new IllegalStateException(
                    "Location cluster repository returned null Optional for Community lookup id "
                            + clusterLocationsId
                            + ".");
        }
        return optionalClusterLocations.map(ClusterLocationsMapper::convertComRegrasAlocacaoDTO).orElse(null);
    }

    /**
     * Converte ids de path/query vindos como texto em chaves Long funcionais.
     *
     * <p>Controllers legados ainda recebem alguns ids como `String`. Validar
     * aqui impede que `Long.parseLong(...)` vaze `NumberFormatException` sem
     * contexto ou que uma chamada interna chegue ao repository com chave
     * incompleta.</p>
     */
    private Long parseLongIdObrigatorioCommunity(
            String id,
            String nomeCampo) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(nomeCampo + " is required");
        }

        try {
            return Long.parseLong(id);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(
                    nomeCampo + " must be numeric: " + id,
                    numberFormatException);
        }

    }

    public List<ClusterProdutosDTO> getListaTodosClusterProdutosDTOExcetoPadrao() {
        List<ClusterProdutosDTO> clusterRuleDTOList = new ArrayList<>();
        List<ClusterMateriais> clusterMateriaisList =
                clusterMateriaisDemandPlanningRepository
                        .customFindAllByPadraoIsFalseComRegrasAlocacaoEStatusProduto();
        validaClusterMateriaisListCarregadaCommunity(
                clusterMateriaisList,
                "non-default material cluster listing");

        clusterMateriaisList.stream()
                .map(ClusterProdutosMapper::convertComRegrasAlocacaoDTO).forEach(clusterRuleDTOList::add);
        return clusterRuleDTOList;
    }

    public List<ClusterLocationsDTO> getListaTodosClusterLocationsDTOExcetoPadrao() {
        List<ClusterLocations> clusterLocationsList = clusterLocationsRepository.customFindAll();
        validaClusterLocationsListCarregadaCommunity(
                clusterLocationsList,
                "non-default location cluster listing");

        return clusterLocationsList.stream()
                .filter(clusterLocations -> !clusterLocations.getPadrao())
                .map(ClusterLocationsMapper::convertComRegrasAlocacaoDTO)
                .collect(Collectors.toList());
    }

    public List<ClusterProdutosDTO> getListaClusterMateriaisDTO() {
        List<ClusterProdutosDTO> clusterRuleDTOList = new ArrayList<>();
        List<ClusterMateriais> clusterMateriaisList =
                clusterMateriaisDemandPlanningRepository
                        .customFindAllComRegrasAlocacaoEStatusProduto();
        validaClusterMateriaisListCarregadaCommunity(
                clusterMateriaisList,
                "material cluster listing");
        clusterMateriaisList.stream()
                .map(ClusterProdutosMapper::convertComRegrasAlocacaoDTO).forEach(clusterRuleDTOList::add);
        return clusterRuleDTOList;
    }

    /**
     * Valida o snapshot de clusters de materiais antes dos mappers.
     *
     * <p>Lista vazia e valida. Cada cluster precisa de id, flag padrao e set
     * de regras materializado, pois os mappers Community usam esses campos para
     * montar seletores e configuracoes de cluster sem acessar capacidades
     * Enterprise de caracteristicas dinamicas.</p>
     */
    private void validaClusterMateriaisListCarregadaCommunity(
            List<ClusterMateriais> clusterMateriaisList,
            String contextoListagem) {

        if (clusterMateriaisList == null) {
            throw new IllegalStateException("Material cluster snapshot is required for " + contextoListagem + ".");
        }

        for (int index = 0; index < clusterMateriaisList.size(); index++) {
            ClusterMateriais clusterMateriais = clusterMateriaisList.get(index);

            if (clusterMateriais == null) {
                throw new IllegalStateException(
                        "Material cluster at index " + index + " is required for " + contextoListagem + ".");
            }
            if (clusterMateriais.getId() == null) {
                throw new IllegalStateException(
                        "Material cluster at index " + index + " has no id for " + contextoListagem + ".");
            }
            if (clusterMateriais.getPadrao() == null) {
                throw new IllegalStateException(
                        "Material cluster at index " + index + " has no default flag for " + contextoListagem + ".");
            }
            if (clusterMateriais.getRegrasAlocacaoClusterProdutos() == null) {
                throw new IllegalStateException(
                        "Material cluster at index " + index + " has no allocation rules snapshot for " + contextoListagem + ".");
            }
        }

    }

    /**
     * Valida o snapshot de clusters de locations antes dos mappers.
     */
    private void validaClusterLocationsListCarregadaCommunity(
            Collection<ClusterLocations> clusterLocationsList,
            String contextoListagem) {

        if (clusterLocationsList == null) {
            throw new IllegalStateException("Location cluster snapshot is required for " + contextoListagem + ".");
        }

        int index = 0;
        for (ClusterLocations clusterLocations : clusterLocationsList) {
            if (clusterLocations == null) {
                throw new IllegalStateException(
                        "Location cluster at index " + index + " is required for " + contextoListagem + ".");
            }
            if (clusterLocations.getId() == null) {
                throw new IllegalStateException(
                        "Location cluster at index " + index + " has no id for " + contextoListagem + ".");
            }
            if (clusterLocations.getPadrao() == null) {
                throw new IllegalStateException(
                        "Location cluster at index " + index + " has no default flag for " + contextoListagem + ".");
            }
            if (clusterLocations.getRegrasAlocacaoClusterLocations() == null) {
                throw new IllegalStateException(
                        "Location cluster at index " + index + " has no allocation rules snapshot for " + contextoListagem + ".");
            }
            index++;
        }

    }

    /**
     * Remove uma regra de alocacao de cluster de materiais e invalida o snapshot de parametros.
     *
     * <p>A exclusao altera o conjunto de regras usado pela
     * `ClusterEParametrosProjection`, por isso a entrada compartilhada de cache
     * precisa ser descartada junto com a remocao persistida.</p>
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    public void deleteRegraAlocacaoClusterProdutosBy(Long id) {

        regraAlocacaoClusterProdutosRepository.deleteById(id);

    }

    /**
     * Remove uma regra de alocacao de cluster de locations e invalida o snapshot de parametros.
     *
     * <p>A exclusao muda a regra funcional usada para classificar locations no
     * snapshot de clusters e parametros. O cache e limpo para impedir reuso da
     * projection anterior ao delete.</p>
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    public void deleteRegraAlocacaoClusterLocationsBy(Long id) {

        regraAlocacaoClusterLocationsRepository.deleteById(id);

    }

    /**
     * Remove do agregado do cluster de materiais todas as regras persistidas que nao vieram no payload editado.
     * A UI compartilhada remove a linha localmente antes do save; sem esta sincronizacao, o JPA mantem as
     * regras antigas no agregado e a exclusao nunca chega ao banco.
     */
    private void removeRegrasAlocacaoClusterProdutosAusentes(ClusterProdutos clusterProdutos, ClusterProdutosDTO clusterProdutosDTO) {
        List<RegraAlocaoClusterProdutosDTO> regraAlocacaoClusterDTOList = Optional.ofNullable(clusterProdutosDTO.getRegraAlocacaoClusterDTOList())
                .orElseGet(Collections::emptyList);

        Set<Long> regraAlocacaoMantidaIdSet = regraAlocacaoClusterDTOList.stream()
                .map(RegraAlocaoClusterProdutosDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Regras sem id no DTO ainda nao foram persistidas; so removemos orfaos que ja existem no banco.
        clusterProdutos.getRegrasAlocacaoClusterProdutos().removeIf(regraAlocacaoClusterProdutos ->
                regraAlocacaoClusterProdutos.getId() != null
                        && !regraAlocacaoMantidaIdSet.contains(regraAlocacaoClusterProdutos.getId()));
    }

    /**
     * Aplica a mesma sincronizacao de orfaos para clusters de location.
     * Isso garante que a ausencia da regra no payload represente remocao explicita no cadastro.
     */
    private void removeRegrasAlocacaoClusterLocationsAusentes(ClusterLocations clusterLocations, ClusterLocationsDTO clusterLocationsDTO) {
        List<RegraAlocaoClusterLocationsDTO> regraAlocacaoClusterDTOList = Optional.ofNullable(clusterLocationsDTO.getRegraAlocacaoClusterDTOList())
                .orElseGet(Collections::emptyList);

        Set<Long> regraAlocacaoMantidaIdSet = regraAlocacaoClusterDTOList.stream()
                .map(RegraAlocaoClusterLocationsDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // A colecao da entidade e dona da remocao via orphanRemoval, entao a exclusao precisa acontecer aqui.
        clusterLocations.getRegrasAlocacaoClusterLocations().removeIf(regraAlocacaoClusterLocations ->
                regraAlocacaoClusterLocations.getId() != null
                        && !regraAlocacaoMantidaIdSet.contains(regraAlocacaoClusterLocations.getId()));
    }

    /**
     * Salva a definicao unica de cluster de materiais e invalida a projection de parametros.
     *
     * <p>O metodo sincroniza descricao, prioridade e regras filhas recebidas do
     * DTO compartilhado. Como esse cadastro alimenta diretamente
     * `ClusterEParametrosProjection`, qualquer save precisa limpar o cache
     * compartilhado antes do proximo calculo/listagem.</p>
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    public void saveClusterProdutosDTO(ClusterProdutosDTO clusterProdutosDTO) {

        validaPayloadClusterMateriaisCommunity(clusterProdutosDTO);

        ClusterMateriais clusterMateriais;
        if (clusterProdutosDTO.getId() == null) {
            clusterProdutosDTO.setId(-1L); // para não gerar erro na linha abaixo ao jogar null
        }
        Optional<ClusterMateriais> optionalClusterMateriais =
                clusterMateriaisDemandPlanningRepository.findById(clusterProdutosDTO.getId());

        /*
         * Cluster ausente representa criacao funcional de novo cluster.
         * Optional nulo deve falhar antes do save inicial, para nao esconder
         * repository quebrado como criacao normal.
         */
        if (optionalClusterMateriais == null) {
            throw new IllegalStateException(
                    "Material cluster repository returned null Optional for Community save id "
                            + clusterProdutosDTO.getId()
                            + ".");
        }

        clusterMateriais = optionalClusterMateriais
                .orElseGet(() -> {
                    /*
                     * Id nulo/nao encontrado representa criacao de cluster.
                     * O save inicial materializa o id antes de sincronizar
                     * regras filhas e orphanRemoval.
                     */
                    ClusterMateriais novoClusterMateriais =
                            new ClusterMateriais(null, false, null);
                    ClusterMateriais clusterMateriaisSalvo =
                            clusterMateriaisDemandPlanningRepository.save(novoClusterMateriais);
                    validaClusterMateriaisSalvoCommunity(clusterMateriaisSalvo);
                    return clusterMateriaisSalvo;
                });
        mapClusterProdutos(clusterMateriais, clusterProdutosDTO);
        clusterMateriais.setPrioridade(clusterProdutosDTO.getPriority());
        clusterMateriais.setDescricao(clusterProdutosDTO.getDescription());
        removeRegrasAlocacaoClusterProdutosAusentes(clusterMateriais, clusterProdutosDTO);
        alocaRegrasAoClusterProdutos(clusterProdutosDTO, clusterMateriais);
        ClusterMateriais clusterMateriaisSalvo =
                clusterMateriaisDemandPlanningRepository.save(clusterMateriais);
        validaClusterMateriaisSalvoCommunity(clusterMateriaisSalvo);

    }

    /**
     * Salva o cluster Community de locations e invalida a projection de parametros.
     *
     * <p>A alteracao de cluster, prioridade ou regras filhas altera a forma
     * como locations sao classificadas nas projections compartilhadas. A
     * invalidacao garante que o proximo consumidor reconstrua o snapshot.</p>
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    public void saveClusterLocationsDTO(ClusterLocationsDTO clusterLocationsDTO) {

        validaPayloadClusterLocationsCommunity(clusterLocationsDTO);
        ClusterLocations clusterLocations;
        if (clusterLocationsDTO.getId() == null) clusterLocationsDTO.setId(-1L); // para não gerar erro na linha abaixo ao jogar null
        Optional<ClusterLocations> optionalClusterLocations =
                clusterLocationsRepository.findById(clusterLocationsDTO.getId());

        /*
         * Cluster de location ausente cria novo cadastro. Optional nulo indica
         * repository quebrado e deve falhar antes do save inicial.
         */
        if (optionalClusterLocations == null) {
            throw new IllegalStateException(
                    "Location cluster repository returned null Optional for Community save id "
                            + clusterLocationsDTO.getId()
                            + ".");
        }

        clusterLocations = optionalClusterLocations
                .orElseGet(() -> {
                    /*
                     * Id nulo/nao encontrado representa criacao de cluster de
                     * locations. O cluster Community nasce nao-padrao porque
                     * clusters padrao pertencem ao bootstrap/configuracao base.
                     */
                    ClusterLocations novoClusterLocations = new ClusterLocations();
                    novoClusterLocations.setPadrao(false);
                    ClusterLocations clusterLocationsSalvo =
                            clusterLocationsRepository.save(novoClusterLocations);
                    validaClusterLocationsSalvoCommunity(clusterLocationsSalvo);
                    return clusterLocationsSalvo;
                });
        mapClusterLocations(clusterLocations, clusterLocationsDTO);
        clusterLocations.setPrioridade(clusterLocationsDTO.getPriority());
        clusterLocations.setDescricao(clusterLocationsDTO.getDescription());
        removeRegrasAlocacaoClusterLocationsAusentes(clusterLocations, clusterLocationsDTO);
        alocaRegrasAoClusterLocations(clusterLocationsDTO, clusterLocations);
        ClusterLocations clusterLocationsSalvo = clusterLocationsRepository.save(clusterLocations);
        validaClusterLocationsSalvoCommunity(clusterLocationsSalvo);
    }

    /**
     * Remove um cluster Community de materiais DP com suas regras e invalida a projection de parametros.
     *
     * <p>A operacao e transacional porque remove primeiro as regras de
     * alocacao e depois o cluster. O cache de parametros e limpo para que a
     * proxima projection nao enxergue a classificacao removida.</p>
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    @Transactional
    public void deleteClusterProdutos(ClusterRuleDTO clusterRuleDTO) {

        validaClusterRuleDTOParaDeleteCommunity(
                clusterRuleDTO,
                "Material cluster delete payload id is required");
        // Remove regras primeiro porque o endpoint apaga somente clusters de materiais DP validados pela borda.
        regraAlocacaoClusterProdutosRepository.deleteAllByClusterProdutosId(clusterRuleDTO.getId());
        clusterProdutosRepository.deleteById(clusterRuleDTO.getId());
    }

    /**
     * Remove um cluster Community de locations com suas regras e invalida a projection de parametros.
     *
     * <p>A operacao preserva a ordem explicita de exclusao das regras antes do
     * cluster e descarta o cache compartilhado de parametros para reconstruir o
     * snapshot sem o cadastro removido.</p>
     */
    @CacheEvict(value = "clusterEParametrosProjection", allEntries = true)
    @Transactional
    public void deleteClusterLocations(ClusterRuleDTO clusterRuleDTO) {

        validaClusterRuleDTOParaDeleteCommunity(
                clusterRuleDTO,
                "Location cluster delete payload id is required");
        // Remove regras primeiro para manter a exclusao do cluster de location explicita.
        regraAlocacaoClusterLocationsRepository.deleteAllByClusterLocationsId(clusterRuleDTO.getId());
        clusterLocationsRepository.deleteById(clusterRuleDTO.getId());
    }

    public void mapClusterProdutos(ClusterProdutos clusterProdutos, ClusterProdutosDTO clusterProdutosDTO) {
        clusterProdutos.setDescricao(clusterProdutosDTO.getDescription());
        clusterProdutos.setPrioridade(clusterProdutosDTO.getPriority());
    }

    public void mapClusterLocations(ClusterLocations clusterLocations, ClusterLocationsDTO clusterLocationsDTO) {
        clusterLocations.setDescricao(clusterLocationsDTO.getDescription());
        clusterLocations.setPrioridade(clusterLocationsDTO.getPriority());
    }

    /**
     * Valida a fotografia salva do cluster de materiais.
     *
     * <p>O id do cluster e usado logo depois para regras de alocacao e
     * parametros nivel cluster. Um repository que devolve snapshot nulo ou sem
     * id nao pode ser tratado como sucesso de configuracao Community.</p>
     */
    private void validaClusterMateriaisSalvoCommunity(
            ClusterMateriais clusterMateriaisSalvo) {

        if (clusterMateriaisSalvo == null) {
            throw new IllegalStateException("Saved material cluster snapshot is required.");
        }
        if (clusterMateriaisSalvo.getId() == null) {
            throw new IllegalStateException("Saved material cluster id is required.");
        }

    }

    /**
     * Valida a fotografia salva do cluster de locations.
     *
     * <p>Clusters de location alimentam `ClusterEParametrosProjection` e
     * filtros de Planning Book. A borda front precisa falhar antes de devolver
     * sucesso quando o repository nao confirma uma chave persistida.</p>
     */
    private void validaClusterLocationsSalvoCommunity(
            ClusterLocations clusterLocationsSalvo) {

        if (clusterLocationsSalvo == null) {
            throw new IllegalStateException("Saved location cluster snapshot is required.");
        }
        if (clusterLocationsSalvo.getId() == null) {
            throw new IllegalStateException("Saved location cluster id is required.");
        }

    }

    /**
     * Valida a regra de alocacao de material retornada pelo repository.
     *
     * <p>O save da regra acontece antes dos filhos por status justamente para
     * materializar o id usado pelas chaves compostas filhas. Por isso a regra
     * salva precisa carregar id proprio e cluster material com id.</p>
     */
    private void validaRegraAlocacaoClusterProdutosSalvaCommunity(
            RegraAlocacaoClusterProdutos regraAlocacaoClusterProdutosSalva) {

        if (regraAlocacaoClusterProdutosSalva == null) {
            throw new IllegalStateException("Saved material cluster allocation rule snapshot is required.");
        }
        if (regraAlocacaoClusterProdutosSalva.getId() == null) {
            throw new IllegalStateException("Saved material cluster allocation rule id is required.");
        }
        if (regraAlocacaoClusterProdutosSalva.getClusterProdutos() == null
                || regraAlocacaoClusterProdutosSalva.getClusterProdutos().getId() == null) {
            throw new IllegalStateException(
                    "Saved material cluster allocation rule material cluster id is required.");
        }

    }

    /**
     * Valida a regra de alocacao de location retornada pelo repository.
     *
     * <p>A mesma regra de chave vale para filhos por tipo de location e
     * pais/estado: sem id da regra e sem id do cluster, a configuracao salva
     * nao forma snapshot consistente.</p>
     */
    private void validaRegraAlocacaoClusterLocationsSalvaCommunity(
            RegraAlocacaoClusterLocations regraAlocacaoClusterLocationsSalva) {

        if (regraAlocacaoClusterLocationsSalva == null) {
            throw new IllegalStateException("Saved location cluster allocation rule snapshot is required.");
        }
        if (regraAlocacaoClusterLocationsSalva.getId() == null) {
            throw new IllegalStateException("Saved location cluster allocation rule id is required.");
        }
        if (regraAlocacaoClusterLocationsSalva.getClusterLocations() == null
                || regraAlocacaoClusterLocationsSalva.getClusterLocations().getId() == null) {
            throw new IllegalStateException(
                    "Saved location cluster allocation rule location cluster id is required.");
        }

    }

    /**
     * Valida o payload minimo de save de cluster de materiais.
     *
     * <p>Id continua opcional porque ausencia de id representa criacao. O
     * contrato nao recebe processo: ha uma unica definicao de clusters de
     * materiais.</p>
     */
    private void validaPayloadClusterMateriaisCommunity(
            ClusterProdutosDTO clusterProdutosDTO) {

        if (clusterProdutosDTO == null) {
            throw new IllegalArgumentException("Material cluster payload is required");
        }
        validaRegrasAlocacaoClusterProdutosCommunity(clusterProdutosDTO);

    }

    /**
     * Valida o payload minimo de save de cluster de locations.
     *
     * <p>Id nulo permanece criacao de cluster nao-padrao. As regras filhas sao
     * validadas separadamente para bloquear caracteristicas Enterprise e campos
     * incompletos antes da sincronizacao com o agregado JPA.</p>
     */
    private void validaPayloadClusterLocationsCommunity(
            ClusterLocationsDTO clusterLocationsDTO) {

        if (clusterLocationsDTO == null) {
            throw new IllegalArgumentException("Location cluster payload is required");
        }
        validaRegrasAlocacaoClusterLocationsCommunity(clusterLocationsDTO);

    }

    /**
     * Valida o envelope historico usado pelos endpoints de delete de cluster.
     */
    private void validaClusterRuleDTOParaDeleteCommunity(
            ClusterRuleDTO clusterRuleDTO,
            String mensagemIdObrigatorio) {

        if (clusterRuleDTO == null) {
            throw new IllegalArgumentException("Cluster delete payload is required");
        }
        if (clusterRuleDTO.getId() == null) {
            throw new IllegalArgumentException(mensagemIdObrigatorio);
        }

    }

    /**
     * Valida ids de regras removidas diretamente pela tela de clusterizacao.
     */
    /**
     * Falha para textos obrigatorios nulos ou em branco.
     */
    public List<DFUDTO> getDFUsDeClusterProdutosEmClusterLocations(Long clusterProdutosId, Long clusterLocationsId, LocalDateTime dataReferencia) {
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        ClusterLocations clusterLocations = clusterEParametrosProjection.getClusterLocationsDeId(clusterLocationsId);
        ClusterProdutos clusterProdutos = clusterEParametrosProjection.getClusterProdutosDeId(clusterProdutosId);

        List<DFUDTO> listaDFUDTOs = new ArrayList<>();

        for (Location location : clusterEParametrosProjection.getLocationsAtivasDeClusterLocations(clusterLocations)) {
            for (Produto produto : clusterEParametrosProjection.getMateriaisDeClusterProdutosAtivosNaLocation(clusterProdutos, location)) {
                DFUDTO dfuDTO = DFUDTO.builder()
                        .materialId(produto.getId())
                        .locationId(location.getId())
                        .build();
                listaDFUDTOs.add(dfuDTO);
            }
        }
        return listaDFUDTOs;
    }

    /**
     * Retorna a alocacao de materiais para os clusters de Demand Planning.
     * A alocacao e obtida pela mesma logica usada no projection em memoria (regras + prioridades).
     *
     * <p>No Community a tela compartilhada de alocacao de materiais nao deve expor
     * clusters de Pricing. A entidade transicional ainda pode existir no modelo
     * durante a migracao, mas a superficie funcional aberta permanece restrita a
     * Demand Planning.</p>
     */
    public List<AlocacaoClusterMaterialDTO> getAlocacaoMateriaisEmClusters() {
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        Comparator<AlocacaoClusterMaterialDTO> comparadorAlocacao = Comparator
                .comparing(AlocacaoClusterMaterialDTO::getClusterId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(AlocacaoClusterMaterialDTO::getMaterialId, Comparator.nullsLast(String::compareTo));

        List<AlocacaoClusterMaterialDTO> alocacaoDemandPlanning = clusterEParametrosProjection.getMateriaisAtivos().stream()
                .map(material -> {
                    ClusterMateriais clusterMateriais = clusterEParametrosProjection.getClusterProdutosDemandPlanning(material);
                    return AlocacaoClusterMaterialDTO.builder()
                            .clusterId(clusterMateriais.getId())
                            .clusterDescription(clusterMateriais.getDescricao())
                            .materialId(material.getId())
                            .materialDescription(material.getDescricao())
                            .build();
                })
                .sorted(comparadorAlocacao)
                .collect(Collectors.toList());

        List<AlocacaoClusterMaterialDTO> alocacaoTotal = new ArrayList<>(alocacaoDemandPlanning.size());
        alocacaoTotal.addAll(alocacaoDemandPlanning);
        return alocacaoTotal;
    }

    /**
     * Retorna a alocacao de locations ativas em clusters de location.
     * A alocacao segue regras e prioridades do projection em memoria.
     */
    public List<AlocacaoClusterLocationDTO> getAlocacaoLocationsEmClusters() {
        ClusterEParametrosProjection clusterEParametrosProjection = clusterEParametrosProjectionFactory.getParametrosProjectionCompletoDeCache();

        return clusterEParametrosProjection.getLocationsAtivas().stream()
                .map(location -> {
                    ClusterLocations clusterLocations = clusterEParametrosProjection.getClusterLocationsDeLocation(location);
                    return AlocacaoClusterLocationDTO.builder()
                            .clusterId(clusterLocations.getId())
                            .clusterDescription(clusterLocations.getDescricao())
                            .locationId(location.getId())
                            .locationDescription(location.getDescricao())
                            .build();
                })
                .sorted(Comparator
                        .comparing(AlocacaoClusterLocationDTO::getClusterId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(AlocacaoClusterLocationDTO::getLocationId, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }
}
