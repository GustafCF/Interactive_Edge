package com.br.elohostel.exceptions;

public class NoBedsAvailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoBedsAvailableException(String message) {
        super(message);
    }

}
