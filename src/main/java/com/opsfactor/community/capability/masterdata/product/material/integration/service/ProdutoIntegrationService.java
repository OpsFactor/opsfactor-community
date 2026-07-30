package com.opsfactor.community.capability.masterdata.product.material.integration.service;

import com.opsfactor.community.capability.masterdata.product.material.integration.dto.ProdutoIntegrationDataDto;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationMapper;
import com.opsfactor.community.capability.masterdata.product.material.integration.mapper.ProdutoIntegrationSupportData;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.repository.UnidadeMedidaRepository;
import com.opsfactor.community.platform.integration.service.EmptyIntegrationDataFilter;
import com.opsfactor.community.platform.integration.service.IntegrationServiceInterface;
import com.opsfactor.community.platform.exception.DataUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.opsfactor.community.platform.integration.service.IntegrationSupportDataValidation.getMapaPorIdObrigatorio;

/**
 * Service de integracao do cadastro basico de materiais Community.
 *
 * <p>A entidade fisica ainda se chama {@link Produto}, mas esta borda publica
 * usa material. Caracteristicas, filtros/agregadores, pricing e atributos
 * economicos permanecem em capabilities Enterprise.</p>
 */
@Component
public class ProdutoIntegrationService implements IntegrationServiceInterface<ProdutoIntegrationDataDto, ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO, Produto, ProdutoIntegrationSupportData, ProdutoIntegrationMapper,EmptyIntegrationDataFilter> {

    /**
     * Repository de unidade de medida usado para validar e resolver a UOM base
     * informada na carga de materiais Community.
     */
    @Autowired
    private UnidadeMedidaRepository unidadeMedidaRepository;

    /**
     * Repository operacional de materiais. Caracteristicas, filtros e
     * agregadores de material continuam fora deste service Community.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Mapper de integracao responsavel por converter linhas publicas de carga
     * em entidades de material do schema Community.
     */
    @Autowired
    private ProdutoIntegrationMapper produtoIntegrationMapper;
    

    @Override
    public ProdutoIntegrationMapper getMapper() {
        return produtoIntegrationMapper;
    }

    @Override
    public List<Produto> saveEntityList(Collection<Produto> entityList) {
        if (!entityList.isEmpty()) {
            validaSavedEntityCollection(
                    produtoRepository.saveAll(entityList),
                    "Material saved collection",
                    entityList.size());
            produtoRepository.flush(); // flush para carregar os valores salvos no lote de importacao
        }
        return new ArrayList<>();
    }

    @Override
    public void removeEntityList(Collection<Produto> entityList) {
        /*
         * Caracteristicas de material nao existem como carga nem como
         * repository Community. Se houver valores dependentes no banco, a base
         * esta fora do contrato desta edicao e deve falhar explicitamente.
         */
        produtoRepository.deleteAll(entityList);
    }

    @Override
    public ProdutoIntegrationSupportData getSupportData() {
        return ProdutoIntegrationSupportData.builder()
                /*
                 * UOM e o support data minimo do cadastro de material. Validar
                 * antes do mapper evita erro generico de stream quando a base
                 * de apoio vem nula, com item nulo ou id duplicado.
                 */
                .unidadeMedidaMap(getMapaPorIdObrigatorio(
                        unidadeMedidaRepository.findAll(),
                        UnidadeMedida::getId,
                        "Unit of Measure snapshot"))
                .build();
    }

    @Override
    public int getBatchSize() {
        return 5000;
    }

    @Override
    public Collection<Produto> getPersistedEntityCollectionFromPrimaryKeyDtoCollection(Collection<ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO> dtoBatchList) {
        Collection<ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO> produtoPrimaryKeyCollection =
                validaProdutoPrimaryKeyCollection(dtoBatchList);
        Set<String> idsInBatch = produtoPrimaryKeyCollection.stream()
                .map(dto -> dto.id)
                .collect(Collectors.toSet());
        
        return produtoRepository.findAllById(idsInBatch);
    }

    @Override
    public String getSaveSuccessMessage() {
        return "Material data saved";
    }

    @Override
    public Collection<Produto> getAllPersistedEntities() {

        /*
         * O download completo usa o mapper para acessar as UOMs operacionais
         * e, no runtime Enterprise, a UOM de COGS. Carrega-las no mesmo
         * snapshot evita quatro caminhos lazy por material sem alterar a
         * entidade, o layout de integracao ou a semantica de listagem.
         */
        return produtoRepository.findAllWithUnitOfMeasures();

    }

    /**
     * Desativa materiais por lista explicita de ids ou, quando nenhum filtro é
     * informado, desativa toda a base operacional Community.
     */
    public void desativaMateriaisDeFiltro(
            ProdutoIntegrationDataDto.MaterialDeactivationFilterIntegrationDTO filtroMateriaisADesativar) {

        if (filtroMateriaisADesativar != null && filtroMateriaisADesativar.id != null && !filtroMateriaisADesativar.id.isEmpty()) {
            List<Produto> materialList = produtoRepository.findAllById(
                    filtroMateriaisADesativar.id);
            if (materialList.isEmpty()) return;
            materialList.forEach(material -> material.setAtivo(false));
            produtoRepository.saveAll(materialList);
        } else {
            List<Produto> materiais = produtoRepository.findAll();
            if (materiais.isEmpty()) return;
            materiais.forEach(material -> material.setAtivo(false));
            produtoRepository.saveAll(materiais);
        }
    }

    /**
     * Valida o snapshot administrativo usado por desativacao de materiais.
     *
     * <p>A borda Community nao possui filtros por caracteristica ou agregacao.
     * Mesmo assim, a colecao retornada pelo repository precisa carregar
     * materiais reais antes do `saveAll`; item nulo ou id ausente indica
     * snapshot quebrado e deve falhar antes de a carga reportar sucesso de
     * desativacao.</p>
     */
    /**
     * Valida chaves de material recebidas antes do lookup por id.
     *
     * <p>O repository aceita um conjunto de ids, mas o payload de upload e uma
     * lista de linhas. Duplicidade de id precisa falhar na borda da carga, com
     * indice do item recebido, antes de virar um unico elemento em `Set`.</p>
     */
    private static Collection<ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO> validaProdutoPrimaryKeyCollection(
            Collection<ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO> produtoPrimaryKeyCollection) {

        if (produtoPrimaryKeyCollection == null) {
            throw new DataUploadException("Material primary key collection is required.");
        }

        Set<String> materialIds = new HashSet<>();
        int indice = 0;
        for (ProdutoIntegrationDataDto.ProdutoPrimaryKeyIntegrationDTO produtoPrimaryKeyIntegrationDTO
                : produtoPrimaryKeyCollection) {
            if (produtoPrimaryKeyIntegrationDTO == null) {
                throw new DataUploadException(
                        "Material primary key collection item at index " + indice + " is required.");
            }
            if (produtoPrimaryKeyIntegrationDTO.id == null || produtoPrimaryKeyIntegrationDTO.id.isBlank()) {
                throw new DataUploadException("materialId is required.");
            }
            if (!materialIds.add(produtoPrimaryKeyIntegrationDTO.id)) {
                throw new DataUploadException(
                        "Material primary key collection item at index "
                                + indice
                                + " has duplicated materialId "
                                + produtoPrimaryKeyIntegrationDTO.id
                                + ".");
            }
            indice++;
        }

        return produtoPrimaryKeyCollection;

    }

}
