package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleStatusConverter implements AttributeConverter<RoleStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(RoleStatus roleStatus) {
        if (roleStatus == null) {
            return null;
        }
        return roleStatus.getCode();
    }

    @Override
    public RoleStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return RoleStatus.valueOf(code);
    }
}
