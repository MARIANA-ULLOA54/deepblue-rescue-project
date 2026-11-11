package com.deepblue.rescue.exception;

/**
 * El recurso buscado no existe.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
