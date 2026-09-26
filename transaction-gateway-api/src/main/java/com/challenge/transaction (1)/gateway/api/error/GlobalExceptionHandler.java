package com.challenge.transaction.gateway.api.error;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> badRequest() {
        return response(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "No fue posible interpretar o validar la solicitud.",
                "bad_request");
    }

    @ExceptionHandler(FeignException.class)
    ResponseEntity<ProblemDetail> upstream(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());

        if (status == null || status.is5xxServerError()) {
            return response(
                    HttpStatus.BAD_GATEWAY,
                    "Servicio no disponible",
                    "No fue posible completar la operación con el servicio de transacciones.",
                    "upstream_error");
        }

        String detail =
                status == HttpStatus.UNAUTHORIZED
                        ? "Credenciales inválidas."
                        : "La solicitud fue rechazada por el servicio de transacciones.";

        return response(status, "Solicitud rechazada", detail, "upstream_rejected");
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
