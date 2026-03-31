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
   
    @Query("SELECT bo FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND bo.tenant.tenantKey = :tenantKey " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    List<BedOccupation> findConflicts(@Param("bed") Bed bed, 
                                    @Param("dates") Set<LocalDate> dates,
                                    @Param("tenantKey") String tenantKey);

    @Query("SELECT bo FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND bo.tenant.tenantKey = :tenantKey " +
           "AND :date MEMBER OF bo.occupiedDays")
    List<BedOccupation> findByBedAndOccupiedDaysContaining(@Param("bed") Bed bed, 
                                                         @Param("date") LocalDate date,
                                                         @Param("tenantKey") String tenantKey);

    List<BedOccupation> findByReserveAndTenant_TenantKey(Reserve reserve, String tenantKey);

    @Query("SELECT COUNT(bo) > 0 FROM BedOccupation bo WHERE bo.bed.id = :bedId " +
           "AND bo.tenant.tenantKey = :tenantKey " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    boolean existsByBedIdAndOccupiedDaysIn(@Param("bedId") Long bedId, 
                                         @Param("dates") Set<LocalDate> dates,
                                         @Param("tenantKey") String tenantKey);

    @Query("SELECT bo FROM BedOccupation bo WHERE " +
           "bo.tenant.tenantKey = :tenantKey " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    List<BedOccupation> findByOccupiedDaysIn(@Param("dates") Set<LocalDate> dates,
                                           @Param("tenantKey") String tenantKey);
    
    @Query("SELECT bo FROM BedOccupation bo WHERE " +
           "bo.tenant.tenantKey = :tenantKey " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od BETWEEN :startDate AND :endDate)")
    List<BedOccupation> findByOccupiedDaysBetween(@Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("tenantKey") String tenantKey);

    @Query("SELECT bo FROM BedOccupation bo WHERE :date MEMBER OF bo.occupiedDays " +
           "AND bo.tenant.tenantKey = :tenantKey")
    List<BedOccupation> findByOccupiedDaysContaining(@Param("date") LocalDate date,
                                                    @Param("tenantKey") String tenantKey);
    
    List<BedOccupation> findByBedAndTenant_TenantKey(Bed bed, String tenantKey);
    
    @Query("SELECT COUNT(bo) > 0 FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND bo.tenant.tenantKey = :tenantKey " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    boolean existsByBedAndOccupiedDaysIn(@Param("bed") Bed bed, 
                                       @Param("dates") Set<LocalDate> dates,
                                       @Param("tenantKey") String tenantKey);

    @Query("SELECT bo FROM BedOccupation bo WHERE bo.reserve = :reserve AND bo.bed.room = :room " +
           "AND bo.tenant.tenantKey = :tenantKey")
    Optional<BedOccupation> findByReserveAndRoom(@Param("reserve") Reserve reserve, 
                                                @Param("room") Room room,
                                                @Param("tenantKey") String tenantKey);

    @Query("SELECT COUNT(bo) = 0 FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND bo.tenant.tenantKey = :tenantKey " +
           "AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)")
    boolean isBedAvailableForDates(@Param("bed") Bed bed, 
                                 @Param("dates") Set<LocalDate> dates,
                                 @Param("tenantKey") String tenantKey);

    @Query("SELECT bo FROM BedOccupation bo WHERE bo.bed = :bed " +
           "AND bo.tenant.tenantKey = :tenantKey")
    List<BedOccupation> findAllByBed(@Param("bed") Bed bed, @Param("tenantKey") String tenantKey);
}