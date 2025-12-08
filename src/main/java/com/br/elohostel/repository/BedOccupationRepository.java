package com.br.elohostel.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.BedOccupation;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Room;

@Repository
public interface BedOccupationRepository extends JpaRepository<BedOccupation, Long> {
   
    /**
     * ✅ CORREÇÃO: Consulta para encontrar conflitos usando MEMBER OF
     */
    @Query("SELECT bo FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    List<BedOccupation> findConflicts(@Param("bed") Bed bed, @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ CORREÇÃO: Método simplificado usando MEMBER OF
     */
    @Query("SELECT bo FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND :date MEMBER OF bo.occupiedDays")
    List<BedOccupation> findByBedAndOccupiedDaysContaining(@Param("bed") Bed bed, @Param("date") LocalDate date);

    List<BedOccupation> findByReserve(Reserve reserve);

    /**
     * ✅ CORREÇÃO: Usando COUNT e MEMBER OF
     */
    @Query("SELECT COUNT(bo) > 0 FROM BedOccupation bo WHERE bo.bed.id = :bedId " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    boolean existsByBedIdAndOccupiedDaysIn(@Param("bedId") Long bedId, 
                                      @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ CORREÇÃO: Consulta para múltiplas datas
     */
    @Query("SELECT bo FROM BedOccupation bo WHERE " +
           "EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    List<BedOccupation> findByOccupiedDaysIn(@Param("dates") Set<LocalDate> dates);
    
    /**
     * ✅ CORREÇÃO: Consulta para período usando BETWEEN
     */
    @Query("SELECT bo FROM BedOccupation bo WHERE " +
           "EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od BETWEEN :startDate AND :endDate)")
    List<BedOccupation> findByOccupiedDaysBetween(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate);
    
    /**
     * ✅ CORREÇÃO: Consulta para data específica
     */
    @Query("SELECT bo FROM BedOccupation bo WHERE :date MEMBER OF bo.occupiedDays")
    List<BedOccupation> findByOccupiedDaysContaining(@Param("date") LocalDate date);
    

    List<BedOccupation> findByBed(Bed bed);
    

    /**
     * ✅ CORREÇÃO: Método para verificar existência
     */
    @Query("SELECT COUNT(bo) > 0 FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    boolean existsByBedAndOccupiedDaysIn(@Param("bed") Bed bed, @Param("dates") Set<LocalDate> dates);

    @Query("SELECT bo FROM BedOccupation bo WHERE bo.reserve = :reserve AND bo.bed.room = :room")
    Optional<BedOccupation> findByReserveAndRoom(@Param("reserve") Reserve reserve, @Param("room") Room room);

    /**
     * ✅ NOVO: Método para verificar disponibilidade de uma cama em datas específicas
     */
    @Query("SELECT COUNT(bo) = 0 FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    boolean isBedAvailableForDates(@Param("bed") Bed bed, @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ NOVO: Encontrar todas as ocupações de uma cama
     */
    @Query("SELECT bo FROM BedOccupation bo WHERE bo.bed = :bed")
    List<BedOccupation> findAllByBed(@Param("bed") Bed bed);
}