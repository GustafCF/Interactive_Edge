package com.br.elohostel.model.enums;

public enum RoleStatus {

    ADMIN(1),
    BASIC(2);

    private final int code;

    private RoleStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RoleStatus valueOf(int code) {
        for (RoleStatus value : RoleStatus.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code");
    }

}
