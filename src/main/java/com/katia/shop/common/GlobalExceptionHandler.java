package com.katia.shop.common;

import com.katia.shop.auth.exception.InvalidRefreshTokenException;
import com.katia.shop.product.exception.*;
import com.katia.shop.order.exception.OrderNotFoundException;
import com.katia.shop.order.exception.InvalidOrderStateException;
import com.katia.shop.user.exception.EmailAlreadyExistException;
import com.katia.shop.user.exception.UserNotFoundException;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ProductNotFoundException.class, OrderNotFoundException.class,
            UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        log.warn("Not found. path={}, message={}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler({ProductAlreadyExistException.class, EmailAlreadyExistException.class,
            InsufficientStockException.class, InvalidOrderStateException.class,
            ProductNotAvailableException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest req) {
        log.warn("Conflict. path={}, message={}", req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencyFailure(HttpServletRequest req) {
        log.warn("Concurrency failure. path={}", req.getRequestURI());
        return respond(HttpStatus.CONFLICT,
                "The resource was modified by another request. Please retry.", req.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest req) {
        log.warn("Data integrity violation. path={}, cause={}", req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT,
                "The request conflicts with existing data", req.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest req) {
        log.warn("Authentication failed. path={}", req.getRequestURI());
        return respond(HttpStatus.UNAUTHORIZED, "Invalid email or password", req.getRequestURI());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex,
                                                              HttpServletRequest req) {
        log.warn("Invalid refresh token. path={}", req.getRequestURI());
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest req) {
        log.warn("Access denied. path={}", req.getRequestURI());
        return respond(HttpStatus.FORBIDDEN, "Access denied", req.getRequestURI());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleBadSort(HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid sort property", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error. path={}", req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", req.getRequestURI());
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
              HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        log.warn("Validation failed. path={}, fields={}", path(request), fieldErrors.keySet());
        return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), 400,
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validation failed", path(request), fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
                                                             HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String message = status.is4xxClientError() ? "Invalid request" : "An unexpected error occurred";
        return ResponseEntity.status(statusCode).headers(headers).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, path(request), null));
    }

    private static String path(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, path, null));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Authentication required", req.getRequestURI());
    }



}