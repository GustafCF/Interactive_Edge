package com.br.elohostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.elohostel.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    List<User> findByTenant_TenantKey(String tenantKey);
    Optional<User> findByIdAndTenant_TenantKey(Long id, String tenantKey);
    Optional<User> findByNameAndTenant_TenantKey(String name, String tenantKey);
    Optional<User> findByEmailAndTenant_TenantKey(String email, String tenantKey);
}
