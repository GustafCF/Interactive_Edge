package com.br.elohostel.model.dtos;

import java.math.BigDecimal;

public class UpdateReserveValueRequest {
    private BigDecimal customValue;
    private Boolean useCustomValue;
    private BigDecimal extraGuestFee;

    // Construtores, getters e setters
    public UpdateReserveValueRequest() {}

    public UpdateReserveValueRequest(BigDecimal customValue, Boolean useCustomValue, BigDecimal extraGuestFee) {
        this.customValue = customValue;
        this.useCustomValue = useCustomValue;
        this.extraGuestFee = extraGuestFee;
    }

    public BigDecimal getCustomValue() { return customValue; }
    public void setCustomValue(BigDecimal customValue) { this.customValue = customValue; }
    
    public Boolean getUseCustomValue() { return useCustomValue; }
    public void setUseCustomValue(Boolean useCustomValue) { this.useCustomValue = useCustomValue; }
    
    public BigDecimal getExtraGuestFee() { return extraGuestFee; }
    public void setExtraGuestFee(BigDecimal extraGuestFee) { this.extraGuestFee = extraGuestFee; }
}