package com.cocos.llallire.infrastructure.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.cocos.llallire.domain.exception.InvalidOrderRequestException;
import com.cocos.llallire.domain.exception.OrderNotCancellableException;
import com.cocos.llallire.domain.exception.ResourceNotFoundException;

/**
 * Translates exceptions into HTTP responses using the RFC 9457 format ({@link ProblemDetail}). Business errors
 * (4xx) are logged at INFO and unexpected errors at ERROR, so they show up in the log backend with their trace.
 *
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404</li>
 *   <li>{@link InvalidOrderRequestException} and malformed bodies or parameters → 400</li>
 *   <li>{@link OrderNotCancellableException} → 409</li>
 *   <li>Spring MVC client errors ({@link ErrorResponse}: unknown path, wrong method, ...) → their own 4xx</li>
 *   <li>any other error → 500, without exposing internal details</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing entities.
     *
     * @param exception thrown exception
     * @return problem with status 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        LOGGER.info("Request rejected with 404: {}", exception.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    /**
     * Handles invalid order requests.
     *
     * @param exception thrown exception
     * @return problem with status 400
     */
    @ExceptionHandler(InvalidOrderRequestException.class)
    public ProblemDetail handleInvalidOrder(InvalidOrderRequestException exception) {
        LOGGER.info("Request rejected with 400: {}", exception.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid order request", exception.getMessage());
    }

    /**
     * Handles unreadable JSON bodies or bodies with out-of-range values (for example, an unknown {@code side}).
     *
     * @param exception thrown exception
     * @return problem with status 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body",
                "The request body is not valid JSON or contains invalid values");
    }

    /**
     * Handles path or query parameters with an invalid type (for example, a non-numeric id).
     *
     * @param exception thrown exception
     * @return problem with status 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "Parameter '%s' has an invalid value".formatted(exception.getName()));
    }

    /**
     * Handles attempts to cancel orders that are not in NEW status.
     *
     * @param exception thrown exception
     * @return problem with status 409
     */
    @ExceptionHandler(OrderNotCancellableException.class)
    public ProblemDetail handleNotCancellable(OrderNotCancellableException exception) {
        LOGGER.info("Request rejected with 409: {}", exception.getMessage());
        return problem(HttpStatus.CONFLICT, "Order cannot be cancelled", exception.getMessage());
    }

    /**
     * Handles any unexpected error. Logs it and responds without internal details.
     *
     * <p>Spring MVC's own client errors (unknown path → 404, unsupported method → 405, unsupported media type → 415,
     * missing parameter → 400, ...) implement {@link ErrorResponse}; they keep their status and standard body and
     * are logged at INFO, since they are not server failures.
     *
     * @param exception thrown exception
     * @return the framework's problem for {@link ErrorResponse} exceptions, otherwise a problem with status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            LOGGER.info("Request rejected with {}: {}", errorResponse.getStatusCode().value(), exception.getMessage());
            return errorResponse.getBody();
        }
        LOGGER.error("Unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "An unexpected error occurred");
    }

    /**
     * Builds a {@link ProblemDetail}.
     *
     * @param status HTTP status
     * @param title  short title
     * @param detail error detail
     * @return built problem
     */
    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
