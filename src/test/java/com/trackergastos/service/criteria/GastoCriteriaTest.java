package com.trackergastos.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class GastoCriteriaTest {

    @Test
    void newGastoCriteriaHasAllFiltersNullTest() {
        var gastoCriteria = new GastoCriteria();
        assertThat(gastoCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void gastoCriteriaFluentMethodsCreatesFiltersTest() {
        var gastoCriteria = new GastoCriteria();

        setAllFilters(gastoCriteria);

        assertThat(gastoCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void gastoCriteriaCopyCreatesNullFilterTest() {
        var gastoCriteria = new GastoCriteria();
        var copy = gastoCriteria.copy();

        assertThat(gastoCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(gastoCriteria)
        );
    }

    @Test
    void gastoCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var gastoCriteria = new GastoCriteria();
        setAllFilters(gastoCriteria);

        var copy = gastoCriteria.copy();

        assertThat(gastoCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(gastoCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var gastoCriteria = new GastoCriteria();

        assertThat(gastoCriteria).hasToString("GastoCriteria{}");
    }

    private static void setAllFilters(GastoCriteria gastoCriteria) {
        gastoCriteria.id();
        gastoCriteria.monto();
        gastoCriteria.fecha();
        gastoCriteria.descripcion();
        gastoCriteria.categoriaId();
        gastoCriteria.userId();
        gastoCriteria.distinct();
    }

    private static Condition<GastoCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getMonto()) &&
                condition.apply(criteria.getFecha()) &&
                condition.apply(criteria.getDescripcion()) &&
                condition.apply(criteria.getCategoriaId()) &&
                condition.apply(criteria.getUserId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<GastoCriteria> copyFiltersAre(GastoCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getMonto(), copy.getMonto()) &&
                condition.apply(criteria.getFecha(), copy.getFecha()) &&
                condition.apply(criteria.getDescripcion(), copy.getDescripcion()) &&
                condition.apply(criteria.getCategoriaId(), copy.getCategoriaId()) &&
                condition.apply(criteria.getUserId(), copy.getUserId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
