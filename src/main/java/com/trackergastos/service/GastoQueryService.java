package com.trackergastos.service;

import com.trackergastos.domain.*; // for static metamodels
import com.trackergastos.domain.Gasto;
import com.trackergastos.repository.GastoRepository;
import com.trackergastos.service.criteria.GastoCriteria;
import com.trackergastos.service.dto.GastoDTO;
import com.trackergastos.service.mapper.GastoMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Gasto} entities in the database.
 * The main input is a {@link GastoCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link GastoDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class GastoQueryService extends QueryService<Gasto> {

    private static final Logger LOG = LoggerFactory.getLogger(GastoQueryService.class);

    private final GastoRepository gastoRepository;

    private final GastoMapper gastoMapper;

    public GastoQueryService(GastoRepository gastoRepository, GastoMapper gastoMapper) {
        this.gastoRepository = gastoRepository;
        this.gastoMapper = gastoMapper;
    }

    /**
     * Return a {@link Page} of {@link GastoDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<GastoDTO> findByCriteria(GastoCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Gasto> specification = createSpecification(criteria);
        return gastoRepository.findAll(specification, page).map(gastoMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(GastoCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Gasto> specification = createSpecification(criteria);
        return gastoRepository.count(specification);
    }

    /**
     * Function to convert {@link GastoCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Gasto> createSpecification(GastoCriteria criteria) {
        Specification<Gasto> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Gasto_.categoria, JoinType.LEFT);
                root.fetch(Gasto_.user, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Gasto_.id),
                    buildRangeSpecification(criteria.getMonto(), Gasto_.monto),
                    buildRangeSpecification(criteria.getFecha(), Gasto_.fecha),
                    buildStringSpecification(criteria.getDescripcion(), Gasto_.descripcion),
                    buildSpecification(criteria.getCategoriaId(), root -> root.join(Gasto_.categoria, JoinType.LEFT).get(Categoria_.id)),
                    buildSpecification(criteria.getUserId(), root -> root.join(Gasto_.user, JoinType.LEFT).get(User_.id))
                )
            );
        }
        return specification;
    }
}
