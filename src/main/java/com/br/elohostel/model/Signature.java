package com.br.elohostel.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_SIGNATURE")
public class Signature implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mpPreapprovalId;    
    private String status;              
    private LocalDateTime initialDate;
    private LocalDateTime finalDate;
    private String clienteEmail;
    private String clienteId;     

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;

    public Signature() {}

    public Signature(String mpPreapprovalId, String status, LocalDateTime initialDate,
            LocalDateTime finalDate, String clienteEmail, String clienteId) {
        this.mpPreapprovalId = mpPreapprovalId;
        this.status = status;
        this.initialDate = initialDate;
        this.finalDate = finalDate;
        this.clienteEmail = clienteEmail;
        this.clienteId = clienteId;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMpPreapprovalId() {
        return mpPreapprovalId;
    }

    public void setMpPreapprovalId(String mpPreapprovalId) {
        this.mpPreapprovalId = mpPreapprovalId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getInitialDate() {
        return initialDate;
    }

    public void setInitialDate(LocalDateTime initialDate) {
        this.initialDate = initialDate;
    }

    public LocalDateTime getFinalDate() {
        return finalDate;
    }

    public void setFinalDate(LocalDateTime finalDate) {
        this.finalDate = finalDate;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Signature other = (Signature) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
    
    
}