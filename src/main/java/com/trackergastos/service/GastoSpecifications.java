package com.trackergastos.service;

import com.trackergastos.domain.Categoria_;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.Gasto_;
import com.trackergastos.domain.User_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Condiciones de consulta de gastos que comparten {@link GastoService} y {@link GastoQueryService}.
 * Sin métodos nuevos en {@code GastoRepository}, que es territorio del generador (ADR-0008).
 */
final class GastoSpecifications {

    private GastoSpecifications() {}

    /** Dueño = el login indicado (defensas S1 y S2). */
    static Specification<Gasto> delDueno(String login) {
        return (root, query, cb) -> cb.equal(root.get(Gasto_.user).get(User_.login), login);
    }

    static Specification<Gasto> conId(Long id) {
        return (root, query, cb) -> cb.equal(root.get(Gasto_.id), id);
    }

    /** Gastos de una categoría, sin importar el dueño (para saber si la categoría está en uso, 001-CA13). */
    static Specification<Gasto> deCategoria(Long idCategoria) {
        return (root, query, cb) -> cb.equal(root.get(Gasto_.categoria).get(Categoria_.id), idCategoria);
    }
}
