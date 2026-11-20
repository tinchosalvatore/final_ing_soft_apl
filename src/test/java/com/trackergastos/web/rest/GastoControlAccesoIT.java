package com.trackergastos.web.rest;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.Categoria;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests de integración del control de acceso a gastos (spec 001, criterios 001-CA01 y CA04 a CA10; ADR-0007).
 * Todos los pedidos se hacen como {@code ana}; {@code beto} es el otro usuario.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser("ana")
class GastoControlAccesoIT {

    private static final String API = "/api/gastos";
    private static final String API_ID = API + "/{id}";
    private static final long ID_INEXISTENTE = Long.MAX_VALUE;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private ObjectMapper om;

    private User ana;
    private User beto;
    private Categoria supermercado;
    private Categoria transporte;

    @BeforeEach
    void setUp() {
        ana = usuario(em, "ana");
        beto = usuario(em, "beto");
        supermercado = categoria(em, "Supermercado");
        transporte = categoria(em, "Transporte");
    }

    private String cuerpoGasto(Long id, String monto, LocalDate fecha, Categoria categoria, User dueno) {
        StringBuilder json = new StringBuilder("{");
        if (id != null) {
            json.append("\"id\":").append(id).append(',');
        }
        json.append("\"monto\":").append(monto).append(',');
        json.append("\"fecha\":\"").append(fecha).append("\",");
        json.append("\"categoria\":{\"id\":").append(categoria.getId()).append('}');
        if (dueno != null) {
            json.append(",\"user\":{\"id\":").append(dueno.getId()).append(",\"login\":\"").append(dueno.getLogin()).append("\"}");
        }
        return json.append('}').toString();
    }

    private List<Long> idsDelListado(MvcResult result) throws Exception {
        List<?> lista = om.readValue(result.getResponse().getContentAsString(), List.class);
        List<Long> ids = new ArrayList<>();
        for (Object o : lista) {
            ids.add(((Number) ((Map<?, ?>) o).get("id")).longValue());
        }
        return ids;
    }

    private Gasto recargar(Gasto g) {
        em.flush();
        em.clear();
        return em.find(Gasto.class, g.getId());
    }

    @Test
    @DisplayName("001-CA01 registrar gasto válido: se crea con dueño ana y aparece primero en su listado")
    void ca01RegistrarGastoValido() throws Exception {
        gasto(em, ana, transporte, "300", hoy().minusDays(5));
        gasto(em, ana, transporte, "200", hoy()); // misma fecha, registrado antes

        MvcResult creado = mvc
            .perform(
                post(API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoGasto(null, "1500.50", hoy(), supermercado, null))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.login").value("ana"))
            .andReturn();
        long id = ((Number) om.readValue(creado.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        Gasto enBase = em.find(Gasto.class, id);
        assertThat(enBase.getUser().getLogin()).isEqualTo("ana");
        assertThat(enBase.getMonto()).isEqualByComparingTo("1500.50");

        MvcResult listado = mvc.perform(get(API)).andExpect(status().isOk()).andReturn();
        assertThat(idsDelListado(listado)).first().isEqualTo(id);
    }

    @Test
    @DisplayName("001-CA04 dueño forzado al crear: se ignora beto y el dueño es ana")
    void ca04DuenoForzadoAlCrear() throws Exception {
        MvcResult creado = mvc
            .perform(
                post(API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoGasto(null, "100", hoy(), supermercado, beto))
            )
            .andExpect(status().isCreated())
            .andReturn();
        long id = ((Number) om.readValue(creado.getResponse().getContentAsString(), Map.class).get("id")).longValue();

        em.flush();
        em.clear();
        assertThat(em.find(Gasto.class, id).getUser().getLogin()).isEqualTo("ana");
    }

    @Test
    @DisplayName("001-CA04 dueño forzado al editar: se edita pero el dueño sigue siendo ana")
    void ca04DuenoForzadoAlEditar() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());

        mvc.perform(
            put(API_ID, propio.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoGasto(propio.getId(), "250", hoy(), supermercado, beto))
        ).andExpect(status().isOk());

        Gasto enBase = recargar(propio);
        assertThat(enBase.getMonto()).isEqualByComparingTo("250");
        assertThat(enBase.getUser().getLogin()).isEqualTo("ana");
    }

    @Test
    @DisplayName("001-CA05 listado aislado: ana ve exactamente sus 3 gastos")
    void ca05ListadoAislado() throws Exception {
        List<Long> deAna = List.of(
            gasto(em, ana, supermercado, "10", hoy()).getId(),
            gasto(em, ana, transporte, "20", hoy().minusDays(1)).getId(),
            gasto(em, ana, supermercado, "30", hoy().minusDays(2)).getId()
        );
        gasto(em, beto, supermercado, "40", hoy());
        gasto(em, beto, transporte, "50", hoy());

        MvcResult listado = mvc
            .perform(get(API).param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andReturn();

        assertThat(idsDelListado(listado)).containsExactlyInAnyOrderElementsOf(deAna);
    }

    @Test
    @DisplayName("001-CA06 filtro no escapa al dueño: pedir los gastos de beto no devuelve ninguno de beto")
    void ca06FiltroNoEscapaAlDueno() throws Exception {
        Long deAna = gasto(em, ana, supermercado, "10", hoy()).getId();
        Long deBeto1 = gasto(em, beto, supermercado, "40", hoy()).getId();
        Long deBeto2 = gasto(em, beto, transporte, "50", hoy()).getId();

        MvcResult listado = mvc
            .perform(get(API).param("userId.equals", beto.getId().toString()).param("size", "100"))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(idsDelListado(listado)).doesNotContain(deBeto1, deBeto2).isSubsetOf(deAna);

        MvcResult listadoIn = mvc
            .perform(get(API).param("userId.in", beto.getId().toString(), ana.getId().toString()).param("size", "100"))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(idsDelListado(listadoIn)).doesNotContain(deBeto1, deBeto2);
    }

    @Test
    @DisplayName("001-CA06 el conteo filtrado tampoco escapa al dueño")
    void ca06ConteoNoEscapaAlDueno() throws Exception {
        gasto(em, beto, supermercado, "40", hoy());
        gasto(em, beto, transporte, "50", hoy());

        mvc.perform(get(API + "/count").param("userId.equals", beto.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("001-CA07 ver gasto ajeno (IDOR): 404 idéntico a pedir un id inexistente")
    void ca07VerGastoAjeno() throws Exception {
        Gasto deBeto = gasto(em, beto, supermercado, "40", hoy());

        String ajeno = mvc
            .perform(get(API_ID, deBeto.getId()))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String inexistente = mvc
            .perform(get(API_ID, ID_INEXISTENTE))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(sinCamposConId(ajeno)).isEqualTo(sinCamposConId(inexistente));
    }

    private Map<?, ?> sinCamposConId(String problem) {
        Map<?, ?> cuerpo = om.readValue(problem, Map.class);
        cuerpo.remove("instance");
        cuerpo.remove("path");
        return cuerpo;
    }

    @Test
    @DisplayName("001-CA08 editar y borrar gasto ajeno: 404 en PUT, PATCH y DELETE y el gasto de beto queda intacto")
    void ca08EditarBorrarGastoAjeno() throws Exception {
        Gasto deBeto = gasto(em, beto, supermercado, "40", hoy().minusDays(3));
        Long id = deBeto.getId();

        mvc.perform(
            put(API_ID, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoGasto(id, "1", hoy(), transporte, null))
        ).andExpect(status().isNotFound());
        mvc.perform(
            patch(API_ID, id)
                .contentType("application/merge-patch+json")
                .content("{\"id\":" + id + ",\"monto\":1}")
        ).andExpect(status().isNotFound());
        mvc.perform(delete(API_ID, id)).andExpect(status().isNotFound());

        Gasto enBase = recargar(deBeto);
        assertThat(enBase).isNotNull();
        assertThat(enBase.getMonto()).isEqualByComparingTo("40");
        assertThat(enBase.getFecha()).isEqualTo(hoy().minusDays(3));
        assertThat(enBase.getCategoria().getId()).isEqualTo(supermercado.getId());
        assertThat(enBase.getUser().getLogin()).isEqualTo("beto");
    }

    @Test
    @DisplayName("001-CA08 editar y borrar un id inexistente: 404 en PUT, PATCH y DELETE")
    void ca08EditarBorrarInexistente() throws Exception {
        mvc.perform(
            put(API_ID, ID_INEXISTENTE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoGasto(ID_INEXISTENTE, "1", hoy(), transporte, null))
        ).andExpect(status().isNotFound());
        mvc.perform(
            patch(API_ID, ID_INEXISTENTE)
                .contentType("application/merge-patch+json")
                .content("{\"id\":" + ID_INEXISTENTE + ",\"monto\":1}")
        ).andExpect(status().isNotFound());
        mvc.perform(delete(API_ID, ID_INEXISTENTE)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("001-CA09 filtro por fecha y categoría: solo de ana, del rango y de Transporte, del más reciente al más antiguo")
    void ca09FiltroPorFechaYCategoria() throws Exception {
        LocalDate primero = hoy().withDayOfMonth(1);
        LocalDate ultimo = hoy().withDayOfMonth(hoy().lengthOfMonth());
        Long transporteDia1 = gasto(em, ana, transporte, "10", primero).getId();
        Long transporteHoy = gasto(em, ana, transporte, "20", hoy()).getId();
        gasto(em, ana, supermercado, "30", hoy()); // otra categoría
        gasto(em, ana, transporte, "40", primero.minusDays(1)); // mes anterior
        gasto(em, beto, transporte, "50", hoy()); // otro usuario

        MvcResult listado = mvc
            .perform(
                get(API)
                    .param("fecha.greaterThanOrEqual", primero.toString())
                    .param("fecha.lessThanOrEqual", ultimo.toString())
                    .param("categoriaId.equals", transporte.getId().toString())
                    .param("size", "100")
            )
            .andExpect(status().isOk())
            .andReturn();

        // si hoy es día 1, las dos fechas coinciden y desempata el id (el último registrado primero): el orden esperado es el mismo
        assertThat(idsDelListado(listado)).containsExactly(transporteHoy, transporteDia1);
    }

    @Test
    @DisplayName("001-CA09 el orden lo impone el backend aunque el cliente mande otro sort (spec 001 §5.1)")
    void ca09OrdenImpuesto() throws Exception {
        Long viejo = gasto(em, ana, transporte, "10", hoy().minusDays(10)).getId();
        Long hoyPrimero = gasto(em, ana, transporte, "20", hoy()).getId();
        Long hoySegundo = gasto(em, ana, transporte, "30", hoy()).getId();

        MvcResult listado = mvc.perform(get(API).param("sort", "id,asc").param("size", "100")).andExpect(status().isOk()).andReturn();

        assertThat(idsDelListado(listado)).containsExactly(hoySegundo, hoyPrimero, viejo);
    }

    @Test
    @DisplayName("001-CA09 filtro con desde posterior a hasta: listado vacío, sin error")
    void ca09DesdePosteriorAHasta() throws Exception {
        gasto(em, ana, transporte, "10", hoy());

        mvc.perform(
            get(API).param("fecha.greaterThanOrEqual", hoy().toString()).param("fecha.lessThanOrEqual", hoy().minusDays(1).toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithUnauthenticatedMockUser
    @DisplayName("001-CA10 sin sesión: todas las operaciones de gastos dan 401")
    void ca10SinSesion() throws Exception {
        Gasto deAna = gasto(em, ana, supermercado, "10", hoy());
        Long id = deAna.getId();
        String cuerpo = cuerpoGasto(id, "1", hoy(), supermercado, null);

        mvc.perform(get(API)).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/count")).andExpect(status().isUnauthorized());
        mvc.perform(get(API_ID, id)).andExpect(status().isUnauthorized());
        mvc.perform(
            post(API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoGasto(null, "1", hoy(), supermercado, null))
        ).andExpect(status().isUnauthorized());
        mvc.perform(put(API_ID, id).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isUnauthorized());
        mvc.perform(patch(API_ID, id).contentType("application/merge-patch+json").content(cuerpo)).andExpect(status().isUnauthorized());
        mvc.perform(delete(API_ID, id)).andExpect(status().isUnauthorized());

        assertThat(recargar(deAna)).isNotNull();
    }

    @Test
    @DisplayName("001-CA07 control: ana sí ve su propio gasto por id y en el listado")
    void verGastoPropio() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "10", hoy());

        mvc.perform(get(API_ID, propio.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(propio.getId()));
        mvc.perform(get(API).param("size", "100")).andExpect(jsonPath("$[*].id").value(hasItem(propio.getId().intValue())));
    }
}
