package com.br.elohostel.model.dtos;

public class BookingBidirectionalRequest {
    private String bookingIcalUrl;
    private String propertyId;
    private String propertyName;
    private String calendarName;
    private Integer roomNumber;

    // Construtores
    public BookingBidirectionalRequest() {}

    public BookingBidirectionalRequest(String bookingIcalUrl, String propertyId, String propertyName) {
        this.bookingIcalUrl = bookingIcalUrl;
        this.propertyId = propertyId;
        this.propertyName = propertyName;
    }

    // Getters e Setters
    public String getBookingIcalUrl() { return bookingIcalUrl; }
    public void setBookingIcalUrl(String bookingIcalUrl) { this.bookingIcalUrl = bookingIcalUrl; }
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public String getCalendarName() { return calendarName; }
    public void setCalendarName(String calendarName) { this.calendarName = calendarName; }
    public Integer getRoomNumber() { return roomNumber; }
    public void setRoomNumber(Integer roomNumber) { this.roomNumber = roomNumber; }
}