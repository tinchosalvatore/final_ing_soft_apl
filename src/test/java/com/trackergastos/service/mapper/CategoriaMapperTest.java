package com.trackergastos.service.mapper;

import static com.trackergastos.domain.CategoriaAsserts.*;
import static com.trackergastos.domain.CategoriaTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoriaMapperTest {

    private CategoriaMapper categoriaMapper;

    @BeforeEach
    void setUp() {
        categoriaMapper = new CategoriaMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCategoriaSample1();
        var actual = categoriaMapper.toEntity(categoriaMapper.toDto(expected));
        assertCategoriaAllPropertiesEquals(expected, actual);
    }
}
