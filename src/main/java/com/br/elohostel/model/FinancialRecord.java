package com.br.elohostel.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.br.elohostel.model.enums.PeriodType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_FINANCIAL_RECORD")
public class FinancialRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate; 

    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "total_revenue", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "average_daily_rate", precision = 10, scale = 2)
    private BigDecimal averageDailyRate = BigDecimal.ZERO;

    @Column(name = "occupancy_rate", precision = 5, scale = 2)
    private BigDecimal occupancyRate = BigDecimal.ZERO;

    @Column(name = "total_reservations")
    private Integer totalReservations = 0;

    @Column(name = "total_guests")
    private Integer totalGuests = 0;

    @Column(name = "total_nights")
    private Integer totalNights = 0;

    @Column(name = "revenue_per_available_room", precision = 10, scale = 2)
    private BigDecimal revPAR = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "financial_record_room_types", joinColumns = @JoinColumn(name = "financial_record_id"))
    private Set<RoomTypeRevenue> roomTypeRevenues = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Construtores
    public FinancialRecord() {
        this.createdAt = LocalDateTime.now();
    }

    public FinancialRecord(LocalDate recordDate, PeriodType periodType) {
        this();
        this.recordDate = recordDate;
        this.periodType = periodType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getAverageDailyRate() {
        return averageDailyRate;
    }

    public void setAverageDailyRate(BigDecimal averageDailyRate) {
        this.averageDailyRate = averageDailyRate;
    }

    public BigDecimal getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(BigDecimal occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public Integer getTotalReservations() {
        return totalReservations;
    }

    public void setTotalReservations(Integer totalReservations) {
        this.totalReservations = totalReservations;
    }

    public Integer getTotalGuests() {
        return totalGuests;
    }

    public void setTotalGuests(Integer totalGuests) {
        this.totalGuests = totalGuests;
    }

    public Set<RoomTypeRevenue> getRoomTypeRevenues() {
        return roomTypeRevenues;
    }

    public Integer getTotalNights() {
        return totalNights;
    }

    public void setTotalNights(Integer totalNights) {
        this.totalNights = totalNights;
    }

    public BigDecimal getRevPAR() {
        return revPAR;
    }

    public void setRevPAR(BigDecimal revPAR) {
        this.revPAR = revPAR;
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

    public void addRevenue(BigDecimal revenue) {
        this.totalRevenue = this.totalRevenue.add(revenue);
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementReservations() {
        this.totalReservations++;
        this.updatedAt = LocalDateTime.now();
    }

    public void addGuests(int guests) {
        this.totalGuests += guests;
        this.updatedAt = LocalDateTime.now();
    }

    public void addNights(int nights) {
        this.totalNights += nights;
        this.updatedAt = LocalDateTime.now();
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
        FinancialRecord other = (FinancialRecord) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
