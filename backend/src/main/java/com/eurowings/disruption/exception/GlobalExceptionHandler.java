package com.eurowings.disruption.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Turns exceptions into RFC 7807 problem responses (application/problem+json).
 *
 * Spring MVC errors such as 404 and 405 use the same format through ResponseEntityExceptionHandler.
 * Each problem carries a stable "code" so clients can react without parsing messages.
 * Unexpected errors are logged with their stack trace but never expose internals to the client.
 *
 * A failing transport source is not an error here: the search leaves that source out (see DisruptionService).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DisruptionDataUnavailableException.class)
    public ProblemDetail handleUnavailable(DisruptionDataUnavailableException ex, HttpServletRequest request) {
        log.error("Disruption data unavailable", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Disruption data unavailable",
                "The disruption could not be loaded. Please try again later.", "DISRUPTION_DATA_UNAVAILABLE", request);
    }

    @ExceptionHandler(InvalidDisruptionDataException.class)
    public ProblemDetail handleInvalid(InvalidDisruptionDataException ex, HttpServletRequest request) {
        log.error("Invalid disruption data: {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid disruption data",
                ex.getMessage(), "INVALID_DISRUPTION_DATA", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                "Something went wrong while finding options.", "INTERNAL_ERROR", request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code,
                                         HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return problem;
    }
}
