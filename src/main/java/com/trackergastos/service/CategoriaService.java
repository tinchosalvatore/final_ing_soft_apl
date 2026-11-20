package com.trackergastos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.trackergastos.domain.Categoria;
import com.trackergastos.repository.CategoriaRepository;
import com.trackergastos.repository.GastoRepository;
import com.trackergastos.service.dto.CategoriaDTO;
import com.trackergastos.service.mapper.CategoriaMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.trackergastos.domain.Categoria}.
 * Reglas de la spec 001 (territorio del desarrollador, ADR-0007):
 * <ul>
 *   <li>Regla 11 / 001-CA12: el nombre se guarda sin espacios al inicio ni al final, tiene entre 2 y 40 caracteres y es
 *   único sin distinguir mayúsculas.</li>
 *   <li>Regla 12 / 001-CA13: no se borra una categoría con gastos asociados.</li>
 * </ul>
 * Que solo un ADMIN escriba (regla 10, S10) se controla en {@code CategoriaResource}.
 */
@Service
@Transactional
public class CategoriaService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoriaService.class);

    private final CategoriaRepository categoriaRepository;

    private final CategoriaMapper categoriaMapper;

    private final GastoRepository gastoRepository;

    private static final String ENTIDAD = "categoria";

    private static final int NOMBRE_MINIMO = 2;

    private static final int NOMBRE_MAXIMO = 40;

    public CategoriaService(CategoriaRepository categoriaRepository, CategoriaMapper categoriaMapper, GastoRepository gastoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
        this.gastoRepository = gastoRepository;
    }

    /**
     * Normaliza y valida el nombre (regla 11). Devuelve el nombre a guardar.
     *
     * @param idExcluido la categoría que se renombra (no choca consigo misma), o {@code null} al crear.
     */
    private String nombreValido(String nombre, Long idExcluido) {
        String normalizado = nombre == null ? "" : nombre.strip();
        if (normalizado.length() < NOMBRE_MINIMO || normalizado.length() > NOMBRE_MAXIMO) {
            throw new CampoInvalidoException(ENTIDAD, "nombre", "El nombre tiene que tener entre 2 y 40 caracteres");
        }
        if (categoriaRepository.existeNombreNormalizado(normalizado, idExcluido)) {
            throw new ReglaDeNegocioException(ENTIDAD, "nombreduplicado", "Ya existe una categoría con ese nombre");
        }
        return normalizado;
    }

    /**
     * Save a categoria.
     *
     * @param categoriaDTO the entity to save.
     * @return the persisted entity.
     */
    public CategoriaDTO save(CategoriaDTO categoriaDTO) {
        Categoria categoria = categoriaMapper.toEntity(categoriaDTO);
        categoria.setNombre(nombreValido(categoriaDTO.getNombre(), null));
        categoria = categoriaRepository.save(categoria);
        LOG.info("Categoría creada", kv("evento", "CATEGORIA_CREADA"), kv("categoriaId", categoria.getId()));
        return categoriaMapper.toDto(categoria);
    }

    /**
     * Update a categoria.
     *
     * @param categoriaDTO the entity to save.
     * @return the persisted entity.
     */
    public CategoriaDTO update(CategoriaDTO categoriaDTO) {
        Categoria categoria = categoriaMapper.toEntity(categoriaDTO);
        categoria.setNombre(nombreValido(categoriaDTO.getNombre(), categoriaDTO.getId()));
        categoria = categoriaRepository.save(categoria);
        LOG.info("Categoría editada", kv("evento", "CATEGORIA_EDITADA"), kv("categoriaId", categoria.getId()));
        return categoriaMapper.toDto(categoria);
    }

    /**
     * Partially update a categoria.
     *
     * @param categoriaDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CategoriaDTO> partialUpdate(CategoriaDTO categoriaDTO) {
        return categoriaRepository
            .findById(categoriaDTO.getId())
            .map(existingCategoria -> {
                if (categoriaDTO.getNombre() != null) {
                    categoriaDTO.setNombre(nombreValido(categoriaDTO.getNombre(), existingCategoria.getId()));
                }
                categoriaMapper.partialUpdate(existingCategoria, categoriaDTO);

                return existingCategoria;
            })
            .map(categoriaRepository::save)
            .map(categoria -> {
                LOG.info("Categoría editada", kv("evento", "CATEGORIA_EDITADA"), kv("categoriaId", categoria.getId()));
                return categoriaMapper.toDto(categoria);
            });
    }

    /**
     * Get all the categorias.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> findAll() {
        LOG.debug("Request to get all Categorias");
        return categoriaRepository.findAll().stream().map(categoriaMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one categoria by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CategoriaDTO> findOne(Long id) {
        LOG.debug("Request to get Categoria : {}", id);
        return categoriaRepository.findById(id).map(categoriaMapper::toDto);
    }

    /**
     * Delete the categoria by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        if (gastoRepository.exists(GastoSpecifications.deCategoria(id))) {
            throw new ReglaDeNegocioException(ENTIDAD, "categoriaenuso", "No se puede borrar una categoría que tiene gastos");
        }
        categoriaRepository.deleteById(id);
        LOG.info("Categoría borrada", kv("evento", "CATEGORIA_BORRADA"), kv("categoriaId", id));
    }
}
