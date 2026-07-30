package com.opsfactor.community.capability.masterdata.production.productionresource.facade;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.network.location.repository.LocationRepository;
import com.opsfactor.community.capability.masterdata.production.productionresource.repository.RecursoProdutivoRepository;
import com.opsfactor.community.platform.exception.DataUploadException;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.mapper.RecursoProdutivoAutoMapper;
import com.opsfactor.community.capability.masterdata.production.productionresource.facade.dto.RecursoProdutivoDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service front de recursos produtivos Community.
 *
 * <p>O Community persiste somente descricao, status, eficiencia e location do
 * recurso produtivo. Custos, calendarios de turno, manutencao e capacidade por
 * linha pertencem ao Enterprise.</p>
 */
@Service
public class RecursoProdutivoFacade {

    /**
     * Repository canonico do recurso produtivo Community.
     */
    @Autowired
    private RecursoProdutivoRepository recursoProdutivoRepository;

    /**
     * Mapper para o DTO operacional exposto ao front.
     */
    @Autowired
    private RecursoProdutivoAutoMapper recursoProdutivoAutoMapper;

    /**
     * Repository de locations usado para validar a location obrigatoria do
     * recurso produtivo antes da persistencia.
     */
    @Autowired
    private LocationRepository locationRepository;

    /**
     * Lista recursos produtivos no contrato Community da tela.
     *
     * <p>A listagem e consumida por seletores e telas de master data do
     * Supply Planning. Antes do mapper, validamos o snapshot minimo para que
     * dados quebrados nao virem DTOs parciais nem cheguem nas projections de
     * capacidade produtiva.</p>
     */
    public List<RecursoProdutivoDTO> getRecursoProdutivoDTOList() {

        List<RecursoProdutivo> recursoProdutivoList =
                recursoProdutivoRepository.customFindAllWithLocation();
        validaRecursoProdutivoListCarregadaCommunity(recursoProdutivoList);

        List<RecursoProdutivoDTO> recursoProdutivoDTOList =
                recursoProdutivoAutoMapper.converteListaEntidadeParaListaDTO(
                        recursoProdutivoList);
        validaRecursoProdutivoDTOListCarregadaCommunity(recursoProdutivoDTOList);

        return recursoProdutivoDTOList;

    }

    /**
     * Cria ou atualiza um recurso produtivo operacional.
     *
     * <p>O metodo preserva apenas campos Community: id, descricao, ativo,
     * eficiencia e location. Custos, turnos, manutencao e capacidade avancada
     * nao sao preenchidos nesta borda.</p>
     */
    public void saveRecursoProdutivoDTO(RecursoProdutivoDTO recursoProdutivoDTO) throws DataUploadException {

        validaRecursoProdutivoDTOCommunity(recursoProdutivoDTO);

        /*
         * Se o recurso produtivo ja existe, atualizamos o registro persistido;
         * caso contrario, criamos uma entidade nova com o id informado pela
         * tela.
         */
        Optional<RecursoProdutivo> recursoProdutivoOptional =
                recursoProdutivoRepository.findById(recursoProdutivoDTO.productionResourceId);

        /*
         * Recurso inexistente e caminho funcional: a tela pode criar um novo
         * cadastro com o id informado. Retorno nulo do repository, entretanto,
         * quebra o contrato Spring Data e deve falhar antes de montar entidade
         * parcial ou consultar Location.
         */
        if (recursoProdutivoOptional == null) {
            throw new IllegalStateException(
                    "Production Resource repository returned null Optional for front save id "
                            + recursoProdutivoDTO.productionResourceId
                            + ".");
        }

        RecursoProdutivo recursoProdutivo = recursoProdutivoOptional
                .orElse(RecursoProdutivo.builder()
                        .id(recursoProdutivoDTO.productionResourceId)
                        .build());
        validaRecursoProdutivoCarregadoCommunity(
                recursoProdutivo,
                recursoProdutivoDTO.productionResourceId);
        
        /*
         * Location e obrigatoria. Se nao existir, falhamos antes de salvar para
         * nao deixar recurso produtivo orfao ou apontando para default tecnico.
         */
        Optional<Location> locationOptional = locationRepository.findById(recursoProdutivoDTO.locationId);

        /*
         * Location ausente continua erro funcional de cadastro. Optional nulo
         * indica repository quebrado e deve falhar antes do save para evitar
         * recurso produtivo orfao ou com default tecnico.
         */
        if (locationOptional == null) {
            throw new IllegalStateException(
                    "Location repository returned null Optional for Production Resource front save id "
                            + recursoProdutivoDTO.locationId
                            + ".");
        }

        Location location = locationOptional
                .orElseThrow(() -> new DataUploadException("Location " + recursoProdutivoDTO.locationId + " not found"));
        validaLocationCarregadaRecursoProdutivoCommunity(
                location,
                recursoProdutivoDTO.locationId);
        
        recursoProdutivo.setDescricao(recursoProdutivoDTO.description);
        recursoProdutivo.setAtivo(recursoProdutivoDTO.active);
        recursoProdutivo.setEficiencia(recursoProdutivoDTO.efficiency);
        recursoProdutivo.setLocation(location);
        
        RecursoProdutivo recursoProdutivoSalvo =
                recursoProdutivoRepository.save(recursoProdutivo);
        validaRecursoProdutivoSalvoCommunity(
                recursoProdutivoSalvo,
                recursoProdutivoDTO.productionResourceId,
                recursoProdutivoDTO.locationId);
        
    }

    /**
     * Valida a chave minima do recurso produtivo Community.
     *
     * <p>O heuristico precisa do recurso vinculado a uma location real. Campos
     * como descricao, ativo e eficiencia mantem a semantica operacional do
     * cadastro e podem ser nulos conforme a entidade/model mapper; id do
     * recurso e location, entretanto, sao obrigatorios antes de repositories.</p>
     */
    private void validaRecursoProdutivoDTOCommunity(RecursoProdutivoDTO recursoProdutivoDTO) {

        if (recursoProdutivoDTO == null) {
            throw new IllegalArgumentException("Production Resource payload is required.");
        }
        if (recursoProdutivoDTO.productionResourceId == null || recursoProdutivoDTO.productionResourceId.isBlank()) {
            throw new IllegalArgumentException("Production Resource id is required.");
        }
        if (recursoProdutivoDTO.locationId == null || recursoProdutivoDTO.locationId.isBlank()) {
            throw new IllegalArgumentException("Production Resource location id is required.");
        }

    }

    /**
     * Valida o recurso produtivo carregado para atualizacao.
     *
     * <p>Quando o repository encontra um registro existente, ele precisa ser o
     * mesmo recurso solicitado pelo DTO. Um id ausente ou divergente aqui
     * indica lookup/stub quebrado e deve falhar antes de sobrescrever campos
     * ou salvar a entidade sob identidade incorreta.</p>
     */
    private void validaRecursoProdutivoCarregadoCommunity(
            RecursoProdutivo recursoProdutivo,
            String productionResourceIdEsperado) {

        if (recursoProdutivo.getId() == null || recursoProdutivo.getId().isBlank()) {
            throw new IllegalStateException(
                    "Loaded Production Resource id is required for front save id "
                            + productionResourceIdEsperado
                            + ".");
        }
        if (!productionResourceIdEsperado.equals(recursoProdutivo.getId())) {
            throw new IllegalStateException(
                    "Loaded Production Resource id must match front save id "
                            + productionResourceIdEsperado
                            + ".");
        }

    }

    /**
     * Valida a location carregada para vinculo do recurso produtivo.
     */
    private void validaLocationCarregadaRecursoProdutivoCommunity(
            Location location,
            String locationIdEsperado) {

        if (location.getId() == null || location.getId().isBlank()) {
            throw new IllegalStateException(
                    "Loaded Location id is required for Production Resource front save id "
                            + locationIdEsperado
                            + ".");
        }
        if (!locationIdEsperado.equals(location.getId())) {
            throw new IllegalStateException(
                    "Loaded Location id must match Production Resource front save id "
                            + locationIdEsperado
                            + ".");
        }

    }

    /**
     * Valida o snapshot lido para a listagem front de recursos produtivos.
     *
     * <p>Lista vazia e um estado funcional valido. Lista nula, item nulo,
     * recurso sem id ou sem location indicam quebra de contrato do repository
     * ou cadastro estruturalmente incompleto, e devem falhar antes da montagem
     * dos DTOs.</p>
     */
    private void validaRecursoProdutivoListCarregadaCommunity(
            List<RecursoProdutivo> recursoProdutivoList) {

        if (recursoProdutivoList == null) {
            throw new IllegalStateException("Production Resource list snapshot is required.");
        }

        for (int index = 0; index < recursoProdutivoList.size(); index++) {
            RecursoProdutivo recursoProdutivo = recursoProdutivoList.get(index);

            if (recursoProdutivo == null) {
                throw new IllegalStateException(
                        "Production Resource at index " + index + " is required in list snapshot.");
            }
            if (recursoProdutivo.getId() == null || recursoProdutivo.getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Resource at index " + index + " has no id in list snapshot.");
            }
            if (recursoProdutivo.getLocation() == null
                    || recursoProdutivo.getLocation().getId() == null
                    || recursoProdutivo.getLocation().getId().isBlank()) {
                throw new IllegalStateException(
                        "Production Resource at index " + index + " has no location in list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de recursos produtivos devolvida pelo mapper.
     *
     * <p>A listagem da SPA trabalha com o DTO, nao com a entidade. Por isso a
     * borda Community valida novamente a identidade operacional depois da
     * conversao, garantindo que mapper/overlay futuro nao devolva recurso sem
     * id ou sem location.</p>
     */
    private void validaRecursoProdutivoDTOListCarregadaCommunity(
            List<RecursoProdutivoDTO> recursoProdutivoDTOList) {

        if (recursoProdutivoDTOList == null) {
            throw new IllegalStateException("Production Resource DTO list snapshot is required.");
        }

        for (int index = 0; index < recursoProdutivoDTOList.size(); index++) {
            RecursoProdutivoDTO recursoProdutivoDTO = recursoProdutivoDTOList.get(index);

            if (recursoProdutivoDTO == null) {
                throw new IllegalStateException(
                        "Production Resource DTO at index " + index + " is required in list snapshot.");
            }
            if (isBlank(recursoProdutivoDTO.productionResourceId)) {
                throw new IllegalStateException(
                        "Production Resource DTO at index " + index + " has no id in list snapshot.");
            }
            if (isBlank(recursoProdutivoDTO.locationId)) {
                throw new IllegalStateException(
                        "Production Resource DTO at index " + index + " has no location in list snapshot.");
            }
        }

    }

    /**
     * Valida o snapshot devolvido pelo repository apos salvar recurso
     * produtivo.
     *
     * <p>A tela assume sucesso imediatamente depois desta chamada. Retorno
     * nulo, id vazio ou location ausente indicam problema de repository/mapping
     * e precisam falhar na borda front, antes de o cadastro ser usado por
     * projections de capacidade produtiva do Supply Planning.</p>
     */
    private void validaRecursoProdutivoSalvoCommunity(
            RecursoProdutivo recursoProdutivoSalvo,
            String productionResourceIdEsperado,
            String locationIdEsperado) {

        if (recursoProdutivoSalvo == null) {
            throw new IllegalStateException("Saved Production Resource snapshot is required.");
        }
        if (recursoProdutivoSalvo.getId() == null || recursoProdutivoSalvo.getId().isBlank()) {
            throw new IllegalStateException("Saved Production Resource id is required.");
        }
        if (!productionResourceIdEsperado.equals(recursoProdutivoSalvo.getId())) {
            throw new IllegalStateException("Saved Production Resource id must match requested id.");
        }
        if (recursoProdutivoSalvo.getLocation() == null
                || recursoProdutivoSalvo.getLocation().getId() == null
                || recursoProdutivoSalvo.getLocation().getId().isBlank()) {
            throw new IllegalStateException("Saved Production Resource location is required.");
        }
        if (!locationIdEsperado.equals(recursoProdutivoSalvo.getLocation().getId())) {
            throw new IllegalStateException("Saved Production Resource location must match requested location.");
        }

    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }

}
