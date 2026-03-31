package com.br.elohostel.model;

import java.io.Serializable;
import java.math.BigDecimal;

import com.br.elohostel.model.enums.RoomType;

import jakarta.persistence.Embeddable;

@Embeddable
public class RoomTypeRevenue implements Serializable {
    private static final long serialVersionUID = 1L;

    private RoomType roomType;
    
    private Integer reservationsCount = 0;
    private Integer nightsCount = 0;
    private BigDecimal revenue = BigDecimal.ZERO;

    public RoomTypeRevenue() {}

    public RoomTypeRevenue(RoomType roomType) {
        this.roomType = roomType;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Integer getReservationsCount() {
        return reservationsCount;
    }

    public void setReservationsCount(Integer reservationsCount) {
        this.reservationsCount = reservationsCount;
    }

    public Integer getNightsCount() {
        return nightsCount;
    }

    public void setNightsCount(Integer nightsCount) {
        this.nightsCount = nightsCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public void incrementReservations() {
        this.reservationsCount++;
    }

    public void addNights(int nights) {
        this.nightsCount += nights;
    }

    public void addRevenue(BigDecimal revenue) {
        this.revenue = this.revenue.add(revenue);
    }

}
