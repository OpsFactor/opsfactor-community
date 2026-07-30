package com.opsfactor.community.capability.transactionaldata.inventory.stock.facade;

import com.opsfactor.community.capability.transactionaldata.inventory.stock.domain.Estoque;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.repository.EstoqueRepository;
import com.opsfactor.community.capability.transactionaldata.inventory.stock.facade.dto.EstoqueDTO;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service de estoque inicial Community.
 *
 * <p>Estoque inicial e dado transacional permitido no Community e usado pelo
 * Supply Planning heuristico. Estoque em lote, producao em batch e demais
 * aberturas transacionais avancadas pertencem ao Enterprise.</p>
 */
@Service
public class EstoqueService {

    /**
     * Repository de estoque inicial, com consultas especificas para ultimo
     * estoque por material e remocao por janela de datas.
     */
    @Autowired
    private EstoqueRepository estoqueRepository;

    /**
     * Consulta o estoque mais recente de um material.
     */
    public List<EstoqueDTO> checkMaterialStock(String materialId) {


        List<EstoqueDTO> estoqueDTOList = new ArrayList<>();
        Collection<Estoque> estoqueCollection =
                estoqueRepository.findByLastMaterial(materialId);

        for (Estoque estoque : estoqueCollection) {
            validaEstoqueUltimoMaterialCommunity(estoque, materialId);
            EstoqueDTO estoqueDTO = new EstoqueDTO();
            estoqueDTO.setReference_date(estoque.getDataReferencia());
            estoqueDTO.setLocation_id(estoque.getLocation().getId());
            estoqueDTO.setMaterial_id(estoque.getProduto().getId());
            estoqueDTO.setQuantity(estoque.getQuantidade());
            estoqueDTOList.add(estoqueDTO);
        }

        return estoqueDTOList;

    }
    
    /**
     * Remove estoques iniciais dentro da janela informada.
     */
    public void apagaEstoquesEntreDatas(LocalDateTime dataInicial, LocalDateTime dataFinal) {

        validaJanelaRemocaoEstoqueCommunity(dataInicial, dataFinal);

        estoqueRepository.removeByEstoqueCompositeKeyDataReferenciaBetween(dataInicial, dataFinal);
    }

    /**
     * Valida a colecao de estoques retornada para o material consultado.
     *
     * <p>Colecao vazia e ausencia operacional valida de estoque para o material.
     * Colecao nula indica repository quebrado e deve falhar antes da montagem do
     * DTO, mantendo claro que o problema nao e material sem saldo.</p>
     */
    /**
     * Valida cada linha de estoque antes de expor o DTO simples do Community.
     *
     * <p>O DTO depende de location, material, data e quantidade. Se qualquer
     * parte da chave vier quebrada, o service deve acusar snapshot incoerente em
     * vez de devolver linha parcial ou explodir com NPE durante os getters da
     * entidade.</p>
     */
    private void validaEstoqueUltimoMaterialCommunity(
            Estoque estoque,
            String materialId) {

        if (estoque == null) {
            throw new IllegalStateException(
                    "Latest stock repository returned null item for material "
                            + materialId
                            + ".");
        }
        if (estoque.getEstoqueCompositeKey() == null) {
            throw new IllegalStateException(
                    "Latest stock item for material "
                            + materialId
                            + " must have a primary key.");
        }
        if (estoque.getLocation() == null || estoque.getLocation().getId() == null || estoque.getLocation().getId().isBlank()) {
            throw new IllegalStateException(
                    "Latest stock item for material "
                            + materialId
                            + " must have a location id.");
        }
        if (estoque.getProduto() == null || estoque.getProduto().getId() == null || estoque.getProduto().getId().isBlank()) {
            throw new IllegalStateException(
                    "Latest stock item for material "
                            + materialId
                            + " must have a material id.");
        }
        if (estoque.getDataReferencia() == null) {
            throw new IllegalStateException(
                    "Latest stock item for material "
                            + materialId
                            + " must have a reference date.");
        }
        if (estoque.getQuantidade() == null || !Double.isFinite(estoque.getQuantidade())) {
            throw new IllegalStateException(
                    "Latest stock item for material "
                            + materialId
                            + " must have finite quantity.");
        }

    }

    /**
     * Valida a janela de remocao administrativa de estoques.
     */
    private void validaJanelaRemocaoEstoqueCommunity(
            LocalDateTime dataInicial,
            LocalDateTime dataFinal) {

        if (dataInicial == null) {
            throw new IllegalArgumentException("Initial stock deletion start date is required.");
        }
        if (dataFinal == null) {
            throw new IllegalArgumentException("Initial stock deletion end date is required.");
        }
        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException("Initial stock deletion start date must be before or equal to end date.");
        }

    }
    /**
     * Falha para texto nulo ou em branco antes de consultar repository.
     */
    
}
