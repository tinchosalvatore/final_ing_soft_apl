package com.trackergastos.service.mapper;

import static com.trackergastos.domain.GastoAsserts.*;
import static com.trackergastos.domain.GastoTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GastoMapperTest {

    private GastoMapper gastoMapper;

    @BeforeEach
    void setUp() {
        gastoMapper = new GastoMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGastoSample1();
        var actual = gastoMapper.toEntity(gastoMapper.toDto(expected));
        assertGastoAllPropertiesEquals(expected, actual);
    }
}
