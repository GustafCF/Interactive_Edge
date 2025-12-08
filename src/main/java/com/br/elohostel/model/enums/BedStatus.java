package com.br.elohostel.model.enums;

public enum BedStatus {

    VAGUE(1),
    OCCUPIED(2),
    AVAILABLE(3),      // Disponível
    MAINTENANCE(4),    // Em manutenção
    CLEANING(5),       // Em limpeza
    RESERVED(6);        // Reservada

    private int code; 

    private BedStatus(int code){
        this.code = code;
    }

    public int getCode(){
        return code;
    }

    public static BedStatus valueOf(int code) {
        for(BedStatus value : BedStatus.values()){
            if(value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Code invalid!");
    }
}
