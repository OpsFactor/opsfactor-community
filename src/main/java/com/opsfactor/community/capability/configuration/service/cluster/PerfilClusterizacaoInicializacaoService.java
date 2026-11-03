package com.opsfactor.community.capability.configuration.service.cluster;

import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.capability.configuration.repository.cluster.PerfilClusterizacaoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Garante o perfil inicial obrigatório antes dos cadastros que referenciam clusters.
 * O componente pertence à camada compartilhada, portanto inicializa tanto web
 * quanto jobs/batch e é herdado pelo Enterprise. Não depende de uma primeira
 * execução de forecast, de leitura de projection ou de uma tela de cadastro.
 *
 * <p>Este bootstrap cria somente o registro padrão. Alterar PKs, preencher
 * origens históricas e ampliar FKs continua sendo responsabilidade da migração
 * explícita, que utiliza o mesmo identificador estável.</p>
 */
@Service
public class PerfilClusterizacaoInicializacaoService {

    private final PerfilClusterizacaoRepository perfilClusterizacaoRepository;

    /** Injeta apenas o catálogo, evitando ciclos com factories e serviços de cálculo. */
    @Autowired
    public PerfilClusterizacaoInicializacaoService(PerfilClusterizacaoRepository perfilClusterizacaoRepository) {

        this.perfilClusterizacaoRepository = perfilClusterizacaoRepository;

    }

    /**
     * Inicialização idempotente: preserva descrição e versão de um registro já
     * existente. A transação de saveAndFlush pertence ao repository, porque
     * callbacks PostConstruct executam antes do proxy transacional do service.
     */
    @PostConstruct
    public void inicializaPerfilPadrao() {

        if (perfilClusterizacaoRepository.findById(PerfilClusterizacao.ID_LEGADO).isPresent()) {
            return;
        }
        try {
            perfilClusterizacaoRepository.saveAndFlush(new PerfilClusterizacao(
                    PerfilClusterizacao.ID_LEGADO, PerfilClusterizacao.DESCRICAO_PADRAO));
        } catch (DataIntegrityViolationException exception) {
            // Dois runtimes podem inicializar a mesma base simultaneamente.
            // Só a existência efetiva do registro após a transação concorrente
            // satisfaz o contrato; qualquer falha sem o perfil é propagada.
            if (perfilClusterizacaoRepository.findById(PerfilClusterizacao.ID_LEGADO).isEmpty()) {
                throw exception;
            }
        }

    }
}
