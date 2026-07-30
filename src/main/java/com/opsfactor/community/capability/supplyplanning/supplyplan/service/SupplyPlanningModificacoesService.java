package com.opsfactor.community.capability.supplyplanning.supplyplan.service;

import com.opsfactor.community.capability.supplyplanning.configuration.domain.PerfilExecucaoSupplyPlan;
import com.opsfactor.community.capability.masterdata.network.location.domain.Location;
import com.opsfactor.community.capability.masterdata.production.productionresource.domain.RecursoProdutivo;
import com.opsfactor.community.capability.masterdata.production.productionversion.domain.VersaoProducao;
import com.opsfactor.community.capability.masterdata.product.material.domain.Produto;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.domain.UnidadeMedida;
import com.opsfactor.community.capability.supplyplanning.productionplan.domain.ProductionPlanLinha;
import com.opsfactor.community.capability.supplyplanning.supplyplan.domain.SupplyPlan;
import com.opsfactor.community.capability.configuration.projection.parametros.ClusterEParametrosProjection;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.DFU;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.LocationProjectionFactory;
import com.opsfactor.community.capability.masterdata.demand.dfu.projection.MaterialProjectionFactory;
import com.opsfactor.community.capability.masterdata.inventory.inventorypolicy.projection.PoliticaEstoquesProjection;
import com.opsfactor.community.capability.masterdata.network.supplynetwork.projection.SupplyNetworkProjection;
import com.opsfactor.community.capability.masterdata.measurement.unitofmeasure.projection.UnidadeMedidaProjection;
import com.opsfactor.community.capability.planningbook.keyfigure.domain.KeyFigureStandardEnum;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanProjectionFactory;
import com.opsfactor.community.capability.supplyplanning.supplyplan.projection.SupplyPlanningProjection;
import com.opsfactor.community.platform.bi.AgregacaoDFU;
import com.opsfactor.community.capability.supplyplanning.engine.SupplyPlanning;
import com.opsfactor.community.capability.supplyplanning.service.SupplyPlanService;
import com.opsfactor.community.platform.utility.Constantes;
import org.apache.commons.compress.utils.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Community que persiste ajustes manuais do Supply Planning Book.
 *
 * <p>O recorte aqui permanece heuristico e material/location: modifica estoque,
 * inbound planejado, producao planejada e cobertura operacional sem acionar
 * optimizer, process chain, line scheduling ou contratos Enterprise.</p>
 */
@Service
public class SupplyPlanningModificacoesService {

    /**
     * Key figures que este service sabe transformar em alteracoes fisicas no
     * Supply Plan Community.
     *
     * <p>A borda de front valida que apenas KFs editaveis do `Working Plan`
     * chegam ao endpoint, mas este service tambem precisa falhar cedo quando
     * for chamado por teste, job interno ou payload transicional. A lista aqui
     * representa capacidade de persistencia, nao visibilidade de tela: por isso
     * `ESTOQUE_DIAS` aparece como forma operacional de converter cobertura em
     * quantidade, embora a UI Community atual bloqueie edicao direta dessa KF.</p>
     */
    private static final Set<KeyFigureStandardEnum> KEY_FIGURES_MODIFICAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY = Set.of(
            KeyFigureStandardEnum.ESTOQUE,
            KeyFigureStandardEnum.ESTOQUE_DIAS,
            KeyFigureStandardEnum.INBOUND_PLANEJADO,
            KeyFigureStandardEnum.PRODUCAO_PLANEJADA);

    /**
     * Service principal de Supply Planning usado para recalcular as DFUs
     * dependentes apos alteracoes manuais do Planning Book.
     */
    @Autowired
    private SupplyPlanService supplyPlanService;

    /**
     * Factory de projections de Supply Planning usada quando a alteracao
     * precisa reconstruir parte do snapshot de planejamento.
     */
    @Autowired
    private SupplyPlanProjectionFactory supplyPlanProjectionFactory;


    /**
     * Modifica o supply plan e atualiza a demanda indireta/estoque das DFUs dependentes
     */
    public void modificaSupplyPlan(
            Constantes.TipoPlano tipoPlano,
            KeyFigureStandardEnum keyFigure,
            double novoValor,
            double valorAntigo,
            UnidadeMedida unidadeMedidaValor,
            SupplyPlanningProjection supplyPlanningProjectionDestino,
            int posicaoPeriodoModificacao,
            Set<Produto> materiaisModificados) {

        modificaSupplyPlan(tipoPlano, keyFigure, novoValor, valorAntigo, unidadeMedidaValor,
                supplyPlanningProjectionDestino,
                Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL,
                posicaoPeriodoModificacao,
                materiaisModificados, null);

    }

    /**
     * Modifica o supply plan e atualiza a demanda indireta/estoque das DFUs dependentes
     * @param locationsOrigemConsideradasParaRequisicoes no caso de ajuste de requisições inbound, apenas as origens elencadas são consideradas
     */
    public void modificaSupplyPlan(
            Constantes.TipoPlano tipoPlano,
            KeyFigureStandardEnum keyFigure,
            double novoValor,
            double valorAntigo,
            UnidadeMedida unidadeMedidaValor,
            SupplyPlanningProjection supplyPlanningProjectionDestino,
            Constantes.ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodoModificacao,
            Set<Produto> materiaisModificados,
            Set<Location> locationsOrigemConsideradasParaRequisicoes) {

        validaKeyFigureModificavelSupplyPlanningBookCommunity(keyFigure);
        validaEscopoMateriaisModificadosSupplyPlanningBookCommunity(
                keyFigure,
                materiaisModificados);
        validaEntradasModificacaoSupplyPlanningBookCommunity(
                tipoPlano,
                keyFigure,
                unidadeMedidaValor,
                supplyPlanningProjectionDestino,
                referenciaPeriodo,
                posicaoPeriodoModificacao,
                materiaisModificados);

        PoliticaEstoquesProjection politicaEstoquesProjection = supplyPlanningProjectionDestino.getPoliticaEstoquesProjection();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlanningProjectionDestino.getPerfilExecucaoSupplyPlanConsiderado();

        ClusterEParametrosProjection clusterEParametrosProjection = supplyPlanningProjectionDestino.getClusterEParametrosProjection();

        double modificacaoSolicitada =
                Math.max(novoValor, 0) -
                        Math.max(valorAntigo, 0);

        switch (keyFigure) {
            case ESTOQUE:
                SupplyPlanning.modificaEstoqueTotalAgregado(
                        modificacaoSolicitada,
                        supplyPlanningProjectionDestino,
                        referenciaPeriodo, posicaoPeriodoModificacao,
                        materiaisModificados,
                        tipoPlano,
                        unidadeMedidaValor,
                        true, true,
                        locationsOrigemConsideradasParaRequisicoes); // pode-se ajustar tanto ordens de produção planejadas como requisições inbound planejadas
                break;
            case ESTOQUE_DIAS:
                Produto materialUnico = materiaisModificados.iterator().next();
                Location locationDestino = supplyPlanningProjectionDestino.getLocation();
                UnidadeMedida unidadeMedidaPadraoDestino = clusterEParametrosProjection.getSNPUnidadeMedidaPadrao(materialUnico, locationDestino);

                int posicaoPeriodoDisponibilizacaoMaterial = SupplyPlanning.getPeriodoPadraoParaPrimeiraDisponibilizacaoMaterial(
                        materialUnico,
                        locationDestino,
                        posicaoPeriodoModificacao,
                        supplyPlanningProjectionDestino.getCalendario(),
                        supplyPlanningProjectionDestino.getSupplyPlan().getVersaoMalha(),
                        true,
                        supplyPlanningProjectionDestino.getLocationProjectionLocationsOrigem(),
                        supplyPlanningProjectionDestino.getMaterialProjection(),
                        locationsOrigemConsideradasParaRequisicoes,
                        null,
                        supplyPlanningProjectionDestino.getSupplyNetworkProjection());

                double estoqueAnteriorEmQuantidade = SupplyPlanning.getEstoqueProjetado(
                        supplyPlanningProjectionDestino,
                        -1,
                        posicaoPeriodoDisponibilizacaoMaterial,
                        materialUnico,
                        tipoPlano,
                        unidadeMedidaPadraoDestino, true, true, false,
                        true);

                double novoEstoqueEmQuantidade = SupplyPlanning.getQuantidadeRelativaACoberturaEstoqueEmDias(
                        posicaoPeriodoDisponibilizacaoMaterial,
                        Math.max(0, novoValor),
                        locationDestino,
                        materialUnico,
                        tipoPlano,
                        unidadeMedidaPadraoDestino,
                        supplyPlanningProjectionDestino)
                        * ((modificacaoSolicitada >= 0) ? +1f : -1f);

                SupplyPlanning.modificaEstoqueTotalAgregado(
                        novoEstoqueEmQuantidade - estoqueAnteriorEmQuantidade,
                        supplyPlanningProjectionDestino,
                        referenciaPeriodo, posicaoPeriodoModificacao,
                        materiaisModificados,
                        tipoPlano,
                        unidadeMedidaPadraoDestino,
                        true, true,
                        locationsOrigemConsideradasParaRequisicoes); // pode-se ajustar tanto ordens de produção planejadas como requisições inbound planejadas
                break;
            case INBOUND_PLANEJADO:
                SupplyPlanning.modificaEstoqueTotalAgregado(
                        modificacaoSolicitada,
                        supplyPlanningProjectionDestino,
                        referenciaPeriodo, posicaoPeriodoModificacao,
                        materiaisModificados,
                        tipoPlano,
                        unidadeMedidaValor,
                        false, true,
                        locationsOrigemConsideradasParaRequisicoes); // pode-se ajustar somente requisições inbound planejadas
                break;
            case PRODUCAO_PLANEJADA:
                SupplyPlanning.modificaEstoqueTotalAgregado(
                        modificacaoSolicitada,
                        supplyPlanningProjectionDestino,
                        referenciaPeriodo, posicaoPeriodoModificacao,
                        materiaisModificados,
                        tipoPlano,
                        unidadeMedidaValor,
                        true, false,
                        locationsOrigemConsideradasParaRequisicoes); // pode-se ajustar somente ordens de produção planejadas
                break;
            default:
                throw getUnsupportedKeyFigureModificationException(keyFigure);
        }

        // salva planos distribuicao, producao e estoques
        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(supplyPlanningProjectionDestino, tipoPlano);
        SupplyPlanning.atualizaEstoqueSeguranca(supplyPlanningProjectionDestino, tipoPlano);
        if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
            SupplyPlanning.limitaEstoquesNegativosAZero(tipoPlano, supplyPlanningProjectionDestino);
        }
        // if evita que se salvem linhas desnecessariamente
        if (keyFigure.equals(KeyFigureStandardEnum.ESTOQUE) || keyFigure.equals(KeyFigureStandardEnum.INBOUND_PLANEJADO)) {
            supplyPlanService.saveDistributionPlanInboundDePlanningProjection(supplyPlanningProjectionDestino, referenciaPeriodo, posicaoPeriodoModificacao, true);
        }
        // if evita que se salvem linhas desnecessariamente
        if (keyFigure.equals(KeyFigureStandardEnum.ESTOQUE) || keyFigure.equals(KeyFigureStandardEnum.PRODUCAO_PLANEJADA)) {
            supplyPlanService.saveProductionPlanOutputDePlanningProjection(supplyPlanningProjectionDestino, posicaoPeriodoModificacao, true);
        }
        supplyPlanService.saveInventoryPlanDePlanningProjection(supplyPlanningProjectionDestino, true);

        // atualiza o inventory plan das DFUs dependentes (material/location de origem ou insumos de produção)
        atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                tipoPlano,
                supplyPlanningProjectionDestino.getLocation(),
                materiaisModificados,
                supplyPlanningProjectionDestino.getSupplyPlan(),
                perfilExecucaoSupplyPlan,
                supplyPlanningProjectionDestino.getSupplyNetworkProjection(),
                politicaEstoquesProjection,
                true, true, true);

    }

    /**
     * Valida limitacoes de escopo que nao dependem da projection carregada.
     *
     * <p>`Stock Days` e uma KF operacional do Community, mas a conversao de
     * cobertura em dias para quantidade depende da demanda do material ajustado.
     * Por isso o service aceita exatamente um material nessa rota e falha antes
     * de consultar projections quando o front/payload tenta fazer ajuste
     * agregado.</p>
     */
    private void validaEscopoMateriaisModificadosSupplyPlanningBookCommunity(
            KeyFigureStandardEnum keyFigure,
            Set<Produto> materiaisModificados) {

        if (keyFigure == KeyFigureStandardEnum.ESTOQUE_DIAS
                && (materiaisModificados == null || materiaisModificados.size() != 1)) {
            throw getUnsupportedStockDaysMaterialScopeException(materiaisModificados);
        }

    }

    /**
     * Valida argumentos comuns de ajuste antes de acessar a projection de
     * Supply Planning.
     *
     * <p>Key figure e escopo de `Stock Days` sao verificados antes deste metodo
     * para preservar a ordem funcional: payload Enterprise ou agregado deve
     * falhar antes de qualquer dependencia carregada. Aqui validamos apenas os
     * dados obrigatorios para um ajuste Community material/location.</p>
     */
    private void validaEntradasModificacaoSupplyPlanningBookCommunity(
            Constantes.TipoPlano tipoPlano,
            KeyFigureStandardEnum keyFigure,
            UnidadeMedida unidadeMedidaValor,
            SupplyPlanningProjection supplyPlanningProjectionDestino,
            Constantes.ReferenciaPeriodo referenciaPeriodo,
            int posicaoPeriodoModificacao,
            Set<Produto> materiaisModificados) {

        if (tipoPlano == null) {
            throw new IllegalArgumentException(
                    "Supply Planning target plan type is required for Community Planning Book modification.");
        }
        if (referenciaPeriodo == null) {
            throw new IllegalArgumentException(
                    "Supply Planning reference period is required for Community Planning Book modification.");
        }
        if (posicaoPeriodoModificacao < 0) {
            throw new IllegalArgumentException(
                    "Supply Planning modification period position must be non-negative for Community Planning Book modification.");
        }
        if (materiaisModificados == null) {
            throw new IllegalArgumentException(
                    "Modified material set is required for Community Supply Planning Book modification.");
        }
        if (materiaisModificados.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one modified material is required for Community Supply Planning Book modification.");
        }

        int indiceMaterialModificado = 0;
        for (Produto materialModificado : materiaisModificados) {
            if (materialModificado == null) {
                throw new IllegalArgumentException(
                        "Modified material at index "
                                + indiceMaterialModificado
                                + " is required for Community Supply Planning Book modification.");
            }
            if (materialModificado.getId() == null || materialModificado.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Modified material at index "
                                + indiceMaterialModificado
                                + " must have an id for Community Supply Planning Book modification.");
            }
            indiceMaterialModificado++;
        }

        if (unidadeMedidaValor == null) {
            throw new IllegalArgumentException(
                    "Unit of measure is required for Community Supply Planning Book modification of "
                            + keyFigure
                            + ".");
        }
        if (supplyPlanningProjectionDestino == null) {
            throw new IllegalArgumentException(
                    "Supply Planning projection is required for Community Planning Book modification.");
        }

    }

    private IllegalArgumentException getUnsupportedStockDaysMaterialScopeException(
            Set<Produto> materiaisModificados) {

        return new IllegalArgumentException(
                "SupplyPlanningModificacoesService can modify Stock Days for exactly one material/location at a time; received "
                        + (materiaisModificados == null ? "null" : materiaisModificados.size())
                        + " materials. Aggregated Stock Days adjustments must be blocked before projection loading.");

    }

    /**
     * Falha antes de montar projections ou persistir dados quando uma key
     * figure nao pertence ao contrato de modificacao deste service.
     */
    private void validaKeyFigureModificavelSupplyPlanningBookCommunity(
            KeyFigureStandardEnum keyFigure) {

        if (!KEY_FIGURES_MODIFICAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY.contains(keyFigure)) {
            throw getUnsupportedKeyFigureModificationException(keyFigure);
        }

    }

    private IllegalArgumentException getUnsupportedKeyFigureModificationException(
            KeyFigureStandardEnum keyFigure) {

        return new IllegalArgumentException(
                "SupplyPlanningModificacoesService can modify only Community operational Supply Planning Book key figures "
                        + KEY_FIGURES_MODIFICAVEIS_SUPPLY_PLANNING_BOOK_COMMUNITY
                        + "; received " + keyFigure
                        + ". Calculated, firm-order, in-transit, batch/writeoff and Enterprise key figures must be blocked before persistence.");

    }

    /**
     * Modifica o supply plan e atualiza a demanda indireta/estoque das DFUs dependentes
     * @param novoValor novo valor para ordens planejadas
     */
    public void modificaProductionPlanParaRecursoProdutivo(
            Constantes.TipoPlano tipoPlano,
            double novoValor,
            UnidadeMedida unidadeMedidaValor,
            SupplyPlanningProjection supplyPlanningProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            int posicaoPeriodoModificacao,
            RecursoProdutivo recursoProdutivo, Produto materialModificado) {

        validaEntradasModificacaoProductionPlanParaRecursoProdutivoCommunity(
                tipoPlano,
                unidadeMedidaValor,
                supplyPlanningProjection,
                politicaEstoquesProjection,
                posicaoPeriodoModificacao,
                recursoProdutivo,
                materialModificado);

        Location location = supplyPlanningProjection.getLocation();
        SupplyNetworkProjection supplyNetworkProjection = supplyPlanningProjection.getSupplyNetworkProjection();
        UnidadeMedidaProjection conversaoUnidadeMedidaProjection = supplyPlanningProjection.getConversaoUnidadeMedidaProjection();
        PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan = supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado();

        double valorAntigoMaterialRecursoProdutivo =
                supplyPlanningProjection.getQuantidadeProductionPlan(
                        posicaoPeriodoModificacao,
                        materialModificado,
                        recursoProdutivo,
                        tipoPlano,
                        Constantes.FirmePlanejado.PLANEJADO,
                        unidadeMedidaValor);

        Collection<ProductionPlanLinha> productionPlanLinhaList = supplyPlanningProjection.getProductionPlanLinhaOutput(posicaoPeriodoModificacao, recursoProdutivo, materialModificado);

        if (novoValor == valorAntigoMaterialRecursoProdutivo) return;

        if (novoValor < valorAntigoMaterialRecursoProdutivo) {

            // Community ajusta somente ordens planejadas. Ordens firmes/transacionais sao Enterprise e
            // nao entram no projection salvo ou editavel desta edicao.
            novoValor = Math.max(0, novoValor);
            if (valorAntigoMaterialRecursoProdutivo == 0) return;

            for (ProductionPlanLinha productionPlanLinha : productionPlanLinhaList) {
                productionPlanLinha.setQuantidade(
                        novoValor / valorAntigoMaterialRecursoProdutivo * productionPlanLinha.getQuantidade(
                                tipoPlano,
                                Constantes.FirmePlanejado.PLANEJADO,
                                unidadeMedidaValor,
                                conversaoUnidadeMedidaProjection),
                        tipoPlano,
                        Constantes.FirmePlanejado.PLANEJADO,
                        unidadeMedidaValor,
                        conversaoUnidadeMedidaProjection);
            }
            // novoValor > valorAntigo
        } else {

            // Community persiste ajustes produtivos apenas na versao prioritaria.
            // Roteiros paralelos e line scheduling voltam no Enterprise por overlay.
            boolean consideraVersoesProducaoParalelas = false;
            Optional<VersaoProducao> versaoProducao = supplyNetworkProjection.getVersaoProducaoViavelPrioritaria(
                    location,
                    materialModificado,
                    consideraVersoesProducaoParalelas,
                    null);

            VersaoProducao versaoProducaoPrioritaria = versaoProducao
                    .orElseThrow(() -> getMissingViableProductionVersionForResourceAdjustmentException(
                            materialModificado,
                            location,
                            recursoProdutivo));

            supplyPlanningProjection.modificaProductionPlan(
                    posicaoPeriodoModificacao,
                    materialModificado,
                    versaoProducaoPrioritaria,
                    (novoValor - valorAntigoMaterialRecursoProdutivo),
                    tipoPlano,
                    Constantes.FirmePlanejado.PLANEJADO,
                    unidadeMedidaValor);

        }


        // salva planos distribuicao, producao e estoques
        SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                supplyPlanningProjection,
                tipoPlano);
        SupplyPlanning.atualizaEstoqueSeguranca(
                supplyPlanningProjection,
                tipoPlano);
        if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
            SupplyPlanning.limitaEstoquesNegativosAZero(tipoPlano, supplyPlanningProjection);
        }
        supplyPlanService.saveDistributionPlanInboundDePlanningProjection(supplyPlanningProjection, Constantes.ReferenciaPeriodo.DISPONIBILIZACAO_MATERIAL, posicaoPeriodoModificacao, true);
        supplyPlanService.saveProductionPlanOutputDePlanningProjection(supplyPlanningProjection, posicaoPeriodoModificacao, true);
        supplyPlanService.saveInventoryPlanDePlanningProjection(supplyPlanningProjection, true);

        // atualiza o inventory plan das DFUs dependentes (material/location de origem ou insumos de produção)
        atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                tipoPlano,
                location,
                Sets.newHashSet(materialModificado),
                supplyPlanningProjection.getSupplyPlan(),
                perfilExecucaoSupplyPlan,
                supplyPlanningProjection.getSupplyNetworkProjection(),
                politicaEstoquesProjection,
                true, true, true);

    }

    /**
     * Valida a entrada do ajuste produtivo por recurso antes de acessar
     * projections e mapas de production plan.
     *
     * <p>Esse ajuste pertence ao Planning Book Community e sempre trabalha com
     * ordens planejadas do plano de trabalho material/location. Roteiros
     * paralelos, line scheduling, setup e demais decisões Enterprise continuam
     * fora deste service; por isso a entrada precisa identificar claramente o
     * Supply Plan salvo, a location, o material e o recurso produtivo alvo
     * antes de qualquer recalculo ou persistencia.</p>
     */
    private void validaEntradasModificacaoProductionPlanParaRecursoProdutivoCommunity(
            Constantes.TipoPlano tipoPlano,
            UnidadeMedida unidadeMedidaValor,
            SupplyPlanningProjection supplyPlanningProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            int posicaoPeriodoModificacao,
            RecursoProdutivo recursoProdutivo,
            Produto materialModificado) {

        if (tipoPlano == null) {
            throw new IllegalArgumentException(
                    "Supply Planning target plan type is required for Community production resource adjustment.");
        }
        if (unidadeMedidaValor == null) {
            throw new IllegalArgumentException(
                    "Unit of measure is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection == null) {
            throw new IllegalArgumentException(
                    "Supply Planning projection is required for Community production resource adjustment.");
        }
        if (politicaEstoquesProjection == null) {
            throw new IllegalArgumentException(
                    "Inventory policy projection is required for Community production resource adjustment.");
        }
        if (posicaoPeriodoModificacao < 0) {
            throw new IllegalArgumentException(
                    "Supply Planning modification period position must be non-negative for Community production resource adjustment.");
        }
        if (materialModificado == null
                || materialModificado.getId() == null
                || materialModificado.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Material with id is required for Community production resource adjustment.");
        }
        if (recursoProdutivo == null
                || recursoProdutivo.getId() == null
                || recursoProdutivo.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Productive resource with id is required for Community production resource adjustment.");
        }

        validaProjectionModificacaoProductionPlanParaRecursoProdutivoCommunity(
                supplyPlanningProjection);

    }

    /**
     * Valida a fotografia estrutural carregada na projection usada pelo ajuste
     * por recurso produtivo.
     */
    private void validaProjectionModificacaoProductionPlanParaRecursoProdutivoCommunity(
            SupplyPlanningProjection supplyPlanningProjection) {

        SupplyPlan supplyPlan = supplyPlanningProjection.getSupplyPlan();
        if (supplyPlan == null || supplyPlan.getId() == null) {
            throw new IllegalArgumentException(
                    "Persisted Supply Plan with id is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection.getLocation() == null
                || supplyPlanningProjection.getLocation().getId() == null
                || supplyPlanningProjection.getLocation().getId().isBlank()) {
            throw new IllegalArgumentException(
                    "Supply Planning projection location with id is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection.getPerfilExecucaoSupplyPlanConsiderado() == null) {
            throw new IllegalArgumentException(
                    "Supply Planning execution profile is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection.getSupplyNetworkProjection() == null) {
            throw new IllegalArgumentException(
                    "Supply Network projection is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection.getConversaoUnidadeMedidaProjection() == null) {
            throw new IllegalArgumentException(
                    "Unit of measure projection is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection.getClusterEParametrosProjection() == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection is required for Community production resource adjustment.");
        }
        if (supplyPlanningProjection.getClusterEParametrosProjection().getParametrosGlobais() == null) {
            throw new IllegalArgumentException(
                    "Global parameters are required for Community production resource adjustment.");
        }

    }

    private IllegalStateException getMissingViableProductionVersionForResourceAdjustmentException(
            Produto material,
            Location location,
            RecursoProdutivo recursoProdutivo) {

        return new IllegalStateException(
                "SupplyPlanningModificacoesService requires a viable simple production version before increasing "
                        + "planned production for a productive resource; material="
                        + getMaterialId(material)
                        + ", location="
                        + getLocationId(location)
                        + ", productive resource="
                        + getRecursoProdutivoId(recursoProdutivo)
                        + ". Community cannot create a production adjustment by resource without a concrete routing/BOM version.");

    }

    private String getMaterialId(Produto material) {

        if (material == null) {
            return "null";
        }

        return material.getId();

    }

    private String getLocationId(Location location) {

        if (location == null) {
            return "null";
        }

        return location.getId();

    }

    private String getRecursoProdutivoId(RecursoProdutivo recursoProdutivo) {

        if (recursoProdutivo == null) {
            return "null";
        }

        return recursoProdutivo.getId();

    }

    /*
     * Atualizacao de estoque projetado/safety stock das DFUs dependentes.
     *
     * Quando um ajuste manual altera recebimento, expedicao ou producao de uma
     * DFU, o Community precisa recalcular tambem os materiais/locations que
     * dependem dela pela malha ou pela lista tecnica. Este fluxo continua sendo
     * heuristico e local ao Supply Plan: nao abre process chain, Constraint
     * Tracker, reotimizacao nem diagnostico Enterprise de causa raiz.
     */
    public void atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
            Constantes.TipoPlano tipoPlano,
            Location location,
            Set<Produto> materiais,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            boolean salvaInventoryPlanZero,
            boolean consideraComoDependenciaInsumosEmListasTecnicas,
            boolean consideraComoDependenciaLocationsOrigemPossiveis) {

        atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
                tipoPlano, Sets.newHashSet(location), materiais, supplyPlan, perfilExecucaoSupplyPlan,
                supplyNetworkProjection, politicaEstoquesProjection, salvaInventoryPlanZero,
                consideraComoDependenciaInsumosEmListasTecnicas, consideraComoDependenciaLocationsOrigemPossiveis);

    }

    public void atualizaESalvaInventoryPlanLinhasDeDFUsDependentes(
            Constantes.TipoPlano tipoPlano,
            Set<Location> locations,
            Set<Produto> materiais,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            boolean salvaInventoryPlanZero,
            boolean consideraComoDependenciaInsumosEmListasTecnicas,
            boolean consideraComoDependenciaLocationsOrigemPossiveis) {

        validaEntradasAtualizacaoDFUsDependentesCommunity(
                tipoPlano,
                locations,
                materiais,
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyNetworkProjection,
                politicaEstoquesProjection);
        if (locations.isEmpty() || materiais.isEmpty()) {
            return;
        }

        Set<DFU> dfusDependentes = SupplyPlanning.getDfusDependentes(
                AgregacaoDFU.getDFUListDeProdutoCartesianoLocationMaterial(
                        locations,
                        materiais,
                        supplyNetworkProjection.getClusterEParametrosProjection()),
                supplyPlan.getVersaoMalha(),
                supplyPlan.getDataInicioPlano(),
                supplyNetworkProjection,
                consideraComoDependenciaInsumosEmListasTecnicas, consideraComoDependenciaLocationsOrigemPossiveis);

        atualizaESalvaInventoryPlanLinhasDeDFUs(
                tipoPlano, dfusDependentes, supplyPlan, perfilExecucaoSupplyPlan,
                supplyNetworkProjection, politicaEstoquesProjection, salvaInventoryPlanZero);

    }

    /**
     * Atualiza e salva estoque de segurança/estoque projetado de conjunto de DFUs
     * Não usa low level code para cálculo, então não deve haver interdependência entre DFUs
     * Extrai um demand/supply plan projection para cada location contida nas DFUs
     * @param dfus
     */
    public void atualizaESalvaInventoryPlanLinhasDeDFUs(
            Constantes.TipoPlano tipoPlano,
            Set<DFU> dfus,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection,
            boolean salvaInventoryPlanZero) {

        if (tipoPlano == null) {
            throw new IllegalArgumentException(
                    "Supply Planning target plan type is required to update dependent Community Inventory Plan lines.");
        }

        validaDFUsAtualizacaoInventoryPlanCommunity(dfus);
        if (dfus.isEmpty()) {
            return;
        }

        validaContextoAtualizacaoInventoryPlanCommunity(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyNetworkProjection,
                politicaEstoquesProjection);

        ClusterEParametrosProjection clusterEParametrosProjection = supplyNetworkProjection.getClusterEParametrosProjection();

        Set<Location> locations = dfus.stream().map(x -> x.getLocation()).collect(Collectors.toSet());

        for (Location location : locations) {
            Set<Produto> materiais = dfus.stream()
                    .filter(x -> x.getLocation().equals(location))
                    .map(x -> x.getProduto())
                    .collect(Collectors.toSet());

            SupplyPlanningProjection supplyPlanningProjection = supplyPlanProjectionFactory.getSupplyPlanningProjectionCompleto(
                    supplyPlan,
                    perfilExecucaoSupplyPlan,
                    location,
                    supplyNetworkProjection,
                    politicaEstoquesProjection,
                    MaterialProjectionFactory.getProjectionSetMateriais(materiais, clusterEParametrosProjection),
                    LocationProjectionFactory.getProjectionSetLocations(locations, clusterEParametrosProjection));

            SupplyPlanning.atualizaEstoqueSeguranca(
                    supplyPlanningProjection,
                    tipoPlano);
            SupplyPlanning.atualizaEstoqueProjetadoSemLimitarAZero(
                    supplyPlanningProjection,
                    tipoPlano);
            if (!perfilExecucaoSupplyPlan.getPermiteBacklogDemanda()) {
                SupplyPlanning.limitaEstoquesNegativosAZero(tipoPlano, supplyPlanningProjection);
            }

            if (perfilExecucaoSupplyPlan.getSalvaInventoryPlan()) {
                supplyPlanService.saveInventoryPlanDePlanningProjection(supplyPlanningProjection, salvaInventoryPlanZero);
            }

        }

    }

    /**
     * Valida a entrada usada para derivar DFUs dependentes de um ajuste manual.
     *
     * <p>O Community recalcula apenas o entorno fisico local afetado pelo
     * Planning Book. Locations/materiais vazios indicam que nao ha DFU de
     * origem para expandir e devem encerrar como no-op; colecao nula ou item
     * nulo indicam chamada quebrada e precisam falhar antes da montagem do
     * produto cartesiano.</p>
     */
    private void validaEntradasAtualizacaoDFUsDependentesCommunity(
            Constantes.TipoPlano tipoPlano,
            Set<Location> locations,
            Set<Produto> materiais,
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection) {

        if (tipoPlano == null) {
            throw new IllegalArgumentException(
                    "Supply Planning target plan type is required to update dependent Community Inventory Plan lines.");
        }

        validaLocationsOrigemAtualizacaoDFUsDependentesCommunity(locations);
        validaMateriaisOrigemAtualizacaoDFUsDependentesCommunity(materiais);
        if (locations.isEmpty() || materiais.isEmpty()) {
            return;
        }

        validaContextoAtualizacaoInventoryPlanCommunity(
                supplyPlan,
                perfilExecucaoSupplyPlan,
                supplyNetworkProjection,
                politicaEstoquesProjection);

    }

    /**
     * Valida locations de origem usadas para expandir DFUs dependentes.
     */
    private void validaLocationsOrigemAtualizacaoDFUsDependentesCommunity(
            Set<Location> locations) {

        if (locations == null) {
            throw new IllegalArgumentException(
                    "Dependent Inventory Plan origin location set is required for Community Planning Book recalculation.");
        }

        int indiceLocation = 0;
        for (Location location : locations) {
            if (location == null) {
                throw new IllegalArgumentException(
                        "Dependent Inventory Plan origin location at index "
                                + indiceLocation
                                + " is required for Community Planning Book recalculation.");
            }
            if (location.getId() == null || location.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Dependent Inventory Plan origin location at index "
                                + indiceLocation
                                + " must have an id for Community Planning Book recalculation.");
            }
            indiceLocation++;
        }

    }

    /**
     * Valida materiais de origem usados para expandir DFUs dependentes.
     */
    private void validaMateriaisOrigemAtualizacaoDFUsDependentesCommunity(
            Set<Produto> materiais) {

        if (materiais == null) {
            throw new IllegalArgumentException(
                    "Dependent Inventory Plan origin material set is required for Community Planning Book recalculation.");
        }

        int indiceMaterial = 0;
        for (Produto material : materiais) {
            if (material == null) {
                throw new IllegalArgumentException(
                        "Dependent Inventory Plan origin material at index "
                                + indiceMaterial
                                + " is required for Community Planning Book recalculation.");
            }
            if (material.getId() == null || material.getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Dependent Inventory Plan origin material at index "
                                + indiceMaterial
                                + " must have an id for Community Planning Book recalculation.");
            }
            indiceMaterial++;
        }

    }

    /**
     * Valida o conjunto final de DFUs que tera estoque/safety stock recalculado.
     */
    private void validaDFUsAtualizacaoInventoryPlanCommunity(
            Set<DFU> dfus) {

        if (dfus == null) {
            throw new IllegalArgumentException(
                    "Dependent DFU set is required for Community Inventory Plan recalculation.");
        }

        int indiceDFU = 0;
        for (DFU dfu : dfus) {
            if (dfu == null) {
                throw new IllegalArgumentException(
                        "Dependent DFU at index "
                                + indiceDFU
                                + " is required for Community Inventory Plan recalculation.");
            }
            if (dfu.getProduto() == null ||
                    dfu.getProduto().getId() == null ||
                    dfu.getProduto().getId().isBlank() ||
                    dfu.getLocation() == null ||
                    dfu.getLocation().getId() == null ||
                    dfu.getLocation().getId().isBlank()) {
                throw new IllegalArgumentException(
                        "Dependent DFU at index "
                                + indiceDFU
                                + " must have material and location ids for Community Inventory Plan recalculation.");
            }
            indiceDFU++;
        }

    }

    /**
     * Valida o snapshot estrutural minimo para recalcular Inventory Plan.
     */
    private void validaContextoAtualizacaoInventoryPlanCommunity(
            SupplyPlan supplyPlan,
            PerfilExecucaoSupplyPlan perfilExecucaoSupplyPlan,
            SupplyNetworkProjection supplyNetworkProjection,
            PoliticaEstoquesProjection politicaEstoquesProjection) {

        if (supplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Plan is required for Community Inventory Plan recalculation.");
        }
        if (perfilExecucaoSupplyPlan == null) {
            throw new IllegalArgumentException(
                    "Supply Planning execution profile is required for Community Inventory Plan recalculation.");
        }
        if (supplyNetworkProjection == null) {
            throw new IllegalArgumentException(
                    "Supply Network projection is required for Community Inventory Plan recalculation.");
        }
        if (supplyNetworkProjection.getClusterEParametrosProjection() == null) {
            throw new IllegalArgumentException(
                    "Cluster and parameters projection is required for Community Inventory Plan recalculation.");
        }
        if (politicaEstoquesProjection == null) {
            throw new IllegalArgumentException(
                    "Inventory policy projection is required for Community Inventory Plan recalculation.");
        }

    }
}
