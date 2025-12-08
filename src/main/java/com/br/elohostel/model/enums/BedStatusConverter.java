package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BedStatusConverter implements AttributeConverter<BedStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(BedStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public BedStatus convertToEntityAttribute(Integer dbData) {
        if(dbData == null) {
            return null;
        }
        return BedStatus.valueOf(dbData);
    }
}