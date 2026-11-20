package com.trackergastos.repository;

import com.trackergastos.domain.Gasto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Gasto entity.
 */
@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long>, JpaSpecificationExecutor<Gasto> {
    @Query("select gasto from Gasto gasto where gasto.user.login = ?#{authentication.name}")
    List<Gasto> findByUserIsCurrentUser();

    default Optional<Gasto> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Gasto> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Gasto> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select gasto from Gasto gasto left join fetch gasto.categoria left join fetch gasto.user",
        countQuery = "select count(gasto) from Gasto gasto"
    )
    Page<Gasto> findAllWithToOneRelationships(Pageable pageable);

    @Query("select gasto from Gasto gasto left join fetch gasto.categoria left join fetch gasto.user")
    List<Gasto> findAllWithToOneRelationships();

    @Query("select gasto from Gasto gasto left join fetch gasto.categoria left join fetch gasto.user where gasto.id =:id")
    Optional<Gasto> findOneWithToOneRelationships(@Param("id") Long id);
}
