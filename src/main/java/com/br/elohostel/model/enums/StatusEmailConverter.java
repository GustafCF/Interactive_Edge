package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusEmailConverter implements AttributeConverter<StatusEmail, Integer> {

    @Override
    public Integer convertToDatabaseColumn(StatusEmail attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public StatusEmail convertToEntityAttribute(Integer dbData) {
        if(dbData == null){
            return null;
        }
        return StatusEmail.valueOf(dbData);
    }

}
