package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoomTypeConverter implements AttributeConverter<RoomType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RoomType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public RoomType convertToEntityAttribute(Integer dbData) {
        if(dbData == null){
            return null;
        }
        return RoomType.valueOf(dbData);
    }
}
