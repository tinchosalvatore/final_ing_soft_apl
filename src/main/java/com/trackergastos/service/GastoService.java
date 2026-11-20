package com.trackergastos.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.trackergastos.domain.Categoria;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import com.trackergastos.repository.CategoriaRepository;
import com.trackergastos.repository.GastoRepository;
import com.trackergastos.repository.UserRepository;
import com.trackergastos.security.SecurityUtils;
import com.trackergastos.service.dto.GastoDTO;
import com.trackergastos.service.mapper.GastoMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de los gastos (spec 001; ADR-0007). Territorio del desarrollador: al regenerar, responder {@code n}.
 * <ul>
 *   <li><b>S1</b>: toda lectura, edición o borrado busca el gasto por id <b>y</b> dueño = usuario autenticado. Un gasto
 *   ajeno se comporta igual que uno inexistente ({@link Optional#empty()} / {@code false} → 404 en el resource).</li>
 *   <li><b>S3</b>: el dueño lo asigna el servidor; el {@code user} del DTO se ignora al crear y al editar.</li>
 *   <li><b>S4</b>: monto, fecha y categoría se validan acá ({@link CampoInvalidoException}).</li>
 * </ul>
 * Los logs llevan solo ids: nunca montos ni descripciones (AGENTS.md, regla 6).
 */
@Service
@Transactional
public class GastoService {

    private static final Logger LOG = LoggerFactory.getLogger(GastoService.class);

    private static final String ENTIDAD = "gasto";

    /** "Hoy" según la spec 001 (definiciones): la fecha actual en Buenos Aires, no la del servidor. */
    static final ZoneId ZONA_HOY = ZoneId.of("America/Argentina/Buenos_Aires");

    static final BigDecimal MONTO_MAXIMO = new BigDecimal("10000000");

    private static final int DECIMALES_MAXIMOS = 2;

    /** Spec 001, regla 2: descripción opcional de hasta 140 caracteres. */
    static final int DESCRIPCION_MAXIMA = 140;

    private final GastoRepository gastoRepository;

    private final CategoriaRepository categoriaRepository;

    private final UserRepository userRepository;

    private final GastoMapper gastoMapper;

    public GastoService(
        GastoRepository gastoRepository,
        CategoriaRepository categoriaRepository,
        UserRepository userRepository,
        GastoMapper gastoMapper
    ) {
        this.gastoRepository = gastoRepository;
        this.categoriaRepository = categoriaRepository;
        this.userRepository = userRepository;
        this.gastoMapper = gastoMapper;
    }

    /**
     * Registra un gasto del usuario autenticado (001-CA01). El dueño del DTO se ignora (001-CA04, S3).
     */
    public GastoDTO save(GastoDTO gastoDTO) {
        Categoria categoria = validar(gastoDTO.getMonto(), gastoDTO.getFecha(), idCategoria(gastoDTO), gastoDTO.getDescripcion());
        Gasto gasto = new Gasto().monto(gastoDTO.getMonto()).fecha(gastoDTO.getFecha()).descripcion(gastoDTO.getDescripcion());
        gasto.setCategoria(categoria);
        gasto.setUser(usuarioActual());
        gasto = gastoRepository.save(gasto);
        LOG.info("Gasto creado", kv("evento", "GASTO_CREADO"), kv("gastoId", gasto.getId()));
        return gastoMapper.toDto(gasto);
    }

    /**
     * Reemplaza los datos de un gasto propio (PUT). El dueño no cambia (S3).
     *
     * @return vacío si el gasto no existe o no es del usuario autenticado (S1).
     */
    public Optional<GastoDTO> update(GastoDTO gastoDTO) {
        return buscarPropio(gastoDTO.getId(), "editar").map(gasto -> {
            Categoria categoria = validar(gastoDTO.getMonto(), gastoDTO.getFecha(), idCategoria(gastoDTO), gastoDTO.getDescripcion());
            gasto.setMonto(gastoDTO.getMonto());
            gasto.setFecha(gastoDTO.getFecha());
            gasto.setDescripcion(gastoDTO.getDescripcion());
            gasto.setCategoria(categoria);
            LOG.info("Gasto editado", kv("evento", "GASTO_EDITADO"), kv("gastoId", gasto.getId()));
            return gastoMapper.toDto(gastoRepository.save(gasto));
        });
    }

    /**
     * Actualiza solo los campos presentes de un gasto propio (PATCH), con las mismas reglas que el PUT.
     * No usa {@code GastoMapper.partialUpdate}: copiaría el {@code user} del DTO y modificaría la categoría gestionada.
     *
     * @return vacío si el gasto no existe o no es del usuario autenticado (S1).
     */
    public Optional<GastoDTO> partialUpdate(GastoDTO gastoDTO) {
        return buscarPropio(gastoDTO.getId(), "editar").map(gasto -> {
            BigDecimal monto = gastoDTO.getMonto() != null ? gastoDTO.getMonto() : gasto.getMonto();
            LocalDate fecha = gastoDTO.getFecha() != null ? gastoDTO.getFecha() : gasto.getFecha();
            Long idCategoria = gastoDTO.getCategoria() != null ? gastoDTO.getCategoria().getId() : gasto.getCategoria().getId();
            String descripcion = gastoDTO.getDescripcion() != null ? gastoDTO.getDescripcion() : gasto.getDescripcion();
            // O1 del verify: el PATCH no pasa por @Valid, así que el largo de la descripción también lo valida el service
            Categoria categoria = validar(monto, fecha, idCategoria, descripcion);
            gasto.setMonto(monto);
            gasto.setFecha(fecha);
            gasto.setCategoria(categoria);
            gasto.setDescripcion(descripcion);
            LOG.info("Gasto editado", kv("evento", "GASTO_EDITADO"), kv("gastoId", gasto.getId()));
            return gastoMapper.toDto(gastoRepository.save(gasto));
        });
    }

    /**
     * Un gasto propio por id (001-CA07).
     *
     * @return vacío si no existe o no es del usuario autenticado: las dos situaciones son indistinguibles (S1).
     */
    @Transactional(readOnly = true)
    public Optional<GastoDTO> findOne(Long id) {
        return buscarPropio(id, "ver").map(gastoMapper::toDto);
    }

    /**
     * Borra un gasto propio (001-CA08).
     *
     * @return {@code false} si no existe o no es del usuario autenticado (S1).
     */
    public boolean delete(Long id) {
        return buscarPropio(id, "borrar")
            .map(gasto -> {
                gastoRepository.delete(gasto);
                LOG.info("Gasto borrado", kv("evento", "GASTO_BORRADO"), kv("gastoId", id));
                return true;
            })
            .orElse(false);
    }

    /**
     * Hoy en {@link #ZONA_HOY}. Es la única definición de "hoy" del sistema: la usan la spec 001 (fecha no futura) y la
     * spec 002 (mes actual del resumen).
     *
     * @return la fecha actual en Buenos Aires.
     */
    public static LocalDate hoy() {
        return LocalDate.now(ZONA_HOY);
    }

    /**
     * El gasto del usuario autenticado. Si el id existe pero es de otro, registra {@code GASTO_AJENO} (spec 004); la
     * respuesta sigue siendo la de "no existe" (S1).
     */
    private Optional<Gasto> buscarPropio(Long id, String operacion) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<Gasto> propio = gastoRepository.findOne(GastoSpecifications.conId(id).and(GastoSpecifications.delDueno(loginActual())));
        if (propio.isEmpty() && gastoRepository.existsById(id)) {
            LOG.warn("Gasto ajeno", kv("evento", "GASTO_AJENO"), kv("gastoId", id), kv("operacion", operacion));
        }
        return propio;
    }

    /**
     * Reglas de un gasto válido (spec 001, regla 2; criterios 001-CA02 y CA03). Devuelve la categoría ya resuelta.
     */
    private Categoria validar(BigDecimal monto, LocalDate fecha, Long idCategoria, String descripcion) {
        if (monto == null) {
            throw new CampoInvalidoException(ENTIDAD, "monto", "El monto es obligatorio");
        }
        if (monto.signum() <= 0) {
            throw new CampoInvalidoException(ENTIDAD, "monto", "El monto tiene que ser mayor a 0");
        }
        if (monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new CampoInvalidoException(ENTIDAD, "monto", "El monto no puede superar 10.000.000");
        }
        if (monto.stripTrailingZeros().scale() > DECIMALES_MAXIMOS) {
            throw new CampoInvalidoException(ENTIDAD, "monto", "El monto puede tener como máximo 2 decimales");
        }
        if (fecha == null) {
            throw new CampoInvalidoException(ENTIDAD, "fecha", "La fecha es obligatoria");
        }
        if (fecha.isAfter(hoy())) {
            throw new CampoInvalidoException(ENTIDAD, "fecha", "La fecha no puede ser posterior a hoy");
        }
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAXIMA) {
            throw new CampoInvalidoException(ENTIDAD, "descripcion", "La descripción puede tener como máximo 140 caracteres");
        }
        if (idCategoria == null) {
            throw new CampoInvalidoException(ENTIDAD, "categoria", "La categoría es obligatoria");
        }
        return categoriaRepository
            .findById(idCategoria)
            .orElseThrow(() -> new CampoInvalidoException(ENTIDAD, "categoria", "La categoría no existe"));
    }

    private static Long idCategoria(GastoDTO gastoDTO) {
        return gastoDTO.getCategoria() != null ? gastoDTO.getCategoria().getId() : null;
    }

    private User usuarioActual() {
        String login = loginActual();
        return userRepository.findOneByLogin(login).orElseThrow(() -> new IllegalStateException("Usuario autenticado inexistente"));
    }

    static String loginActual() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Operación de gastos sin sesión"));
    }
}
