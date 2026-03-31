package com.br.elohostel.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_TENANT")
public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true, nullable = false)
    private String tenantKey;
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<User> user = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<Reserve> reserves = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<Room> rooms = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<Bed> beds = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<Guest> guests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<AirbnbReservation> airbnbReservation = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<AirbnbSync> airbnbSync = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<BedOccupation> bedOccupation = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "tenant")
    private List<RoomOccupation> roomOccupation = new ArrayList<>();

    public Tenant() {}

    public Tenant(String tenantKey, String name) {
        this.tenantKey = tenantKey;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
        this.tenantKey = tenantKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getUser() {
        return user;
    }

    public List<Reserve> getReserves() {
        return reserves;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Bed> getBeds() {
        return beds;
    }

    public List<Guest> getGuests() {
        return guests;
    }

    public List<AirbnbReservation> getAirbnbReservation() {
        return airbnbReservation;
    }

    public List<AirbnbSync> getAirbnbSync() {
        return airbnbSync;
    }

    public List<BedOccupation> getBedOccupation() {
        return bedOccupation;
    }

    public List<RoomOccupation> getRoomOccupation() {
        return roomOccupation;
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
        Tenant other = (Tenant) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}