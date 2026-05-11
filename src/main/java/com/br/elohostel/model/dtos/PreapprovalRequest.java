package com.br.elohostel.model.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreapprovalRequest {
    private String reason;
    @JsonProperty("auto_recurring")
    private AutoRecurring autoRecurring;
    @JsonProperty("payer_email")
    private String payerEmail;
    @JsonProperty("payer_name")
    private String payerName;
    @JsonProperty("back_url")
    private String backUrl;
    private String status;
    
    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public AutoRecurring getAutoRecurring() {
        return autoRecurring;
    }
    public void setAutoRecurring(AutoRecurring autoRecurring) {
        this.autoRecurring = autoRecurring;
    }
    public String getPayerEmail() {
        return payerEmail;
    }
    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }
    public String getPayerName() {
        return payerName;
    }
    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }
    public String getBackUrl() {
        return backUrl;
    }
    public void setBackUrl(String backUrl) {
        this.backUrl = backUrl;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}