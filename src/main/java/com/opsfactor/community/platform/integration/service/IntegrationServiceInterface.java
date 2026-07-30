package com.opsfactor.community.platform.integration.service;

import com.opsfactor.community.platform.integration.dto.IntegrationDataDtoAbstract;
import com.opsfactor.community.platform.integration.dto.IntegrationOptionsDto;
import com.opsfactor.community.platform.integration.dto.IntegrationPrimaryKeyDTOAbstract;
import com.opsfactor.community.platform.integration.mapper.IntegrationMapperInterface;

/**
 * Atalho para services de integracao que nao possuem options especificas.
 *
 * <p>O tipo ainda herda toda a infraestrutura de batch, suporte, filtros e
 * configuracao de campos da interface completa, mas fixa `IntegrationOptionsDto`
 * como objeto sem semantica adicional.</p>
 */
public interface IntegrationServiceInterface<DTO extends IntegrationDataDtoAbstract<DTO,PRIMARYKEYDTO,ENTITY>,PRIMARYKEYDTO extends IntegrationPrimaryKeyDTOAbstract<PRIMARYKEYDTO,ENTITY>,ENTITY,SUPPORTDATA,MAPPER extends IntegrationMapperInterface<DTO,PRIMARYKEYDTO,ENTITY,SUPPORTDATA>,DATAFILTER>
        extends IntegrationServiceComConfiguracoesInterface<DTO, PRIMARYKEYDTO, ENTITY,SUPPORTDATA,MAPPER,DATAFILTER,IntegrationOptionsDto> {

    /**
     * Implementação onde o método não gera ação alguma pois não há Options
     * @param dto dto recebido na integração sem options específicas
     * @param entity entidade já convertida a partir do dto
     * @param options options comuns da infraestrutura, ignoradas neste contrato sem options
     * @param supportData support data carregado para a integração
     */
    @Override
    public default void executaAcaoColateralOptionsNaConversaoDtoEmEntidade(DTO dto, ENTITY entity, IntegrationOptionsDto options, SUPPORTDATA supportData) {

        /*
         * Services que usam esta interface nao declaram options funcionais. O
         * no-op e intencional e evita que implementacoes simples precisem
         * repetir um metodo vazio.
         */

    }

}
