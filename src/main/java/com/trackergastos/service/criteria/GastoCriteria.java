package com.trackergastos.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.trackergastos.domain.Gasto} entity. This class is used
 * in {@link com.trackergastos.web.rest.GastoResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /gastos?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GastoCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private BigDecimalFilter monto;

    private LocalDateFilter fecha;

    private StringFilter descripcion;

    private LongFilter categoriaId;

    private LongFilter userId;

    private Boolean distinct;

    public GastoCriteria() {}

    public GastoCriteria(GastoCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.monto = other.optionalMonto().map(BigDecimalFilter::copy).orElse(null);
        this.fecha = other.optionalFecha().map(LocalDateFilter::copy).orElse(null);
        this.descripcion = other.optionalDescripcion().map(StringFilter::copy).orElse(null);
        this.categoriaId = other.optionalCategoriaId().map(LongFilter::copy).orElse(null);
        this.userId = other.optionalUserId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public GastoCriteria copy() {
        return new GastoCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public BigDecimalFilter getMonto() {
        return monto;
    }

    public Optional<BigDecimalFilter> optionalMonto() {
        return Optional.ofNullable(monto);
    }

    public BigDecimalFilter monto() {
        if (monto == null) {
            setMonto(new BigDecimalFilter());
        }
        return monto;
    }

    public void setMonto(BigDecimalFilter monto) {
        this.monto = monto;
    }

    public LocalDateFilter getFecha() {
        return fecha;
    }

    public Optional<LocalDateFilter> optionalFecha() {
        return Optional.ofNullable(fecha);
    }

    public LocalDateFilter fecha() {
        if (fecha == null) {
            setFecha(new LocalDateFilter());
        }
        return fecha;
    }

    public void setFecha(LocalDateFilter fecha) {
        this.fecha = fecha;
    }

    public StringFilter getDescripcion() {
        return descripcion;
    }

    public Optional<StringFilter> optionalDescripcion() {
        return Optional.ofNullable(descripcion);
    }

    public StringFilter descripcion() {
        if (descripcion == null) {
            setDescripcion(new StringFilter());
        }
        return descripcion;
    }

    public void setDescripcion(StringFilter descripcion) {
        this.descripcion = descripcion;
    }

    public LongFilter getCategoriaId() {
        return categoriaId;
    }

    public Optional<LongFilter> optionalCategoriaId() {
        return Optional.ofNullable(categoriaId);
    }

    public LongFilter categoriaId() {
        if (categoriaId == null) {
            setCategoriaId(new LongFilter());
        }
        return categoriaId;
    }

    public void setCategoriaId(LongFilter categoriaId) {
        this.categoriaId = categoriaId;
    }

    public LongFilter getUserId() {
        return userId;
    }

    public Optional<LongFilter> optionalUserId() {
        return Optional.ofNullable(userId);
    }

    public LongFilter userId() {
        if (userId == null) {
            setUserId(new LongFilter());
        }
        return userId;
    }

    public void setUserId(LongFilter userId) {
        this.userId = userId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GastoCriteria that = (GastoCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(monto, that.monto) &&
            Objects.equals(fecha, that.fecha) &&
            Objects.equals(descripcion, that.descripcion) &&
            Objects.equals(categoriaId, that.categoriaId) &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, monto, fecha, descripcion, categoriaId, userId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GastoCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalMonto().map(f -> "monto=" + f + ", ").orElse("") +
            optionalFecha().map(f -> "fecha=" + f + ", ").orElse("") +
            optionalDescripcion().map(f -> "descripcion=" + f + ", ").orElse("") +
            optionalCategoriaId().map(f -> "categoriaId=" + f + ", ").orElse("") +
            optionalUserId().map(f -> "userId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
