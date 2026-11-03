package com.opsfactor.community.capability.configuration.service.cluster;
import com.opsfactor.community.capability.configuration.domain.cluster.PerfilClusterizacao;
import com.opsfactor.community.platform.exception.RequiresEnterpriseVersionException;
import com.opsfactor.community.capability.cluster.domain.produto.*;
import com.opsfactor.community.capability.cluster.domain.location.*;
import com.opsfactor.community.capability.configuration.domain.cluster.location.ParametrosClusterLocations;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * Cadastro/versionamento da definição conjunta de clusters. Histórico nunca é
 * reatribuído: uma versão consumida só pode originar outra versão por clone.
 * As consultas de uso percorrem relações mapeadas, não uma lista parcial de
 * cabeçalhos modernos; assim FKs antigas de Pricing/Workflow também protegem a origem.
 */
@Service
@RequiredArgsConstructor
public class PerfilClusterizacaoService {

    private final EntityManager entityManager;

    public record PerfilDTO(String id, String description, Long version, boolean readOnly,
            List<String> usageSources, long materialClusters, long locationClusters) { }
    public record SaveRequest(String id, String description, Long version) { }
    public record CloneRequest(String sourceId, String id, String description) { }
    public enum Action { CREATE_EMPTY, CREATE_SINGLE, UPDATE, CLONE, DELETE }
    public record Mutation(Action action, String id, String description, Long version, String sourceId) { }

    /**
     * O Community possui uma única definição operacional. O overlay Enterprise
     * reabre o versionamento neste ponto, sem duplicar leitura, locks ou caches.
     */
    protected boolean supportsVersionedProfiles() {

        return false;

    }

    /**
     * Normaliza a ausência enviada por telas e MCP para o perfil inicial.
     * A edição aberta rejeita outro identificador antes de acessar dados, pois
     * esconder o seletor não deve permitir seleção manual pela API.
     */
    public String resolveProfileId(String requestedProfileId) {

        if (requestedProfileId == null || requestedProfileId.isBlank()) {
            return PerfilClusterizacao.ID_LEGADO;
        }
        validateId(requestedProfileId);
        if (!supportsVersionedProfiles() && !PerfilClusterizacao.ID_LEGADO.equals(requestedProfileId)) {
            throw new RequiresEnterpriseVersionException("Versioned clustering profiles");
        }
        return requestedProfileId;

    }

    /** Guarda administrativa comum às operações privadas de versionamento. */
    private void requireVersionedProfiles() {

        if (!supportsVersionedProfiles()) {
            throw new RequiresEnterpriseVersionException("Versioned clustering profiles");
        }

    }

    /** Mesmas operações de cadastro/versionamento em REST, Vue e MCP; não gera forecasts. */
    @Transactional
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public List<PerfilDTO> mutate(Mutation request) {

        requireVersionedProfiles();

        if (request == null || request.action() == null) throw new IllegalArgumentException("Informe a operação do perfil.");
        validateId(request.id());
        switch (request.action()) {
            case CREATE_EMPTY -> {
                if (entityManager.find(PerfilClusterizacao.class, request.id()) != null)
                    throw new IllegalArgumentException("O perfil de destino já existe.");
                save(new SaveRequest(request.id(), request.description(), null));
            }
            case CREATE_SINGLE -> createSingle(new SaveRequest(request.id(), request.description(), null));
            case UPDATE -> {
                required(request.id(), LockModeType.PESSIMISTIC_WRITE);
                save(new SaveRequest(request.id(), request.description(), request.version()));
            }
            case CLONE -> cloneProfile(new CloneRequest(request.sourceId(), request.id(), request.description()));
            case DELETE -> {
                if (request.version() == null) throw new IllegalArgumentException("Informe a versão a excluir.");
                delete(request.id(), request.version());
            }
        }
        entityManager.flush();
        return list();

    }


    /**
     * Número de consultas depende das relações do modelo, nunca da quantidade de
     * perfis. Cada relação retorna todos os perfis referenciados em uma consulta.
     * Não navega entidades/coleções lazy para descobrir consumidores.
     */
    @Transactional(readOnly = true)
    public List<PerfilDTO> list() {

        requireVersionedProfiles();

        Map<String, Set<String>> usages = usages();
        Map<String, Long> materials = counts("ClusterProdutos");
        Map<String, Long> locations = counts("ClusterLocations");
        return entityManager.createQuery("select p from PerfilClusterizacao p order by p.id", PerfilClusterizacao.class)
                .getResultList().stream().map(profile -> dto(profile, usages,
                        materials.getOrDefault(profile.getId(), 0L), locations.getOrDefault(profile.getId(), 0L))).toList();

    }

    /** Cria uma definição vazia ou atualiza descrição de uma versão ainda não consumida. */
    @Transactional
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public PerfilDTO save(SaveRequest request) {

        requireVersionedProfiles();

        validateId(request.id());
        PerfilClusterizacao profile = entityManager.find(PerfilClusterizacao.class, request.id(), LockModeType.PESSIMISTIC_WRITE);
        if (profile == null) {
            if (request.version() != null) throw new IllegalArgumentException("Versão inexistente; recarregue o cadastro.");
            if (PerfilClusterizacao.ID_LEGADO.equals(request.id())) throw new IllegalArgumentException("O perfil padrão é reservado à inicialização da plataforma.");
            profile = new PerfilClusterizacao(request.id(), request.description());
            entityManager.persist(profile);
        } else {
            requireEditable(profile);
            if (request.version() == null || request.version() != profile.getVersion())
                throw new IllegalArgumentException("A versão do perfil mudou; recarregue antes de salvar.");
            profile.setDescricao(request.description());
        }
        entityManager.flush();
        return dto(profile, Map.of(), counts("ClusterProdutos").getOrDefault(profile.getId(), 0L), counts("ClusterLocations").getOrDefault(profile.getId(), 0L));

    }

    /**
     * Alternativa explícita ao perfil multi-cluster: cria um padrão de material
     * e outro de location. Não copia parâmetros/ganhadores AutoFit de outro escopo.
     * A criação ocorre no cadastro; nenhum cálculo cria defaults ocultamente.
     */
    @Transactional
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public PerfilDTO createSingle(SaveRequest request) {

        requireVersionedProfiles();

        PerfilClusterizacao profile = newProfile(request.id(), request.description());
        ClusterProdutos material = new ClusterMateriais("All materials", true, 0);
        material.setPerfilClusterizacao(profile);
        ClusterLocations location = new ClusterLocations("All locations", true, 0);
        location.setPerfilClusterizacao(profile);
        // Default cadastrado nesta criação, nunca durante a leitura do cálculo.
        location.getParametrosClusterLocations();
        entityManager.persist(material);
        entityManager.persist(location);
        entityManager.flush();
        return dto(profile, Map.of(), 1, 1);

    }

    /**
     * Copia somente definição: clusters, regras, filhos próprios e características
     * principais. Novas identidades são alocadas antes de inserir em Sets cujos
     * equals dependem de ID, evitando colapsar duas regras novas com ID null.
     * Cada coleção é inicializada por consulta separada, sem produto cartesiano.
     * Persists permanecem na mesma unidade de trabalho para batching Hibernate;
     * não há save/flush por entidade e não são copiadas receitas/históricos.
     */
    @Transactional
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public PerfilDTO cloneProfile(CloneRequest request) {

        requireVersionedProfiles();

        PerfilClusterizacao source = required(request.sourceId(), LockModeType.PESSIMISTIC_READ);
        PerfilClusterizacao target = newProfile(request.id(), request.description());
        List<ClusterProdutos> materials = entityManager.createQuery(
                "select c from ClusterProdutos c where c.perfilClusterizacao.id=:profile", ClusterProdutos.class)
                .setParameter("profile", source.getId()).getResultList();
        List<ClusterLocations> locations = entityManager.createQuery(
                "select c from ClusterLocations c where c.perfilClusterizacao.id=:profile", ClusterLocations.class)
                .setParameter("profile", source.getId()).getResultList();
        Map<Long, ClusterProdutos> materialCopies = new HashMap<>();
        Map<Long, ClusterLocations> locationCopies = new HashMap<>();
        for (ClusterProdutos original : materials) {
            ClusterProdutos copy;
            try { copy = (ClusterProdutos) Hibernate.getClass(original).getDeclaredConstructor().newInstance(); }
            catch (ReflectiveOperationException exception) { throw new IllegalStateException("Tipo de cluster não clonável.", exception); }
            copy.setPerfilClusterizacao(target); copy.setDescricao(original.getDescricao());
            copy.setPadrao(original.getPadrao()); copy.setPrioridade(original.getPrioridade());
            entityManager.persist(copy); materialCopies.put(original.getId(), copy);
        }
        for (ClusterLocations original : locations) {
            ClusterLocations copy = new ClusterLocations(original.getDescricao(), original.getPadrao(), original.getPrioridade());
            copy.setPerfilClusterizacao(target); entityManager.persist(copy); locationCopies.put(original.getId(), copy);
        }
        var materialRules = entityManager.createQuery("select r from RegraAlocacaoClusterProdutos r where r.clusterProdutos.perfilClusterizacao.id=:profile", RegraAlocacaoClusterProdutos.class)
                .setParameter("profile", source.getId()).getResultList();
        fetch("RegraAlocacaoClusterProdutos", "clusterProdutos", "regraAlocacaoClusterProdutosStatusSet", source.getId());
        fetch("RegraAlocacaoClusterProdutos", "clusterProdutos", "regrasAlocacaoClusterProdutosCaracteristicaSet", source.getId());
        for (RegraAlocacaoClusterProdutos original : materialRules) {
            var copy = new RegraAlocacaoClusterProdutos();
            copy.setClusterProdutos(materialCopies.get(original.getClusterProdutos().getId()));
            copy.setRegraAlocacaoTipo(original.getRegraAlocacaoTipo());
            copy.setPrioridade(original.getPrioridade());
            entityManager.persist(copy);
            for (var status : original.getStatusProdutoSet()) copy.addStatusProduto(status);
            for (var characteristic : original.getRegrasAlocacaoClusterProdutosCaracteristicaSet()) {
                copy.addRegraAlocacaoCaracteristica(new RegraAlocacaoClusterProdutosCaracteristica(
                        new RegraAlocacaoClusterProdutosCaracteristica.RegraAlocacaoClusterProdutosCaracteristicaCompositeKey(copy, characteristic.getCaracteristica(), characteristic.getAtributo())));
            }
        }
        var locationRules = entityManager.createQuery("select r from RegraAlocacaoClusterLocations r where r.clusterLocations.perfilClusterizacao.id=:profile", RegraAlocacaoClusterLocations.class)
                .setParameter("profile", source.getId()).getResultList();
        for (String collection : List.of("regrasAlocacaoClusterLocationsPaisEstadoSet", "regrasAlocacaoClusterLocationsTipoLocationSet"))
            fetch("RegraAlocacaoClusterLocations", "clusterLocations", collection, source.getId());
        for (RegraAlocacaoClusterLocations original : locationRules) {
            var copy = new RegraAlocacaoClusterLocations();
            copy.setClusterLocations(locationCopies.get(original.getClusterLocations().getId()));
            copy.setRegraAlocacaoTipo(original.getRegraAlocacaoTipo()); copy.setPrioridade(original.getPrioridade());
            entityManager.persist(copy);
            for (var item : original.getRegrasAlocacaoClusterLocationsPaisEstadoSet())
                copy.addRegraAlocacaoPaisEstado(new RegraAlocacaoClusterLocationsPaisEstado(new RegraAlocacaoClusterLocationsPaisEstado.RegraAlocacaoClusterLocationsPaisEstadoCompositeKey(copy, item.getPais(), item.getEstado())));
            for (var item : original.getRegrasAlocacaoClusterLocationsTipoLocationSet())
                copy.addRegraAlocacaoTipoLocation(new RegraAlocacaoClusterLocationsTipoLocation(new RegraAlocacaoClusterLocationsTipoLocation.RegraAlocacaoClusterLocationsTipoLocationCompositeKey(copy, item.getTipoLocation())));
        }
        cloneAdditionalDefinition(source.getId(), materialCopies, locationCopies);
        entityManager.flush();
        return dto(target, Map.of(), materials.size(), locations.size());

    }

    /** Somente cadastro vazio pode ser removido; os clusters são editados em seu próprio fluxo. */
    @Transactional
    @CacheEvict(value = {"cadastroEParametrosProjection", "clusterizacaoProjection", "supplyNetworkProjection"}, allEntries = true)
    public void delete(String profileId, long version) {

        requireVersionedProfiles();

        var profile = required(profileId, LockModeType.PESSIMISTIC_WRITE);
        requireEditable(profile);
        if (profile.getVersion() != version) throw new IllegalArgumentException("Versão divergente; recarregue o cadastro.");
        if (counts("ClusterProdutos").containsKey(profileId) || counts("ClusterLocations").containsKey(profileId))
            throw new IllegalArgumentException("Remova os clusters da versão não usada antes de excluir o perfil.");
        entityManager.remove(profile);

    }

    /** Inicializa filhos de regras em consultas por coleção, compartilhadas por todos os clusters do perfil. */
    @Transactional(readOnly = true)
    public void initializeDefinition(String profileId) {

        profileId = resolveProfileId(profileId);
        required(profileId, LockModeType.NONE);
        fetch("RegraAlocacaoClusterProdutos", "clusterProdutos", "regraAlocacaoClusterProdutosStatusSet", profileId);
        fetch("RegraAlocacaoClusterProdutos", "clusterProdutos", "regrasAlocacaoClusterProdutosCaracteristicaSet", profileId);
        for (String collection : List.of("regrasAlocacaoClusterLocationsPaisEstadoSet", "regrasAlocacaoClusterLocationsTipoLocationSet"))
            fetch("RegraAlocacaoClusterLocations", "clusterLocations", collection, profileId);

    }

    /** Mesma barreira deve proteger salvar/remover regras pelas rotas de cluster. */
    @Transactional
    public void requireEditable(String profileId) {

        PerfilClusterizacao profile = required(resolveProfileId(profileId), LockModeType.PESSIMISTIC_WRITE);
        // A única definição Community continua editável; só o registro padrão
        // e os clusters default são protegidos contra exclusão. A edição
        // Enterprise mantém a barreira histórica de versões consumidas.
        if (supportsVersionedProfiles()) {
            requireEditable(profile);
        }

    }

    private void requireEditable(PerfilClusterizacao profile) {

        if (PerfilClusterizacao.ID_LEGADO.equals(profile.getId()) || usages().containsKey(profile.getId()))
            throw new IllegalArgumentException("O perfil é padrão ou já foi utilizado; clone para outro identificador antes de editar.");

    }

    private PerfilClusterizacao newProfile(String id, String description) {

        validateId(id);
        if (PerfilClusterizacao.ID_LEGADO.equals(id) || entityManager.find(PerfilClusterizacao.class, id) != null)
            throw new IllegalArgumentException("O identificador de destino é reservado ou já existe.");
        var target = new PerfilClusterizacao(id, description); entityManager.persist(target); return target;

    }

    private PerfilClusterizacao required(String id, LockModeType lockMode) {

        validateId(id);
        var profile = entityManager.find(PerfilClusterizacao.class, id, lockMode);
        if (profile == null) throw new IllegalArgumentException("Perfil de clusterização inexistente: " + id);
        return profile;

    }

    private static void validateId(String id) {

        if (id == null || id.isBlank() || id.length() > 50 || !id.equals(id.trim()))
            throw new IllegalArgumentException("Informe ID de perfil sem espaços nas pontas, com até 50 caracteres.");

    }

    private static PerfilDTO dto(PerfilClusterizacao profile, Map<String, Set<String>> usages, long materials, long locations) {

        return new PerfilDTO(profile.getId(), profile.getDescricao(), profile.getVersion(),
                PerfilClusterizacao.ID_LEGADO.equals(profile.getId()) || usages.containsKey(profile.getId()),
                usages.getOrDefault(profile.getId(), Set.of()).stream().sorted().toList(), materials, locations);

    }

    private Map<String, Long> counts(String entityName) {

        Map<String, Long> result = new HashMap<>();
        for (Object[] row : entityManager.createQuery("select c.perfilClusterizacao.id, count(c) from " + entityName + " c group by c.perfilClusterizacao.id", Object[].class).getResultList())
            result.put((String) row[0], ((Number) row[1]).longValue());
        return result;

    }

    private void fetch(String entityName, String clusterProperty, String collection, String profileId) {

        entityManager.createQuery("select distinct r from " + entityName + " r left join fetch r." + collection
                + " where r." + clusterProperty + ".perfilClusterizacao.id=:profile")
                .setParameter("profile", profileId).getResultList();

    }

    /** Definições próprias não caracterizam consumo. Dados/receitas de outros processos sim. */
    private Map<String, Set<String>> usages() {

        Map<String, Set<String>> result = new HashMap<>();
        for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
            String name = entity.getJavaType().getSimpleName();
            if (ClusterProdutos.class.isAssignableFrom(entity.getJavaType()) || ClusterLocations.class.isAssignableFrom(entity.getJavaType())
                    || name.equals("PerfilClusterizacao") || name.startsWith("RegraAlocacaoCluster")
                    || name.startsWith("CaracteristicasPrincipaisCluster") || name.equals("ParametrosClusterLocations")
                    || name.equals("CaracteristicaProduto") || name.equals("CaracteristicaLocation")) continue;
            Set<String> paths = new LinkedHashSet<>();
            usagePaths(entity, "", paths, new HashSet<>());
            // A coleção pode ser um consumidor transversal com referência direta a clusters.
            // Definições próprias de características já foram excluídas acima.
            for (PluralAttribute<?, ?, ?> attribute : entity.getPluralAttributes()) {
                Class<?> target = attribute.getElementType().getJavaType();
                String tail = PerfilClusterizacao.class.isAssignableFrom(target) ? "id"
                        : ClusterProdutos.class.isAssignableFrom(target) || ClusterLocations.class.isAssignableFrom(target)
                                ? "perfilClusterizacao.id" : null;
                if (tail != null) for (String id : entityManager.createQuery("select distinct used." + tail + " from " + entity.getName()
                        + " c join c." + attribute.getName() + " used", String.class).getResultList())
                    result.computeIfAbsent(id, ignored -> new TreeSet<>()).add(entity.getName());
            }
            for (String path : paths) {
                for (String id : entityManager.createQuery("select distinct c." + path + " from " + entity.getName() + " c where c." + path + " is not null", String.class).getResultList())
                    result.computeIfAbsent(id, ignored -> new TreeSet<>()).add(entity.getName());
            }
        }
        return result;

    }

    /** Percorre embeddables de chaves antigas; não atravessa associações arbitrárias/lazy. */
    private static void usagePaths(ManagedType<?> type, String prefix, Set<String> paths, Set<Class<?>> ancestry) {

        if (!ancestry.add(type.getJavaType())) return;
        for (SingularAttribute<?, ?> attribute : type.getSingularAttributes()) {
            String path = prefix + attribute.getName();
            Class<?> target = attribute.getJavaType();
            if (PerfilClusterizacao.class.isAssignableFrom(target)) paths.add(path + ".id");
            else if (ClusterProdutos.class.isAssignableFrom(target) || ClusterLocations.class.isAssignableFrom(target)) paths.add(path + ".perfilClusterizacao.id");
            else if (attribute.getType() instanceof EmbeddableType<?> embedded) usagePaths(embedded, path + ".", paths, new HashSet<>(ancestry));
        }

    }

    /** Campos de configuração próprios são copiados em lote, sem puxar parâmetros de planos. */
    private void cloneAdditionalDefinition(String sourceId, Map<Long, ClusterProdutos> materials, Map<Long, ClusterLocations> locations) {

        for (var original : entityManager.createQuery("select p from ParametrosClusterLocations p where p.parametrosClusterLocationsCompositeKey.clusterLocations.perfilClusterizacao.id=:profile", ParametrosClusterLocations.class).setParameter("profile", sourceId).getResultList()) {
            var copy = new ParametrosClusterLocations(new ParametrosClusterLocations.ParametrosClusterLocationsCompositeKey(locations.get(original.getClusterLocations().getId())));
            copy.setPlanejaDP(original.getPlanejaDP()); copy.setPlanejaPricing(original.getPlanejaPricing()); entityManager.persist(copy);
        }

        // Características são referências compartilhadas; duplicar apenas o vínculo
        // da definição ao novo cluster, mantendo membros e dados históricos intactos.
        for (var characteristic : entityManager.createQuery("select distinct c from CaracteristicaProduto c join fetch c.materialClusters where exists (select x.id from ClusterProdutos x where x member of c.materialClusters and x.perfilClusterizacao.id=:profile)", com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaProduto.class).setParameter("profile", sourceId).getResultList()) {
            var selected = characteristic.getMaterialClusters().stream().filter(c -> sourceId.equals(c.getPerfilClusterizacao().getId())).toList();
            selected.forEach(c -> characteristic.addMaterialCluster(materials.get(c.getId())));
        }
        for (var characteristic : entityManager.createQuery("select distinct c from CaracteristicaLocation c join fetch c.clustersLocations where exists (select x.id from ClusterLocations x where x member of c.clustersLocations and x.perfilClusterizacao.id=:profile)", com.opsfactor.community.capability.masterdata.classification.characteristic.domain.CaracteristicaLocation.class).setParameter("profile", sourceId).getResultList()) {
            var selected = characteristic.getClustersLocations().stream().filter(c -> sourceId.equals(c.getPerfilClusterizacao().getId())).toList();
            selected.forEach(c -> characteristic.addClusterLocations(locations.get(c.getId())));
        }

    }
}
