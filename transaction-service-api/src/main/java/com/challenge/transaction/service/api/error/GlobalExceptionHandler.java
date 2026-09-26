package com.challenge.transaction.service.api.error;

import com.challenge.transaction.service.domain.exception.InvalidOperationException;
import com.challenge.transaction.service.domain.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem =
                problem(
                        HttpStatus.BAD_REQUEST,
                        "Solicitud inválida",
                        "Uno o más campos no cumplen el contrato de entrada.",
                        "validation_error");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(
                        violation ->
                                errors.putIfAbsent(
                                        violation.getPropertyPath().toString(),
                                        violation.getMessage()));

        ProblemDetail problem =
                problem(
                        HttpStatus.BAD_REQUEST,
                        "Parámetros inválidos",
                        "Uno o más parámetros no cumplen el contrato de entrada.",
                        "constraint_violation");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformedBody() {
        return response(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "El cuerpo de la solicitud no tiene un formato válido.",
                "malformed_request");
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> badCredentials() {
        return response(
                HttpStatus.UNAUTHORIZED,
                "No autorizado",
                "Credenciales inválidas.",
                "invalid_credentials");
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException ex) {
        return response(
                HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), "not_found");
    }

    @ExceptionHandler(InvalidOperationException.class)
    ResponseEntity<ProblemDetail> invalidOperation(InvalidOperationException ex) {
        return response(
                HttpStatus.BAD_REQUEST, "Operación inválida", ex.getMessage(), "invalid_operation");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> dataIntegrity(DataIntegrityViolationException ex) {
        String correlationId = UUID.randomUUID().toString();
        LOGGER.warn("event=DATA_INTEGRITY_VIOLATION correlationId={}", correlationId);

        ProblemDetail problem =
                problem(
                        HttpStatus.CONFLICT,
                        "Conflicto de datos",
                        "La operación entra en conflicto con el estado actual de los datos.",
                        "data_conflict");
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        LOGGER.error(
                "event=UNHANDLED_ERROR correlationId={} exception={}",
                correlationId,
                ex.getClass().getName());

        ProblemDetail problem =
                problem(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error interno",
                        "La solicitud no pudo completarse.",
                        "internal_error");
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status, String title, String detail, String code) {
        return ResponseEntity.status(status).body(problem(status, title, detail, code));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:reto-developer:problem:" + code));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
