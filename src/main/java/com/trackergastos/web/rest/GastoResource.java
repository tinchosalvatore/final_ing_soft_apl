package com.trackergastos.web.rest;

import com.trackergastos.service.GastoQueryService;
import com.trackergastos.service.GastoService;
import com.trackergastos.service.criteria.GastoCriteria;
import com.trackergastos.service.dto.GastoDTO;
import com.trackergastos.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;

/**
 * REST controller for managing {@link com.trackergastos.domain.Gasto}.
 * Territorio del desarrollador (ADR-0007): las reglas viven en {@link GastoService} y {@link GastoQueryService};
 * acá solo se traduce "no es mío o no existe" a 404 (spec 001 §5.1). Logs sin montos ni descripciones.
 */
@RestController
@RequestMapping("/api/gastos")
public class GastoResource {

    private static final Logger LOG = LoggerFactory.getLogger(GastoResource.class);

    private static final String ENTITY_NAME = "gasto";

    @Value("${jhipster.clientApp.name:trackerGastos}")
    private String applicationName;

    private final GastoService gastoService;

    private final GastoQueryService gastoQueryService;

    public GastoResource(GastoService gastoService, GastoQueryService gastoQueryService) {
        this.gastoService = gastoService;
        this.gastoQueryService = gastoQueryService;
    }

    /**
     * Gasto ajeno o inexistente (spec 001 §5.1, defensa S1): siempre el mismo 404, que es el que da
     * {@code ResponseUtil.wrapOrNotFound} en el GET, para que las dos situaciones sean indistinguibles (001-CA07).
     */
    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    /**
     * {@code POST  /gastos} : Create a new gasto.
     *
     * @param gastoDTO the gastoDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new gastoDTO, or with status {@code 400 (Bad Request)} if the gasto already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GastoDTO> createGasto(@Valid @RequestBody GastoDTO gastoDTO) throws URISyntaxException {
        LOG.debug("REST: crear gasto");
        if (gastoDTO.getId() != null) {
            throw new BadRequestAlertException("A new gasto cannot already have an ID", ENTITY_NAME, "idexists");
        }
        gastoDTO = gastoService.save(gastoDTO);
        return ResponseEntity.created(new URI("/api/gastos/" + gastoDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, gastoDTO.getId().toString()))
            .body(gastoDTO);
    }

    /**
     * {@code PUT  /gastos/:id} : Updates an existing gasto.
     *
     * @param id the id of the gastoDTO to save.
     * @param gastoDTO the gastoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated gastoDTO,
     * or with status {@code 400 (Bad Request)} if the gastoDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the gastoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GastoDTO> updateGasto(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody GastoDTO gastoDTO
    ) throws URISyntaxException {
        LOG.debug("REST: actualizar gasto id={}", id);
        if (gastoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, gastoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        gastoDTO = gastoService.update(gastoDTO).orElseThrow(GastoResource::noEncontrado);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, gastoDTO.getId().toString()))
            .body(gastoDTO);
    }

    /**
     * {@code PATCH  /gastos/:id} : Partial updates given fields of an existing gasto, field will ignore if it is null
     *
     * @param id the id of the gastoDTO to save.
     * @param gastoDTO the gastoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated gastoDTO,
     * or with status {@code 400 (Bad Request)} if the gastoDTO is not valid,
     * or with status {@code 404 (Not Found)} if the gastoDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the gastoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GastoDTO> partialUpdateGasto(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody GastoDTO gastoDTO
    ) throws URISyntaxException {
        LOG.debug("REST: actualizar parcialmente gasto id={}", id);
        if (gastoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, gastoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        GastoDTO result = gastoService.partialUpdate(gastoDTO).orElseThrow(GastoResource::noEncontrado);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /gastos} : get all the Gastos.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Gastos in body.
     */
    @GetMapping("")
    public ResponseEntity<List<GastoDTO>> getAllGastos(
        GastoCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST: listar gastos");

        Page<GastoDTO> page = gastoQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /gastos/count} : count all the gastos.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countGastos(GastoCriteria criteria) {
        LOG.debug("REST: contar gastos");
        return ResponseEntity.ok().body(gastoQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /gastos/:id} : get the "id" gasto.
     *
     * @param id the id of the gastoDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the gastoDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GastoDTO> getGasto(@PathVariable("id") Long id) {
        LOG.debug("REST: ver gasto id={}", id);
        return ResponseEntity.ok().body(gastoService.findOne(id).orElseThrow(GastoResource::noEncontrado));
    }

    /**
     * {@code DELETE  /gastos/:id} : delete the "id" gasto.
     *
     * @param id the id of the gastoDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGasto(@PathVariable("id") Long id) {
        LOG.debug("REST: borrar gasto id={}", id);
        if (!gastoService.delete(id)) {
            throw noEncontrado();
        }
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
