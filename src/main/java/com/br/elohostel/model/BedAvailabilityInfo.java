package com.br.elohostel.model;

import java.time.LocalDate;
import java.util.Set;

public class BedAvailabilityInfo {

    private final Integer roomNumber;
    private final Integer totalBeds;
    private final Integer availableBeds;
    private final Set<LocalDate> dates;
    
    public BedAvailabilityInfo(Integer roomNumber, Integer totalBeds, Integer availableBeds, Set<LocalDate> dates) {
        this.roomNumber = roomNumber;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
        this.dates = dates;
    }
    
    public Integer getRoomNumber() { return roomNumber; }
    public Integer getTotalBeds() { return totalBeds; }
    public Integer getAvailableBeds() { return availableBeds; }
    public Set<LocalDate> getDates() { return dates; }
    public boolean isAvailable() { return availableBeds > 0; }

}
