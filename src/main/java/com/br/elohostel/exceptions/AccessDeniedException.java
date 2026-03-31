package com.br.elohostel.exceptions;

public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException() {
        super("Access Denied!");
    }
}
