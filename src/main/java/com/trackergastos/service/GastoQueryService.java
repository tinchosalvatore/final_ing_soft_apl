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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Consultas filtradas de gastos (spec 001, reglas 4 y 5; ADR-0007). Territorio del desarrollador: al regenerar, {@code n}.
 * <ul>
 *   <li><b>S2</b>: la especificación siempre agrega "dueño = usuario autenticado" y descarta el criterio de usuario que
 *   mande el cliente ({@code userId.*}). Vale para el listado y para {@code /count} (001-CA05, CA06).</li>
 *   <li>El orden lo impone el backend: fecha descendente y después id descendente (001-CA09, spec §5.1).</li>
 * </ul>
 * Logs sin el contenido de los filtros (montos, descripciones): AGENTS.md, regla 6.
 *
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
        LOG.debug("Listado de gastos: pagina={}", page.isPaged() ? page.getPageNumber() : "sin paginar");
        final Specification<Gasto> specification = createSpecification(criteria);
        return gastoRepository.findAll(specification, conOrdenImpuesto(page)).map(gastoMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(GastoCriteria criteria) {
        LOG.debug("Conteo de gastos");
        final Specification<Gasto> specification = createSpecification(criteria);
        return gastoRepository.count(specification);
    }

    /**
     * Function to convert {@link GastoCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Gasto> createSpecification(GastoCriteria criteriaDelCliente) {
        // S2: el criterio de usuario del cliente se pisa; el dueño es siempre el usuario autenticado.
        GastoCriteria criteria = criteriaDelCliente != null ? criteriaDelCliente.copy() : null;
        if (criteria != null) {
            criteria.setUserId(null);
        }
        Specification<Gasto> specification = GastoSpecifications.delDueno(GastoService.loginActual());
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Gasto_.categoria, JoinType.LEFT);
                root.fetch(Gasto_.user, JoinType.LEFT);
            }
            return null;
        });
        if (criteria == null) {
            return specification;
        }
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
        return specification;
    }

    /** Fecha descendente y, a igual fecha, el último registrado primero. Se ignora el {@code sort} del cliente. */
    private static Pageable conOrdenImpuesto(Pageable page) {
        Sort orden = Sort.by(Sort.Order.desc(Gasto_.FECHA), Sort.Order.desc(Gasto_.ID));
        return page.isPaged() ? PageRequest.of(page.getPageNumber(), page.getPageSize(), orden) : Pageable.unpaged(orden);
    }
}
