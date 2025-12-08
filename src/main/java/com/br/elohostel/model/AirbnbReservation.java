package com.br.elohostel.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_AIRBNB_RESERVATION")
public class AirbnbReservation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "airbnb_reservation_id")
    private String airbnbReservationId;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    @Column(name = "status")
    private String status;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "is_processed")
    private Boolean isProcessed = false;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "airbnb_sync_id")
    private AirbnbSync airbnbSync;

    @OneToOne
    @JoinColumn(name = "reserve_id")
    private Reserve reserve;

    public AirbnbReservation() {}

    public AirbnbReservation(Long id, String airbnbReservationId, String guestName, String guestEmail,
            String guestPhone, LocalDate checkIn, LocalDate checkOut, String status, LocalDateTime lastModified,
            Boolean isProcessed, AirbnbSync airbnbSync, Reserve reserve) {
        this.id = id;
        this.airbnbReservationId = airbnbReservationId;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestPhone = guestPhone;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = status;
        this.lastModified = lastModified;
        this.isProcessed = isProcessed;
        this.airbnbSync = airbnbSync;
        this.reserve = reserve;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAirbnbReservationId() {
        return airbnbReservationId;
    }

    public void setAirbnbReservationId(String airbnbReservationId) {
        this.airbnbReservationId = airbnbReservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Boolean getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(Boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public AirbnbSync getAirbnbSync() {
        return airbnbSync;
    }

    public void setAirbnbSync(AirbnbSync airbnbSync) {
        this.airbnbSync = airbnbSync;
    }

    public Reserve getReserve() {
        return reserve;
    }

    public void setReserve(Reserve reserve) {
        this.reserve = reserve;
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
        AirbnbReservation other = (AirbnbReservation) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}