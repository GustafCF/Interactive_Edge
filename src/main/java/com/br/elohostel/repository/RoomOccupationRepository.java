package com.br.elohostel.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.RoomOccupation;

@Repository
public interface RoomOccupationRepository extends JpaRepository<RoomOccupation, Long> {
    
    /**
     * ✅ CORREÇÃO: Consulta usando MEMBER OF para múltiplas datas
     */
    @Query("SELECT ro FROM RoomOccupation ro WHERE ro.room = :room AND " +
           "EXISTS (SELECT 1 FROM ro.occupiedDays od WHERE od IN :dates)")
    List<RoomOccupation> findConflicts(@Param("room") Room room, @Param("dates") Set<LocalDate> dates);
    
    /**
     * ✅ CORREÇÃO: Método alternativo usando MEMBER OF
     */
    @Query("SELECT ro FROM RoomOccupation ro WHERE ro.room = :room AND " +
           ":date MEMBER OF ro.occupiedDays")
    List<RoomOccupation> findByRoomAndOccupiedDaysContaining(@Param("room") Room room, @Param("date") LocalDate date);

    /**
     * ✅ CORREÇÃO: Consulta para período específico
     */
    @Query("SELECT ro FROM RoomOccupation ro WHERE ro.room = :room AND " +
           "EXISTS (SELECT 1 FROM ro.occupiedDays od WHERE od BETWEEN :startDate AND :endDate)")
    List<RoomOccupation> findConflictsBetween(@Param("room") Room room,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    List<RoomOccupation> findByReserve(Reserve reserve);

    /**
     * ✅ NOVO: Verificar se existe conflito para um quarto em datas específicas
     */
    @Query("SELECT COUNT(ro) > 0 FROM RoomOccupation ro WHERE ro.room = :room AND " +
           "EXISTS (SELECT 1 FROM ro.occupiedDays od WHERE od IN :dates)")
    boolean existsConflictForRoomAndDates(@Param("room") Room room, @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ NOVO: Verificar disponibilidade de um quarto
     */
    @Query("SELECT COUNT(ro) = 0 FROM RoomOccupation ro WHERE ro.room = :room AND " +
           "EXISTS (SELECT 1 FROM ro.occupiedDays od WHERE od IN :dates)")
    boolean isRoomAvailableForDates(@Param("room") Room room, @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ NOVO: Encontrar todas as ocupações de um quarto
     */
    @Query("SELECT ro FROM RoomOccupation ro WHERE ro.room = :room")
    List<RoomOccupation> findAllByRoom(@Param("room") Room room);

    /**
     * ✅ NOVO: Encontrar ocupações por data específica
     */
    @Query("SELECT ro FROM RoomOccupation ro WHERE :date MEMBER OF ro.occupiedDays")
    List<RoomOccupation> findByDate(@Param("date") LocalDate date);
}