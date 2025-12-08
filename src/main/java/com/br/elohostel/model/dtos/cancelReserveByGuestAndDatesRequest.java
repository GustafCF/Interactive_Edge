package com.br.elohostel.model.dtos;

import java.time.LocalDate;
import java.util.Set;

public record cancelReserveByGuestAndDatesRequest(String guestName, Set<LocalDate> dates) {

}
