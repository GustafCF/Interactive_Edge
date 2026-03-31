package com.br.elohostel.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_BOOKING_RESERVATION")
public class BookingReservation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reservation_id")
    private String bookingReservationId;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

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

    @ManyToOne
    @JoinColumn(name = "booking_sync_id")
    private BookingSync bookingSync;

    @ManyToOne
    @JoinColumn(name = "reserve_id")
    private Reserve reserve;

    // Construtores
    public BookingReservation() {}

    public BookingReservation(String bookingReservationId, String guestName, LocalDate checkIn, LocalDate checkOut) {
        this.bookingReservationId = bookingReservationId;
        this.guestName = guestName;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.lastModified = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookingReservationId() { return bookingReservationId; }
    public void setBookingReservationId(String bookingReservationId) { this.bookingReservationId = bookingReservationId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    public Boolean getIsProcessed() { return isProcessed; }
    public void setIsProcessed(Boolean isProcessed) { this.isProcessed = isProcessed; }
    public BookingSync getBookingSync() { return bookingSync; }
    public void setBookingSync(BookingSync bookingSync) { this.bookingSync = bookingSync; }
    public Reserve getReserve() { return reserve; }
    public void setReserve(Reserve reserve) { this.reserve = reserve; }

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
        BookingReservation other = (BookingReservation) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    
}