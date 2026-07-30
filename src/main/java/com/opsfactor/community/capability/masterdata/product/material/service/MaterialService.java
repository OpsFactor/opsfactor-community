package com.opsfactor.community.capability.masterdata.product.material.service;

import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.product.material.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.NoResultException;
import java.util.*;

/**
 * Service Community para leitura simples do cadastro de materiais.
 *
 * <p>A entidade JPA ainda se chama {@link Produto} porque a tabela fisica
 * compartilhada nao foi renomeada nesta fase da migracao. A borda de uso nova,
 * porem, deve falar em material para evitar que novas classes propaguem a
 * nomenclatura historica produto.</p>
 */
@Service
@Slf4j
public class MaterialService {

    /*
     * Repository fisico transicional. A entidade/tabela ainda se chama Produto,
     * mas este bean e a borda de dominio Community para materiais.
     */
    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Conta os materiais cadastrados na entidade fisica compartilhada.
     */
    public long countMaterial() {

        return produtoRepository.count();

    }

    /**
     * Retorna materiais ativos ou todos os materiais do cadastro.
     *
     * <p>O tipo retornado permanece {@link Produto} porque a entidade JPA ainda
     * e compartilhada com a nomenclatura fisica legada.</p>
     */
    public List<Produto> getMateriais(boolean somenteMateriaisAtivos) {

        if (somenteMateriaisAtivos) {
            return produtoRepository.customFindProdutosAtivos();
        } else {
            return produtoRepository.findAll();
        }

    }

    /**
     * Busca material por ID, falhando explicitamente quando o ID nao existe.
     */
    public Produto getMaterialDeId(String materialId) {

        if (materialId == null) throw new NoResultException("Empty Material Id");
        return produtoRepository.findById(materialId).orElseThrow(() -> new NoResultException("Material " + materialId + " not found"));

    }

}
