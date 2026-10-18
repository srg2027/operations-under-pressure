package com.newtonite.operations.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should map ResourceNotFoundException to 404 NOT_FOUND")
    void shouldHandleResourceNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/work-items/99");
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Work item not found"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Work item not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/work-items/99");
    }

    @Test
    @DisplayName("Should map UnauthorizedOperationException to 403 FORBIDDEN")
    void shouldHandleUnauthorizedOperation() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/work-items/1/claim");
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedOperation(
                new UnauthorizedOperationException("User not a member of team"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("Should map WorkItemConflictException and InvalidWorkflowException to 409 CONFLICT")
    void shouldHandleConflictExceptions() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/work-items/1/claim");
        ResponseEntity<ErrorResponse> response1 = handler.handleConflict(
                new WorkItemConflictException("Item concurrently claimed"),
                request
        );
        ResponseEntity<ErrorResponse> response2 = handler.handleConflict(
                new InvalidWorkflowException("Item already completed"),
                request
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should map IllegalArgumentException to 400 BAD_REQUEST")
    void shouldHandleIllegalArgument() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/work-items");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Note cannot be blank"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should map unhandled generic exceptions to 500 without leaking stack trace or internal info")
    void shouldHandleGenericExceptionSafely() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/work-items");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Sensitive database credentials or SQL syntax error"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage())
                .doesNotContain("database")
                .doesNotContain("SQL")
                .isEqualTo("An unexpected internal error occurred. Please try again later.");
    }
}
