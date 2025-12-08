package com.br.elohostel.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.br.elohostel.model.enums.ReserveStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_RESERVE")
public class Reserve implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ElementCollection
    @CollectionTable(name = "reserve_days", joinColumns = @JoinColumn(name = "reserve_id"))
    @Column(name = "reserved_day")
    private Set<LocalDate> reservedDays = new HashSet<>();
    @Column(name = "reserve_status")
    private ReserveStatus reserveStatus;
    @Column(name = "check_in")
    private List<LocalDateTime> checkIn = new ArrayList<>();
    @Column(name = "check_out")
    private List<LocalDateTime> checkOut = new ArrayList<>();
    @Column(name = "initital_value")
    private BigDecimal initialValue;
    @Column(name = "custom_value")
    private BigDecimal customValue;
    
    @Column(name = "use_custom_value")
    private Boolean useCustomValue = false;

    @Column(name = "financial_processed")
    private Boolean financialProcessed = false;
    
    @Column(name = "extra_guest_fee")
    private BigDecimal extraGuestFee = new BigDecimal("20.00");

    @JsonIgnore
    @OneToOne(mappedBy = "reserve")
    private AirbnbReservation airbnbReservation;

    

    @ManyToMany(mappedBy = "reservation")
    private Set<Guest> guest = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "TB_RESERVATIONS_ROOMS",
        joinColumns = @JoinColumn(name = "reserve_id"),
        inverseJoinColumns = @JoinColumn(name = "rooms_id")
    )
    private Set<Room> rooms = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "reserve", cascade = CascadeType.ALL)
    private List<RoomOccupation> roomOccupation = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "reserve", cascade = CascadeType.ALL)
    private List<BedOccupation> bedOccupation = new ArrayList<>();

    public Reserve() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<LocalDate> getReservedDays() {
        return reservedDays;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = initialValue;
    }

    public ReserveStatus getReserveStatus() {
        return reserveStatus;
    }

    public void setReserveStatus(ReserveStatus reserveStatus) {
        this.reserveStatus = reserveStatus;
    }

    public List<LocalDateTime> getCheckIn() {
        return checkIn;
    }

    public List<LocalDateTime> getCheckOut() {
        return checkOut;
    }

    public Set<Guest> getGuest() {
        return guest;
    }

    public Set<Room> getRooms() {
        return rooms;
    }

    public AirbnbReservation getAirbnbReservation() {
        return airbnbReservation;
    }

    public void setAirbnbReservation(AirbnbReservation airbnbReservation) {
        this.airbnbReservation = airbnbReservation;
    }

    public List<RoomOccupation> getRoomOccupation() {
        return roomOccupation;
    }

    public Boolean dateBefore(LocalDate date) {
        return date.isBefore(LocalDate.now());
    }

    public void setReservedDays(Set<LocalDate> reservedDays) {
        this.reservedDays = reservedDays;
    }

    public List<BedOccupation> getBedOccupation() {
        return bedOccupation;
    }

    public BigDecimal getCustomValue() {
        return customValue;
    }

    public Boolean getFinancialProcessed() {
        return financialProcessed;
    }

    public void setFinancialProcessed(Boolean financialProcessed) {
        this.financialProcessed = financialProcessed;
    }

    public void setCustomValue(BigDecimal customValue) {
        this.customValue = customValue;
    }

    public Boolean getUseCustomValue() {
        return useCustomValue;
    }

    public void setUseCustomValue(Boolean useCustomValue) {
        this.useCustomValue = useCustomValue;
    }

    public BigDecimal getExtraGuestFee() {
        return extraGuestFee;
    }

    public void setExtraGuestFee(BigDecimal extraGuestFee) {
        this.extraGuestFee = extraGuestFee;
    }

    public LocalDate getCheckInDate() {
        if (reservedDays == null || reservedDays.isEmpty()) {
            return null;
        }
        return reservedDays.stream()
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    public LocalDate getCheckOutDate() {
        if (reservedDays == null || reservedDays.isEmpty()) {
            return null;
        }
        return reservedDays.stream()
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    // Método para obter o período formatado
    public String getFormattedPeriod() {
        LocalDate checkIn = getCheckInDate();
        LocalDate checkOut = getCheckOutDate();
        
        if (checkIn == null || checkOut == null) {
            return "Período não definido";
        }
        
        return checkIn.toString() + " a " + checkOut.toString();
    }

    public BigDecimal calculateTotalValue() {
        if (Boolean.TRUE.equals(useCustomValue) && customValue != null) {
            return customValue;
        }
        
        BigDecimal baseValue = this.initialValue != null ? this.initialValue : BigDecimal.ZERO;
        int numberOfDays = this.reservedDays != null ? this.reservedDays.size() : 0;
        int extraGuests = Math.max(0, (this.guest != null ? this.guest.size() : 0) - 1);
        
        // ✅ CORREÇÃO: Multiplicar a taxa de hóspedes extras pelo número de dias
        BigDecimal dailyTotal = baseValue.multiply(BigDecimal.valueOf(numberOfDays));
        BigDecimal extraFees = this.extraGuestFee.multiply(BigDecimal.valueOf(extraGuests * numberOfDays)); // Multiplica pelos dias
        
        return dailyTotal.add(extraFees);
    }

    public int getNumberOfDays() {
        return this.reservedDays != null ? this.reservedDays.size() : 0;
    }
    
    public int getNumberOfExtraGuests() {
        return Math.max(0, (this.guest != null ? this.guest.size() : 0) - 1);
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
        Reserve other = (Reserve) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
