package com.opsfactor.community.capability.supplyplanning.configuration.domain.optimizer.presetconstraint;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cabeçalho compartilhado de um conjunto de preset constraints de Supply
 * Planning.
 *
 * <p>O Community persiste somente a identidade e a descrição do grupo para
 * que {@code SupplyPlan} mantenha uma relação JPA única e unidirecional. As
 * regras, filhos e execução de otimização continuam sendo uma capacidade
 * Enterprise; por isso este aggregate não expõe coleção inversa nem cascata
 * para entidades privadas.</p>
 */
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class RestricaoPredefinidaGrupo implements Serializable {

    @Id
    private String id;

    /**
     * Rótulo administrativo do grupo, persistido no mesmo aggregate
     * compartilhado sem materializar suas regras Enterprise.
     */
    private String description;

}
