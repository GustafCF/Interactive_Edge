package com.br.elohostel.model.dtos;

import java.time.LocalDate;
import java.util.Set;

public record AddDatesRequest(Set<LocalDate> dates) {}