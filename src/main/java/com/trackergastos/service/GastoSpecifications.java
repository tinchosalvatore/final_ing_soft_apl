package com.trackergastos.service;

// Wildcard a propósito, como en el GastoQueryService generado: el metamodelo (Gasto_, ...) lo genera el compilador y
// `javadoc:javadoc` (paso backend:doc:test del CI) corre sin compilar. Un import puntual de una clase *_ lo rompe.
import com.trackergastos.domain.*; // for static metamodels
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
