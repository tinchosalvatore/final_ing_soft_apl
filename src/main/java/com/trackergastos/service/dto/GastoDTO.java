package com.trackergastos.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link com.trackergastos.domain.Gasto} entity.
 */
@Schema(
    description = "Un gasto de un usuario. El dueño lo asigna el backend a partir del usuario autenticado (spec 001).\nReglas de negocio que NO expresa el JDL (monto > 0, fecha no futura, propiedad): spec 001."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GastoDTO implements Serializable {

    private Long id;

    @NotNull
    private BigDecimal monto;

    @NotNull
    private LocalDate fecha;

    @Size(max = 140)
    private String descripcion;

    @NotNull
    private CategoriaDTO categoria;

    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public CategoriaDTO getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaDTO categoria) {
        this.categoria = categoria;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GastoDTO)) {
            return false;
        }

        GastoDTO gastoDTO = (GastoDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, gastoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GastoDTO{" +
            "id=" + getId() +
            ", monto=" + getMonto() +
            ", fecha='" + getFecha() + "'" +
            ", descripcion='" + getDescripcion() + "'" +
            ", categoria=" + getCategoria() +
            ", user=" + getUser() +
            "}";
    }
}
