package com.br.elohostel.model.dtos;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AutoRecurring {
    
    private Integer frequency;
    
    @JsonProperty("frequency_type")
    private String frequencyType;
    
    @JsonProperty("transaction_amount")
    private BigDecimal transactionAmount;
    
    @JsonProperty("currency_id")
    private String currencyId;
    
    public Integer getFrequency() {
        return frequency;
    }
    
    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }
    
    public String getFrequencyType() {
        return frequencyType;
    }
    
    public void setFrequencyType(String frequencyType) {
        this.frequencyType = frequencyType;
    }
    
    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }
    
    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }
    
    public String getCurrencyId() {
        return currencyId;
    }
    
    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }
}