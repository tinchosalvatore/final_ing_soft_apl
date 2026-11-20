package com.trackergastos.repository;

import com.trackergastos.domain.Categoria;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Categoria entity.
 *
 * Archivo del generador con un método propio (ADR-0008): al regenerar, responder {@code i} (ignorar siempre).
 */
@SuppressWarnings("unused")
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    /**
     * ¿Existe otra categoría con el mismo nombre sin distinguir mayúsculas ni espacios al inicio o al final?
     * (spec 001, regla 11; 001-CA12). {@code idExcluido} es la categoría que se renombra, o {@code null} al crear.
     */
    @Query("select count(c) > 0 from Categoria c where lower(trim(c.nombre)) = lower(trim(:nombre)) and c.id <> coalesce(:idExcluido, -1)")
    boolean existeNombreNormalizado(@Param("nombre") String nombre, @Param("idExcluido") Long idExcluido);
}
