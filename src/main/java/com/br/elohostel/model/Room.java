package com.br.elohostel.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.br.elohostel.model.enums.RoomStatus;
import com.br.elohostel.model.enums.RoomType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_ROOM")
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer number;
    private RoomStatus roomStatus;
    private RoomType roomType;
    private BigDecimal price;

    @JsonIgnore
    @ManyToMany(mappedBy = "rooms")
    private List<Reserve> reservation;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private Set<Bed> beds = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<RoomOccupation> roomOccupations = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<AirbnbSync> airbnbSync = new ArrayList<>();

    public Room() {}

    public Room(Integer number, RoomStatus roomStatus, RoomType roomType, BigDecimal price) {
        this.number = number;
        this.roomStatus = roomStatus;
        this.roomType = roomType;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public RoomStatus getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(RoomStatus roomStatus) {
        this.roomStatus = roomStatus;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<Reserve> getReservation() {
        return reservation;
    }

    public Set<Bed> getBeds() {
        return beds;
    }

    public List<AirbnbSync> getAirbnbSync() {
        return airbnbSync;
    }

    public Boolean isExclusiveRoom() {
        return RoomType.EXCLUSIVE == this.roomType;
    }

    public Boolean isSharedRoom() {
        return RoomType.SHARED == this.roomType;
    }

    public Boolean isSuite() {
        return RoomType.SUITE == this.roomType;
    }

    public Boolean isStudio() {
        return RoomType.STUDIO == this.roomType;
    }

    public Boolean isSharedBathroom() {
        return RoomType.ROOM_SHARED_BATHROOM == this.roomType;
    }

     public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public List<RoomOccupation> getRoomOccupations() {
        return roomOccupations;
    }

    public void setRoomOccupations(List<RoomOccupation> roomOccupations) {
        this.roomOccupations = roomOccupations;
    }

    public Boolean isAnyExclusiveType() {
        return isExclusiveRoom() || isSuite() || isStudio() || isSharedBathroom();
    }

    public String getRoomTypeDescription() {
        switch (this.roomType) {
            case SHARED: return "QUARTO COMPARTILHADO";
            case EXCLUSIVE: return "QUARTO EXCLUSIVO";
            case SUITE: return "SUÍTE";
            case STUDIO: return "STÚDIO";
            case ROOM_SHARED_BATHROOM: return "QUARTO COM BANHEIRO COMPARTILHADO";
            default: return "TIPO DESCONHECIDO";
        }
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
        Room other = (Room) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}