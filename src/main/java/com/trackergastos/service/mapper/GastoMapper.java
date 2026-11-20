package com.trackergastos.service.mapper;

import com.trackergastos.domain.Categoria;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import com.trackergastos.service.dto.CategoriaDTO;
import com.trackergastos.service.dto.GastoDTO;
import com.trackergastos.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Gasto} and its DTO {@link GastoDTO}.
 */
@Mapper(componentModel = "spring")
public interface GastoMapper extends EntityMapper<GastoDTO, Gasto> {
    @Mapping(target = "categoria", source = "categoria", qualifiedByName = "categoriaNombre")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    GastoDTO toDto(Gasto s);

    @Named("categoriaNombre")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    CategoriaDTO toDtoCategoriaNombre(Categoria categoria);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
