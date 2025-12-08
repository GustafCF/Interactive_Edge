package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReserveStatusConverter implements AttributeConverter<ReserveStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ReserveStatus attribute) {
        if (attribute == null){
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public ReserveStatus convertToEntityAttribute(Integer dbData) {
        if(dbData == null) {
            return null;
        }
        return ReserveStatus.valueOf(dbData);
    }
}
