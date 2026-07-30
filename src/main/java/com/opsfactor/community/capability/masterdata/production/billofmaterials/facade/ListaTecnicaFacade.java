package com.opsfactor.community.capability.masterdata.production.billofmaterials.facade;

import com.opsfactor.community.capability.configuration.domain.ParametrosGlobais;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnica;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.domain.ListaTecnicaComponente;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaComponenteRepository;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.repository.ListaTecnicaRepository;
import com.opsfactor.community.capability.configuration.service.ParametrosGlobaisService;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper.ListaTecnicaAutoMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.mapper.ListaTecnicaComponenteAutoMapper;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaComponenteDTO;
import com.opsfactor.community.capability.masterdata.production.billofmaterials.facade.dto.ListaTecnicaDTO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service front de listas tecnicas Community.
 *
 * <p>BOM e componentes sao master data operacional do Supply Planning
 * heuristico. Variantes paralelas, custos de componentes e line scheduling sao
 * tratados como Enterprise e nao sao expostos por esta camada.</p>
 */
@Service
public class ListaTecnicaFacade {

    /**
     * Repository das listas tecnicas operacionais.
     */
    @Autowired
    private ListaTecnicaRepository listaTecnicaRepository;

    /**
     * Repository dos componentes de lista tecnica.
     */
    @Autowired
    private ListaTecnicaComponenteRepository listaTecnicaComponenteRepository;

    /**
     * Mapper de BOM para DTO da tela.
     */
    @Autowired
    private ListaTecnicaAutoMapper listaTecnicaAutoMapper;

    /**
     * Mapper de componente de BOM para DTO da tela.
     */
    @Autowired
    private ListaTecnicaComponenteAutoMapper listaTecnicaComponenteAutoMapper;

    /**
     * Service de parametros globais usado para resolver unidade padrao e
     * formatacao operacional dos DTOs.
     */
    @Autowired
    private ParametrosGlobaisService parametrosGlobaisService;

    /**
     * Lista BOMs no contrato Community da tela.
     *
     * <p>O mapper precisa resolver a unidade de medida do output, usando a
     * unidade especifica da BOM ou a unidade padrao SNP de Parametros Globais.
     * Por isso validamos parametros e snapshot antes de montar DTO.</p>
     */
    public List<ListaTecnicaDTO> getListaTecnicaDTOList() {
        
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        validaParametrosGlobaisListaTecnicaCommunity(parametrosGlobais);
        List<ListaTecnica> listaTecnicaList =
                listaTecnicaRepository.customFindAllWithLocationMaterialOutputAndUnidadeMedidaMaterialOutput();
        validaListaTecnicaListCarregadaCommunity(listaTecnicaList, parametrosGlobais);
        
        List<ListaTecnicaDTO> listaTecnicaDTOList =
                listaTecnicaAutoMapper.converteListaEntidadeParaListaDTO(
                        listaTecnicaList, parametrosGlobais);
        validaListaTecnicaDTOListCarregadaCommunity(listaTecnicaDTOList);

        return listaTecnicaDTOList;

    }

    /**
     * Lista componentes de BOM no contrato Community da tela.
     *
     * <p>Componentes precisam de BOM, material componente e unidade resolvida
     * antes do mapper. Custos e modelos avancados de receita permanecem fora
     * desta borda Community.</p>
     */
    public List<ListaTecnicaComponenteDTO> getListaTecnicaComponenteDTOList() {
        
        ParametrosGlobais parametrosGlobais = parametrosGlobaisService.getParametrosGlobais();
        validaParametrosGlobaisListaTecnicaCommunity(parametrosGlobais);
        List<ListaTecnicaComponente> listaTecnicaComponenteList =
                listaTecnicaComponenteRepository.customFindAll();
        validaListaTecnicaComponenteListCarregadaCommunity(
                listaTecnicaComponenteList,
                parametrosGlobais);
        
        List<ListaTecnicaComponenteDTO> listaTecnicaComponenteDTOList =
                listaTecnicaComponenteAutoMapper.converteListaEntidadeParaListaDTO(
                        listaTecnicaComponenteList,
                        parametrosGlobais);
        validaListaTecnicaComponenteDTOListCarregadaCommunity(listaTecnicaComponenteDTOList);

        return listaTecnicaComponenteDTOList;
        
    }

    /**
     * Valida a existencia dos parametros globais usados pelos mappers de BOM.
     */
    private void validaParametrosGlobaisListaTecnicaCommunity(ParametrosGlobais parametrosGlobais) {

        if (parametrosGlobais == null) {
            throw new IllegalStateException("Global Parameters snapshot is required for Bill of Materials list.");
        }

    }

    /**
     * Valida o snapshot de BOMs antes de montar DTOs.
     *
     * <p>Lista vazia e valida. Cada BOM carregada precisa ter id, material
     * output, location e unidade de output resolvida, porque esses campos sao
     * a base funcional usada pelo Supply Planning heuristico.</p>
     */
    private void validaListaTecnicaListCarregadaCommunity(
            List<ListaTecnica> listaTecnicaList,
            ParametrosGlobais parametrosGlobais) {

        if (listaTecnicaList == null) {
            throw new IllegalStateException("Bill of Materials list snapshot is required.");
        }

        for (int index = 0; index < listaTecnicaList.size(); index++) {
            ListaTecnica listaTecnica = listaTecnicaList.get(index);

            if (listaTecnica == null) {
                throw new IllegalStateException(
                        "Bill of Materials at index " + index + " is required in list snapshot.");
            }
            if (listaTecnica.getId() == null || listaTecnica.getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials at index " + index + " has no id in list snapshot.");
            }
            if (listaTecnica.getLocation() == null
                    || listaTecnica.getLocation().getId() == null
                    || listaTecnica.getLocation().getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials at index " + index + " has no location in list snapshot.");
            }
            if (listaTecnica.getMaterialOutput() == null
                    || listaTecnica.getMaterialOutput().getId() == null
                    || listaTecnica.getMaterialOutput().getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials at index " + index + " has no output material in list snapshot.");
            }
            if (listaTecnica.getUnidadeMedidaMaterialOutput(parametrosGlobais) == null
                    || listaTecnica.getUnidadeMedidaMaterialOutput(parametrosGlobais).getId() == null
                    || listaTecnica.getUnidadeMedidaMaterialOutput(parametrosGlobais).getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials at index " + index + " has no output unit of measure in list snapshot.");
            }
        }

    }

    /**
     * Valida o snapshot de componentes de BOM antes de montar DTOs.
     *
     * <p>O componente e identificado por BOM + material componente. A unidade
     * de consumo pode vir do proprio componente ou do padrao SNP dos parametros
     * globais, mas precisa estar resolvida antes de sair para o front.</p>
     */
    private void validaListaTecnicaComponenteListCarregadaCommunity(
            List<ListaTecnicaComponente> listaTecnicaComponenteList,
            ParametrosGlobais parametrosGlobais) {

        if (listaTecnicaComponenteList == null) {
            throw new IllegalStateException("Bill of Materials Component list snapshot is required.");
        }

        for (int index = 0; index < listaTecnicaComponenteList.size(); index++) {
            ListaTecnicaComponente listaTecnicaComponente = listaTecnicaComponenteList.get(index);

            if (listaTecnicaComponente == null) {
                throw new IllegalStateException(
                        "Bill of Materials Component at index " + index + " is required in list snapshot.");
            }
            if (listaTecnicaComponente.getListaTecnicaComponenteCompositeKey() == null
                    || listaTecnicaComponente.getListaTecnica() == null
                    || listaTecnicaComponente.getListaTecnica().getId() == null
                    || listaTecnicaComponente.getListaTecnica().getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials Component at index " + index + " has no bill of materials in list snapshot.");
            }
            if (listaTecnicaComponente.getMaterialComponente() == null
                    || listaTecnicaComponente.getMaterialComponente().getId() == null
                    || listaTecnicaComponente.getMaterialComponente().getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials Component at index " + index + " has no component material in list snapshot.");
            }
            if (listaTecnicaComponente.getUnidadeMedidaMaterialComponente(parametrosGlobais) == null
                    || listaTecnicaComponente.getUnidadeMedidaMaterialComponente(parametrosGlobais).getId() == null
                    || listaTecnicaComponente.getUnidadeMedidaMaterialComponente(parametrosGlobais).getId().isBlank()) {
                throw new IllegalStateException(
                        "Bill of Materials Component at index " + index + " has no component unit of measure in list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de BOM devolvida pelo mapper.
     *
     * <p>A entidade ja foi validada antes do mapper, mas a SPA consome apenas
     * este DTO depois da listagem. Um mapper quebrado nao pode devolver lista
     * nula, item nulo ou perder os campos estruturais usados pelo cadastro e
     * pelo heuristico.</p>
     */
    private void validaListaTecnicaDTOListCarregadaCommunity(
            List<ListaTecnicaDTO> listaTecnicaDTOList) {

        if (listaTecnicaDTOList == null) {
            throw new IllegalStateException("Bill of Materials DTO list snapshot is required.");
        }

        for (int index = 0; index < listaTecnicaDTOList.size(); index++) {
            ListaTecnicaDTO listaTecnicaDTO = listaTecnicaDTOList.get(index);

            if (listaTecnicaDTO == null) {
                throw new IllegalStateException(
                        "Bill of Materials DTO at index " + index + " is required in list snapshot.");
            }
            if (isBlank(listaTecnicaDTO.getId())) {
                throw new IllegalStateException(
                        "Bill of Materials DTO at index " + index + " has no id in list snapshot.");
            }
            if (isBlank(listaTecnicaDTO.getOutputMaterialId())) {
                throw new IllegalStateException(
                        "Bill of Materials DTO at index " + index + " has no output material in list snapshot.");
            }
            if (isBlank(listaTecnicaDTO.getOutputUnitOfMeasureId())) {
                throw new IllegalStateException(
                        "Bill of Materials DTO at index " + index + " has no output unit of measure in list snapshot.");
            }
            if (listaTecnicaDTO.getOutputQuantity() == null
                    || !Float.isFinite(listaTecnicaDTO.getOutputQuantity())) {
                throw new IllegalStateException(
                        "Bill of Materials DTO at index " + index + " has no finite output quantity in list snapshot.");
            }
        }

    }

    /**
     * Valida a fotografia DTO de componentes de BOM devolvida pelo mapper.
     */
    private void validaListaTecnicaComponenteDTOListCarregadaCommunity(
            List<ListaTecnicaComponenteDTO> listaTecnicaComponenteDTOList) {

        if (listaTecnicaComponenteDTOList == null) {
            throw new IllegalStateException("Bill of Materials Component DTO list snapshot is required.");
        }

        for (int index = 0; index < listaTecnicaComponenteDTOList.size(); index++) {
            ListaTecnicaComponenteDTO listaTecnicaComponenteDTO =
                    listaTecnicaComponenteDTOList.get(index);

            if (listaTecnicaComponenteDTO == null) {
                throw new IllegalStateException(
                        "Bill of Materials Component DTO at index " + index + " is required in list snapshot.");
            }
            if (isBlank(listaTecnicaComponenteDTO.getBillOfMaterialsId())) {
                throw new IllegalStateException(
                        "Bill of Materials Component DTO at index " + index + " has no bill of materials in list snapshot.");
            }
            if (isBlank(listaTecnicaComponenteDTO.getComponentMaterialId())) {
                throw new IllegalStateException(
                        "Bill of Materials Component DTO at index " + index + " has no component material in list snapshot.");
            }
            if (isBlank(listaTecnicaComponenteDTO.getComponentMaterialUnitOfMeasureId())) {
                throw new IllegalStateException(
                        "Bill of Materials Component DTO at index " + index + " has no component unit of measure in list snapshot.");
            }
            if (listaTecnicaComponenteDTO.getQuantity() == null
                    || !Float.isFinite(listaTecnicaComponenteDTO.getQuantity())) {
                throw new IllegalStateException(
                        "Bill of Materials Component DTO at index " + index + " has no finite quantity in list snapshot.");
            }
        }

    }

    private boolean isBlank(String value) {

        return value == null || value.isBlank();

    }

}
