package com.opsfactor.community.platform.security.login.repository;

import com.opsfactor.community.platform.security.login.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository Community de usuarios locais usados pelo login Basic.
 *
 * <p>A consulta completa carrega roles via entity graph para evitar lazy load
 * por usuario durante validacoes de seguranca.</p>
 */
@Repository
public interface UserRepository extends CrudRepository<User, String> {

    public Optional<User> findByIdIgnoreCase(String id);

    @EntityGraph(attributePaths = {"userRoles"})
    public List<User> findAll();

}
