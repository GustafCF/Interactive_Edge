package com.br.elohostel.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.enums.BedStatus;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByRoomId(Long roomId);

    List<Bed> findByBedStatusAndRoomId(BedStatus bedStatus, Long roomId);

    /**
     * ✅ CORREÇÃO: Consulta otimizada usando EXISTS
     */
    @Query("SELECT b FROM Bed b WHERE b.room.id = :roomId " +
           "AND b.bedStatus = com.br.elohostel.model.enums.BedStatus.VAGUE " +
           "AND NOT EXISTS (" +
           "   SELECT bo FROM BedOccupation bo WHERE bo.bed = b " +
           "   AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)" +
           ")")
    List<Bed> findAvailableBedsInRoom(@Param("roomId") Long roomId, 
                                     @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ CORREÇÃO: Versão com parâmetro de status
     */
    @Query("SELECT b FROM Bed b WHERE b.room.id = :roomId " +
           "AND b.bedStatus = :bedStatus " +
           "AND NOT EXISTS (" +
           "   SELECT bo FROM BedOccupation bo WHERE bo.bed = b " +
           "   AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)" +
           ")")
    List<Bed> findBedsInRoomByStatus(@Param("roomId") Long roomId, 
                                   @Param("bedStatus") BedStatus bedStatus,
                                   @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ CORREÇÃO: Método simplificado - ignora status, só verifica ocupação
     */
    @Query("SELECT b FROM Bed b WHERE b.room.id = :roomId " +
           "AND NOT EXISTS (" +
           "   SELECT bo FROM BedOccupation bo WHERE bo.bed = b " +
           "   AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)" +
           ")")
    List<Bed> findUnoccupiedBedsInRoom(@Param("roomId") Long roomId, 
                                      @Param("dates") Set<LocalDate> dates);
    
    /**
     * Encontra camas por quarto
     */
    List<Bed> findByRoom(Room room);
    
    /**
     * ✅ NOVO: Encontra camas por quarto e status
     */
    List<Bed> findByRoomAndBedStatus(Room room, BedStatus bedStatus);
    
    /**
     * Encontra camas por status
     */
    List<Bed> findByBedStatus(BedStatus bedStatus);
    
    /**
     * Conta camas por quarto e status
     */
    @Query("SELECT COUNT(b) FROM Bed b WHERE b.room.id = :roomId AND b.bedStatus = :status")
    Long countByRoomAndStatus(@Param("roomId") Long roomId, @Param("status") BedStatus status);

    /**
     * ✅ CORREÇÃO: Método otimizado para camas disponíveis
     */
    @Query("SELECT b FROM Bed b WHERE b.room.id = :roomId " +
           "AND b.bedStatus = com.br.elohostel.model.enums.BedStatus.VAGUE " +
           "AND NOT EXISTS (" +
           "   SELECT bo FROM BedOccupation bo WHERE bo.bed = b " +
           "   AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)" +
           ")")
    List<Bed> findAvailableAndUnoccupiedBeds(@Param("roomId") Long roomId, 
                                           @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ NOVO: Contar camas disponíveis (mais eficiente que buscar lista)
     */
    @Query("SELECT COUNT(b) FROM Bed b WHERE b.room.id = :roomId " +
           "AND b.bedStatus = com.br.elohostel.model.enums.BedStatus.VAGUE " +
           "AND NOT EXISTS (" +
           "   SELECT bo FROM BedOccupation bo WHERE bo.bed = b " +
           "   AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)" +
           ")")
    Long countAvailableBedsInRoom(@Param("roomId") Long roomId, 
                                @Param("dates") Set<LocalDate> dates);

    /**
     * ✅ NOVO: Verificar se uma cama específica está disponível
     */
    @Query("SELECT COUNT(b) > 0 FROM Bed b WHERE b.id = :bedId " +
           "AND b.bedStatus = com.br.elohostel.model.enums.BedStatus.VAGUE " +
           "AND NOT EXISTS (" +
           "   SELECT bo FROM BedOccupation bo WHERE bo.bed.id = :bedId " +
           "   AND EXISTS (SELECT 1 FROM bo.occupiedDays od WHERE od IN :dates)" +
           ")")
    boolean isBedAvailable(@Param("bedId") Long bedId, 
                         @Param("dates") Set<LocalDate> dates);
}