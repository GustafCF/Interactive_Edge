package com.br.elohostel.model.enums;

public enum SubscriptionStatus {

    ACTIVE(1),
    EXPIRED(2),
    NONE(3),
    TRIAL(4),
    PENDING(5);

    private final int code;

    private SubscriptionStatus(int code) {
        this.code = code;
    }

    public int getCode(){
        return code;
    }

    public static SubscriptionStatus valueOf(int code){
        for (SubscriptionStatus value : SubscriptionStatus.values()){
            if(value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid Room Type Code");
    }
}
