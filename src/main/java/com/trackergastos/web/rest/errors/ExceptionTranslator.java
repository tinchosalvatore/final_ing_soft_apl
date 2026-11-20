package com.trackergastos.web.rest.errors;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

import com.trackergastos.service.CampoInvalidoException;
import com.trackergastos.service.ReglaDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;
import tech.jhipster.web.util.HeaderUtil;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    private static final String FIELD_ERRORS_KEY = "fieldErrors";
    private static final String MESSAGE_KEY = "message";
    private static final String PATH_KEY = "path";
    private static final boolean CAUSAL_CHAIN_ENABLED = false;

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionTranslator.class);

    static final String DETALLE_ERROR_INESPERADO = "Unexpected error";
    static final String DETALLE_GENERICO = "Invalid request";

    @Value("${jhipster.clientApp.name:trackerGastos}")
    private String applicationName;

    private final Environment env;

    public ExceptionTranslator(Environment env) {
        this.env = env;
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleAnyException(Throwable ex, NativeWebRequest request) {
        LOG.debug("Converting Exception to Problem Details:", ex);
        ProblemDetailWithCause pdCause = wrapAndCustomizeProblem(ex, request);
        return handleExceptionInternal((Exception) ex, pdCause, buildHeaders(ex), HttpStatusCode.valueOf(pdCause.getStatus()), request);
    }

    @SuppressWarnings("java:S2638")
    @Nullable
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        body = body == null ? wrapAndCustomizeProblem(ex, (NativeWebRequest) request) : body;
        registrarEvento(ex, statusCode, (NativeWebRequest) request);
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    /**
     * Spec 004: el detalle técnico de un error inesperado va solo al log ({@code ERROR_INESPERADO}, con el stack); el
     * rechazo de un {@code @PreAuthorize} se registra como {@code ACCESO_DENEGADO}.
     */
    private void registrarEvento(Throwable ex, HttpStatusCode statusCode, NativeWebRequest request) {
        if (statusCode.is5xxServerError()) {
            LOG.error("Error inesperado", kv("evento", "ERROR_INESPERADO"), kv("tipoError", ex.getClass().getName()), ex);
        } else if (ex instanceof AccessDeniedException) {
            HttpServletRequest nativeRequest = request != null ? request.getNativeRequest(HttpServletRequest.class) : null;
            String operacion = nativeRequest != null ? nativeRequest.getMethod() + " " + nativeRequest.getRequestURI() : null;
            LOG.warn("Acceso denegado", kv("evento", "ACCESO_DENEGADO"), kv("operacion", operacion));
        }
    }

    protected ProblemDetailWithCause wrapAndCustomizeProblem(Throwable ex, NativeWebRequest request) {
        return customizeProblem(getProblemDetailWithCause(ex), ex, request);
    }

    private ProblemDetailWithCause getProblemDetailWithCause(Throwable ex) {
        if (
            ex instanceof com.trackergastos.service.UsernameAlreadyUsedException
        ) return (ProblemDetailWithCause) new LoginAlreadyUsedException().getBody();
        if (
            ex instanceof com.trackergastos.service.EmailAlreadyUsedException
        ) return (ProblemDetailWithCause) new EmailAlreadyUsedException().getBody();
        if (
            ex instanceof com.trackergastos.service.InvalidPasswordException
        ) return (ProblemDetailWithCause) new InvalidPasswordException().getBody();

        if (
            ex instanceof ErrorResponseException exp && exp.getBody() instanceof ProblemDetailWithCause problemDetailWithCause
        ) return problemDetailWithCause;
        return ProblemDetailWithCauseBuilder.instance().withStatus(toStatus(ex).value()).build();
    }

    protected ProblemDetailWithCause customizeProblem(ProblemDetailWithCause problem, Throwable err, NativeWebRequest request) {
        if (problem.getStatus() <= 0) problem.setStatus(toStatus(err));

        if (problem.getType() == null || problem.getType().equals(URI.create("about:blank"))) problem.setType(getMappedType(err));

        // higher precedence to Custom/ResponseStatus types
        String title = extractTitle(err, problem.getStatus());
        String problemTitle = problem.getTitle();
        if (problemTitle == null || !problemTitle.equals(title)) {
            problem.setTitle(title);
        }

        if (problem.getDetail() == null) {
            // higher precedence to cause
            problem.setDetail(getCustomizedErrorDetails(err));
        }

        Map<String, Object> problemProperties = problem.getProperties();
        if (problemProperties == null || !problemProperties.containsKey(MESSAGE_KEY)) problem.setProperty(
            MESSAGE_KEY,
            getMappedMessageKey(err) != null ? getMappedMessageKey(err) : "error.http." + problem.getStatus()
        );

        if (problemProperties == null || !problemProperties.containsKey(PATH_KEY)) problem.setProperty(PATH_KEY, getPathValue(request));

        if (
            err instanceof MethodArgumentNotValidException fieldException &&
            (problemProperties == null || !problemProperties.containsKey(FIELD_ERRORS_KEY))
        ) problem.setProperty(FIELD_ERRORS_KEY, getFieldErrors(fieldException));

        // Spec 001 §5.1: una regla de un campo validada en el service sale con el mismo formato que Bean Validation
        if (
            err instanceof CampoInvalidoException campoInvalido &&
            (problemProperties == null || !problemProperties.containsKey(FIELD_ERRORS_KEY))
        ) problem.setProperty(
            FIELD_ERRORS_KEY,
            List.of(new FieldErrorVM(campoInvalido.getEntidad(), campoInvalido.getCampo(), campoInvalido.getMessage()))
        );

        problem.setCause(buildCause(err.getCause(), request).orElse(null));

        return problem;
    }

    private String extractTitle(Throwable err, int statusCode) {
        return getCustomizedTitle(err) != null ? getCustomizedTitle(err) : extractTitleForResponseStatus(err, statusCode);
    }

    private List<FieldErrorVM> getFieldErrors(MethodArgumentNotValidException ex) {
        return ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(f ->
                new FieldErrorVM(
                    f.getObjectName().replaceFirst("DTO$", ""),
                    f.getField(),
                    StringUtils.isNotBlank(f.getDefaultMessage()) ? f.getDefaultMessage() : f.getCode()
                )
            )
            .toList();
    }

    private String extractTitleForResponseStatus(Throwable err, int statusCode) {
        var specialStatus = extractResponseStatus(err);
        return specialStatus == null ? HttpStatus.valueOf(statusCode).getReasonPhrase() : specialStatus.reason();
    }

    private String extractURI(NativeWebRequest request) {
        HttpServletRequest nativeRequest = request.getNativeRequest(HttpServletRequest.class);
        return nativeRequest != null ? nativeRequest.getRequestURI() : StringUtils.EMPTY;
    }

    private HttpStatus toStatus(final Throwable throwable) {
        // Let the ErrorResponse take this responsibility
        if (throwable instanceof ErrorResponse err) return HttpStatus.valueOf(err.getBody().getStatus());

        return Optional.ofNullable(getMappedStatus(throwable)).orElse(
            Optional.ofNullable(resolveResponseStatus(throwable)).map(ResponseStatus::value).orElse(HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    private ResponseStatus extractResponseStatus(final Throwable throwable) {
        return resolveResponseStatus(throwable);
    }

    private ResponseStatus resolveResponseStatus(final Throwable type) {
        final ResponseStatus candidate = findMergedAnnotation(type.getClass(), ResponseStatus.class);
        return candidate == null && type.getCause() != null ? resolveResponseStatus(type.getCause()) : candidate;
    }

    private URI getMappedType(Throwable err) {
        if (
            err instanceof MethodArgumentNotValidException ||
            err instanceof ConstraintViolationException ||
            err instanceof CampoInvalidoException
        ) return ErrorConstants.CONSTRAINT_VIOLATION_TYPE;
        return ErrorConstants.DEFAULT_TYPE;
    }

    private String getMappedMessageKey(Throwable err) {
        if (
            err instanceof MethodArgumentNotValidException ||
            err instanceof ConstraintViolationException ||
            err instanceof CampoInvalidoException
        ) {
            return ErrorConstants.ERR_VALIDATION;
        } else if (err instanceof ReglaDeNegocioException reglaDeNegocio) {
            return "error." + reglaDeNegocio.getClave();
        } else if (err instanceof ConcurrencyFailureException || err.getCause() instanceof ConcurrencyFailureException) {
            return ErrorConstants.ERR_CONCURRENCY_FAILURE;
        }
        return null;
    }

    private String getCustomizedTitle(Throwable err) {
        if (
            err instanceof MethodArgumentNotValidException || err instanceof ConstraintViolationException
        ) return "Method argument not valid";
        return null;
    }

    /**
     * Spec 004 (§5.1, CA05): en <b>todos</b> los perfiles, ningún detalle expone clases, paquetes ni SQL. Los 5xx llevan
     * un mensaje genérico; el detalle técnico queda en el log ({@link #registrarEvento}).
     */
    private String getCustomizedErrorDetails(Throwable err) {
        if (toStatus(err).is5xxServerError()) return DETALLE_ERROR_INESPERADO;
        if (err instanceof HttpMessageConversionException) return "Unable to convert http message";
        if (err instanceof DataAccessException) return "Failure during data access";
        String detalle = err.getCause() != null ? err.getCause().getMessage() : err.getMessage();
        if (!esInterno(detalle)) return detalle;
        // Ej.: getMessage() de un BadRequestAlertException incluye el toString() del cuerpo (ProblemDetailWithCause[...])
        if (err instanceof ErrorResponse errorResponse && errorResponse.getBody().getTitle() != null) return errorResponse
            .getBody()
            .getTitle();
        return DETALLE_GENERICO;
    }

    private boolean esInterno(String mensaje) {
        return (
            mensaje != null &&
            (containsPackageName(mensaje) || Strings.CI.containsAny(mensaje, "Exception", "ProblemDetail", "SQL", "select ", "insert "))
        );
    }

    private HttpStatus getMappedStatus(Throwable err) {
        // Where we disagree with Spring defaults
        if (err instanceof AccessDeniedException) return HttpStatus.FORBIDDEN;
        if (err instanceof ConcurrencyFailureException) return HttpStatus.CONFLICT;
        if (err instanceof BadCredentialsException) return HttpStatus.UNAUTHORIZED;
        if (err instanceof ConstraintViolationException) return HttpStatus.BAD_REQUEST;
        if (err instanceof CampoInvalidoException || err instanceof ReglaDeNegocioException) return HttpStatus.BAD_REQUEST;
        return null;
    }

    private URI getPathValue(NativeWebRequest request) {
        if (request == null) return URI.create("about:blank");
        return URI.create(extractURI(request));
    }

    private HttpHeaders buildHeaders(Throwable err) {
        return err instanceof BadRequestAlertException badRequestAlertException
            ? HeaderUtil.createFailureAlert(
                  applicationName,
                  true,
                  badRequestAlertException.getEntityName(),
                  badRequestAlertException.getErrorKey(),
                  badRequestAlertException.getMessage()
              )
            : null;
    }

    public Optional<ProblemDetailWithCause> buildCause(final Throwable throwable, NativeWebRequest request) {
        if (throwable != null && isCausalChainEnabled()) {
            return Optional.of(customizeProblem(getProblemDetailWithCause(throwable), throwable, request));
        }
        return Optional.empty();
    }

    private boolean isCausalChainEnabled() {
        // Customize as per the needs
        return CAUSAL_CHAIN_ENABLED;
    }

    private boolean containsPackageName(String message) {
        // This list is for sure not complete
        return Strings.CS.containsAny(message, "org.", "java.", "net.", "jakarta.", "javax.", "com.", "io.", "de.", "com.trackergastos");
    }
}
