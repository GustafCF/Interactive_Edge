package com.br.elohostel.model.enums;

public enum StatusEmail {

    SENT(1),
	ERROR(2);

    private final int code;

    private StatusEmail(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static StatusEmail valueOf(int code) {
        for (StatusEmail value : StatusEmail.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code");
    }



}
