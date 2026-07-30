package com.opsfactor.community.capability.configuration.facade;

import com.opsfactor.community.capability.cluster.domain.location.ClusterLocations;
import com.opsfactor.community.capability.configuration.domain.ParametrosProdutoLocation;
import com.opsfactor.community.capability.configuration.domain.cluster.location.ParametrosClusterLocations;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.cluster.repository.location.ClusterLocationsRepository;
import com.opsfactor.community.capability.configuration.repository.cluster.location.ParametrosClusterLocationsRepository;
import com.opsfactor.community.capability.configuration.repository.ParametrosProdutoLocationRepository;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ParametroClusterLocationDTO;
import com.opsfactor.community.capability.configuration.facade.dto.ParametrosMaterialLocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.facade.dto.LocationDTO;
import com.opsfactor.community.capability.masterdata.network.location.service.LocationService;
import com.opsfactor.community.capability.masterdata.product.material.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fachada Community de parametros simples por cluster, material e
 * material/location.
 *
 * <p>O service preserva a entidade fisica historica `Produto`, mas a borda
 * publica nova usa material. Pricing, novos materiais, caracteristicas
 * dinamicas e parametros Enterprise permanecem bloqueados ou neutros.</p>
 */
@Slf4j
@Service
public class ParametrosFacade {

    /**
     * Repository dos parametros associados a clusters de locations.
     */
    @Autowired
    private ParametrosClusterLocationsRepository parametrosClusterLocationsRepository;

    /**
     * Repository dos clusters de locations usados nas telas de parametros.
     */
    @Autowired
    private ClusterLocationsRepository clusterLocationsRepository;

    /**
     * Repository de materiais da entidade fisica `Produto`.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Repository dos parametros operacionais por material/location.
     */
    @Autowired
    private ParametrosProdutoLocationRepository parametrosProdutoLocationRepository;

    /**
     * Repository de unidades usado somente para resolver, em uma leitura em
     * lote, os identificadores informados no payload administrativo.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Service de locations usado para carregar recortes operacionais sem
     * duplicar regra de master data.
     */
    @Autowired
    private LocationService locationService;

    /**
     * Service de materiais usado para resolver a entidade fisica a partir do
     * identificador publico `materialID`.
     */
    @Autowired
    private MaterialService materialService;
    @Transactional
    public List<ParametroClusterLocationDTO> getParametroClusterLocationDTO() {
        List<ParametroClusterLocationDTO> parametroClusterLocationDTOList = new ArrayList<>();
        try {
            /*
             * A montagem do DTO atravessa os parametros one-to-one do
             * cluster. A consulta administrativa precisa trazer essa
             * associacao na mesma fotografia para nao disparar uma consulta
             * adicional por cluster.
             */
            for (ClusterLocations clusterLocations : clusterLocationsRepository.customFindAll()) {
                ParametrosClusterLocations parametrosClusterLocations = clusterLocations.getParametrosClusterLocations();
                ParametroClusterLocationDTO dto = new ParametroClusterLocationDTO();
                dto.setId(parametrosClusterLocations.getClusterLocations().getId());
                dto.setClusterLocations(clusterLocations.getDescricao());
                dto.setClusterLocationsID(clusterLocations.getId());
                dto.setPlanejaDP(parametrosClusterLocations.getPlanejaDP());
                dto.setPlanejaPricing(false);
                parametroClusterLocationDTOList.add(dto);
            }
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao carregar parametros de cluster/location Community", runtimeException);
        }
        return parametroClusterLocationDTOList;
    }

    public boolean saveParametroClusterLocationDTO(ParametroClusterLocationDTO parametroClusterLocationDTO) {

        validaParametrosClusterLocationCommunity(parametroClusterLocationDTO);

        try {
            /*
             * `Optional.empty()` representa cluster inexistente e mantem o
             * retorno publico `false`. `Optional` nulo e falha de contrato do
             * repository e deve ficar explicito antes de acessar o mapper de
             * parametros ou qualquer snapshot salvo.
             */
            Optional<ClusterLocations> optionalClusterLocations = clusterLocationsRepository
                    .findById(parametroClusterLocationDTO.getClusterLocationsID());
            if (optionalClusterLocations == null) {
                throw new IllegalStateException(
                        "Cluster/location parameter repository returned null Optional for Community cluster id "
                                + parametroClusterLocationDTO.getClusterLocationsID() + ".");
            }
            ClusterLocations clusterLocations = optionalClusterLocations.orElseThrow(() -> new IllegalStateException(
                    "Cluster Location " + parametroClusterLocationDTO.getClusterLocationsID()
                            + " not found for Community cluster/location parameters"));
            if (clusterLocations.getParametrosClusterLocations() != null) {
                ParametrosClusterLocations parametro = clusterLocations.getParametrosClusterLocations();
                parametro.setPlanejaDP(parametroClusterLocationDTO.getPlanejaDP());
                /*
                 * O cluster/location parametriza a inclusao do cluster na
                 * execucao de Demand Planning. A tela trabalha com retorno
                 * booleano, mas o repository nao pode devolver uma fotografia
                 * nula ou sem cluster e ainda assim ser tratado como sucesso.
                 */
                ParametrosClusterLocations parametrosClusterLocationsSalvos =
                        parametrosClusterLocationsRepository.save(parametro);
                validaParametrosClusterLocationsSalvosCommunity(parametrosClusterLocationsSalvos);
            } else {
                throw new IllegalStateException("Not Found a parameter");
            }
            return true;
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao salvar parametros de cluster/location Community {}", parametroClusterLocationDTO.getClusterLocationsID(), runtimeException);
            return false;
        }
    }

    /**
     * Pricing e configuracoes derivadas de cluster/location para precificacao sao
     * Enterprise. O DTO ainda aceita o campo para manter compatibilidade com o front
     * compartilhado, mas o Community nunca persiste ou habilita esse parametro.
     */
    private void validaParametrosClusterLocationCommunity(ParametroClusterLocationDTO parametroClusterLocationDTO) {

        if (parametroClusterLocationDTO == null) {
            throw new IllegalArgumentException("Cluster/location parameter payload is required.");
        }
        if (Boolean.TRUE.equals(parametroClusterLocationDTO.getPlanejaPricing())) {
            throw new RequiresEnterpriseVersionException("Pricing cluster/location parameters");
        }
        if (parametroClusterLocationDTO.getClusterLocationsID() == null) {
            throw new IllegalArgumentException("Cluster/location parameter cluster id is required.");
        }

    }

    /**
     * Valida a fotografia salva dos parametros por cluster de location.
     *
     * <p>O id do cluster e a menor chave funcional usada pelas telas e por
     * `ClusterEParametrosProjection`. Snapshot salvo nulo ou sem cluster
     * indica persistencia quebrada e deve preservar o contrato publico
     * `false`, nao sucesso silencioso.</p>
     */
    private void validaParametrosClusterLocationsSalvosCommunity(
            ParametrosClusterLocations parametrosClusterLocationsSalvos) {

        if (parametrosClusterLocationsSalvos == null) {
            throw new IllegalStateException("Saved cluster/location parameter snapshot is required.");
        }
        if (parametrosClusterLocationsSalvos.getParametrosClusterLocationsCompositeKey() == null
                || parametrosClusterLocationsSalvos.getClusterLocations() == null
                || parametrosClusterLocationsSalvos.getClusterLocations().getId() == null) {
            throw new IllegalStateException("Saved cluster/location parameter cluster id is required.");
        }

    }
    
    /**
     * Lista os parametros operacionais de material expostos na configuracao
     * Community. A entidade fisica ainda e `Produto`, mas o DTO publico novo
     * usa a nomenclatura material.
     */
    public List<ParametrosMaterialDTO> getParametrosMaterialDTO() {

        List<ParametrosMaterialDTO> parametrosMaterialDTOList = new ArrayList<>();
        try {
            for (Produto produto : produtoRepository.findAll()) {
                ParametrosMaterialDTO dto = new ParametrosMaterialDTO();
                dto.setId(produto.getId());
                dto.setDescricao(produto.getDescricao());
                dto.setAtivo(produto.getAtivo());
                parametrosMaterialDTOList.add(dto);
            }
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao carregar parametros de materiais Community", runtimeException);
        }
        return parametrosMaterialDTOList;

    }

    /**
     * Persiste apenas campos de material permitidos no Community. Campos
     * transicionais como novo/fora de linha seguem bloqueados ou neutros em
     * bordas especificas da edicao aberta.
     */
    public Boolean saveParametrosMaterialDTO(ParametrosMaterialDTO parametrosMaterialDTO) {

        validaParametrosMaterialDTOCommunity(parametrosMaterialDTO);

        try{
            /*
             * Material inexistente e um erro funcional esperado da tela e
             * retorna `false`. Optional nulo denuncia repository quebrado e
             * tambem deve falhar de forma nomeada antes de alterar a entidade.
             */
            Optional<Produto> optionalProduto = produtoRepository.findById(parametrosMaterialDTO.getId());
            if (optionalProduto == null) {
                throw new IllegalStateException(
                        "Material parameter repository returned null Optional for Community material id "
                                + parametrosMaterialDTO.getId() + ".");
            }
            Produto produto = optionalProduto.orElseThrow(() -> new IllegalStateException(
                    "Material " + parametrosMaterialDTO.getId()
                            + " not found for Community material parameters"));
            produto.setDescricao(parametrosMaterialDTO.getDescricao());
            produto.setAtivo(parametrosMaterialDTO.getAtivo());
            /*
             * O endpoint historicamente retorna booleano, mas nao pode tratar
             * repository quebrado como sucesso. A validacao do snapshot salvo
             * falha dentro deste bloco e preserva o contrato publico `false`
             * para a tela.
             */
            Produto produtoSalvo = produtoRepository.save(produto);
            validaMaterialSalvoCommunity(produtoSalvo);
            return true;
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao salvar parametros do material Community {}", parametrosMaterialDTO.getId(), runtimeException);
            return false;
        }

    }

    /**
     * Valida a chave minima do payload de parametros de material.
     *
     * <p>Descricao e flag de ativo seguem a semantica historica da tela e podem
     * ser saneadas/persistidas pelas entidades. O identificador do material,
     * porem, e obrigatorio para que a service nao consulte repository com id
     * nulo nem tente logar DTO inexistente no bloco de erro.</p>
     */
    private void validaParametrosMaterialDTOCommunity(ParametrosMaterialDTO parametrosMaterialDTO) {

        if (parametrosMaterialDTO == null) {
            throw new IllegalArgumentException("Material parameter payload is required.");
        }
        if (isBlank(parametrosMaterialDTO.getId())) {
            throw new IllegalArgumentException("Material parameter material id is required.");
        }

    }

    /**
     * Valida a fotografia salva dos parametros basicos de material.
     *
     * <p>O Community usa material ativo/inativo e descricao em seletores e
     * projections. Retorno nulo ou sem id do repository indica persistencia
     * quebrada e deve fazer o fluxo administrativo retornar `false`, nao
     * sucesso silencioso.</p>
     */
    private void validaMaterialSalvoCommunity(Produto produtoSalvo) {

        if (produtoSalvo == null) {
            throw new IllegalStateException("Saved material parameter snapshot is required.");
        }
        if (isBlank(produtoSalvo.getId())) {
            throw new IllegalStateException("Saved material parameter material id is required.");
        }

    }

    /**
     * Lista parametros material/location para todas as locations.
     *
     * <p>A lista de materiais e carregada uma unica vez porque o helper percorre
     * todas as locations; repetir `findAll()` dentro do loop multiplicaria
     * round-trips ao banco sem alterar o resultado.</p>
     */
    public List<ParametrosMaterialLocationDTO> getParametrosMaterialLocation() {

        List<ParametrosMaterialLocationDTO> parametrosMaterialLocationDTOList = new ArrayList<>();
        try {
            List<Produto> materialList = produtoRepository.findAll();
            Map<String, Map<String, ParametrosProdutoLocation>> parametrosPorLocationEMaterial =
                    indexaParametrosProdutoLocationPorLocationEMaterial();
            for (Location location : locationService.findAll()) {
                montaListaParametrosMaterialLocationDTO(
                        parametrosMaterialLocationDTOList,
                        location,
                        materialList,
                        parametrosPorLocationEMaterial);
            }
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao carregar parametros material/location Community", runtimeException);
        }
        return parametrosMaterialLocationDTOList;

    }

    /**
     * Lista parametros material/location para uma unica location selecionada.
     * O carregamento dos materiais tambem fica fora do helper para manter uma
     * unica consulta por request.
     */
    public List<ParametrosMaterialLocationDTO> getParametrosMaterialLocation(String locationId) {

        if (isBlank(locationId)) {
            throw new IllegalArgumentException("Material/location parameter location id is required.");
        }

        List<ParametrosMaterialLocationDTO> parametrosMaterialLocationDTOList = new ArrayList<>();
        try {
            Location location = locationService.getLocation(locationId);
            List<Produto> materialList = produtoRepository.findAll();
            montaListaParametrosMaterialLocationDTO(
                    parametrosMaterialLocationDTOList,
                    location,
                    materialList,
                    indexaParametrosProdutoLocationPorLocationEMaterial());
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao carregar parametros material/location Community para location {}", locationId, runtimeException);
        }
        return parametrosMaterialLocationDTOList;

    }

    /**
     * Materializa uma linha DTO por combinacao material/location.
     *
     * <p>O helper recebe materiais e parâmetros já carregados pelos métodos
     * públicos. Em particular, ele nunca acessa o mapa LAZY da location por
     * par material/location: a API administrativa precisa manter um número
     * constante de consultas, e não uma consulta adicional por linha da tela.</p>
     */
    private void montaListaParametrosMaterialLocationDTO(
            List<ParametrosMaterialLocationDTO> parametrosMaterialLocationDTOList,
            Location location,
            List<Produto> materialList,
            Map<String, Map<String, ParametrosProdutoLocation>> parametrosPorLocationEMaterial) {

        Map<String, ParametrosProdutoLocation> parametrosPorMaterial =
                parametrosPorLocationEMaterial.getOrDefault(location.getId(), Map.of());
        for (Produto produto : materialList) {
            ParametrosMaterialLocationDTO dto = new ParametrosMaterialLocationDTO();
            ParametrosProdutoLocation parametrosProdutoLocation = parametrosPorMaterial.get(produto.getId());
            dto.setMaterialID(produto.getId());
            dto.setMaterial(produto.getDescricao());
            dto.setLocationID(location.getId());
            dto.setLocation(location.getDescricao());
            dto.setProductionMinimumQuantity(parametrosProdutoLocation == null
                    ? null
                    : parametrosProdutoLocation.getLoteMinimoProducaoCadastrado());
            dto.setProductionMultipleQuantity(parametrosProdutoLocation == null
                    ? null
                    : parametrosProdutoLocation.getMultiploProducaoCadastrado());
            dto.setInativo(parametrosProdutoLocation == null
                    ? produto.getInativo()
                    : parametrosProdutoLocation.getInativo());
            /*
             * Estes tres valores sao os dados cadastrados da combinacao, e
             * nao o status efetivo calculado para uma data de referencia.
             * Assim a API nao perde as datas quando existe um override de
             * lifecycleStage com precedencia sobre elas.
             */
            dto.setLifecycleStage(parametrosProdutoLocation == null
                    ? null
                    : parametrosProdutoLocation.getEstagioCicloVidaCadastrado());
            dto.setIntroductionDate(parametrosProdutoLocation == null
                    ? null
                    : parametrosProdutoLocation.getDataIntroducao());
            dto.setDiscontinuationDate(parametrosProdutoLocation == null
                    ? null
                    : parametrosProdutoLocation.getDataDescontinuacao());
            /*
             * A borda administrativa precisa expor o valor cadastrado, e nao
             * o getter efetivo. Assim `null` continua distinguindo ausencia
             * de override local de um congelamento explicitamente zerado.
             */
            dto.setFrozenHorizonDpInDays(parametrosProdutoLocation == null
                    ? null
                    : parametrosProdutoLocation.getNumeroDiasHorizonteCongeladoDpCadastrado());
            /*
             * Estes ids sao os overrides cadastrados, nao as unidades
             * efetivas. Dessa forma null continua permitindo que o cliente
             * remova a configuracao local sem tentar materializar a cascata
             * usada pelos calculos de Supply Planning.
             */
            dto.setDefaultUomId(getUnidadeMedidaId(
                    parametrosProdutoLocation == null
                            ? null
                            : parametrosProdutoLocation.getUnidadeMedidaPadraoCadastrado()));
            dto.setProductionMinimumMultipleUomId(getUnidadeMedidaId(
                    parametrosProdutoLocation == null
                            ? null
                            : parametrosProdutoLocation.getUnidadeMedidaLoteMinimoMultiploProducaoCadastrado()));
            parametrosMaterialLocationDTOList.add(dto);
        }

    }

    /**
     * Carrega todos os parâmetros administrativos com os relacionamentos
     * Many-to-One necessários e cria um índice de leitura por location e
     * material.
     *
     * <p>O GET da tela combina cada location com os materiais cadastrados. A
     * leitura prévia pelo repository evita acessar
     * {@code Location.mapaParametrosProdutoLocation}, que é LAZY e produziria
     * N+1 consultas em uma tela com muitas combinações.</p>
     */
    private Map<String, Map<String, ParametrosProdutoLocation>>
    indexaParametrosProdutoLocationPorLocationEMaterial() {

        List<ParametrosProdutoLocation> parametrosProdutoLocationList =
                parametrosProdutoLocationRepository.customFindAllComFetchAtributosManyToOne();
        if (parametrosProdutoLocationList == null) {
            throw new IllegalStateException(
                    "Material/location parameter repository returned null fetch result.");
        }

        Map<String, Map<String, ParametrosProdutoLocation>> parametrosPorLocationEMaterial =
                new HashMap<>();
        for (ParametrosProdutoLocation parametrosProdutoLocation : parametrosProdutoLocationList) {
            validaParametrosProdutoLocationCarregadoCommunity(parametrosProdutoLocation);
            parametrosPorLocationEMaterial
                    .computeIfAbsent(
                            parametrosProdutoLocation.getLocation().getId(),
                            ignored -> new HashMap<>())
                    .put(parametrosProdutoLocation.getProduto().getId(), parametrosProdutoLocation);
        }
        return parametrosPorLocationEMaterial;

    }

    /**
     * Impede que uma linha parcialmente carregada seja silenciosamente
     * confundida com ausência de override durante a montagem da resposta.
     */
    private void validaParametrosProdutoLocationCarregadoCommunity(
            ParametrosProdutoLocation parametrosProdutoLocation) {

        if (parametrosProdutoLocation == null
                || parametrosProdutoLocation.getProduto() == null
                || isBlank(parametrosProdutoLocation.getProduto().getId())
                || parametrosProdutoLocation.getLocation() == null
                || isBlank(parametrosProdutoLocation.getLocation().getId())) {
            throw new IllegalStateException(
                    "Fetched material/location parameter requires material and location keys.");
        }

    }

    /**
     * Persiste os parametros operacionais da combinacao material/location.
     * O contrato publico usa `materialID`.
     */
    public boolean saveParametrosMaterialLocationDTO(ParametrosMaterialLocationDTO parametrosMaterialLocationDTO) {

        validaParametrosMaterialLocationDTOCommunity(parametrosMaterialLocationDTO);

        try{
            /*
             * Ambos os ids do payload sao resolvidos antes de carregar a
             * combinacao material/location. Uma unica consulta em lote evita
             * transformar dois overrides em round-trips por linha da tela.
             */
            Map<String, UnidadeMedida> unidadeMedidaPorId = resolveUnidadesMedidaConfiguradas(
                    parametrosMaterialLocationDTO.getDefaultUomId(),
                    parametrosMaterialLocationDTO.getProductionMinimumMultipleUomId());
            Location location = locationService.getLocation(parametrosMaterialLocationDTO.getLocationID());
            Produto produto = materialService.getMaterialDeId(parametrosMaterialLocationDTO.getMaterialID());
            ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey compositeKey =
                    new ParametrosProdutoLocation.ParametrosProdutoLocationCompositeKey(produto, location);
            
            Optional<ParametrosProdutoLocation> optionalParametrosProdutoLocation = parametrosProdutoLocationRepository.findById(compositeKey);
            if (optionalParametrosProdutoLocation == null) {
                throw new IllegalStateException(
                        "Material/location parameter repository returned null Optional for Community material "
                                + parametrosMaterialLocationDTO.getMaterialID()
                                + " and location "
                                + parametrosMaterialLocationDTO.getLocationID()
                                + ".");
            }
            ParametrosProdutoLocation parametrosProdutoLocation = optionalParametrosProdutoLocation
                    .orElseGet(() -> new ParametrosProdutoLocation(compositeKey));
            // Campo ausente no payload Community significa que a combinacao
            // material/location permanece ativa.
            parametrosProdutoLocation.setAtivo(!Boolean.TRUE.equals(parametrosMaterialLocationDTO.getInativo()));
            parametrosProdutoLocation.setLoteMinimoProducao(
                    parametrosMaterialLocationDTO.getProductionMinimumQuantity());
            parametrosProdutoLocation.setMultiploProducao(
                    parametrosMaterialLocationDTO.getProductionMultipleQuantity());
            /*
             * O status explicito e as datas sao independentes no modelo
             * persistido. A entidade resolve lifecycleStage antes das datas;
             * por isso nao limpamos datas quando ha override e tambem
             * persistimos null para que a API possa limpar cada campo.
             */
            parametrosProdutoLocation.setEstagioCicloVida(
                    parametrosMaterialLocationDTO.getLifecycleStage());
            parametrosProdutoLocation.setDataIntroducao(
                    parametrosMaterialLocationDTO.getIntroductionDate());
            parametrosProdutoLocation.setDataDescontinuacao(
                    parametrosMaterialLocationDTO.getDiscontinuationDate());
            parametrosProdutoLocation.setNumeroDiasHorizonteCongeladoDp(
                    parametrosMaterialLocationDTO.getFrozenHorizonDpInDays());
            parametrosProdutoLocation.setUnidadeMedidaPadrao(
                    getUnidadeMedidaConfigurada(
                            unidadeMedidaPorId,
                            parametrosMaterialLocationDTO.getDefaultUomId()));
            parametrosProdutoLocation.setUnidadeMedidaLoteMinimoMultiploProducao(
                    getUnidadeMedidaConfigurada(
                            unidadeMedidaPorId,
                            parametrosMaterialLocationDTO.getProductionMinimumMultipleUomId()));
            /*
             * A chave material/location e usada por projections de Supply e
             * Demand Planning. Repository quebrado deve manter o contrato
             * publico `false`, mas nao pode virar sucesso silencioso.
             */
            ParametrosProdutoLocation parametrosProdutoLocationSalvo =
                    parametrosProdutoLocationRepository.save(parametrosProdutoLocation);
            validaParametrosProdutoLocationSalvoCommunity(parametrosProdutoLocationSalvo);
            return true;
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao salvar parametros material/location Community para material {} e location {}",
                    parametrosMaterialLocationDTO.getMaterialID(),
                    parametrosMaterialLocationDTO.getLocationID(),
                    runtimeException);
            return false;
        }

    }

    /**
     * Valida somente a chave material/location obrigatoria para o save.
     *
     * <p>Quantidades produtivas nulas continuam significando ausência de
     * mínimo/múltiplo cadastrado e `inativo` nulo continua significando
     * combinação ativa. Essas regras
     * sao diferentes de material/location sem material ou sem location, que
     * nao forma chave persistivel.</p>
     */
    private void validaParametrosMaterialLocationDTOCommunity(
            ParametrosMaterialLocationDTO parametrosMaterialLocationDTO) {

        if (parametrosMaterialLocationDTO == null) {
            throw new IllegalArgumentException("Material/location parameter payload is required.");
        }
        if (isBlank(parametrosMaterialLocationDTO.getLocationID())) {
            throw new IllegalArgumentException("Material/location parameter location id is required.");
        }
        if (isBlank(parametrosMaterialLocationDTO.getMaterialID())) {
            throw new IllegalArgumentException("Material/location parameter material id is required.");
        }
        if (parametrosMaterialLocationDTO.getFrozenHorizonDpInDays() != null
                && parametrosMaterialLocationDTO.getFrozenHorizonDpInDays() < 0) {
            throw new IllegalArgumentException(
                    "Material/location frozen Demand Planning horizon must be non-negative.");
        }
        validaQuantidadeMinimaProducao(
                parametrosMaterialLocationDTO.getProductionMinimumQuantity());
        validaMultiploProducao(parametrosMaterialLocationDTO.getProductionMultipleQuantity());

        validaUnidadeMedidaConfiguradaId(
                parametrosMaterialLocationDTO.getDefaultUomId(),
                "default unit of measure");
        validaUnidadeMedidaConfiguradaId(
                parametrosMaterialLocationDTO.getProductionMinimumMultipleUomId(),
                "production minimum/multiple unit of measure");

    }

    /**
     * Valida o mínimo opcional sem substituir uma configuração inválida por
     * zero, o que mudaria a semântica do cálculo posterior.
     */
    private void validaQuantidadeMinimaProducao(Double productionMinimumQuantity) {

        if (productionMinimumQuantity != null
                && (!Double.isFinite(productionMinimumQuantity)
                || productionMinimumQuantity < 0.0d)) {
            throw new IllegalArgumentException(
                    "Material/location production minimum quantity must be finite and non-negative.");
        }

    }

    /**
     * Valida o múltiplo opcional, que não pode ser zero porque os consumidores
     * de Supply Planning o usam como divisor de arredondamento.
     */
    private void validaMultiploProducao(Double productionMultipleQuantity) {

        if (productionMultipleQuantity != null
                && (!Double.isFinite(productionMultipleQuantity)
                || productionMultipleQuantity <= 0.0d)) {
            throw new IllegalArgumentException(
                    "Material/location production multiple quantity must be finite and positive.");
        }

    }

    /**
     * Resolve os overrides de unidade enviados pela API em uma leitura de
     * repository. Valores nulos significam limpeza explicita e nao participam
     * da consulta; ids presentes precisam existir, sem fallback silencioso.
     */
    private Map<String, UnidadeMedida> resolveUnidadesMedidaConfiguradas(
            String defaultUomId,
            String productionMinimumMultipleUomId) {

        Set<String> unidadeMedidaIdSet = new LinkedHashSet<>();
        adicionaUnidadeMedidaIdQuandoConfigurado(unidadeMedidaIdSet, defaultUomId);
        adicionaUnidadeMedidaIdQuandoConfigurado(
                unidadeMedidaIdSet,
                productionMinimumMultipleUomId);
        if (unidadeMedidaIdSet.isEmpty()) {
            return Map.of();
        }

        Iterable<UnidadeMedida> unidadeMedidaIterable = unidadeMedidaRepository.findAllById(unidadeMedidaIdSet);
        if (unidadeMedidaIterable == null) {
            throw new IllegalStateException("Unit of measure repository returned null batch result.");
        }

        Map<String, UnidadeMedida> unidadeMedidaPorId = new HashMap<>();
        for (UnidadeMedida unidadeMedida : unidadeMedidaIterable) {
            if (unidadeMedida == null || isBlank(unidadeMedida.getId())) {
                throw new IllegalStateException("Unit of measure repository returned an invalid configured unit.");
            }
            unidadeMedidaPorId.put(unidadeMedida.getId(), unidadeMedida);
        }

        for (String unidadeMedidaId : unidadeMedidaIdSet) {
            if (!unidadeMedidaPorId.containsKey(unidadeMedidaId)) {
                throw new IllegalStateException(
                        "Configured unit of measure " + unidadeMedidaId + " was not found.");
            }
        }
        return unidadeMedidaPorId;

    }

    /**
     * Recupera a unidade previamente validada, preservando {@code null} como
     * instrucao de limpeza do override local.
     */
    private UnidadeMedida getUnidadeMedidaConfigurada(
            Map<String, UnidadeMedida> unidadeMedidaPorId,
            String unidadeMedidaId) {

        if (unidadeMedidaId == null) {
            return null;
        }
        UnidadeMedida unidadeMedida = unidadeMedidaPorId.get(unidadeMedidaId);
        if (unidadeMedida == null) {
            throw new IllegalStateException(
                    "Configured unit of measure " + unidadeMedidaId + " disappeared after batch resolution.");
        }
        return unidadeMedida;

    }

    /**
     * Adiciona id nao nulo ao batch, sem reinterpretar null como um valor
     * padrao de configuracao.
     */
    private void adicionaUnidadeMedidaIdQuandoConfigurado(
            Collection<String> unidadeMedidaIdCollection,
            String unidadeMedidaId) {

        if (unidadeMedidaId != null) {
            unidadeMedidaIdCollection.add(unidadeMedidaId);
        }

    }

    /**
     * Garante que uma limpeza seja expressa por null e nao por identificador
     * vazio, que nao poderia ser resolvido de maneira deterministica.
     */
    private void validaUnidadeMedidaConfiguradaId(
            String unidadeMedidaId,
            String contextoUnidadeMedida) {

        if (unidadeMedidaId != null && isBlank(unidadeMedidaId)) {
            throw new IllegalArgumentException(
                    "Material/location parameter " + contextoUnidadeMedida + " id must not be blank.");
        }

    }

    /**
     * Extrai o id cadastrado para serializacao administrativa sem aplicar a
     * cascata de unidade efetiva usada no calculo.
     */
    private String getUnidadeMedidaId(UnidadeMedida unidadeMedida) {

        return (unidadeMedida == null) ? null : unidadeMedida.getId();

    }

    /**
     * Valida a fotografia salva de parametros material/location.
     *
     * <p>O par material/location e a menor chave operacional de varios
     * calculos. Snapshot salvo nulo ou sem uma das pontas da chave indica erro
     * de persistencia e precisa fazer o save administrativo retornar `false`.</p>
     */
    private void validaParametrosProdutoLocationSalvoCommunity(
            ParametrosProdutoLocation parametrosProdutoLocationSalvo) {

        if (parametrosProdutoLocationSalvo == null) {
            throw new IllegalStateException("Saved material/location parameter snapshot is required.");
        }
        if (parametrosProdutoLocationSalvo.getParametrosProdutoLocationCompositeKey() == null
                || parametrosProdutoLocationSalvo.getProduto() == null
                || isBlank(parametrosProdutoLocationSalvo.getProduto().getId())
                || parametrosProdutoLocationSalvo.getLocation() == null
                || isBlank(parametrosProdutoLocationSalvo.getLocation().getId())) {
            throw new IllegalStateException("Saved material/location parameter key is required.");
        }

    }
    @Transactional
    public List<LocationDTO> getLocationsDTO(){
        List<LocationDTO> locationDTOList = new ArrayList<>();
        try{
            for (Location location : locationService.findAll()) {
                LocationDTO dto = new LocationDTO();
                dto.setId(location.getId());
                dto.setDescription(location.getDescricao());
                locationDTOList.add(dto);
            }
        } catch (RuntimeException runtimeException) {
            log.error("Erro ao carregar locations para parametros Community", runtimeException);
        }
        return locationDTOList;
    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }

}
