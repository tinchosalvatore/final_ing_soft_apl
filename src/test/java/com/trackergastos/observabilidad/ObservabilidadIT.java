package com.trackergastos.observabilidad;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import com.trackergastos.security.AuthoritiesConstants;
import com.trackergastos.service.GastoQueryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Spec 004: registros estructurados y sin datos sensibles (criterios 004-CA01 a CA05 y CA11).
 * Los registros se validan en el JSON que se mandaría a Logstash (ver {@link CapturaDeLogs}).
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ObservabilidadIT {

    private static final String CONTRASENA = "correcta-123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GastoQueryService gastoQueryService;

    private final CapturaDeLogs logs = new CapturaDeLogs();

    @BeforeEach
    void capturar() {
        logs.iniciar();
    }

    @AfterEach
    void soltar() {
        logs.detener();
    }

    private User usuarioConContrasena(String login, boolean activado) {
        User u = usuario(em, login);
        u.setPassword(passwordEncoder.encode(CONTRASENA));
        u.setActivated(activado);
        em.flush();
        return u;
    }

    private org.springframework.test.web.servlet.ResultActions login(String login, String contrasena) throws Exception {
        return mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + login + "\",\"password\":\"" + contrasena + "\"}")
        );
    }

    @Test
    @DisplayName("004-CA01 login fallido: LOGIN_FALLIDO WARN con login e IP, sin la contraseña")
    void ca01LoginFallido() throws Exception {
        usuarioConContrasena("ana", true);

        login("ana", "secreta-incorrecta").andExpect(status().isUnauthorized());

        assertThat(logs.eventos("LOGIN_FALLIDO"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.path("level").asString()).isEqualTo("WARN");
                assertThat(json.path("login").asString()).isEqualTo("ana");
                assertThat(json.path("ip").asString()).isNotBlank();
                assertThat(json.path("app_name").asString()).isEqualTo("trackerGastos");
            });
        assertThat(logs.lineas()).noneMatch(l -> l.contains("secreta-incorrecta"));
    }

    @Test
    @DisplayName("004-CA01 borde: un usuario inexistente se registra igual; un login con forma de email se enmascara")
    void ca01UsuarioInexistenteYEmail() throws Exception {
        login("nadie", "secreta-incorrecta").andExpect(status().isUnauthorized());
        login("nadie@prueba.local", "secreta-incorrecta").andExpect(status().isUnauthorized());

        assertThat(logs.eventos("LOGIN_FALLIDO"))
            .extracting(json -> json.path("login").asString())
            .containsExactly("nadie", "n***@prueba.local");
        assertThat(logs.eventos("LOGIN_FALLIDO")).allMatch(json -> json.path("level").asString().equals("WARN"));
        assertThat(logs.lineas()).noneMatch(l -> l.contains("nadie@prueba.local"));
    }

    @Test
    @DisplayName("004-CA01 LOGIN_OK al iniciar sesión")
    void loginCorrecto() throws Exception {
        usuarioConContrasena("ana", true);

        login("ana", CONTRASENA).andExpect(status().isOk());

        assertThat(logs.eventos("LOGIN_OK"))
            .singleElement()
            .satisfies(json -> assertThat(json.path("login").asString()).isEqualTo("ana"));
        assertThat(logs.lineas()).noneMatch(l -> l.contains(CONTRASENA));
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA02 crear un gasto: GASTO_CREADO con el id y ningún registro con el monto ni la descripción")
    void ca02SinDatosSensibles() throws Exception {
        usuario(em, "ana");
        Long categoria = categoria(em, "Salud").getId();

        mvc.perform(
            post("/api/gastos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"monto\":1234.56,\"fecha\":\"" +
                        hoy() +
                        "\",\"descripcion\":\"farmacia X\",\"categoria\":{\"id\":" +
                        categoria +
                        "}}"
                )
        ).andExpect(status().isCreated());

        assertThat(logs.eventos("GASTO_CREADO"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.path("level").asString()).isEqualTo("INFO");
                assertThat(json.path("gastoId").isNumber()).isTrue();
                assertThat(json.path("usuario").asString()).isEqualTo("ana");
            });
        assertThat(logs.lineasSinCamposTecnicos())
            .isNotEmpty()
            .noneMatch(l -> l.contains("1234") || l.contains("farmacia"));
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA03 ana pide un gasto de beto: 404 y GASTO_AJENO WARN con usuario ana y el id")
    void ca03GastoAjeno() throws Exception {
        usuario(em, "ana");
        Gasto deBeto = gasto(em, usuario(em, "beto"), categoria(em, "Salud"), "100", hoy());

        mvc.perform(get("/api/gastos/{id}", deBeto.getId())).andExpect(status().isNotFound());

        assertThat(logs.eventos("GASTO_AJENO"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.path("level").asString()).isEqualTo("WARN");
                assertThat(json.path("usuario").asString()).isEqualTo("ana");
                assertThat(json.path("gastoId").asLong()).isEqualTo(deBeto.getId());
                assertThat(json.path("operacion").asString()).isEqualTo("ver");
            });
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA03 borde: un id que no existe no es GASTO_AJENO")
    void ca03IdInexistente() throws Exception {
        usuario(em, "ana");

        mvc.perform(get("/api/gastos/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());

        assertThat(logs.eventos("GASTO_AJENO")).isEmpty();
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA04 crear un gasto: PETICION_HTTP y GASTO_CREADO comparten el traceId")
    void ca04Traza() throws Exception {
        usuario(em, "ana");
        Long categoria = categoria(em, "Salud").getId();

        mvc.perform(
            post("/api/gastos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\":10,\"fecha\":\"" + hoy() + "\",\"categoria\":{\"id\":" + categoria + "}}")
        ).andExpect(status().isCreated());

        JsonNode creado = logs.eventos("GASTO_CREADO").getFirst();
        JsonNode peticion = logs.eventos("PETICION_HTTP").getFirst();
        String traceId = creado.path("traceId").asString();
        assertThat(traceId).isNotBlank();
        assertThat(peticion.path("traceId").asString()).isEqualTo(traceId);
        assertThat(peticion.path("metodo").asString()).isEqualTo("POST");
        assertThat(peticion.path("ruta").asString()).isEqualTo("/api/gastos");
        assertThat(peticion.path("estado").asInt()).isEqualTo(201);
        assertThat(peticion.path("duracion_ms").isNumber()).isTrue();
        assertThat(peticion.path("usuario").asString()).isEqualTo("ana");
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA04 PETICION_HTTP registra el patrón de la ruta, no el id")
    void ca04PatronDeRuta() throws Exception {
        usuario(em, "ana");

        mvc.perform(get("/api/gastos/{id}", 987654)).andExpect(status().isNotFound());

        assertThat(logs.eventos("PETICION_HTTP"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.path("ruta").asString()).isEqualTo("/api/gastos/{id}");
                assertThat(json.path("estado").asInt()).isEqualTo(404);
            });
    }

    @Test
    @DisplayName("004 borde: petición sin sesión tiene traza pero no usuario")
    void peticionSinSesion() throws Exception {
        mvc.perform(get("/api/gastos")).andExpect(status().isUnauthorized());

        assertThat(logs.eventos("PETICION_HTTP"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.has("usuario")).isFalse();
                assertThat(json.path("traceId").asString()).isNotBlank();
                assertThat(json.path("estado").asInt()).isEqualTo(401);
            });
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA05 error inesperado: respuesta genérica y ERROR_INESPERADO con el detalle")
    void ca05ErrorSinFiltracion() throws Exception {
        when(gastoQueryService.findByCriteria(any(), any())).thenThrow(
            new IllegalStateException("com.trackergastos falló: select * from gasto")
        );

        String cuerpo = mvc
            .perform(get("/api/gastos"))
            .andExpect(status().isInternalServerError())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(cuerpo).doesNotContain("com.trackergastos", "Exception", "ProblemDetailWithCause", "select", "at com.");
        assertThat(logs.eventos("ERROR_INESPERADO"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.path("level").asString()).isEqualTo("ERROR");
                assertThat(json.path("tipoError").asString()).isEqualTo("java.lang.IllegalStateException");
                assertThat(json.path("stack_trace").asString()).contains("select * from gasto");
            });
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004-CA05 también los 400: el idinvalid no expone ProblemDetailWithCause")
    void ca05BadRequestSinClasesInternas() throws Exception {
        String cuerpo = mvc
            .perform(
                put("/api/gastos/{id}", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"id\":2,\"monto\":10,\"fecha\":\"" + hoy() + "\",\"categoria\":{\"id\":1}}")
            )
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(cuerpo).contains("idinvalid").doesNotContain("com.trackergastos", "Exception", "ProblemDetailWithCause");
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("004 ACCESO_DENEGADO: un USER crea una categoría")
    void accesoDenegado() throws Exception {
        mvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nueva\"}")).andExpect(
            status().isForbidden()
        );

        assertThat(logs.eventos("ACCESO_DENEGADO"))
            .singleElement()
            .satisfies(json -> {
                assertThat(json.path("level").asString()).isEqualTo("WARN");
                assertThat(json.path("usuario").asString()).isEqualTo("ana");
                assertThat(json.path("operacion").asString()).isEqualTo("POST /api/categorias");
            });
    }

    @Test
    @DisplayName("004-CA11 usuario desactivado: mismo 401 que credenciales incorrectas, LOGIN_FALLIDO y ningún ERROR_INESPERADO")
    void ca11UsuarioDesactivado() throws Exception {
        usuarioConContrasena("ana", true);
        usuarioConContrasena("inactivo", false);

        String incorrectas = login("ana", "secreta-incorrecta")
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String desactivadoContrasenaCorrecta = login("inactivo", CONTRASENA)
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String desactivadoContrasenaIncorrecta = login("inactivo", "secreta-incorrecta")
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(desactivadoContrasenaCorrecta).isEqualTo(incorrectas);
        assertThat(desactivadoContrasenaIncorrecta).isEqualTo(incorrectas);
        assertThat(logs.eventos("LOGIN_FALLIDO"))
            .extracting(json -> json.path("login").asString())
            .containsExactly("ana", "inactivo", "inactivo");
        assertThat(logs.eventos("ERROR_INESPERADO")).isEmpty();
    }
}
