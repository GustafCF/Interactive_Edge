package com.br.elohostel.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_AIRBNB_SYNC")
public class AirbnbSync implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ical_url", length = 500)
    private String icalUrl;

    @Column(name = "export_ical_url", length = 500)
    private String exportIcalUrl;

    @Column(name = "property_id")
    private String propertyId;

    @Column(name = "property_name")
    private String propertyName;

    @Column(name = "calendar_name")
    private String calendarName;

    @Column(name = "room_number")
    private Integer roomNumber;

     // ✅ ADICIONE: Associação direta com Room
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "sync_direction")
    @Enumerated(EnumType.STRING)
    private SyncDirection syncDirection = SyncDirection.BIDIRECTIONAL;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "airbnbSync", cascade = CascadeType.ALL)
    private List<AirbnbReservation> reservations = new ArrayList<>();

    public enum SyncDirection {
        IMPORT_ONLY, EXPORT_ONLY, BIDIRECTIONAL
    }

    // Construtores
    public AirbnbSync() {}

    public AirbnbSync(String icalUrl, String propertyId, String propertyName) {
        this.icalUrl = icalUrl;
        this.propertyId = propertyId;
        this.propertyName = propertyName;
    }

     public AirbnbSync(String icalUrl, String propertyId, String propertyName, Integer roomNumber) {
        this.icalUrl = icalUrl;
        this.propertyId = propertyId;
        this.propertyName = propertyName;
        this.roomNumber = roomNumber;
    }

    public Long getId() {
        return id;
    }

     public void setId(Long id) {
         this.id = id;
     }

     public String getIcalUrl() {
         return icalUrl;
     }

     public void setIcalUrl(String icalUrl) {
         this.icalUrl = icalUrl;
     }

     public String getExportIcalUrl() {
         return exportIcalUrl;
     }

     public void setExportIcalUrl(String exportIcalUrl) {
         this.exportIcalUrl = exportIcalUrl;
     }

     public String getPropertyId() {
         return propertyId;
     }

     public void setPropertyId(String propertyId) {
         this.propertyId = propertyId;
     }

     public String getPropertyName() {
         return propertyName;
     }

     public void setPropertyName(String propertyName) {
         this.propertyName = propertyName;
     }

     public String getCalendarName() {
         return calendarName;
     }

     public void setCalendarName(String calendarName) {
         this.calendarName = calendarName;
     }

     public Integer getRoomNumber() {
         return roomNumber;
     }

     public void setRoomNumber(Integer roomNumber) {
         this.roomNumber = roomNumber;
     }

     public Room getRoom() {
         return room;
     }

     public void setRoom(Room room) {
         this.room = room;
     }

     public SyncDirection getSyncDirection() {
         return syncDirection;
     }

     public void setSyncDirection(SyncDirection syncDirection) {
         this.syncDirection = syncDirection;
     }

     public LocalDateTime getLastSync() {
         return lastSync;
     }

     public void setLastSync(LocalDateTime lastSync) {
         this.lastSync = lastSync;
     }

     public Boolean getIsActive() {
         return isActive;
     }

     public void setIsActive(Boolean isActive) {
         this.isActive = isActive;
     }

     public LocalDateTime getCreatedAt() {
         return createdAt;
     }

     public void setCreatedAt(LocalDateTime createdAt) {
         this.createdAt = createdAt;
     }

    public List<AirbnbReservation> getReservations() {
        return reservations;
    }

    public Integer getAssociatedRoomNumber() {
        if (this.room != null) {
            return this.room.getNumber();
        }
        return this.roomNumber;
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
        AirbnbSync other = (AirbnbSync) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}    