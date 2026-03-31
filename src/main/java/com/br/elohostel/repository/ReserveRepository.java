package com.br.elohostel.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.enums.ReserveStatus;

public interface ReserveRepository extends JpaRepository<Reserve, Long> {

    List<Reserve> findByReserveStatus(ReserveStatus reserveStatus);

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND " +
           "EXISTS (SELECT 1 FROM r.reservedDays rd WHERE rd BETWEEN :startDate AND :endDate)")
    List<Reserve> findByReserveStatusAndReservedDaysBetween(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND " +
           "r.financialProcessed = :processed AND " +
           "EXISTS (SELECT 1 FROM r.reservedDays rd WHERE rd BETWEEN :startDate AND :endDate)")
    List<Reserve> findByReserveStatusAndProcessedAndDateRange(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("processed") Boolean processed,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("UPDATE Reserve r SET r.financialProcessed = true WHERE r.reserveStatus = com.br.elohostel.model.enums.ReserveStatus.CONFIRMED AND r.financialProcessed IS NULL")
    int markAllConfirmedAsProcessed();

    @Modifying
    @Query("UPDATE Reserve r SET r.financialProcessed = true WHERE r.reserveStatus = :reserveStatus AND r.financialProcessed IS NULL")
    int markAllByReserveStatusAsProcessed(@Param("reserveStatus") ReserveStatus reserveStatus);

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND :date MEMBER OF r.reservedDays")
    List<Reserve> findByReserveStatusAndReservedDate(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("date") LocalDate date
    );

    @Modifying
    @Query("UPDATE Reserve r SET r.financialProcessed = true WHERE r.reserveStatus = :reserveStatus AND r.financialProcessed IS NULL")
    int markReservationsAsProcessedByStatus(@Param("reserveStatus") ReserveStatus reserveStatus);

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND " +
        ":date IN (SELECT rd FROM r.reservedDays rd)")
    List<Reserve> findByReserveStatusAndReservedDateContains(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("date") LocalDate date
    );

    List<Reserve> findByReserveStatusAndTenant_TenantKey(ReserveStatus reserveStatus, String tenantKey);

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND " +
           "EXISTS (SELECT 1 FROM r.reservedDays rd WHERE rd BETWEEN :startDate AND :endDate) AND " +
           "r.tenant.tenantKey = :tenantKey")
    List<Reserve> findByReserveStatusAndReservedDaysBetween(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("tenantKey") String tenantKey
    );

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND " +
           "r.financialProcessed = :processed AND " +
           "EXISTS (SELECT 1 FROM r.reservedDays rd WHERE rd BETWEEN :startDate AND :endDate) AND " +
           "r.tenant.tenantKey = :tenantKey")
    List<Reserve> findByReserveStatusAndProcessedAndDateRange(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("processed") Boolean processed,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("tenantKey") String tenantKey
    );

    @Modifying
    @Query("UPDATE Reserve r SET r.financialProcessed = true WHERE r.reserveStatus = :reserveStatus AND r.financialProcessed IS NULL AND r.tenant.tenantKey = :tenantKey")
    int markAllByReserveStatusAsProcessed(@Param("reserveStatus") ReserveStatus reserveStatus, @Param("tenantKey") String tenantKey);

    @Query("SELECT r FROM Reserve r WHERE r.reserveStatus = :reserveStatus AND :date MEMBER OF r.reservedDays AND r.tenant.tenantKey = :tenantKey")
    List<Reserve> findByReserveStatusAndReservedDate(
        @Param("reserveStatus") ReserveStatus reserveStatus,
        @Param("date") LocalDate date,
        @Param("tenantKey") String tenantKey
    );

    Optional<Reserve> findByIdAndTenant_TenantKey(Long id, String tenantKey);

    List<Reserve> findByTenant_TenantKey(String tenantKey);
}