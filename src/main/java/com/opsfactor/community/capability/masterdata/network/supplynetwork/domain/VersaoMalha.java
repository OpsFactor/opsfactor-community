package com.opsfactor.community.capability.masterdata.network.supplynetwork.domain;

import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Versao da malha de transporte usada para selecionar linhas de abastecimento.
 *
 * <p>O plano heuristico Community consome essa versao para resolver origens,
 * destinos, prioridades e lead times basicos. Analises de rede, network flows,
 * greenfield/brownfield e visualizacoes geograficas ficam restritas ao
 * Enterprise.</p>
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class VersaoMalha implements Serializable {

    @Id
    private String id;

    private String descricao;
    
    public VersaoMalha(String id) {
        this.id = id;
    }

    @ToString.Exclude
    @OneToMany(mappedBy = "linhaTransporteCompositeKey.versaoMalha", orphanRemoval = true)
    private Set<LinhaTransporte> linhaTransporteSet = new HashSet<>();

    @ManyToOne
    private Location locationOrigemPadraoClientes;

    /**
     * Location usada como origem temporaria de abastecimento para materias-primas
     * sem cadastro produtivo interno e sem linha de transporte inbound elegivel.
     */
    @ManyToOne
    private Location locationOrigemPadraoMateriasPrimas;

    /**
     * Lead time, em dias, aplicado nas linhas temporarias criadas a partir da
     * origem padrao de materias-primas.
     */
    private Double leadTimeDiasLocationOrigemPadraoMateriasPrimas;

}
