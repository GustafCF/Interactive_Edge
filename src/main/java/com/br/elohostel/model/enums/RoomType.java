package com.br.elohostel.model.enums;

public enum RoomType {

    SHARED(1),
    EXCLUSIVE(2),
    SUITE(3),
    STUDIO(4),
    ROOM_SHARED_BATHROOM(5);

    private final int code;

    private RoomType(int code) {
        this.code = code;
    }

    public int getCode(){
        return code;
    }

    public static RoomType valueOf(int code){
        for (RoomType value : RoomType.values()){
            if(value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid Room Type Code");
    }

}
