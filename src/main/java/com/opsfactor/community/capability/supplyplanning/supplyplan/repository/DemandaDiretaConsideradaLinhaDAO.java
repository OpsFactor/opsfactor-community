package com.opsfactor.community.capability.supplyplanning.supplyplan.repository;

import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.DemandaDiretaConsideradaLinha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;

/**
 * DAO JDBC para persistencia em lote das linhas de demanda direta consideradas
 * pelo Supply Plan.
 * <p>
 * A tabela costuma receber uma fotografia completa por plano, com centenas de
 * milhares de linhas. Usar {@code JpaRepository.saveAll} nesse caso força o
 * Hibernate a fazer {@code merge} entidade a entidade em chaves compostas, o
 * que aumenta muito o custo de CPU e memoria. O batch JDBC mantém a mesma
 * semântica de upsert e reduz o trabalho para round-trips batched ao banco.
 */
@Repository
public class DemandaDiretaConsideradaLinhaDAO {

    /**
     * Tamanho do lote JDBC usado para delete/upsert da fotografia do plano.
     */
    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:1000}")
    private Integer batchSize;

    /**
     * Executor JDBC usado para delete e upsert em lote. Este DAO usa SQL
     * direto justamente para evitar N+1/merge entidade a entidade no volume de
     * linhas de demanda direta considerada.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Remove a fotografia completa de demanda direta considerada de um Supply
     * Plan antes de gravar uma nova versao do snapshot.
     */
    public void deleteBySupplyPlanId(Long supplyPlanId) {

        if (supplyPlanId == null) {
            throw new IllegalArgumentException(
                    "Supply Plan id is required to delete direct demand considered JDBC batch snapshot.");
        }

        jdbcTemplate.update(
                "DELETE FROM demanda_direta_considerada_linha WHERE supply_plan_id = ?",
                supplyPlanId);

    }

    /**
     * Persiste em lote as linhas de demanda direta considerada.
     *
     * <p>Lista vazia representa snapshot sem linhas a gravar e nao deve abrir
     * transacao ou round-trip desnecessario. Quando ha dados, validamos a
     * chave composta antes de preparar o batch para que falhas de projection
     * aparecam como contrato quebrado, nao como `NullPointerException` dentro
     * do setter JDBC. O Community publica PostgreSQL, portanto o DAO usa a
     * sintaxe de upsert desse motor diretamente.</p>
     */
    public void saveInBatch(Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhas) {

        validaDemandaDiretaConsideradaLinhasParaBatch(demandaDiretaConsideradaLinhas);

        if (demandaDiretaConsideradaLinhas.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                getSqlUpsertDemandaDiretaConsideradaLinha(),
                demandaDiretaConsideradaLinhas,
                batchSize,
                new ParameterizedPreparedStatementSetter<>() {
                    @Override
                    public void setValues(
                            PreparedStatement preparedStatement,
                            DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) throws SQLException {
                        setValores(preparedStatement, demandaDiretaConsideradaLinha);
                    }
                });
    }

    /**
     * Valida a colecao recebida pela borda JDBC antes de acessar `isEmpty()` ou
     * qualquer getter da chave composta.
     */
    private void validaDemandaDiretaConsideradaLinhasParaBatch(
            Collection<DemandaDiretaConsideradaLinha> demandaDiretaConsideradaLinhas) {

        if (demandaDiretaConsideradaLinhas == null) {
            throw new IllegalArgumentException(
                    "Direct demand considered JDBC batch collection is required.");
        }

        int indiceDemandaDiretaConsideradaLinha = 0;
        for (DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha : demandaDiretaConsideradaLinhas) {
            if (demandaDiretaConsideradaLinha == null) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " is required.");
            }
            DemandaDiretaConsideradaLinha.DemandaDiretaConsideradaLinhaCompositeKey compositeKey =
                    demandaDiretaConsideradaLinha.getDemandaDiretaConsideradaLinhaCompositeKey();
            if (compositeKey == null) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " must have a composite key.");
            }
            if (compositeKey.getSupplyPlan() == null || compositeKey.getSupplyPlan().getId() == null) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " must have a Supply Plan id.");
            }
            if (compositeKey.getLocation() == null
                    || compositeKey.getLocation().getId() == null
                    || compositeKey.getLocation().getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " must have a location id.");
            }
            if (compositeKey.getMaterial() == null
                    || compositeKey.getMaterial().getId() == null
                    || compositeKey.getMaterial().getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " must have a material id.");
            }
            if (compositeKey.getDataReferencia() == null) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " must have a reference date.");
            }
            if (demandaDiretaConsideradaLinha.getUnidadeMedidaCadastrado() != null
                    && (demandaDiretaConsideradaLinha.getUnidadeMedidaCadastrado().getId() == null
                    || demandaDiretaConsideradaLinha.getUnidadeMedidaCadastrado().getId().isBlank())) {
                throw new IllegalArgumentException(
                        "Direct demand considered JDBC batch line at index "
                                + indiceDemandaDiretaConsideradaLinha
                                + " must have a unit of measure id when unit of measure is explicitly provided.");
            }
            indiceDemandaDiretaConsideradaLinha++;
        }

    }

    /**
     * Retorna o upsert Community PostgreSQL. O Enterprise sobrescreve somente
     * este ponto para manter a infraestrutura JDBC comum e selecionar seu SQL
     * MySQL/MariaDB pelo bean primário, sem consultar metadata em runtime.
     */
    protected String getSqlUpsertDemandaDiretaConsideradaLinha() {

        return getSqlUpsertDemandaDiretaConsideradaLinhaPostgreSql();

    }

    /**
     * Upsert PostgreSQL. {@code excluded} representa a
     * linha do batch que colidiu com a chave composta já persistida.
     */
    private String getSqlUpsertDemandaDiretaConsideradaLinhaPostgreSql() {

        return """
                INSERT INTO demanda_direta_considerada_linha (
                    data_referencia,
                    quantidade_carteira_original,
                    quantidade_carteira_original_propagada_location_interna,
                    quantidade_demanda_direta_carteira_irrestrita,
                    quantidade_demanda_direta_carteira_restrita,
                    quantidade_demanda_direta_estoque_seguranca,
                    quantidade_demanda_direta_plano_demanda_irrestrita,
                    quantidade_demanda_direta_plano_demanda_restrita,
                    quantidade_plano_demanda_original,
                    quantidade_plano_demanda_original_propagada_location_interna,
                    location_id,
                    material_id,
                    supply_plan_id,
                    unidade_medida_id
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (
                    data_referencia,
                    location_id,
                    material_id,
                    supply_plan_id
                ) DO UPDATE SET
                    quantidade_carteira_original = excluded.quantidade_carteira_original,
                    quantidade_carteira_original_propagada_location_interna = excluded.quantidade_carteira_original_propagada_location_interna,
                    quantidade_demanda_direta_carteira_irrestrita = excluded.quantidade_demanda_direta_carteira_irrestrita,
                    quantidade_demanda_direta_carteira_restrita = excluded.quantidade_demanda_direta_carteira_restrita,
                    quantidade_demanda_direta_estoque_seguranca = excluded.quantidade_demanda_direta_estoque_seguranca,
                    quantidade_demanda_direta_plano_demanda_irrestrita = excluded.quantidade_demanda_direta_plano_demanda_irrestrita,
                    quantidade_demanda_direta_plano_demanda_restrita = excluded.quantidade_demanda_direta_plano_demanda_restrita,
                    quantidade_plano_demanda_original = excluded.quantidade_plano_demanda_original,
                    quantidade_plano_demanda_original_propagada_location_interna = excluded.quantidade_plano_demanda_original_propagada_location_interna,
                    unidade_medida_id = excluded.unidade_medida_id
                """;

    }

    private void setValores(
            PreparedStatement preparedStatement,
            DemandaDiretaConsideradaLinha demandaDiretaConsideradaLinha) throws SQLException {

        /*
         * A ordem dos parametros precisa acompanhar exatamente a ordem dos
         * placeholders do SQL PostgreSQL acima. Manter esse preenchimento em
         * metodo único impede divergência entre SQL e parâmetros JDBC.
         */
        preparedStatement.setTimestamp(1, Timestamp.valueOf(demandaDiretaConsideradaLinha.getDataReferencia()));
        preparedStatement.setObject(2, demandaDiretaConsideradaLinha.getQuantidadeCarteiraOriginal(), Types.DOUBLE);
        preparedStatement.setObject(3, demandaDiretaConsideradaLinha.getQuantidadeCarteiraOriginalPropagadaLocationInterna(), Types.DOUBLE);
        preparedStatement.setObject(4, demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaCarteiraIrrestrita(), Types.DOUBLE);
        preparedStatement.setObject(5, demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaCarteiraRestrita(), Types.DOUBLE);
        preparedStatement.setObject(6, demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaEstoqueSeguranca(), Types.DOUBLE);
        preparedStatement.setObject(7, demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaPlanoDemandaIrrestrita(), Types.DOUBLE);
        preparedStatement.setObject(8, demandaDiretaConsideradaLinha.getQuantidadeDemandaDiretaPlanoDemandaRestrita(), Types.DOUBLE);
        preparedStatement.setObject(9, demandaDiretaConsideradaLinha.getQuantidadePlanoDemandaOriginal(), Types.DOUBLE);
        preparedStatement.setObject(10, demandaDiretaConsideradaLinha.getQuantidadePlanoDemandaOriginalPropagadaLocationInterna(), Types.DOUBLE);
        preparedStatement.setObject(11, demandaDiretaConsideradaLinha.getLocation().getId(), Types.VARCHAR);
        preparedStatement.setObject(12, demandaDiretaConsideradaLinha.getMaterial().getId(), Types.VARCHAR);
        preparedStatement.setObject(13, demandaDiretaConsideradaLinha.getSupplyPlan().getId(), Types.BIGINT);
        preparedStatement.setObject(
                14,
                demandaDiretaConsideradaLinha.getUnidadeMedidaCadastrado() == null
                        ? null
                        : demandaDiretaConsideradaLinha.getUnidadeMedidaCadastrado().getId(),
                Types.VARCHAR);
    }
}
