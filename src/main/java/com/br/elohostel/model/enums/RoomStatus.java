package com.br.elohostel.model.enums;

public enum RoomStatus {

    VAGUE(1),
    OCCUPIED(2);

    private final int code;

    private RoomStatus (int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RoomStatus valueOf(int code) {
        for (RoomStatus value : RoomStatus.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid Room Status code");
    }

}
