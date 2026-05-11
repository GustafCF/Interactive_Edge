package com.br.elohostel.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SubscriptionStatusConverter implements AttributeConverter<SubscriptionStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SubscriptionStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public SubscriptionStatus convertToEntityAttribute(Integer dbData) {
        if(dbData == null){
            return null;
        }
        return SubscriptionStatus.valueOf(dbData);
    }
}