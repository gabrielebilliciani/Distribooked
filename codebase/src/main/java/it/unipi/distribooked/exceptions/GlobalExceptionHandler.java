package it.unipi.distribooked.exceptions;

import com.mongodb.MongoException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Global exception handler for managing exceptions across the application.
 *
 * This class centralizes exception handling and returns consistent error responses using ProblemDetail.
 *
 * We use ProblemDetail because it provides a standard, built-in way to structure error responses
 * without the need to manually define a custom ErrorResponse class. Additionally, it simplifies
 * API documentation generation, as it is compliant with OpenAPI and integrates seamlessly with tools
 * like Swagger or Springdoc.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation exceptions triggered by @Valid annotation.
     *
     * @param ex The exception object containing validation errors.
     * @return A structured error response with field-specific validation errors.
     */
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach(error -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//
//        logger.warn("Validation failed: {}", errors);
//
//        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
//        problemDetail.setProperty("validationErrors", errors);
//        return problemDetail;
//    }

//    @Override
//    protected ResponseEntity<Object> handleMethodArgumentNotValid(
//            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
//
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach(error -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//
//        logger.warn("Validation failed: {}", errors);
//
//        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
//        problemDetail.setProperty("validationErrors", errors);
//
//        return new ResponseEntity<>(problemDetail, headers, status);
//    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        logger.warn("Malformed JSON request: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request. Please check the request body syntax"
        );
        problemDetail.setType(URI.create("/errors/malformed-json"));
        problemDetail.setInstance(URI.create(request.getContextPath()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(problemDetail, headers, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        logger.warn("Validation failed: {}", validationErrors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more input parameters"
        );
        problemDetail.setProperty("validationErrors", validationErrors);
        problemDetail.setType(URI.create("/errors/validation"));
        problemDetail.setInstance(URI.create(request.getContextPath()));

        return new ResponseEntity<>(problemDetail, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        logger.warn("Validation failed: {}", validationErrors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more input parameters"
        );
        problemDetail.setProperty("validationErrors", validationErrors);
        problemDetail.setType(URI.create("/errors/validation"));
        problemDetail.setInstance(URI.create(request.getContextPath()));

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BookNotSavedException.class)
    public ProblemDetail handleBookNotSaved(BookNotSavedException ex) {
        logger.warn("Book not saved: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("/errors/book-not-saved"));

        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("/errors/resource-not-found"));

        return problemDetail;
    }

    /**
     * Handles cases where no authors or books are found.
     *
     * @param ex The exception object.
     * @return A structured error response indicating the resource was not found.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
        logger.info("No resource found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        problemDetail.setProperty("error", "Resource not found");

        return problemDetail;
    }

    /**
     * Handles authentication-related exceptions.
     *
     * @param ex The exception object.
     * @return A structured error response for authentication failures.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    /**
     * Handles bad credentials exceptions.
     *
     * @param ex The exception object.
     * @return A structured error response for bad credentials.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("Bad credentials provided: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    /**
     * Handles user registration conflicts.
     *
     * @param ex The exception object.
     * @return A structured error response for user already exists exceptions.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        logger.info("User registration conflict: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NoAvailableCopiesException.class)
    public ProblemDetail handleNoAvailableCopiesException(NoAvailableCopiesException ex) {
        logger.warn("No available copies exception: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CannotRemoveBookException.class)
    public ProblemDetail handleCannotRemoveBookException(CannotRemoveBookException ex) {
        logger.warn("Cannot remove book exception: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(LibraryEntryAlreadyExistsException.class)
    public ProblemDetail handleLibraryEntryAlreadyExistsException(LibraryEntryAlreadyExistsException ex) {
        logger.info("Library entry already exists: {}", ex.getMessage());

        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        logger.error("Operation failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Operation failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Invalid input: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ProblemDetail handleReservationConflictException(ReservationConflictException ex) {
        logger.warn("Reservation conflict: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setProperty("error", "Reservation conflict");
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        problemDetail.setType(URI.create("/errors/reservation-conflict"));

        return problemDetail;
    }

    /**
     * Handles MongoDB-specific exceptions.
     *
     * @param ex The MongoDB exception object.
     * @return A structured error response for database errors.
     */
    @ExceptionHandler(MongoException.class)
    public ProblemDetail handleMongoException(MongoException ex) {
        logger.error("MongoDB error occurred: {}", ex.getMessage(), ex);

        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while processing your request");
    }

    @ExceptionHandler(BookSaveException.class)
    public ProblemDetail handleBookSaveException(BookSaveException ex) {
        logger.warn("Book save failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles all uncaught exceptions.
     *
     * @param ex The exception object.
     * @return A structured error response for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAllExceptions(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
