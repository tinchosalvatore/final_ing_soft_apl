package com.trackergastos.web.rest;

import static com.trackergastos.domain.GastoAsserts.*;
import static com.trackergastos.web.rest.TestUtil.createUpdateProxyForBean;
import static com.trackergastos.web.rest.TestUtil.sameNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.Categoria;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import com.trackergastos.repository.GastoRepository;
import com.trackergastos.repository.UserRepository;
import com.trackergastos.service.GastoService;
import com.trackergastos.service.dto.GastoDTO;
import com.trackergastos.service.mapper.GastoMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link GastoResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class GastoResourceIT {

    private static final BigDecimal DEFAULT_MONTO = new BigDecimal(1);
    private static final BigDecimal UPDATED_MONTO = new BigDecimal(2);
    private static final BigDecimal SMALLER_MONTO = new BigDecimal(1 - 1);

    private static final LocalDate DEFAULT_FECHA = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_FECHA = LocalDate.parse("2023-12-12");
    private static final LocalDate SMALLER_FECHA = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_DESCRIPCION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPCION = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/gastos";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private GastoRepository gastoRepositoryMock;

    @Autowired
    private GastoMapper gastoMapper;

    @Mock
    private GastoService gastoServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGastoMockMvc;

    private Gasto gasto;

    private Gasto insertedGasto;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Gasto createEntity(EntityManager em) {
        Gasto gasto = new Gasto().monto(DEFAULT_MONTO).fecha(DEFAULT_FECHA).descripcion(DEFAULT_DESCRIPCION);
        // Add required entity
        Categoria categoria;
        if (TestUtil.findAll(em, Categoria.class).isEmpty()) {
            categoria = CategoriaResourceIT.createEntity();
            em.persist(categoria);
            em.flush();
        } else {
            categoria = TestUtil.findAll(em, Categoria.class).getFirst();
        }
        gasto.setCategoria(categoria);
        return gasto;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Gasto createUpdatedEntity(EntityManager em) {
        Gasto updatedGasto = new Gasto().monto(UPDATED_MONTO).fecha(UPDATED_FECHA).descripcion(UPDATED_DESCRIPCION);
        // Add required entity
        Categoria categoria;
        if (TestUtil.findAll(em, Categoria.class).isEmpty()) {
            categoria = CategoriaResourceIT.createUpdatedEntity();
            em.persist(categoria);
            em.flush();
        } else {
            categoria = TestUtil.findAll(em, Categoria.class).getFirst();
        }
        updatedGasto.setCategoria(categoria);
        return updatedGasto;
    }

    @BeforeEach
    void initTest() {
        gasto = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedGasto != null) {
            gastoRepository.delete(insertedGasto);
            insertedGasto = null;
        }
    }

    @Test
    @Transactional
    void createGasto() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);
        var returnedGastoDTO = om.readValue(
            restGastoMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GastoDTO.class
        );

        // Validate the Gasto in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGasto = gastoMapper.toEntity(returnedGastoDTO);
        assertGastoUpdatableFieldsEquals(returnedGasto, getPersistedGasto(returnedGasto));

        insertedGasto = returnedGasto;
    }

    @Test
    @Transactional
    void createGastoWithExistingId() throws Exception {
        // Create the Gasto with an existing ID
        gasto.setId(1L);
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGastoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkMontoIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gasto.setMonto(null);

        // Create the Gasto, which fails.
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        restGastoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkFechaIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gasto.setFecha(null);

        // Create the Gasto, which fails.
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        restGastoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllGastos() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList
        restGastoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gasto.getId().intValue())))
            .andExpect(jsonPath("$.[*].monto").value(hasItem(sameNumber(DEFAULT_MONTO))))
            .andExpect(jsonPath("$.[*].fecha").value(hasItem(DEFAULT_FECHA.toString())))
            .andExpect(jsonPath("$.[*].descripcion").value(hasItem(DEFAULT_DESCRIPCION)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGastosWithEagerRelationshipsIsEnabled() throws Exception {
        when(gastoServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGastoMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(gastoServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGastosWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(gastoServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGastoMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(gastoRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getGasto() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get the gasto
        restGastoMockMvc
            .perform(get(ENTITY_API_URL_ID, gasto.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(gasto.getId().intValue()))
            .andExpect(jsonPath("$.monto").value(sameNumber(DEFAULT_MONTO)))
            .andExpect(jsonPath("$.fecha").value(DEFAULT_FECHA.toString()))
            .andExpect(jsonPath("$.descripcion").value(DEFAULT_DESCRIPCION));
    }

    @Test
    @Transactional
    void getGastosByIdFiltering() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        Long id = gasto.getId();

        defaultGastoFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultGastoFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultGastoFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto equals to
        defaultGastoFiltering("monto.equals=" + DEFAULT_MONTO, "monto.equals=" + UPDATED_MONTO);
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto in
        defaultGastoFiltering("monto.in=" + DEFAULT_MONTO + "," + UPDATED_MONTO, "monto.in=" + UPDATED_MONTO);
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto is not null
        defaultGastoFiltering("monto.specified=true", "monto.specified=false");
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto is greater than or equal to
        defaultGastoFiltering("monto.greaterThanOrEqual=" + DEFAULT_MONTO, "monto.greaterThanOrEqual=" + UPDATED_MONTO);
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto is less than or equal to
        defaultGastoFiltering("monto.lessThanOrEqual=" + DEFAULT_MONTO, "monto.lessThanOrEqual=" + SMALLER_MONTO);
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto is less than
        defaultGastoFiltering("monto.lessThan=" + UPDATED_MONTO, "monto.lessThan=" + DEFAULT_MONTO);
    }

    @Test
    @Transactional
    void getAllGastosByMontoIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where monto is greater than
        defaultGastoFiltering("monto.greaterThan=" + SMALLER_MONTO, "monto.greaterThan=" + DEFAULT_MONTO);
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha equals to
        defaultGastoFiltering("fecha.equals=" + DEFAULT_FECHA, "fecha.equals=" + UPDATED_FECHA);
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha in
        defaultGastoFiltering("fecha.in=" + DEFAULT_FECHA + "," + UPDATED_FECHA, "fecha.in=" + UPDATED_FECHA);
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha is not null
        defaultGastoFiltering("fecha.specified=true", "fecha.specified=false");
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha is greater than or equal to
        defaultGastoFiltering("fecha.greaterThanOrEqual=" + DEFAULT_FECHA, "fecha.greaterThanOrEqual=" + UPDATED_FECHA);
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha is less than or equal to
        defaultGastoFiltering("fecha.lessThanOrEqual=" + DEFAULT_FECHA, "fecha.lessThanOrEqual=" + SMALLER_FECHA);
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha is less than
        defaultGastoFiltering("fecha.lessThan=" + UPDATED_FECHA, "fecha.lessThan=" + DEFAULT_FECHA);
    }

    @Test
    @Transactional
    void getAllGastosByFechaIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where fecha is greater than
        defaultGastoFiltering("fecha.greaterThan=" + SMALLER_FECHA, "fecha.greaterThan=" + DEFAULT_FECHA);
    }

    @Test
    @Transactional
    void getAllGastosByDescripcionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where descripcion equals to
        defaultGastoFiltering("descripcion.equals=" + DEFAULT_DESCRIPCION, "descripcion.equals=" + UPDATED_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllGastosByDescripcionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where descripcion in
        defaultGastoFiltering("descripcion.in=" + DEFAULT_DESCRIPCION + "," + UPDATED_DESCRIPCION, "descripcion.in=" + UPDATED_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllGastosByDescripcionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where descripcion is not null
        defaultGastoFiltering("descripcion.specified=true", "descripcion.specified=false");
    }

    @Test
    @Transactional
    void getAllGastosByDescripcionContainsSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where descripcion contains
        defaultGastoFiltering("descripcion.contains=" + DEFAULT_DESCRIPCION, "descripcion.contains=" + UPDATED_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllGastosByDescripcionNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        // Get all the gastoList where descripcion does not contain
        defaultGastoFiltering("descripcion.doesNotContain=" + UPDATED_DESCRIPCION, "descripcion.doesNotContain=" + DEFAULT_DESCRIPCION);
    }

    @Test
    @Transactional
    void getAllGastosByCategoriaIsEqualToSomething() throws Exception {
        Categoria categoria;
        if (TestUtil.findAll(em, Categoria.class).isEmpty()) {
            gastoRepository.saveAndFlush(gasto);
            categoria = CategoriaResourceIT.createEntity();
        } else {
            categoria = TestUtil.findAll(em, Categoria.class).getFirst();
        }
        em.persist(categoria);
        em.flush();
        gasto.setCategoria(categoria);
        gastoRepository.saveAndFlush(gasto);
        Long categoriaId = categoria.getId();
        // Get all the gastoList where categoria equals to categoriaId
        defaultGastoShouldBeFound("categoriaId.equals=" + categoriaId);

        // Get all the gastoList where categoria equals to (categoriaId + 1)
        defaultGastoShouldNotBeFound("categoriaId.equals=" + (categoriaId + 1));
    }

    @Test
    @Transactional
    void getAllGastosByUserIsEqualToSomething() throws Exception {
        User user;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            gastoRepository.saveAndFlush(gasto);
            user = UserResourceIT.createEntity();
        } else {
            user = TestUtil.findAll(em, User.class).getFirst();
        }
        em.persist(user);
        em.flush();
        gasto.setUser(user);
        gastoRepository.saveAndFlush(gasto);
        Long userId = user.getId();
        // Get all the gastoList where user equals to userId
        defaultGastoShouldBeFound("userId.equals=" + userId);

        // Get all the gastoList where user equals to (userId + 1)
        defaultGastoShouldNotBeFound("userId.equals=" + (userId + 1));
    }

    private void defaultGastoFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultGastoShouldBeFound(shouldBeFound);
        defaultGastoShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultGastoShouldBeFound(String filter) throws Exception {
        restGastoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gasto.getId().intValue())))
            .andExpect(jsonPath("$.[*].monto").value(hasItem(sameNumber(DEFAULT_MONTO))))
            .andExpect(jsonPath("$.[*].fecha").value(hasItem(DEFAULT_FECHA.toString())))
            .andExpect(jsonPath("$.[*].descripcion").value(hasItem(DEFAULT_DESCRIPCION)));

        // Check, that the count call also returns 1
        restGastoMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultGastoShouldNotBeFound(String filter) throws Exception {
        restGastoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restGastoMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingGasto() throws Exception {
        // Get the gasto
        restGastoMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGasto() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gasto
        Gasto updatedGasto = gastoRepository.findById(gasto.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGasto are not directly saved in db
        em.detach(updatedGasto);
        updatedGasto.monto(UPDATED_MONTO).fecha(UPDATED_FECHA).descripcion(UPDATED_DESCRIPCION);
        GastoDTO gastoDTO = gastoMapper.toDto(updatedGasto);

        restGastoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, gastoDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO))
            )
            .andExpect(status().isOk());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGastoToMatchAllProperties(updatedGasto);
    }

    @Test
    @Transactional
    void putNonExistingGasto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gasto.setId(longCount.incrementAndGet());

        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGastoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, gastoDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGasto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gasto.setId(longCount.incrementAndGet());

        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGastoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(gastoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGasto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gasto.setId(longCount.incrementAndGet());

        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGastoMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(gastoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGastoWithPatch() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gasto using partial update
        Gasto partialUpdatedGasto = new Gasto();
        partialUpdatedGasto.setId(gasto.getId());

        partialUpdatedGasto.descripcion(UPDATED_DESCRIPCION);

        restGastoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGasto.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGasto))
            )
            .andExpect(status().isOk());

        // Validate the Gasto in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGastoUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedGasto, gasto), getPersistedGasto(gasto));
    }

    @Test
    @Transactional
    void fullUpdateGastoWithPatch() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gasto using partial update
        Gasto partialUpdatedGasto = new Gasto();
        partialUpdatedGasto.setId(gasto.getId());

        partialUpdatedGasto.monto(UPDATED_MONTO).fecha(UPDATED_FECHA).descripcion(UPDATED_DESCRIPCION);

        restGastoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGasto.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGasto))
            )
            .andExpect(status().isOk());

        // Validate the Gasto in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGastoUpdatableFieldsEquals(partialUpdatedGasto, getPersistedGasto(partialUpdatedGasto));
    }

    @Test
    @Transactional
    void patchNonExistingGasto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gasto.setId(longCount.incrementAndGet());

        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGastoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, gastoDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(gastoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGasto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gasto.setId(longCount.incrementAndGet());

        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGastoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(gastoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGasto() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gasto.setId(longCount.incrementAndGet());

        // Create the Gasto
        GastoDTO gastoDTO = gastoMapper.toDto(gasto);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGastoMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(gastoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Gasto in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGasto() throws Exception {
        // Initialize the database
        insertedGasto = gastoRepository.saveAndFlush(gasto);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the gasto
        restGastoMockMvc
            .perform(delete(ENTITY_API_URL_ID, gasto.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return gastoRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Gasto getPersistedGasto(Gasto gasto) {
        return gastoRepository.findById(gasto.getId()).orElseThrow();
    }

    protected void assertPersistedGastoToMatchAllProperties(Gasto expectedGasto) {
        assertGastoAllPropertiesEquals(expectedGasto, getPersistedGasto(expectedGasto));
    }

    protected void assertPersistedGastoToMatchUpdatableProperties(Gasto expectedGasto) {
        assertGastoAllUpdatablePropertiesEquals(expectedGasto, getPersistedGasto(expectedGasto));
    }
}
