package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Signature;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByClienteEmail(String email);
    List<Signature> findByStatus(String status);
    Optional<Signature> findByMpPreapprovalId(String preapprovalId);
}
