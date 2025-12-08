package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoomStatusConverter implements AttributeConverter<RoomStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RoomStatus roomStatus){
        if (roomStatus == null) {
            return null;
        }
        return roomStatus.getCode();       
    }

    @Override
    public RoomStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        return RoomStatus.valueOf(code);
    }
}
