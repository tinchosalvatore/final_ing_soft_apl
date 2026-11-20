package com.trackergastos.service;

import com.trackergastos.domain.Gasto;
import com.trackergastos.repository.GastoRepository;
import com.trackergastos.service.dto.GastoDTO;
import com.trackergastos.service.mapper.GastoMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.trackergastos.domain.Gasto}.
 */
@Service
@Transactional
public class GastoService {

    private static final Logger LOG = LoggerFactory.getLogger(GastoService.class);

    private final GastoRepository gastoRepository;

    private final GastoMapper gastoMapper;

    public GastoService(GastoRepository gastoRepository, GastoMapper gastoMapper) {
        this.gastoRepository = gastoRepository;
        this.gastoMapper = gastoMapper;
    }

    /**
     * Save a gasto.
     *
     * @param gastoDTO the entity to save.
     * @return the persisted entity.
     */
    public GastoDTO save(GastoDTO gastoDTO) {
        LOG.debug("Request to save Gasto : {}", gastoDTO);
        Gasto gasto = gastoMapper.toEntity(gastoDTO);
        gasto = gastoRepository.save(gasto);
        return gastoMapper.toDto(gasto);
    }

    /**
     * Update a gasto.
     *
     * @param gastoDTO the entity to save.
     * @return the persisted entity.
     */
    public GastoDTO update(GastoDTO gastoDTO) {
        LOG.debug("Request to update Gasto : {}", gastoDTO);
        Gasto gasto = gastoMapper.toEntity(gastoDTO);
        gasto = gastoRepository.save(gasto);
        return gastoMapper.toDto(gasto);
    }

    /**
     * Partially update a gasto.
     *
     * @param gastoDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<GastoDTO> partialUpdate(GastoDTO gastoDTO) {
        LOG.debug("Request to partially update Gasto : {}", gastoDTO);

        return gastoRepository
            .findById(gastoDTO.getId())
            .map(existingGasto -> {
                gastoMapper.partialUpdate(existingGasto, gastoDTO);

                return existingGasto;
            })
            .map(gastoRepository::save)
            .map(gastoMapper::toDto);
    }

    /**
     * Get all the gastos with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<GastoDTO> findAllWithEagerRelationships(Pageable pageable) {
        return gastoRepository.findAllWithEagerRelationships(pageable).map(gastoMapper::toDto);
    }

    /**
     * Get one gasto by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<GastoDTO> findOne(Long id) {
        LOG.debug("Request to get Gasto : {}", id);
        return gastoRepository.findOneWithEagerRelationships(id).map(gastoMapper::toDto);
    }

    /**
     * Delete the gasto by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Gasto : {}", id);
        gastoRepository.deleteById(id);
    }
}
