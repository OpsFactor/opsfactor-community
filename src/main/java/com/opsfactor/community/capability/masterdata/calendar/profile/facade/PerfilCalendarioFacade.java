package com.opsfactor.community.capability.masterdata.calendar.profile.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendario;
import com.opsfactor.community.capability.masterdata.calendar.profile.domain.PerfilCalendarioSimples;
import com.opsfactor.community.capability.masterdata.calendar.profile.dto.PerfilCalendarioDTO;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioRepository;
import com.opsfactor.community.capability.masterdata.calendar.profile.repository.PerfilCalendarioSimplesRepository;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cadastro de calendários para telas, separado da integração de dados.
 * O Enterprise substitui os pontos polimórficos sem publicar seus campos no Community.
 */
@Service
public class PerfilCalendarioFacade {

    protected final PerfilCalendarioRepository repository;
    protected final PerfilCalendarioSimplesRepository simplesRepository;
    protected final ObjectMapper objectMapper;

    /**
     * Injeta o catálogo compartilhado, a consulta tipada simples e a conversão dos DTOs de UI.
     */
    @Autowired
    public PerfilCalendarioFacade(PerfilCalendarioRepository repository,
                                  PerfilCalendarioSimplesRepository simplesRepository,
                                  ObjectMapper objectMapper) {

        this.repository = repository;
        this.simplesRepository = simplesRepository;
        this.objectMapper = objectMapper;

    }

    /** Consulta o catálogo em lote; Demand usa a seleção tipada uniforme. */
    @Transactional(readOnly = true)
    public List<PerfilCalendarioDTO> listar(boolean simpleOnly) {

        List<? extends PerfilCalendario> perfis = simpleOnly ? simplesRepository.findAll() : repository.findAll();
        return perfis.stream().sorted(java.util.Comparator.comparing(PerfilCalendario::getId))
                .map(this::converterDTO).toList();

    }

    /**
     * Resolve a receita completa antes de sair da transação. As extensões usam
     * EAGER/SUBSELECT para seus filhos; a validação polimórfica também visita a
     * receita integral antes de entregá-la à montagem de uma execução.
     */
    @Transactional(readOnly = true)
    public PerfilCalendario obterPerfilCompleto(String id) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("calendarProfileId é obrigatório.");
        }
        PerfilCalendario perfil = (PerfilCalendario) Hibernate.unproxy(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil calendário não encontrado: " + id)));
        perfil.validarEstrutura();
        return perfil;

    }

    /**
     * Valida antes de gravar. Trocar o tipo de uma receita já referenciada não é
     * permitido: altera o discriminator JPA e o contrato de perfis Demand.
     */
    @Transactional
    @CacheEvict(value = "parametrosDemandPlanProjection", allEntries = true)
    public PerfilCalendarioDTO salvar(JsonNode payload) {

        PerfilCalendarioDTO dto = lerDTO(payload);
        PerfilCalendario novaReceita = criarEntidade(dto);
        novaReceita.validarEstrutura();
        PerfilCalendario perfil = repository.customFindByIdForUpdate(dto.getId())
                .map(entidade -> (PerfilCalendario) Hibernate.unproxy(entidade)).orElse(novaReceita);
        if (!perfil.getClass().equals(novaReceita.getClass())) {
            throw new IllegalArgumentException("O tipo de um perfil calendário existente não pode ser alterado.");
        }
        atualizarEntidade(perfil, novaReceita);
        perfil.validarEstrutura();
        return converterDTO(repository.save(perfil));

    }

    /** Bloqueia uma extensão antes de desserializar ou acessar qualquer repository. */
    protected PerfilCalendarioDTO lerDTO(JsonNode payload) {

        String tipo = payload.path("type").asText("SIMPLES");
        if (!"SIMPLES".equals(tipo)) {
            throw new RequiresEnterpriseVersionException("Calendar profile type " + tipo);
        }
        return objectMapper.convertValue(payload, PerfilCalendarioDTO.class);

    }

    protected PerfilCalendario criarEntidade(PerfilCalendarioDTO dto) {

        if (dto.getNumberOfBasePeriods() == null) {
            throw new IllegalArgumentException("O calendário simples exige numberOfBasePeriods.");
        }
        PerfilCalendarioSimples perfil = new PerfilCalendarioSimples(dto.getId(),
                dto.getBaseBucketSize(), dto.getNumberOfBasePeriods());
        perfil.setDescricao(dto.getDescription());
        return perfil;

    }

    /** A edição mantém a entidade referenciada pelos perfis de execução. */
    protected void atualizarEntidade(PerfilCalendario perfil, PerfilCalendario novaReceita) {

        perfil.setDescricao(novaReceita.getDescricao());
        perfil.setTamanhoBucketBase(novaReceita.getTamanhoBucketBase());
        ((PerfilCalendarioSimples) perfil).setNumeroPeriodosBucketBase(
                ((PerfilCalendarioSimples) novaReceita).getNumeroPeriodosBucketBase());

    }

    protected PerfilCalendarioDTO converterDTO(PerfilCalendario perfil) {

        perfil = (PerfilCalendario) Hibernate.unproxy(perfil);
        if (!(perfil instanceof PerfilCalendarioSimples simples)) {
            throw new RequiresEnterpriseVersionException("Calendar profile extension");
        }
        PerfilCalendarioDTO dto = new PerfilCalendarioDTO();
        preencherCamposComuns(dto, perfil);
        dto.setNumberOfBasePeriods(simples.getNumeroPeriodosBucketBase());
        return dto;

    }

    /** Bucket inicial é calculado pela própria receita, sem materializar uma grade datada. */
    protected void preencherCamposComuns(PerfilCalendarioDTO dto, PerfilCalendario perfil) {

        perfil.validarEstrutura();
        dto.setId(perfil.getId());
        dto.setDescription(perfil.getDescricao());
        dto.setBaseBucketSize(perfil.getTamanhoBucketBase());
        dto.setFirstPeriodBucketSize(perfil.getTamanhoBucketPrimeiroPeriodo());

    }

}
