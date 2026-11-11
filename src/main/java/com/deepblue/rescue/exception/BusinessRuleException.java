package com.deepblue.rescue.exception;

/**
 * El recurso existe, pero la operacion solicitada viola una regla de negocio.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
