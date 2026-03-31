package com.br.elohostel.model.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CreateReservationWithGuestRequest(
    List<GuestInfo> guests,     
    Integer roomNumber,      
    Set<LocalDate> dates      
) {
    public record GuestInfo(
        String name,          
        String rg,             
        String phone,         
        String email          
    ) {}
}