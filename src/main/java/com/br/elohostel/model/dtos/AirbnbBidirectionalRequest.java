package com.br.elohostel.model.dtos;

public class AirbnbBidirectionalRequest {
    private String airbnbIcalUrl;
    private String propertyId;
    private String propertyName;
    private String calendarName;
    private Integer roomNumber;
    private Long roomId; // ✅ NOVO: Para associar diretamente com Room

    // Construtores
    public AirbnbBidirectionalRequest() {}

    public AirbnbBidirectionalRequest(String airbnbIcalUrl, String propertyId, String propertyName) {
        this.airbnbIcalUrl = airbnbIcalUrl;
        this.propertyId = propertyId;
        this.propertyName = propertyName;
    }

    // Getters e Setters
    public String getAirbnbIcalUrl() { return airbnbIcalUrl; }
    public void setAirbnbIcalUrl(String airbnbIcalUrl) { this.airbnbIcalUrl = airbnbIcalUrl; }
    
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    
    public String getCalendarName() { return calendarName; }
    public void setCalendarName(String calendarName) { this.calendarName = calendarName; }
    
    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
    
    // ✅ NOVO: Getter e Setter para roomId
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    @Override
    public String toString() {
        return "AirbnbBidirectionalRequest{" +
                "propertyId='" + propertyId + '\'' +
                ", propertyName='" + propertyName + '\'' +
                ", roomNumber=" + roomNumber +
                ", roomId=" + roomId +
                '}';
    }
}