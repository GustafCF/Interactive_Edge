package com.br.elohostel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

}
