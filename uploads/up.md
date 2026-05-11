private void createOccupations(Reserve reserve, Room room, Set<LocalDate> dates) {
        String tenantKey = getCurrentTenantKey();
        Tenant tenant = getCurrentTenant();
        
        if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
            RoomOccupation ro = new RoomOccupation();
            ro.setRoom(room);
            ro.setReserve(reserve);
            ro.getOccupiedDays().addAll(dates);
            ro.setTenant(room.getTenant());
            roomOccupationRepo.save(ro);
            
        } else if (room.isSharedRoom()) {
            int numberOfGuests = reserve.getGuest().size();
            
            List<Bed> availableBeds = findAvailableBedsForRoom(room, dates, numberOfGuests);
            
            for (int i = 0; i < numberOfGuests; i++) {
                Bed bed = availableBeds.get(i);
                
                BedOccupation bo = new BedOccupation();
                bo.setBed(bed);
                bo.setReserve(reserve);
                bo.getOccupiedDays().addAll(dates);
                bo.setTenant(tenant);
                
                bedOccupationRepo.save(bo);
                
                // Atualizar status da cama
                bed.setBedStatus(BedStatus.OCCUPIED);
                bedRepo.save(bed);
            }
        }
    }




    @Transactional
    public Reserve createReservationWithGuest(CreateReservationWithGuestRequest request) {
        try {
            if (request.guests() == null || request.guests().isEmpty()) {
                throw new IllegalArgumentException("Pelo menos um hóspede deve ser informado");
            }
            String tenantKey = getCurrentTenantKey();

            List<Guest> guests = request.guests().stream()
                .map(this::findOrCreateGuestWithCompleteInfo)
                .collect(Collectors.toList());
            
            Room room = roomRepo.findByNumberAndTenant_TenantKey(request.roomNumber(), tenantKey)
                    .orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado: " + request.roomNumber()));

            // Validar disponibilidade considerando o número de hóspedes
            validateDatesAvailability(room, request.dates(), guests.size());

            Reserve reserve = new Reserve();
            reserve.setReservedDays(request.dates());
            reserve.setReserveStatus(ReserveStatus.CONFIRMED);
            reserve.setTenant(getCurrentTenant());
            
            guests.forEach(reserve.getGuest()::add);
            reserve.getRooms().add(room);
            
            reserve.setInitialValue(room.getPrice());
            reserve.setUseCustomValue(false);
            
            reserve.calculateTotalValue();

            Reserve savedReserve = reserveRepo.save(reserve);

            createOccupations(savedReserve, room, request.dates());

            guests.forEach(guest -> {
                guest.getReservation().add(savedReserve);
                guestRepo.save(guest);
            });

            return savedReserve;

        } catch (Exception e) {
            logger.severe("Erro na criação de reserva: " + e.getMessage());
            throw new RuntimeException("Falha na criação de reserva: " + e.getMessage(), e);
        }
    }



    @Transactional
    public Reserve checkIn(Long id) {
        String tenantKey = getCurrentTenantKey();        
        Reserve reserva = findById(id);
        reserva.getCheckIn().add(LocalDateTime.now());

        var rooms = reserva.getRooms();
        for (Room room : rooms) {
            if (room.isExclusiveRoom() || room.isSharedBathroom() || room.isStudio() || room.isSuite()) {
                room.setRoomStatus(RoomStatus.OCCUPIED);
            } else if (room.isSharedRoom()) {
                List<BedOccupation> bedOccupations = bedOccupationRepo.findByReserveAndTenant_TenantKey(reserva, tenantKey);
                for (BedOccupation bo : bedOccupations) {
                    Bed bed = bo.getBed();
                    bed.setBedStatus(BedStatus.OCCUPIED);
                    bedRepo.save(bed);
                }

                boolean allBedsOccupied = room.getBeds().stream()
                        .allMatch(b -> b.getBedStatus() == BedStatus.OCCUPIED);
                
                if (allBedsOccupied) {
                    room.setRoomStatus(RoomStatus.OCCUPIED);
                }
            }
        }
        roomRepo.saveAll(rooms);

        return reserveRepo.save(reserva);
    }