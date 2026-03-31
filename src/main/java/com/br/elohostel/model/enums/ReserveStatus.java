package com.br.elohostel.model.enums;

public enum ReserveStatus {

    CONFIRMED(1),      
    CANCELLED(2);   
    
    private int code;

    private ReserveStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReserveStatus valueOf(int code) {
        for(ReserveStatus value : ReserveStatus.values()){
            if(value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Code invalid!");
    }
}