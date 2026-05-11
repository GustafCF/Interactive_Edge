package com.br.elohostel.model;

import java.time.LocalDateTime;

import com.br.elohostel.model.enums.SubscriptionStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_SUBSCRIPTION")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    private String mercadoPagoPreapprovalId; 
    private String mercadoPagoCollectorId;
    private LocalDateTime nextPaymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
    private SubscriptionStatus status; 
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    public Subscription() {}

    public Subscription(Tenant tenant, Plan plan, String mercadoPagoPreapprovalId,
                        String mercadoPagoCollectorId, LocalDateTime nextPaymentDate) {
        this.tenant = tenant;
        this.plan = plan;
        this.mercadoPagoPreapprovalId = mercadoPagoPreapprovalId;
        this.mercadoPagoCollectorId = mercadoPagoCollectorId;
        this.nextPaymentDate = nextPaymentDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
        this.status = status.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getMercadoPagoPreapprovalId() {
        return mercadoPagoPreapprovalId;
    }

    public void setMercadoPagoPreapprovalId(String mercadoPagoPreapprovalId) {
        this.mercadoPagoPreapprovalId = mercadoPagoPreapprovalId;
    }

    public String getMercadoPagoCollectorId() {
        return mercadoPagoCollectorId;
    }

    public void setMercadoPagoCollectorId(String mercadoPagoCollectorId) {
        this.mercadoPagoCollectorId = mercadoPagoCollectorId;
    }

    public LocalDateTime getNextPaymentDate() {
        return nextPaymentDate;
    }

    public void setNextPaymentDate(LocalDateTime nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Subscription other = (Subscription) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }
}