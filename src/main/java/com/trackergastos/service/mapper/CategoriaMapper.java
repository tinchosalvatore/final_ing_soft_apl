package com.trackergastos.service.mapper;

import com.trackergastos.domain.Categoria;
import com.trackergastos.service.dto.CategoriaDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Categoria} and its DTO {@link CategoriaDTO}.
 */
@Mapper(componentModel = "spring")
public interface CategoriaMapper extends EntityMapper<CategoriaDTO, Categoria> {}
