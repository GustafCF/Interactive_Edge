package com.br.elohostel.model.enums;

public enum PeriodType {

    DIARIO(1),    
    MENSAL(2),   
    ANUAL(3);
    
    private int code;

    private PeriodType(int code){
        this.code = code;
    }

    public int getCode(){
        return code;
    }

    public static PeriodType valueOf(int code) {
        for(PeriodType value : PeriodType.values()){
            if(value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Code invalid!");
    }
}
