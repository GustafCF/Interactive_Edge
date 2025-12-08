package com.br.elohostel.model.dtos;

import java.time.LocalDate;
import java.util.Set;

public record ReservesionRequest(Set<LocalDate> dates, String guestName, Integer roomNumber) {

}
