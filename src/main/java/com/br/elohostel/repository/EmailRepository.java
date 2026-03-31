package com.br.elohostel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Email;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

}
