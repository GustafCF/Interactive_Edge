package com.br.elohostel.model.dtos;

import java.math.BigDecimal;

public record UpdateReserveValueRequest(
    BigDecimal customTotalAmount,
    Boolean useCustomAmount,
    BigDecimal extraGuestDailyFee
) {
}