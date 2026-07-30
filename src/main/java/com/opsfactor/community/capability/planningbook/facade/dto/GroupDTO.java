package com.opsfactor.community.capability.planningbook.facade.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.EditMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Grupo hierarquico renderizado no Planning Book.
 *
 * <p>No Community o ultimo nivel sempre representa material/location. Niveis
 * agregados configuraveis, apresentacao por caracteristica e arvore de
 * agrupamento pertencem ao Enterprise; esta estrutura continua generica porque
 * o front compartilhado consome a mesma forma de resposta.</p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupDTO {

    // combinações de ID da característica -> valor
    // pode incluir colunas com código de material 'materialId' ou de location 'locationId'
    public Map<String,String> locationDescriptionCols;
    public Map<String,String> materialDescriptionCols;

    /**
     * Contexto tecnico opcional de um pai Enterprise. Folhas Community e
     * folhas Enterprise permanecem sem este campo e conservam material/location
     * como identidade direta.
     */
    public PlanningBookParentSelectionDTO parentSelection;
    
    // lista ordenada de key figures
    public List<KeyFigureDTOAbstract> keyFigures = new ArrayList<>(); //sempre será inicializado, independente do nível
    
    // nulo se for o nível mais detalhado (location + material)
    public List<GroupDTO> subGroups;
                
    /**
     * Primeiro se chama o próprio método recursivamente p/ atualização
     * de níveis inferiores e então se atualiza as key figures deste nível
     * com os valores agregados do nível imediatamente inferior
     * Ao se chegar no último nível não se faz nada (interrupção da recursão)
     * Depende dos mapas keyFigureIdToUOMMap e keyFigureIdToClickableMap
     */
    public void atualizaKeyFiguresComDadosSubGrupos() {
                
        if (subGroups != null && subGroups.size() > 0) {
            subGroups.stream().forEach(x -> {
                x.atualizaKeyFiguresComDadosSubGrupos();
            });
            
            // mapa que indica por ex. 'Demand' -> List<KeyFigureDTO>
            Map<String,List<KeyFigureDTOAbstract>> mapaKeyFigureDTOListSubGroupsPorTipoKeyFigure = subGroups.stream()
                    .map(GroupDTO::getKeyFigures)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.groupingBy(KeyFigureDTOAbstract::getKeyFigure, Collectors.toList()));
            
            // adiciona uma key figure agregada para cada id de 
            // key figure disponível nos subgrupos
            keyFigures.stream()
                    .forEach(keyFigureDTONivelAgregado -> {
                        String keyFigureId = keyFigureDTONivelAgregado.keyFigure;

                        List<KeyFigureDTOAbstract> keyFigureDTOListDesagregadosSubgroup = mapaKeyFigureDTOListSubGroupsPorTipoKeyFigure.get(keyFigureId);
                        if (keyFigureDTOListDesagregadosSubgroup.size() > 0) {
                            for (KeyFigureDTOAbstract keyFigureDTODesagregado : keyFigureDTOListDesagregadosSubgroup) {
                                keyFigureDTONivelAgregado.incorporaValoresTooltipsEClassesAdicionaisDeKeyFigure(keyFigureDTODesagregado);
                            }
                        }
                    });
        }
    }
    
    /**
     * Faz com que todas as key figures do grupo tenham editMode = NOEDIT
     */
    public void bloqueiaEdicao() {
        List<KeyFigureDTOAbstract> keyFigureDTOListNovo = new ArrayList<>();
        for (KeyFigureDTOAbstract keyFigureDTOAntigo : keyFigures) {
            // cria novo KeyFigureDTO para evitar que modificação em NOEDIT impacte o nível subgrupo (referência compartilhada)
            KeyFigureDTOAbstract keyFigureDTONovo = keyFigureDTOAntigo.getCopiaCompleta();
            // bloqueia edição
            keyFigureDTONovo.editMode = EditMode.NOEDIT;
            keyFigureDTOListNovo.add(keyFigureDTONovo);
        }
        keyFigures = keyFigureDTOListNovo;
    }
            
    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public boolean isMaterialLevel() {
        if (materialDescriptionCols == null) return false;
        return materialDescriptionCols.keySet().stream().anyMatch(x -> x.equals("materialId"));
    }
    
    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public boolean isLocationLevel() {
        if (locationDescriptionCols == null) return false;
        return locationDescriptionCols.keySet().stream().anyMatch(x -> x.equals("locationId"));
    }
    
    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public String getMaterialId() {
        if (materialDescriptionCols == null) return null;
        if (!materialDescriptionCols.containsKey("materialId")) return null;
        return materialDescriptionCols.get("materialId");
    }

    @JsonIgnore // necessário pois Jackson tenta serializar métodos e gera erro quando valor é nulo
    public String getLocationId() {
        if (locationDescriptionCols == null) return null;
        if (!locationDescriptionCols.containsKey("locationId")) return null;
        return locationDescriptionCols.get("locationId");
    }

    
}
