package com.trackergastos.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackergastos.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GastoDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GastoDTO.class);
        GastoDTO gastoDTO1 = new GastoDTO();
        gastoDTO1.setId(1L);
        GastoDTO gastoDTO2 = new GastoDTO();
        assertThat(gastoDTO1).isNotEqualTo(gastoDTO2);
        gastoDTO2.setId(gastoDTO1.getId());
        assertThat(gastoDTO1).isEqualTo(gastoDTO2);
        gastoDTO2.setId(2L);
        assertThat(gastoDTO1).isNotEqualTo(gastoDTO2);
        gastoDTO1.setId(null);
        assertThat(gastoDTO1).isNotEqualTo(gastoDTO2);
    }
}
