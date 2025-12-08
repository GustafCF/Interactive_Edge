package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PeriodTypeConverter implements AttributeConverter<PeriodType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PeriodType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public PeriodType convertToEntityAttribute(Integer dbData) {
        if(dbData == null) {
            return null;
        }
        return PeriodType.valueOf(dbData);
    }

}
