package com.br.elohostel.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(Object obj) {
        super("Resource not found: " + obj);
    }
}
