package com.br.elohostel.exceptions;

public class RoomOccupied extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RoomOccupied (Object obj) {
        super("The room is occupied: " + obj);
    }
}