package com.br.elohostel.model.dtos;

import java.math.BigDecimal;

public record UpdateDataReserveDTO (BigDecimal customValue, BigDecimal extraGuestFee, Boolean useCustomValue, BigDecimal initialValue) {

}