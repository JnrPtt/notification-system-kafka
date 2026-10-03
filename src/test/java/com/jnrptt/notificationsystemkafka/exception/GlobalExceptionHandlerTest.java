package com.jnrptt.notificationsystemkafka.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/users");
    }

    @Test
    void resourceNotFoundMapsTo404() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("User not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("User not found");
        assertThat(body.path()).isEqualTo("/api/v1/users");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.validationErrors()).isEmpty();
    }

    @Test
    void duplicateResourceMapsTo409() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleDuplicateResource(new DuplicateResourceException("already exists"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("already exists");
        assertThat(response.getBody().error()).isEqualTo("Conflict");
    }

    @Test
    void dataIntegrityViolationMapsTo409WithGenericMessage() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("constraint xyz details"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("The request conflicts with existing data");
        assertThat(response.getBody().message()).doesNotContain("xyz");
    }

    @Test
    void methodArgumentNotValidMapsTo400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("user", "email", "Email must have a valid format"),
                new FieldError("user", "name", "Name cannot be empty")));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().validationErrors())
                .containsEntry("email", "Email must have a valid format")
                .containsEntry("name", "Name cannot be empty")
                .hasSize(2);
    }

    @Test
    void constraintViolationMapsTo400UsingPropertyPathAsKey() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("getExpenseById.id");
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be greater than 0");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().validationErrors())
                .containsEntry("getExpenseById.id", "must be greater than 0");
    }
}
