package com.br.elohostel.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.FinancialRecord;
import com.br.elohostel.model.enums.PeriodType;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findByRecordDateAndPeriodType(LocalDate recordDate, PeriodType periodType);
    
    List<FinancialRecord> findByPeriodTypeAndRecordDateBetween(
        PeriodType periodType, LocalDate startDate, LocalDate endDate);
    
    List<FinancialRecord> findByPeriodTypeOrderByRecordDateDesc(PeriodType periodType);
    
    @Query("SELECT fr FROM FinancialRecord fr WHERE fr.periodType = :periodType AND YEAR(fr.recordDate) = :year ORDER BY fr.recordDate")
    List<FinancialRecord> findByPeriodTypeAndYear(
        @Param("periodType") PeriodType periodType, 
        @Param("year") int year);
    
    @Query("SELECT fr FROM FinancialRecord fr WHERE fr.periodType = :periodType AND YEAR(fr.recordDate) = :year AND MONTH(fr.recordDate) = :month")
    List<FinancialRecord> findByPeriodTypeAndYearAndMonth(
        @Param("periodType") PeriodType periodType, 
        @Param("year") int year, 
        @Param("month") int month);
    
    @Query("SELECT COALESCE(SUM(fr.totalRevenue), 0) FROM FinancialRecord fr WHERE fr.periodType = :periodType AND fr.recordDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalRevenueByPeriodTypeAndDateRange(
        @Param("periodType") PeriodType periodType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}