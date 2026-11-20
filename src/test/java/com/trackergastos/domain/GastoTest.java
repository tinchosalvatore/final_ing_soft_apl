package com.trackergastos.domain;

import static com.trackergastos.domain.CategoriaTestSamples.*;
import static com.trackergastos.domain.GastoTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.trackergastos.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GastoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Gasto.class);
        Gasto gasto1 = getGastoSample1();
        Gasto gasto2 = new Gasto();
        assertThat(gasto1).isNotEqualTo(gasto2);

        gasto2.setId(gasto1.getId());
        assertThat(gasto1).isEqualTo(gasto2);

        gasto2 = getGastoSample2();
        assertThat(gasto1).isNotEqualTo(gasto2);
    }

    @Test
    void categoriaTest() {
        Gasto gasto = getGastoRandomSampleGenerator();
        Categoria categoriaBack = getCategoriaRandomSampleGenerator();

        gasto.setCategoria(categoriaBack);
        assertThat(gasto.getCategoria()).isEqualTo(categoriaBack);

        gasto.categoria(null);
        assertThat(gasto.getCategoria()).isNull();
    }
}
