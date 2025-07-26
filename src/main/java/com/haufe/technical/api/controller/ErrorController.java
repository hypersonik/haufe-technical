package com.haufe.technical.api.controller;

import com.haufe.technical.api.exception.ApiException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * ErrorController handles exceptions thrown by the API and returns appropriate error responses.
 * It specifically handles ApiException and formats the response accordingly.
 */
@ControllerAdvice
public class ErrorController {

    /**
     * Handles ApiException and returns a ResponseEntity with the error details.
     *
     * @param ex the ApiException to handle
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        return new ResponseEntity<>(buildResponse(ex), ex.getCode());
    }

    /**
     * Handles PropertyReferenceException and returns a ResponseEntity with the error details.
     *
     * @param ex the PropertyReferenceException to handle
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, Object>> handlePropertyReferenceException(PropertyReferenceException ex) {
        return new ResponseEntity<>(buildResponse(ex), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all other exceptions and returns a ResponseEntity with the error details.
     *
     * @param ex the Exception to handle
     * @return a ResponseEntity containing the error details
     */
//    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        return new ResponseEntity<>(buildResponse(ex), HttpStatus.BAD_REQUEST);
    }

    /**
     * Builds a response map for the Exception.
     *
     * @param ex the Exception to build the response for
     * @return a map containing the error description
     */
    private static Map<String, Object> buildResponse(Exception ex) {
        // For demonstration purposes, we return a simple map with the error description.
        return Map.of("error",
                Map.of("description", ex.getMessage()));
    }
}
